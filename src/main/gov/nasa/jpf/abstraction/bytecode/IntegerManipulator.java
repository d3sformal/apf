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
package gov.nasa.jpf.abstraction.bytecode;

import gov.nasa.jpf.vm.StackFrame;

import gov.nasa.jpf.abstraction.common.Expression;

public class IntegerManipulator implements DataWordManipulator<Integer> {
    @Override
    public Expression getExpression(StackFrame sf) {
        return (Expression) sf.getOperandAttr();
    }

    @Override
    public Integer pop(StackFrame sf) {
        return sf.pop();
    }

    @Override
    public void push(StackFrame sf, Integer i) {
        sf.push(i);
    }

    @Override
    public void setExpression(StackFrame sf, Expression attribute) {
        sf.setOperandAttr(attribute);
    }
}
