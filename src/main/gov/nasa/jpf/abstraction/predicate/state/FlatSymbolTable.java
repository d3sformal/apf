package gov.nasa.jpf.abstraction.predicate.state;

import gov.nasa.jpf.abstraction.common.access.AccessExpression;
import gov.nasa.jpf.abstraction.common.access.ArrayElementRead;
import gov.nasa.jpf.abstraction.common.access.ObjectFieldRead;
import gov.nasa.jpf.abstraction.common.access.PackageAndClass;
import gov.nasa.jpf.abstraction.common.access.ReturnValue;
import gov.nasa.jpf.abstraction.common.access.Root;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultArrayElementRead;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultObjectFieldRead;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultPackageAndClass;
import gov.nasa.jpf.abstraction.common.access.impl.DefaultRoot;
import gov.nasa.jpf.abstraction.common.Constant;
import gov.nasa.jpf.abstraction.common.Expression;
import gov.nasa.jpf.abstraction.common.Notation;
import gov.nasa.jpf.abstraction.concrete.AnonymousArray;
import gov.nasa.jpf.abstraction.concrete.AnonymousExpression;
import gov.nasa.jpf.abstraction.concrete.AnonymousObject;
import gov.nasa.jpf.abstraction.predicate.PredicateAbstraction;
import gov.nasa.jpf.abstraction.predicate.state.universe.Universe;
import gov.nasa.jpf.abstraction.predicate.state.universe.UniverseObject;
import gov.nasa.jpf.abstraction.predicate.state.universe.UniverseArray;
import gov.nasa.jpf.abstraction.predicate.state.universe.UniverseValue;
import gov.nasa.jpf.abstraction.predicate.state.universe.StructuredValue;
import gov.nasa.jpf.abstraction.predicate.state.universe.PrimitiveValue;
import gov.nasa.jpf.abstraction.predicate.state.universe.LocalVariable;
import gov.nasa.jpf.abstraction.predicate.state.universe.LoadedClass;
import gov.nasa.jpf.abstraction.predicate.state.universe.UniverseIdentifier;
import gov.nasa.jpf.abstraction.predicate.state.universe.PrimitiveValueIdentifier;
import gov.nasa.jpf.abstraction.predicate.state.universe.StructuredValueIdentifier;
import gov.nasa.jpf.abstraction.predicate.state.universe.Associative;
import gov.nasa.jpf.abstraction.predicate.state.universe.Indexed;
import gov.nasa.jpf.abstraction.predicate.state.universe.FieldName;
import gov.nasa.jpf.abstraction.predicate.state.universe.ElementIndex;
import gov.nasa.jpf.abstraction.predicate.state.universe.PrimitiveLocalVariable;
import gov.nasa.jpf.abstraction.predicate.state.universe.StructuredLocalVariable;
import gov.nasa.jpf.abstraction.predicate.state.universe.Reference;
import gov.nasa.jpf.abstraction.predicate.state.universe.Identifier;
import gov.nasa.jpf.abstraction.predicate.state.universe.UniverseSlotKey;
import gov.nasa.jpf.abstraction.predicate.state.universe.UniverseSlot;
import gov.nasa.jpf.abstraction.predicate.state.universe.PrimitiveValueSlot;
import gov.nasa.jpf.abstraction.predicate.state.universe.StructuredValueSlot;
import gov.nasa.jpf.abstraction.common.Predicate;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.StaticElementInfo;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A symbol table for a single scope
 */
public class FlatSymbolTable implements SymbolTable, Scope {
	
	private static String[] doNotMonitor = new String[] {
	};
	private static String[] doNotPrint = new String[] {
		"java",
		"boolean",
		"byte",
		"char",
		"double",
		"float",
		"int",
		"long",
		"short",
		"void",
		"[", // Statics of arrays
		"javax",
		"sun",
		"gov.nasa.jpf" // JPF
	};
	private static int GUARANTEED_LENGTH = 8;
	
	/**
	 * Abstract heap
	 */
	private Universe universe = new Universe();
	
	/**
	 * Entry points to the abstract heap
	 * 
	 * Local Variables:
	 */
	private Map<Root, LocalVariable> locals = new HashMap<Root, LocalVariable>();
	
	/**
	 * Return values (special "variables" with no backup in the program itself):
	 */
	private Map<ReturnValue, LocalVariable> returns = new HashMap<ReturnValue, LocalVariable>();
	
