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
import com.tinkerpop.blueprints.Vertex;

public interface EdgeEvent extends Event {

	/**
	 * Get a vertex event
	 * 
	 * @param direction the direction of the event
	 * @return the vertex event
	 */
    VertexEvent getVertexEvent(Direction direction);

	Vertex getVertex(Direction direction);

	String getLabel();
}
