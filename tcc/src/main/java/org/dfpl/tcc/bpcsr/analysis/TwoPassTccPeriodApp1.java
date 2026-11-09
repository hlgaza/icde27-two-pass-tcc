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
package org.dfpl.tcc.bpcsr.analysis;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.lang3.time.StopWatch;
import org.dfpl.chronograph.EdgeEvent;
import org.dfpl.chronograph.Time;
import org.dfpl.chronograph.TimeInstant;
import org.dfpl.chronograph.TimePeriod;
import org.dfpl.utils.Utils;
import org.dfpl.tcc.bpcsr.BEdgeCursor;
import org.dfpl.tcc.bpcsr.BPCSR;
import org.dfpl.tcc.bpcsr.BVertex;
import org.dfpl.chronograph.ChronoGraph;
import org.dfpl.chronograph.ChronoVertex;
import org.dfpl.tcc.clique.CliqueUpdater;
import org.dfpl.tcc.clique.chang.MCBRB;
import org.dfpl.tcc.clique.chang.MCDD;
import org.dfpl.tcc.rgraph.RGraph;
import org.dfpl.tcc.tr.algos.TraversalTBFS;
import org.dfpl.tcc.tr.SmallGammaLongTR;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TwoPassTccPeriodApp1 {
    private static final Logger log = Logger.getLogger(TwoPassTccPeriodApp1.class.getName());

    private static final String EDGE_LABEL = "label";

    public static long START_TIME = Long.MAX_VALUE;
    public static long LAST_TIME = 0;

    public static StopWatch trWatch = new StopWatch();
    public static StopWatch rGraphWatch = new StopWatch();

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
            Time startTime = new TimeInstant(Integer.parseInt(arr[2]));
            Time endTime = new TimeInstant(Integer.parseInt(arr[3]));
            Time time = new TimePeriod(startTime.getStartTime(), endTime.getFinishTime());

            if (START_TIME > startTime.getTime())
                START_TIME = startTime.getTime();
            if (LAST_TIME < endTime.getTime())
                LAST_TIME = endTime.getTime();

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

    public static RGraph<BVertex, BEdgeCursor> createRGraph(ChronoGraph graph, List<Integer> sources) {
        // ----- STATS -----
        int sourcesProcessed = 0;
        trWatch.start();
        trWatch.suspend();
        rGraphWatch.start();
        rGraphWatch.suspend();

        BPCSR rGraph = new BPCSR(graph.getVertices().size());

        SmallGammaLongTR tempGamma = new SmallGammaLongTR(graph.getVertices().size());
        BitSet include = new BitSet(graph.getVertices().size());

         TraversalTBFS traversal = new TraversalTBFS(tempGamma);

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

            if (!pred.isEmpty()) {
                rGraph.addEdges(sourceId, pred);
                pred.clear();
            }

            rGraphWatch.suspend();

            if (++sourcesProcessed % 5000 == 0) {
                int logSourcesProcessed = sourcesProcessed;
                log.info(() -> String.format("%d sources processed. density = %f", logSourcesProcessed, rGraph.getDensity()));
            }
        }

       return rGraph;
    }

    public static void main(String[] args) throws IOException {
        log.info(() -> TwoPassTccPeriodApp1.class.getName() + " started");
        StopWatch cliqueWatch = new StopWatch();

        Utils.checkInput();

        ChronoGraph graph = createGraph();

        List<Integer> sources = Utils.getSources(graph);

        log.info(() -> "Creating the reachability graph...");
        RGraph<BVertex, BEdgeCursor> rGraph = createRGraph(graph, sources);
        log.info(() -> String.format("Temporal reachability computed in %s ms.", trWatch.getTime()));
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));

        double unDirectedDensity = rGraph.getEdgeCount() / ((double) rGraph.getVertices().size() * (rGraph.getVertices().size() - 1));
        log.info(() -> String.format("Undirected density: %f", unDirectedDensity));

        log.info(() -> "Computing Max Clique...");
        CliqueUpdater cliqueUpdater = new CliqueUpdater();
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

        log.info(() -> String.format("Temporal reachability computed in %s ms.", trWatch.getTime()));
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));
        log.info(() -> String.format("Max Clique computed in %d ms. Size: %d", cliqueWatch.getTime(), cliqueUpdater.getMaxClique().size()));

        log.finer(() -> String.format("RGraph Max Clique: %s", cliqueUpdater.getMaxClique()));
        log.info(() -> "---Finished---");
    }
}
