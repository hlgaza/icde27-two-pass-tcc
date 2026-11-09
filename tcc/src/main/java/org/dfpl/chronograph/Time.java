/**
 * Apache 2.0 License
 * Copyright 2024 Jaewook Byun
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dfpl.chronograph;

/**
 * The in-memory implementation of temporal graph database.
 *
 * @author Jaewook Byun, Ph.D., Assistant Professor, DFPL, Department of
 *         Software, Sejong University
 *
 * @author Haifa Gaza, Ph.D., Student, DFPL, Sejong University
 *
 *         Gaza, Haifa, and Jaewook Byun. "Kairos: Enabling prompt monitoring of
 *         information diffusion over temporal networks." IEEE Transactions on
 *         Knowledge and Data Engineering (2023).
 *
 *         Byun, Jaewook. "Enabling time-centric computation for efficient
 *         temporal graph traversals from multiple sources." IEEE Transactions
 *         on Knowledge and Data Engineering (2020).
 *
 *         Byun, Jaewook, Sungpil Woo, and Daeyoung Kim. "Chronograph: Enabling
 *         temporal graph traversals for efficient information diffusion
 *         analysis over time." IEEE Transactions on Knowledge and Data
 *         Engineering 32.3 (2019): 424-437.
 *
 */
public interface Time extends Comparable<Time> {

	/**
	 * Compares this time with the specified time for order. Returns a negative
	 * integer, zero, or a positive integer as this time is less than, equal to, or
	 * greater than the specified time.
	 *
	 * @param o
	 *            the time to be compared
	 * @return a negative integer, zero, or a positive integer as this time is less
	 *         than, equal to, or greater than the specified time.
	 */
	default int compareTo(Time o) {
		if (this.getTime() < o.getTime())
			return -1;
		else if (this.getTime() > o.getTime())
			return +1;
		else
			return 0;
	}

	/**
	 * return a time in time-instant or a start time in time-period used for
	 * compareTo, due to disjointness, it is not ambiguous.
	 *
	 * @return a time in time-instant or a start time in time-period
	 */
	long getTime();

	/**
	 * @return a time in time-instant or a start time in time-period
	 */
	long getStartTime();

	/**
	 * @return a time in time-instant or a finish time in time-period
	 */
	long getFinishTime();

	/**
	 * @return finishTime - startTime
	 */
	long getDuration();

	/**
	 * Checks whether a temporal relation is true between two time instances e.g.
	 * this: 5, time: 5, 9, tr: starts, return true this (5) tr (starts) time (5,9)
	 *
	 * @param time
	 *            the time to be compared
	 * @param tr
	 *            the temporal relation to check
	 */
	boolean checkTemporalRelation(Time time, TemporalRelation tr);

	/**
	 * get the temporal relation for time from this
	 * <p>
	 * e.g., this: 3 time: 5 return isAfter
	 *
	 * @param time
	 *            the time to be checked
	 * @return the temporal relation for time from this
	 */
	TemporalRelation getTemporalRelation(Time time);
}
