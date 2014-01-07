package gov.nasa.jpf.abstraction.common.access.impl;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Notation;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.ObjectAccessExpression;
import gov.nasa.jpf.abstraction.common.access.PackageAndClass;
import gov.nasa.jpf.abstraction.common.access.ReturnValue;
import gov.nasa.jpf.abstraction.common.access.Root;
import gov.nasa.jpf.abstraction.common.impl.DefaultObjectExpression;
import gov.nasa.jpf.abstraction.predicate.parser.PredicatesLexer;
import gov.nasa.jpf.abstraction.predicate.parser.PredicatesParser;
import gov.nasa.jpf.abstraction.predicate.smt.PredicatesSMTStringifier;

/**
 * Implements common behaviour of most of the access expression elements
 */
public abstract class DefaultAccessExpression extends DefaultObjectExpression implements AccessExpression {

	/**
	 * Retrieves all access expressions present in this expression
	 * 
	 * that is:
	 * 1) this expression itself
	 * 2) any access expression present in an array index
	 * 3) any access expression present in an update expression (fwrite, awrite, alengthupdate ...)
	 */
	@Override
	public final List<AccessExpression> getAccessExpressions() {
		List<AccessExpression> ret = getAccessSubExpressions();
		
		ret.add(this);
		
		return ret;
	}

	@Override
	public final List<AccessExpression> getAllPrefixes() {
		List<AccessExpression> ret = new LinkedList<AccessExpression>();

		AccessExpression prefix = this;

		while (!(prefix instanceof Root)) {
			ret.add(prefix);
			prefix = prefix.cutTail();
		}

		ret.add(prefix);

		return ret;
	}

	/**
	 * Replaces occurances of access expressions by other expressions 
	 */
	@Override
	public final Expression replace(Map<AccessExpression, Expression> replacements) {
		for (AccessExpression expression : replacements.keySet()) {
			Expression newExpression = replacements.get(expression);

			if (equals(expression)) {
				return newExpression;
			}
		}
		
		AccessExpression path = this;
		
		for (AccessExpression expression : replacements.keySet()) {
			Expression newExpression = replacements.get(expression);

			if (expression.isPrefixOf(path) && newExpression instanceof AccessExpression) {
				AccessExpression newPrefix = ((AccessExpression)newExpression).clone();

				path = path.reRoot(expression, newPrefix);

				break; // Do not chain the replacements
			}
		}
		
		return path.replaceSubExpressions(replacements);
	}
	
	/**
	 * Transforms the access expression in such a way that it reflects all updates (fwrite, awrite...) in affected subexpressions (fread, aread).
	 * 
	 * update(fread(f, o), o.f, 3) = fread(fwrite(f, o, 3), o)
	 * update(fread(f, o), p.f, 3) = fread(fwrite(f, p, 3), o)
	 * ...
	 */
	@Override
	public Expression update(AccessExpression expression, Expression newExpression) {
		return clone();
	}
	
	@Override
	public abstract DefaultAccessExpression clone();

	/**
	 * Is this expression referring (entirely) to the java keyword this
	 */
	@Override
	public final boolean isThis() {
		if (this instanceof Root) {
			Root r = (Root) this;
			
			return r.getName().equals("this");
		}
		
		return false;
	}
	
	/**
	 * Is the expression accessing a variable through a static context
	 */
	@Override
	public final boolean isStatic() {
		return getRoot() instanceof PackageAndClass;
	}
	
	/**
	 * Is the expression accessing a local variable
	 */
	@Override
	public final boolean isLocalVariable() {
		return this instanceof Root && !isStatic() && !isReturnValue();
	}

	/**
	 * Is the expression referring to the keyword return
	 */
	@Override
	public final boolean isReturnValue() {
		return this instanceof ReturnValue;
	}
	
	/**
	 * Is this expression a prefix of the supplied expression
	 * 
	 * a.b.c is a prefix of a.b.c, a.b.c.d ...
	 * a.b.c is not a prefix of a.b, a.b.d ...
	 */
	@Override
	public boolean isPrefixOf(AccessExpression path) {
		if (getLength() > path.getLength()) {
			return false;
		}
		
		return equals(path.get(getLength()));
	}
	
