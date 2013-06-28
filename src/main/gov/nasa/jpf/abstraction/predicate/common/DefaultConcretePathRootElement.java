package gov.nasa.jpf.abstraction.predicate.common;

import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.ThreadInfo;

public class DefaultConcretePathRootElement extends DefaultAccessPathRootElement implements ConcretePathRootElement {

	private Object rootObject;
	private ConcretePath.Type type;

	public DefaultConcretePathRootElement(String name, Object rootObject, ConcretePath.Type type) {
		super(name);

		this.rootObject = rootObject;
		this.type = type;
	}

	@Override
	public ConcretePath.Type getType() {
		return type;
	}
	
	@Override
	public void setNext(AccessPathMiddleElement element) {
		switch (type) {
		case LOCAL:
			throw new RuntimeException("Cannot access structure of a primitive type.");
		case STATIC:
			if (element instanceof AccessPathIndexElement) {
				throw new RuntimeException("Cannot access a class as an array.");
			}
		default:
			super.setNext(element);
		}
	}

	@Override
	public Object getObject(ThreadInfo ti) {
		/**
		 * If the path is of a primitive form (one element)
		 * it necessarily refers to a primitive local variable.
		 */		
		switch (type) {
		case LOCAL:
			LocalVarInfo info = (LocalVarInfo) rootObject;

			return new LocalVariableID(info.getName(), info.getSlotIndex());
		}

		return rootObject;
	}

}
