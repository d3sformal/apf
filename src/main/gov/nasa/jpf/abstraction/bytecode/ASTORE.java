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
import gov.nasa.jpf.abstraction.predicate.common.AccessPath;
import gov.nasa.jpf.abstraction.predicate.common.CompleteVariableID;
import gov.nasa.jpf.abstraction.predicate.common.ConcretePath;
import gov.nasa.jpf.abstraction.predicate.common.ScopedSymbolTable;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class ASTORE extends gov.nasa.jpf.jvm.bytecode.ASTORE {

	public ASTORE(int index) {
		super(index);
	}
	
	@Override
	public Instruction execute(ThreadInfo ti) {
		//TODO
		/*
		 * current objRef may have an access path in the attribute
		 * we need to find all paths in Symbol table whose prefix it is
		 * 
		 * those need to be copied
		 * copies need to be rerooted to start in this local variable
		 * new paths need to be registered in Symbol table
		 */
		StackFrame sf = ti.getModifiableTopFrame();
		LocalVarInfo var = getMethodInfo().getLocalVars()[index];

		String v1 = null;
		String v2 = null;
		String v3 = null;
		
		try { v1 = getLocalVarInfo().getName(); } catch (Exception e) {}
		try { v2 = sf.getLocalVarInfo(index).getName(); } catch (Exception e) {}
		try { v3 = getMethodInfo().getLocalVars()[index].getName(); } catch (Exception e) {}
		
		System.err.println("S " + ((v1 != null && v2 != null && v3 != null && v1.equals(v2) && v2.equals(v3)) || (v1 == v2 && v2 == v3 && v1 == null) ? "OK" : "EE") + " " + v1 + " " + v2 + " " + v3);

		ElementInfo ei = ti.getElementInfo(sf.getLocalVariable(index));

		Attribute attribute = (Attribute) sf.getOperandAttr();
		
		Instruction ret = super.execute(ti);
		
		if (attribute != null) {
			ConcretePath prefix = attribute.accessPath;

			for (AccessPath path : ScopedSymbolTable.getInstance().lookupAccessPaths(prefix)) {
				CompleteVariableID variableID = ScopedSymbolTable.getInstance().resolvePath(path);

				ConcretePath clone = (ConcretePath) path.clone();
				
				System.err.println(">> " + var.getName());
			
				AccessPath.reRoot(clone, prefix, new ConcretePath(var.getName(), ti, ei, ConcretePath.Type.HEAP));

				ScopedSymbolTable.getInstance().registerPathToVariable(clone, variableID);
			}
		}
		
		return ret;
	}
}
