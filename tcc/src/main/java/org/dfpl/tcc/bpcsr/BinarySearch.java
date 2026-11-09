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

import java.lang.foreign.MemorySegment;

public class BinarySearch {
    public static long compute(BEdgeList list, int elemDst, long start, long end) {
        MemorySegment edges = list.getEdges();
        BEdgeCursor edge = new BEdgeCursor();
        while (start + 1 < end) {
            long mid = (start + end) / 2;

            edge.at(edges, mid);
            long change = 1L;
            long check = mid;

            boolean flag = true;
            while (edge.getValue() == BEdgeCursor.NULL_MARKER && flag) {
                flag = false;
                check = mid + change;
                if (check < end) {
                    flag = true;
                    edge.at(edges, check);
                    if (edge.getValue() != BEdgeCursor.NULL_MARKER)
                        break;
                }
                check = mid - change;
                if (check >= start) {
                    flag = true;
                    edge.at(edges, check);
                }
                change++;
            }

            if (edge.getValue() == BEdgeCursor.NULL_MARKER || start == check || end == check) {
                if (edge.getValue() != BEdgeCursor.NULL_MARKER && start == check && elemDst <= edge.getDest())
                    return check;
                return mid;
            }

            // if elem is found, return
            if (elemDst == edge.getDest())
                return check;
            else if (elemDst < edge.getDest())
                end = check;
            else
                start = check;
        }

        if (end < start)
            start = end;
        edge.at(edges, start);
        if (elemDst <= edge.getDest() && edge.getValue() != BEdgeCursor.NULL_MARKER)
            return start;
        return end;
    }
}
