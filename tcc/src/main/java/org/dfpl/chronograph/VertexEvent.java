/**
 * Apache 2.0 License
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

import java.util.Collection;

public interface VertexEvent extends Event {
	
	/**
	 * Return chronologically closest neighbor edge events per a pair (out vertex,
	 * label, in vertex)
	 * 
	 * @param direction the direction of the element from this vertex
	 * @param tr the temporal relation to match with this vertex event's time
	 * @param label the label of the edge
	 * @return Set<EdgeEvent>
	 */
    Collection<EdgeEvent> getEdgeEvents(Direction direction, TemporalRelation tr, String label);

	/**
	 * Return chronologically closest neighbor vertex events per a pair (out vertex,
	 * label, in vertex)
	 * 
	 * @param direction the direction of the element from this vertex
	 * @param tr the temporal relation to match with this vertex event's time
	 * @param label the label of the edge
	 * @return Set<VertexEvent>
	 */
    Collection<VertexEvent> getVertexEvents(Direction direction, TemporalRelation tr, String label);

}
