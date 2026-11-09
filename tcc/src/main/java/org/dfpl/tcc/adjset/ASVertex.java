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
package org.dfpl.tcc.adjset;

import org.dfpl.tcc.rgraph.RVertex;

public class ASVertex implements RVertex {

    private final int id;
    private int neighborCount = 0;

    public ASVertex(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public long getBeginning() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getEnd() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNeighborCount() {
        return this.neighborCount;
    }

    @Override
    public void setBeginning(long idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEnd(long idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNeighborCount(int count) {
        this.neighborCount = count;
    }

    @Override
    public String toString() {
        return "ALVertex{" + "id=" + id + ", neighborCount=" + neighborCount + '}';
    }
}
