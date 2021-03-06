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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.choice.BreakGenerator;
import gov.nasa.jpf.vm.choice.IntChoiceFromList;

import gov.nasa.jpf.abstraction.AbstractChoiceGenerator;
import gov.nasa.jpf.abstraction.BranchingExecutionHelper;
import gov.nasa.jpf.abstraction.PandaConfig;
import gov.nasa.jpf.abstraction.PredicateAbstraction;
import gov.nasa.jpf.abstraction.common.BranchingConditionValuation;
import gov.nasa.jpf.abstraction.common.BytecodeInterval;
import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Equals;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.ExpressionUtil;
import gov.nasa.jpf.abstraction.common.Negation;
import gov.nasa.jpf.abstraction.common.Notation;
import gov.nasa.jpf.abstraction.common.Operation;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.ArrayElementRead;
import gov.nasa.jpf.abstraction.common.access.ObjectFieldRead;
import gov.nasa.jpf.abstraction.common.access.PackageAndClass;
import gov.nasa.jpf.abstraction.common.access.Root;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultArrayElementRead;
import gov.nasa.jpf.abstraction.concrete.AnonymousExpression;
import gov.nasa.jpf.abstraction.state.TruthValue;
import gov.nasa.jpf.abstraction.state.universe.ClassName;
import gov.nasa.jpf.abstraction.state.universe.Reference;
import gov.nasa.jpf.abstraction.state.universe.UniverseIdentifier;
import gov.nasa.jpf.abstraction.util.RunDetector;

/**
 * Implementation of all binary IF instructions regardless their precise type.
 */
public class BinaryIfInstructionExecutor {

    private Instruction next;

