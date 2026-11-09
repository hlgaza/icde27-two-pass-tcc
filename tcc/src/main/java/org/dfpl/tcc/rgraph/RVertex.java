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
package org.dfpl.tcc.rgraph;

/**
 * Represents a vertex in an RGraph.
 *
 * @author Haifa Gaza, DFPL, Department of Software, Sejong University
 */
public interface RVertex {
    /**
     * Returns the id of the vertex.
     *
     * @return the id of the vertex
     */
    int getId();

    /**
     * Returns the beginning index of the vertex
     *
     * @return the beginning index of the vertex
     */
    long getBeginning();

    /**
     * Returns the end index of the vertex
     *
     * @return the end index of the vertex
     */
    long getEnd();

    /**
     * Returns the number of neighbors of the vertex
     *
     * @return the number of neighbors of the vertex
     */
    int getNeighborCount();

    /**
     * Sets the beginning index of the vertex
     *
     * @param idx the beginning index of the vertex
     */
    void setBeginning(long idx);

    /**
     * Sets the end index of the vertex
     *
     * @param idx the end index of the vertex
     */
    void setEnd(long idx);

    /**
     * Sets the number of neighbors of the vertex
     *
     * @param count the number of neighbors of the vertex
     */
    void setNeighborCount(int count);
}
