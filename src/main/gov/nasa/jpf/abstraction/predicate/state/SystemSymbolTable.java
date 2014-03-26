package gov.nasa.jpf.abstraction.predicate.state;

import gov.nasa.jpf.abstraction.Attribute;
import gov.nasa.jpf.abstraction.impl.NonEmptyAttribute;
import gov.nasa.jpf.abstraction.common.access.Root;
import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.ReturnValue;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultReturnValue;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultRoot;
import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.concrete.AnonymousExpression;
import gov.nasa.jpf.abstraction.concrete.AnonymousArray;
import gov.nasa.jpf.abstraction.predicate.PredicateAbstraction;
import gov.nasa.jpf.abstraction.predicate.state.SystemSymbolTable;
import gov.nasa.jpf.abstraction.predicate.state.MethodFrameSymbolTable;
import gov.nasa.jpf.abstraction.predicate.state.universe.UniverseIdentifier;
import gov.nasa.jpf.abstraction.predicate.state.universe.StructuredValueIdentifier;
import gov.nasa.jpf.abstraction.predicate.state.universe.Universe;
import gov.nasa.jpf.abstraction.predicate.state.universe.Reference;
import gov.nasa.jpf.abstraction.predicate.state.universe.ClassName;
import gov.nasa.jpf.abstraction.predicate.state.universe.LocalVariable;
import gov.nasa.jpf.abstraction.util.RunDetector;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.KernelState;
import gov.nasa.jpf.vm.ClassLoaderInfo;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadList;
import gov.nasa.jpf.vm.Instruction;

import gov.nasa.jpf.jvm.bytecode.DLOAD;
import gov.nasa.jpf.jvm.bytecode.FLOAD;
import gov.nasa.jpf.jvm.bytecode.ILOAD;
import gov.nasa.jpf.jvm.bytecode.LLOAD;
import gov.nasa.jpf.jvm.bytecode.DSTORE;
import gov.nasa.jpf.jvm.bytecode.FSTORE;
import gov.nasa.jpf.jvm.bytecode.ISTORE;
import gov.nasa.jpf.jvm.bytecode.LSTORE;
import gov.nasa.jpf.jvm.bytecode.LocalVariableInstruction;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * Symbol table aware of method call scope changes
 */
public class SystemSymbolTable extends CallAnalyzer implements SymbolTable, Scoped {
    private Universe universe = new Universe();

	/**
	 * Stacks of scopes (pushed by invoke, poped by return) separately for all threads
	 */
	private Map<Integer, SymbolTableStack> scopes = new HashMap<Integer, SymbolTableStack>();

	private PredicateAbstraction abstraction;
    private Integer currentThreadID = 0;
	
	public SystemSymbolTable(PredicateAbstraction abstraction) {
		this.abstraction = abstraction;
	}

	/**
	 * Create a scope for a given method
	 */
	@Override
	public MethodFrameSymbolTable createDefaultScope(ThreadInfo threadInfo, MethodInfo method) {
		MethodFrameSymbolTable ret = new MethodFrameSymbolTable(scopes.get(currentThreadID).top());

		/**
		 * Register new local variables
		 */
        StackFrame sf = threadInfo.getTopFrame();

		/**
		 * Handle main(String[] args)
         *
         * it is necessary to initialize the arguments
		 */
		VM vm = threadInfo.getVM();
		String target = vm.getConfig().getTarget();
		
		if (method.getFullName().equals(target + ".main([Ljava/lang/String;)V")) {
			ElementInfo ei = threadInfo.getElementInfo(sf.getLocalVariable(0));
			int length = ei.arrayLength();
			
			LocalVarInfo args = method.getArgumentLocalVars()[0];
            AnonymousArray argsExpr = AnonymousArray.create(new Reference(ei), Constant.create(length));
            Attribute attr = new NonEmptyAttribute(null, argsExpr);

            sf.setLocalAttr(0, attr);
            method.addAttr(attr);

            ret.addObject(argsExpr);
			ret.processObjectStore(argsExpr, DefaultRoot.create(args.getName(), 0));
		}
		
		return ret;
	}
	
	@Override
	public Set<AccessExpression> processPrimitiveStore(Expression from, AccessExpression to) {
		return scopes.get(currentThreadID).top().processPrimitiveStore(from, to);
	}
	
	@Override
	public Set<AccessExpression> processObjectStore(Expression from, AccessExpression to) {
		return scopes.get(currentThreadID).top().processObjectStore(from, to);
	}
	