	/**
	 * Classes (holders for static fields)
	 */
	private Map<PackageAndClass, LoadedClass> classes = new HashMap<PackageAndClass, LoadedClass>();

	private PredicateAbstraction abstraction;
	
	public FlatSymbolTable(PredicateAbstraction abstraction) {
		this.abstraction = abstraction;
	}

    public FlatSymbolTable(FlatSymbolTable previous) {
        this.abstraction = previous.abstraction;
        this.universe = previous.universe;
        this.classes = previous.classes;
    }
	
	public Set<Root> getLocalVariables() {
		return locals.keySet();
	}
	
	public Set<ReturnValue> getReturnValues() {
		return returns.keySet();
	}

	public Set<PackageAndClass> getClasses() {
		return classes.keySet();
	}

	public LocalVariable getLocal(Root l) {
		return locals.get(l);
	}
	
	public LocalVariable getReturnValue(ReturnValue r) {
		return returns.get(r);
	}

	public LoadedClass getClass(Root c) {
		return classes.get(c);
	}
	
	@Override
	public Universe getUniverse() {
		return universe;
	}

    private void createPrimitiveLocalVariable(PrimitiveLocalVariable v) {
        PrimitiveValue value = universe.get(universe.add());

        v.addPossiblePrimitiveValue(value.getIdentifier());
        value.addParentSlot(v, PrimitiveLocalVariable.slotKey);
    }

    private void createStructuredLocalVariable(StructuredLocalVariable v) {
        StructuredValue value = universe.get(Universe.nullReference);

        v.addPossibleStructuredValue(value.getIdentifier());
        value.addParentSlot(v, StructuredLocalVariable.slotKey);
    }
	
	public void addPrimitiveLocalVariable(Root l) {
		PrimitiveLocalVariable v = new PrimitiveLocalVariable(l, this);

        createPrimitiveLocalVariable(v);

		locals.put(l, v);
	}
	
	public void addStructuredLocalVariable(Root l) {
		StructuredLocalVariable v = new StructuredLocalVariable(l, this);

        createStructuredLocalVariable(v);
				
		locals.put(l, v);
	}
	
	public void addPrimitiveReturn(ReturnValue r) {
		PrimitiveLocalVariable v = new PrimitiveLocalVariable(r, this);

        createPrimitiveLocalVariable(v);

		returns.put(r, v);
	}

	public void addStructuredReturn(ReturnValue r) {
		StructuredLocalVariable v = new StructuredLocalVariable(r, this);

        createStructuredLocalVariable(v);

		returns.put(r, v);
	}

	public void addClass(StaticElementInfo elementInfo, ThreadInfo threadInfo) {
		boolean excluded = false;

        String name = elementInfo.getClassInfo().getName();
			
		for (String e : doNotMonitor) {
			excluded |= name.startsWith(e);
		}
			
		if (!excluded) {
			PackageAndClass c = DefaultPackageAndClass.create(name);

            LoadedClass lc = new LoadedClass(c);

			classes.put(c, lc);

            StructuredValueIdentifier value = universe.add(elementInfo, threadInfo);

            lc.addPossibleStructuredValue(value);

            universe.get(value).addParentSlot(lc, LoadedClass.slotKey);
		}
	}

    public void addThread(ThreadInfo threadInfo) {
        universe.add(threadInfo.getThreadObject(), threadInfo);
    }
	
	/**
	 * Resolve a path to all values it may be pointing to (primitive/objects)
	 */
	private void lookupValues(AccessExpression expression, Set<UniverseIdentifier> outValues) {
		if (expression.getRoot() instanceof AnonymousExpression) {
			AnonymousExpression anonymous = (AnonymousExpression) expression.getRoot();
			Reference reference = anonymous.getReference();
			
			if (universe.contains(reference)) {
				universe.lookupValues(reference, expression, outValues);

                return;
			}
		}
		
		if (expression.getRoot().isLocalVariable()) {
			if (locals.containsKey(expression.getRoot())) {
				universe.lookupValues(locals.get(expression.getRoot()).getPossibleValues(), expression, outValues);
			}
			
            return;
		}
		
		if (expression.getRoot().isReturnValue()) {
			if (returns.containsKey(expression.getRoot())) {
				universe.lookupValues(returns.get(expression.getRoot()).getPossibleValues(), expression, outValues);
            }

            return;
		}

		if (expression.getRoot().isStatic()) {
			if (classes.containsKey(expression.getRoot())) {
				universe.lookupValues(classes.get(expression.getRoot()).getPossibleValues(), expression, outValues);
            }

            return;
		}
		
		throw new RuntimeException("Attempting to resolve access expression not rooted in a local variable nor a static field: " + expression);
	}
	
