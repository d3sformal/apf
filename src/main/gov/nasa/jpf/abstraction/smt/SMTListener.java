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
package gov.nasa.jpf.abstraction.smt;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.ListenerAdapter;

import gov.nasa.jpf.abstraction.TraceFormula;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.state.TruthValue;
import gov.nasa.jpf.abstraction.util.Pair;

/**
 * A special listener that JPF can instantiate as a common listener which also listens to SMT events
 */
public abstract class SMTListener extends ListenerAdapter {

    protected List<SMTCache> caches = new LinkedList<SMTCache>();

    public SMTListener() {
        SMT.registerListener(this);
    }

    public void registerCache(SMTCache cache) {
        caches.add(cache);
    }

    public void isSatisfiableInvoked(List<Predicate> formulas) {}
    public void interpolateInvoked(TraceFormula traceFormula) {}
    public void valuatePredicatesInvoked(Map<Predicate, PredicateValueDeterminingInfo> predicates) {}
    public void valuatePredicatesInvoked(Set<Predicate> predicates) {}
    public void getModelInvoked(Expression expression, List<Pair<Predicate, TruthValue>> determinants) {}
    public void getModelsInvoked(Predicate formula, AccessExpression[] expression) {}

    public void isSatisfiableInputGenerated(String input) {}
    public void interpolateInputGenerated(String input) {}
    public void valuatePredicatesInputGenerated(String input) {}
    public void getModelInputGenerated(String input) {}
    public void getModelsInputGenerated(String input) {}

    public void isSatisfiableExecuted(List<Predicate> formulas, boolean[] satisfiable) {}
    public void interpolateExecuted(Predicate[] interpolants) {}
    public void valuatePredicatesExecuted(Map<Predicate, TruthValue> valuation) {}
    public void getModelExecuted(Boolean satisfiability, Integer model) {}
    public void getModelsExecuted(AccessExpression[] exps, int[] models) {}

}
