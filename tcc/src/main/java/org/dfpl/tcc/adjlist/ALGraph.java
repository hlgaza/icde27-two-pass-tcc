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
package org.dfpl.tcc.adjlist;

import org.apache.commons.lang3.NotImplementedException;
import org.dfpl.tcc.rgraph.RGraph;

import java.util.*;
import java.util.function.IntConsumer;

public class ALGraph implements RGraph<ALVertex, ALEdge> {

    private final int[] ranks;
    private final int[] cores;
    private final int[] upperBounds;
    private final int[] colors;

    private final Map<Integer, ALVertex> vertices = new HashMap<>();
    private final Map<Integer, List<ALEdge>> adjList = new HashMap<>();

    public ALGraph(Collection<Integer> sources) {
        int n = sources.size();
        this.ranks = new int[n];
        this.cores = new int[n];
        this.upperBounds = new int[n];
        this.colors = new int[n];

        for (Integer id : sources) {
            addVertex(id);
        }
    }

    @Override
    public void addVertex(int id) {
        vertices.putIfAbsent(id, new ALVertex(id));
        adjList.putIfAbsent(id, new LinkedList<>());
    }

    @Override
    public ALVertex getVertex(int id) {
        return vertices.get(id);
    }

    @Override
    public void removeVertex(int id) {
        // Remove in-going edges
        List<ALEdge> outEdges = adjList.get(id);
        for (ALEdge outEdge : outEdges) {
            List<ALEdge> inEdges = adjList.get(outEdge.getDest());
            boolean isFound = false;
            Iterator<ALEdge> it = inEdges.iterator();
            while (it.hasNext()) {
                ALEdge inEdge = it.next();
                if (inEdge.getDest() == id) {
                    it.remove();
                    isFound = true;
                    break;
                }
            }

            if (isFound) {
                ALVertex inVertex = vertices.get(outEdge.getDest());
                inVertex.setNeighborCount(inVertex.getNeighborCount() - 1);
            }
        }

        // Remove out-going edges
        adjList.get(id).clear();

        // Remove vertex
        vertices.get(id).setNeighborCount(0);
    }

    @Override
    public List<ALVertex> getVertices() {
        return vertices.values().stream().toList();
    }

    @Override
    public void addEdge(int src, int dst, long val) {
        // Add the vertices if they don't exist
        if (src != dst) {
            addVertex(src);
            addVertex(dst);
        }

        addDirectedEdge(src, dst, (int) val);
    }

    public void addEdgeNoCheck(int src, int dst, long val) {
        // Add the vertices if they don't exist
        if (src != dst) {
            addVertex(src);
            addVertex(dst);
        }

        addDirectedEdgeNoCheck(src, dst, (int) val);
    }

    @Override
    public void updateEdge(int src, int dst, long val) {
        if (!vertices.containsKey(src)) addVertex(src);

        List<ALEdge> edges = adjList.get(src);
        for (ALEdge edge : edges) {
            if (edge.getDest() == dst) {
                edge.setValue(val);
                return;
            }
        }
    }

    @Override
    public ALEdge getEdge(long idx) {
//        return null;
        throw new NotImplementedException();
    }

    @Override
    public ALEdge getEdge(int src, int dst) {
        List<ALEdge> edges = adjList.get(src);

        if(edges == null || edges.isEmpty())
            return null;

        for (ALEdge edge : edges) {
            if (edge.getDest() == dst) return edge;
        }
        return null;
    }

    @Override
    public void addEdges(int src, Collection<ALEdge> newEdges) {
        List<ALEdge> existingEdges = adjList.get(src);
        if(existingEdges == null)
            addVertex(src);

        for(ALEdge edge : newEdges){
            addEdge(src, edge.getDest(), edge.getValue());
        }
    }

    @Override
    public List<ALEdge> getEdges(int id) {
        return adjList.getOrDefault(id, List.of());
    }

    @Override
    public void removeEdge(int src, int dst) {
        List<ALEdge> edges = adjList.get(src);
        if(edges == null)
            return;

        edges.removeIf(edge -> edge.getDest() == dst);

        ALVertex srcVertex = vertices.get(src);
        srcVertex.setNeighborCount(srcVertex.getNeighborCount() - 1);
    }

    @Override
    public long getEdgeCount() {
        return vertices.values().stream().mapToInt(ALVertex::getNeighborCount).sum();
    }

    @Override
    public int getCore(int id) {
        return cores[id];
    }

    @Override
    public void setCore(int id, int core) {
        this.cores[id] = core;
    }

    @Override
    public int getUpperBound(int id) {
        return upperBounds[id];
    }

    @Override
    public int getRank(int id) {
        return ranks[id];
    }

    @Override
    public int[] getColors() {
        return colors;
    }

    @Override
    public void setRank(int id, int rank) {
        this.ranks[id] = rank;
    }

    @Override
    public void setUpperBound(int id, int val) {
        this.upperBounds[id] = val;
    }

    @Override
    public Set<Integer> getNeighbors(int id) {
        return new HashSet<>(adjList.get(id).stream().map(ALEdge::getDest).toList());
    }

    @Override
    public void forEachNeighbor(int id, IntConsumer consumer) {
        List<ALEdge> edges = adjList.getOrDefault(id, List.of());
        edges.forEach(edge -> consumer.accept(edge.getDest()));
    }

    @Override
    public Iterator<ALEdge> edgeIterator(int id) {
        return adjList.get(id).iterator();
    }

    @Override
    public double getDensity() {
        return getEdgeCount() / ((double) getVertices().size() * getVertices().size() - 1);
    }

    /**
     * Adds a directed edge to the graph.
     *
     * @param src the source vertex id
     * @param dst the destination vertex id
     * @param val the edge value
     */
    private void addDirectedEdge(int src, int dst, long val) {
        ALEdge outEdge = null;
        for (ALEdge edge : adjList.get(src)) {
            if (edge.getDest() == dst) {
                edge.setValue(val);
                outEdge = edge;
                break;
            }
        }
        if (outEdge == null) {
            adjList.get(src).add(new ALEdge(dst, val));
            // Increase the neighbor count
            ALVertex sourceVertex = this.getVertex(src);
            sourceVertex.setNeighborCount(sourceVertex.getNeighborCount() + 1);
        }
    }

    /**
     * Adds a directed edge to the graph.
     *
     * @param src the source vertex id
     * @param dst the destination vertex id
     * @param val the edge value
     */
    private void addDirectedEdgeNoCheck(int src, int dst, long val) {
        adjList.get(src).add(new ALEdge(dst, val));
        // Increase the neighbor count
        ALVertex sourceVertex = this.getVertex(src);
        sourceVertex.setNeighborCount(sourceVertex.getNeighborCount() + 1);
    }

    public void debugPrint() {
        for (ALVertex v : vertices.values()) {
            System.out.println(v);
            System.out.println(adjList.get(v.getId()));
        }
    }
}
