grammar Interpolants;

@header {
    import java.util.AbstractMap;
    import java.util.HashMap;
    import java.util.HashSet;
    import java.util.List;
    import java.util.LinkedList;
    import java.util.Map;
    import java.util.Set;
    import java.util.SortedSet;
    import java.util.TreeSet;

    import gov.nasa.jpf.vm.ElementInfo;
    import gov.nasa.jpf.vm.ThreadInfo;

    import gov.nasa.jpf.abstraction.PandaConfig;
    import gov.nasa.jpf.abstraction.PredicateAbstraction;
    import gov.nasa.jpf.abstraction.common.*;
    import gov.nasa.jpf.abstraction.common.impl.*;
    import gov.nasa.jpf.abstraction.common.access.*;
    import gov.nasa.jpf.abstraction.common.access.impl.*;
    import gov.nasa.jpf.abstraction.common.access.meta.*;
    import gov.nasa.jpf.abstraction.common.access.meta.impl.*;
    import gov.nasa.jpf.abstraction.concrete.*;
    import gov.nasa.jpf.abstraction.smt.*;
    import gov.nasa.jpf.abstraction.state.universe.*;
    import gov.nasa.jpf.abstraction.util.*;
}

@members{
}

predicates returns [Predicate[] val]
    : '(' p=predicatelist ')' {
        $ctx.val = $p.val;
    }
    ;

predicatelist returns [Predicate[] val]
    : /* EMPTY */ {
        $ctx.val = new Predicate[0];
    }
    | ps=predicatelist p=predicate {
        $ctx.val = new Predicate[$ps.val.length + 1];

        for (int i = 0; i < $ps.val.length; ++i) {
            $ctx.val[i] = $ps.val[i];
        }

        $ctx.val[$ps.val.length] = $p.val;
    }
    ;

letpair returns [Map.Entry<String, Object> val]
    : '(' '.' v=ID_TOKEN e=expression ')' {
        $ctx.val = new AbstractMap.SimpleEntry<String, Object>($v.text, $e.val);
    }
    | '(' v=ID_TOKEN '!' n=CONSTANT_TOKEN e=expression ')' {
        $ctx.val = new AbstractMap.SimpleEntry<String, Object>($v.text + '!' + Integer.parseInt($n.text), $e.val);
    }
    | '(' '.' v=ID_TOKEN p=predicate ')' {
        $ctx.val = new AbstractMap.SimpleEntry<String, Object>($v.text, $p.val);
    }
    | '(' v=ID_TOKEN '!' n=CONSTANT_TOKEN p=predicate ')' {
        $ctx.val = new AbstractMap.SimpleEntry<String, Object>($v.text + '!' + Integer.parseInt($n.text), $p.val);
    }
    ;

standalonepredicate returns [Predicate val]
    : p=predicate {
        $ctx.val = $p.val;
    }
    ;

qvar returns [String val]
    : '%' {$ctx.val = "%";} n=CONSTANT_TOKEN {$ctx.val += $n.text;} ( '!' {$ctx.val += "!";} m=CONSTANT_TOKEN {$ctx.val += $m.text;} )*
    ;

