package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.impl.NonEmptyAttribute;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

import gov.nasa.jpf.abstraction.concrete.AnonymousObject;
import gov.nasa.jpf.abstraction.predicate.state.universe.Reference;

public class LDC extends gov.nasa.jpf.jvm.bytecode.LDC {
	
	public LDC(int v) {
		super(v);
	}

	public LDC(float f) {
		super(f);
	}
	
	public LDC(String v, boolean isClass) {
		super(v, isClass);
	}

	@Override
	public Instruction execute(ThreadInfo ti) {
		Instruction ret = super.execute(ti);
		
		StackFrame sf = ti.getModifiableTopFrame();
		Expression expression;
		
		switch (getType()) {
		case INT:
			expression = Constant.create(getValue());
			break;
		case FLOAT:
			expression = Constant.create(getFloatValue());
			break;
        case STRING:
        case CLASS:
            expression = AnonymousObject.create(new Reference(ti.getElementInfo(sf.peek()), ti));
            break;
		default:
			expression = null;
			break;
		}
		
		sf.setOperandAttr(new NonEmptyAttribute(null, expression));
		
		return ret;
	}

}
