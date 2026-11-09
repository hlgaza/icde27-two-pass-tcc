/**
 * Apache 2.0 License
 * <p>
 * Portions Copyright 2026 Haifa Gaza
 * Modified functions
 * <p>
 * Copyright 2024 Jaewook Byun
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dfpl.chronograph;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.dfpl.chronograph.Tokens.TimeComparator;

import java.util.*;

/**
 * The in-memory implementation of temporal graph database.
 *
 * @author Jaewook Byun, Ph.D., Assistant Professor, DFPL, Department of
 * Software, Sejong University
 * @author Haifa Gaza, Master Student, DFPL, Sejong University
 * <br/>
 * Byun, Jaewook, Sungpil Woo, and Daeyoung Kim. "Chronograph: Enabling
 * temporal graph traversals for efficient information diffusion
 * analysis over time." IEEE Transactions on Knowledge and Data
 * Engineering 32.3 (2019): 424-437.
 * <br/>
 * Byun, Jaewook. "Enabling time-centric computation for efficient
 * temporal graph traversals from multiple sources." IEEE Transactions
 * on Knowledge and Data Engineering (2020). *
 */
public class ChronoEdge implements Edge {

    private final ChronoGraph g;
    private final String id;
    private final Vertex out;
    private final String label;
    private final Integer intLabel;
    private final Vertex in;
    private final int outId;
    private final int inId;
    private final HashMap<String, Object> properties;
//    private NavigableSet<EdgeEvent> eventsSet;
    private final List<EdgeEvent> eventsIndex = new ArrayList<>();
    private int eventCount = 0;

    public static final Comparator<EdgeEvent> TIME_COMPARATOR = Comparator.comparing(EdgeEvent::getTime)
            .thenComparing(o -> o.getVertex(Direction.OUT).getId())
            .thenComparing(o -> o.getVertex(Direction.IN).getId());

    public ChronoEdge(ChronoGraph g, Vertex out, String label, Vertex in) {
        this.g = g;
        this.out = out;
        this.label = label;
        this.in = in;
        this.outId = ((ChronoVertex) out).getIntId();
        this.inId = ((ChronoVertex) in).getIntId();
        this.id = getEdgeID(out, in, label);
        this.properties = new HashMap<>();

//        this.eventsSet = new TreeSet<>(TIME_COMPARATOR);

        this.intLabel = this.g.getLabelIdManager().add(label);
    }

    public static String getEdgeID(Vertex out, Vertex in, String label) {
        return out.toString() + "|" + label + "|" + in.toString();
    }

    public int getOutId(){
        return outId;
    }

    public  int getInId(){
        return inId;
    }

    @Override
    public Vertex getVertex(Direction direction) throws IllegalArgumentException {
        if (direction == Direction.OUT) {
            return out;
        } else if (direction == Direction.IN) {
            return in;
        } else {
            throw new IllegalArgumentException("A direction should be either OUT or IN");
        }
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void remove() {
        g.removeEdge(this);
    }

    @Override
    public EdgeEvent addEvent(Time time) {
        EdgeEvent newEvent = new ChronoEdgeEvent(this, time);

        int index = Collections.binarySearch(eventsIndex, newEvent, TIME_COMPARATOR);
        if (index < 0) {
            int loc = -(index + 1);
            eventsIndex.add(loc, newEvent);
            eventCount++;
        }

//        if (eventsSet != null) {
//            eventsSet.add(newEvent);
//        }

        return newEvent;
    }

    @Override
    public EdgeEvent getEvent(Time time) {
        EdgeEvent event = getCeiling(time);

        if (event != null && event.getTime().equals(time)) return event;

        return null;
    }

    @Override
    public Collection<EdgeEvent> getEvents() {
        return eventsIndex;
    }

    @Override
    public NavigableSet<EdgeEvent> getEvents(Time time, TemporalRelation temporalRelation,
                                             Comparator<Event> eventComparator) {

        NavigableSet<EdgeEvent> validEvents = new TreeSet<>(eventComparator);
        if (temporalRelation == null)
            return validEvents;

        this.eventsIndex.stream().filter(edgeEvent -> time.getTemporalRelation(edgeEvent.getTime()).equals(temporalRelation))
                .forEach(validEvents::add);

        return validEvents;
    }

    public int getEventCount() {
        return eventCount;
    }

    @Override
    public void removeEvents(Time time, TemporalRelation temporalRelation) {
        int index = Collections.binarySearch(eventsIndex, new ChronoEdgeEvent(this, time), TIME_COMPARATOR);
        if (index >= 0) {
            eventsIndex.remove(index);
            eventCount--;
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Edge))
            return false;
        return id.equals(obj.toString());
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(String key) {
        return (T) properties.get(key);
    }

    @Override
    public Set<String> getPropertyKeys() {
        return this.properties.keySet();
    }

    @Override
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T removeProperty(String key) {
        return (T) properties.remove(key);
    }

    @Override
    public NavigableSet<Time> getTimes(TimeComparator timeComparator) {
        NavigableSet<Time> times = new TreeSet<>(timeComparator);
        for (Event event : this.eventsIndex) {
            times.add(event.getTime());
        }
        return times;
    }

    public EdgeEvent getEvent(Time time, TemporalRelation temporalRelation) {
//        ChronoEdgeEvent eventToSearch = new ChronoEdgeEvent(this, time);

        if (temporalRelation == TemporalRelation.isAfter) {
            return this.getCeiling(time);
        } else if (temporalRelation == TemporalRelation.isBefore) {
            return this.getFloor(time);
        } else {
            return null;
        }
    }

    public EdgeEvent getCeiling(Time targetTime) {
        int low = 0;
        int high = eventsIndex.size() - 1;
        EdgeEvent bestMatch = null;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            EdgeEvent midVal = eventsIndex.get(mid);
            int cmp = midVal.getTime().compareTo(targetTime);

            if (cmp >= 0) {
                bestMatch = midVal; // This is a candidate for ceiling
                high = mid - 1;     // Keep looking left for a tighter bound
            } else {
                low = mid + 1;      // Target is larger, look right
            }
        }
        return bestMatch;
    }

    public EdgeEvent getFloor(Time targetTime) {
        int low = 0;
        int high = eventsIndex.size() - 1;
        EdgeEvent bestMatch = null;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            EdgeEvent midVal = eventsIndex.get(mid);
            int cmp = midVal.getTime().compareTo(targetTime);

            if (cmp <= 0) {
                bestMatch = midVal; // This is a candidate for floor
                low = mid + 1;      // Keep looking right for a tighter bound
            } else {
                high = mid - 1;     // Target is smaller, look left
            }
        }
        return bestMatch;
    }

    public int compareTo(Edge o) {
        return Integer.compare(inId, ((ChronoEdge) o).inId);
    }
}
