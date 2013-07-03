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

import java.util.Map;

import gov.nasa.jpf.abstraction.Attribute;
import gov.nasa.jpf.abstraction.predicate.concrete.CompleteVariableID;
import gov.nasa.jpf.abstraction.predicate.concrete.ConcretePath;
import gov.nasa.jpf.abstraction.predicate.grammar.AccessPath;
import gov.nasa.jpf.abstraction.predicate.state.ScopedSymbolTable;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

public class AASTORE extends gov.nasa.jpf.jvm.bytecode.AASTORE {
	
	@Override
	public Instruction execute(ThreadInfo ti) {
		StackFrame sf = ti.getModifiableTopFrame();
		
        Attribute source = (Attribute) sf.getOperandAttr(0);
		Attribute destination = (Attribute) sf.getOperandAttr(2);

		Instruction ret = super.execute(ti);

		if (destination != null) {
			ConcretePath pathRoot = destination.accessPath;
		
			if (pathRoot != null) {
				pathRoot.appendIndexElement(null);
			
                if (source == null) {
                	Map<AccessPath, CompleteVariableID> vars = pathRoot.resolve();
    				
    				for (AccessPath p : vars.keySet()) {
    					ScopedSymbolTable.getInstance().registerPathToVariable(p, vars.get(p));
    				}
                } else {
                    ConcretePath prefix = source.accessPath;

                    if (prefix != null) {
                    	System.err.println(pathRoot.toString(AccessPath.NotationPolicy.DOT_NOTATION) + " := " + prefix.toString(AccessPath.NotationPolicy.DOT_NOTATION));
                        for (AccessPath path : ScopedSymbolTable.getInstance().lookupAccessPaths(prefix)) {
            				CompleteVariableID variableID = ScopedSymbolTable.getInstance().resolvePath(path);

		    		        
                            for (AccessPath newPrefix : pathRoot.partialResolve().keySet()) {
                            	AccessPath newPath = path.clone();
                            	AccessPath.reRoot(newPath, prefix, newPrefix);
                            	
                            	System.err.println("\t" + newPath.toString(AccessPath.NotationPolicy.DOT_NOTATION) + " := " + path.toString(AccessPath.NotationPolicy.DOT_NOTATION));
                            	
    		            		ScopedSymbolTable.getInstance().registerPathToVariable(newPath, variableID);
                            }
			            }
                    }
                }
			}
		}
		
		return ret;
	}
}