package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

import gov.nasa.jpf.abstraction.PredicateAbstraction;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultRoot;

public class ALOAD extends gov.nasa.jpf.jvm.bytecode.ALOAD implements VariableLoadInstruction {

    public ALOAD(int index) {
        super(index);
    }

    @Override
    public Instruction execute(ThreadInfo ti) {
        Instruction actualNextInsn = super.execute(ti);

        DefaultRoot path = getVariable();

        StackFrame sf = ti.getModifiableTopFrame();
        sf.setOperandAttr(path);

        PredicateAbstraction.getInstance().informAboutStructuredLocalVariable(path);

        return actualNextInsn;
    }

    @Override
    public DefaultRoot getVariable() {
        return DefaultRoot.create(getLocalVariableName(), getLocalVariableIndex());
    }
}
