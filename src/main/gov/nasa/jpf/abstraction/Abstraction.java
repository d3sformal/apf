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
package gov.nasa.jpf.abstraction;

import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

import gov.nasa.jpf.abstraction.common.BranchingCondition;
import gov.nasa.jpf.abstraction.common.BranchingConditionInfo;
import gov.nasa.jpf.abstraction.common.BranchingDecision;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.Root;
import gov.nasa.jpf.abstraction.concrete.AnonymousObject;
import gov.nasa.jpf.abstraction.state.TruthValue;

/**
 * Common root class for numeric abstractions.
 */
public abstract class Abstraction {
    /**
     * Informs the abstraction about the start of a search (called once at the beginning)
     */
    public void start(ThreadInfo thread) {
    }

    /**
     * Informs the abstraction about an advancement
     */
    public void forward(MethodInfo method) {
    }

    /**
     * Informs the abstraction about a backtack
     * @param method Restored method
     */
    public void backtrack(MethodInfo method) {
    }

    /**
     * Informs the abstraction about a symbolic assignment
     * @param to An access expression referring to a primitive value
     */
    public void processPrimitiveStore(MethodInfo lastM, int lastPC, MethodInfo nextM, int nextPC, Expression from, AccessExpression to) {
    }

    /**
     * Informs the abstraction about a symbolic assignment
     * @param to An access expression referring to an object on heap
     */
    public void processObjectStore(MethodInfo lastM, int lastPC, MethodInfo nextM, int nextPC, Expression from, AccessExpression to) {
    }

    /**
     * Informs the abstraction about a symbolic assignment
     * @param to An access expression referring to an object on heap
     */
    public void processObjectStore(MethodInfo lastM, int lastPC, MethodInfo nextM, int nextPC, Expression from, AccessExpression to, AccessExpression exactTo) {
    }

    /**
     * Called by all InvokeInstructions to inform about a successful method invocations
     * @param before Caller stack
     * @param after  Callee stack
     */
    public void processMethodCall(ThreadInfo threadInfo, StackFrame before, StackFrame after) {
    }

    /**
     * Called by all ReturnInstructions to inform about a successful return from a method
     * @param before Callee stack
     * @param after  Caller stack
     */
    public void processMethodReturn(ThreadInfo threadInfo, StackFrame before, StackFrame after) {
    }

    /**
     * Called by all ReturnInstructions to inform about a successful return from a method
     * No return value
     * @param before Callee stack
     * @param after  Caller stack
     */
    public void processVoidMethodReturn(ThreadInfo threadInfo, StackFrame before, StackFrame after) {
    }

    /**
     * Most abstractions do not provide this behaviour, but branching instructions need this method to be present.
     */
    public BranchingConditionInfo processBranchingCondition(int lastPC, BranchingCondition condition) {
        return BranchingConditionInfo.NONE;
    }

    /**
     * Notify about a class that has not been CLINITed
     *
     * this is an alternative approach to let the abstraction know about its existence
     */
    public void processNewClass(ThreadInfo thread, ClassInfo classInfo) {
    }

    public void processObject(AnonymousObject object, MethodInfo m, int pc) {
    }

    public void processNewObject(AnonymousObject object, MethodInfo m, int pc) {
    }

    public void informAboutPrimitiveLocalVariable(Root root) {
    }

    public void informAboutStructuredLocalVariable(Root root) {
    }

    public void informAboutBranchingDecision(BranchingDecision decision, MethodInfo m, int lastPC) {
    }

    public void addThread(ThreadInfo threadInfo) {
    }

    public void scheduleThread(ThreadInfo threadInfo) {
    }
}
