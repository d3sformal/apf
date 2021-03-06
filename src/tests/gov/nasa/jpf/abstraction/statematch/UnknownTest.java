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
package gov.nasa.jpf.abstraction.statematch;

import gov.nasa.jpf.abstraction.Test;
import gov.nasa.jpf.abstraction.Verifier;

public class UnknownTest extends StateMatchingTest {
    public UnknownTest() {
        config.add("+panda.refinement=true");
        config.add("+panda.branch.prune_infeasible=true");
    }

    @Test
    public static void test1() {
        int i = Verifier.unknownInt();

        // Without matching already explored abstract traces
        // 1. Comes with 0
        // 2. Generates unknown = 1 ... to get to now-disabled then-branch
        // 3. Comes with 1
        // 4. Generates unknown = -1 ... to get to now-disabled else-branch
        // 5. Comes with -1
        // 6. Cannot generate more models for i
        if (i == 1) {
        }

        assertRevisitedAtLeast(1);
        assertVisitedAtMost(2);
    }

    @Test
    public static void test2() {
        int i = Verifier.unknownInt();

        // Test with the special value 0 which is the default
        // Use < to avoid one of the branches having only one model
        // Both branches having multiple models -> higher chance Panda might diverge
        if (i < 0) {
        }

        assertRevisitedAtLeast(1);
        assertVisitedAtMost(2);
    }

    @Test
    public static void test3() {
        int i = Verifier.unknownInt(); // Should register choice (starting with for example 0) and mark the constant as NONDET

        int j = i + 2;

        if (i > 498) { // Inconsistent branching will take Trace Formula and derive such a value of NONDET different from previously derived values
            assert j > 500;
        }

        // There will be one concrete path avoiding the body of the IF
        //   lets say its for i: 0
        //
        // There will be exactly one concrete path hitting the body of the IF
        //   lets say its for i: 499

        assertRevisitedAtLeast(1);
        assertVisitedAtMost(2);
    }

    @Test
    public static void test4() {
        int i = Verifier.unknownInt();

        switch (i) {
            case 0:
            case 42:
            case 1024:
            case -5:
            case Integer.MAX_VALUE:
            case -Integer.MAX_VALUE:
                System.out.println(i);
                break;
            default:
                System.out.println("Default: " + i);
                break;
        }

        assertRevisitedAtLeast(6);
        assertVisitedAtMost(7);
    }

    @Test
    public static void test5() {
        int i = Verifier.unknownInt();
        int[] a = new int[5];

        if (0 <= i && i < a.length) { // -1, ..., 5
            int c = a[i]; // 0, 1, 2, 3, 4

            System.out.println(i);
        }

        // -1, 0, 1, 2, 3, 4, 5

        assertRevisitedAtLeast(6);
        assertVisitedAtMost(7);
    }

};
