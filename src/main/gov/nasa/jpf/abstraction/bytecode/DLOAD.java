package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

import gov.nasa.jpf.abstraction.PredicateAbstraction;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultRoot;

public class DLOAD extends gov.nasa.jpf.jvm.bytecode.DLOAD implements VariableLoadInstruction {

    public DLOAD(int index) {
        super(index);
    }

    @Override
    public Instruction execute(ThreadInfo ti) {
        Instruction actualNextInsn = super.execute(ti);

        DefaultRoot path = getVariable();

        StackFrame sf = ti.getModifiableTopFrame();
        sf.setLongOperandAttr(path);

        PredicateAbstraction.getInstance().informAboutPrimitiveLocalVariable(path);

        return actualNextInsn;
    }

    @Override
    public DefaultRoot getVariable() {
        return DefaultRoot.create(getLocalVariableName(), getLocalVariableIndex());
    }
}
