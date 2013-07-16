package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.impl.NonEmptyAttribute;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class LDC2_W extends gov.nasa.jpf.jvm.bytecode.LDC2_W {
	
	public LDC2_W(long l) {
		super(l);
	}

	public LDC2_W(double d) {
		super(d);
	}

	@Override
	public Instruction execute(ThreadInfo ti) {
		Instruction ret = super.execute(ti);
		
		StackFrame sf = ti.getModifiableTopFrame();
		Expression expression;
		
		switch (getType()) {
		case LONG:
			expression = new Constant(getValue());
			break;
		case DOUBLE:
			expression = new Constant(getDoubleValue());
			break;
		default:
			expression = null;
			break;
		}
		
		sf.setOperandAttr(new NonEmptyAttribute(null, expression));
		
		return ret;
	}

}