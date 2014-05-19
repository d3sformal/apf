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
import gov.nasa.jpf.abstraction.impl.NonEmptyAttribute;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;

import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Multiply;
import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Equals;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.common.Conjunction;
import gov.nasa.jpf.abstraction.common.Disjunction;
import gov.nasa.jpf.abstraction.GlobalAbstraction;
import gov.nasa.jpf.abstraction.predicate.state.TruthValue;
import gov.nasa.jpf.abstraction.util.RunDetector;

/**
 * And integer
 * ..., value1, value2 => ..., result
 */
public class IAND extends gov.nasa.jpf.jvm.bytecode.IAND implements AbstractBinaryOperator<Integer> {

	IntegerBinaryOperatorExecutor executor = IntegerBinaryOperatorExecutor.getInstance();
	
	@Override
	public Instruction execute(ThreadInfo ti) {
		
		/**
		 * Delegates the call to a shared object that does all the heavy lifting
		 */
		return executor.execute(this, ti);
	}

	@Override
	public NonEmptyAttribute getResult(Integer v1, Attribute attr1, Integer v2, Attribute attr2) {
		AbstractValue abs_v1 = attr1.getAbstractValue();
		AbstractValue abs_v2 = attr2.getAbstractValue();

        Expression a = attr1.getExpression();
        Expression b = attr2.getExpression();

        if (RunDetector.isRunning()) {
            Predicate inSupportedDomain = Conjunction.create(
                Disjunction.create(
                    Equals.create(a, Constant.create(0)),
                    Equals.create(a, Constant.create(1))
                ),
                Disjunction.create(
                    Equals.create(b, Constant.create(0)),
                    Equals.create(b, Constant.create(1))
                )
            );

            TruthValue value = (TruthValue) GlobalAbstraction.getInstance().processBranchingCondition(inSupportedDomain);

            if (value != TruthValue.TRUE) {
                throw new IllegalArgumentException("logical & bitwise operations over values other than {0, 1} are unsupported");
            }
        }

		/**
		 * Performs the adequate operation over abstractions
		 */
        // Assume input values to be either 0 or 1 (logical)
        // Other values will result in errors (bitwise)
        //
        // Therefore:
        // MUL(a, b) = AND(a, b)
		return new NonEmptyAttribute(Abstraction._and(v1, abs_v1, v2, abs_v2), Multiply.create(a, b));
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
