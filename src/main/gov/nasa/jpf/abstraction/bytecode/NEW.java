package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.abstraction.GlobalAbstraction;
import gov.nasa.jpf.abstraction.concrete.AnonymousObject;
import gov.nasa.jpf.abstraction.predicate.state.universe.Reference;
import gov.nasa.jpf.abstraction.impl.NonEmptyAttribute;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class NEW extends gov.nasa.jpf.jvm.bytecode.NEW {

	public NEW(String clsDescriptor) {
		super(clsDescriptor);
	}
	
	@Override
	public Instruction execute(ThreadInfo ti) {
		Instruction expectedNextInsn = JPFInstructionAdaptor.getStandardNextInstruction(this, ti);

		Instruction actualNextInsn = super.execute(ti);
		
		if (JPFInstructionAdaptor.testNewInstructionAbort(this, ti, expectedNextInsn, actualNextInsn)) {
			return actualNextInsn;
		}
		
		StackFrame sf = ti.getModifiableTopFrame();
		AnonymousObject object = AnonymousObject.create(new Reference(ti.getElementInfo(sf.peek())));

        GlobalAbstraction.getInstance().processNewObject(object);
		sf.setOperandAttr(new NonEmptyAttribute(null, object));

		return actualNextInsn;
	}

}
