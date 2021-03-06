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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Stack;

import static java.nio.file.StandardCopyOption.*;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.search.Search;

import gov.nasa.jpf.abstraction.PredicateAbstraction;

public class BacktrackedLogger extends ListenerAdapter {
    private static PrintStream originalStdOut = System.out;

    public static PrintStream getOriginalStdOut() {
        return originalStdOut;
    }

    private Stack<Long> positions = new Stack<Long>();
    private FileOutputStream fos;
    private FileChannel fc;

    private String PREFIX = "output/";
    private String WORKING = ".pathlog";
    private String FINAL;
    private int i = 0;
    private boolean frozen = true;

    public BacktrackedLogger(Config config) {
        positions.push(0L);

        WORKING = getName(config.getTarget()) + WORKING;
        FINAL = WORKING + ".";

        try {
            File dir = new File(PREFIX);

            if (!dir.exists()) {
                dir.mkdir();
            }

            fos = new FileOutputStream(PREFIX + WORKING);
            fc = fos.getChannel();

            PrintStream ps = new PrintStream(fos, true);

            System.setOut(ps);
            System.setErr(ps);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stateAdvanced(Search search) {
        try {
            unfreeze();

            positions.push(getCurrentPosition());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stateBacktracked(Search search) {
        try {
            positions.pop();

            setCurrentPosition(positions.peek());

            freeze();

            truncate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void searchFinished(Search search) {
        try {
            finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getName(String fullname) {
        return fullname.substring(fullname.lastIndexOf('.') + 1);
    }

    private long getCurrentPosition() throws IOException {
        return fc.position();
    }

    private void setCurrentPosition(long position) throws IOException {
        fc.position(position);
    }

    private void truncate() throws IOException {
        fc.truncate(getCurrentPosition());
    }

    private void unfreeze() {
        frozen = false;
    }

    private void freeze() throws IOException {
        if (!frozen) {
            fos.flush();
            Files.copy(Paths.get(PREFIX, WORKING), Paths.get(PREFIX, FINAL + PredicateAbstraction.getInstance().getNumberOfRefinements() + "." + (++i)), REPLACE_EXISTING);

            frozen = true;
        }
    }

    private void finish() throws IOException {
        fos.flush();
        fc.close();
    }
}
