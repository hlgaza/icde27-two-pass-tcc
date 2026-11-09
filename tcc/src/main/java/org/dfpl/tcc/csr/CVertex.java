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
package org.dfpl.tcc.csr;

import org.dfpl.tcc.rgraph.RVertex;

public class CVertex implements RVertex {
    public static final int NULL_MARKER = -1;

    private int id;
    private int neighborCount = 0;
    private int beginning;
    private int end;

    public CVertex(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public long getBeginning() {
        return beginning;
    }

    @Override
    public long getEnd() {
        return end;
    }

    @Override
    public int getNeighborCount() {
        return neighborCount;
    }

    @Override
    public void setBeginning(long idx) {
        this.beginning = (int) idx;
    }

    @Override
    public void setEnd(long idx) {
        this.end = (int) idx;
    }

    @Override
    public void setNeighborCount(int count) {
        this.neighborCount = count;
    }

    @Override
    public String toString() {
        return "CVertex{" + "id=" + id + ", neighborCount=" + neighborCount + ", beginning=" + beginning + ", end=" + end + '}';
    }
}
