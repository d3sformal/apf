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
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.Root;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultRoot;
import gov.nasa.jpf.abstraction.concrete.AnonymousObject;
import gov.nasa.jpf.abstraction.state.universe.Reference;

public class RUNSTART extends gov.nasa.jpf.jvm.bytecode.RUNSTART {
    @Override
    public Instruction execute(ThreadInfo ti) {
        Instruction ret = super.execute(ti);

        StackFrame sf = ti.getModifiableTopFrame();

        Root thisExpr = DefaultRoot.create("this");
        AccessExpression threadObjectExpr = AnonymousObject.create(new Reference(ti.getElementInfo(sf.peek())));

        // Do not update Predicate Valuation (that has been setup at .start()V)
        PredicateAbstraction.getInstance().getSymbolTable().get(0).addStructuredLocalVariable(thisExpr);
        PredicateAbstraction.getInstance().getSymbolTable().processObjectStore(threadObjectExpr, thisExpr);

        sf.setOperandAttr(thisExpr);

        return ret;
    }
}
