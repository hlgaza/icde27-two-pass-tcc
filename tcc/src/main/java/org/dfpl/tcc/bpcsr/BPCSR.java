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

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.dfpl.tcc.rgraph.RGraph;

import java.lang.foreign.MemorySegment;
import java.math.BigInteger;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

public class BPCSR implements RGraph<BVertex, BEdgeCursor> {
    private static final Logger log = Logger.getLogger(BPCSR.class.getName());

    public static final int NULL_VALUE = -1;

    private final List<BVertex> vertices;
    private final BEdgeList edges;

    private final int[] ranks;
    private final int[] cores;
    private final int[] upperBounds;
    private final int[] colors;

    public BPCSR(int n) {
        edges = new BEdgeList(n * 2L);

        vertices = new ArrayList<>(n);
        for (int id = 0; id < n; id++) {
            addVertex(id);
        }

        this.colors = new int[n];
        this.ranks = new int[n];
        this.cores = new int[n];
        this.upperBounds = new int[n];

        Arrays.fill(colors, BPCSR.NULL_VALUE);
        Arrays.fill(ranks, BPCSR.NULL_VALUE);
        Arrays.fill(cores, BPCSR.NULL_VALUE);
        Arrays.fill(upperBounds, BPCSR.NULL_VALUE);
    }

    public int[] getColors() {
        return colors;
    }

    public int getCore(int id) {
        return cores[id];
    }

    public double getDensity() {
        return getEdgeCount() / ((double) vertices.size() * vertices.size() - 1);
    }

    public BEdgeCursor getEdge(long index) {
        return edges.getEdgeCursor(index);
    }

    public BEdgeCursor getEdge(int src, int dest) {
        if (src >= vertices.size() || dest >= vertices.size()) return null;

        long start = vertices.get(src).getBeginning();
        long end = vertices.get(src).getEnd();

        BEdgeCursor edge = new BEdgeCursor();
        long loc = BinarySearch.compute(edges, dest, start + 1, end);
        edge.at(edges.getEdges(), loc);

        if (!edge.isNull() && !edge.isSentinel() && edge.getDest() == dest) {
            return edge;
        }

        return null;
    }

    @Override
    public void addEdges(int src, Collection<BEdgeCursor> newEdges) {
        throw new NotImplementedException();
    }

    @Override
    public List<BEdgeCursor> getEdges(int id) {
        throw new NotImplementedException();
    }

    @Override
    public void removeEdge(int src, int dst) {
        throw new NotImplementedException();
    }

    public long getEdgeCount() {
        long count = 0;
        for (BVertex vertex : vertices) {
            count += vertex.getNeighborCount();
        }
        return count;
    }

    public Set<Integer> getNeighbors(int id) {
        BVertex sourceVertex = vertices.get(id);

        Set<Integer> output = new HashSet<>(sourceVertex.getNeighborCount());

        BEdgeCursor edgeCursor = new BEdgeCursor();
        for (long i = sourceVertex.getBeginning() + 1; i < sourceVertex.getEnd(); i++) {
            edgeCursor.at(edges.getEdges(), i);
            if (edgeCursor.getValue() != BEdgeCursor.NULL_MARKER) {
                output.add(edgeCursor.getDest());
            }
            if (output.size() == sourceVertex.getNeighborCount()) break;
        }

        return output;
    }

    public int getRank(int id) {
        return ranks[id];
    }

    public int getUpperBound(int id) {
        return upperBounds[id];
    }

    public BVertex getVertex(int id) {
        return vertices.get(id);
    }

    @Override
    public void removeVertex(int id) {
        throw new UnsupportedOperationException();
    }

    public List<BVertex> getVertices() {
        return vertices;
    }

    @Override
    public void addEdge(int src, int dst, long val) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEdge(int src, int dst, long val) {
        throw new UnsupportedOperationException();
    }

    public void setCore(int id, int core) {
        cores[id] = core;
    }

