package gov.nasa.jpf.abstraction.predicate.common;

public class AccessPathMiddleElement extends AccessPathElement {
	public AccessPathElement previous;
	
	public AccessPathMiddleElement(AccessPathElement previous) {
		super(null);

		this.previous = previous;
	}
}
