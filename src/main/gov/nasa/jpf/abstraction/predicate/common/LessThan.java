package gov.nasa.jpf.abstraction.predicate.common;

import gov.nasa.jpf.abstraction.common.AccessPath;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.PredicatesVisitor;

public class LessThan extends Comparison {
	protected LessThan(Expression a, Expression b) {
		super(a, b);
	}
	
	@Override
	public void accept(PredicatesVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public LessThan replace(AccessPath formerPath, Expression expression) {
		return new LessThan(a.replace(formerPath, expression), b.replace(formerPath, expression));
	}
	
	public static Predicate create(Expression a, Expression b) {
		if (!argumentsDefined(a, b)) return null;
		
		return new LessThan(a, b);
	}
}
