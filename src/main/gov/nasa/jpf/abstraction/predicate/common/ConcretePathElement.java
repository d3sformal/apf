package gov.nasa.jpf.abstraction.predicate.common;

import gov.nasa.jpf.vm.ThreadInfo;

public interface ConcretePathElement extends AccessPathElement {
	public VariableID getVariableID(ThreadInfo ti);
}
