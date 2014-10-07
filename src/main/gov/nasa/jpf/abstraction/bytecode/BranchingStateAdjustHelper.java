package gov.nasa.jpf.abstraction.bytecode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.StateSet;
import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

import gov.nasa.jpf.abstraction.DynamicIntChoiceGenerator;
import gov.nasa.jpf.abstraction.PredicateAbstraction;
import gov.nasa.jpf.abstraction.ResetableStateSet;
import gov.nasa.jpf.abstraction.common.Conjunction;
import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Contradiction;
import gov.nasa.jpf.abstraction.common.Disjunction;
import gov.nasa.jpf.abstraction.common.Equals;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Negation;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.common.Tautology;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.ArrayElementRead;
import gov.nasa.jpf.abstraction.common.access.ObjectFieldRead;
import gov.nasa.jpf.abstraction.common.access.Root;
import gov.nasa.jpf.abstraction.common.access.Unknown;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultArrayElementRead;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultRoot;
import gov.nasa.jpf.abstraction.state.TruthValue;
import gov.nasa.jpf.abstraction.state.universe.ClassName;
import gov.nasa.jpf.abstraction.state.universe.UniverseIdentifier;

public class BranchingStateAdjustHelper {
    public static void synchronizeConcreteAndAbstractExecutions(AbstractBranching br, ThreadInfo ti, int v1, int v2, Expression expr1, Expression expr2, boolean abstractJump) {
        StackFrame sf = ti.getModifiableTopFrame();
        SystemState ss = ti.getVM().getSystemState();

        Predicate branchCondition = br.createPredicate(expr1, expr2);

        if (!abstractJump) {
            branchCondition = Negation.create(branchCondition);
        }

        boolean pruneInfeasible = VM.getVM().getJPF().getConfig().getBoolean("panda.branch.prune_infeasible");
        boolean forceFeasibleOnce = VM.getVM().getJPF().getConfig().getBoolean("panda.branch.force_feasible_once");
        boolean forceFeasible = forceFeasibleOnce || VM.getVM().getJPF().getConfig().getBoolean("panda.branch.force_feasible");

        // In case the concrete execution does not allow the same branch to be taken
        if (br.getConcreteBranchValue(v1, v2) == TruthValue.create(abstractJump)) {
            if (forceFeasibleOnce) {
                // Add state to allow forward state matching (look if we should generate more choices to get into the branch (it might have been explored before - actually it is explored when this branch is hit))

                ti.breakTransition("Creating state after taking an enabled branch");
            }
        } else {
            System.err.println("[WARNING] Inconsistent concrete and abstract branching: " + branchCondition);

            // Either cut of the inconsistent branch
            // or make the concrete state represent the abstract one (force concrete values)
            if (pruneInfeasible) {
                ss.setIgnored(true);

                if (forceFeasible) {
                    if (VM.getVM().getJPF.getConfig().getBoolean("panda.verbose")) {
                        System.out.println("[WARNING] Finding feasible trace with matching prefix and " + branchCondition);
                    }

                    Predicate traceFormula = PredicateAbstraction.getInstance().getTraceFormula().toConjunction();
                    Map<String, Unknown> unknowns = PredicateAbstraction.getInstance().getUnknowns();

                    int i = 0;
                    AccessExpression[] exprArray = new AccessExpression[unknowns.keySet().size()];

                    // Collect unknown expressions
                    for (String unknown : unknowns.keySet()) {
                        exprArray[i] = DefaultRoot.create(unknown);

                        ++i;
                    }

                    /**
                     * Blocking clauses may not be necessary (the trace itself (extended with appropriate branch condition) is enough to demand a new value of unknown)
                     * On contrary they may cause divergence
                     * But they are there currently to avoid returning to previously picked choices:
                     *   i = *
                     *   if (i = 1) {
                     *   }
                     *
                     *   Starts with 0
                     *   Generates 1
                     *   Goes back to 0
                     *   Goes back to 1
                     *   Goes back to 0
                     *   ...
                     */
                    // Add blocking clause for unknown models
                    //   (u1 != v11 & u1 != v12 & ... u1 != v1n) | (u2 != ...) | ... (un != ...)

                    Predicate blockings = Contradiction.create();

                    for (int j = 0; j < exprArray.length; ++j) {
                        Predicate blocking = Tautology.create();

                        for (int model : unknowns.get(((DefaultRoot) exprArray[j]).getName()).getChoiceGenerator().getChoices()) {
                            blocking = Conjunction.create(blocking, Negation.create(Equals.create(exprArray[j], Constant.create(model))));
                        }

                        blockings = Disjunction.create(blockings, blocking);
                    }

                    traceFormula = Conjunction.create(traceFormula, blockings);

                    int[] models = PredicateAbstraction.getInstance().getPredicateValuation().get(0).getModels(traceFormula, exprArray);

                    if (models == null) {
                        if (VM.getVM().getJPF.getConfig().getBoolean("panda.verbose")) {
                            System.out.println("[WARNING] No feasible trace found");
                        }
                    } else {
                        // To avoid divergence:
                        //   one concrete path allows only one branch -> second trace is explored to cover the other branch
                        //   the second concrete path does not allow the first branch -> third trace is explored to cover the first branch
                        //   ...
                        //
                        // Assume this is the case:
                        //   We revisit this branch (the other branch has already been visited - it created a state after passing the check)
                        // Then:
                        //   Try to create a state (but not store it) if the state matches then dont add the choices
                        //     - We cannot store it now because:
                        //       - If this is the first visit -> first enable of a disabled branch -> we WOULD CREATE STATE in the branch -> when we get here with the correct concrete model we would match and not continue exploring
                        //

                        StateSet stateSet = VM.getVM().getStateSet();
                        ResetableStateSet rStateSet = null;

                        if (stateSet instanceof ResetableStateSet) {
                            rStateSet = (ResetableStateSet) stateSet;
                        }

                        if (rStateSet == null || rStateSet.isCurrentUnique() || !forceFeasibleOnce) {
                            if (VM.getVM().getJPF.getConfig().getBoolean("panda.verbose")) {
                                System.out.println("[WARNING] Feasible trace found for unknown values: " + Arrays.toString(models));
                            }

                            for (int j = 0; j < models.length; ++j) {
                                DynamicIntChoiceGenerator cg = unknowns.get(((DefaultRoot) exprArray[j]).getName()).getChoiceGenerator();

                                if (cg.hasProcessed(models[j]) || !cg.has(models[j])) {
                                    cg.add(models[j]);
                                }
                            }
                        }
                    }
                }
            } else if (ti.getVM().getJPF().getConfig().getBoolean("panda.branch.adjust_concrete_values")) {
                Map<AccessExpression, ElementInfo> primitiveExprs = new HashMap<AccessExpression, ElementInfo>();
                Set<AccessExpression> allExprs = new HashSet<AccessExpression>();

                PredicateAbstraction.getInstance().getPredicateValuation().get(0).addAccessExpressionsToSet(allExprs);

                // Collect all access expressions pointing at primitive values
                // Restrain to those that are mentioned in predicates
                // These will be tweaked so that they represent the abstract state
                collectAllStateExpressions(primitiveExprs, allExprs, sf, ti);

                ElementInfo[] targetArray = new ElementInfo[primitiveExprs.keySet().size()];
                AccessExpression[] exprArray = new AccessExpression[primitiveExprs.keySet().size()];

                int i = 0;
                for (AccessExpression expr : primitiveExprs.keySet()) {
                    exprArray[i] = expr;
                    targetArray[i] = primitiveExprs.get(expr);
                    ++i;
                }

                // Compute a concrete (sub)state representing the abstract one
                int[] valueArray = PredicateAbstraction.getInstance().getPredicateValuation().get(0).getConcreteState(exprArray, br.getSelf().getPosition());

                if (valueArray == null) {
                    throw new RuntimeException("Cannot compute the corresponding concrete state.");
                }

                // Inject the newly computed values into the concrete state
                for (int j = 0; j < exprArray.length; ++j) {
                    adjustValueInConcreteState(exprArray[j], valueArray[j], targetArray[j], sf, ti);
                }
            }
        }
    }

