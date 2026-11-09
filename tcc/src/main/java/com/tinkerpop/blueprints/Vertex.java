/**
 * Portions Copyright (c) Jaewook Byun
 * Modified interface
 * <p>
 * BSD 3-Clause License
 * Copyright (c) 2009-Infinity, TinkerPop
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the TinkerPop nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TINKERPOP BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tinkerpop.blueprints;

import org.dfpl.chronograph.TemporalRelation;
import org.dfpl.chronograph.Time;
import org.dfpl.chronograph.Tokens.EventComparator;
import org.dfpl.chronograph.VertexEvent;

import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;

/**
 * A vertex maintains pointers to both a set of incoming and outgoing edges. The
 * outgoing edges are those edges for which the vertex is the tail. The incoming
 * edges are those edges for which the vertex is the head. Diagrammatically,
 * ---inEdges---&gt; vertex ---outEdges---&gt;.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Jaewook Byun, Ph.D., Assistant Professor, Department of Software,
 *         Sejong University (slightly modify interface)
 */
public interface Vertex extends Element {

	/**
	 * Return the edges incident to the vertex according to the provided direction
	 * and edge labels. The resulting collection would have multiple e(i|j) with
	 * multiple labels. labels of candidate edges should be included in labels if
	 * labels are empty, all the candidate edges should be returned.
	 *
	 * @param direction the direction of the edges to retrieve
	 * @param labels    the labels of the edges to retrieve
	 * @return a collection of incident edges
	 */
    Collection<Edge> getEdges(Direction direction, List<String> labels);

	/**
	 * Return the vertices adjacent to the vertex according to the provided
	 * direction and edge labels. The resulting collection does not have redundancy
	 * <p>
	 * This method would remove duplicate vertices according to the definition of
	 * Vertex.getEdges
	 *
	 * @param direction the direction of the edges of the adjacent vertices
	 * @param labels    the labels of the edges of the adjacent vertices
	 * @return a collection of adjacent vertices
	 */
    Collection<Vertex> getVertices(Direction direction, List<String> labels);

	/**
	 * Add a new outgoing edge from this vertex to the parameter vertex with
	 * provided edge label.
	 *
	 * @param label    the label of the edge
	 * @param inVertex the vertex to connect to with an incoming edge
	 * @return the newly created edge
	 */
    Edge addEdge(String label, Vertex inVertex);

	/**
	 * Remove the element from the graph.
	 */
    void remove();

	/**
	 * Explicitly add a vertex event of this graph element valid at time.
	 * 
	 * @param time TimeInstant or TimePeriod
	 * @return the created vertex event
	 */
    VertexEvent addEvent(Time time);

	/**
	 * Return a vertex event of this graph element valid at time.
	 * 
	 * @param time TimeInstant or TimePeriod
	 * @return VertexEvent valid at time
	 */
    VertexEvent getEvent(Time time);

	/**
	 * Return events of this element that are matched with tr for time. In addition
	 * to getEvents(time, tr), the method includes in-going vertex event for
	 * out-going edge events if aware out events and out-going vertex event for
	 * in-going edge events if aware in events.
	 * 
	 * 
	 * @param time           the time to check
	 * @param tr             the temporal relation to match with time
	 * @param awareOutEvents include in-going vertex events for out-going edge
	 *                       events
	 * @param awareInEvents  include out-going vertex events for in-going edge
	 *                       events
	 * @return NavigableSet of VertexEvent or EdgeEvent
	 */
    NavigableSet<VertexEvent> getEvents(Time time, TemporalRelation tr, boolean awareOutEvents,
                                        boolean awareInEvents, EventComparator eventComparator);
	
	/**
	 * Remove all the events that are matched with tr for time
	 * 
	 * @param time the time to check
	 * @param tr   the temporal relation to match with time
	 */
    void removeEvents(Time time, TemporalRelation tr);

}