predicate returns [Predicate val] locals [static ScopedDefineMap let = new ScopedDefineMap(); Predicate acc;]
    : '.' id=ID_TOKEN {
        $ctx.val = (Predicate) PredicateContext.let.get($id.text);
    }
    | id=ID_TOKEN '!' n=CONSTANT_TOKEN {
        $ctx.val = (Predicate) PredicateContext.let.get($id.text + '!' + Integer.parseInt($n.text));
    }
    | '(' LET_TOKEN {PredicateContext.let.enterNested();} '(' (l=letpair {PredicateContext.let.put($l.val.getKey(), $l.val.getValue());})* ')' p=predicate ')' {
        PredicateContext.let.exitNested();

        $ctx.val = $p.val;
    }
    | TRUE_TOKEN {
        $ctx.val = Tautology.create();
    }
    | FALSE_TOKEN {
        $ctx.val = Contradiction.create();
    }
    | '(' AND_TOKEN ((p=predicate {$ctx.acc = $p.val;}) | (qv=qvar {$ctx.acc = (Predicate) PredicateContext.let.get($qv.val);})) ((q=predicate {$ctx.acc = Conjunction.create($ctx.acc, $q.val);}) | ( qv=qvar {$ctx.acc = Conjunction.create($ctx.acc, (Predicate) PredicateContext.let.get($qvar.val));} ))+ ')' {
        $ctx.val = $ctx.acc;
    }
    | '(' OR_TOKEN ((p=predicate {$ctx.acc = $p.val;}) | (qv=qvar {$ctx.acc = (Predicate) PredicateContext.let.get($qv.val);})) ((q=predicate {$ctx.acc = Disjunction.create($ctx.acc, $q.val);}) | ( qv=qvar {$ctx.acc = Disjunction.create($ctx.acc, (Predicate) PredicateContext.let.get($qvar.val));} ))+ ')' {
        $ctx.val = $ctx.acc;
    }
    | '(' NOT_TOKEN p=predicate ')' {
        $ctx.val = Negation.create($p.val);
    }
    | '(' ITE_TOKEN p=predicate q=predicate r=predicate ')' {
        $ctx.val = Disjunction.create(Conjunction.create($p.val, $q.val), Conjunction.create(Negation.create($p.val), $r.val));
    }
    | '(' FORALL_TOKEN {PredicateContext.let.enterNested(); String vars = "";} '(' ( '(' qv=qvar TYPE_TOKEN ')' {vars += " " + $qvar.val; PredicateContext.let.put($qvar.val, DefaultRoot.create($qvar.val));} )+ ')' '(' '!' p=predicate ( ':pattern' '(' expression+ ')' )* ':qid' 'itp' ')' ')' {
        PredicateContext.let.exitNested();

        if (PandaConfig.getInstance().enabledVerbose(this.getClass())) {
            InterpolantExtractor.println("[WARNING] Omitting quantifier in FOR ALL" + vars + ": " + $p.val);
        }

        // Collect values that the quantified variables may possess
        Map<Root, Set<Expression>> eqOptions = new HashMap<Root, Set<Expression>>();

        InterpolantExtractor.collectQuantifiedVarValues($p.val, eqOptions);

        Predicate p = InterpolantExtractor.expandQuantifiedVars($p.val, eqOptions);

        if (PandaConfig.getInstance().enabledVerbose(this.getClass())) {
            InterpolantExtractor.println("[WARNING] Expanding quantified formula FOR ALL" + vars + " using " + eqOptions + ": ");
            System.out.print("[WARNING] \t\t");
            InterpolantExtractor.println(p.toString());
        }

        $ctx.val = p;
    }
    | '(' EXISTS_TOKEN {PredicateContext.let.enterNested(); String vars = "";} '(' ( '(' qv=qvar TYPE_TOKEN ')' {vars += " " + $qvar.val; PredicateContext.let.put($qvar.val, DefaultRoot.create($qvar.val));} )+ ')' '(' '!' p=predicate ( ':pattern' '(' expression+ ')' )* ':qid' 'itp' ')' ')' {
        PredicateContext.let.exitNested();

        if (PandaConfig.getInstance().enabledVerbose(this.getClass())) {
            InterpolantExtractor.println("[WARNING] Omitting quantifier in EXISTS " + vars + ": " + $p.val);
        }

        // Collect values that the quantified variables may possess
        Map<Root, Set<Expression>> eqOptions = new HashMap<Root, Set<Expression>>();

        InterpolantExtractor.collectQuantifiedVarValues($p.val, eqOptions);

        Predicate p = InterpolantExtractor.expandQuantifiedVars($p.val, eqOptions);

        if (PandaConfig.getInstance().enabledVerbose(this.getClass())) {
            InterpolantExtractor.println("[WARNING] Expanding quantified formula EXISTS" + vars + " using " + eqOptions + ": ");
            System.out.print("[WARNING] \t\t");
            InterpolantExtractor.println(p.toString());
        }

        $ctx.val = p;
    }
    | '(=>' p=predicate q=predicate ')' {
        $ctx.val = Disjunction.create(Negation.create($p.val), $q.val);
    }
    | '(=' '.' id1=ID_TOKEN '.' id2=ID_TOKEN ')' {
        Object o1 = PredicateContext.let.get($id1.text);
        Object o2 = PredicateContext.let.get($id2.text);

        if (o1 instanceof Predicate && o2 instanceof Predicate) {
            Predicate p = (Predicate) o1;
            Predicate q = (Predicate) o2;

            $ctx.val = Disjunction.create(Conjunction.create(p, q), Conjunction.create(Negation.create(p), Negation.create(q)));
        }

        if (o1 instanceof Expression && o2 instanceof Expression) {
            Expression a = (Expression) o1;
            Expression b = (Expression) o2;

            $ctx.val = InterpolantExtractor.eq(a, b);
        }
    }
    | '(=' id1=ID_TOKEN '!' n1=CONSTANT_TOKEN id2=ID_TOKEN '!' n2=CONSTANT_TOKEN ')' {
        Object o1 = PredicateContext.let.get($id1.text + '!' + Integer.parseInt($n1.text));
        Object o2 = PredicateContext.let.get($id2.text + '!' + Integer.parseInt($n2.text));

        if (o1 instanceof Predicate && o2 instanceof Predicate) {
            Predicate p = (Predicate) o1;
            Predicate q = (Predicate) o2;

            $ctx.val = Disjunction.create(Conjunction.create(p, q), Conjunction.create(Negation.create(p), Negation.create(q)));
        }

        if (o1 instanceof Expression && o2 instanceof Expression) {
            Expression a = (Expression) o1;
            Expression b = (Expression) o2;

            $ctx.val = InterpolantExtractor.eq(a, b);
        }
    }
    | '(=' p=predicate q=predicate ')' {
        $ctx.val = Disjunction.create(Conjunction.create($p.val, $q.val), Conjunction.create(Negation.create($p.val), Negation.create($q.val)));
    }
    | '(' DISTINCT_TOKEN a=expression b=expression ')' {
        $ctx.val = Negation.create(InterpolantExtractor.eq($a.val, $b.val));
    }
    | '(=' a=expression b=expression ')' {
        $ctx.val = InterpolantExtractor.eq($a.val, $b.val);
    }
    | '(=' arr=ARR_TOKEN b=expression ')' {
        $ctx.val = InterpolantExtractor.eq(DefaultArrays.create(), $b.val);
    }
    | '(=' a=expression arr=ARR_TOKEN ')' {
        $ctx.val = InterpolantExtractor.eq(DefaultArrays.create(), $a.val);
    }
    | '(=' a=expression NULL_TOKEN ')' {
        $ctx.val = InterpolantExtractor.eq($a.val, NullExpression.create());
    }
    | '(=' NULL_TOKEN b=expression ')' {
        $ctx.val = InterpolantExtractor.eq(NullExpression.create(), $b.val);
    }
    | NULL_TOKEN '=' NULL_TOKEN {
        $ctx.val = Tautology.create();
    }
    | '(<' a=expression b=expression ')' {
        $ctx.val = InterpolantExtractor.lt($a.val, $b.val);
    }
    | '(>' a=expression b=expression ')' {
        $ctx.val = InterpolantExtractor.lt($b.val, $a.val);
    }
    | '(<=' a=expression b=expression ')' {
        $ctx.val = InterpolantExtractor.lt($b.val, $a.val);
    }
    | '(>=' a=expression b=expression ')' {
        $ctx.val = InterpolantExtractor.lt($a.val, $b.val);
    }
    | '(=' ARRLEN_TOKEN ARRLEN_TOKEN ')' {
        $ctx.val = Tautology.create();
    }
    ;

