// Copyright (C) 2012 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.

// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.

// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.abstraction.numeric.Abstraction;
import gov.nasa.jpf.abstraction.numeric.FocusAbstractChoiceGenerator;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.SystemState;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Instruction;

/**
 * Increment local variable by constant
 * No change
 */
public class IINC extends gov.nasa.jpf.jvm.bytecode.IINC {

	public IINC(int localVarIndex, int increment) {
		super(localVarIndex, increment);
	}

	public Instruction execute(ThreadInfo ti) {

		SystemState ss = ti.getVM().getSystemState();
		StackFrame sf = ti.getTopFrame();
		Abstraction abs_v = (Abstraction) sf.getLocalAttr(index);
		
		if (abs_v == null) {
			sf.setLocalVariable(index, sf.getLocalVariable(index) + increment, false);
		} else {
			Abstraction result = abs_v._plus(increment);
			System.out.printf("IINC> Value:  %d (%s)\n", sf.getLocalVariable(index), abs_v);

			if (result.isComposite()) {
				System.out.println("Top");

				if (!ti.isFirstStepInsn()) { // first time around
					int size = result.getTokensNumber();
					
					System.out.println("size "+size);//should be 3
					
					ChoiceGenerator<?> cg = new FocusAbstractChoiceGenerator(size);
					ss.setNextChoiceGenerator(cg);
					
					return this;
				} else { // this is what really returns results
					ChoiceGenerator<?> cg = ss.getChoiceGenerator();
					
					assert (cg instanceof FocusAbstractChoiceGenerator);
					
					int key = (Integer) cg.getNextChoice();
					result = result.getToken(key);
				}
			}
			
			System.out.printf("IINC> Result: %s\n", result);
			
			sf.setLocalAttr(index, result);
			sf.setLocalVariable(index, 0, false);					
		}

		return getNext(ti);
	}

}
