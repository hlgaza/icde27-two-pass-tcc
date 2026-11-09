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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * A KCF-Clique object to hold the KCF-clique algorithm data.
 */
public class KCFClique {
    List<Integer> idList;
//    int cliqueSize;

    int[] clique;
    List<Integer> contractions;
    List<int[]> changes;

    int firstId;

    int[] heads;
    int[] tempIds;
    int[] nexts;
    int[] idxs;

    boolean[] vis;

    public static Logger log = Logger.getLogger(KCFClique.class.getName());


    public KCFClique(int firstId, List<Integer> idList, int upperBound) {
        this.firstId = firstId;
        this.idList = idList;

        this.clique = new int[upperBound];
        this.clique[0] = firstId;
        this.contractions = new ArrayList<>();
        this.changes = new ArrayList<>();

        this.heads =  new int[idList.size()];
        Arrays.fill(heads, -1);
        this.tempIds = new int[idList.size()];
        this.nexts = new int[idList.size()];

        this.idxs = new int[idList.size()];

        this.vis = new boolean[idList.size()];
    }
}
