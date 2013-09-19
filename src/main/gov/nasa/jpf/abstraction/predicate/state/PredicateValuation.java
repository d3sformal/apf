package gov.nasa.jpf.abstraction.predicate.state;

import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Predicate;

public interface PredicateValuation {
	public void put(Predicate predicate, TruthValue value);
	public void putAll(Map<Predicate, TruthValue> values);
	public void remove(Predicate predicate);
	public boolean containsKey(Predicate predicate);
	public TruthValue get(Predicate predicate);
	public Set<Predicate> getPredicates();
	public void reevaluate(AccessExpression affected, Set<AccessExpression> resolvedAffected, Expression expression);
	public TruthValue evaluatePredicate(Predicate predicate);
	public Map<Predicate, TruthValue> evaluatePredicates(Set<Predicate> predicates);
}