    // Collects all deterministic (only constant array indices) access expressions that point to primitive data contributing to the current concrete state
    //   State expression ~ access expression pointing at a primitive value that contributes to the concrete state
    // The set of access expressions is restricted to the current scope
    // Effectively it converts all expressions in `allExprs` of the form `a[expr]` into `a[0]` ... `a[n]`
    private static void collectAllStateExpressions(Map<AccessExpression, ElementInfo> stateExprs, Set<AccessExpression> allExprs, StackFrame sf, ThreadInfo ti) {
        Set<UniverseIdentifier> cls = new HashSet<UniverseIdentifier>();

        for (AccessExpression expr : allExprs) {
            Root root = expr.getRoot();

            if (root.isLocalVariable()) {
                int idx = sf.getLocalVariableSlotIndex(root.getName());

                if (idx >= 0) {
                    if (sf.isLocalVariableRef(idx)) {
                        collectStateExpressions(stateExprs, ti, ti.getElementInfo(sf.getLocalVariable(idx)), expr, 2, root);
                    } else {
                        stateExprs.put(root, null);
                    }
                }
            } else if (root.isStatic()) {
                cls.clear();

                PredicateAbstraction.getInstance().getSymbolTable().get(0).lookupValues(root, cls);

                assert cls.size() == 1;

                ClassName clsName = (ClassName) cls.iterator().next();

                collectStateExpressions(stateExprs, ti, clsName.getStaticElementInfo(), expr, 2, root);
            }
        }
    }

