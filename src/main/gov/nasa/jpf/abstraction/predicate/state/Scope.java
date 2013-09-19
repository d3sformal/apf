package gov.nasa.jpf.abstraction.predicate.state;

/**
 * An interface for all structures that model a particular runtime method scope.
 */
public interface Scope extends Cloneable {
	public Scope clone();
	public int count();
}
