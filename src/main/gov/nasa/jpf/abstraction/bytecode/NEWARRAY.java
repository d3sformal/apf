package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.abstraction.util.RunDetector;
import gov.nasa.jpf.abstraction.GlobalAbstraction;
import gov.nasa.jpf.abstraction.Attribute;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.common.Negation;
import gov.nasa.jpf.abstraction.common.LessThan;
import gov.nasa.jpf.abstraction.concrete.AnonymousArray;
import gov.nasa.jpf.abstraction.predicate.PredicateAbstraction;
import gov.nasa.jpf.abstraction.predicate.state.TruthValue;
import gov.nasa.jpf.abstraction.predicate.state.universe.Reference;
import gov.nasa.jpf.abstraction.impl.EmptyAttribute;
import gov.nasa.jpf.abstraction.impl.NonEmptyAttribute;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class NEWARRAY extends gov.nasa.jpf.jvm.bytecode.NEWARRAY {

    public NEWARRAY(int typeCode) {
        super(typeCode);
    }

    @Override
    public Instruction execute(ThreadInfo ti) {
        StackFrame sf = ti.getTopFrame();
        Attribute attr = (Attribute) sf.getOperandAttr();

        attr = Attribute.ensureNotNull(attr);

        Instruction expectedNextInsn = JPFInstructionAdaptor.getStandardNextInstruction(this, ti);

        if (RunDetector.isRunning()) {
            PredicateAbstraction abs = (PredicateAbstraction) GlobalAbstraction.getInstance().get();
            Expression lengthExpression = attr.getExpression();
            Integer lengthValue = abs.computePreciseExpressionValue(lengthExpression);

            // Determine the unambiguous concrete array length from predicates
            if (lengthValue == null) {
                return ti.createAndThrowException("java.lang.IllegalArgumentException", "predicates do not specify exact array length");
            }

            // Check validity of the array length
            Predicate negative = LessThan.create(lengthExpression, Constant.create(0));
            TruthValue value = (TruthValue) GlobalAbstraction.getInstance().processBranchingCondition(negative);

            if (value != TruthValue.FALSE) {
                return ti.createAndThrowException("java.lang.NegativeArraySizeException");
            } else {
                // Replace the original concrete value (possibly inconsistent with the abstraction) with the value derived from the abstraction
                sf.pop();
                sf.push(lengthValue);
            }
        }

        Instruction actualNextInsn = super.execute(ti);

        if (JPFInstructionAdaptor.testNewArrayInstructionAbort(this, ti, expectedNextInsn, actualNextInsn)) {
            return actualNextInsn;
        }

        ElementInfo array = ti.getElementInfo(sf.peek());
        AnonymousArray expression = AnonymousArray.create(new Reference(array), attr.getExpression());

        GlobalAbstraction.getInstance().processNewObject(expression);

        sf = ti.getModifiableTopFrame();
        sf.setOperandAttr(new NonEmptyAttribute(null, expression));

        return actualNextInsn;
    }

}
