package org.dfpl.chronograph;
/**
 * Portions Copyright (c) Jaewook Byun
 * Modified class
 * <p>
 * BSD 3-Clause License
 * Copyright (c) 2009-Infinity, TinkerPop
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the TinkerPop nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TINKERPOP BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
import java.util.Comparator;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 *
 * @author Jaewook Byun, Assistant Professor, Halla University
 * <p>
 *         Data Frameworks and Platforms Laboratory (DFPL)
 *
 *         jaewook.byun@halla.ac.kr, bjw0829@kaist.ac.kr, bjw0829@gmail.com
 *
 */
public class Tokens {

	public enum TimeComparator implements Comparator<Time> {
		earlyFinishTimeShortDuration((t1, t2) -> {
			if (t1.getFinishTime() < t2.getFinishTime())
				return -1;
			else if (t1.getFinishTime() == t2.getFinishTime()) {
				if (t1.getDuration() < t2.getDuration())
					return -1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return +1;
			} else
				return +1;
		}), earlyFinishTimeLongDuration((t1, t2) -> {
			if (t1.getFinishTime() < t2.getFinishTime())
				return -1;
			else if (t1.getFinishTime() == t2.getFinishTime()) {
				if (t1.getDuration() < t2.getDuration())
					return +1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return -1;
			} else
				return +1;
		}), earlyStartTimeShortDuration((t1, t2) -> {
			if (t1.getStartTime() < t2.getStartTime())
				return -1;
			else if (t1.getStartTime() == t2.getStartTime()) {
				if (t1.getDuration() < t2.getDuration())
					return -1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return +1;
			} else
				return +1;
		}), earlyStartTimeLongDuration((t1, t2) -> {
			if (t1.getStartTime() < t2.getStartTime())
				return -1;
			else if (t1.getStartTime() == t2.getStartTime()) {
				if (t1.getDuration() < t2.getDuration())
					return 1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return -1;
			} else
				return +1;
		}), lateFinishTimeShortDuration((t1, t2) -> {
			if (t1.getFinishTime() < t2.getFinishTime())
				return +1;
			else if (t1.getFinishTime() == t2.getFinishTime()) {
				if (t1.getDuration() < t2.getDuration())
					return -1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return +1;
			} else
				return -1;
		}), lateFinishTimeLongDuration((t1, t2) -> {
			if (t1.getFinishTime() < t2.getFinishTime())
				return +1;
			else if (t1.getFinishTime() == t2.getFinishTime()) {
				if (t1.getDuration() < t2.getDuration())
					return +1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return -1;
			} else
				return -1;
		}), lateStartTimeShortDuration((t1, t2) -> {
			if (t1.getStartTime() < t2.getStartTime())
				return +1;
			else if (t1.getStartTime() == t2.getStartTime()) {
				if (t1.getDuration() < t2.getDuration())
					return -1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return +1;
			} else
				return -1;
		}), lateStartTimeLongDuration((t1, t2) -> {
			if (t1.getStartTime() < t2.getStartTime())
				return +1;
			else if (t1.getStartTime() == t2.getStartTime()) {
				if (t1.getDuration() < t2.getDuration())
					return +1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return -1;
			} else
				return -1;
		});

		private final Comparator<Time> comparator;

		TimeComparator(Comparator<Time> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Time o1, Time o2) {
			return comparator.compare(o1, o2);
		}
	}

	public enum EventComparator implements Comparator<Event> {
		earlyStartTimeShortDuration((t1, t2) -> {
			if (t1.getStartTime() < t2.getStartTime())
				return -1;
			else if (t1.getStartTime() == t2.getStartTime()) {
				if (t1.getDuration() < t2.getDuration())
					return -1;
				else if (t1.getDuration() == t2.getDuration())
					return 0;
				else
					return +1;
			} else
				return +1;
		});

		private final Comparator<Event> comparator;

		EventComparator(Comparator<Event> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Event o1, Event o2) {
			return comparator.compare(o1, o2);
		}
	}
}
