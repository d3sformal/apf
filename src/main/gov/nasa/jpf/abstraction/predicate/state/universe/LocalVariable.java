package gov.nasa.jpf.abstraction.predicate.state.universe;

import gov.nasa.jpf.abstraction.common.access.Root;
import gov.nasa.jpf.abstraction.predicate.state.MethodFrameSymbolTable;

public interface LocalVariable extends Identifier, UniverseSlot {
    public class SlotKey implements UniverseSlotKey {
        @Override
        public String toString() {
            return "";
        }
    }

    @Override
    public LocalVariable createShallowCopy();

    public Root getAccessExpression();
    public int getScope();
}
