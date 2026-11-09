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

import java.util.Map;
import java.util.Set;

/**
 * The in-memory implementation of temporal graph database.
 *
 * @author Jaewook Byun, Ph.D., Assistant Professor, DFPL, Department of
 *         Software, Sejong University
 *
 * @author Haifa Gaza, Master Student, DFPL, Sejong University
 * 			<br/>
 *         Byun, Jaewook, Sungpil Woo, and Daeyoung Kim. "Chronograph: Enabling
 *         temporal graph traversals for efficient information diffusion
 *         analysis over time." IEEE Transactions on Knowledge and Data
 *         Engineering 32.3 (2019): 424-437.
 * 			<br/>
 *         Byun, Jaewook. "Enabling time-centric computation for efficient
 *         temporal graph traversals from multiple sources." IEEE Transactions
 *         on Knowledge and Data Engineering (2020). *
 * 
 */
public class ChronoEdgeEvent implements EdgeEvent, Comparable<ChronoEdgeEvent> {
	private final Edge edge;
	private final Time time;

	public ChronoEdgeEvent(Edge e, Time time) {
		this.edge = e;
		this.time = time;
	}

	@Override
	public VertexEvent getVertexEvent(Direction direction) {
		return edge.getVertex(direction).getEvent(time);
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
	public int compareTo(ChronoEdgeEvent event) {
		int elementComparison = this.getElement().getId().compareTo(event.getElement().getId());

		if (elementComparison != 0)
			return elementComparison;

		return this.getTime().compareTo(event.getTime());
	}

	@Override
	public Time getTime() {
		return time;
	}

	@Override
	public Element getElement() {
		return edge;
	}

	@Override
	public String getId() {
		return edge.getId() + "_" + time;
	}

	@Override
	public Map<String, Object> getProperties() {
		return edge.getProperties();
	}

	@Override
	public <T> T getProperty(String key) {
		return edge.getProperty(key);
	}

	@Override
	public Set<String> getPropertyKeys() {
		return edge.getPropertyKeys();
	}

	@Override
	public void setProperty(String key, Object value) {
		edge.setProperty(key, value);
	}

	@Override
	public <T> T removeProperty(String key) {
		return edge.removeProperty(key);
	}

	@Override
	public Vertex getVertex(Direction direction) {
		return edge.getVertex(direction);
	}

	@Override
	public String getLabel() {
		return edge.getLabel();
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
	public String getElementId() {
		return edge.getId();
	}

	@Override
	public long getDuration() {
		return time.getDuration();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EdgeEvent))
			return false;
		return this.edge.equals(((Event) obj).getElement()) && this.getTime().equals(((Event) obj).getTime());
	}

	@Override
	public String toString() {
		return "ChronoEdgeEvent{" + "edge=" + edge + ", time=" + time + '}';
	}
}
