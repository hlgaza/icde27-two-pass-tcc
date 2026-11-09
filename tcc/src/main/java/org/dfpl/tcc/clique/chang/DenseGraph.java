/**
 * Portions Copyright (c) 2026 Haifa Gaza <br>
 * Modified and translated from C++ to Java
 * <p>
 * Original Work:
 * Copyright (C) 2019 Lijun Chang [<a href="https://github.com/LijunChang/MC-BRB">GitHub</a>] <br>
 * Licensed under GPL-3.0 License (see below)
 * <p>
 * GPL-3.0 License
 * <p>
 * Copyright (C) 2019 Lijun Chang
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
package org.dfpl.tcc.clique.chang;

import org.dfpl.tcc.rgraph.REdge;
import org.dfpl.tcc.rgraph.RGraph;
import org.dfpl.tcc.rgraph.RVertex;

import java.util.*;
import java.util.logging.Logger;

public class DenseGraph {

    int[] newIdToOldIdMapper;
    HashMap<Integer, Integer> oldIdToNewIdMapper;

    BitSet[] graph;

    ArrayList<Integer> ids;
    public static Logger log = Logger.getLogger(DenseGraph.class.getName());

    private int maxNewId = 0;

    DenseGraph(List<Integer> originalIds) {
        this.ids = new ArrayList<>(originalIds.size());
        for (int i = 0; i < originalIds.size(); i++) this.ids.add(i);

        // Convert the IDs
        newIdToOldIdMapper = new int[ids.size()];
        oldIdToNewIdMapper = new HashMap<>(ids.size());

        int newId = 0;
        for (Integer id : originalIds) {
            oldIdToNewIdMapper.put(id, newId);
            newIdToOldIdMapper[newId++] = id;
            if(newId > maxNewId) maxNewId = newId;
        }

        // Initialize adjacency row per vertex
        graph = new BitSet[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            graph[i] = new BitSet(maxNewId);
        }
    }

    public int getMaxNewId() {
        return maxNewId;
    }

    public <V extends RVertex, E extends REdge<?>> void convert(RGraph<V, E> rGraph) {
        for (int newIId = 0; newIId < ids.size(); newIId++) {
            int ogIId = newIdToOldIdMapper[newIId];

            Iterator<E> edgeIterator = rGraph.edgeIterator(ogIId);
            while (edgeIterator.hasNext()) {
                E edge = edgeIterator.next();
                int vId = edge.getDest();

                if (!oldIdToNewIdMapper.containsKey(vId)) continue;

                int newVId = oldIdToNewIdMapper.get(vId);
                if (newIId < newVId) {
                    graph[newIId].set(newVId);
                    graph[newVId].set(newIId);
                }
            }
            graph[newIId].set(newIId);
        }
    }

    public BitSet[] getGraph() {
        return graph;
    }

    public int getSize() {
        return graph.length;
    }

    public boolean isAdjacent(int uId, int vId) {
        return graph[uId].get(vId);
    }

    public void flip(int uId, int vId) {
        graph[uId].flip(vId);
    }

    public ArrayList<Integer> getIdList() {
        return ids;
    }

    public BitSet getNeighbors(int id) {
        return graph[id];
    }


    public int getOldId(int id) {
        return newIdToOldIdMapper[id];
    }

    public int getNewId(int id) {
        boolean test = oldIdToNewIdMapper.get(id) == null;
        if (test)
            return -1;
        return oldIdToNewIdMapper.get(id);
    }

    public int[] getNewIdToOldIdMapper() {
        return newIdToOldIdMapper;
    }

    public int getId(int idx) {
        return ids.get(idx);
    }

    public HashMap<Integer, Integer> getOldIdToNewIdMapper() {
        return oldIdToNewIdMapper;
    }
}
