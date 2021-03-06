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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Notation;
import gov.nasa.jpf.abstraction.common.PredicatesComponentVisitable;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultAccessExpression;
import gov.nasa.jpf.abstraction.common.impl.DefaultExpression;

/**
 * A common ancestor to all predicates used in the abstraction
 */
public abstract class Predicate implements PredicatesComponentVisitable, BranchingCondition, Cloneable {
    public abstract void addAccessExpressionsToSet(Set<AccessExpression> out);
    public abstract Predicate replace(Map<AccessExpression, Expression> replacements);

    public final Predicate replace(AccessExpression original, Expression replacement) {
        Map<AccessExpression, Expression> replacements = new HashMap<AccessExpression, Expression>();

        replacements.put(original, replacement);

        return replace(replacements);
    }

    private Integer hashCodeValue = null;
    private String stringValue = null;
    private BytecodeRange scope = BytecodeUnlimitedRange.getInstance();

    public void setScope(BytecodeRange scope) {
        this.scope = scope;
    }

    public boolean isInScope(int pc) {
        return scope instanceof BytecodeUnlimitedRange || scope.contains(pc);
    }

    public BytecodeRange getScope() {
        return scope;
    }

    public String toString() {
        return toString(Notation.policy);
    }
    public String toString(Notation policy) {
        return Notation.convertToString(this, policy);
    }

    /**
     * Changes all access expression present in the predicate to a form which reflects an assignment "expression := newExpression"
     *
     * Let p = (a = 3):
     *   update(p, a, b + 3) returns b + 3 = 3
     *
     * Let q = (aread(arr, a, 1) = 0):
     *   update(q, a[0], 3) returns aread(awrite(arr, a, 0, 3), 1) = 0
     *
     * Let r = (fread(f, o) = 10)
     *   update(r, s.f, x) returns fread(fwrite(f, s, x), o) = 10
     *
     * Used to determine weakest preconditions.
     *
     * @param expression an access expression being written to (e.g. local variable "a")
     * @param newExpression any arbitrary expression being written (e.g. "b + 3")
     * @return a predicate reflecting the updates
     */
    public abstract Predicate update(AccessExpression expression, Expression newExpression);

    private final String getStringRep() {
        if (stringValue == null) {
            stringValue = toString(Notation.DOT_NOTATION);
        }

        return stringValue;
    }

    @Override
    public final int hashCode() {
        if (hashCodeValue == null) {
            hashCodeValue = getStringRep().hashCode();
        }
        return hashCodeValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Predicate) {
            Predicate p = (Predicate) o;

            if (hashCode() != p.hashCode()) {
                return false;
            }

            return getStringRep().equals(p.getStringRep());
        }

        return false;
    }

    @Override
    public abstract Predicate clone();

}
