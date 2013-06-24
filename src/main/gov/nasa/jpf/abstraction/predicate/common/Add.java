package gov.nasa.jpf.abstraction.predicate.common;

public class Add extends Operation {
	public Add(Expression a, Expression b) {
		super(a, b);
	}
	
	@Override
	public String toString() {
		return "(" + a.toString() + " + " + b.toString() + ")";
	}
}
