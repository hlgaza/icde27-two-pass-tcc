/**
 * GPL-3.0 License
 * <p>
 * Copyright (C) 2026 Haifa Gaza
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.dfpl.tcc.tr.algos;

import com.tinkerpop.blueprints.Direction;
import org.dfpl.chronograph.EdgeEvent;
import org.dfpl.chronograph.Time;
import org.dfpl.tcc.adjlist.ALEdge;
import org.dfpl.tcc.adjlist.ALGraph;
import org.dfpl.tcc.adjlist.ALVertex;
import org.dfpl.chronograph.ChronoGraph;
import org.dfpl.chronograph.ChronoVertex;
import org.dfpl.utils.MutableInteger;
import org.dfpl.tcc.rgraph.RGraph;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReversedTimeScanAL {
    private static final Logger log = Logger.getLogger(ReversedTimeScanAL.class.getName());

    /**
     * Computes the reversed-time scan given time intervals
     * <a href="https://doi.org/10.1103/PhysRevE.84.016105">Reference</a>
     */
    public static RGraph<ALVertex, ALEdge> computePeriod(ChronoGraph g, Collection<Integer> sourceIds) {

        RGraph<ALVertex, ALEdge> phiGraph = new ALGraph(sourceIds); // Latest time of contact, holds the reachability information
        RGraph<ALVertex, ALEdge> psiGraph = new ALGraph(sourceIds); // Path's starting time

        NavigableMap<Time, List<EdgeEvent>> eventsByTime = g.getEdgeEvents().stream().distinct()
                .collect(Collectors.groupingBy(EdgeEvent::getTime, TreeMap::new, Collectors.toList()));

        eventsByTime.descendingMap().forEach((time, events) -> {
            boolean hasChanged = true;
            long startTime = time.getStartTime();
            long endTime = time.getFinishTime();

            while (hasChanged) {
                hasChanged = false;
                BitSet kIds = new BitSet(g.getVertices().size());
                for (EdgeEvent edgeEvent : events) {
                    int iId = ((ChronoVertex) edgeEvent.getVertex(Direction.OUT)).getIntId();
                    int jId = ((ChronoVertex) edgeEvent.getVertex(Direction.IN)).getIntId();

                    long prevEdgeCount = phiGraph.getVertex(iId).getNeighborCount();

                    ALEdge psiEdgeOfIToJ = psiGraph.getEdge(iId, jId);
                    if (psiEdgeOfIToJ == null)
                        psiGraph.addEdge(iId, jId, startTime);
                    else
                        psiEdgeOfIToJ.setValue(startTime);

                    ALEdge phiEdgeOfIToJ = phiGraph.getEdge(iId, jId);
                    if (phiEdgeOfIToJ == null)
                        phiGraph.addEdge(iId, jId, endTime);
                    else
                        phiGraph.updateEdge(iId, jId, endTime);

                    // Compute the reachable nodes from vertices i and j
                    Iterator<ALEdge> edgeIterator = phiGraph.edgeIterator(iId);
                    while (edgeIterator.hasNext()) {
                        ALEdge edge = edgeIterator.next();
                        kIds.set(edge.getDest());
                    }

                    edgeIterator = phiGraph.edgeIterator(jId);
                    while (edgeIterator.hasNext()) {
                        ALEdge edge = edgeIterator.next();
                        kIds.set(edge.getDest());
                    }

                    // Update the reachability information
                    for (int kId = kIds.nextSetBit(0); kId >= 0; kId = kIds.nextSetBit(kId + 1)) {
                        long psiValueOfJToK = Long.MAX_VALUE;
                        ALEdge psiEdgeOfJToK = psiGraph.getEdge(jId, kId);
                        if (psiEdgeOfJToK != null)
                            psiValueOfJToK = psiEdgeOfJToK.getValue();

                        if (psiValueOfJToK - endTime >= 0) {
                            long phiValueOfIToK = Long.MAX_VALUE;
                            ALEdge phiEdgeOfIToK = psiGraph.getEdge(iId, kId);
                            if (phiEdgeOfIToK != null)
                                phiValueOfIToK = phiEdgeOfIToK.getValue();

                            ALEdge psiEdgeOfIToK = psiGraph.getEdge(iId, kId);

                            long phiValueOfJToK = Long.MAX_VALUE;
                            ALEdge phiEdgeOfJToK = phiGraph.getEdge(jId, kId);
                            if (phiEdgeOfJToK != null)
                                phiValueOfJToK = phiEdgeOfJToK.getValue();

                            if (phiValueOfIToK > phiValueOfJToK) {
                                phiGraph.addEdge(iId, kId, phiValueOfJToK);

                                if (psiEdgeOfIToK == null)
                                    psiGraph.addEdge(iId, kId, startTime);
                                else
                                    psiEdgeOfIToK.setValue(startTime);
                            }
                        }
                    }

                    long newEdgeCount = phiGraph.getVertex(iId).getNeighborCount();
                    if (prevEdgeCount != newEdgeCount)
                        hasChanged = true;

                    kIds.clear();
                }
            }
        });


        return phiGraph;
    }

    /**
     * Computes the reversed-time scan with fixed-time iteration
     *
     * @param g         the temporal graph
     * @param sourceIds the sources to compute
     * @return the adjacency-list-based reachability graph
     */
    public static RGraph<ALVertex, ALEdge> computeInstant(ChronoGraph g, Collection<Integer> sourceIds) {
        RGraph<ALVertex, ALEdge> rGraph = new ALGraph(sourceIds);

        // Initialize values
        for (int id : sourceIds) {
            rGraph.addEdge(id, id, 1);
        }

        MutableInteger edgeEventsProcessed = new MutableInteger(0);

        NavigableMap<Time, List<EdgeEvent>> eventsByTime = g.getEdgeEvents().stream().distinct()
                .collect(Collectors.groupingBy(EdgeEvent::getTime, TreeMap::new, Collectors.toList()));

        eventsByTime.descendingMap().forEach((_, edgeEvents) -> {
            boolean hasChanged = true;
            while (hasChanged) {
                hasChanged = false;
                for (EdgeEvent edgeEvent : edgeEvents) {
                    ChronoVertex outV = (ChronoVertex) edgeEvent.getVertex(Direction.OUT);
                    ChronoVertex inV = (ChronoVertex) edgeEvent.getVertex(Direction.IN);

                    Collection<ALEdge> rEdges = rGraph.getEdges(inV.getIntId());
                    int prevEdgeCount = rGraph.getVertex(outV.getIntId()).getNeighborCount();

                    rGraph.addEdges(outV.getIntId(), rEdges);
                    int newEdgeCount = rGraph.getVertex(outV.getIntId()).getNeighborCount();

                    if (newEdgeCount == prevEdgeCount)
                        continue;

                    hasChanged = true;
                }
            }
            if (edgeEventsProcessed.addAndGet(edgeEvents.size()) % 1000 == 0) {
                log.info(() -> String.format("%d edge events processed...", edgeEventsProcessed.get()));
            }
        });

        return rGraph;
    }
}