    public void setRank(int id, int rank) {
        ranks[id] = rank;
    }

    public void setUpperBound(int id, int val) {
        upperBounds[id] = val;
    }

    public void addEdge(int src, int dest, byte val) {
        if (src == dest) throw new IllegalArgumentException("Cannot add a self-loop edge");
        if (val == BEdgeCursor.SENTINEL_MARKER) throw new IllegalArgumentException("Cannot add a sentinel edge");
        if (val == BEdgeCursor.NULL_MARKER) return;

        addDirectedEdge(dest, src, val);
        addDirectedEdge(src, dest, val);
    }

    public void addEdges(int source, BitSet newNeighbors) {
        if (newNeighbors.isEmpty()) return;

        // Check the global density and expand if necessary
        long totalNeeded = this.getEdgeCount() + this.vertices.size() + (newNeighbors.cardinality() * 2L);
        if ((double) totalNeeded / edges.getN() >= densityBound(0).getRight()) {
            int factor = 2;
            while ((double) totalNeeded / (edges.getN() * factor) >= densityBound(0).getRight()) {
                factor *= 2;
            }
            edges.expandList(factor);
            redistribute(0, edges.getN());
        }

        // Insert the edges from all the other vertices to source
        for (int neighborId = newNeighbors.nextSetBit(0); neighborId >= 0; neighborId = newNeighbors.nextSetBit(neighborId + 1)) {
            addDirectedEdge(neighborId, source, BEdgeCursor.EDGE_MARKER);
        }

//        log.info("Single insertion");
//        debugEdges();

        // Batch insert the edges from source to all other vertices
        insertBulk(source, newNeighbors);

//        log.info("Batch insertion");
//        debugEdges();

//        log.info("density=" + getDensity(0,32));
    }

    public void close() throws Exception {
        edges.close();
    }

    public void debugEdges() {
        StringBuilder sb = new StringBuilder();
        sb.append("N: ").append(edges.getN()).append(" LogN: ").append(edges.getLogN()).append(" H: ").append(edges.getH()).append("\n");
        log.info(sb.toString());
        sb.setLength(0);

        BEdgeCursor edgeCursor = new BEdgeCursor();
        for (long i = 0; i < edges.getN(); i++) {
            edgeCursor.at(edges.getEdges(), i);

            if (edgeCursor.isSentinel()) {
                BVertex v = vertices.get(edgeCursor.getDest());
                sb.append("Vertex ").append(edgeCursor.getDest()).append(": ").append(v);
                log.info(sb.toString());
                sb.setLength(0);
            }
            sb.append("Edge ").append(i).append(": dest=").append(edgeCursor.getDest())
                    .append(", value=").append(edgeCursor.getValue());
            log.info(sb.toString());
            sb.setLength(0);
        }
    }

    public Iterator<BEdgeCursor> edgeIterator(int id) {
        BVertex vertex = vertices.get(id);
        return edges.iterator(vertex.getBeginning() + 1, vertex.getEnd());
    }

