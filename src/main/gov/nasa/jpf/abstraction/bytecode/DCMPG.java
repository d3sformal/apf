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

import gov.nasa.jpf.abstraction.AbstractValue;
import gov.nasa.jpf.abstraction.Abstraction;
import gov.nasa.jpf.abstraction.Attribute;
import gov.nasa.jpf.abstraction.predicate.common.Expression;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Instruction;

/**
 * Compare double
 * ..., value1, value2 => ..., result
 */
public class DCMPG extends gov.nasa.jpf.jvm.bytecode.DCMPG implements AbstractBinaryOperator<Double> {

	DoubleComparatorExecutor executor = DoubleComparatorExecutor.getInstance();
	
	@Override
	public Instruction execute(ThreadInfo ti) {
		
		/**
		 * Delegates the call to a shared object that does all the heavy lifting
		 */
		return executor.execute(this, ti);
	}

	@Override
	public Attribute getResult(Double v1, Attribute attr1, Double v2, Attribute attr2) {
		AbstractValue abs_v1 = null;
		AbstractValue abs_v2 = null;
		
		if (attr1 != null) {
			abs_v1 = attr1.abstractValue;
		}
		if (attr2 != null) {
			abs_v2 = attr2.abstractValue;
		}

		/**
		 * Performs the adequate operation over abstractions
		 */
		return new Attribute(Abstraction._cmpg(v1, abs_v1, v2, abs_v2), null);
	}

	@Override
	public Instruction executeConcrete(ThreadInfo ti) {
		
		/**
		 * Ensures execution of the original instruction
		 */
		return super.execute(ti);
	}

	@Override
	public Instruction getSelf() {
		
		/**
		 * Ensures translation into an ordinary instruction
		 */
		return this;
	}

}