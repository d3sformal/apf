//
// Copyright (C) 2012 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
// 
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.abstraction.Attribute;
import gov.nasa.jpf.abstraction.GlobalAbstraction;
import gov.nasa.jpf.abstraction.concrete.ConcretePath;
import gov.nasa.jpf.abstraction.impl.EmptyAttribute;
import gov.nasa.jpf.abstraction.impl.NonEmptyAttribute;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class DALOAD extends gov.nasa.jpf.jvm.bytecode.DALOAD {
	
	@Override
	public Instruction execute(ThreadInfo ti) {		
		StackFrame sf = ti.getTopFrame();
		Attribute arrayAttr = (Attribute) sf.getOperandAttr(1);
		Attribute indexAttr = (Attribute) sf.getOperandAttr(0);
		
		if (arrayAttr == null) arrayAttr = new EmptyAttribute();
		if (indexAttr == null) arrayAttr = new EmptyAttribute();

		Instruction expectedNextInsn = JPFInstructionAdaptor.getStandardNextInstruction(this, ti);

		Instruction actualNextInsn = super.execute(ti);
		
		if (JPFInstructionAdaptor.testArrayElementInstructionAbort(this, ti, expectedNextInsn, actualNextInsn)) {
			return actualNextInsn;
		}   
		
		if (arrayAttr.getExpression() instanceof ConcretePath) {
			ConcretePath path = (ConcretePath) arrayAttr.getExpression();
			
			path.appendIndexElement(indexAttr.getExpression());
			
			GlobalAbstraction.getInstance().processLoad(path);
			
			Attribute attribute = new NonEmptyAttribute(null, path);

			sf = ti.getTopFrame();
			sf.setLongOperandAttr(attribute);
		}


		return actualNextInsn;
	}
}
