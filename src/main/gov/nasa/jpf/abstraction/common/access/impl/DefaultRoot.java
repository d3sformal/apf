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
package gov.nasa.jpf.abstraction.common.access.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.vm.LocalVarInfo;

import gov.nasa.jpf.abstraction.common.Contradiction;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.common.PredicatesComponentVisitor;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.Root;

/**
 * A common ancestor of all symbolic expressions that can stand alone (variables, package-class expression)
 *
 * this contrasts with expressions such as object field read (@see gov.nasa.jpf.abstraction.common.access.ObjectFieldRead) that are not atomic enough in this sense
 */
public class DefaultRoot extends DefaultAccessExpression implements Root {
    private static Map<String, DefaultRoot> instances = new HashMap<String, DefaultRoot>();

    private String name;
    private Integer hashCodeValue;

    protected DefaultRoot(String name) {
        super(1);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public final boolean isPrefixOf(AccessExpression expression) {
        return expression.getRoot().equals(this);
    }

    @Override
    public final boolean isSimilarToPrefixOf(AccessExpression expression) {
        return expression.getRoot().equals(this);
    }

    @Override
    public final AccessExpression cutTail() {
        return this;
    }

    @Override
    public Root getRoot() {
        return this;
    }

    @Override
    public AccessExpression get(int depth) {
        if (depth == 1) {
            return this;
        }

        return null;
    }

    public static DefaultRoot create(String name, int index) {
        if (name == null || name.equals("?")) {
            return create("local_" + index);
        }

        return create(name);
    }

    public static DefaultRoot create(String name) {
        if (name == null) {
            return null;
        }

        if (!instances.containsKey(name)) {
            instances.put(name, new DefaultRoot(name));
        }

        return instances.get(name);
    }

    @Override
    public void accept(PredicatesComponentVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void addAccessSubExpressionsToSet(Set<AccessExpression> out) {
    }

    @Override
    public DefaultRoot createShallowCopy() {
        return this;
    }

    @Override
    public AccessExpression reRoot(AccessExpression newPrefix) {
        return newPrefix;
    }

    @Override
    public Expression update(AccessExpression expression, Expression newExpression) {
        if (equals(expression)) {
            return newExpression;
        }

        return this;
    }

    @Override
    public boolean isEqualToSlow(AccessExpression o) {
        if (o instanceof Root) {
            Root r = (Root) o;

            return getName().equals(r.getName());
        }

        return false;
    }

    @Override
    public boolean isSimilarToSlow(AccessExpression expression) {
        return isEqualToSlow(expression);
    }

    @Override
    public int hashCode() {
        if (hashCodeValue == null) {
            hashCodeValue = ("root_" + getName()).hashCode();
        }

        return hashCodeValue;
    }

    @Override
    public AccessExpression replaceSubExpressions(Map<AccessExpression, Expression> replacements) {
        return this;
    }

    @Override
    public Predicate getPreconditionForBeingFresh() {
        return Contradiction.create();
    }

}
