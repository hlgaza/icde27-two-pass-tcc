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

public class TimePeriod implements Time {

	private final long startTime;
	private final long finishTime;

	public TimePeriod(long startTime, long finishTime) {
		if (startTime == finishTime) {
			throw new IllegalArgumentException("Use TimeInstant");
		} else {
			this.startTime = startTime;
			this.finishTime = finishTime;
		}
	}

	@Override
	public String toString() {
		return "(" + startTime + "," + finishTime + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TimePeriod oTime))
			return false;
		return startTime == oTime.getStartTime() && finishTime == oTime.getFinishTime();
	}

	@Override
	public long getTime() {
		return startTime;
	}

	@Override
	public long getStartTime() {
		return startTime;
	}

	@Override
	public long getFinishTime() {
		return finishTime;
	}

	@Override
	public boolean checkTemporalRelation(Time t, TemporalRelation tr) {
		if (t instanceof TimePeriod tp) {
			return switch (tr) {
				case isBefore -> finishTime < tp.getStartTime();
				case isAfter -> startTime > tp.getFinishTime();
				case meets -> finishTime == tp.getStartTime();
				case isMetBy -> startTime == tp.getFinishTime();
				case overlapsWith -> startTime < tp.getStartTime() && finishTime > tp.getStartTime()
						&& finishTime < tp.getFinishTime();
				case isOverlappedBy -> startTime > tp.getStartTime() && startTime < tp.getFinishTime()
						&& finishTime > tp.getFinishTime();
				case starts -> startTime == tp.getStartTime() && finishTime < tp.getFinishTime();
				case isStartedBy -> startTime == tp.getStartTime() && finishTime > tp.getFinishTime();
				case during -> startTime > tp.getStartTime() && finishTime < tp.getFinishTime();
				case contains -> startTime < tp.getStartTime() && finishTime > tp.getFinishTime();
				case finishes -> startTime > tp.getStartTime() && finishTime == tp.getFinishTime();
				case isFinishedBy -> startTime < tp.getStartTime() && finishTime == tp.getFinishTime();
				case cotemporal -> startTime == tp.getStartTime() && finishTime == tp.getFinishTime();
			};
		} else {
			TimeInstant ti = (TimeInstant) t;
			return switch (tr) {
				case isBefore -> finishTime < ti.getTime();
				case isAfter -> startTime > ti.getTime();
				case meets, isFinishedBy -> finishTime == ti.getTime();
				case isMetBy, isStartedBy -> startTime == ti.getTime();
				case contains -> startTime < ti.getTime() && finishTime > ti.getTime();
				default -> false;
			};
		}
	}

	@Override
	public TemporalRelation getTemporalRelation(Time time) {
		if (time instanceof TimeInstant) {
			if (finishTime < time.getTime())
				return TemporalRelation.isAfter;
			if (startTime > time.getTime())
				return TemporalRelation.isBefore;
			if (startTime == time.getTime())
				return TemporalRelation.starts;
			if (finishTime == time.getTime())
				return TemporalRelation.finishes;
			if (startTime < time.getTime() && finishTime > time.getTime())
				return TemporalRelation.during;
		}

		if (time instanceof TimePeriod) {
			if (startTime > time.getFinishTime())
				return TemporalRelation.isBefore;
			if (finishTime < time.getStartTime())
				return TemporalRelation.isAfter;
			if (finishTime == time.getStartTime())
				return TemporalRelation.isMetBy;
			if (startTime == time.getFinishTime())
				return TemporalRelation.meets;
			if (startTime < time.getStartTime() && finishTime > time.getStartTime()
					&& finishTime < time.getFinishTime())
				return TemporalRelation.isOverlappedBy;
			if (startTime > time.getStartTime() && startTime < time.getFinishTime()
					&& finishTime > time.getFinishTime())
				return TemporalRelation.overlapsWith;
			if (startTime == time.getStartTime() && finishTime < time.getFinishTime())
				return TemporalRelation.isStartedBy;
			if (startTime == time.getStartTime() && finishTime > ((TimePeriod) time).finishTime)
				return TemporalRelation.starts;
			if (startTime > time.getStartTime() && finishTime < time.getFinishTime())
				return TemporalRelation.contains;
			if (startTime < time.getStartTime() && finishTime > time.getFinishTime())
				return TemporalRelation.during;
			if (startTime > time.getStartTime() && finishTime == time.getFinishTime())
				return TemporalRelation.isFinishedBy;
			if (startTime < time.getStartTime() && finishTime == time.getFinishTime())
				return TemporalRelation.finishes;
			if (this.equals(time))
				return TemporalRelation.cotemporal;
		}
		return null;
	}

	public long getDuration() {
		return finishTime - startTime;
	}
}