    public void forEachNeighbor(int id, IntConsumer consumer) {
        int processed = 0;
        BVertex vertex = vertices.get(id);

        BEdgeCursor edgeCursor = new BEdgeCursor();
        for (long i = vertex.getBeginning() + 1; i < vertex.getEnd(); i++) {
            edgeCursor.at(edges.getEdges(), i);
            if (!edgeCursor.isNull()) {
                processed++;
                consumer.accept(edgeCursor.getDest());
            }

            if (processed == vertex.getNeighborCount()) break;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (BVertex vertex : vertices) {
            int matrixIndex = 0;
            for (long i = vertex.getBeginning() + 1; i < vertex.getEnd(); i++) {
                BEdgeCursor edgeCursor = new BEdgeCursor();
                edgeCursor.at(edges.getEdges(), i);

                if (!edgeCursor.isNull()) {
                    while (matrixIndex < edgeCursor.getDest()) {
                        sb.append("000 ");
                        matrixIndex++;
                    }
                    sb.append(String.format("%03d ", edgeCursor.getValue()));
                    matrixIndex++;
                }
            }
            sb.repeat("000 ", Math.max(0, vertices.size() - matrixIndex));
            sb.append("\n");
        }

        return sb.toString();
    }

    private void addDirectedEdge(int src, int dest, byte val) {
        BVertex vertex = vertices.get(src);
        long locToAdd = BinarySearch.compute(edges, dest, vertex.getBeginning() + 1, vertex.getEnd());

        insert(locToAdd, dest, val, src);
        vertices.get(src).setNeighborCount(vertex.getNeighborCount() + 1);
    }

    @Override
    public void addVertex(int id) {
        BVertex vertex;

        if (id > 0) {
            long beginning = vertices.get(id - 1).getEnd();
            vertex = new BVertex(id, beginning, beginning + 1, 0);
        } else {
            vertex = new BVertex(id, 0L, 1L, 0);
        }

        vertices.add(vertex);
        insert(vertex.getBeginning(), id, BEdgeCursor.SENTINEL_MARKER, vertices.size() - 1);
    }

    private Pair<Double, Double> densityBound(long depth) {
        double lVal = 1.0 / 4.0 - ((.125 * depth) / edges.getH());
        double rVal = 3.0 / 4.0 + ((.25 * depth) / edges.getH());
        return Pair.of(lVal, rVal);
    }

    private void doubleList() {
        edges.expandList(2);

        redistribute(0, edges.getN());
    }

    private long findElemPointer(long index, int elemDest, byte elemVal) {
        BEdgeCursor edge = edges.getEdgeCursor(index);
        while (index < edges.getN() - 1 && edge.getDest() != elemDest) {
            index++;
            edge.at(edges.getEdges(), index);
        }
        return index;
    }

    private long findLeaf(long index) {
        return (index / edges.getLogN()) * edges.getLogN();
    }

    private long findLeaf(long index, long len) {
        return (index / len) * len;
    }

    private void fixSentinel(int vertexIndex, long in) {
        vertices.get(vertexIndex).setBeginning(in);

        if (vertexIndex > 0) vertices.get(vertexIndex - 1).setEnd(in);

        if (vertexIndex == vertices.size() - 1) vertices.get(vertexIndex).setEnd(edges.getN() - 1);
    }

    private double getDensity(long index, long len) {
        long full = 0;
        BEdgeCursor edge = new BEdgeCursor();
        for (long i = index; i < index + len; i++) {
            edge.at(edges.getEdges(), i);
            full += (edge.isNull()) ? 0 : 1;
        }
        return (double) full / len;
    }

    private double getDensity(long index, long len, long offset) {
        long full = 0;
        BEdgeCursor edge = new BEdgeCursor();
        for (long i = index; i < index + len; i++) {
            edge.at(edges.getEdges(), i);
            full += (edge.isNull()) ? 0 : 1;
        }
        return ((double) full + offset) / len;
    }

    private long insert(long index, int elemDest, byte elemVal, int src) {
        long leaf = findLeaf(index);
        long level = edges.getH();
        long len = edges.getLogN();

        BEdgeCursor edgeCursor = edges.getEdgeCursor(index);
        if (edgeCursor.isNull()) {
            edgeCursor.set(elemDest, elemVal);
        } else {
            // if the edge already exists in the graph, update its value
            if (elemVal != BEdgeCursor.SENTINEL_MARKER
                    && !edgeCursor.isSentinel()
                    && edgeCursor.getDest() == elemDest) {
                edgeCursor.setValue(elemVal);
                return index;
            }

            if (index == edges.getN() - 1) {
                // when adding at the end of the edges, double edges, then add.
                doubleList();
                BVertex vertex = vertices.get(src);
                long locToAdd = BinarySearch.compute(edges, elemDest, vertex.getBeginning() + 1, vertex.getEnd());
                return insert(locToAdd, elemDest, elemVal, src);
            } else {
                if (slideRight(index) == -1) {
                    index--;
                    slideLeft(index);
                }
            }
            edgeCursor.at(edges.getEdges(), index);
            edgeCursor.set(elemDest, elemVal);
        }

        double density = getDensity(leaf, len);

        // Spill over into the next level up, vertex is completely full
        if (density == 1) {
            leaf = findLeaf(leaf, len * 2L);
            redistribute(leaf, len * 2L);
        } else {
            redistribute(leaf, len);
        }

        Pair<Double, Double> densityBound = densityBound(level);
        density = getDensity(leaf, len);

        // while density too high, go up the implicit tree
        while (density >= densityBound.getRight()) {
            len *= 2;
            if (len <= edges.getN()) {
                level--;
                leaf = findLeaf(leaf, len);
                densityBound = densityBound(level);
                density = getDensity(leaf, len);
            } else {
                doubleList();
                // Search from the beginning because the list was doubled;
                return findElemPointer(0, elemDest, elemVal);
            }
        }
        redistribute(leaf, len);

        return findElemPointer(leaf, elemDest, elemVal);
    }

    private void insertBulk(int source, BitSet newNeighbors) {
        BVertex sourceVertex = vertices.get(source);

        long vertexRange = sourceVertex.getEnd() - sourceVertex.getBeginning();
        long len = edges.getLogN();
        long level = edges.getH();
        while (len < vertexRange) {
            len *= 2;
            if (len <= edges.getN()) {
                level--;
            }
        }

        long leaf = findLeaf(sourceVertex.getBeginning(), len);
        double density = getDensity(leaf, len, newNeighbors.cardinality());
        Pair<Double, Double> densityBound = densityBound(level);

        while (density >= densityBound.getRight()) {
            if (len >= edges.getN()) break;

            len *= 2;
            level--;
            leaf = findLeaf(leaf, len);
            densityBound = densityBound(level);
            density = getDensity(leaf, len, newNeighbors.cardinality());
        }
        redistribute(source, leaf, len, newNeighbors);
        vertices.get(source).setNeighborCount(sourceVertex.getNeighborCount() + newNeighbors.cardinality());
    }

    private void redistribute(long index, long len) {
        long j = 0L;

        // Compact non-null edges to the beginning of the range
        BEdgeCursor edgeCursor = new BEdgeCursor();
        for (long i = 0L; i < len; i++) {
            edgeCursor.at(edges.getEdges(), index + i);

            int edgeDest = edgeCursor.getDest();
            byte edgeVal = edgeCursor.getValue();

            if (edgeVal != BEdgeCursor.NULL_MARKER) {
                if (i != j) {
                    edges.setEdge(index + j, edgeDest, edgeVal);
                }
                j++;
            }
        }

        // Clear out the trailing spaces left after compaction
        if (j < len) {
            long clearStartByte = (index + j) * BEdgeCursor.EDGE_LAYOUT.byteSize();
            long clearByteCount = (len - j) * BEdgeCursor.EDGE_LAYOUT.byteSize();
            edges.getEdges().asSlice(clearStartByte, clearByteCount).fill(BEdgeCursor.NULL_MARKER);
        }

        if (j == 0) return;

        // Redistribute the edges
        long writePos = j - 1;
        for (long i = index + len - 1; i >= index; i--) {
            edgeCursor.at(edges.getEdges(), i);
            int edgeDest = edgeCursor.getDest();
            byte edgeVal = edgeCursor.getValue();

            if (edgeVal == BEdgeCursor.NULL_MARKER) continue;

            long in = Math.addExact(index, mulDiv(writePos, len, j));
            if (i != in) {
                edgeCursor.set(BEdgeCursor.NULL_MARKER, BEdgeCursor.NULL_MARKER);

                edges.setEdge(in, edgeDest, edgeVal);
            }

            if (edgeVal == BEdgeCursor.SENTINEL_MARKER) {
                fixSentinel(edgeDest, in);
            }
            writePos--;
        }
    }

    static long mulDiv(long a, long b, long d) {
        long hi = Math.multiplyHigh(a, b);   // upper 64 bits of the 128-bit product (Java 9+)
        long lo = a * b;                     // lower 64 bits

        // The product fits in a signed long iff hi is just the sign extension of lo
        if ((hi == 0 && lo >= 0) || (hi == -1 && lo < 0)) {
            return lo / d;
        }
        return BigInteger.valueOf(a)
                .multiply(BigInteger.valueOf(b))
                .divide(BigInteger.valueOf(d))   // truncates toward zero, same as long division
                .longValueExact();               // throws if the result doesn't fit
    }

    private void redistribute(int src, long index, long len, BitSet newNeighbors) {
        BVertex sourceVertex = vertices.get(src);
        BEdgeCursor edgeCursor = new BEdgeCursor();

        int edgesCountInRange = 0;
        if (sourceVertex.getEnd() > index + len) { // Some neighbors are stored outside the affected range
            for (long i = sourceVertex.getBeginning() + 1; i < index + len; i++) {
                edgeCursor.at(edges.getEdges(), i);
                if (!edgeCursor.isNull()) {
                    edgesCountInRange++;
                }
            }
        } else
            edgesCountInRange = sourceVertex.getNeighborCount();

        long j = 0; // index of non-null edges in the range, holds the number of non-null edges in the range after compaction
        long startIndexOfSource = -1;
        BEdgeCursor writeCursor = new BEdgeCursor();
        for (long i = 0; i < len; i++) {
            edgeCursor.at(edges.getEdges(), index + i);
            int edgeDest = edgeCursor.getDest();
            byte edgeVal = edgeCursor.getValue();

            if (edgeVal != BEdgeCursor.NULL_MARKER) {
                if (i != j) {
                    writeCursor.at(edges.getEdges(), index + j);
                    writeCursor.set(edgeDest, edgeVal);
                }
                j++;

                if (edgeVal == BEdgeCursor.SENTINEL_MARKER && edgeDest == src) {
                    startIndexOfSource = index + j - 1;
                }
            }
        }

        // Make space for new edges by shifting the edges array to the right
        long endIndexOfSource = startIndexOfSource + 1 + edgesCountInRange;

        MemorySegment.copy(edges.getEdges(), endIndexOfSource * BEdgeCursor.EDGE_LAYOUT.byteSize(),
                edges.getEdges(), (endIndexOfSource + newNeighbors.cardinality()) * BEdgeCursor.EDGE_LAYOUT.byteSize(),
                (index + j - endIndexOfSource) * BEdgeCursor.EDGE_LAYOUT.byteSize());


        for (int neighborId = newNeighbors.nextSetBit(0); neighborId >= 0; neighborId = newNeighbors.nextSetBit(neighborId + 1)) {
            edgeCursor.at(edges.getEdges(), endIndexOfSource);
            edgeCursor.set(neighborId, BEdgeCursor.EDGE_MARKER);
            endIndexOfSource++;
            j++;
        }

        // Clear out the trailing spaces left after compaction
        if (j < len) {
            long clearStartByte = (index + j) * BEdgeCursor.EDGE_LAYOUT.byteSize();
            long clearByteCount = (len - j) * BEdgeCursor.EDGE_LAYOUT.byteSize();
            edges.getEdges().asSlice(clearStartByte, clearByteCount).fill(BEdgeCursor.NULL_MARKER);
        }

        // Sort the edges in the source range
        if (sourceVertex.getEnd() > index + len) { // Some neighbors are stored outside the affected range
            long startIndexOutOfRange;

            for (startIndexOutOfRange = endIndexOfSource; startIndexOutOfRange < sourceVertex.getEnd(); startIndexOutOfRange++) {
                edgeCursor.at(edges.getEdges(), startIndexOutOfRange);
                if (!edgeCursor.isNull())
                    break;
            }

            MergePartitions.merge(edges.getEdges(), startIndexOfSource + 1, startIndexOfSource + 1 + newNeighbors.cardinality(),
                    endIndexOfSource, startIndexOutOfRange, sourceVertex.getEnd());
        } else if (sourceVertex.getNeighborCount() > 0) {
            MergePartitions.merge(edges.getEdges(), startIndexOfSource + 1,
                    startIndexOfSource + 1 + newNeighbors.cardinality(),
                    endIndexOfSource);
        }

        // Redistribute the edges
        long totalEdges = j;
        long writeIdx = totalEdges - 1;
        for (long i = index + len - 1; i >= index; i--) {
            edgeCursor.at(edges.getEdges(), i);
            int edgeDest = edgeCursor.getDest();
            byte edgeVal = edgeCursor.getValue();

            if (edgeVal == BEdgeCursor.NULL_MARKER) continue;

            long in = Math.addExact(index, mulDiv(writeIdx, len, totalEdges));

            if (i != in) {
                edgeCursor.set(BEdgeCursor.NULL_MARKER, BEdgeCursor.NULL_MARKER);
                edges.setEdge(in, edgeDest, edgeVal);
            }

            if (edgeVal == BEdgeCursor.SENTINEL_MARKER) {
                fixSentinel(edgeDest, in);
            }

            writeIdx--;
        }
    }

    private void slideLeft(long index) {
        BEdgeCursor currEdgeCursor = edges.getEdgeCursor(index);
        int currDest = currEdgeCursor.getDest();
        byte currVal = currEdgeCursor.getValue();
        currEdgeCursor.set(BEdgeCursor.NULL_MARKER, BEdgeCursor.NULL_MARKER);

        index--;
        currEdgeCursor.at(edges.getEdges(), index);
        while (index >= 0 && !currEdgeCursor.isNull()) {
            BEdgeCursor nextEdgeCursor = edges.getEdgeCursor(index);
            int nextDest = nextEdgeCursor.getDest();
            byte nextVal = nextEdgeCursor.getValue();

            nextEdgeCursor.set(currDest, currVal);

            if (currVal == BEdgeCursor.SENTINEL_MARKER) {
                fixSentinel(currDest, index);
            }
            currDest = nextDest;
            currVal = nextVal;
            --index;
            currEdgeCursor.at(edges.getEdges(), index);
        }

        if (index == -1) {
            doubleList();
            slideRight(0);
            index = 0;
        }

        if (currVal == BEdgeCursor.SENTINEL_MARKER) {
            fixSentinel(currDest, index);
        }

        currEdgeCursor.at(edges.getEdges(), index);
        currEdgeCursor.set(currDest, currVal);
    }

    private int slideRight(long index) {
        int rVal = 0;
        BEdgeCursor currEdge = edges.getEdgeCursor(index);

        int currDest = currEdge.getDest();
        byte currVal = currEdge.getValue();
        currEdge.set(BEdgeCursor.NULL_MARKER, BEdgeCursor.NULL_MARKER);

        ++index;
        currEdge.at(edges.getEdges(), index);

        while (index < edges.getN() && !currEdge.isNull()) {
            BEdgeCursor next = edges.getEdgeCursor(index);
            int nextDest = next.getDest();
            byte nextVal = next.getValue();

            next.set(currDest, currVal);

            if (currVal == BEdgeCursor.SENTINEL_MARKER) {
                // Fix pointer of vertex that goes into sentinel
                fixSentinel(currDest, index);
            }
            currDest = nextDest;
            currVal = nextVal;
            ++index;
            currEdge.at(edges.getEdges(), index);
        }

        if (index < edges.getN() && currVal == BEdgeCursor.SENTINEL_MARKER) {
            fixSentinel(currDest, index);
        }

        if (index == edges.getN()) {
            index--;
            slideLeft(index);
            rVal = -1;
        }

        currEdge.at(edges.getEdges(), index);
        currEdge.set(currDest, currVal);
        return rVal;
    }
}
