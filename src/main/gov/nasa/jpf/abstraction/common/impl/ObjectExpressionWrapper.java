package gov.nasa.jpf.abstraction.common.impl;

import java.util.List;
import java.util.Map;

import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.ObjectExpression;
import gov.nasa.jpf.abstraction.common.PredicatesVisitor;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.ReturnValue;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.predicate.state.SymbolTable;

/**
 * Wrapper which marks expressions as Object Expressions @see gov.nasa.jpf.abstraction.common.ObjectExpression
 */
public class ObjectExpressionWrapper extends DefaultObjectExpression {
	protected Expression expression;
	
	protected ObjectExpressionWrapper(Expression expression) {
		this.expression = expression;
	}
	
	public static ObjectExpression wrap(Expression expression, SymbolTable symbols) {
		if (expression instanceof ObjectExpression) {
			return (ObjectExpression) expression;
		}
		
		if (expression instanceof AccessExpression) {
			AccessExpression path = (AccessExpression) expression;
			
			if (path instanceof ReturnValue) {
				ReturnValue r = (ReturnValue) path;
				
				if (r.isReference()) {
					return ObjectExpressionWrapper.create(path);
				}
			}
			if (symbols.isObject(path)) {
				if (symbols.isArray(path)) {
					return ArrayExpressionWrapper.create(path);
				}
				
				return ObjectExpressionWrapper.create(path);
			}
		}
		
		throw new RuntimeException("Invalid cast to Object Expression " + expression.getClass().getSimpleName());
	}
	
	public static ObjectExpressionWrapper create(Expression expression) {
		if (expression == null) {
			return null;
		}
		
		return new ObjectExpressionWrapper(expression);
	}

	@Override
	public List<AccessExpression> getAccessExpressions() {
		return expression.getAccessExpressions();
	}
	
	@Override
	public Expression replace(Map<AccessExpression, Expression> replacements) {
		return create(this.expression.replace(replacements));
	}

	@Override
	public Expression update(AccessExpression expression, Expression newExpression) {
		return create(this.expression.update(expression, newExpression));
	}

	@Override
	public void accept(PredicatesVisitor visitor) {
		expression.accept(visitor);
	}

	@Override
	public DefaultObjectExpression clone() {
		return create(expression.clone());
	}

	@Override
	public Predicate preconditionForBeingFresh() {
		return expression.preconditionForBeingFresh();
	}
	
	@Override
	public boolean equals(java.lang.Object o) {
		return expression.equals(o);
	}
	
	@Override
	public int hashCode() {
		return expression.hashCode();
	}
}
