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
import gov.nasa.jpf.abstraction.predicate.common.ConcretePath;
import gov.nasa.jpf.abstraction.predicate.common.Number;
import gov.nasa.jpf.abstraction.predicate.common.ScopedSymbolTable;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class GETSTATIC extends gov.nasa.jpf.jvm.bytecode.GETSTATIC {
	
	public GETSTATIC(String fieldName, String classType, String fieldDescriptor) {
		super(fieldName, classType, fieldDescriptor);
	}

	@Override
	public Instruction execute(ThreadInfo ti) {		
		StackFrame sf = ti.getModifiableTopFrame();
		
		ConcretePath path = new ConcretePath(getClassName(), getClassInfo(), ConcretePath.Type.STATIC);		
		Instruction ret = super.execute(ti);
		
		if (path != null) {
			path.appendSubElement(getFieldName());
			
			Number number = path.resolve();
			
			if (number != null) {
				ScopedSymbolTable.getInstance().register(path, number);
			}
		}
		
		sf.setOperandAttr(new Attribute(null, path));
		
		return ret;
	}
}