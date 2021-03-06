/*
 * Copyright (C) 2015, Charles University in Prague.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.nasa.jpf.abstraction.common;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import gov.nasa.jpf.abstraction.common.Conjunction;
import gov.nasa.jpf.abstraction.common.Contradiction;
import gov.nasa.jpf.abstraction.common.Disjunction;
import gov.nasa.jpf.abstraction.common.Equals;
import gov.nasa.jpf.abstraction.common.Implication;
import gov.nasa.jpf.abstraction.common.LessThan;
import gov.nasa.jpf.abstraction.common.MethodPredicateContext;
import gov.nasa.jpf.abstraction.common.ObjectPredicateContext;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.common.PredicateContext;
import gov.nasa.jpf.abstraction.common.Predicates;
import gov.nasa.jpf.abstraction.common.StaticPredicateContext;
import gov.nasa.jpf.abstraction.common.Tautology;
import gov.nasa.jpf.abstraction.common.UpdatedPredicate;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.Method;
import gov.nasa.jpf.abstraction.common.access.PackageAndClass;
import gov.nasa.jpf.abstraction.common.access.SpecialVariable;
import gov.nasa.jpf.abstraction.common.access.meta.impl.DefaultArrayLengths;
import gov.nasa.jpf.abstraction.common.impl.ArraysAssign;
import gov.nasa.jpf.abstraction.common.impl.FieldAssign;
import gov.nasa.jpf.abstraction.common.impl.New;
import gov.nasa.jpf.abstraction.common.impl.NullExpression;
import gov.nasa.jpf.abstraction.common.impl.VariableAssign;
import gov.nasa.jpf.abstraction.concrete.EmptyExpression;

/**
 * A special visitor of the hierarchy:
 *
 * predicates
 *   -> context
 *     -> predicate
 *       -> expression
 *
 * to be used to produce a string representation of the captured hierarchy.
 */
public abstract class PredicatesStringifier implements PredicatesComponentVisitor {

    public final Comparator<Expression> exprCmp = new Comparator<Expression>() {
        @Override
        public int compare(Expression e1, Expression e2) {
            return cmpExpr(e1, e2);
        }
    };

    public final static Comparator<Predicate> predCmp = new Comparator<Predicate>() {
        @Override
        public int compare(Predicate p1, Predicate p2) {
            return cmpPred(p1, p2);
        }
    };

    private static int cmpExpr(Expression e1, Expression e2) {
        if (e1 instanceof Constant) {
            Constant c1 = (Constant) e1;

            if (e2 instanceof Constant) {
                Constant c2 = (Constant) e2;

                return c1.value.intValue() - c2.value.intValue();
            }

            return -1;
        }

        if (e2 instanceof Constant) {
            return +1;
        }

        if (e1 instanceof AccessExpression) {
            if (e2 instanceof AccessExpression) {
                return e1.toString().compareTo(e2.toString());
            }

            return -1;
        }

        if (e2 instanceof AccessExpression) {
            return +1;
        }

        return e1.toString().compareTo(e2.toString());
    }

    private static int cmpPred(Predicate p1, Predicate p2) {
        if (p1 instanceof Equals) {
            Equals e1 = (Equals) p1;

            if (p2 instanceof Equals) {
                Equals e2 = (Equals) p2;

                int cmpA = cmpExpr(e1.a, e2.a);

                if (cmpA < 0) return -1;
                if (cmpA > 0) return +1;

                int cmpB = cmpExpr(e1.b, e2.b);

                return cmpB;
            }
        }
        if (p1 instanceof LessThan) {
            LessThan l1 = (LessThan) p1;

            if (p2 instanceof LessThan) {
                LessThan l2 = (LessThan) p2;

                int cmpA = cmpExpr(l1.a, l2.a);

                if (cmpA < 0) return -1;
                if (cmpA > 0) return +1;

                int cmpB = cmpExpr(l1.b, l2.b);

                return cmpB;
            }
        }
        return p1.toString().compareTo(p2.toString());
    }

    protected StringBuilder ret = new StringBuilder();
    protected boolean topmost = true;

    public String getString() {
        return ret.toString();
    }

    @Override
    public void visit(Predicates predicates) {
        for (PredicateContext c : predicates.contexts) {
            c.accept(this);
            ret.append("\n");
        }
    }

