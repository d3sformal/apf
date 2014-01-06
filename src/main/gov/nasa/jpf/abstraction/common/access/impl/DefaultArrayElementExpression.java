package gov.nasa.jpf.abstraction.common.access.impl;

import java.util.List;

import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.ArrayElementExpression;
import gov.nasa.jpf.abstraction.common.access.meta.Arrays;

/**
 * Read/Write to an array element aread(arr, a, i); awrite(arr, a, i, e); 
 */
public abstract class DefaultArrayElementExpression extends DefaultArrayAccessExpression implements ArrayElementExpression {
	
	private Expression index;
	private Arrays arrays;

	protected DefaultArrayElementExpression(AccessExpression array, Arrays arrays, Expression index) {
		super(array);
		
		this.arrays = arrays;
		this.index = index;
	}
	
	@Override
	public Expression getIndex() {
		return index;
	}
	
	@Override
	public Arrays getArrays() {
		return arrays;
	}
	
	@Override
	public List<AccessExpression> getAccessSubExpressions() {
		List<AccessExpression> ret = super.getAccessSubExpressions();
		
		ret.addAll(arrays.getAccessSubExpressions());
		ret.addAll(index.getAccessExpressions());
		
		return ret;
	}
}
