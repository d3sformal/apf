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

import java.util.Map;
import java.util.Set;

import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.abstraction.common.PredicatesComponentVisitor;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.ArrayElementWrite;
import gov.nasa.jpf.abstraction.common.access.meta.Arrays;
import gov.nasa.jpf.abstraction.common.access.meta.impl.DefaultArrays;

/**
 * Write to an array element awrite(arr, a, i, e) ~ a[i] := e
 */
public class DefaultArrayElementWrite extends DefaultArrayElementExpression implements ArrayElementWrite {

    private Expression newValue;
    private Integer hashCodeValue;

    protected DefaultArrayElementWrite(AccessExpression array, Expression index, Expression newValue) {
        this(array, DefaultArrays.create(), index, newValue);
    }

    protected DefaultArrayElementWrite(AccessExpression array, Arrays arrays, Expression index, Expression newValue) {
        super(array, arrays, index);

        this.newValue = newValue;
    }

    @Override
    public Expression getNewValue() {
        return newValue;
    }

    public static DefaultArrayElementWrite create(AccessExpression array, Expression index, Expression newValue) {
        if (array == null || index == null || newValue == null) {
            return null;
        }

        return new DefaultArrayElementWrite(array, index, newValue);
    }

    public static DefaultArrayElementWrite create(AccessExpression array, Arrays arrays, Expression index, Expression newValue) {
        if (array == null || arrays == null || index == null || newValue == null) {
            return null;
        }

        return new DefaultArrayElementWrite(array, arrays, index, newValue);
    }

    @Override
    public void addAccessSubExpressionsToSet(Set<AccessExpression> out) {
        super.addAccessSubExpressionsToSet(out);

        newValue.addAccessExpressionsToSet(out);
    }

    @Override
    public void accept(PredicatesComponentVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public DefaultArrayElementWrite createShallowCopy() {
        return create(getArray(), getArrays(), getIndex(), getNewValue());
    }

    @Override
    public AccessExpression reRoot(AccessExpression newPrefix) {
        return create(newPrefix, getArrays(), getIndex(), getNewValue());
    }

    @Override
    public boolean isEqualToSlow(AccessExpression o) {
        if (o instanceof ArrayElementWrite) {
            ArrayElementWrite w = (ArrayElementWrite) o;

            return getArrays().equals(w.getArrays()) && getArray().isEqualToSlow(w.getArray()) && getIndex().equals(w.getIndex()) && getNewValue().equals(w.getNewValue());
        }

        return false;
    }

    @Override
    public boolean isSimilarToSlow(AccessExpression expression) {
        if (expression instanceof ArrayElementWrite) {
            ArrayElementWrite w = (ArrayElementWrite) expression;

            return getArray().isSimilarToSlow(w.getArray());
        }

        return false;
    }

    @Override
    public int hashCode() {
        if (hashCodeValue == null) {
            hashCodeValue = ("write_element_" + getArray().hashCode() + "_" + getIndex().hashCode() + "_" + getNewValue().hashCode()).hashCode();
        }

        return hashCodeValue;
    }

    @Override
    public AccessExpression replaceSubExpressions(Map<AccessExpression, Expression> replacements) {
        AccessExpression newA = getObject().replaceSubExpressions(replacements);
        Expression newI = getIndex().replace(replacements);
        Expression newNV = getNewValue().replace(replacements);

        if (newA == getObject() && newI == getIndex() && newNV == getNewValue()) return this;
        else return create(newA, getArrays(), newI, newNV);
    }

    @Override
    public Predicate getPreconditionForBeingFresh() {
        return getNewValue().getPreconditionForBeingFresh();
    }

}
