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
package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;

import gov.nasa.jpf.abstraction.Abstraction;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.LessThan;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.state.TruthValue;

/**
 * Branch if int comparison succeeds
 * ..., value1, value2 => ...
 */
public class IF_ICMPLT extends gov.nasa.jpf.jvm.bytecode.IF_ICMPLT implements BinaryAbstractBranching {

    /**
     * Share implementation with all the other binary if instructions.
     */
    BinaryIfInstructionExecutor executor = new BinaryIfInstructionExecutor();
    Predicate last;

    public IF_ICMPLT(int targetPc) {
        super(targetPc);
    }

    @Override
    public Instruction execute(ThreadInfo ti) {
        return executor.execute(this, ti);
    }

    @Override
    public Instruction executeConcrete(ThreadInfo ti) {
        return super.execute(ti);
    }

    @Override
    public Instruction getSelf() {
        return this;
    }

    @Override
    public boolean getConcreteBranchValue(int v1, int v2) {
        return v1 < v2;
    }

    @Override
    public Predicate createPredicate(Expression expr1, Expression expr2) {
        last = LessThan.create(expr1, expr2);
        return last;
    }

    @Override
    public Predicate getLastPredicate() {
        return last;
    }

    @Override
    public Instruction getDefaultTarget() {
        return getTarget();
    }

    @Override
    public Instruction getTarget(ThreadInfo ti, int num) {
        if (num == 0) {
            return getNext(ti);
        }

        return getTarget();
    }

}
