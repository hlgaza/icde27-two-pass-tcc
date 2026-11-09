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

import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.lang3.time.StopWatch;
import org.dfpl.tcc.tr.SmallGammaLongTR;
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
import org.dfpl.tcc.tr.algos.TimeCentricTR;
import org.dfpl.tcc.tr.BigGammaTR;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TimeCentricTccPeriodApp1 {
    private static final Logger log = Logger.getLogger(TimeCentricTccPeriodApp1.class.getName());
    private static final String EDGE_LABEL = "label";
    private static final Long SOURCE_TIME = 0L;

    /**
     * Compute the temporal reachability using the time-centric approach with fixed-time point iteration
     *
     * @return the program containing the graph and Gamma
     */
    public static BigGammaTR computeTR(ChronoGraph g, HashSet<Vertex> sources) {
        log.info(() -> "Creating the Gamma Table...");
        BigGammaTR bigGamma = new BigGammaTR(g, sources, SOURCE_TIME);
        log.info(() -> "Gamma table created.");

        log.info(() -> "Computing temporal reachability...");

        new TimeCentricTR(g, bigGamma).computePeriod(sources);
        log.info(() -> "Temporal reachability computed.");
        return bigGamma;
    }

    /**
     * Loads the sources from the file
     *
     * @param g the temporal graph
     * @return the sources
     */
    private static HashSet<Vertex> getSources(ChronoGraph g) throws IOException {
        log.info(() -> "Initializing the sources...");

        HashSet<Vertex> sources = new HashSet<>();

        if (System.getProperty("sources") != null) {
            int lineRead = 0;
            BufferedReader br = new BufferedReader(new FileReader(System.getProperty("sources")));
            while (true) {
                String line = br.readLine();
                if (line == null) break;
                if (line.startsWith("#")) continue;

                String[] arr = line.split("[\\s,]+");

                try {
                    Vertex newVertex = g.addVertex(arr[0]);
                    sources.add(newVertex);
                } catch (Exception e) {
                    g.getVertex(arr[0]);
                }

                if (++lineRead % 10000 == 0) {
                    int logLineRead = lineRead;
                    log.info(() -> logLineRead + " sources added...");
                }
            }
            br.close();
        } else {
            sources.addAll(g.getVertices());
        }
        log.info(() -> "Sources initialized.");

        return sources;
    }

    /**
     * Creates the reachability graph by removing non-reciprocal vertex pairs and converting the temporal reachability to CSR
     *
     * @param gammaTable the table containing the temporal reachability
     * @return the reachability graph
     */
    public static RGraph<CVertex, CEdge> createRGraph(BigGammaTR gammaTable) {
        int sourcesProcessed = 0;
        RGraph<CVertex, CEdge> rGraph = null;
        int edgeCount = 0;

        // Remove self-referencing edges and non-reciprocal edges
        for (Vertex source : gammaTable.getSources()) {
            int sourceId = ((ChronoVertex) source).getIntId();
            SmallGammaLongTR gamma = gammaTable.getGamma(sourceId);

            for (int jId = 0; jId < gamma.size(); jId++) {
                if (sourceId == jId) {
                    gamma.put(sourceId, SmallGammaLongTR.NULL_TIME);
                    continue;
                }

                if (gamma.get(jId) == SmallGammaLongTR.NULL_TIME) {
                    gammaTable.set(jId, sourceId, SmallGammaLongTR.NULL_TIME);
                    continue;
                }

                if (gammaTable.get(jId, sourceId) == SmallGammaLongTR.NULL_TIME) {
                    gamma.put(jId, SmallGammaLongTR.NULL_TIME);
                }
            }

            if (++sourcesProcessed % 5000 == 0) {
                int logSourcesProcessed = sourcesProcessed;
                log.info(() -> logSourcesProcessed + " sources processed...");
            }
        }

        // Initialize CSR graph
        edgeCount = gammaTable.getSources().stream()
                .mapToInt(source -> gammaTable.getGamma(source.getId()).getNeighborCount())
                .sum();

        rGraph = new CGraph(gammaTable.getSourceSize(), edgeCount);

        // Add CSR edges
        for (int sourceId = 0; sourceId < rGraph.getVertices().size(); sourceId++) {
            SmallGammaLongTR gamma = gammaTable.getGamma(sourceId);
            for (int jId = 0; jId < gamma.size(); jId++) {
                if (gamma.get(jId) == SmallGammaLongTR.NULL_TIME) continue;
                rGraph.addEdge(sourceId, jId, 1);
            }
        }

        return rGraph;
    }

    public static void main(String[] args) throws IOException {
        log.info(() -> TimeCentricTccPeriodApp1.class.getName() + " started.");

        Utils.checkInput();

        StopWatch tcWatch = new StopWatch();
        StopWatch cliqueWatch = new StopWatch();

        CliqueUpdater cliqueUpdater = new CliqueUpdater();
        ChronoGraph graph = Utils.createPeriodGraph(EDGE_LABEL);
        HashSet<Vertex> sources = getSources(graph);

        tcWatch.start();
        BigGammaTR bigGamma = computeTR(graph, sources);
        tcWatch.stop();

        int directedEdgeCount = IntStream.range(0, bigGamma.getSourceSize()).mapToObj(bigGamma::getGamma).mapToInt(SmallGammaLongTR::getNeighborCount).sum();
        log.info(() -> String.format("Temporal reachability computed in %s ms. E = %d", tcWatch.getTime(), directedEdgeCount));
        double directedDensity = directedEdgeCount / ((double) graph.getVertices().size() * (graph.getVertices().size() - 1));
        log.info(() -> String.format("Directed Density: %f", directedDensity));

        log.info(() -> "Creating the reachability graph...");
        StopWatch rGraphWatch = new StopWatch();
        rGraphWatch.start();
        RGraph<CVertex, CEdge> rGraph = createRGraph(bigGamma);
        rGraphWatch.stop();
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));
        double unDirectedDensity = rGraph.getEdgeCount() / ((double) rGraph.getVertices().size() * (rGraph.getVertices().size() - 1));
        log.info(() -> String.format("Undirected Density: %f", unDirectedDensity));

        log.info(() -> "Computing Max Clique...");

        cliqueWatch.start();
        ArrayList<Integer> sortedIds = graph.getVertices().stream()
                .map(vertex -> ((ChronoVertex) vertex).getIntId())
                .collect(Collectors.toCollection(ArrayList::new));

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
                    if (vId <= uId) continue;
                    if (rGraph.getEdge(uId, vId).isNull())
                        log.severe(() -> String.format("v(%d) -> v(%d) not found", uId, vId));
                }
            }
        }

        log.info(() -> String.format("Temporal reachability computed in %s ms. E = %d", tcWatch.getTime(), directedEdgeCount));
        log.info(() -> String.format("Reachability graph created in %s ms. V = %d, E = %d", rGraphWatch.getTime(), rGraph.getVertices().size(), rGraph.getEdgeCount()));
        log.info(() -> String.format("Max Clique computed in %d ms. Size: %d", cliqueWatch.getTime(), cliqueUpdater.getMaxClique().size()));
        log.finer(() -> String.format("RGraph Max Clique: %s", cliqueUpdater.getMaxClique()));
        log.info(() -> "---Finished---");
    }
}
