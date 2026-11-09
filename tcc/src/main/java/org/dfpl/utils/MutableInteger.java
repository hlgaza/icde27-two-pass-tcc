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
 * <p>
 * For more information about this program, see <https://github.com/hlgaza/two-tbfs-tcc> or reach out to haifalapuz at gmail dot com.
 */
package org.dfpl.utils;

public class MutableInteger {
    public int value;

    public MutableInteger(int value) {
        this.value = value;
    }

    public void increment() {
        this.value++;
    }

    public int get() {
        return this.value;
    }

    public int getAndIncrement() {
        int oldValue = this.value;
        this.value++;
        return oldValue;
    }

    public int decrementAndGet() {
        this.value--;
        return this.value;
    }

    public void set(int i) {
        this.value = i;
    }

    public int addAndGet(int size) {
        this.value += size;
        return this.value;
    }
}
