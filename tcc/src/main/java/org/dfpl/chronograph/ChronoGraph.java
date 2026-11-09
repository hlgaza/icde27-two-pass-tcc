/**
 * Apache 2.0 License
 * <p>
 * Portions Copyright 2026 Haifa Gaza
 * Modified functions
 * <p>
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
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.dfpl.chronograph.Tokens.TimeComparator;

import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.LogManager;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The in-memory implementation of a temporal graph database.
 *
 * @author Jaewook Byun, Ph.D., Assistant Professor, DFPL, Department of
 * Software, Sejong University
 * @author Haifa Gaza, Master Student, DFPL, Sejong University
 * <br/>
 * Byun, Jaewook, Sungpil Woo, and Daeyoung Kim. "Chronograph: Enabling
 * temporal graph traversals for efficient information diffusion
 * analysis over time." IEEE Transactions on Knowledge and Data
 * Engineering 32.3 (2019): 424-437.
 * <br/>
 * Byun, Jaewook. "Enabling time-centric computation for efficient
 * temporal graph traversals from multiple sources." IEEE Transactions
 * on Knowledge and Data Engineering (2020).
 */
public class ChronoGraph implements Graph {

    /**
     * Vertices queried by a vertex identifier
     */
    private final HashMap<String, Vertex> vertices;

    /**
     * Edges queried by an edge identifier
     */
    private final HashMap<String, Edge> edges;

    /**
     * simple index for out-going edges by a key of vertex identifier
     * <br/>
     * <vertexID, HashSet<out-going edges>
     */
    private ArrayList<Edge>[] outEdges;

    /**
     * simple index for in-going edges by a key of vertex identifier
     * <br/>
     * <vertexID, HashSet<in-going edges>
     */
    private ArrayList<Edge>[] inEdges;

    /**
     * TODO
     */
    private LogManager logManager;

    /**
     * TODO
     */
    private final IdManager vertexIdManager;

    /**
     * TODO
     */
    private final LabelManager labelIdManager;

    /**
     * Create an empty graph
     */
    public ChronoGraph(Path filePath, HashSet<String> idSet) {
        vertices = new HashMap<>();
        edges = new HashMap<>();
        outEdges = new ArrayList[idSet.size()];
        inEdges = new ArrayList[idSet.size()];
        vertexIdManager = new IdManager(idSet);
        labelIdManager = new LabelManager();
    }

    /**
     * Create an empty graph
     */
    public ChronoGraph(HashMap<String, Integer> vertexIdSet) {
        vertices = new HashMap<>();
        edges = new HashMap<>();
        outEdges = new ArrayList[vertexIdSet.size()];
        inEdges = new ArrayList[vertexIdSet.size()];
        vertexIdManager = new IdManager(vertexIdSet);
        labelIdManager = new LabelManager();

        for (String id : vertexIdSet.keySet()) {
            vertices.put(id, new ChronoVertex(ChronoGraph.this, id));
        }
    }

    /**
     * Create an empty graph
     */
    public ChronoGraph(IdManager vertexIdManager, LabelManager labelIdManager) {
        this.vertices = new HashMap<>();
        this.edges = new HashMap<>();
        outEdges = new ArrayList[vertexIdManager.size()];
        inEdges = new ArrayList[vertexIdManager.size()];
        this.vertexIdManager = vertexIdManager;
        this.labelIdManager = labelIdManager;
    }

    /**
     * TODO
     *
     * @return the vertex ID manager
     */
    public IdManager getVertexIdManager() {
        return this.vertexIdManager;
    }

    /**
     * TODO
     *
     * @return the label id manager
     */
    public LabelManager getLabelIdManager() {
        return this.labelIdManager;
    }

    /**
     * Create a new vertex, add it to the graph, and return the newly created
     * vertex. The provided object identifier is a recommendation for the identifier
     * to use. It is not required that the implementation use this identifier.
     *
     * @param id the recommended object identifier
     * @return the newly created vertex
     */
    @Override
    public Vertex addVertex(String id) {
        if (id.contains("|"))
            throw new IllegalArgumentException("Vertex ID cannot contain '|'");

        return vertices.compute(id, (String identifier, Vertex existingVertex) -> {
            if (existingVertex == null)
                return new ChronoVertex(ChronoGraph.this, identifier);
            return existingVertex;
        });
    }

    /**
     * Return the vertex referenced by the provided object identifier. If no vertex
     * is referenced by that identifier, then return null.
     *
     * @param id the identifier of the vertex to retrieve from the graph
     * @return the vertex referenced by the provided identifier or null when no such
     * vertex exists
     */
    @Override
    public Vertex getVertex(String id) {
        return vertices.get(id);
    }