    @Override
    public void visit(Expressions expressions) {
        for (ExpressionContext c : expressions.contexts) {
            c.accept(this);
            ret.append("\n");
        }
    }

    private void visitPredicate(Predicate p) {
        BytecodeRange scope = p.getScope();

        if (!(scope instanceof BytecodeUnlimitedRange)) {
            ret.append(scope.toString());
            ret.append(": ");
        }

        p.accept(this);
        ret.append("\n");
    }

    @Override
    public void visit(ObjectPredicateContext context) {
        ret.append("[object ");

        context.getPackageAndClass().accept(this);

        ret.append("]\n");

        visitRawPredicateContext(context);
    }

    @Override
    public void visit(MethodPredicateContext context) {
        ret.append("[method ");

        context.getMethod().accept(this);

        ret.append("]\n");

        visitRawPredicateContext(context);
    }

    @Override
    public void visit(MethodAssumePrePredicateContext context) {
        ret.append("[method assume pre ");

        context.getMethod().accept(this);

        ret.append("]\n");

        visitRawPredicateContext(context);
    }

    @Override
    public void visit(MethodAssumePostPredicateContext context) {
        ret.append("[method assume post ");

        context.getMethod().accept(this);

        ret.append("]\n");

        visitRawPredicateContext(context);
    }

    @Override
    public void visit(StaticPredicateContext context) {
        ret.append("[static]\n");

        visitRawPredicateContext(context);
    }

    private void visitRawPredicateContext(PredicateContext context) {
        Set<Predicate> order = new TreeSet<Predicate>(predCmp);

        order.addAll(context.predicates.keySet());

        for (Predicate p : order) {
            visitPredicate(p);
        }
    }

    @Override
    public void visit(ObjectExpressionContext context) {
        ret.append("[object ");

        context.getPackageAndClass().accept(this);

        ret.append("]\n");

        visitRawExpressionContext(context);
    }

    @Override
    public void visit(MethodExpressionContext context) {
        ret.append("[method ");

        context.getMethod().accept(this);

        ret.append("]\n");

        visitRawExpressionContext(context);
    }

    @Override
    public void visit(StaticExpressionContext context) {
        ret.append("[static]\n");

        visitRawExpressionContext(context);
    }

    private void visitRawExpressionContext(ExpressionContext context) {
        Set<Expression> order = new TreeSet<Expression>(exprCmp);

        order.addAll(context.expressions);

        for (Expression e : order) {
            e.accept(this);
            ret.append("\n");
        }
    }

    @Override
    public void visit(Negation predicate) {
        ret.append("not(");

        predicate.predicate.accept(this);

        ret.append(")");
    }

    @Override
    public void visit(LessThan predicate) {
        predicate.a.accept(this);

        ret.append(" < ");

        predicate.b.accept(this);
    }

    @Override
    public void visit(Equals predicate) {
        predicate.a.accept(this);

        ret.append(" = ");

        predicate.b.accept(this);
    }

    @Override
    public void visit(Tautology predicate) {
        ret.append("true");
    }

    @Override
    public void visit(Contradiction predicate) {
        ret.append("false");
    }

    private void inlineConjunction(Predicate predicate) {
        if (predicate.getClass().equals(Conjunction.class)) {
            Conjunction c = (Conjunction) predicate;

            if (c.a.getClass().equals(Conjunction.class) && (c.b instanceof Comparison || c.b instanceof Assign)) {
                while (c.a.getClass().equals(Conjunction.class) && (c.b instanceof Comparison || c.b instanceof Assign)) {
                    c.b.accept(this);
                    ret.append(" and ");
                    c = (Conjunction) c.a;
                }
                c.accept(this);
            } else if (c.b.getClass().equals(Conjunction.class) && (c.a instanceof Comparison || c.a instanceof Assign)) {
                while (c.b.getClass().equals(Conjunction.class) && (c.a instanceof Comparison || c.a instanceof Assign)) {
                    c.a.accept(this);
                    ret.append(" and ");
                    c = (Conjunction) c.b;
                }
                c.accept(this);
            } else {
                inlineConjunction(c.a);

                ret.append(" and ");

                inlineConjunction(c.b);
            }
        } else {
            predicate.accept(this);
        }
    }

