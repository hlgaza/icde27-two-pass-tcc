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
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import org.dfpl.chronograph.Tokens.EventComparator;

import java.util.*;

/**
 * The in-memory implementation of temporal graph database.
 *
 * @author Jaewook Byun, Ph.D., Assistant Professor, DFPL, Department of
 *         Software, Sejong University
 * 
 * @author Haifa Gaza, Master Student, DFPL, Sejong University
 * <br/>
 *         Byun, Jaewook, Sungpil Woo, and Daeyoung Kim. "Chronograph: Enabling
 *         temporal graph traversals for efficient information diffusion
 *         analysis over time." IEEE Transactions on Knowledge and Data
 *         Engineering 32.3 (2019): 424-437.
 * <br/>
 *         Byun, Jaewook. "Enabling time-centric computation for efficient
 *         temporal graph traversals from multiple sources." IEEE Transactions
 *         on Knowledge and Data Engineering (2020). *
 * 
 */
public class ChronoVertexEvent implements VertexEvent, Comparable<ChronoVertexEvent> {

	private final Vertex vertex;
	private final Time time;

	public ChronoVertexEvent(Vertex v, Time time) {
		this.vertex = v;
		this.time = time;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Event))
			return false;
		return this.vertex.equals(((Event) obj).getElement()) && this.getTime().equals(((Event) obj).getTime());
	}

	@Override
	public String toString() {
		return "ChronoVertexEvent{" + "vertex=" + vertex + ", time=" + time + '}';
	}

	/**
	 * Checks the difference of two events by comparing the element, and then the
	 * element
	 * 
	 * @param event
	 *            the event to be compared
	 * @return Integer difference
	 */
	@Override
	public int compareTo(ChronoVertexEvent event) {
		int elementComparison = this.getElement().getId().compareTo(event.getElement().getId());

		if (elementComparison != 0)
			return elementComparison;

		return this.getTime().compareTo(event.getTime());
	}

	@Override
	public String getId() {
		return vertex.getId() + "_" + time;
	}

	@Override
	public String getElementId() {
		return vertex.getId();
	}

	@Override
	public Map<String, Object> getProperties() {
		return vertex.getProperties();
	}

	@Override
	public <T> T getProperty(String key) {
		return vertex.getProperty(key);
	}

	@Override
	public Set<String> getPropertyKeys() {
		return vertex.getPropertyKeys();
	}

	@Override
	public void setProperty(String key, Object value) {
		vertex.setProperty(key, value);

	}

	@Override
	public <T> T removeProperty(String key) {
		return vertex.removeProperty(key);
	}

	@Override
	public Time getTime() {
		return time;
	}

	@Override
	public long getStartTime() {
		return time.getStartTime();
	}

	@Override
	public long getFinishTime() {
		return time.getFinishTime();
	}

	@Override
	public long getDuration() {
		return time.getDuration();
	}

	@Override
	public Element getElement() {
		return vertex;
	}

	@Override
	public NavigableSet<EdgeEvent> getEdgeEvents(Direction direction, TemporalRelation tr, String label) {
		NavigableSet<EdgeEvent> neighborEvents = new TreeSet<>(EventComparator.earlyStartTimeShortDuration);
		for (Edge e : this.vertex.getEdges(direction, List.of(label))) {
			NavigableSet<EdgeEvent> events = e.getEvents(time, tr, EventComparator.earlyStartTimeShortDuration);
			if (events.isEmpty())
				continue;
			neighborEvents.add(events.first());
		}
		return neighborEvents;
	}

	@Override
	public NavigableSet<VertexEvent> getVertexEvents(Direction direction, TemporalRelation tr, String label) {
		NavigableSet<VertexEvent> neighborEvents = new TreeSet<>(EventComparator.earlyStartTimeShortDuration);
		for (Vertex v : this.vertex.getVertices(direction, List.of(label))) {
			NavigableSet<VertexEvent> vertexEvents = v.getEvents(time, tr, false, false,
					EventComparator.earlyStartTimeShortDuration);
			if (vertexEvents.isEmpty())
				continue;
			neighborEvents.add(vertexEvents.first());
		}
		return neighborEvents;
	}
}