    // Recursively expands expressions
    // Used in collectAllStateExpressions only
    private static void collectStateExpressions(Map<AccessExpression, ElementInfo> stateExprs, ThreadInfo ti, ElementInfo parent, AccessExpression expr, int i, AccessExpression prefix) {
        if (i < expr.getLength()) {
            AccessExpression access = expr.get(i);

            if (access instanceof ObjectFieldRead) {
                ObjectFieldRead r = (ObjectFieldRead) access;

                if (parent.getClassInfo().getInstanceField(r.getField().getName()).isReference()) {
                    collectStateExpressions(stateExprs, ti, ti.getElementInfo(parent.getReferenceField(r.getField().getName())), expr, i + 1, r.reRoot(prefix));
                } else {
                    stateExprs.put(r.reRoot(prefix), parent);
                }
            } else if (access instanceof ArrayElementRead) {
                ArrayElementRead r = (ArrayElementRead) access;
                int[] indices;

                if (r.getIndex() instanceof Constant) {
                    indices = new int[] {((Constant) r.getIndex()).value.intValue()};
                } else {
                    indices = PredicateAbstraction.getInstance().computeAllExpressionValuesInRange(r.getIndex(), 0, parent.arrayLength());
                }

                for (int index : indices) {
                    if (parent.isReferenceArray()) {
                        collectStateExpressions(stateExprs, ti, ti.getElementInfo(parent.getArrayFields().getReferenceValue(index)), expr, i + 1, DefaultArrayElementRead.create(prefix, Constant.create(index)));
                    } else {
                        stateExprs.put(DefaultArrayElementRead.create(prefix, Constant.create(index)), parent);
                    }
                }
            }
        }
    }

    private static void adjustValueInConcreteState(AccessExpression expr, int value, ElementInfo ei, StackFrame sf, ThreadInfo ti) {
        if (ei == null) {
            LocalVarInfo lvi = sf.getLocalVarInfo(expr.getRoot().getName());

            // Update only variables that are in scope
            if (lvi != null) {
                sf.setLocalVariable(lvi.getSlotIndex(), value);
            }
        } else if (expr instanceof ObjectFieldRead) {
            ObjectFieldRead r = (ObjectFieldRead) expr;

            ti.getModifiableElementInfo(ei.getObjectRef()).setIntField(r.getField().getName(), value);
        } else if (expr instanceof ArrayElementRead) {
            ArrayElementRead r = (ArrayElementRead) expr;
            Constant c = (Constant) r.getIndex();

            ti.getModifiableElementInfo(ei.getObjectRef()).getArrayFields().setIntValue(c.value.intValue(), value);
        } else {
            throw new RuntimeException("Cannot inject value into anything else than local variable, object field, static field and array element.");
        }
    }
}
