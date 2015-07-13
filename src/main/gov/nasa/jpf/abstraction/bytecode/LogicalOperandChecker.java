package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;

import gov.nasa.jpf.abstraction.PredicateAbstraction;
import gov.nasa.jpf.abstraction.common.Conjunction;
import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Disjunction;
import gov.nasa.jpf.abstraction.common.Equals;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Negation;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.state.TruthValue;
import gov.nasa.jpf.abstraction.util.RunDetector;

public class LogicalOperandChecker {
    public static void check(int lastPC, Expression a, Expression b) {
        if (RunDetector.isRunning()) {
            Predicate inSupportedDomain = Conjunction.create(
                Disjunction.create(
                    Equals.create(a, Constant.create(0)),
                    Equals.create(a, Constant.create(1))
                ),
                Disjunction.create(
                    Equals.create(b, Constant.create(0)),
                    Equals.create(b, Constant.create(1))
                )
            );

            TruthValue value = PredicateAbstraction.getInstance().processBranchingCondition(lastPC, inSupportedDomain);

            if (value != TruthValue.TRUE) {
                Instruction pc = ThreadInfo.getCurrentThread().getPC();
                PredicateAbstraction.getInstance().extendTraceFormulaWithConstraint(Negation.create(inSupportedDomain), pc.getMethodInfo(), pc.getPosition());

                throw new IllegalArgumentException("logical and bitwise operations over values other than {0, 1} are not supported");
            }
        }
    }
}
