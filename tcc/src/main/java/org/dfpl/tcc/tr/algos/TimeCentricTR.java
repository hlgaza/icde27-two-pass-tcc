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
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.dfpl.chronograph.*;
import org.dfpl.chronograph.ChronoVertex;
import org.dfpl.tcc.tr.BigGammaTR;
import org.dfpl.tcc.tr.SmallGammaLongTR;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The time-centric computation for temporal reachability
 * <a href="https://doi.org/10.1109/TKDE.2020.3005672">Reference</a>
 */
public class TimeCentricTR {
    private static final Logger log = Logger.getLogger(TimeCentricTR.class.getName());
    private final BigGammaTR bigGamma;
    private final Graph graph;
    private long computationTime;
    Logger logger;

    /**
     * Constructs time-centric temporal reachability algorithm
     *
     * @param graph    the graph to traverse
     * @param bigGamma the big gamma to update
     */
    public TimeCentricTR(Graph graph, BigGammaTR bigGamma) {
        this.bigGamma = bigGamma;
        this.graph = graph;
        this.logger = Logger.getLogger(TimeCentricTR.class.getName());
    }

    /**
     * Returns the computation time of this implementation.
     *
     * @return the computation time
     */
    public long getComputationTime() {
        return computationTime;
    }

    /**
     * Computes temporal reachability using time-centric computation with fixed-time point iteration.
     * It considers edge events with a temporal relation to specified base time.
     *
     * @param sources the sources to compute
     */
    public void computeInstant(Set<Vertex> sources) {
        long pre = System.currentTimeMillis();

        NavigableMap<Time, List<EdgeEvent>> eventsByTime = graph.getEdgeEvents().stream().distinct()
                .collect(Collectors.groupingBy(EdgeEvent::getTime, TreeMap::new, Collectors.toList()));

        eventsByTime.forEach((time, edgeEvents) -> {
            boolean hasChanged = true;
            int prevCount;
            int newCount;

            while (hasChanged) {
                hasChanged = false;
                for (EdgeEvent edgeEvent : edgeEvents) {
                    ChronoVertex outVertex = (ChronoVertex) edgeEvent.getVertex(Direction.OUT);
                    ChronoVertex inVertex = (ChronoVertex) edgeEvent.getVertex(Direction.IN);

                    for (Vertex source : sources) {
                        if (inVertex == source) continue;

                        ChronoVertex chronoSource = (ChronoVertex) source;

                        Long reachableTime = bigGamma.get(chronoSource.getId(), outVertex.getId());
                        if (reachableTime != SmallGammaLongTR.NULL_TIME && reachableTime.compareTo(time.getFinishTime()) <= 0) {
                            prevCount = bigGamma.getGamma(chronoSource.getIntId()).getNeighborCount();

                            Long inTime = bigGamma.get(chronoSource.getId(), inVertex.getId());
                            if (inTime == SmallGammaLongTR.NULL_TIME || time.getFinishTime() < inTime )
                                bigGamma.set(chronoSource.getIntId(), inVertex.getIntId(), time.getFinishTime());
                            newCount = bigGamma.getGamma(chronoSource.getIntId()).getNeighborCount();

                            if (newCount > prevCount) {
                                hasChanged = true;
                            }
                        }
                    }
                }
            }
        });

        computationTime = System.currentTimeMillis() - pre;
    }

    public void computePeriod(Set<Vertex> sources) {
        NavigableMap<Time, List<EdgeEvent>> eventsByTime = graph.getEdgeEvents().stream().distinct()
                .collect(Collectors.groupingBy(EdgeEvent::getTime, TreeMap::new, Collectors.toList()));

        eventsByTime.forEach((_, events) -> {
            boolean hasChanged = true;
            while (hasChanged) {
                hasChanged = false;
                for (EdgeEvent edgeEvent : events) {
                    ChronoVertex outVertex = (ChronoVertex) edgeEvent.getVertex(Direction.OUT);
                    ChronoVertex inVertex = (ChronoVertex) edgeEvent.getVertex(Direction.IN);
                    long departureTime = edgeEvent.getStartTime();
                    long arrivalTime = edgeEvent.getFinishTime();

                    for (Vertex source : sources) {
                        if (inVertex == source) continue;

                        ChronoVertex chronoSource = (ChronoVertex) source;
                        SmallGammaLongTR sourceGamma = bigGamma.getGamma(chronoSource.getIntId());

                        long arrivalTimeOfOut = Long.MAX_VALUE;
                        if (sourceGamma.get(outVertex.getIntId()) != null) {
                            arrivalTimeOfOut = sourceGamma.get(outVertex.getIntId());
                        } else { // The out-vertex is not reachable
                            continue;
                        }

                        long arrivalTimeOfIn = Long.MAX_VALUE;
                        if (sourceGamma.get(inVertex.getIntId()) != null) {
                            arrivalTimeOfIn = sourceGamma.get(inVertex.getIntId());
                        }

                        if (departureTime >= arrivalTimeOfOut && arrivalTime < arrivalTimeOfIn) {
                            sourceGamma.put(inVertex.getIntId(),arrivalTime);
                            hasChanged = true;
                        }
                    }
                }
            }
        });
    }
}