	/**
	 * Find all paths (up to a certain length) that can describe the given value (primitive/object)
	 */
	private void valueToAccessExpressions(UniverseIdentifier id, int maxLength, Set<AccessExpression> outAccessExpressions) {
		if (maxLength == 0) {
			return;
		}
		
        /**
         * Each object/array can be described by an anonymous object/array access expression
         */
        UniverseValue value = universe.get(id);

        // Objects
		if (value instanceof UniverseObject) {
			UniverseObject uo = (UniverseObject) value;

			outAccessExpressions.add(AnonymousObject.create(uo.getReference()));
		}

        // Arrays
		if (value instanceof UniverseArray) {
			UniverseArray ua = (UniverseArray) value;

			outAccessExpressions.add(AnonymousArray.create(ua.getReference()));
		}

        /**
         * It can as well be described as object field/array element of its parental object/array
         * or a local variable that contains it
         */
		for (UniverseValue.Pair<Identifier, UniverseSlotKey> pair: value.getParentSlots()) {
			Identifier parent = pair.getFirst();
			
			if (parent instanceof StructuredValueIdentifier) {
				Set<AccessExpression> resolution = new HashSet<AccessExpression>();

                StructuredValueIdentifier parentObject = (StructuredValueIdentifier) parent;
                UniverseSlot slot = universe.get(parentObject).getSlot(pair.getSecond());
                
                valueToAccessExpressions(parentObject,  maxLength - 1, resolution);

				for (AccessExpression prefix : resolution) {
					AccessExpression path = null;
					
					if (universe.get(parentObject) instanceof Associative) {						
						path = DefaultObjectFieldRead.create(prefix, ((FieldName) slot.getSlotKey()).getName());
					}
					
					if (universe.get(parentObject) instanceof Indexed) {
						path = DefaultArrayElementRead.create(prefix, Constant.create(((ElementIndex) slot.getSlotKey()).getIndex()));
					}
					
					outAccessExpressions.add(path);
				}
			} else if (parent instanceof LocalVariable) {
				LocalVariable l = (LocalVariable) parent;
				
                //TODO change scope from flatsymboltable instance to something else that does get broken after making copies
				if (l.getScope() == this) {
                    outAccessExpressions.add(l.getAccessExpression());
                }
			} else if (parent instanceof LoadedClass) {
				LoadedClass c = (LoadedClass) parent;
				
				outAccessExpressions.add(c.getAccessExpression());
			}
		}
	}

	@Override
	public int count() {
		return getFilteredRelevantAccessExpressions().size();
	}
	
	/**
	 * Store values pointed to by one expression into the other
	 */
	@Override
	public Set<AccessExpression> processPrimitiveStore(Expression from, AccessExpression to) {
		return processPrimitiveStore(from, this, to);
	}

	/**
	 * Same as the other variant except it allows cross-universe writes (Necessary for method call boundaries)
     *
     * @return access path to all the affected objects
	 */
	public Set<AccessExpression> processPrimitiveStore(Expression from, FlatSymbolTable fromTable, AccessExpression to) {
        fromTable.ensureAnonymousObjectExistance(from);

		ensureAnonymousObjectExistance(from);
		ensureAnonymousObjectExistance(to);
		
		Set<AccessExpression> ret = new HashSet<AccessExpression>();
		Set<UniverseIdentifier> destinations = new HashSet<UniverseIdentifier>();
        
        lookupValues(to, destinations);

		for (UniverseIdentifier destination : destinations) {
            PrimitiveValueIdentifier oldValue = (PrimitiveValueIdentifier) destination;

			valueToAccessExpressions(oldValue, getMaximalAccessExpressionLength(), ret);
		}

		return ret;
	}
	
	/**
	 * Store values pointed to by one expression into the other
     */
    @Override
    public Set<AccessExpression> processObjectStore(Expression from, AccessExpression to) {
        return processObjectStore(from, this, to);
    }
       
