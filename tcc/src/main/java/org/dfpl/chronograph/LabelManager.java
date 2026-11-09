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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manages the integer representation of labels
 * 
 * @author Haifa Gaza, MS student, DFPL, Department of Sofware, Sejong
 *         University
 * 
 * @author Jaewook Byun, Ph.D., Assistant Professor, DFPL, Department of
 *         Software, Sejong University
 *
 */
public class LabelManager {
	// TODO
	ArrayList<String> intIds;
	// Contains the integer representation of string values
	HashMap<String, Integer> stringIds;

	/**
	 * Initializes the collection of integer ids, string ids, and freed ids
	 */
	public LabelManager() {
		this.intIds = new ArrayList<>();
		this.stringIds = new HashMap<>();
	}

	/**
	 * Sets the integer value of a string id
	 * 
	 * @param id the string id to be represented
	 * @return the integer id
	 */
	public Integer add(String id) {
		// Check existence of id in stringIds
		if (stringIds.containsKey(id))
			return stringIds.get(id);

		Integer intId = intIds.size();
		intIds.add(id);
		stringIds.put(id, intId);

		return intId;
	}

	/**
	 * Returns the string id of the integer id
	 * 
	 * @param id the integer id to be retrieved
	 * 
	 * @return the string id representation or {@code null} if not mapped
	 */
	public String getId(Integer id) {
		if (intIds.size() <= id)
			return null;

		return intIds.get(id);
	}

	/**
	 * Returns the integer representation of a string id if present
	 * 
	 * @param id the id to be retrieved
	 * @return the integer representation of the id, or {@code null} if not mapped
	 */
	public Integer getId(String id) {
		if (!stringIds.containsKey(id))
			return null;
		return stringIds.get(id);
	}

	public void printInfo() {
		System.out.println("Ints: " + intIds);
		System.out.println("Strings: " + stringIds);
		System.out.println();
	}
}