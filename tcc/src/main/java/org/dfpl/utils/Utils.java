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
package org.dfpl.utils;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import org.dfpl.chronograph.EdgeEvent;
import org.dfpl.chronograph.Time;
import org.dfpl.chronograph.TimeInstant;
import org.dfpl.chronograph.TimePeriod;
import org.dfpl.chronograph.ChronoGraph;
import org.dfpl.chronograph.ChronoVertex;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class Utils {
    private static final Logger log = Logger.getLogger(Utils.class.getName());

    public static void checkInput() throws FileNotFoundException {
        boolean error = false;

        File sources = System.getProperty("sources") == null ? null : new File(System.getProperty("sources"));
        if (sources != null && sources.exists()) {
            log.info(() -> "Sources found in " + System.getProperty("sources"));
        } else {
            log.info(() -> "Sources file not initialized. Using all vertices as sources.");
        }

        File vertices = System.getProperty("vertices") == null ? null : new File(System.getProperty("vertices"));
        if (vertices != null && vertices.exists()) {
            log.info(() -> "Vertices found in " + System.getProperty("vertices"));
        } else {
            error = true;
            if (vertices == null)
                log.severe(() -> "-Dvertices=/path/ must be set.");
            else
                log.severe(() -> System.getProperty("vertices") + " does not exist.");
        }

        File stream = System.getProperty("stream") == null ? null : new File(System.getProperty("stream"));
        if (stream != null && stream.exists()) {
            log.info(() -> "Graph stream found in " + System.getProperty("stream"));
        } else {
            error = true;
            if (stream == null)
                log.severe(() -> "-Dstream=/path/ must be set.");
            else
                log.severe(() -> System.getProperty("stream") + " does not exist.");
        }

        if (System.getProperty("directed") == null || Boolean.parseBoolean(System.getProperty("directed")))
            log.info(() -> "Temporal graph is directed.");
        else
            log.info(() -> "Temporal graph is undirected.");

        if (error)
            throw new FileNotFoundException("Error in checking data sources. Please check the error messages above.");
    }

    /**
     * Creates a temporal graph with time-instant edge events
     *
     * @return the temporal graph
     */
    public static ChronoGraph createInstantGraph(String edgeLabel) throws IOException {
        log.info(() -> "Mapping vertices to integer IDs...");
        int lineRead = 0;
        BufferedReader br = new BufferedReader(new FileReader(System.getProperty("vertices")));
        HashMap<String, Integer> vertexToID = new HashMap<>();
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            if (line.startsWith("#")) continue;

            String[] arr = line.split("[\\s,]+");
            int index = Integer.parseInt(arr[1]);

            vertexToID.put(arr[0], index);

            if (++lineRead % 10000 == 0) {
                int logLineRead = lineRead;
                log.info(() -> logLineRead + " vertices mapped...");
            }
        }
        br.close();
        ChronoGraph g = new ChronoGraph(vertexToID);

        log.info(() -> "Mapped vertices.");

        boolean isDirected = true;
        if (System.getProperty("directed") != null) {
            isDirected = Boolean.parseBoolean(System.getProperty("directed"));
        }

        log.info(() -> "Adding edge events...");

        lineRead = 0;
        br = new BufferedReader(new FileReader(System.getProperty("stream")));

        while (true) {
            String line = br.readLine();
            if (line == null) break;
            if (line.startsWith("#")) continue;

            String[] arr = line.split("[\\s,]+");

            Vertex from = g.addVertex(arr[0]);
            Vertex to = g.addVertex(arr[1]);
            Time time = new TimeInstant(Integer.parseInt(arr[2]));

            EdgeEvent edgeEvent;
            Edge fromTo = g.addEdge(from, to, edgeLabel);
            edgeEvent = fromTo.addEvent(time);
            if (edgeEvent != null) lineRead++;

            if (!isDirected) {
                Edge toFrom = g.addEdge(to, from, edgeLabel);
                edgeEvent = toFrom.addEvent(time);

                if (edgeEvent != null) lineRead++;
            }

            if (lineRead % 10000 == 0) {
                int logLineRead = lineRead;
                log.info(() -> logLineRead + " edge events added...");
            }
        }
        br.close();
        log.info(() -> "Edge events added.");

        log.info(() -> String.format("Temporal graph created. V = %d, E=%d, Ee = %d", g.getVertices().size(), g.getEdges().size(), g.getEdgeEvents().size()));
        return g;
    }

    /**
     * Creates a temporal graph with time-period edge events
     *
     * @return the temporal graph
     */
    public static ChronoGraph createPeriodGraph(String edgeLabel) throws IOException {
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
                log.info(lineRead + " vertices mapped...");
            }
        }
        br.close();
        ChronoGraph g = new ChronoGraph(vertexToID);
        log.info(() -> "Mapped vertices.");

        boolean isDirected = true;
        if (System.getProperty("directed") != null) {
            isDirected = Boolean.parseBoolean(System.getProperty("directed"));
        }

        lineRead = 0;
        br = new BufferedReader(new FileReader(System.getProperty("stream")));

        log.info(() -> "Adding edge events...");
        while (true) {
            String line = br.readLine();
            if (line == null)
                break;
            if (line.startsWith("#"))
                continue;

            String[] arr = line.split("[\\s,]+");

            Vertex from = g.addVertex(arr[0]);
            Vertex to = g.addVertex(arr[1]);

            long startTime = Integer.parseInt(arr[2]);
            long endTime = Integer.parseInt(arr[3]);
            Time time = new TimePeriod(startTime, endTime);

            EdgeEvent edgeEvent;
            Edge fromTo = g.addEdge(from, to, edgeLabel);

            edgeEvent = fromTo.addEvent(time);
            if (edgeEvent != null)
                lineRead++;

            if (!isDirected) {
                Edge toFrom = g.addEdge(to, from, edgeLabel);
                edgeEvent = toFrom.addEvent(time);

                if (edgeEvent != null)
                    lineRead++;
            }

            if (lineRead % 10000 == 0) {
                int logLineRead = lineRead;
                log.info(() -> String.format("%d edge events added", logLineRead));
            }
        }

        br.close();
        int logLineRead = lineRead;
        log.info(() -> String.format("%d edge events added.", logLineRead));

        log.info(() -> String.format("Temporal graph created. V = %d, E=%d, Ee = %d", g.getVertices().size(), g.getEdges().size(), g.getEdgeEvents().size()));

        return g;
    }

    /**
     * Loads the sources from the file or uses all vertices as sources
     *
     * @param g the temporal graph
     * @return the list of source ids
     * @throws IOException if the file is not found or the buffered reader encounters an error
     */
    public static List<Integer> getSources(ChronoGraph g) throws IOException {
        log.info(() -> "Initializing the sources...");

        List<Integer> sources = new ArrayList<>(g.getVertices().size());

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
                    sources.add(((ChronoVertex) newVertex).getIntId());
                } catch (Exception e) {
                    g.getVertex(arr[0]);
                }

                if (++lineRead % 10000 == 0) {
                    int logLineRead = lineRead;
                    log.info(() -> String.format("%d sources added...", logLineRead));
                }
            }
            br.close();
        } else {
            g.getVertices().stream().map(vertex -> ((ChronoVertex) vertex).getIntId()).forEach(sources::add);
        }

        sources.sort(Comparator.comparingInt(Integer::intValue));
        log.info(() -> "Sources initialized.");

        return sources;
    }
}