    /**
     * Same as the other variant except it allows cross-universe writes (Necessary for method call boundaries)
     */
    // The universes may differ instance-wise (different objects representing the same universe)
    // FromTable may have a different Locals/Statics sets
    public Set<AccessExpression> processObjectStore(Expression from, FlatSymbolTable fromTable, AccessExpression to) {
        fromTable.ensureAnonymousObjectExistance(from);

		ensureAnonymousObjectExistance(from);
		ensureAnonymousObjectExistance(to);
		
		Set<AccessExpression> ret = new HashSet<AccessExpression>();
		Set<UniverseIdentifier> destinations = new HashSet<UniverseIdentifier>();
        
        lookupValues(to.cutTail(), destinations);
		
        Set<UniverseIdentifier> sources = new HashSet<UniverseIdentifier>();

        // First collect objects which may be referred to by the `from` access expression

		if (from instanceof AccessExpression) {
            fromTable.lookupValues((AccessExpression) from, sources);
		}
		
        // Special case is, when we store a constant MJIEnv.NULL which stands for NULL (by type of the target we know whether it is primitive MJIEnv.NULL or NULL)
		if (from instanceof Constant) {
			Constant referenceConstant = (Constant) from; // null, MJIEnv.NULL
			
            int ref = referenceConstant.value.intValue();
            VM vm = VM.getVM();
            ThreadInfo ti = vm.getCurrentThread();
            ElementInfo ei = ti.getElementInfo(ref);

            Reference reference = new Reference(ei, ti);

			if (universe.contains(reference)) {
				sources.add(reference);
			}
		}
		
		boolean ambiguous = destinations.size() > 1;
		
        // For each new parent (object whose field/element is being set, or a local var ...)
        // Add the objects into the field/element or rewrite a local variable
		for (UniverseIdentifier destination : destinations) {
			if (to instanceof ObjectFieldRead) {
				ObjectFieldRead read = (ObjectFieldRead) to;
				StructuredValueIdentifier parent = (StructuredValueIdentifier) destination;
				
				FieldName field = new FieldName(read.getField().getName());

                Set<AccessExpression> prefixes = new HashSet<AccessExpression>();
                valueToAccessExpressions(parent, getMaximalAccessExpressionLength(), prefixes);
				
				for (AccessExpression prefix : prefixes) {
					ret.add(DefaultObjectFieldRead.create(prefix, field.getName()));
				}
				
    			Associative associative = (Associative) universe.get(parent);

				if (!ambiguous) {
					associative.getField(field).clear();
				}
				
                for (UniverseIdentifier value : sources) {
                    StructuredValueSlot slot = (StructuredValueSlot) associative.getField(field);
                    
                    slot.addPossibleStructuredValue((StructuredValueIdentifier) value);

                    universe.get(value).addParentSlot(parent, field);
                }
			}
			
			if (to instanceof ArrayElementRead) {
				StructuredValueIdentifier parent = (StructuredValueIdentifier) destination;

                Set<AccessExpression> prefixes = new HashSet<AccessExpression>();
                valueToAccessExpressions(parent, getMaximalAccessExpressionLength(), prefixes);
				
				for (int i = 0; i < ((UniverseArray) universe.get(parent)).getLength(); ++i) {
					for (AccessExpression prefix : prefixes) {
						ret.add(DefaultArrayElementRead.create(prefix, Constant.create(i)));
					}
					
					Indexed indexed = (Indexed) universe.get(parent);
                    ElementIndex index = new ElementIndex(i);

					if (!ambiguous) {
						indexed.getElement(index).clear();
					}
					
                    for (UniverseIdentifier value : sources) {
                        StructuredValueSlot slot = (StructuredValueSlot) indexed.getElement(index);
                        
                        slot.addPossibleStructuredValue((StructuredValueIdentifier) value);

                        universe.get(value).addParentSlot(parent, index);
                    }
				}
			}
			
			if (to instanceof Root) {
				StructuredLocalVariable parent;

				if (to instanceof ReturnValue) {
					parent = (StructuredLocalVariable) returns.get(to);
				} else {
					parent = (StructuredLocalVariable) locals.get(to);
				}
				
				ret.add(parent.getAccessExpression());
					
				if (!ambiguous) {
                    //TODO for all remove parent
					parent.clear();
				}
					
                for (UniverseIdentifier value : sources) {
                    parent.addPossibleStructuredValue((StructuredValueIdentifier) value);

                    universe.get(value).addParentSlot(parent, StructuredLocalVariable.slotKey);
                }
			}
		}

        System.err.println(to + " := " + from + " \t " + ret);

        Set<UniverseIdentifier> objs = new HashSet<UniverseIdentifier>();

        lookupValues(to, objs);

        for (UniverseIdentifier id : objs) {
            Set<AccessExpression> vals = new HashSet<AccessExpression>();
            valueToAccessExpressions(id, 10, vals);

            System.err.println(to + " = " + vals);
        }
		
		return ret;
	}

