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
import java.util.logging.Logger;

public class MergePartitions {
    private static Logger log = Logger.getLogger(MergePartitions.class.getName());

    /**
     * Merges two ordered partitions of edges into a single ordered partition.
     * Partition 1: [low, mid)
     * Partition 2: [mid, high)
     *
     * @param edges the container of the edges
     * @param low   the low index of the first partition
     * @param mid   the last index of the first partition or the first index of the second partition
     * @param high  the last index of the second partition
     */
    public static void merge(MemorySegment edges, long low, long mid, long high) {
        long structSize = BEdgeCursor.EDGE_LAYOUT.byteSize();
        long totalElements = high - low;

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment tempEdges = arena.allocate(BEdgeCursor.EDGE_LAYOUT, totalElements);

            long i = low; // pointer for the left partition [low, mid)
            long j = mid; // pointer for the right partition [mid, high)
            long dest = 0L; // pointer for tempSegment

            while (i < mid && j < high) {
                long iOffset = i * structSize;
                long jOffset = j * structSize;

                int dstI = (int) BEdgeCursor.DST_HANDLE.get(edges, iOffset);
                int dstJ = (int) BEdgeCursor.DST_HANDLE.get(edges, jOffset);

                long destOffset = dest * structSize;
                if (dstI <= dstJ) {
                    copyStruct(edges, iOffset, tempEdges, destOffset);
                    i++;
                } else {
                    copyStruct(edges, jOffset, tempEdges, destOffset);
                    j++;
                }
                dest++;
            }

            long remainingElements = 0;
            // Copy the remaining elements from the left partition to the end of edges
            if (i < mid) {
                remainingElements = mid - i;
                MemorySegment.copy(edges, i * structSize, edges, (low + dest) * structSize, remainingElements * structSize);
            }

            // The remaining elements from the right partition are already in edges
            if (j < high) {
                remainingElements = high - j;
            }

            // Move the tempSegment to the edges segment
            MemorySegment.copy(tempEdges, 0L, edges, low * structSize, (totalElements - remainingElements) * structSize);
        }
    }

    /**
     * Merges three partitions of edges into a single ordered partition.
     * This assumes that there are gaps in the last partition.
     *
     * Partition 1: [p0, p1)
     * Partition 2: [p1, p2) **There may be gaps between partitions 2 and 3
     * Partition 3: [p3, p4) ***There could be gaps in partition 3
     *
     * @param edges the container of the edges
     * @param p0    the first index of the first partition
     * @param p1    the last index of the first partition, or the first index of the second partition
     * @param p2    the last index of the second partition (there might be gaps between partitions 2 and 3)
     * @param p3    the first index of the third partition
     * @param p4    the last index of the third partition
     */
    public static void merge(MemorySegment edges, long p0, long p1, long p2, long p3, long p4) {
        long structSize = BEdgeCursor.EDGE_LAYOUT.byteSize();
        long totalElements = p4 - p0;

        if (totalElements <= 1) return;

        try (Arena tempArena = Arena.ofConfined()) {
            MemorySegment tempEdges = tempArena.allocate(BEdgeCursor.EDGE_LAYOUT, totalElements);

            long i = p0; // pointer of the first partition
            long j = p1; // pointer of the second partition
            long k = p3; // pointer of the third partition
            long dest = 0L; // target index of the tempEdges

            while (i < p1 && j < p2 && k < p4) {
                byte valK = (byte) BEdgeCursor.VAL_HANDLE.get(edges, k * structSize);
                if (valK == BEdgeCursor.NULL_MARKER) {
                    k++;
                    continue;
                }

                long iOffset = i * structSize;
                long jOffset = j * structSize;
                long kOffset = k * structSize;

                int dstI = (int) BEdgeCursor.DST_HANDLE.get(edges, iOffset);
                int dstJ = (int) BEdgeCursor.DST_HANDLE.get(edges, jOffset);
                int dstK = (int) BEdgeCursor.DST_HANDLE.get(edges, kOffset);

                long destOffset = dest * structSize;
                if (dstI <= dstJ && dstI <= dstK) {
                    copyStruct(edges, iOffset, tempEdges, destOffset);
                    i++;
                } else if (dstJ <= dstI && dstJ <= dstK) {
                    copyStruct(edges, jOffset, tempEdges, destOffset);
                    j++;
                } else {
                    copyStruct(edges, kOffset, tempEdges, destOffset);
                    k++;
                }

                // Increment the dest index, but take care of the gap in the reference edges
                dest = moveDestIndex(edges, structSize, p0, dest, totalElements);
            }

            // Merge elements from the two remaining partitions
            while (i < p1 && j < p2) { // Check partition 1 and partition 2
                long[] mergeResult = mergeRemaining(edges, i, j, dest, structSize, tempEdges);
                i = mergeResult[0];
                j = mergeResult[1];
                dest = moveDestIndex(edges, structSize, p0, dest, totalElements);
            }

            while (i < p1 && k < p4) { // Check partition 1 and partition 3
                byte valK = (byte) BEdgeCursor.VAL_HANDLE.get(edges, k * structSize);
                if (valK == BEdgeCursor.NULL_MARKER) {
                    k++;
                    continue;
                }

                long[] mergeResult = mergeRemaining(edges, i, k, dest, structSize, tempEdges);
                i = mergeResult[0];
                k = mergeResult[1];
                dest = moveDestIndex(edges, structSize, p0, dest, totalElements);
            }

            while (j < p2 && k < p4) { // Check partition 2 and partition 3
                byte valK = (byte) BEdgeCursor.VAL_HANDLE.get(edges, k * structSize);
                if (valK == BEdgeCursor.NULL_MARKER) {
                    k++;
                    continue;
                }

                long[] mergeResult = mergeRemaining(edges, j, k, dest, structSize, tempEdges);
                j = mergeResult[0];
                k = mergeResult[1];
                dest = moveDestIndex(edges, structSize, p0, dest, totalElements);
            }

            // Flush any remaining elements from the last partition
            while (i < p1) {
                copyStruct(edges, i * structSize, tempEdges, dest * structSize);
                i++;
                dest = moveDestIndex(edges, structSize, p0, dest, totalElements);
            }

            while (j < p2) {
                copyStruct(edges, j * structSize, tempEdges, dest * structSize);
                j++;
                dest = moveDestIndex(edges, structSize, p0, dest, totalElements);
            }

            // Partition 3 is already in place
            long remainingElements = 0;
            if (k < p4) {
                remainingElements = p4 - k;
            }

            MemorySegment.copy(tempEdges, 0L, edges, p0 * structSize, (totalElements - remainingElements) * structSize);
        }
    }

    private static long[] mergeRemaining(MemorySegment sourceEdges, long p0, long p1, long dest,
                                         long structSize, MemorySegment destEdges) {
        long p0Offset = p0 * structSize;
        long p1Offset = p1 * structSize;

        int dstP0 = (int) BEdgeCursor.DST_HANDLE.get(sourceEdges, p0Offset);
        int dstP1 = (int) BEdgeCursor.DST_HANDLE.get(sourceEdges, p1Offset);

        long destOffset = dest * structSize;
        if (dstP0 <= dstP1) {
            copyStruct(sourceEdges, p0Offset, destEdges, destOffset);
            p0++;
        } else {
            copyStruct(sourceEdges, p1Offset, destEdges, destOffset);
            p1++;
        }

        return new long[]{p0, p1};
    }

    private static long moveDestIndex(MemorySegment edges, long structSize, long startIndex, long dest, long end) {
        dest++;
        long destOffset;
        byte referenceVal;

        while (dest < end) {
            destOffset = (startIndex + dest) * structSize;
            referenceVal = (byte) BEdgeCursor.VAL_HANDLE.get(edges, destOffset);
            if (referenceVal != BEdgeCursor.NULL_MARKER)
                break;
            dest++;
        }
        return dest;
    }

    private static void copyStruct(MemorySegment src, long srcOffset, MemorySegment dest, long destOffset) {
        int dst = (int) BEdgeCursor.DST_HANDLE.get(src, srcOffset);
        byte val = (byte) BEdgeCursor.VAL_HANDLE.get(src, srcOffset);

        BEdgeCursor.DST_HANDLE.set(dest, destOffset, dst);
        BEdgeCursor.VAL_HANDLE.set(dest, destOffset, val);
    }
}
