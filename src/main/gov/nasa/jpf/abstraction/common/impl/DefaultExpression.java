package gov.nasa.jpf.abstraction.common.impl;

import java.util.Map;
import java.util.HashMap;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Notation;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.predicate.parser.PredicatesLexer;
import gov.nasa.jpf.abstraction.predicate.parser.PredicatesParser;

/**
 * Implementation of stringification common to all expressions
 */
public abstract class DefaultExpression implements Expression {

    @Override
    public Expression replace(AccessExpression original, Expression replacement) {
        Map<AccessExpression, Expression> replacements = new HashMap<AccessExpression, Expression>();

        replacements.put(original, replacement);

        return replace(replacements);
    }

	@Override
	public final String toString() {
		return toString(Notation.policy);
	}

	@Override
	public final String toString(Notation policy) {
		return Notation.convertToString(this, policy);
	}
	
	@Override
	public abstract DefaultExpression clone();

	public static Expression createFromString(String definition) {
		ANTLRInputStream chars = new ANTLRInputStream(definition);
		PredicatesLexer lexer = new PredicatesLexer(chars);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		PredicatesParser parser = new PredicatesParser(tokens);

		return parser.expressionorreturn().val;
	}

}