	/**
	 * Creates (if not already existent) object in the universe to match an anonymous expression
	 */
	private void ensureAnonymousObjectExistance(Expression expr) {
		if (expr instanceof AnonymousExpression) {
            Reference reference = ((AnonymousExpression) expr).getReference();

			universe.add(reference.getElementInfo(), reference.getThreadInfo());
		}
	}

	@Override
	public boolean isArray(AccessExpression path) {
		boolean isArray = false;

        Set<UniverseIdentifier> values = new HashSet<UniverseIdentifier>();

        lookupValues(path, values);
		
		for (UniverseIdentifier value : values) {
			if (value instanceof PrimitiveValueIdentifier) return false;
			
			isArray |= universe.get((StructuredValueIdentifier) value) instanceof UniverseArray;
		}
		
		return isArray;
	}

	@Override
	public boolean isObject(AccessExpression path) {
		boolean isObject = false;
		
        Set<UniverseIdentifier> values = new HashSet<UniverseIdentifier>();

        lookupValues(path, values);
		
		for (UniverseIdentifier value : values) {
			if (value instanceof PrimitiveValueIdentifier) return false;
			
			isObject |= universe.get((StructuredValueIdentifier) value) instanceof UniverseObject;
		}
		
		return isObject;
	}

	@Override
	public boolean isPrimitive(AccessExpression path) {
		boolean isPrimitive = false;
		
        Set<UniverseIdentifier> values = new HashSet<UniverseIdentifier>();

        lookupValues(path, values);
		
		for (UniverseIdentifier value : values) {
			if (value instanceof StructuredValueIdentifier) return false;
			
			isPrimitive |= value instanceof PrimitiveValueIdentifier;
		}
		
		return isPrimitive;
	}
	
	@Override
	public FlatSymbolTable clone() {
		FlatSymbolTable clone = new FlatSymbolTable(abstraction);
		
		clone.universe = universe.clone();
		clone.locals = new HashMap<Root, LocalVariable>();
		clone.returns = new HashMap<ReturnValue, LocalVariable>();
		clone.classes = new HashMap<PackageAndClass, LoadedClass>();
		
		for (Root l : locals.keySet()) {
			LocalVariable lValue = locals.get(l);
			
			clone.locals.put(l, lValue);
		}
		
		for (ReturnValue r : returns.keySet()) {
			LocalVariable lValue = returns.get(r);

			clone.returns.put(r, lValue);
		}

		for (PackageAndClass c : classes.keySet()) {
			LoadedClass cValue = classes.get(c);
			
			clone.classes.put(c, cValue);
		}

		return clone;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		Set<AccessExpression> order = new TreeSet<AccessExpression>(new Comparator<AccessExpression>() {

			public int compare(AccessExpression expr1, AccessExpression expr2) {
				if (expr1.getLength() < expr2.getLength()) return -1;
				if (expr2.getLength() < expr1.getLength()) return +1;
				
				return expr1.toString(Notation.DOT_NOTATION).compareTo(expr2.toString(Notation.DOT_NOTATION));
			}
		});
		
		order.addAll(getFilteredRelevantAccessExpressions());
				
		int maxLength = 0;
		int padding = 4;
		
		for (AccessExpression expr : order) {
			maxLength = Math.max(maxLength, expr.toString(Notation.DOT_NOTATION).length());
		}
		
		for (AccessExpression expr : order) {
			ret.append(expr.toString(Notation.DOT_NOTATION));
			
			for (int i = 0; i < maxLength - expr.toString(Notation.DOT_NOTATION).length() + padding; ++i) {
				ret.append(' ');
			}
			
            Set<UniverseIdentifier> values = new HashSet<UniverseIdentifier>();

            lookupValues(expr, values);

			ret.append(values);
			
			ret.append('\n');
		}
		
		return ret.toString();
	}
	
