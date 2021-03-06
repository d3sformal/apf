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
package gov.nasa.jpf.abstraction.util;

import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTask;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTaskMirror;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

public class AntTestTask extends JUnitTask {

    private int total = 0;
    private List<String> errorTests = new LinkedList<String>();
    private List<String> failureTests = new LinkedList<String>();
    private List<String> timedOutTests = new LinkedList<String>();
    private List<String> crashedTests = new LinkedList<String>();

    public AntTestTask() throws Exception {
    }

    @Override
    protected void actOnTestResult(JUnitTask.TestResultHolder result, JUnitTest test, String name) {
        ++total;

        switch (result.exitCode) {
            case JUnitTaskMirror.JUnitTestRunnerMirror.SUCCESS:
                break;
            case JUnitTaskMirror.JUnitTestRunnerMirror.FAILURES:
                if (!failureTests.contains(test.getName())) {
                   failureTests.add(test.getName());
                }
                break;
            case JUnitTaskMirror.JUnitTestRunnerMirror.ERRORS:
                if (!errorTests.contains(test.getName())) {
                   errorTests.add(test.getName());
                }
                break;
        }

        if (result.timedOut) {
            if (!timedOutTests.contains(test.getName())) {
                timedOutTests.add(test.getName());
            }
        }

        if (result.crashed) {
            if (!crashedTests.contains(test.getName())) {
                crashedTests.add(test.getName());
            }
        }

        super.actOnTestResult(result, test, name);

        System.out.println();
        System.out.println();
        System.out.println();
    }

    // UGLY to do this in cleanup but it is the only straightforward way to do so in current JUnitTask implementation
    @Override
    protected void cleanup() {
        super.cleanup();

        System.out.println("Total: " + total + "; Failed: " + (failureTests.size() + errorTests.size() + crashedTests.size()) + "; Timed out: " + timedOutTests.size());

        if (!failureTests.isEmpty() || !errorTests.isEmpty() || !crashedTests.isEmpty()) {
            System.out.println("Failed tests:");

            for (String testName : failureTests) {
                System.out.println("\t" + testName);
            }

            for (String testName : errorTests) {
                System.out.println("\t" + testName);
            }

            for (String testName : crashedTests) {
                System.out.println("\t" + testName);
            }
        }

        if (!timedOutTests.isEmpty()) {
            System.out.println("Timed out tests:");

            for (String testName : timedOutTests) {
                System.out.println("\t" + testName);
            }
        }
    }
}
