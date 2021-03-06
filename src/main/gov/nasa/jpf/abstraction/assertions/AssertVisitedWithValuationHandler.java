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

import gov.nasa.jpf.abstraction.PredicateAbstraction;
import gov.nasa.jpf.abstraction.bytecode.AnonymousExpressionTracker;
import gov.nasa.jpf.abstraction.common.ExpressionUtil;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.common.PredicatesFactory;
import gov.nasa.jpf.abstraction.state.TruthValue;

public abstract class AssertVisitedWithValuationHandler extends AssertHandler {

    @Override
    public void executeInstruction(VM vm, ThreadInfo curTh, Instruction nextInsn) {
        StackFrame sf = curTh.getModifiableTopFrame();

        AnonymousExpressionTracker.notifyPopped(ExpressionUtil.getExpression(sf.getOperandAttr()), 1);

        ElementInfo arrayEI = curTh.getElementInfo(sf.pop());
        int limit = sf.pop();

        PredicateValuationMap trackedValuation = new PredicateValuationMap();
        PredicateValuationMap valuation = new PredicateValuationMap();

        for (int j = 0; j < arrayEI.arrayLength(); ++j) {
            ElementInfo ei = curTh.getElementInfo(arrayEI.getReferenceElement(j));

            String str = new String(ei.getStringChars());

            try {
                String[] strParts = str.split(":");

                if (strParts.length != 2) {
                    throw new Exception();
                }

                Predicate predicate = PredicatesFactory.createPredicateFromString(strParts[0]);
                TruthValue trackedValue = TruthValue.create(strParts[1]);
                TruthValue value = PredicateAbstraction.getInstance().processBranchingCondition(curTh.getPC().getPosition(), predicate);

                trackedValuation.put(predicate, trackedValue);
                valuation.put(predicate, value);
            } catch (Exception e) {
                throw new RuntimeException("Incorrect format of asserted facts: `" + str + "`");
            }
        }

        update(vm, nextInsn, trackedValuation, valuation, new Integer(limit));
    }

    protected abstract void update(VM vm, Instruction insn, PredicateValuationMap trackedValuation, PredicateValuationMap valuation, Integer limit);

    protected void reportError(VM vm, Instruction nextInsn) {
        reportError(vm, nextInsn.getLineNumber(), AssertStateMatchingContext.get(nextInsn).getError());
    }

}
