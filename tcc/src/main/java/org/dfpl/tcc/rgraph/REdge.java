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

public interface REdge<T> {
    /**
     * Sets the value of the edge
     *
     * @param val the new value of the edge
     */
    void setValue(T val);

    /**
     * Returns the value of the edge
     *
     * @return the value of the edge
     */
    T getValue();

    /**
     * Returns the destination of the edge
     *
     * @return the destination of the edge
     */
    int getDest();

    /**
     * Sets the destination of the edge
     *
     * @param dst the destination of the edge
     */
    void setDest(int dst);

    /**
     * Returns true if the edge is null, false otherwise
     *
     * @return true if the edge is null, false otherwise
     */
    boolean isNull();
}
