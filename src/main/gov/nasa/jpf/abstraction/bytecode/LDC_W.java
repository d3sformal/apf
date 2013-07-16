package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.impl.NonEmptyAttribute;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class LDC_W extends gov.nasa.jpf.jvm.bytecode.LDC_W {
	
	public LDC_W(int v) {
		super(v);
	}

	public LDC_W(float f) {
		super(f);
	}
	
	public LDC_W(String v, boolean isClass) {
		super(v, isClass);
	}

	@Override
	public Instruction execute(ThreadInfo ti) {
		Instruction ret = super.execute(ti);
		
		StackFrame sf = ti.getModifiableTopFrame();
		Expression expression;
		
		switch (getType()) {
		case INT:
			expression = new Constant(getValue());
			break;
		case FLOAT:
			expression = new Constant(getFloatValue());
			break;
		default:
			expression = null;
			break;
		}
		
		sf.setOperandAttr(new NonEmptyAttribute(null, expression));
		
		return ret;
	}

}