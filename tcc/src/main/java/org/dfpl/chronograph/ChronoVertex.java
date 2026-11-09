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
import org.dfpl.chronograph.Tokens.EventComparator;
import org.dfpl.chronograph.Tokens.TimeComparator;

import java.util.*;
import java.util.stream.Collectors;

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
 *
 */
public class ChronoVertex implements Vertex {

    private final ChronoGraph g;
    private final String id;
    private final int intId;
    private final HashMap<String, Object> properties;
    private final NavigableSet<VertexEvent> events;

    ChronoVertex(ChronoGraph g, String id) {
        this.id = id;
        this.g = g;
        this.properties = new HashMap<>();
        this.events = new TreeSet<>();
        this.intId = g.getVertexIdManager().get(id);
    }

    @Override
    public Collection<Edge> getEdges(Direction direction, List<String> labels) {
        ArrayList<Edge>[] edgeMap = direction.equals(Direction.OUT) ? g.getOutEdges() : g.getInEdges();

        ArrayList<Edge> edgesOfId = edgeMap[intId];

        if (edgesOfId == null || edgesOfId.isEmpty()) {
            return Collections.emptySet();
        }

        if (labels == null || labels.isEmpty())
            return edgesOfId;
        Set<String> labelSet = new HashSet<>(labels);

        return edgesOfId.stream().filter(edge -> labelSet.contains(edge.getLabel())).collect(Collectors.toSet());
    }

    public Collection<Edge> getEdges(Direction direction) {
        ArrayList<Edge>[] edgeMap = direction.equals(Direction.OUT) ? g.getOutEdges() : g.getInEdges();

        ArrayList<Edge> edgesOfId = edgeMap[intId];

        if (edgesOfId == null || edgesOfId.isEmpty()) {
            return Collections.emptySet();
        }

        return edgesOfId;
    }

    public Iterator<Edge> getEdgeIterator(Direction direction, Collection<String> labels) {

        if(direction == Direction.BOTH){
            return g.getEdges().parallelStream().filter(e -> e.getVertex(Direction.IN) == this || e.getVertex(Direction.OUT) == this)
                    .filter(e -> labels.contains(e.getLabel()))
                    .iterator();
        }

        ArrayList<Edge> edgesOfId = null;

        if(direction == Direction.IN)
            edgesOfId = g.getInEdges()[intId];
        else if (direction == Direction.OUT)
            edgesOfId = g.getOutEdges()[intId];

        if (edgesOfId == null || edgesOfId.isEmpty()) {
            return Collections.emptyIterator();
        }

        if (labels == null || labels.isEmpty())
            return edgesOfId.iterator();

        return edgesOfId.stream().filter(edge -> labels.contains(edge.getLabel())).iterator();
    }

    public Collection<Edge> getEdges(Direction direction, List<String> labels, HashSet<Vertex> vIds) {
        ArrayList<Edge>[] edgeMap = direction.equals(Direction.OUT) ? g.getOutEdges() : g.getInEdges();

        ArrayList<Edge> edgesOfId = edgeMap[intId];
        if(edgesOfId == null)
            edgesOfId = new ArrayList<>();

        if (labels == null || labels.isEmpty())
            return edgesOfId;
        Set<String> labelSet = new HashSet<>(labels);

        return edgesOfId.stream()
                .filter(edge -> vIds.contains(((ChronoVertex) edge.getVertex(direction.opposite())).getIntId()))
                .filter(edge -> labelSet.contains(edge.getLabel())).collect(Collectors.toSet());
    }

    @Override
    public Collection<Vertex> getVertices(Direction direction, List<String> labels) {
        ArrayList<Edge> edgeSet = null;
        if (direction.equals(Direction.OUT))
            edgeSet = g.getOutEdges()[intId];
        else if (direction.equals(Direction.IN))
            edgeSet = g.getInEdges()[intId];
        else
            return Collections.emptySet();

        if (edgeSet == null)
            return new HashSet<>();

        HashSet<String> labelSet = new HashSet<>(labels);
        return edgeSet.stream().filter(e -> labelSet.contains(e.getLabel()))
                .map(e -> e.getVertex(direction.opposite()))
                .collect(Collectors.toSet());
    }

    @Override
    public Edge addEdge(String label, Vertex inVertex) {
        return g.addEdge(this, inVertex, label);
    }

    @Override
    public void remove() {
        g.removeVertex(this);
    }

    /// ////////////////////////
    /// // Temporal Support ////
    /// ////////////////////////