    final public Instruction execute(AbstractBranching br, ThreadInfo ti) {
        String name = br.getClass().getSimpleName();

        SystemState ss = ti.getVM().getSystemState();
        StackFrame sf = ti.getModifiableTopFrame();
        ChoiceGenerator<?> cg = null;

        if (ti.isFirstStepInsn()) {
            cg = ss.getChoiceGenerator();

            if (cg instanceof BreakGenerator) {
                return next;
            }
        }

        Expression expr1 = ExpressionUtil.getExpression(sf.getOperandAttr(1));
        Expression expr2 = ExpressionUtil.getExpression(sf.getOperandAttr(0));

        int v1 = sf.peek(1);
        int v2 = sf.peek(0);

        boolean conditionValue;

        /**
         * First we check whether there is no choice generator present
         * If not we evaluate the branching condition
         * Otherwise we inspect all the choices
         */
        if (!ti.isFirstStepInsn()) { // first time around
            Predicate predicate = null;
            TruthValue truth = TruthValue.UNDEFINED;

            /**
             * If there is enough information (symbolic expressions) to decide the condition we ask abstractions to provide the truth value
             * Only predicate abstraction is designed to respond with a valid value (TRUE, FALSE, UNKNOWN).
             * No other abstraction can do that, the rest of them returns UNDEFINED.
             */
            if (expr1 != null && expr2 != null && RunDetector.isRunning()) {
                predicate = br.createPredicate(expr1, expr2);
                truth = PredicateAbstraction.getInstance().processBranchingCondition(br.getSelf().getPosition(), predicate);
            }

            switch (truth) {
                // IF THE BRANCH COULD NOT BE PICKED BY PREDICATE ABSTRACTION (IT IS NOT ACTIVE)
                default:
                case UNDEFINED:
                    return br.executeConcrete(ti);
                case TRUE:
                    cg = new IntChoiceFromList("Ensure that state matching is used in case there was an infinite loop", 1);
                    ss.setNextChoiceGenerator(cg);

                    // This happens after reinvoking the instruction
                    //PredicateAbstraction.getInstance().extendTraceFormulaWithConstraint(predicate, br.getSelf().getMethodInfo(), br.getDefaultTarget().getPosition());

                    return br.getSelf();
                case FALSE:
                    cg = new IntChoiceFromList("Ensure that state matching is used in case there was an infinite loop", 0);
                    ss.setNextChoiceGenerator(cg);

                    // This happens after reinvoking the instruction
                    //PredicateAbstraction.getInstance().extendTraceFormulaWithConstraint(Negation.create(predicate), br.getSelf().getMethodInfo(), br.getNext(ti).getPosition());

                    return br.getSelf();
                case UNKNOWN:
                    cg = new AbstractChoiceGenerator();
                    ss.setNextChoiceGenerator(cg);

                    return br.getSelf();
            }
        } else { // this is what really returns results
            cg = ss.getChoiceGenerator();

            if (cg instanceof AbstractChoiceGenerator) {
                conditionValue = (Integer) cg.getNextChoice() == 0 ? false : true;
            } else if (cg instanceof IntChoiceFromList) {
                conditionValue = (Integer) cg.getNextChoice() == 0 ? false : true;
            } else {
                throw new RuntimeException("expected AbstractChoiceGenerator, got: " + cg);
            }

            if (expr1 != null && expr2 != null) {
                Predicate predicate = br.createPredicate(expr1, expr2);
                PredicateAbstraction.getInstance().informAboutBranchingDecision(new BranchingConditionValuation(predicate, TruthValue.create(conditionValue)), br.getSelf().getMethodInfo(), br.getTarget(ti, conditionValue ? 1 : 0).getPosition());

                if (PandaConfig.getInstance().trackExactValueForLoopControlVariable() && !conditionValue) {
                    Instruction bJumpCandidate = br.getSelf();
                    Instruction cmpStart = br.getSelf();

                    /*
                    // find the first instruction of the comparison operands load (based on the structured of the compared expressions)
                    cmpStart = unwindExprLoad(cmpStart, expr1);
                    cmpStart = unwindExprLoad(cmpStart, expr2);
                    */

                    int start = cmpStart.getPosition();
                    int end = start;

                    while (bJumpCandidate != null) {
                        if (bJumpCandidate instanceof gov.nasa.jpf.jvm.bytecode.GOTO) {
                            gov.nasa.jpf.jvm.bytecode.GOTO gt = (gov.nasa.jpf.jvm.bytecode.GOTO) bJumpCandidate;

                            //if (gt.getTarget() == cmpStart) { // find a matching backjump
                            if (gt.getTarget().getPosition() < cmpStart.getPosition()) { // find the first jump to a location before comparison
                                end = gt.getPosition();
                                break;
                            }
                        }

                        bJumpCandidate = bJumpCandidate.getNext();
                    }

                    Predicate eq = Equals.create(expr1, Constant.create(v1));

                    eq.setScope(new BytecodeInterval(start, end));

                    PredicateAbstraction.getInstance().getPredicateValuation().force(eq, TruthValue.UNKNOWN);
                }
            }
        }

        sf.pop();
        sf.pop();

        Predicate branchCondition = br.createPredicate(expr1, expr2);

        if (!conditionValue) {
            branchCondition = Negation.create(branchCondition);
        }

        next = br.getTarget(ti, conditionValue ? 1 : 0);

        return BranchingExecutionHelper.synchronizeConcreteAndAbstractExecutions(ti, branchCondition, br.getConcreteBranchValue(v1, v2), conditionValue, next, br.getSelf());
    }

    /*
    private static Instruction unwindExprLoad(Instruction base, Expression expr) {
        if (expr instanceof Constant) {
            return base.getPrev();
        }
        if (expr instanceof PackageAndClass) {
            return base;
        }
        if (expr instanceof Root) {
            return base.getPrev();
        }
        if (expr instanceof ArrayElementRead) {
            ArrayElementRead ar = (ArrayElementRead) expr;

            return unwind(unwind(base.getPrev(), ar.getIndex()), ar.getArray());
        }
        if (expr instanceof ObjectFieldRead) {
            ObjectFieldRead fr = (ObjectFieldRead) expr;

            return unwind(base.getPrev(), fr.getObject());
        }
        if (expr instanceof Operation) {
            Operation op = (Operation) expr;

            return unwind(unwind(base.getPrev(), op.a), op.b);
        }

        return base;
    }
    */
}