    /**
     * At a method call it is necessary to change the scope correspondingly
     */
    @Override
    public void processMethodCall(ThreadInfo threadInfo, StackFrame before, StackFrame after) {
        MethodInfo method = after.getMethodInfo();

        MethodFrameSymbolTable originalScope = scopes.get(currentThreadID).top();
        MethodFrameSymbolTable newScope = createDefaultScope(threadInfo, method);

        scopes.get(currentThreadID).push(method.getFullName(), newScope);

        if (!method.isNative() && !method.isMJI()) {
            /**
             * Assign values to the formal parameters according to the actual parameters
             */
            boolean[] slotInUse = new boolean[method.getNumberOfStackArguments()];
            boolean[] localVarIsPrimitive = new boolean[method.getNumberOfStackArguments()];

            getArgumentSlotUsage(method, slotInUse);
            getArgumentSlotType(method, localVarIsPrimitive);

            for (int slotIndex = 0; slotIndex < method.getNumberOfStackArguments(); ++slotIndex) {
                if (slotInUse[slotIndex]) {
                    Attribute attr = Attribute.ensureNotNull((Attribute) after.getLocalAttr(slotIndex));

                    LocalVarInfo arg = after.getLocalVarInfo(slotIndex);
                    String name = arg == null ? null : arg.getName();

                    if (localVarIsPrimitive[slotIndex]) {
                        newScope.addPrimitiveLocalVariable(DefaultRoot.create(name, slotIndex));
                        newScope.processPrimitiveStore(attr.getExpression(), originalScope, DefaultRoot.create(name, slotIndex));
                    } else {
                        newScope.addStructuredLocalVariable(DefaultRoot.create(name, slotIndex));
                        newScope.processObjectStore(attr.getExpression(), originalScope, DefaultRoot.create(name, slotIndex));
                    }
                }
            }
        }
    }
	
	@Override
	public void processMethodReturn(ThreadInfo threadInfo, StackFrame before, StackFrame after) {
		ReturnValue calleeReturnValue = DefaultReturnValue.create();
		ReturnValue callerReturnValue = DefaultReturnValue.create(threadInfo.getPC(), before.getMethodInfo().isReferenceReturnType());

		Attribute attr = Attribute.ensureNotNull((Attribute) after.getOperandAttr());
		Expression returnExpression = attr.getExpression();
		//after.setOperandAttr(new NonEmptyAttribute(null, callerReturnValue)); // This is performed by the predicate valuation after it uses the original expression

		/**
		 * Register the return value
         *
         * top()  ... before return
         * top(1) ... after return (previous stack frame)
         *
         * 1) create a new return container in callee
         * 2) write the actual return value into the container (still in callee)
         * 3) create a specific unique return container in the caller
         * 4) write callee return into caller return
		 */
		if (before.getMethodInfo().isReferenceReturnType()) {
			scopes.get(currentThreadID).top().addStructuredReturn(calleeReturnValue);
			scopes.get(currentThreadID).top().processObjectStore(returnExpression, calleeReturnValue);
			scopes.get(currentThreadID).top(1).addStructuredReturn(callerReturnValue);
			scopes.get(currentThreadID).top(1).processObjectStore(calleeReturnValue, scopes.get(currentThreadID).top(), callerReturnValue);
		} else {
			scopes.get(currentThreadID).top().addPrimitiveReturn(calleeReturnValue);
			scopes.get(currentThreadID).top().processPrimitiveStore(returnExpression, calleeReturnValue);
			scopes.get(currentThreadID).top(1).addPrimitiveReturn(callerReturnValue);
			scopes.get(currentThreadID).top(1).processPrimitiveStore(calleeReturnValue, scopes.get(currentThreadID).top(), callerReturnValue);
		}

        /**
         * Handle the rest as if there were no return values
         */
		processVoidMethodReturn(threadInfo, before, after);
	}
	
	@Override
	public void processVoidMethodReturn(ThreadInfo threadInfo, StackFrame before, StackFrame after) {
        /**
         * Drop callee scope
         */
		scopes.get(currentThreadID).pop();
	}
	
	@Override
	public void restore(Map<Integer, ? extends Scopes> scopes) {
        this.scopes.clear();

        if (!scopes.isEmpty()) {
            SymbolTableStack stack = (SymbolTableStack) scopes.get(scopes.keySet().iterator().next());

            if (stack.count() > 0) {
                this.universe = stack.top().getUniverse().clone();
            }
        }

        for (Integer threadId : scopes.keySet()) {
            Scopes threadScopes = scopes.get(threadId);

		    if (threadScopes instanceof SymbolTableStack) {
                SymbolTableStack threadSymbolTableScopes = (SymbolTableStack) threadScopes;

                this.scopes.put(threadId, threadSymbolTableScopes.clone());

                for (MethodFrameSymbolTable scope : this.scopes.get(threadId)) {
                    scope.setUniverse(this.universe);
                }
		    } else {
        		throw new RuntimeException("Invalid scopes type being restored!");
		    }
        }
	}
	
