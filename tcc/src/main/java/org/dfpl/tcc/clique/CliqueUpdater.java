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
package org.dfpl.tcc.clique;

import java.util.HashSet;
import java.util.Set;

public class CliqueUpdater {

    private Set<Integer> maxClique = new HashSet<>();

    private Integer srcId;

    public void update(Set<Integer> newClique, Integer srcId) {
        if (maxClique == null || maxClique.size() < newClique.size()) {
            maxClique = newClique;
            this.srcId = srcId;
        }
    }

    public Set<Integer> getMaxClique() {
        return maxClique;
    }

    public Integer getSrcId() {
        return srcId;
    }

}
