/**
 * Portions Copyright (c) 2026 Haifa Gaza <br>
 * Translated from C++ to Java
 * <p>
 * Original Work:
 * Copyright (c) 2018 wheatman [<a href="https://github.com/wheatman/Packed-Compressed-Sparse-Row">GitHub</a>] <br>
 * Licensed under MIT License (see below)
 * <p>
 * MIT License
 * <p>
 * Copyright (c) 2018 wheatman
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.dfpl.tcc.bpcsr;

import org.dfpl.tcc.rgraph.REdge;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;

public class BEdgeCursor implements REdge<Byte> {
    public static final GroupLayout EDGE_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("dst"), // 4 bytes
            ValueLayout.JAVA_BYTE.withName("val"), // +1 byte
            MemoryLayout.paddingLayout(3)
    );

    public static final VarHandle DST_HANDLE = EDGE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("dst"));
    public static final VarHandle VAL_HANDLE = EDGE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("val"));

    public static final byte NULL_MARKER = 0;
    public static final byte EDGE_MARKER = 1;
    public static final byte SENTINEL_MARKER = 2;

    private MemorySegment segment;
    private long currentOffset;

    public BEdgeCursor at(MemorySegment segment, long index) {
        this.segment = segment;
        this.currentOffset = index * EDGE_LAYOUT.byteSize();
        return this;
    }

    @Override
    public boolean isNull() {
        return getValue() == NULL_MARKER;
    }

    public boolean isSentinel() {
        return getValue() == SENTINEL_MARKER;
    }

    @Override
    public int getDest() {
        return (int) DST_HANDLE.get(segment, currentOffset);
    }

    @Override
    public Byte getValue() {
        return (byte) VAL_HANDLE.get(segment, currentOffset);
    }

    public void set(int dst, Byte val) {
        DST_HANDLE.set(segment, currentOffset, dst);
        VAL_HANDLE.set(segment, currentOffset, val);
    }

    @Override
    public void setDest(int dst) {
        DST_HANDLE.set(segment, currentOffset, dst);
    }

    @Override
    public void setValue(Byte val) {
        VAL_HANDLE.set(segment, currentOffset, val);
    }


    public boolean equals(Object obj) {
        if (!(obj instanceof BEdgeCursor otherEdge)) return false;

        return this.getDest() == otherEdge.getDest() && this.getValue() == otherEdge.getValue();
    }

    @Override
    public String toString() {
        return "Edge{" + "dest=" + getDest() + ", value=" + getDest() + '}';
    }
}