	@Override
	public Map<Integer, SymbolTableStack> memorize() {
        Map<Integer, SymbolTableStack> scopesClone = new HashMap<Integer, SymbolTableStack>();

        Universe universeClone = universe.clone();

        for (Integer threadId : scopes.keySet()) {
            scopesClone.put(threadId, scopes.get(threadId).clone());

            for (MethodFrameSymbolTable scope : scopesClone.get(threadId)) {
                scope.setUniverse(universeClone);
            }
        }

		return scopesClone;
	}
	
	@Override
	public String toString() {
		return scopes.get(currentThreadID).count() > 0 ? scopes.get(currentThreadID).top().toString() : "";
	}

	@Override
	public boolean isArray(AccessExpression path) {
		return scopes.get(currentThreadID).top().isArray(path);
	}

	@Override
	public boolean isObject(AccessExpression path) {
		return scopes.get(currentThreadID).top().isObject(path);
	}

	@Override
	public boolean isPrimitive(AccessExpression path) {
		return scopes.get(currentThreadID).top().isPrimitive(path);
	}

	@Override
	public Universe getUniverse() {
		return universe;
	}

	@Override
	public int count() {
		return scopes.get(currentThreadID).count() > 0 ? scopes.get(currentThreadID).top().count() : 0;
	}

	@Override
	public int depth() {
        return depth(currentThreadID);
	}

	public int depth(int threadID) {
        return scopes.get(threadID).count();
	}

    @Override
    public MethodFrameSymbolTable get(int depth) {
        return get(currentThreadID, depth);
    }

    public MethodFrameSymbolTable get(int threadID, int depth) {
        return scopes.get(threadID).top(depth);
    }

    @Override
    public void addThread(ThreadInfo threadInfo) {
        universe.add(threadInfo.getThreadObject(), threadInfo);

		SymbolTableStack threadStack = new SymbolTableStack();
        threadStack.push("-- Dummy stop scope --", new MethodFrameSymbolTable(universe, abstraction));

        scopes.put(threadInfo.getId(), threadStack);
    }

    @Override
    public void scheduleThread(ThreadInfo threadInfo) {
        scheduleThread(threadInfo.getId());
    }

    @Override
    public void scheduleThread(int threadID) {
        currentThreadID = threadID;
    }

    @Override
    public void print() {
        scopes.get(currentThreadID).print();
    }
	
    public void collectGarbage(VM vm, ThreadInfo ti) {
        KernelState ks = vm.getKernelState();

        ThreadList threads = ks.getThreadList();

        // Add roots
        Set<UniverseIdentifier> liveRoots = new HashSet<UniverseIdentifier>();

        // All classes
        for (ClassLoaderInfo cl : ks.classLoaders) {
            if(cl.isAlive()) {
                for (StaticElementInfo sei : cl.getStatics().liveStatics()) {
                    liveRoots.add(new ClassName(sei));
                    liveRoots.add(new Reference(ti.getElementInfo(sei.getClassObjectRef())));
                }
            }
        }

        for (StructuredValueIdentifier candidate : universe.getStructuredValues()) {
            if (candidate instanceof ClassName) {
                liveRoots.add(candidate);
            }
        }

        // All thread objects, lock objects, local variables
        for (ThreadInfo thread : threads) {
            liveRoots.add(new Reference(thread.getThreadObject()));
            liveRoots.add(new Reference(thread.getLockObject()));

            for (ElementInfo lock : thread.getLockedObjects()) {
                liveRoots.add(new Reference(lock));
            }

            if (thread.isAlive()) {
                for (int depth = 0; depth < depth(thread.getId()); ++depth) {
                    MethodFrameSymbolTable currentSymbolScope = get(thread.getId(), depth);

                    for (Root varName : currentSymbolScope.getLocalVariables()) {
                        LocalVariable var = currentSymbolScope.getLocal(varName);

                        liveRoots.addAll(var.getPossibleValues());
                    }
                }

                // Anonymous objects on stacks are also live
                for (StackFrame frame = thread.getTopFrame(); frame != null; frame = frame.getPrevious()) {
                    for (int i = 0; i < frame.getTopPos() - frame.getLocalVariableCount(); ++i) {
                        Attribute attr = Attribute.ensureNotNull((Attribute) frame.getOperandAttr(i));
                        Expression expr = attr.getExpression();

                        if (expr instanceof AnonymousExpression) {
                            AnonymousExpression ae = (AnonymousExpression) expr;

                            liveRoots.add(ae.getReference());
                        }
                    }
                }
            }
        }

        universe.retainLiveValuesOnly(liveRoots);
    }
}
