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
package org.dfpl.tcc.tr;

import com.tinkerpop.blueprints.Vertex;
import org.dfpl.chronograph.ChronoGraph;
import org.dfpl.chronograph.IdManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * A Big Gamma implementation for temporal reachability.
 */
public class BigGammaTR {

    private static final Logger log = Logger.getLogger(BigGammaTR.class.getName());
    private final IdManager idManager;

    // The number of vertices
    private final int size;
    HashSet<Vertex> sources;

    HashMap<Integer, SmallGammaLongTR> gammaTable;

    private final long srcTime;

    /**
     * The constructor for the Big Gamma
     *
     * @param graph   the reference graph
     * @param sources the source vertex events
     */
    public BigGammaTR(ChronoGraph graph, HashSet<Vertex> sources, long srcTime) {
        this.idManager = graph.getVertexIdManager();
        this.size = idManager.size();
        this.sources = sources;
        this.srcTime = srcTime;

        this.gammaTable = new HashMap<>(sources.size());
        sources.forEach(source -> {
            int srcIdx = idManager.get(source.getId());
            SmallGammaLongTR gamma = new SmallGammaLongTR(size);
            gamma.put(srcIdx, srcTime);
            gammaTable.put(srcIdx, gamma);
        });
    }

    public int getSourceSize() {
        return sources.size();
    }

    public HashSet<Vertex> getSources() {
        return sources;
    }

    /**
     * Returns the gamma value of the reachable vertex at the source
     *
     * @param srcId the source vertex id
     * @param inId  the reachable vertex id
     * @return the gamma value
     */
    public Long get(String srcId, String inId) {
        int srcIdx = idManager.get(srcId);
        int inIdx = idManager.get(inId);

        return gammaTable.get(srcIdx).get(inIdx);
    }

    public Long get(Integer srcIdx, Integer inIdx) {
        return gammaTable.get(srcIdx).get(inIdx);
    }

    /**
     * Returns gamma of the source
     *
     * @param srcId the vertex id of the source
     * @return gamma
     */
    public SmallGammaLongTR getGamma(String srcId) {
        int srcIdx = idManager.get(srcId);

        return gammaTable.get(srcIdx);
    }

    public SmallGammaLongTR getGamma(int srcIdx) {
        return gammaTable.get(srcIdx);
    }


    /**
     * Sets the value of the reachable vertex at the given source
     *
     * @param srcIdx the integer id of the source
     * @param inIdx  the integer id of the reachable vertex
     * @param value  the value to set
     */
    public void set(Integer srcIdx, Integer inIdx, Long value) {
        if (srcIdx == null || inIdx == null) throw new NullPointerException();

        gammaTable.get(srcIdx).put(inIdx, value);
    }

    /**
     * Overwrites the gamma file of the {@code sourceIdx} with the {@code updatedGamma}
     * values
     *
     * @param srcIdx       the integer id of the source vertex
     * @param updatedGamma a map containing the updated gamma
     */
    public void update(Integer srcIdx, HashMap<Integer, Long> updatedGamma) {
        if (updatedGamma.isEmpty()) return;

        SmallGammaLongTR gamma = gammaTable.get(srcIdx);

        updatedGamma.forEach(gamma::put);
    }

    /**
     * Sets the gamma value of the reachable vertex at the source
     *
     * @param srcId the id of the source vertex event
     * @param inId  the id of the reachable vertex
     * @param value the visit time
     */
    public void set(String srcId, String inId, Long value) {
        Integer srcIdx = idManager.get(srcId);
        Integer inIdx = idManager.get(inId);

        if (srcIdx == null || inIdx == null) {
            log.severe("sourceId: " + srcId + " inId: " + inId + " not found");
        }

        this.set(srcIdx, inIdx, value);
    }

    /**
     * Sets the gamma value of the {@code reachableVertex} at the {@code sourceVertex}
     *
     * @param srcVertex the source vertex
     * @param dstVertex the reachable vertex
     * @param value     the visit time
     */
    public void set(Vertex srcVertex, Vertex dstVertex, Long value) {
        this.set(srcVertex.getId(), dstVertex.getId(), value);
    }

    public void print() {
        for (Vertex source : sources) {
            System.out.print(source.getId() + ": ");

            this.getGamma(source.getId()).print();
            System.out.println();
        }
    }
}
