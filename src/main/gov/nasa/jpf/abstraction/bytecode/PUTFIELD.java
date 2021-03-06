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

import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;

import gov.nasa.jpf.abstraction.PredicateAbstraction;
import gov.nasa.jpf.abstraction.common.Equals;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.ExpressionUtil;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultObjectFieldRead;
import gov.nasa.jpf.abstraction.common.impl.NullExpression;

public class PUTFIELD extends gov.nasa.jpf.jvm.bytecode.PUTFIELD {

    public PUTFIELD(String fieldName, String classType, String fieldDescriptor) {
        super(fieldName, classType, fieldDescriptor);
    }

    @Override
    public Instruction execute(ThreadInfo ti) {
        StackFrame sf = ti.getTopFrame();
        Expression from = null;
        AccessExpression to = null;
        ElementInfo ei = null;

        FieldInfo fi = getFieldInfo();

        switch (fi.getTypeCode()) {
            case Types.T_ARRAY:
            case Types.T_REFERENCE:
            case Types.T_BOOLEAN:
            case Types.T_BYTE:
            case Types.T_CHAR:
            case Types.T_SHORT:
            case Types.T_INT:
            case Types.T_FLOAT:
                from = ExpressionUtil.getExpression(sf.getOperandAttr());
                to = ExpressionUtil.getAccessExpression(sf.getOperandAttr(1));
                ei = ti.getModifiableElementInfo(sf.peek(1));
                break;

            case Types.T_LONG:
            case Types.T_DOUBLE:
                from = ExpressionUtil.getExpression(sf.getLongOperandAttr());
                to = ExpressionUtil.getAccessExpression(sf.getOperandAttr(2));
                ei = ti.getModifiableElementInfo(sf.peek(2));
                break;

            default:
        }

        if (ei == null) {
            PredicateAbstraction.getInstance().extendTraceFormulaWithConstraint(Equals.create(to, NullExpression.create()), sf.getMethodInfo(), getPosition());
        }

        AccessExpression field = DefaultObjectFieldRead.create(to, getFieldName());

        ei.setFieldAttr(fi, from);

        Instruction expectedNextInsn = JPFInstructionAdaptor.getStandardNextInstruction(this, ti);

        Instruction actualNextInsn = super.execute(ti);

        if (JPFInstructionAdaptor.testFieldInstructionAbort(this, ti, expectedNextInsn, actualNextInsn)) {
            return actualNextInsn;
        }

        if (ei.getFieldValueObject(getFieldName()) == null || ei.getFieldValueObject(getFieldName()) instanceof ElementInfo) {
            PredicateAbstraction.getInstance().processObjectStore(getMethodInfo(), getPosition(), actualNextInsn.getMethodInfo(), actualNextInsn.getPosition(), from, field);
        } else {
            PredicateAbstraction.getInstance().processPrimitiveStore(getMethodInfo(), getPosition(), actualNextInsn.getMethodInfo(), actualNextInsn.getPosition(), from, field);
        }

        AnonymousExpressionTracker.notifyPopped(from);
        AnonymousExpressionTracker.notifyPopped(to);

        return actualNextInsn;
    }
}