expression returns [Expression val] locals [Expression acc]
    : t=term {
        $ctx.val = $t.val;
    }
    | '(+' a=term {$ctx.acc = $a.val;} (b=term {$ctx.acc = Add.create($ctx.acc, $b.val);})+ ')' {
        $ctx.val = $ctx.acc;
    }
    | '(-' a=term b=term ')' {
        $ctx.val = Subtract.create($a.val, $b.val);
    }
    ;

term returns [Expression val]
    : f=factor {
        $ctx.val = $f.val;
    }
    | '(*' a=factor {Expression e = $a.val;} (b=factor {e = Multiply.create(e, $b.val);})+ ')' {
        $ctx.val = e;
    }
    | '(/' a=factor b=factor ')' {
        $ctx.val = Divide.create($a.val, $b.val);
    }
    ;

factor returns [Expression val]
    : '.' id=ID_TOKEN {
        $ctx.val = (Expression) PredicateContext.let.get($id.text);
    }
    | id=ID_TOKEN '!' n=CONSTANT_TOKEN {
        $ctx.val = (Expression) PredicateContext.let.get($id.text + '!' + Integer.parseInt($n.text));
    }
    | qv=qvar {
        $ctx.val = (Expression) PredicateContext.let.get($qvar.val);
    }
    | CONSTANT_TOKEN {
        $ctx.val = Constant.create(Integer.parseInt($CONSTANT_TOKEN.text));
    }
    | '(' SELECT_TOKEN ARRLEN_TOKEN p=path ')' {
        $ctx.val = DefaultArrayLengthRead.create($p.val);
    }
    | '(' SELECT_TOKEN ARRLEN_TOKEN qv=qvar ')' {
        AccessExpression ae = (AccessExpression) PredicateContext.let.get($qvar.val);

        $ctx.val = DefaultArrayLengthRead.create(ae);
    }
    | p=path {
        $ctx.val = $p.val;
    }
    | '(' ITE_TOKEN pCond=predicate e1=expression e2=expression ')' {
        $ctx.val = IfThenElse.create($pCond.val, $e1.val, $e2.val);
    }
    | '(' ITE_TOKEN eCond=expression e1=expression e2=expression ')' {
        $ctx.val = IfThenElse.create($eCond.val, $e1.val, $e2.val);
    }
    | '(' REF_TOKEN p=path ')' {
        if ($p.val instanceof AnonymousObject) {
            AnonymousObject ao = (AnonymousObject) $p.val;

            $ctx.val = Constant.create(ao.getReference().getReferenceNumber());
        } else {
            $ctx.val = $p.val;
        }
    }
    | '(' e=expression ')' {
        $ctx.val = $e.val;
    }
    | '(-' e=expression ')' {
        $ctx.val = Subtract.create(Constant.create(0), $e.val);
    }
    ;

