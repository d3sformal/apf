//Copyright (C) 2012 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.

//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.

//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.

package gov.nasa.jpf.abstraction.bytecode;


import gov.nasa.jpf.abstraction.numeric.AbstractBoolean;
import gov.nasa.jpf.abstraction.numeric.AbstractChoiceGenerator;
import gov.nasa.jpf.abstraction.numeric.Abstraction;
import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.bytecode.Instruction;


public class IFGT extends gov.nasa.jpf.jvm.bytecode.IFGT {

	public IFGT(int targetPc) {
		super(targetPc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {

		StackFrame sf = ti.getTopFrame();
		Abstraction abs_v = (Abstraction) sf.getOperandAttr();

		if(abs_v == null) { // the condition is concrete
			//System.out.println("Execute IFGT: The condition is concrete");
			return super.execute(ss, ks, ti);
		}
		else { // the condition is abstract

			System.out.printf("IFGT> Values: %d (%s)\n", ti.peek(0), abs_v);
			AbstractBoolean abs_condition = abs_v._gt(0);

			if(abs_condition == AbstractBoolean.TRUE) {
				conditionValue = true;
			}
			else if (abs_condition == AbstractBoolean.FALSE) {
				conditionValue = false;
			}
			else { // TOP
				ChoiceGenerator<?> cg;
				if (!ti.isFirstStepInsn()) { // first time around
					cg = new AbstractChoiceGenerator();
					ss.setNextChoiceGenerator(cg);
					return this;
				} else {  // this is what really returns results
					cg = ss.getChoiceGenerator();
					assert (cg instanceof AbstractChoiceGenerator) : "expected AbstractChoiceGenerator, got: " + cg;
					conditionValue = (Integer)cg.getNextChoice()==0 ? false: true;
				}
			}

			ti.pop();
			return (conditionValue ? getTarget() : getNext(ti));

		}

	}
}
