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
package gov.nasa.jpf.abstraction.assertions;

import java.util.HashSet;
import java.util.Set;

import gov.nasa.jpf.vm.VM;

import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.state.MethodFrameSymbolTable;
import gov.nasa.jpf.abstraction.state.universe.UniverseIdentifier;

public class AssertMayBeAliasedHandler extends AssertAliasingHandler {
    @Override
    public void checkAliasing(VM vm, int lineNumber, AccessExpression[] exprs, MethodFrameSymbolTable symbolTable) {
        if (exprs.length > 0) {
            Set<UniverseIdentifier> commonValues = new HashSet<UniverseIdentifier>();
            Set<UniverseIdentifier> toBeRemoved = new HashSet<UniverseIdentifier>();

            symbolTable.lookupValues(exprs[0], commonValues);

            for (int i = 1; i < exprs.length; ++i) {
                Set<UniverseIdentifier> otherValues = new HashSet<UniverseIdentifier>();

                symbolTable.lookupValues(exprs[i], otherValues);

                for (UniverseIdentifier id : commonValues) {
                    if (!otherValues.contains(id)) {
                        toBeRemoved.add(id);
                    }
                }

                commonValues.removeAll(toBeRemoved);
                toBeRemoved.clear();
            }

            if (commonValues.isEmpty()) {
                reportError(vm, lineNumber, "There exists an access expressions that cannot be aliased with the others.");
            }
        }
    }
}