path returns [DefaultAccessExpression val]
    : f=FRESH_TOKEN {
        int refId = Integer.parseInt($f.text.replaceAll("^fresh_", ""));

        ElementInfo ei = ThreadInfo.getCurrentThread().getElementInfo(refId);
        Reference ref = new Reference(ei);

        Universe u = PredicateAbstraction.getInstance().getSymbolTable().getUniverse();
        StructuredValue v = u.get(ref);

        if (v instanceof UniverseArray) {
            UniverseArray a = (UniverseArray) v;

            $ctx.val = AnonymousArray.create(ref, Constant.create(a.getLength()));
        } else {
            $ctx.val = AnonymousObject.create(new Reference(ei));
        }
    }
    | f=RETURN_TOKEN {
        String r = $f.text.replaceAll("var_ssa_[0-9]+_frame_[0-9]+_", "");

        if (r.equals("return")) {
            $ctx.val = DefaultReturnValue.create();
        } else {
            $ctx.val = DefaultReturnValue.create(r);
        }
    }
    | f=CLASS_TOKEN {
        $ctx.val = DefaultPackageAndClass.create($f.text.substring("class_".length()).replaceAll("_", "."));
    }
    | f=ID_TOKEN {
        $ctx.val = DefaultRoot.create($f.text.replaceAll("var_ssa_[0-9]+_frame_[0-9]+_", ""));
    }
    | '(' SELECT_TOKEN '.' id=ID_TOKEN e=expression ')' {
        AccessExpression ae = (AccessExpression) PredicateContext.let.get($id.text);

        $ctx.val = Select.create(ae, $e.val);
    }
    | '(' SELECT_TOKEN id=ID_TOKEN '!' n=CONSTANT_TOKEN e=expression ')' {
        AccessExpression ae = (AccessExpression) PredicateContext.let.get($id.text + '!' + Integer.parseInt($n.text));

        $ctx.val = Select.create(ae, $e.val);
    }
    | '(' SELECT_TOKEN a=ARR_TOKEN id=ID_TOKEN '!' n=CONSTANT_TOKEN ')' {
        AccessExpression ae = (AccessExpression) PredicateContext.let.get($id.text + '!' + Integer.parseInt($n.text));

        $ctx.val = Select.create(ae);
    }
    | '(' SELECT_TOKEN a=ARR_TOKEN '.' id=ID_TOKEN ')' {
        AccessExpression ae = (AccessExpression) PredicateContext.let.get($id.text);

        $ctx.val = Select.create(ae);
    }
    | '(' SELECT_TOKEN a=ARR_TOKEN p=path ')' {
        $ctx.val = Select.create($p.val);
    }
    | '(' SELECT_TOKEN a=ARR_TOKEN qv=qvar ')' {
        AccessExpression ae = (AccessExpression) PredicateContext.let.get($qvar.val);

        $ctx.val = Select.create(ae);
    }
    | '(' SELECT_TOKEN p=path e=expression ')' {
        $ctx.val = Select.create($p.val, $e.val);
    }
    | '(' STORE_TOKEN p=path e1=expression e2=expression ')' {
        $ctx.val = Store.create($p.val, $e1.val, $e2.val);
    }
    | '(' STORE_TOKEN a=ARR_TOKEN p1=path p2=path ')' {
        $ctx.val = Store.create($p1.val, $p2.val);
    }
    | '(' STORE_TOKEN a=ARR_TOKEN p1=path '.' id=ID_TOKEN ')' {
        AccessExpression e = (DefaultAccessExpression) PredicateContext.let.get($id.text);

        $ctx.val = Store.create($p1.val, e);
    }
    | '(' STORE_TOKEN a=ARR_TOKEN p1=path id=ID_TOKEN '!' n=CONSTANT_TOKEN ')' {
        AccessExpression e = (DefaultAccessExpression) PredicateContext.let.get($id.text + '!' + Integer.parseInt($n.text));

        $ctx.val = Store.create($p1.val, e);
    }
    | '(' STORE_TOKEN f=ID_TOKEN p=path e=expression ')' {
        $ctx.val = DefaultObjectFieldWrite.create($p.val, $f.text.replaceAll("field_ssa_[0-9]+_", ""), $e.val);
    }
    ;

