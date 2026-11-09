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

import org.dfpl.tcc.rgraph.RVertex;

public class BVertex  implements RVertex {
    int id; // Vertex id
    long beginning; // Beginning of the vertex
    long end; // End of the vertex
    int neighborCount; // Number of neighbors

    public BVertex(int id, long beginning, long end, int neighborCount) {
        this.id = id;
        this.beginning = beginning;
        this.end = end;
        this.neighborCount = neighborCount;
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
    public void setBeginning(long beginning) {
        this.beginning = beginning;
    }

    @Override
    public void setEnd(long end) {
        this.end = end;
    }

    @Override
    public void setNeighborCount(int neighborCount) {
        this.neighborCount = neighborCount;
    }

    @Override
    public String toString() {
        return "PVertex{" + "id=" + id + ", beginning=" + beginning + ", end=" + end + ", neighborCount=" + neighborCount + "}";
    }
}
