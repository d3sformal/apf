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
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

import gov.nasa.jpf.abstraction.PredicateAbstraction;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.ExpressionUtil;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultRoot;

/**
 * Stores a value from a stack to a local variable and informs abstractions about such an event
 */
public class DSTORE extends gov.nasa.jpf.jvm.bytecode.DSTORE {

    public DSTORE(int index) {
        super(index);
    }

    @Override
    public Instruction execute(ThreadInfo ti) {
        StackFrame sf = ti.getTopFrame();
        Expression from = ExpressionUtil.getExpression(sf.getLongOperandAttr());

        Instruction actualNextInsn = super.execute(ti);

        DefaultRoot to = DefaultRoot.create(getLocalVariableName(), getLocalVariableIndex());

        sf = ti.getModifiableTopFrame();

        /**
         * Remember what has been stored here
         */
        sf.setLocalAttr(getLocalVariableIndex(), from);

        /**
         * Inform the abstractions that a primitive value of a local variable may have changed
         */
        PredicateAbstraction.getInstance().informAboutPrimitiveLocalVariable(to);
        PredicateAbstraction.getInstance().processPrimitiveStore(getMethodInfo(), getPosition(), actualNextInsn.getMethodInfo(), actualNextInsn.getPosition(), from, to);

        return actualNextInsn;
    }
}
