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
package org.dfpl.tcc.csr.analysis;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.lang3.time.StopWatch;
import org.dfpl.chronograph.EdgeEvent;
import org.dfpl.chronograph.Time;
import org.dfpl.chronograph.TimeInstant;
import org.dfpl.utils.Utils;
import org.dfpl.chronograph.ChronoGraph;
import org.dfpl.chronograph.ChronoVertex;
import org.dfpl.tcc.clique.CliqueUpdater;
import org.dfpl.tcc.clique.chang.MCBRB;
import org.dfpl.tcc.clique.chang.MCDD;
import org.dfpl.tcc.csr.CEdge;
import org.dfpl.tcc.csr.CGraph;
import org.dfpl.tcc.csr.CVertex;
import org.dfpl.tcc.rgraph.RGraph;
import org.dfpl.tcc.tr.algos.TraversalTBFS;
import org.dfpl.tcc.tr.SmallGammaLongTR;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TwoPassTccInstantApp1 {
    private static final Logger log = Logger.getLogger(TwoPassTccInstantApp1.class.getName());
    private static final String EDGE_LABEL = "label";

    public static long START_TIME = Long.MAX_VALUE;
    public static long LAST_TIME = 0;

    public static StopWatch trWatch = new StopWatch();
    public static StopWatch rGraphWatch = new StopWatch();

    /**
     * Loads the temporal graph from the file
     *
     * @return the temporal graph
     * @throws IOException
     */
    public static ChronoGraph createGraph() throws IOException {
        log.info(() -> "Mapping vertices to integer IDs...");
        int lineRead = 0;
        BufferedReader br = new BufferedReader(new FileReader(System.getProperty("vertices")));
        HashMap<String, Integer> vertexToID = new HashMap<>();
        while (true) {
            String line = br.readLine();
            if (line == null)
                break;
            if (line.startsWith("#"))
                continue;

            String[] arr = line.split("[\\s,]+");
            int index = Integer.parseInt(arr[1]);

            vertexToID.put(arr[0], index);

            if (++lineRead % 10000 == 0) {
                int logLineRead = lineRead;
                log.info(() -> String.format("%d vertices mapped...", logLineRead));
            }
        }
        br.close();
        ChronoGraph g = new ChronoGraph(vertexToID);

        log.info(() -> "Mapped vertices.");

        addEdgeEvents(g);

        log.info(() -> String.format("Temporal graph created. V = %d, E=%d, Ee = %d", g.getVertices().size(), g.getEdges().size(), g.getEventCount()));

        return g;
    }

    /**
     * Adds all edge events from the file to the graph
     *
     * @param graph the graph in which all events will be added to
     */
    public static void addEdgeEvents(Graph graph) throws IOException {
        boolean isDirected = true;
        if (System.getProperty("directed") != null)
            isDirected = Boolean.parseBoolean(System.getProperty("directed"));

        int lineRead = 0;
        BufferedReader br = new BufferedReader(new FileReader(System.getProperty("stream")));

        log.info(() -> "Adding edge events...");
        while (true) {
            String line = br.readLine();
            if (line == null)
                break;
            if (line.startsWith("#"))
                continue;

            String[] arr = line.split("[\\s,]+");

            Vertex from = graph.addVertex(arr[0]);
            Vertex to = graph.addVertex(arr[1]);
            Time time = new TimeInstant(Integer.parseInt(arr[2]));

            if (START_TIME > time.getTime())
                START_TIME = time.getTime();
            if (LAST_TIME < time.getTime())
                LAST_TIME = time.getTime();

            EdgeEvent edgeEvent;
            Edge fromTo = graph.addEdge(from, to, EDGE_LABEL);
            edgeEvent = fromTo.addEvent(time);
            if (edgeEvent != null)
                lineRead++;


            if (!isDirected) {
                Edge toFrom = graph.addEdge(to, from, EDGE_LABEL);
                edgeEvent = toFrom.addEvent(time);
                if (edgeEvent != null)
                    lineRead++;
            }

            if (lineRead % 10000 == 0) {
                int logLineRead = lineRead;
                log.info(() -> String.format("%d edge events added...", logLineRead));
            }
        }
        br.close();
        int logLineRead = lineRead;
        log.info(() -> String.format("%d edge events added.", logLineRead));
    }

    public static RGraph<CVertex, CEdge> createRGraph(ChronoGraph graph, List<Integer> sources) {
        ChronoVertex[] vertexCache = new ChronoVertex[graph.getVertices().size()];
        graph.getVertices().forEach(v -> {
            ChronoVertex cv = (ChronoVertex) v;
            vertexCache[((ChronoVertex) v).getIntId()] = cv;
        });
        // ----- STATS -----
        int sourcesProcessed = 0;
        trWatch.start();
        trWatch.suspend();
        rGraphWatch.start();
        rGraphWatch.suspend();

        RGraph<CVertex, CEdge> rGraph = new CGraph(graph.getVertices().size(), 0);

        SmallGammaLongTR tempGamma = new SmallGammaLongTR(graph.getVertices().size());
        BitSet include = new BitSet(graph.getVertices().size());

        TraversalTBFS traversal = new TraversalTBFS(tempGamma, vertexCache);

        for (int sourceId = 0; sourceId < sources.size(); sourceId++) {
            Vertex source = graph.getVertex(sourceId);
            trWatch.resume();

            // Compute isBefore from source
            include.set(0, sourceId);
            rGraph.forEachNeighbor(sourceId, include::clear);
            SmallGammaLongTR predTR = traversal.traverseBackwardInstant(graph, sourceId, LAST_TIME, include);
            BitSet pred = predTR.reachableVertices();
            log.fine(() -> String.format("Predecessors of v(%s): %s", source.getId(), pred.cardinality()));

            tempGamma.clear();
            pred.clear(sourceId, rGraph.getVertices().size());

            if (pred.cardinality() == 0) {
                trWatch.suspend();
                continue;
            }

            // Compute isAfter from source
            include.clear();
            for (int i = pred.nextSetBit(0); i >= 0; i = pred.nextSetBit(i + 1)) {
                include.set(i);
            }

            SmallGammaLongTR descTR = traversal.traverseForwardInstant(graph, sourceId, START_TIME, include);
            BitSet desc = descTR.reachableVertices();
            log.fine(() -> String.format("Descendants of v(%s): %s", source.getId(), desc.cardinality()));
            tempGamma.clear();

            trWatch.suspend();
            rGraphWatch.resume();

            pred.and(desc);

            log.fine(() -> String.format("Connected component size: %s", pred.cardinality()));
            log.fine(() -> String.format("TCC of v(%s): %s", source.getId(), pred));

            // Grow the reachability graph
            if (pred.cardinality() != 0) {
                long totalEdges = rGraph.getEdgeCount() + pred.cardinality();
                RGraph<CVertex, CEdge> newRGraph = new CGraph(graph.getVertices().size(), (int) totalEdges);

                for (int vId = 0; vId < rGraph.getVertices().size(); vId++) {
                    if (vId == sourceId) { // process source
                        long oldIdx = rGraph.getVertex(sourceId).getBeginning();
                        long endIdx = rGraph.getVertex(sourceId).getEnd();
                        int newEdgeDest = pred.nextSetBit(0);

                        while (oldIdx < endIdx && newEdgeDest >= 0) {
                            CEdge existingEdge = rGraph.getEdge(oldIdx);

                            if (existingEdge.getDest() < newEdgeDest) {
                                newRGraph.addEdge(sourceId, existingEdge.getDest(), 1);
                                oldIdx++;
                            } else {
                                newRGraph.addEdge(sourceId, newEdgeDest, 1);
                                newEdgeDest = pred.nextSetBit(newEdgeDest + 1);
                            }
                        }

                        while (oldIdx < endIdx) {
                            newRGraph.addEdge(sourceId, rGraph.getEdge(oldIdx++).getDest(), 1);
                        }

                        while (newEdgeDest >= 0) {
                            newRGraph.addEdge(sourceId, newEdgeDest, 1);
                            newEdgeDest = pred.nextSetBit(newEdgeDest + 1);
                        }
                    } else { // vid has existing edges
                        boolean needsSourceEdge = pred.get(vId);

                        Iterator<CEdge> iterator = rGraph.edgeIterator(vId);
                        while (iterator.hasNext()) {
                            CEdge edge = iterator.next();
                            if (needsSourceEdge && edge.getDest() > sourceId) { // add sourceId
                                newRGraph.addEdge(vId, sourceId, 1);
                                needsSourceEdge = false;
                            }

                            newRGraph.addEdge(vId, edge.getDest(), edge.getValue());
                        }

                        if (needsSourceEdge) // if sourceId belongs at the end
                            newRGraph.addEdge(vId, sourceId, 1);
                    }
                }
                rGraph = newRGraph;

            }

            rGraphWatch.suspend();

            if (++sourcesProcessed % 5000 == 0) {
                int logSourcesProcessed = sourcesProcessed;
                RGraph<CVertex, CEdge> logRGraph = rGraph;
                log.info(() -> String.format("%d sources processed. density = %f", logSourcesProcessed, logRGraph.getDensity()));
            }
        }

        if (log.isLoggable(Level.FINER)) {
            for (int sourceId = 0; sourceId < rGraph.getVertices().size(); sourceId++) {
                CVertex vertex = rGraph.getVertex(sourceId);
                List<CEdge> edges = rGraph.getEdges(sourceId);
                log.finer(() -> String.format("%s\n\t%d edges: %s", vertex, edges.size(), edges));
                assert vertex.getNeighborCount() == edges.size();
            }
        }

        return rGraph;
    }

    public static void main(String[] args) throws IOException {
        log.info(() -> TwoPassTccInstantApp1.class.getName() + " started.");
        StopWatch cliqueWatch = new StopWatch();

        Utils.checkInput();

        CliqueUpdater cliqueUpdater = new CliqueUpdater();

        ChronoGraph graph = createGraph();

        List<Integer> sources = Utils.getSources(graph);

        log.info(() -> "Creating the reachability graph...");
        RGraph<CVertex, CEdge> rGraph = createRGraph(graph, sources);
        log.info(() -> String.format("Temporal reachability computed in %s ms.", trWatch.getTime()));
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));

        double unDirectedDensity = rGraph.getEdgeCount() / ((double) rGraph.getVertices().size() * (rGraph.getVertices().size() - 1));
        log.info(() -> String.format("Undirected density: %f", unDirectedDensity));

        log.info(() -> "Computing Max Clique...");
        cliqueWatch.start();
        ArrayList<Integer> sortedIds = graph.getVertices().stream().map(vertex -> ((ChronoVertex) vertex).getIntId()).collect(Collectors.toCollection(ArrayList::new));

        // Compute degrees of each vertex
        int[] degrees = new int[sortedIds.size()];
        Arrays.fill(degrees, 0);

        boolean[] visited = new boolean[sortedIds.size()];

        for (int id : sortedIds) {
            degrees[id] = rGraph.getVertex(id).getNeighborCount();
        }

        sortedIds.sort(Comparator.comparingInt(id -> rGraph.getVertex((Integer) id).getNeighborCount()).reversed());
        HashSet<Integer> localClique = new HashSet<>();
        MCDD.degreeHeuristic(null, rGraph, sortedIds, 10, cliqueUpdater, localClique);

        MCBRB.compute(null, rGraph, sortedIds, degrees, visited, cliqueUpdater, localClique);
        cliqueWatch.stop();

        if (log.isLoggable(Level.FINER)) {
            for (int uId : cliqueUpdater.getMaxClique()) {
                for (int vId : cliqueUpdater.getMaxClique()) {
                    if (vId <= uId)
                        continue;
                    if (rGraph.getEdge(uId, vId).isNull())
                        log.finer(() -> String.format("v(%d) -> v(%d) not found", uId, vId));
                }
            }
        }

        log.info(() -> String.format("Temporal reachability computed in %s ms.", trWatch.getTime()));
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));
        log.info(() -> String.format("Max Clique computed in %d ms. Size: %d", cliqueWatch.getTime(), cliqueUpdater.getMaxClique().size()));
        log.finer(() -> String.format("RGraph Max Clique: %s", cliqueUpdater.getMaxClique()));
        log.info(() -> "---Finished---");
    }
}
