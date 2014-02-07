package gov.nasa.jpf.abstraction.predicate.state.universe;

import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;

public class ClassName implements StructuredValueIdentifier {
    private StaticElementInfo staticElementInfo;
    private ThreadInfo threadInfo;

    public ClassName(StaticElementInfo staticElementInfo, ThreadInfo threadInfo) {
        this.staticElementInfo = staticElementInfo;
        this.threadInfo = threadInfo;
    }

    public String getClassName() {
        return staticElementInfo.getClassInfo().getName();
    }

    public StaticElementInfo getStaticElementInfo() {
        return staticElementInfo;
    }

    public ThreadInfo getThreadInfo() {
        return threadInfo;
    }

    @Override
    public int hashCode() {
        return getClassName().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ClassName) {
            return getClassName().equals(((ClassName) object).getClassName());
        }

        return false;
    }

    @Override
    public String toString() {
        return getClassName();
    }

    @Override
    public int compareTo(Identifier id) {
        if (id instanceof ClassName) {
            ClassName cln = (ClassName) id;

            return getStaticElementInfo().getClassInfo().getName().compareTo(cln.getStaticElementInfo().getClassInfo().getName());
        }

        return Identifier.Ordering.compare(this, id);
    }
}