    /**
     * TODO
     *
     * @param id the integer index
     * @return the vertex
     */
    public Vertex getVertex(Integer id) {
        String stringId = vertexIdManager.get(id);
        return vertices.get(stringId);
    }

    /**
     * Return an iterable to all the vertices in the graph. If this is not possible
     * for the implementation, then an UnsupportedOperationException can be thrown.
     *
     * @return an iterable reference to all vertices in the graph
     */
    @Override
    public Collection<Vertex> getVertices() {
        return vertices.values();
    }

    /**
     * Return an iterable to all the vertices in the graph that have a particular
     * key/value property. If this is not possible for the implementation, then an
     * UnsupportedOperationException can be thrown. The graph implementation should
     * use indexing structures to make this efficient else a full vertex-filter scan
     * is required.
     *
     * @param key   the key of vertex
     * @param value the value of the vertex
     * @return an iterable of vertices with a provided key and value
     */
    @Override
    public Collection<Vertex> getVertices(String key, Object value) {
        return vertices.values().parallelStream().filter(v -> {
            Object val = v.getProperty(key);
            return val != null && (val.equals(value));
        }).collect(Collectors.toSet());
    }

    /**
     * Add an edge to the graph. The added edges require a recommended identifier,
     * a tail vertex, a head vertex, and a label. Like adding a vertex, the
     * implementation may ignore the provided object identifier.
     *
     * @param outVertex the vertex on the tail of the edge
     * @param inVertex  the vertex on the head of the edge
     * @param label     the label associated with the edge
     * @return the newly created edge
     */
    @Override
    public Edge addEdge(Vertex outVertex, Vertex inVertex, String label) {
        if (label.contains("|"))
            throw new IllegalArgumentException("An edge label cannot contain '|'");
        this.addVertex(outVertex.getId());
        this.addVertex(inVertex.getId());
        String edgeId = ChronoEdge.getEdgeID(outVertex, inVertex, label);

        if (edges.containsKey(edgeId))
            return edges.get(edgeId);

        final ChronoEdge edge = new ChronoEdge(ChronoGraph.this, outVertex, label, inVertex);
        edges.put(edgeId, edge);

        // update an index for out-going edges
        int outId = ((ChronoVertex) outVertex).getIntId();
        if (outEdges[outId] == null) {
            outEdges[outId] = new ArrayList<>();
        }
        int index = Collections.binarySearch(outEdges[outId], edge,
                Comparator.comparingInt(e -> ((ChronoEdge) e).getInId()));
        if (index < 0) {
            outEdges[outId].add(-index - 1, edge);
        }

        int inId = ((ChronoVertex) inVertex).getIntId();
        if (inEdges[inId] == null) {
            inEdges[inId] = new ArrayList<>();
        }

        index = Collections.binarySearch(inEdges[inId], edge,
                Comparator.comparingInt(e -> ((ChronoEdge) e).getOutId()));
        if (index < 0) {
            inEdges[inId].add(-index - 1, edge);
        }

        return edge;
    }

    /**
     * Return the edge referenced by the provided object identifier. If no edge is
     * referenced by that identifier, then return null.
     *
     * @param outVertex the identifier of the out-going vertex to retrieve from the
     *                  graph
     * @param inVertex  the identifier of the in-going vertex to retrieve from the
     *                  graph
     * @param label     the label of the edge
     * @return the edge referenced by the provided identifier or null when no such
     * edge exists
     */
    @Override
    public Edge getEdge(Vertex outVertex, Vertex inVertex, String label) {
        return edges.get(ChronoEdge.getEdgeID(outVertex, inVertex, label));
    }

    /**
     * TODO
     *
     * @param outVertexIntID the out-going vertex ID
     * @param inVertexIntID  the in-going vertex ID
     * @param labelIntID     the label ID
     * @return the edge
     */
    public Edge getEdge(Integer outVertexIntID, Integer inVertexIntID, Integer labelIntID) {
        String outVertexID = vertexIdManager.get(outVertexIntID);
        Vertex outVertex = getVertex(outVertexID);

        String inVertexID = vertexIdManager.get(inVertexIntID);
        Vertex inVertex = getVertex(inVertexID);

        if (outVertex == null || inVertex == null) return null;

        String label = labelIdManager.getId(labelIntID);
        return edges.get(ChronoEdge.getEdgeID(outVertex, inVertex, label));
    }

