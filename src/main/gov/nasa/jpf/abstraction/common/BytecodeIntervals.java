/*
 * Copyright (C) 2015, Charles University in Prague.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.nasa.jpf.abstraction.common;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BytecodeIntervals extends BytecodeRange {
    Set<BytecodeInterval> intervals;

    public BytecodeIntervals(Set<BytecodeInterval> intervals) {
        this.intervals = intervals;
    }

    public int getMin() {
        int min = intervals.iterator().next().getMin();

        for (BytecodeInterval i : intervals) {
            if (min > i.getMin()) {
                min = i.getMin();
            }
        }

        return min;
    }

    public int getMax() {
        int max = intervals.iterator().next().getMax();

        for (BytecodeInterval i : intervals) {
            if (max < i.getMax()) {
                max = i.getMax();
            }
        }

        return max;
    }

    @Override
    public BytecodeRange merge(BytecodeInterval i) {
        BytecodeInterval overlapping = null;
        boolean contained = false;

        for (BytecodeInterval interval : intervals) {
            if (interval.overlaps(i)) {
                contained = interval.contains(i);
                overlapping = interval;

                break;
            }
        }

        if (contained) {
            return this;
        }

        Set<BytecodeInterval> is = new HashSet<BytecodeInterval>();
        is.addAll(intervals);

        while (overlapping != null) {
            is.remove(overlapping);

            i = (BytecodeInterval) i.merge(overlapping);

            overlapping = null;

            for (BytecodeInterval interval : is) {
                if (interval.overlaps(i)) {
                    overlapping = interval;

                    break;
                }
            }
        }

        is.add(i);

        if (is.size() == 1) {
            return is.iterator().next();
        }

        return new BytecodeIntervals(is);
    }

    @Override
    public BytecodeRange merge(BytecodeIntervals is) {
        BytecodeRange r = this;

        for (BytecodeInterval i : is.intervals) {
            r = r.merge(i);
        }

        return r;
    }

    @Override
    public boolean contains(int pc) {
        for (BytecodeInterval i : intervals) {
            if (i.contains(pc)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        String ret = "";

        for (BytecodeInterval i : intervals) {
            if (ret.length() > 0) {
                ret += ",";
            }

            ret += i.toString();
        }

        return ret;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private Iterator<BytecodeInterval> intervals = BytecodeIntervals.this.intervals.iterator();
            private Iterator<Integer> interval = null;

            @Override
            public boolean hasNext() {
                if (interval == null) {
                    if (intervals.hasNext()) {
                        interval = intervals.next().iterator();
                    } else {
                        return false;
                    }
                }

                if (!interval.hasNext()) {
                    interval = null;

                    return hasNext();
                }

                return true;
            }

            @Override
            public Integer next() {
                if (interval == null) {
                    if (intervals.hasNext()) {
                        interval = intervals.next().iterator();
                    } else {
                        return null;
                    }
                }

                if (!interval.hasNext()) {
                    interval = null;

                    return next();
                }

                return interval.next();
            }

            @Override
            public void remove() {
                throw new RuntimeException("Removal not permitted");
            }
        };
    }
}
