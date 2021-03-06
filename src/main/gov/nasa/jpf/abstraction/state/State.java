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
package gov.nasa.jpf.abstraction.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import gov.nasa.jpf.vm.MethodInfo;

import gov.nasa.jpf.abstraction.PredicateAbstraction;
import gov.nasa.jpf.abstraction.SSAFormulaIncarnationsManager;
import gov.nasa.jpf.abstraction.TraceFormula;
import gov.nasa.jpf.abstraction.common.access.Root;
import gov.nasa.jpf.abstraction.common.access.meta.Field;
import gov.nasa.jpf.abstraction.util.Pair;

/**
 * State of the predicate abstraction consists of
 * 1) history of scopes of symbol tables
 * 2) history of scopes of predicate valuations
 */
public class State {
    public int currentThread;
    public Map<Integer, SymbolTableStack> symbolTableStacks;
    public Map<Integer, PredicateValuationStack> predicateValuationStacks;
    public TraceFormula traceFormula;
    public SSAFormulaIncarnationsManager ssa;
    public Set<MethodInfo> visitedMethods;
    public TraceFormula forcedTrace;

    public State(
        int currentThread,
        Map<Integer, SymbolTableStack> symbolTableStacks,
        Map<Integer, PredicateValuationStack> predicateValuationStacks,
        TraceFormula traceFormula,
        SSAFormulaIncarnationsManager ssa,
        Set<MethodInfo> visitedMethods,
        TraceFormula forcedTrace
    ) {
        this.currentThread = currentThread;
        this.symbolTableStacks = symbolTableStacks;
        this.predicateValuationStacks = predicateValuationStacks;
        this.traceFormula = traceFormula;
        this.ssa = ssa;
        this.visitedMethods = visitedMethods;
        this.forcedTrace = forcedTrace;
    }

    @Override
    public State clone() {
      return new State(currentThread, symbolTableStacks, predicateValuationStacks, traceFormula, ssa, visitedMethods, forcedTrace);
    }
}
