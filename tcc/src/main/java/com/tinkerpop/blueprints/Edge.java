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

import org.dfpl.chronograph.EdgeEvent;
import org.dfpl.chronograph.Event;
import org.dfpl.chronograph.TemporalRelation;
import org.dfpl.chronograph.Time;

import java.util.Collection;
import java.util.Comparator;
import java.util.NavigableSet;

/**
 * An Edge links two vertices. Along with its key/value properties, an edge has
 * both a directionality and a label. The directionality determines which vertex
 * is the tail vertex (out vertex) and which vertex is the head vertex (in
 * vertex). The edge label determines the type of relationship that exists
 * between the two vertices. Diagrammatically, outVertex ---label---&gt;
 * inVertex.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Jaewook Byun, Ph.D., Assistant Professor, Department of Software,
 *         Sejong University (slightly modify interface)
 */
public interface Edge extends Element {

	/**
	 * Return the tail/out or head/in vertex.
	 *
	 * @param direction whether to return the tail/out or head/in vertex
	 * @return the tail/out or head/in vertex
	 * @throws IllegalArgumentException is thrown if a direction of both is provided
	 */
    Vertex getVertex(Direction direction) throws IllegalArgumentException;

	/**
	 * Return the label associated with the edge.
	 *
	 * @return the edge label
	 */
    String getLabel();

	/**
	 * Remove the element from the graph.
	 */
    void remove();

	/**
	 * Add an event valid at time. The caller (Element) keeps distinct events
	 * regarding their valid time.
	 * <p>
	 * If time is an instance of TimeInstant and a time instant t is equal to
	 * existing time instant or is in a range of existing time-period, the method
	 * fails and return null.
	 * <p>
	 * If time is an instance of TimeInstant and a time instant t is not equal to
	 * any existing time instant or is not in a range of any existing time-period,
	 * return a newly created event.
	 * <p>
	 * If time is an instance of TimePeriod, the method may return a newly created
	 * event. If the time-period p covers any time-instants, the instants are merged
	 * to p. If the time-period p is overlapped with other time-periods, p extends.
	 * If the time-period p is exactly equal to an existing time-period, the method
	 * fails and returns null.
	 * 
	 * @param time the new time to be added
	 * @return VertexEvent or EdgeEvent
	 */
    EdgeEvent addEvent(Time time);

	/**
	 * Return an event of this graph element valid at time
	 * 
	 * @param time the time to match
	 * @return EdgeEvent
	 */
    EdgeEvent getEvent(Time time);

	/**
	 * Return events of this element that are matched with tr for time
	 *
	 * @param time the time to check in the events
	 * @param temporalRelation the temporal relation of time
	 * @return NavigableSet of VertexEvent or EdgeEvent
	 */
    NavigableSet<EdgeEvent> getEvents(Time time, TemporalRelation temporalRelation,
                                      Comparator<Event> eventComparator);
	
	/**
	 * 
	 * @return events of this element
	 */
    Collection<EdgeEvent> getEvents();
	
	/**
	 * Return the event of this element that is matched with tr for time
	 *
	 * @param time the time to check in the events
	 * @return EdgeEvent
	 */
    EdgeEvent getEvent(Time time, TemporalRelation temporalRelation);

	/**
	 * Remove all the events that are matched with temporalRelation for time
	 * 
	 * @param time the time to check
	 * @param temporalRelation the temporal relation to match
	 */
    void removeEvents(Time time, TemporalRelation temporalRelation);
}
