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
import com.tinkerpop.blueprints.Edge;
import org.dfpl.chronograph.*;
import org.dfpl.chronograph.ChronoGraph;
import org.dfpl.chronograph.ChronoVertex;
import org.dfpl.tcc.tr.SmallGammaLongTR;

import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

public class TraversalTBFS {
    private static Logger log = Logger.getLogger(TraversalTBFS.class.getName());

    private record Element(int id, long time) {
    }

    private ChronoVertex[] vertexCache;

    private final SmallGammaLongTR gamma;

    public TraversalTBFS(SmallGammaLongTR gamma) {
        this.gamma = gamma;
        this.vertexCache = new ChronoVertex[gamma.size()];
    }

    public TraversalTBFS(SmallGammaLongTR gamma, ChronoVertex[] vertexCache) {
        this.gamma = gamma;
        this.vertexCache = vertexCache;
    }


    public SmallGammaLongTR traverseForwardInstant(ChronoGraph g, int sourceId, Long sourceTime, BitSet included) {
        Queue<Element> queue = new LinkedList<>();
        gamma.put(sourceId, sourceTime);

        queue.offer(new Element(sourceId, sourceTime));
        while (!queue.isEmpty() && included.cardinality() != 0) {
            Element curr = queue.poll();

            Long currXTime = gamma.get(curr.id);
            if (currXTime < curr.time)
                continue;

            if (vertexCache[curr.id] == null)
                vertexCache[curr.id] = ((ChronoVertex) g.getVertex(curr.id));
            Collection<Edge> xEdges = vertexCache[curr.id].getEdges(Direction.OUT);
            for (Edge xEdge : xEdges) {
                int jId = ((ChronoVertex) xEdge.getVertex(Direction.IN)).getIntId();

                Long jTime = gamma.get(jId);
                if (jTime != SmallGammaLongTR.NULL_TIME && jTime <= curr.time)
                    continue;

                EdgeEvent edgeEvent = xEdge.getEvent(new TimeInstant(curr.time), TemporalRelation.isAfter);
                if (edgeEvent == null) continue;

                if (jTime == SmallGammaLongTR.NULL_TIME || jTime > edgeEvent.getFinishTime()) {
                    gamma.put(jId, edgeEvent.getFinishTime());
                    queue.offer(new Element(jId, edgeEvent.getFinishTime()));
                    included.clear(jId);
                }
            }
        }

        return gamma;
    }

    public SmallGammaLongTR traverseBackwardInstant(ChronoGraph g, int sourceId, Long sourceTime, BitSet included) {
        Queue<Element> queue = new LinkedList<>();
        gamma.put(sourceId, sourceTime);

        queue.offer(new Element(sourceId, sourceTime));
        while (!queue.isEmpty() && included.cardinality() != 0) {
            Element curr = queue.poll();

            Long currXTime = gamma.get(curr.id);
            if (currXTime > curr.time)
                continue;

            if (vertexCache[curr.id] == null)
                vertexCache[curr.id] = ((ChronoVertex) g.getVertex(curr.id));
            Collection<Edge> xEdges = vertexCache[curr.id].getEdges(Direction.IN);
            for (Edge xEdge : xEdges) {
                int jId = ((ChronoVertex) xEdge.getVertex(Direction.OUT)).getIntId();
                Long jTime = gamma.get(jId);

                if (jTime != SmallGammaLongTR.NULL_TIME && jTime >= curr.time)
                    continue;

                EdgeEvent edgeEvent = xEdge.getEvent(new TimeInstant(curr.time), TemporalRelation.isBefore);
                if (edgeEvent == null) continue;

                if (jTime == SmallGammaLongTR.NULL_TIME || jTime < edgeEvent.getStartTime()) {
                    gamma.put(jId, edgeEvent.getStartTime());
                    queue.offer(new Element(jId, edgeEvent.getStartTime()));
                    included.clear(jId);
                }
            }
        }

        return gamma;
    }

    public SmallGammaLongTR traverseForwardPeriod(ChronoGraph g, int sourceId, Long sourceTime, BitSet included) {
        Queue<Element> queue = new LinkedList<>();
        gamma.put(sourceId, sourceTime);

        queue.offer(new Element(sourceId, sourceTime));
        while (!queue.isEmpty() && included.cardinality() != 0) {
            Element curr = queue.poll();

            Long currXTime = gamma.get(curr.id);
            if (currXTime < curr.time)
                continue;

            if (vertexCache[curr.id] == null)
                vertexCache[curr.id] = ((ChronoVertex) g.getVertex(curr.id));
            Collection<Edge> xEdges = vertexCache[curr.id].getEdges(Direction.OUT);
            for (Edge xEdge : xEdges) {
                int jId = ((ChronoVertex) xEdge.getVertex(Direction.IN)).getIntId();

                Long jTime = gamma.get(jId);
                if (jTime != SmallGammaLongTR.NULL_TIME && jTime <= curr.time)
                    continue;

                EdgeEvent edgeEvent = xEdge.getEvent(new TimeInstant(curr.time), TemporalRelation.isAfter);
                if (edgeEvent == null) continue;

                if (edgeEvent.getFinishTime() != curr.time && (jTime == SmallGammaLongTR.NULL_TIME || jTime > edgeEvent.getFinishTime())) {
                    gamma.put(jId, edgeEvent.getFinishTime());
                    queue.offer(new Element(jId, edgeEvent.getFinishTime()));
                    included.clear(jId);
                }
            }
        }

        return gamma;
    }

    public SmallGammaLongTR traverseBackwardPeriod(ChronoGraph g, int sourceId, Long sourceTime, BitSet included) {
        Queue<Element> queue = new LinkedList<>();
        gamma.put(sourceId, sourceTime);

        queue.offer(new Element(sourceId, sourceTime));
        while (!queue.isEmpty() && included.cardinality() != 0) {
            Element curr = queue.poll();

            Long currXTime = gamma.get(curr.id);
            if (currXTime > curr.time)
                continue;

            if (vertexCache[curr.id] == null)
                vertexCache[curr.id] = ((ChronoVertex) g.getVertex(curr.id));
            Collection<Edge> xEdges = vertexCache[curr.id].getEdges(Direction.IN);
            for (Edge xEdge : xEdges) {
                int jId = ((ChronoVertex) xEdge.getVertex(Direction.OUT)).getIntId();
                Long jTime = gamma.get(jId);

                if (jTime != SmallGammaLongTR.NULL_TIME && jTime >= curr.time)
                    continue;

                EdgeEvent edgeEvent = xEdge.getEvent(new TimeInstant(curr.time), TemporalRelation.isBefore);
                if (edgeEvent == null) continue;

                if (edgeEvent.getStartTime() != curr.time && (jTime == SmallGammaLongTR.NULL_TIME || jTime < edgeEvent.getStartTime())) {
                    gamma.put(jId, edgeEvent.getStartTime());
                    queue.offer(new Element(jId, edgeEvent.getStartTime()));
                    included.clear(jId);
                }
            }
        }

        return gamma;
    }
}
