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

public class TimeInstant implements Time {
	protected long time;

	public TimeInstant(long time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return Long.toString(time);
	}

	@Override
	public long getStartTime() {
		return time;
	}

	@Override
	public long getFinishTime() {
		return time;
	}

	@Override
	public long getTime() {
		return time;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TimeInstant oTime))
			return false;
		return time == oTime.getTime();
	}

	@Override
	public boolean checkTemporalRelation(Time t, TemporalRelation tr) {
		if (t instanceof TimePeriod tp) {
			return switch (tr) {
			case isBefore -> time < tp.getStartTime();
			case isAfter -> time > tp.getFinishTime();
			case meets, starts -> time == tp.getStartTime();
			case isMetBy, finishes -> time == tp.getFinishTime();
			case during -> time > tp.getStartTime() && time < tp.getFinishTime();
			default -> false;
			};
		} else {
			TimeInstant ti = (TimeInstant) t;
			return switch (tr) {
			case isBefore -> time < ti.getStartTime();
			case isAfter -> time > ti.getStartTime();
			case cotemporal -> time == ti.getStartTime();
			default -> false;
			};
		}
	}

	@Override
	public TemporalRelation getTemporalRelation(Time time) {
		if (time instanceof TimeInstant) {
			if (this.getTime() < time.getTime())
				return TemporalRelation.isAfter;
			if (this.getTime() > time.getTime())
				return TemporalRelation.isBefore;
			if (this.equals(time))
				return TemporalRelation.cotemporal;
		} else if (time instanceof TimePeriod) {
			if (this.getTime() < time.getStartTime())
				return TemporalRelation.isAfter;
			if (this.getTime() > time.getFinishTime())
				return TemporalRelation.isBefore;
			if (this.getTime() == time.getStartTime())
				return TemporalRelation.isStartedBy;
			if (this.getTime() == time.getFinishTime())
				return TemporalRelation.isFinishedBy;
			if (this.getTime() > time.getStartTime() && this.getTime() < time.getFinishTime())
				return TemporalRelation.contains;
		}
		return null;
	}

	@Override
	public long getDuration() {
		return 0;
	}
}