AND_TOKEN      : 'and';
ARR_TOKEN      : 'ssa_'[0-9]+'_arr';
ARRLEN_TOKEN   : 'arrlen';
CLASS_TOKEN    : 'class_'[a-zA-Z0-9_$]+;
DISTINCT_TOKEN : 'distinct';
EXISTS_TOKEN   : 'exists';
FALSE_TOKEN    : 'false';
//FIELD_TOKEN    : 'field_ssa_'[0-9]+'_'[a-zA-Z$_][a-zA-Z0-9$_]*; // FIELD_TOKEN could appear in equalities and we don't have the machinery for that
FORALL_TOKEN   : 'forall';
FRESH_TOKEN    : 'fresh_'[0-9]+;
INIT_TOKEN     : '<init>';
ITE_TOKEN      : 'ite';
LET_TOKEN      : 'let';
NOT_TOKEN      : 'not';
NULL_TOKEN     : 'null';
OR_TOKEN       : 'or';
REF_TOKEN      : 'ref';
RETURN_TOKEN   : 'var_ssa_'[0-9]+'_frame_'[0-9]+'_return'('_pc'[0-9]+)?;
SELECT_TOKEN   : 'select';
STORE_TOKEN    : 'store';
TRUE_TOKEN     : 'true';
TYPE_TOKEN     : 'Int';

CONSTANT_TOKEN
    : [-+]?'0'('.'[0-9]+)?
    | [-+]?[1-9][0-9]*('.'[0-9]+)?
    ;

ID_TOKEN
    : [a-zA-Z$_][a-zA-Z0-9$_]*
    ;

WS_TOKEN
    : ([ \t\n\r])+ -> channel(HIDDEN)
    ;
