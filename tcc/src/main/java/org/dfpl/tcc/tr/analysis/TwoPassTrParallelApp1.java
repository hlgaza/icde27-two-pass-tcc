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
package org.dfpl.tcc.tr.analysis;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.apache.commons.lang3.time.StopWatch;
import org.dfpl.chronograph.EdgeEvent;
import org.dfpl.chronograph.Time;
import org.dfpl.chronograph.TimeInstant;
import org.dfpl.utils.Utils;
import org.dfpl.chronograph.ChronoGraph;
import org.dfpl.tcc.tr.algos.TraversalTBFS;
import org.dfpl.tcc.tr.SmallGammaLongTR;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class TwoPassTrParallelApp1 {
    private static final Logger log = Logger.getLogger(TwoPassTrParallelApp1.class.getName());

    private static final String EDGE_LABEL = "label";

    public static long START_TIME = Long.MAX_VALUE;
    public static long LAST_TIME = 0;

    public static StopWatch trWatch = new StopWatch();

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

    record EdgeUpdate(int sourceId, BitSet neighbors) {
    }

    public static void runTwoPass(ChronoGraph graph, List<Integer> sources) {
        // ----- STATS -----
        trWatch.start();
        trWatch.suspend();

        // Local thread structures
        ThreadLocal<SmallGammaLongTR> tempGammaThread = ThreadLocal.withInitial(() -> new SmallGammaLongTR(graph.getVertices().size()));
        ThreadLocal<BitSet> includeThread = ThreadLocal.withInitial(() -> new BitSet(graph.getVertices().size()));

        ThreadLocal<BitSet> predThread = ThreadLocal.withInitial(BitSet::new);
        ThreadLocal<BitSet> descThread = ThreadLocal.withInitial(BitSet::new);


        // Pass updates from workers to the main thread
        BlockingQueue<EdgeUpdate> updateQueue = new ArrayBlockingQueue<>(10_000);
        EdgeUpdate POISON_PILL = new EdgeUpdate(-1, null); // Signal for end of processing

        trWatch.resume();

        // Start traversal in the background using ForkJoinPool
        CompletableFuture<Void> traversalTask = CompletableFuture.runAsync(() -> {
            IntStream.range(0, sources.size()).parallel().forEach(sourceId -> {
                BitSet include = includeThread.get();
                SmallGammaLongTR tempGamma = tempGammaThread.get();
                BitSet pred = predThread.get();
                BitSet desc = descThread.get();

                include.clear();
                tempGamma.clear();
                pred.clear();
                desc.clear();

                TraversalTBFS traversal = new TraversalTBFS(tempGamma);

                include.set(0, sourceId);
                SmallGammaLongTR predTR = traversal.traverseBackwardInstant(graph, sourceId, LAST_TIME, include);
                predTR.reachableVertices(pred);

                pred.clear(sourceId, graph.getVertices().size());

                if (pred.cardinality() > 0) {
                    include.clear();
                    for (int id = pred.nextSetBit(0); id >= 0; id = pred.nextSetBit(id + 1)) {
                        include.set(id);
                    }

                    tempGamma.clear();
                    SmallGammaLongTR descTR = traversal.traverseForwardInstant(graph, sourceId, START_TIME, include);
                    descTR.reachableVertices(desc);

                    pred.and(desc);

                    if (pred.cardinality() > 0) {
                        try {
                            // Push result to main thread; blocks briefly only if queue is full
                            updateQueue.put(new EdgeUpdate(sourceId, (BitSet) pred.clone()));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            });

            // Notify main thread that processing is complete
            try {
                updateQueue.put(POISON_PILL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Main thread consumes queue and updates rGraph
        int sourcesProcessed = 0;

        AtomicLong edgeCount = new AtomicLong(0L);

        long denominator = mul(graph.getVertices().size(), graph.getVertices().size() - 1);

        while (true) {
            try {
                EdgeUpdate update = updateQueue.take();
                if (update == POISON_PILL) {
                    break; // All sources processed
                }

                edgeCount.addAndGet(update.neighbors.cardinality());

                sourcesProcessed++;
                if (sourcesProcessed % 5000 == 0) {
                    long currEdgeCount = 2L * edgeCount.get();

                    double density = (double) currEdgeCount / denominator;

                    int currentProcessed = sourcesProcessed;
                    log.info(() -> String.format("%d sources processed. density = %f", currentProcessed, density));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        traversalTask.join(); // Ensure background work finished cleanly
        trWatch.suspend();

        long undirectedEdgeCount = (2L * edgeCount.get());
        double density = (double) undirectedEdgeCount / denominator;
        log.info(String.format("%d sources processed. |E_R| = %d. density = %f", sourcesProcessed, undirectedEdgeCount, density));
    }

    static long mul(long a, long b) {
        long hi = Math.multiplyHigh(a, b);   // upper 64 bits of the 128-bit product (Java 9+)
        long lo = a * b;                     // lower 64 bits

        // The product fits in a signed long iff hi is just the sign extension of lo
        if ((hi == 0 && lo >= 0) || (hi == -1 && lo < 0)) {
            return lo;
        }

        return BigInteger.valueOf(a)
                .multiply(BigInteger.valueOf(b))
                .longValueExact();
    }

    public static void main(String[] args) throws IOException {
        log.info(() -> TwoPassTrParallelApp1.class.getName() + " started");

        Utils.checkInput();

        ChronoGraph graph = createGraph();

        List<Integer> sources = Utils.getSources(graph);

        log.info(() -> "Compute two-pass TR...");
        runTwoPass(graph, sources);
        log.info(() -> String.format("Temporal reachability computed in %s ms.", trWatch.getTime()));

        log.info(() -> "---Finished---");
    }
}