    @Override
    public void visit(Conjunction predicate) {
        boolean topmost = this.topmost;
        this.topmost = false;

        if (!topmost) ret.append("(");

        inlineConjunction(predicate.a);

        ret.append(" and ");

        inlineConjunction(predicate.b);

        if (!topmost) ret.append(")");
    }

    private void inlineDisjunction(Predicate predicate) {
        if (predicate.getClass().equals(Disjunction.class)) {
            Disjunction d = (Disjunction) predicate;

            inlineDisjunction(d.a);

            ret.append(" or ");

            inlineDisjunction(d.b);
        } else {
            predicate.accept(this);
        }
    }

    @Override
    public void visit(Disjunction predicate) {
        boolean topmost = this.topmost;
        this.topmost = false;

        if (!topmost) ret.append("(");

        inlineDisjunction(predicate.a);

        ret.append(" or ");

        inlineDisjunction(predicate.b);

        if (!topmost) ret.append(")");
    }

    @Override
    public void visit(Implication predicate) {
        boolean topmost = this.topmost;
        this.topmost = false;

        if (!topmost) ret.append("(");

        predicate.a.accept(this);

        ret.append(" => ");

        predicate.b.accept(this);

        if (!topmost) ret.append(")");
    }

    @Override
    public void visit(VariableAssign predicate) {
        predicate.variable.accept(this);

        ret.append(" = ");

        predicate.expression.accept(this);
    }

    @Override
    public void visit(FieldAssign predicate) {
        predicate.field.accept(this);

        ret.append(" = ");

        predicate.newField.accept(this);
    }

    @Override
    public void visit(ArraysAssign predicate) {
        predicate.arrays.accept(this);

        ret.append(" = ");

        predicate.newArrays.accept(this);
    }

    @Override
    public void visit(New predicate) {
        ret.append("ref(");
        predicate.object.accept(this);

        ret.append(") = ");

        ret.append(predicate.object.getReference().getReferenceNumber());
    }

    @Override
    public void visit(EmptyExpression expression) {
        ret.append(" ? ");
    }

    @Override
    public void visit(NullExpression expression) {
        ret.append("null");
    }

    @Override
    public void visit(Add expression) {
        ret.append("(");

        expression.a.accept(this);

        ret.append(" + ");

        expression.b.accept(this);

        ret.append(")");
    }

    @Override
    public void visit(Subtract expression) {
        ret.append("(");

        expression.a.accept(this);

        ret.append(" - ");

        expression.b.accept(this);

        ret.append(")");
    }

    @Override
    public void visit(Multiply expression) {
        ret.append("(");

        expression.a.accept(this);

        ret.append(" * ");

        expression.b.accept(this);

        ret.append(")");
    }

    @Override
    public void visit(Divide expression) {
        ret.append("(");

        expression.a.accept(this);

        ret.append(" / ");

        expression.b.accept(this);

        ret.append(")");
    }

    @Override
    public void visit(Modulo expression) {
        ret.append("(");

        expression.a.accept(this);

        ret.append(" % ");

        expression.b.accept(this);

        ret.append(")");
    }

    @Override
    public void visit(UninterpretedShiftLeft expression) {
        ret.append("SHL(");

        expression.a.accept(this);

        ret.append(", ");

        expression.b.accept(this);

        ret.append(")");
    }

    @Override
    public void visit(UninterpretedShiftRight expression) {
        ret.append("SHR(");

        expression.a.accept(this);

        ret.append(", ");

        expression.b.accept(this);

        ret.append(")");
    }

    @Override
    public void visit(Constant expression) {
        ret.append(expression.value);
    }

    @Override
    public void visit(DefaultArrayLengths meta) {
        ret.append("arrlen");
    }

    @Override
    public void visit(Undefined expression) {
        ret.append("<<UNDEFINED>>");
    }

    @Override
    public void visit(UpdatedPredicate predicate) {
        predicate.apply().accept(this);
    }

    @Override
    public void visit(PackageAndClass packageAndClass) {
        ret.append(packageAndClass.getName());
    }

    @Override
    public void visit(Method method) {
        method.getPackageAndClass().accept(this);

        ret.append(".");

        ret.append(method.getName());
    }

    @Override
    public void visit(SpecialVariable expression) {
        ret.append(expression.getName());
    }
}
