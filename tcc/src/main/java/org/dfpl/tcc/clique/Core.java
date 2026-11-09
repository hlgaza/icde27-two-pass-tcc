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
 *
*/
package org.dfpl.tcc.clique;

import org.dfpl.tcc.rgraph.REdge;
import org.dfpl.tcc.rgraph.RGraph;
import org.dfpl.tcc.rgraph.RVertex;

import java.util.*;
import java.util.logging.Logger;

public class Core {
    public static Logger log = Logger.getLogger(Core.class.getName());

    public static <V extends RVertex, E extends REdge<?>> void reduce(RGraph<V, E> rGraph, List<Integer> ids, boolean[] active, int[] degrees, int K) {
        // Count the degrees of the vertices in the subgraph
        for (int id : ids) {
            rGraph.forEachNeighbor(id, vId -> {
                if (active[vId]) degrees[vId]++;
            });
        }

        int[] queue = new int[ids.size()];
        int queueN = 0;
        for (int id : ids) {
            if (degrees[id] < K) {
                queue[queueN++] = id;
                active[id] = false;
            }
        }

        for (int i = 0; i < queueN; i++) {
            int uId = queue[i];

            Iterator<E> edgeIterator = rGraph.edgeIterator(uId);

            while (edgeIterator.hasNext()) {
                E edge = edgeIterator.next();

                int vId = edge.getDest();
                if (!active[vId]) continue;

                if (--degrees[vId] == K - 1) {
                    queue[queueN++] = vId;
                    active[vId] = false;
                }
            }
        }

        int newSize = 0;
        for (int id : ids) {
            if (active[id]) {
                ids.set(newSize++, id);
            }
        }

        ids.subList(newSize, ids.size()).clear();
    }
}
