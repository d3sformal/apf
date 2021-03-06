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
package gov.nasa.jpf.abstraction.assertions;

import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

import gov.nasa.jpf.abstraction.bytecode.AnonymousExpressionTracker;
import gov.nasa.jpf.abstraction.common.ExpressionUtil;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.common.PredicatesFactory;
import gov.nasa.jpf.abstraction.common.Tautology;
import gov.nasa.jpf.abstraction.state.TruthValue;

public class AssertDisjunctionHandler extends AssertPredicateHandler {

    public enum Type {
        ONE_PREDICATE_PER_SET,
        MULTIPLE_PREDICATES_PER_SET
    }

    private Type type;

    public AssertDisjunctionHandler(Type type) {
        this.type = type;
    }

    @Override
    public void executeInstruction(VM vm, ThreadInfo curTh, Instruction nextInsn) {
        StackFrame sf = curTh.getModifiableTopFrame();

        AnonymousExpressionTracker.notifyPopped(ExpressionUtil.getExpression(sf.getOperandAttr()), 1);

        ElementInfo arrayEI = curTh.getElementInfo(sf.pop());

        boolean foundValid = false;
        boolean foundTwoValid = false;

        for (int i = 0; i < arrayEI.arrayLength(); ++i) {
            ElementInfo ei = curTh.getElementInfo(arrayEI.getReferenceElement(i));

            try {
                switch (type) {
                    case ONE_PREDICATE_PER_SET:
                        checkAssertion(ei, curTh);
                        break;

                    case MULTIPLE_PREDICATES_PER_SET:
                        checkAssertionSet(ei, curTh);
                        break;
                }

                foundTwoValid |= foundValid;
                foundValid |= true;
            } catch (RuntimeException e) {
            }
        }

        if (foundTwoValid) {
            respondToFindingTwoValid(vm, nextInsn.getLineNumber());
        }

        if (!foundValid) {
            respondToNotFindingAnyValid(vm, nextInsn.getLineNumber());
        }
    }

    protected void respondToFindingTwoValid(VM vm, int lineNumber) {
    }

    protected void respondToNotFindingAnyValid(VM vm, int lineNumber) {
        reportError(vm, lineNumber, "No set of assertions satisfied.");
    }

}