    @Override
    public VertexEvent addEvent(Time time) {
        return fixTimeDisjointness(this.events, time);
    }

    private VertexEvent fixTimeDisjointness(NavigableSet<VertexEvent> events, Time time) {
        NavigableSet<VertexEvent> eventsToMerge = new TreeSet<>();
        NavigableSet<VertexEvent> eventsToExtend = new TreeSet<>();

        // Constants
        List<TemporalRelation> relationsForNull = Arrays.asList(TemporalRelation.cotemporal, TemporalRelation.during,
                TemporalRelation.starts, TemporalRelation.finishes);
        List<TemporalRelation> relationsForMerge = Arrays.asList(TemporalRelation.contains,
                TemporalRelation.isStartedBy, TemporalRelation.isFinishedBy);
        List<TemporalRelation> relationsForExtend = Arrays.asList(TemporalRelation.isOverlappedBy,
                TemporalRelation.overlapsWith, TemporalRelation.meets, TemporalRelation.isMetBy);

        for (VertexEvent event : events) {
            Time existingTime = event.getTime();

            // Null conditions
            if (relationsForNull.contains(existingTime.getTemporalRelation(time)))
                return null;

            // Merge conditions
            if (time instanceof TimePeriod && relationsForMerge.contains(existingTime.getTemporalRelation(time)))
                eventsToMerge.add(event);

            // Extend conditions
            if (time instanceof TimePeriod && existingTime instanceof TimePeriod
                    && relationsForExtend.contains(existingTime.getTemporalRelation(time)))
                eventsToExtend.add(event);
        }

        if (!eventsToMerge.isEmpty())
            events.removeAll(eventsToMerge);

        if (!eventsToExtend.isEmpty()) {
            long startTime = Long.min(eventsToExtend.first().getStartTime(), time.getStartTime());
            long finishTime = Long.max(eventsToExtend.last().getFinishTime(), time.getFinishTime());
            time = new TimePeriod(startTime, finishTime);
            events.removeAll(eventsToExtend);
        }
        VertexEvent newVertexEvent = new ChronoVertexEvent(this, time);
        events.add(newVertexEvent);
        return newVertexEvent;
    }

    @Override
    public VertexEvent getEvent(Time time) {
//		for (VertexEvent event : this.events) {
//			if (event.getTime().equals(time))
//				return event;
//		}
//		return null;
        return new ChronoVertexEvent(this, time);
    }

    @Override
    public NavigableSet<VertexEvent> getEvents(Time time, TemporalRelation tr, boolean awareOutEvents,
                                               boolean awareInEvents, EventComparator eventComparator) {

        NavigableSet<VertexEvent> resultSet = new TreeSet<>(eventComparator);

        resultSet.addAll(this.getEvents(time, tr));

        List<Direction> directions = new LinkedList<>();
        if (awareOutEvents)
            directions.add(Direction.OUT);
        if (awareInEvents)
            directions.add(Direction.IN);

        for (Direction direction : directions) {
            for (Vertex v : this.getVertices(direction, null)) {
                if (v.equals(this))
                    continue;
                for (VertexEvent vertexEvent : v.getEvents(time, tr, false, false, eventComparator)) {
                    Time t = vertexEvent.getTime();
                    fixTimeDisjointness(resultSet, t);
                }
            }
        }

        return resultSet;
    }

    @Override
    public void removeEvents(Time time, TemporalRelation temporalRelation) {
        this.events.removeIf(event -> time.getTemporalRelation(event.getTime()).equals(temporalRelation));
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
        times.addAll(this.events.stream().map(Event::getTime).toList());
        return times;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ChronoVertex))
            return false;
        return this.getId().equals(((ChronoVertex) obj).getId());
    }

    @Override
    public String toString() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> NavigableSet<T> getEvents(Time time, TemporalRelation... temporalRelations) {
        HashSet<TemporalRelation> temporalRelationSet = new HashSet<>(Arrays.asList(temporalRelations));
        if (temporalRelations == null || temporalRelationSet.isEmpty()) {
            return Collections.emptyNavigableSet();
        }

        // Use a stream to filter and collect the events
        return (NavigableSet<T>) this.events.stream()
                .filter(event -> temporalRelationSet.stream()
                        .anyMatch(tr -> event.getTime().checkTemporalRelation(time, tr)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> T getEvent(Time time, TemporalRelation tr) {
        for (Event event : this.events) {
            if (time.getTemporalRelation(event.getTime()).equals(tr))
                return (T) event;
        }

        return null;
    }

    public int getIntId() {
        return intId;
    }
}
