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
package org.dfpl.tcc.adjlist;

import org.dfpl.tcc.rgraph.REdge;

public class ALEdge implements REdge<Long> {
    public static final int NULL_MARKER = 0;

    private int dst;
    private long val;

    public ALEdge(int dst, long val) {
        this.dst = dst;
        this.val = val;
    }

    @Override
    public void setValue(Long val) {
        this.val = val;
    }

    @Override
    public Long getValue() {
        return this.val;
    }

    @Override
    public int getDest() {
        return this.dst;
    }

    @Override
    public void setDest(int dst) {
        this.dst = dst;
    }

    @Override
    public boolean isNull() {
        return this.val == NULL_MARKER;
    }

    @Override
    public String toString() {
        return "ALEdge{" + "dst=" + dst + ", val=" + val + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ALEdge) return ((ALEdge) obj).dst == dst;
        return super.equals(obj);
    }
}
