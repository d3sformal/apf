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
package gov.nasa.jpf.abstraction;

// Taken from SVCOMP

public class ReverseArrayTest extends BaseTest {
    private static final int N = 5;

    @Test
    @Config(items = {
        "+panda.refinement=true"
    })
    public static void test() {
        int[] a = new int[N];
        int[] b = new int[N];

        for (int i = 0; i < N; ++i) {
            b[i] = a[N - i - 1];
        }

        for (int j = 0; j < N; ++j) {
            assert a[j] == b[N - j - 1];
        }
    }
};
