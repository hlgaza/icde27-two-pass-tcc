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
package org.dfpl.chronograph;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Manages the integer representation of vertex IDs
 * 
 * @author Haifa Gaza, MS student, DFPL, Department of Sofware, Sejong
 *         University
 * 
 * @author Jaewook Byun, Ph.D., Assistant Professor, DFPL, Department of
 *         Software, Sejong University
 *
 */
public class IdManager {

	private HashMap<String, Integer> idToIdx;
	private String[] idxToID;

	/**
	 * Initializes the collection of integer ids, string ids
	 */
	public IdManager(HashSet<String> idSet) {
		idxToID = new String[idSet.size()];
		idToIdx = new HashMap<>();
		int cnt = 0;
		for (String id : idSet) {
			idToIdx.put(id, cnt);
			idxToID[cnt] = id;
			cnt++;
		}
	}

	/**
	 * Initializes the collection of vertex IDs
	 */
	public IdManager(HashMap<String, Integer> idSet) {
		idxToID = new String[idSet.size()+1];
		idToIdx = idSet;

		idSet.forEach((id, index) -> idxToID[index] = id);
	}

	public Integer get(String vertexId) {
		return idToIdx.get(vertexId);
	}

	/**
	 * Returns the vertex ID mapped to the index
	 *
	 * @param vertexIdx the vertex index
	 * @return the string vertex ID
	 */
	public String get(Integer vertexIdx) {
		return idxToID[vertexIdx];
	}

	public Integer size() {
		return idToIdx.size();
	}

	public HashMap<String, Integer> idToIdxMap() {
		return idToIdx;
	}

	public String[] idxToID() {
		return idxToID;
	}
}
