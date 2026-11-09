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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

public class BEdgeList implements AutoCloseable {
    private static final Logger log = Logger.getLogger(BEdgeList.class.getName());

    private Arena arena;
    private MemorySegment edges;

    private long N; // the size of the edge array
    private long H; // the height of the tree
    private long logN; // tree node size
    private long minN; // minimum number of nodes in the tree, this is vertices * 2 to account for the undirected edges

    public BEdgeList(long n) {
        arena = Arena.ofConfined();

        minN = n;
        N = 2L << bsrWord(minN);
        logN = (1L << bsrWord(bsrWord(N) + 1));
        H = bsrWord(N / logN);

        edges = arena.allocate(BEdgeCursor.EDGE_LAYOUT, N); // this does zero-initialization
    }

    public void setEdge(long index, int dst, byte val) {
        BEdgeCursor edgeCursor = new BEdgeCursor();
        edgeCursor.at(edges, index);
        edgeCursor.setDest(dst);
        edgeCursor.setValue(val);
    }

    public MemorySegment getEdges() {
        return edges;
    }

    public BEdgeCursor getEdgeCursor(long index) {
        return new BEdgeCursor().at(edges, index);
    }

    public long getN() {
        return N;
    }

    public long getH() {
        return H;
    }

    public long getLogN() {
        return logN;
    }

    public void expandList(int factor) {
        if (factor % 2 != 0) throw new IllegalArgumentException("Factor must be even");

        long oldN = N;
        N *= factor;
        logN = (1L << bsrWord(bsrWord(N) + 1));
        H = bsrWord(N / logN);

        Arena newArena = Arena.ofConfined();
        MemorySegment newEdges = newArena.allocate(BEdgeCursor.EDGE_LAYOUT, N);

        MemorySegment.copy(edges, 0L, newEdges, 0L, oldN * BEdgeCursor.EDGE_LAYOUT.byteSize());
        arena.close();

        arena = newArena;
        edges = newEdges;
    }

    /**
     * Find the most significant set bit in a 64-bit integer
     *
     * @param word the integer to scan
     * @return -1 if the input word is 0
     */
    private static long bsrWord(long word) {
        if (word == 0) return -1;
        return 63 - Long.numberOfLeadingZeros(word);
    }

    @Override
    public void close() throws Exception {
        log.info("Closing BEdgeList.");
        if(arena != null)
            arena.close();
    }

    public Iterator<BEdgeCursor> iterator(long start, long end) {
        return new EdgeIterator().reset(start, end);
    }

    private class EdgeIterator implements Iterator<BEdgeCursor> {
        private long index = 0L;
        private long end;
        private final BEdgeCursor edgeCursor = new BEdgeCursor();

        public EdgeIterator reset(long start, long end) {
            this.index = start;
            this.end = end;

            return this;
        }

        @Override
        public boolean hasNext() {
            edgeCursor.at(edges, index);
            while (index < end && edgeCursor.isNull()) {
                edgeCursor.at(edges, ++index);
            }
            return index < end;
        }

        @Override
        public BEdgeCursor next() {
            if (!hasNext()) throw new NoSuchElementException();

            return edgeCursor.at(edges, index++);
        }
    }
}