	/**
	 * Is the expression a prefix (up to different array indices) of the supplied expression
	 * 
	 * a[i] is similar to a prefix of a[k], a[k].b, a[i][d]
	 * a[i] is not similar to a prefix of b[i], a.b
	 */
	@Override
	public boolean isSimilarToPrefixOf(AccessExpression path) {
		if (getLength() > path.getLength()) {
			return false;
		}
		
		return isSimilarTo(path.get(getLength()));
	}
	
	/**
	 * Is the expression a prefix of the supplied expression and is its length strictly lower
	 * 
	 * a.b is a proper prefix of a.b.c but not a proper prefix of a.b
	 */
	@Override
	public final boolean isProperPrefixOf(AccessExpression expression) {
		return getLength() < expression.getLength() && isPrefixOf(expression);
	}
	
	public static DefaultAccessExpression createFromString(String definition) {
		ANTLRInputStream chars = new ANTLRInputStream(definition);
		PredicatesLexer lexer = new PredicatesLexer(chars);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		PredicatesParser parser = new PredicatesParser(tokens);
		
		return parser.standalonepath().val;
	}
	
	/**
	 * Changes the prefix of this expression from oldPrefix to newPrefix
	 * 
	 * reRoot(a.b.c.d, a.b, x.y) = x.y.c.d
	 */
	@Override
	public final AccessExpression reRoot(AccessExpression oldPrefix, AccessExpression newPrefix) {
		if (oldPrefix.isPrefixOf(this)) {
			AccessExpression clone = clone();
			ObjectAccessExpression parent = (ObjectAccessExpression) clone.get(oldPrefix.getLength() + 1);
			
			if (parent == null) {
				return newPrefix;
			}
									
			parent.setObject(parent.getObject().reRoot(newPrefix));
			
			return clone;
		}
		
		return null;
	}
	
	public static void main(String[] args) {
		Notation.policy = Notation.DOT_NOTATION;
		
		AccessExpression p = createFromString("a[a.length - 1]");
		
		AccessExpression a = createFromString("a");
		AccessExpression c = createFromString("c");
		
		Expression e = p.replace(a, c);
		
		System.out.println(e /*+ " " + e.getPaths()*/);
		
		AccessExpression x = createFromString("x.x");		
		AccessExpression y = createFromString("x");
		AccessExpression z = createFromString("y");
		x = x.reRoot(y, z);
		
		System.out.println(x + " " + x.getClass().getSimpleName());
		
		AccessExpression e1;
		AccessExpression e2;
		
		e1 = DefaultObjectFieldRead.create(DefaultArrayElementRead.create(DefaultObjectFieldRead.create(DefaultRoot.create("this"), DefaultObjectFieldWrite.create(DefaultRoot.create("this"), "rectangles", DefaultFresh.create())), Constant.create(0)), "bottom");
		e2 = DefaultObjectFieldRead.create(DefaultArrayElementRead.create(DefaultObjectFieldRead.create(DefaultRoot.create("this"), DefaultObjectFieldWrite.create(DefaultRoot.create("this"), "rectangles", DefaultFresh.create())), Constant.create(0)), "bottom");
		
		e1 = DefaultArrayElementRead.create(DefaultObjectFieldRead.create(DefaultRoot.create("this"), DefaultObjectFieldWrite.create(DefaultRoot.create("this"), "rectangles", DefaultFresh.create())), Constant.create(0));
		e2 = DefaultArrayElementRead.create(DefaultObjectFieldRead.create(DefaultRoot.create("this"), DefaultObjectFieldWrite.create(DefaultRoot.create("this"), "rectangles", DefaultFresh.create())), Constant.create(0));
		
		e1 = DefaultObjectFieldRead.create(DefaultRoot.create("this"), DefaultObjectFieldWrite.create(DefaultRoot.create("this"), "rectangles", DefaultFresh.create()));
		e2 = DefaultObjectFieldRead.create(DefaultRoot.create("this"), DefaultObjectFieldWrite.create(DefaultRoot.create("this"), "rectangles", DefaultFresh.create()));
		
		System.out.println(Notation.convertToString(e1, new PredicatesSMTStringifier()));
		System.out.println(Notation.convertToString(e2, new PredicatesSMTStringifier()));
		System.out.println(e1.equals(e2) + " " + e1.hashCode() + " " + e2.hashCode());
	}

}