    /**
     * Return the edge referenced by the provided object identifier. If no edge is
     * referenced by that identifier, then return null.
     *
     * @param id the identifier of the edge to retrieve from the graph
     * @return the edge referenced by the provided identifier or null when no such
     * edge exists
     */
    @Override
    public Edge getEdge(String id) {
        return edges.get(id);
    }

    /**
     * Return an iterable to all the edges in the graph. If this is not possible for
     * the implementation, then an UnsupportedOperationException can be thrown.
     *
     * @return an iterable reference to all edges in the graph
     */
    @Override
    public Collection<Edge> getEdges() {
        return edges.values();
    }

    /**
     * Return an iterable to all the edges in the graph that have a particular
     * key/value property. If this is not possible for the implementation, then an
     * UnsupportedOperationException can be thrown. The graph implementation should
     * use indexing structures to make this efficient, else a full edge-filter scan
     * is required.
     *
     * @param key   the key of the edge
     * @param value the value of the edge
     * @return an iterable of edges with a provided key and value
     */
    @Override
    public Collection<Edge> getEdges(String key, Object value) {
        return edges.values().parallelStream().filter(v -> {
            Object val = v.getProperty(key);
            return val != null && (val.equals(value));
        }).collect(Collectors.toSet());
    }

    /**
     * Remove the provided vertex from the graph. Upon removing the vertex, all the
     * edges by which the vertex is connected must be removed as well.
     *
     * @param vertex the vertex to remove from the graph
     */
    @Override
    public void removeVertex(Vertex vertex) {
        String id = vertex.getId();
        this.vertices.remove(id);

        // update a key-value map for edges
        Iterator<Entry<String, Edge>> eIter = edges.entrySet().iterator();
        while (eIter.hasNext()) {
            Entry<String, Edge> entry = eIter.next();
            Edge cEdge = entry.getValue();
            if (cEdge.getVertex(Direction.OUT).equals(vertex) || cEdge.getVertex(Direction.IN).equals(vertex)) {
                eIter.remove();
            }
        }
        // indexes
        int intId = ((ChronoVertex) vertex).getIntId();
        for (int idx = 0; idx < outEdges.length; idx++) {
            if (idx == intId)
                outEdges[idx] = null;
            else {
                outEdges[idx].removeIf(e -> e.getVertex(Direction.IN).equals(vertex));
                if (outEdges[idx].isEmpty())
                    outEdges[idx] = null;
            }
        }

        for (int idx = 0; idx < inEdges.length; idx++) {
            if (idx == intId)
                inEdges[idx] = null;
            else {
                inEdges[idx].removeIf(e -> e.getVertex(Direction.OUT).equals(vertex));
                if (inEdges[idx].isEmpty())
                    inEdges[idx] = null;
            }
        }

    }

    @Override
    public void removeEdge(Edge edge) {
        this.edges.remove(edge.toString());
        for (ArrayList<Edge> edges : outEdges) {
            edges.remove(edge);
        }
        for (ArrayList<Edge> edges : inEdges) {
            edges.remove(edge);
        }
    }

    /**
     * Do Nothing in an in-memory graph
     */
    @Override
    public void shutdown() {
        // Do Nothing
    }

    ArrayList<Edge>[] getOutEdges() {
        return outEdges;
    }


    ArrayList<Edge>[] getInEdges() {
        return inEdges;
    }

    @Override
    public Collection<EdgeEvent> getEdgeEvents() {
        return getEdges().parallelStream().flatMap(e -> e.getEvents().parallelStream()).collect(Collectors.toSet());
    }

    public void getEdgeEvents(NavigableSet<EdgeEvent> collection) {
        for (Edge edge : this.edges.values()) {
            collection.addAll(edge.getEvents());
        }
    }

    public int getEventCount() {
        return getEdges().parallelStream().flatMapToInt(e -> IntStream.of(((ChronoEdge) e).getEventCount())).sum();
    }

    @Override
    public NavigableSet<Time> getTimes(TimeComparator timeComparator, boolean scanVertexEvents,
                                       boolean scanEdgeEvents) {
        TreeSet<Time> timeSet = new TreeSet<>(timeComparator);
        if (scanVertexEvents) {
            getVertices().parallelStream().flatMap(v -> v.getTimes(timeComparator).parallelStream())
                    .forEach(timeSet::add);
        }
        if (scanEdgeEvents) {
            for (Edge e : getEdges()) {
                timeSet.addAll(e.getTimes(timeComparator));
            }
        }
        return timeSet;
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }

}