	/**
	 * All symbolic expressions pointing to some values in the universe (up to the length of the longest path in a predicate currently being used in the abstraction) that are not filtered out by their name (some system objects are omitted from the output to increase the readability)
	 */
	private Set<AccessExpression> getFilteredRelevantAccessExpressions() {
		Set<AccessExpression> filtered = new HashSet<AccessExpression>();
	
		for (AccessExpression expr : getAllRelevantAccessExpressions()) {
			if (expr.getRoot() instanceof PackageAndClass) {
				boolean excluded = false;
				
				for (String e : doNotPrint) {
					excluded |= expr.getRoot().getName().startsWith(e);
				}
				
				if (excluded) {
					continue;
				}
			}
			
			filtered.add(expr);
		}
		
		return filtered;
	}

	/**
	 * All symbolic expressions pointing to some values in the universe (up to the length of the longest path in a predicate currently being used in the abstraction)
	 */
	private Set<AccessExpression> getAllRelevantAccessExpressions() {
		Set<AccessExpression> ret = new HashSet<AccessExpression>();
		
		for (Root l : locals.keySet()) {			
			for (UniverseIdentifier value : locals.get(l).getPossibleValues()) {
				subValueAccessExpressions(value, l, getMaximalAccessExpressionLength(), ret);
			}
		}
		
		for (ReturnValue r : returns.keySet()) {
			for (UniverseIdentifier value : returns.get(r).getPossibleValues()) {
				subValueAccessExpressions(value, r, getMaximalAccessExpressionLength(), ret);
			}
		}

		for (PackageAndClass c : classes.keySet()) {			
			for (UniverseIdentifier value : classes.get(c).getPossibleValues()) {
				subValueAccessExpressions(value, c, getMaximalAccessExpressionLength(), ret);
			}
		}
		
		return ret;
	}

	/**
	 * Create symbolic expressions accessing subvalues (fields, elements, or deeper) of a given value starting with the prefix (stop after a threshold is reached)
	 */
	private void subValueAccessExpressions(UniverseIdentifier id, AccessExpression prefix, int maximalAccessExpressionLength, Set<AccessExpression> outAccessExpressions) {
		if (maximalAccessExpressionLength == 0) return;
		
		outAccessExpressions.add(prefix);

        if (id instanceof PrimitiveValueIdentifier) return;

        StructuredValue value = universe.get((StructuredValueIdentifier) id);

		if (value instanceof Associative) {
			Associative object = (Associative) value;
			
			for (FieldName fieldName : object.getFields().keySet()) {
				for (UniverseIdentifier field : object.getField(fieldName).getPossibleValues()) {
					subValueAccessExpressions(field, DefaultObjectFieldRead.create(prefix, fieldName.getName()), maximalAccessExpressionLength - 1, outAccessExpressions);
				}
			}
		}
		
		if (value instanceof Indexed) {
			Indexed array = (Indexed) value;
			
			for (ElementIndex index : array.getElements().keySet()) {
				for (UniverseIdentifier element : array.getElement(index).getPossibleValues()) {
					subValueAccessExpressions(element, DefaultArrayElementRead.create(prefix, Constant.create(index.getIndex())), maximalAccessExpressionLength - 1, outAccessExpressions);
				}
			}
		}
		
	}

	private int getMaximalAccessExpressionLength() {
		int ret = GUARANTEED_LENGTH;

        Set<AccessExpression> paths = new HashSet<AccessExpression>();
		
		for (Predicate predicate : abstraction.getPredicateValuation().getPredicates()) {
            predicate.addAccessExpressionsToSet(paths);

			for (AccessExpression expr : paths) {
				ret = Math.max(ret, expr.getLength());
			}

            paths.clear();
		}
		
		return ret;
	}
	
	public void removeLocals() {
		for (Root l : locals.keySet()) {
			LocalVariable local = locals.get(l);
			
			local.clear();
		}
		
		for (ReturnValue r : returns.keySet()) {
			LocalVariable ret = returns.get(r);

			ret.clear();
		}

		locals.clear();
		returns.clear();
	}

}
