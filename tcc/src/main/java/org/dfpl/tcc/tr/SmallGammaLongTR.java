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
import org.dfpl.chronograph.ChronoVertex;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.IntConsumer;

public class SmallGammaLongTR {
    private Long[] times;

    public static final Long NULL_TIME = null;
    int neighborCount = 0;

    public SmallGammaLongTR(int size) {
        this.times = new Long[size];
        Arrays.fill(this.times, NULL_TIME);
    }

    public SmallGammaLongTR() {
    }

    /**
     * Creates an ArrayList of Time with indices corresponding to the reachable
     * vertex </br>
     * Adds the reachable vertex to the set of reachable vertices
     *
     * @param reachableVertex the reachable vertex to set
     * @param visitTime       the visit time of the reachable vertex
     */
    public SmallGammaLongTR(Vertex reachableVertex, Long visitTime) {
        int index = ((ChronoVertex) reachableVertex).getIntId();
        this.times = new Long[index];
        Arrays.fill(this.times, NULL_TIME);

        times[index] = visitTime;
//        if(visitTime != SmallGammaTR.NULL_TIME) neighborCount++;
    }

    public void put(Vertex inVertex, Long visitTime) {
        int inIndex = ((ChronoVertex) inVertex).getIntId();

        this.put(inIndex, visitTime);
    }

    /**
     * Increases the list size if the index is greater than the current size </br>
     * Adds the index to the set of reachable vertices </br>
     * Sets the visit time at the index </br>
     *
     * @param inIndex   the index of the reachable vertex
     * @param visitTime the visit time of the reachable vertex
     */
    public void put(int inIndex, Long visitTime) {
        if (inIndex >= times.length) {
            times = Arrays.copyOf(times, inIndex + 1);
            times[inIndex] = NULL_TIME;
        }
        if (times[inIndex] == NULL_TIME && visitTime != NULL_TIME) neighborCount++;
        else if (times[inIndex] != NULL_TIME && visitTime == NULL_TIME)
            neighborCount--;

        this.times[inIndex] = visitTime;
    }

    /**
     * Gets the integer value of the reachable vertex
     *
     * @return the visit time of the reachableVertex or {@code null} if not reachable
     */
    public Long getTime(Vertex reachableVertex) {
        int index = ((ChronoVertex) reachableVertex).getIntId();

        if (index < 0 || index >= times.length)
            return NULL_TIME;

        return times[index];
    }

    public Long getTime(int index) {
        if (index < 0 || index >= times.length)
            return NULL_TIME;

        return times[index];
    }


    /**
     * Returns true if the vertex is reachable
     *
     * @param reachableVertex the vertex to check
     * @return true if the vertex is reachable
     */
    public boolean isReachable(Vertex reachableVertex) {
        int index = ((ChronoVertex) reachableVertex).getIntId();

        return this.isReachable(index);
    }

    /**
     * Returns true if the index is reachable
     *
     * @param index the index to check
     * @return true if the index is reachable
     */
    public boolean isReachable(int index) {
        return index < times.length && times[index] != NULL_TIME;
    }

    /**
     * Returns the number of reachable vertices
     *
     * @return the number of reachable vertices
     */
    public int getNeighborCount() {
        return this.neighborCount;
    }

    public void print() {
        for (Long time : times) {
            System.out.print(time + " ");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Long time : times) {
            if (time != NULL_TIME) {
                sb.append(time).append(" ");
            } else {
                sb.append("null " + " ");
            }
        }
        return sb.toString();
    }

    public void forEachNeighbor(IntConsumer action) {
        for (int i = 0; i < times.length; i++) {
            if (times[i] != NULL_TIME) {
                action.accept(i);
            }
        }
    }

    public BitSet reachableVertices() {
        BitSet neighbors = new BitSet(times.length);
        for (int i = 0; i < times.length; i++) {
            if (times[i] != NULL_TIME) {
                neighbors.set(i);
            }
        }
        return neighbors;
    }

    public void reachableVertices(BitSet neighbors) {
        for (int i = 0; i < times.length; i++) {
            if (times[i] != NULL_TIME) {
                neighbors.set(i);
            }
        }
    }

    public int size() {
        return times.length;
    }

    public Long get(int jId) {
        return times[jId];
    }

    public SmallGammaLongTR clone() {
        SmallGammaLongTR clone = new SmallGammaLongTR(this.size());
        clone.neighborCount = this.neighborCount;
        for (int i = 0; i < this.size(); i++) {
            clone.put(i, this.get(i));
        }
        return clone;
    }

    public void clear() {
        Arrays.fill(this.times, NULL_TIME);
        this.neighborCount = 0;
    }
}
