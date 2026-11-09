/**
 * Portions Copyright (c) 2026 Haifa Gaza <br>
 * Modified and translated from C++ to Java
 * <p>
 * Original Work:
 * Copyright (C) 2019 Lijun Chang [<a href="https://github.com/LijunChang/MC-BRB">GitHub</a>] <br>
 * Licensed under GPL-3.0 License (see below)
 * <p>
 * GPL-3.0 License
 * <p>
 * Copyright (C) 2019 Lijun Chang
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
package org.dfpl.tcc.clique.chang;

import org.dfpl.tcc.rgraph.REdge;
import org.dfpl.tcc.rgraph.RGraph;
import org.dfpl.tcc.rgraph.RVertex;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class Color {
    static Logger log = Logger.getLogger(Color.class.getName());

    public static int countColors(Collection<Integer> ids, int[] colors, boolean[] visited) {
        Arrays.fill(visited, false);
        int colorBound = 0;

        for (int id : ids) {
            if (visited[colors[id]]) continue;

            visited[colors[id]] = true;
            ++colorBound;
        }
        for (int id : ids) {
            visited[colors[id]] = false;
        }
        return colorBound;
    }

    public static <V extends RVertex, E extends REdge<?>> int color(RGraph<V, E> rGraph, List<Integer> ids, int idSize, int ogSize, int[] colors,
                                                                    boolean[] visited, int startIdx, int startColor) {
        assert startColor <= idSize && ogSize >= idSize;

        for (int uIdx = 0; uIdx < ogSize; uIdx++) {
            colors[ids.get(uIdx)] = RGraph.NULL_VALUE;
        }

        // Pre-color vertices near the end of the list
        for (int uIdx = idSize - startColor; uIdx < idSize; uIdx++)
            colors[ids.get(uIdx)] = idSize - uIdx - 1;

        int maxColor = 0;
        for (int uIdx = idSize - startColor; uIdx > startIdx; uIdx--) {
            int uId = ids.get(uIdx - 1);

            rGraph.forEachNeighbor(uId, vId -> {
                int color = colors[vId];
                if (color != RGraph.NULL_VALUE)
                    visited[color] = true;
            });

            for (int j = 0; ; j++) {
                if (!visited[j]) { // Find an available color
                    colors[uId] = j; // Assign the color to the current vertex
                    if (j > maxColor)
                        maxColor = j;
                    break; // Stop searching
                }
            }
            // Reset the color markers

            rGraph.forEachNeighbor(uId, vId -> {
                int color = colors[vId];
                if (color != RGraph.NULL_VALUE)
                    visited[color] = false;
            });
        }

        return maxColor + 1;
    }

    public static int colorMatrix(DenseGraph graph, KCFClique kcfClique, List<Integer> ids,
                                  List<Integer> colors, int startColor, int threshold) {
        Arrays.fill(kcfClique.heads, -1);
        Arrays.fill(kcfClique.tempIds, -1);
        Arrays.fill(kcfClique.nexts, -1);
        Arrays.fill(kcfClique.idxs, -1);

        assert startColor > 0;
        assert startColor <= ids.size();

        for (int i = ids.size() - startColor; i < ids.size(); i++) {
            int c = ids.size() - i - 1;
            kcfClique.idxs[ids.get(i)] = c;
            kcfClique.tempIds[i] = ids.get(i);
            kcfClique.nexts[i] = kcfClique.heads[c];
            kcfClique.heads[c] = i;
        }

        for (int i = ids.size() - startColor; i > 0; ) {
            int uId = ids.get(--i);

            BitSet neighbors = graph.getNeighbors(uId);
            kcfClique.idxs[uId] = -1;

            // Strategy 1: Simple greedy coloring
            for (int j = 0; j < threshold; j++) {
                boolean iColorOk = true;
                for (int k = kcfClique.heads[j]; k != -1; k = kcfClique.nexts[k]) {
                    if (neighbors.get(kcfClique.tempIds[k])) {
                        iColorOk = false;
                        break;
                    }
                }
                if (iColorOk) {
                    kcfClique.idxs[uId] = j;
                    kcfClique.tempIds[i] = ids.get(i);
                    kcfClique.nexts[i] = kcfClique.heads[j];
                    kcfClique.heads[j] = i;
                    break;
                }
            }

            if (kcfClique.idxs[uId] < threshold && kcfClique.idxs[uId] != -1) continue;

            // Strategy 2: Color exchange
            for (int j = 0; j < threshold; j++) {
                int cnt = 0, idx = -1;

                for (int k = kcfClique.heads[j]; k != -1; k = kcfClique.nexts[k]) {
                    if (neighbors.get(kcfClique.tempIds[k])) {
                        ++cnt;
                        if (cnt == 1) idx = k;
                        else break;
                    }
                }

                assert cnt > 0;

                if (cnt != 1) continue;
                BitSet ttNeighbors = graph.getNeighbors(kcfClique.tempIds[idx]);

                for (int ii = threshold; ii > 0; ) {
                    --ii;
                    if (ii == j) continue;

                    boolean isColorOk = true;
                    for (int k = kcfClique.heads[ii]; k != -1; k = kcfClique.nexts[k]) {
                        if (ttNeighbors.get(kcfClique.tempIds[k])) {
                            isColorOk = false;
                            break;
                        }
                    }
                    if (isColorOk) {
                        kcfClique.idxs[kcfClique.tempIds[idx]] = ii;
                        kcfClique.tempIds[i] = kcfClique.tempIds[idx];
                        kcfClique.nexts[i] = kcfClique.heads[ii];
                        kcfClique.heads[ii] = i;

                        kcfClique.idxs[uId] = j;
                        kcfClique.tempIds[idx] = ids.get(i);
                        break;
                    }
                }
                if (kcfClique.idxs[uId] < threshold && kcfClique.idxs[uId] != -1) break;
            }

            if (kcfClique.idxs[uId] < threshold && kcfClique.idxs[uId] != -1) continue;


            // Strategy 3: Assign a new color
            for (int j = threshold; ; j++) {
                boolean isColorOk = true;
                for (int k = kcfClique.heads[j]; k != -1; k = kcfClique.nexts[k]) {
                    if (neighbors.get(kcfClique.tempIds[k])) {
                        isColorOk = false;
                        break;
                    }
                }
                if (isColorOk) {
                    kcfClique.idxs[uId] = j;
                    kcfClique.tempIds[i] = ids.get(i);
                    kcfClique.nexts[i] = kcfClique.heads[j];
                    kcfClique.heads[j] = i;
                    break;
                }
            }
        }

        int maxColor = 0;
        for (int i = 0; i < ids.size(); i++) {
            colors.set(i, kcfClique.idxs[ids.get(i)]);

            if (colors.get(i) > maxColor) maxColor = colors.get(i);
        }

        return maxColor + 1;
    }
}
