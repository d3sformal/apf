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
package gov.nasa.jpf.abstraction.state.universe;

import java.util.Set;

public interface UniverseSlot extends Freezable {
    @Override
    public UniverseSlot createShallowCopy();

    public Identifier getParent();
    public void setParent(Identifier identifier);
    public UniverseSlotKey getSlotKey();
    public void setSlotKey(UniverseSlotKey slotKey);
    public Set<? extends UniverseIdentifier> getPossibleValues();
    public void clear();
}
