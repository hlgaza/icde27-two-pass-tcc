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
package org.dfpl.tcc.adjset;

import org.apache.commons.lang3.NotImplementedException;
import org.dfpl.tcc.rgraph.RGraph;

import java.util.*;
import java.util.function.IntConsumer;

public class ASGraph implements RGraph<ASVertex, ASEdge> {

    private final int[] ranks;
    private final int[] cores;
    private final int[] upperBounds;
    private final int[] colors;

    private final Map<Integer, ASVertex> vertices = new HashMap<>();
    private final Map<Integer, Set<ASEdge>> adjList = new HashMap<>();

    public ASGraph(Collection<Integer> sources) {
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
        vertices.putIfAbsent(id, new ASVertex(id));
        adjList.putIfAbsent(id, new HashSet<>());
    }

    @Override
    public ASVertex getVertex(int id) {
        return vertices.get(id);
    }

    @Override
    public void removeVertex(int id) {
        // Remove in-edges
        Set<ASEdge> outEdges = adjList.get(id);
        for (ASEdge outEdge : outEdges) {
            Set<ASEdge> inEdges = adjList.get(outEdge.getDest());

            boolean isFound = inEdges.remove(new ASEdge(id, 1));

            if (isFound) {
                ASVertex inVertex = vertices.get(outEdge.getDest());
                inVertex.setNeighborCount(inVertex.getNeighborCount() - 1);
            }
        }

        // Remove out-edges
        adjList.get(id).clear();

        // Remove vertex
        vertices.get(id).setNeighborCount(0);
    }

    @Override
    public List<ASVertex> getVertices() {
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
        throw new NotImplementedException();
    }

    @Override
    public ASEdge getEdge(long idx) {
        throw new NotImplementedException();
    }

    @Override
    public ASEdge getEdge(int src, int dst) {
        Set<ASEdge> edges = adjList.getOrDefault(src, Set.of());

        for (ASEdge edge : edges) {
            if (edge.getDest() == dst) return edge;
        }
        return null;
    }

    @Override
    public void addEdges(int src, Collection<ASEdge> newEdges) {
        Set<ASEdge> existingEdges = adjList.get(src);
        if (existingEdges == null)
            addVertex(src);

        assert existingEdges != null;

        existingEdges.addAll(Set.copyOf(newEdges));

        ASVertex srcVertex = vertices.get(src);
        srcVertex.setNeighborCount(adjList.get(src).size());
    }

    @Override
    public List<ASEdge> getEdges(int id) {
        return adjList.getOrDefault(id, Set.of()).stream().toList();
    }

    @Override
    public void removeEdge(int src, int dst) {
        Set<ASEdge> edges = adjList.get(src);
        if (edges == null)
            return;

        edges.removeIf(edge -> edge.getDest() == dst);

        ASVertex srcVertex = vertices.get(src);
        srcVertex.setNeighborCount(srcVertex.getNeighborCount() - 1);
    }

    @Override
    public long getEdgeCount() {
        return vertices.values().stream().mapToInt(ASVertex::getNeighborCount).sum();
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
        return new HashSet<>(adjList.get(id).stream().map(ASEdge::getDest).toList());
    }

    @Override
    public void forEachNeighbor(int id, IntConsumer consumer) {
        Set<ASEdge> edges = adjList.getOrDefault(id, Set.of());
        edges.forEach(edge -> consumer.accept(edge.getDest()));
    }

    @Override
    public Iterator<ASEdge> edgeIterator(int id) {
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
        ASEdge outEdge = null;
        for (ASEdge edge : adjList.get(src)) {
            if (edge.getDest() == dst) {
                edge.setValue(val);
                outEdge = edge;
                break;
            }
        }
        if (outEdge == null) {
            adjList.get(src).add(new ASEdge(dst, val));
            // Increase the neighbor count
            ASVertex sourceVertex = this.getVertex(src);
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
        adjList.get(src).add(new ASEdge(dst, val));
        // Increase the neighbor count
        ASVertex sourceVertex = this.getVertex(src);
        sourceVertex.setNeighborCount(sourceVertex.getNeighborCount() + 1);
    }

    public void debugPrint() {
        for (ASVertex v : vertices.values()) {
            System.out.println(v);
            System.out.println(adjList.get(v.getId()));
        }
    }
}
