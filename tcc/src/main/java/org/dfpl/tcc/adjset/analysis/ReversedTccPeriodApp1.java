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
package org.dfpl.tcc.adjset.analysis;

import org.apache.commons.lang3.time.StopWatch;
import org.dfpl.chronograph.Time;
import org.dfpl.chronograph.TimeInstant;
import org.dfpl.tcc.tr.algos.ReversedTimeScanAS;
import org.dfpl.utils.Utils;
import org.dfpl.tcc.adjset.ASEdge;
import org.dfpl.tcc.adjset.ASVertex;
import org.dfpl.chronograph.ChronoGraph;
import org.dfpl.chronograph.ChronoVertex;
import org.dfpl.tcc.clique.CliqueUpdater;
import org.dfpl.tcc.clique.chang.MCBRB;
import org.dfpl.tcc.clique.chang.MCDD;
import org.dfpl.tcc.rgraph.RGraph;
import org.dfpl.tcc.rgraph.RVertex;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ReversedTccPeriodApp1 {
    private static final Logger log = Logger.getLogger(ReversedTccPeriodApp1.class.getName());
    private static final String EDGE_LABEL = "label";
    private static final Time SOURCE_TIME = new TimeInstant(0);

    /**
     * Creates the reachability graph by removing non-reciprocal vertex pairs
     *
     * @param rGraph the pre-initialized reachability graph
     */
    private static void createRGraph(RGraph<ASVertex, ASEdge> rGraph) {
        int sourcesProcessed = 0;
        for (RVertex uVertex : rGraph.getVertices()) {
            int edgeCount = rGraph.getEdges(uVertex.getId()).size();
            Iterator<ASEdge> edgeIterator = rGraph.edgeIterator(uVertex.getId());
            while (edgeIterator.hasNext()) {
                ASEdge edge = edgeIterator.next();
                // Remove self-loop and non-reciprocal pairs
                if (uVertex.getId() == edge.getDest() || rGraph.getEdge(edge.getDest(), uVertex.getId()) == null) {
                    edgeIterator.remove();
                    edgeCount--;
                }
            }
            uVertex.setNeighborCount(edgeCount);

            if (++sourcesProcessed % 5000 == 0) {
                int logSourcesProcessed = sourcesProcessed;
                log.info(() -> String.format("%d sources processed.", logSourcesProcessed));
            }
        }

        if (log.isLoggable(Level.FINER)) {
            for (RVertex vVertex : rGraph.getVertices()) {
                Collection<ASEdge> edges = rGraph.getEdges(vVertex.getId());
                log.finer(() -> String.format("%s\n\t%d edges: %s", vVertex, vVertex.getNeighborCount(), edges));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        log.info(ReversedTccPeriodApp1.class.getName() + " started.");

        Utils.checkInput();

        StopWatch trWatch = new StopWatch();
        StopWatch rGraphWatch = new StopWatch();
        StopWatch cliqueWatch = new StopWatch();

        CliqueUpdater cliqueUpdater = new CliqueUpdater();

        ChronoGraph graph = Utils.createPeriodGraph(EDGE_LABEL);
        Collection<Integer> sources = Utils.getSources(graph);

        trWatch.start();
        RGraph<ASVertex, ASEdge> rGraph = ReversedTimeScanAS.computePeriod(graph, sources);
        trWatch.stop();
        int directedEdgeCount = (int) rGraph.getEdgeCount();
        log.info(() -> String.format("Temporal reachability computed in %s ms. E = %d", trWatch.getTime(), directedEdgeCount));

        double directedDensity = directedEdgeCount / ((double) rGraph.getVertices().size() * (rGraph.getVertices().size() - 1));
        log.info(() -> String.format("Directed Density: %f", directedDensity));

        log.info("Creating the reachability graph...");
        rGraphWatch.start();
        createRGraph(rGraph);
        rGraphWatch.stop();
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));

        double unDirectedDensity = rGraph.getEdgeCount() / ((double) rGraph.getVertices().size() * (rGraph.getVertices().size() - 1));
        log.info(() -> String.format("Undirected Density: %f", unDirectedDensity));

        log.info(() -> "Computing Max Clique...");
        cliqueWatch.start();

        ArrayList<Integer> sortedIds = graph.getVertices().stream().map(vertex -> ((ChronoVertex) vertex).getIntId()).collect(Collectors.toCollection(ArrayList::new));

        // Compute degrees of each vertex
        int[] degrees = new int[sortedIds.size()];
        Arrays.fill(degrees, 0);
        for (int id : sortedIds) {
            degrees[id] = rGraph.getVertex(id).getNeighborCount();
        }

        boolean[] visited = new boolean[sortedIds.size()];

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
                    if (rGraph.getEdge(uId, vId) == null)
                        log.severe(() -> String.format("v(%d) -> v(%d) not found", uId, vId));
                }
            }
        }

        log.info(() -> String.format("Temporal reachability computed in %s ms. E = %d", trWatch.getTime(), directedEdgeCount));
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));
        log.info(() -> String.format("Max Clique computed in %d ms. Size: %d", cliqueWatch.getTime(), cliqueUpdater.getMaxClique().size()));


        log.finer(() -> String.format("RGraph Max Clique: %s", cliqueUpdater.getMaxClique()));
        log.info(() -> "---Finished---");
    }
}
