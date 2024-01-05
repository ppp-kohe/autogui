package org.autogui.base.mapping;

import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.GuiMappingContext.GuiSourceValue;
import org.autogui.base.type.GuiTypeMemberProperty;
import org.autogui.base.type.GuiTypeValue;
import org.autogui.base.type.GuiUpdatedValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * a value component associated with {@link GuiTypeValue}.
 *  matching {@link Class} with the subclass implementing {@link #matchValueType(Class)}.
 */
public class GuiReprValue implements GuiRepresentation {
    public GuiReprValue() {}
    @Override
    public boolean match(GuiMappingContext context) {
        Class<?> cls = getValueType(context);
        if (cls != null && matchValueType(cls)) {
            context.setRepresentation(this);
            return true;
        } else {
            return false;
        }
    }

    /**
     * obtains the value type from the context.
     *   the implementation checks the context is a property, or an element value
     * @param context the context
     * @return a property type class, an element value class, or null
     */
    public Class<?> getValueType(GuiMappingContext context) {
        if (context.isTypeElementProperty()) {
            return context.getTypeElementPropertyTypeAsClass();
        } else if (context.isTypeElementValue()) {
            return context.getTypeElementValueAsClass();
        } else {
            return null;
        }
    }

    public boolean matchValueType(Class<?> cls) {
        return true;
    }

    /**
     * the class supposes that the parent is a {@link GuiReprPropertyPane}: [propName: [objectPane]].
     *  then, the parent of source and this source are a same value;
     *       and the parent is already checkAndUpdateSource and a new value is supplied.
     *   the method uses {@link #getUpdatedValue(GuiMappingContext, ObjectSpecifier)} with the {@link #NONE} specifier.
     *   <p>
     *   As additional side effect, the updated value will be added to the value-history if supported
     *    by {@link #addHistoryValue(GuiMappingContext, Object)},
     *    and the value will also be set as a new source value by {@link #setSource(GuiMappingContext, Object)}
     *  @param context the source context
     *  @return the source context is updated or not
     */
    @Override
    public boolean checkAndUpdateSource(GuiMappingContext context) {
        try {
            GuiUpdatedValue next = getUpdatedValue(context, NONE);
            if (next.isNone()) {
                return false;
            } else {
                addHistoryValue(context, next.getValue());
                setSource(context, next.getValue());
                return true;
            }
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
            return false;
        }
    }

    public void addHistoryValue(GuiMappingContext context, Object value) {
        try {
            if (isHistoryValueSupported(context)) {
                GuiPreferences prefs = context.getPreferences();
                try (var lock = prefs.lock()) {
                    lock.use();
                    context.getPreferences().addHistoryValue(value);
                    context.getPreferences().setCurrentValue(value);
                }
            }
        } catch (Throwable ex) {
            errorWhileAddHistoryValue(context, value, ex);
        }
    }

    /**
     * called from {@link #addHistoryValue(GuiMappingContext, Object)}
     *  and special handling for {@link IllegalAccessException}
     *      which means an addition of a large object to the prefs store
     * @param context the context object
     * @param value the added value
     * @param ex   the exception caught
     * @since 1.2
     */
    public void errorWhileAddHistoryValue(GuiMappingContext context, Object value, Throwable ex) {
        if (ex instanceof IllegalArgumentException) {
            String str = Objects.toString(ex.getMessage());
            if (str.length() > 32) {
                str = str.substring(0, 32) + "...";
            }
            GuiLogManager.get().logFormat("ignore addHistoryValue: %s", str);
        } else {
            context.errorWhileUpdateSource(ex);
        }
    }

    public void setSource(GuiMappingContext context, Object value) {
        context.getContextClock().increment();
        context.setSource(GuiSourceValue.of(value));
    }

    /**
     * obtain the property value from parentSource as an owner.
     *  <p>
     *    In this class, support following cases:
     *     <ul>
     *         <li>the type is a {@link GuiTypeMemberProperty}</li>
     *         <li>other types: use methods of {@link GuiTypeValue} with prev and parentSource as the inherited value</li>
     *     </ul>
     * @param context the context holding the repr.
     * @param parentSource the property owner. if no value then nothing will happen for properties
     * @param specifier the specifier of the value
     * @param prev a previous value compared to the obtained value
     * @return an obtained property value (nullable) or {@link GuiUpdatedValue#NO_UPDATE} if the value is equivalent to the previous value.
     * @throws Throwable might be caused by executing method invocations.
     */
    public GuiUpdatedValue getValue(GuiMappingContext context, GuiSourceValue parentSource,
                                    ObjectSpecifier specifier, GuiSourceValue prev) throws Throwable {
        if (context.isTypeElementProperty()) {
            GuiTypeMemberProperty prop = context.getTypeElementAsProperty();
            return fromSourceUpdated(context, context.execute(() ->
                        prev.isNone() ? //it always needs the parent source
                                prop.executeGet(toParentSource(context, parentSource)) :
                                prop.executeGet(toParentSource(context, parentSource), toSource(prev.getValue()))));

        } else if (context.isTypeElementValue() || context.isTypeElementObject() || context.isTypeElementCollection()) {
            GuiTypeValue val = context.getTypeElementValue();
            return context.execute(() -> { //in the branch, parentSource is handled as the value of the repr. so, use toSource
                if (prev.isNone() && parentSource.isNone()) {
                    return fromSourceUpdated(context, val.getValue());
                } else if (prev.isNone() && !parentSource.isNone()) {
                    return fromSourceUpdated(context, val.getValue(toSource(parentSource.getValue())));
                } else if (!prev.isNone() && parentSource.isNone()) {
                    return fromSourceUpdated(context, val.updatedValue(toSource(prev.getValue())));
                } else {
                    return fromSourceUpdated(context, val.updatedValue(toSource(prev.getValue()), toSource(parentSource.getValue())));
                }
            });
        } else {
            if (prev.isNone()) {
                return GuiUpdatedValue.NO_UPDATE;
            } else {
                return GuiUpdatedValue.of(prev.getValue()); //toSource -> fromSource
            }
        }
    }

    public static void convertLog(String msg, Object from, Object to, GuiMappingContext context) {
        System.err.println(msg + " : " + id(from) + " -> " + id(to) + " : " + context);
    }

    private static String id(Object o) {
        return o == null ? "null" : String.format("<%x:%s>", System.identityHashCode(o), o.getClass().getSimpleName());
    }

    public Object toParentSource(GuiMappingContext context, GuiSourceValue v) {
        Object o;
        if (context.hasParent()) {
            if (context.getParent().isReprCollectionElement() && context.getParent().hasParent()) {
                o = context.getParent().getParent().getReprValue().toSource(v.getValue());
            } else {
                o = context.getParent().getReprValue().toSource(v.getValue());
            }
        } else {
            o = v.getValue();
        }
        return o;
    }

    protected GuiUpdatedValue fromSourceUpdated(GuiMappingContext context, GuiUpdatedValue v) {
        if (v.isNone()) {
            return v;
        } else {
            Object o = fromSource(v.getValue());
            return GuiUpdatedValue.of(o);
        }
    }

    /**
     * call {@link #getValue(GuiMappingContext, GuiMappingContext.GuiSourceValue, GuiReprValue.ObjectSpecifier, GuiMappingContext.GuiSourceValue)}
     * and never return NO_UPDATE
     * @param context the context
     * @param parentSource the property owner. if null then nothing will happen for properties
     * @param specifier the specifier of the value
     * @return an obtained value or null
     * @throws Throwable might be caused by executing method invocation
     */
    public Object getValueWithoutNoUpdate(GuiMappingContext context, GuiSourceValue parentSource, ObjectSpecifier specifier) throws Throwable {
        return unwrapNoUpdate(GuiMappingContext.NO_SOURCE,
                getValue(context, parentSource, specifier, GuiMappingContext.NO_SOURCE));
    }

    public static Object unwrapNoUpdate(GuiSourceValue prev, GuiUpdatedValue ret) {
        if (ret.isNone()) {
            return prev.getValue();
        } else {
            return ret.getValue();
        }
    }

    /**
     * obtain the current value of the context.
     *  This is called from {@link #checkAndUpdateSource(GuiMappingContext)},
     *   and invoke {@link #getParentSource(GuiMappingContext, GuiReprValue.ObjectSpecifier)}
     *   and {@link #getValue(GuiMappingContext, GuiMappingContext.GuiSourceValue, GuiReprValue.ObjectSpecifier, GuiMappingContext.GuiSourceValue)}
     * @param context  target context,
     * @param specifier the specifier of the value
     * @return the current value (nullable) or {@link GuiUpdatedValue#NO_UPDATE}
     * @throws Throwable might be caused by executing method invocations.
     */
    public GuiUpdatedValue getUpdatedValue(GuiMappingContext context, ObjectSpecifier specifier) throws Throwable {
        GuiSourceValue prev = context.getSource();
        GuiSourceValue src = getParentSource(context, specifier.getParent());
        return getValue(context, src, specifier, prev);
    }

    /**
     * call {@link #getUpdatedValue(GuiMappingContext, ObjectSpecifier)} and never return NO_UPDATE
     * @param context the context
     * @param specifier the specifier of the value
     * @return original source (nullable), never NO_UPDATE
     * @throws Throwable might be caused by executing method invocation
     */
    public Object getUpdatedValueWithoutNoUpdate(GuiMappingContext context, ObjectSpecifier specifier) throws Throwable {
        GuiSourceValue prev = context.getSource();
        return unwrapNoUpdate(prev,
                getUpdatedValue(context, specifier));
    }

    /**
     * call {@link #getUpdatedValue(GuiMappingContext, ObjectSpecifier)}
     *  only if the context holds a value pane and 1) the current source is NONE or 2) {@link ObjectSpecifier#isUsingCache()}=false,
     *  and if NO_UPDATE, then return the current source from the context,
     *
     * @param context the context
     * @param specifier the specifier of the value
     * @return the updated value as a source-value or the current source value of the context
     * @throws Throwable might be caused d by executing method invocation
     */
    public GuiSourceValue getUpdatedSource(GuiMappingContext context, ObjectSpecifier specifier) throws Throwable {
        if (context.isParentValuePane() &&
                (context.getSource().isNone() || specifier.isUsingCache())) {
            GuiUpdatedValue v = context.getReprValue().getUpdatedValue(context, specifier);
            if (v.isNone()) {
                return context.getSource();
            } else {
                return GuiSourceValue.of(v.getValue());
            }
        } else {
            return context.getSource();
        }
    }


    /**
     * obtain the current parent value from the parent context
     *  by calling {@link #getUpdatedSource(GuiMappingContext, ObjectSpecifier)}
     * @param context the context of the repr.
     * @param parentSpecifier the specifier of the parent value
     * @return the source value of the parent
     * @throws Throwable the action might cause an error
     */
    public GuiSourceValue getParentSource(GuiMappingContext context, ObjectSpecifier parentSpecifier) throws Throwable {
        if (context.hasParent()) {
            return context.getParent().getReprValue().getUpdatedSource(context.getParent(), parentSpecifier);
        } else {
            return context.getParentSource();
        }
    }

    /**
     * a table pane returns an element in a collection
     * @param context the context of the repr
     * @param collection a collection target
     * @param elementSpecifier an element index i.e. {@link ObjectSpecifierIndex}
     * @param prev the previous value of the element
     * @return an element in the collection, default is null
     */
    public GuiUpdatedValue getValueCollectionElement(GuiMappingContext context, GuiSourceValue collection,
                                                     ObjectSpecifier elementSpecifier, GuiSourceValue prev) {
        return null;
    }

    /**
     * a table pane returns the size of a collection
     * @param context the context of the repr
     * @param collection a collection target
     * @param specifier the specifier of the collection
     * @return the size of the collection, default is 1
     * @throws Throwable an error while getting
     */
    public int getValueCollectionSize(GuiMappingContext context, GuiSourceValue collection, ObjectSpecifier specifier) throws Throwable {
        return 1;
    }

    /**
     * a table pane updates a specified element
     * @param context the context of the repr
     * @param collection a collection target
     * @param newValue an element value
     * @param elementSpecifier an element index i.e. {@link ObjectSpecifierIndex}
     * @return the updated value
     * @throws Throwable an error while updating
     */
    public Object updateCollectionElement(GuiMappingContext context, GuiSourceValue collection,
                                          Object newValue, ObjectSpecifier elementSpecifier) throws Throwable {
        return newValue;
    }

    /**
     * call the setter by editing on GUI, and {@link GuiMappingContext#updateSourceFromGui(Object)}.
     * <ul>
     *   <li>add <code>newValue</code> to the history by {@link #addHistoryValue(GuiMappingContext, Object)} </li>
     *   <li>obtain parent source and update {@link #updateWithParentSource(GuiMappingContext, Object, ObjectSpecifier)}</li>
     *   <li>check {@link #isUpdateContextSourceByUpdateFromGui(GuiMappingContext)} and
     *         {@link GuiMappingContext#updateSourceFromGui(Object)}: update the source and notify to listeners</li>
     * </ul>
     *
     * @param context the context of this repr.
     * @param newValue the updated property value
     * @param specifier the specifier of the value
     * @param viewClock the clock value of sender view
     */
    public void updateFromGui(GuiMappingContext context, Object newValue, ObjectSpecifier specifier,
                              GuiTaskClock viewClock) {
        try {
            if (context.getContextClock().isOlderWithSet(viewClock)) {
                addHistoryValue(context, newValue);
                Object ret = updateWithParentSource(context, newValue, specifier);
                if (isUpdateContextSourceByUpdateFromGui(context)) {
                    context.updateSourceFromGui(ret);
                }
            }
        } catch (Throwable e) {
            context.errorWhileUpdateSource(e);
        }
    }

    public boolean isUpdateContextSourceByUpdateFromGui(GuiMappingContext context) {
        return true;
    }

    /**
     * obtain the parent source by {@link #getUpdatedSource(GuiMappingContext, GuiReprValue.ObjectSpecifier)} and
     *   call {@link #update(GuiMappingContext, GuiMappingContext.GuiSourceValue, Object, GuiReprValue.ObjectSpecifier)}
     * @param context the target context
     * @param newValue a new value to be set to the property
     * @param specifier the specifier of the value
     * @return the returned value by updating
     * @throws Throwable an error while getParentSource or update
     */
    public Object updateWithParentSource(GuiMappingContext context, Object newValue, ObjectSpecifier specifier) throws Throwable {
        GuiSourceValue src = getParentSource(context, specifier.getParent());
        if (src.isNone()) {
            System.err.println("no src: context=" + context.getRepresentation() + " : parent=" + context.getParentRepresentation());
        }
        return update(context, src, newValue, specifier);
    }

    /**
     * set a new value to a property of an object, which specified by the context
     *  <ul>
     *      <li>using {@link GuiMappingContext#execute(Callable)}</li>
     *      <li> if the type is a {@link GuiTypeMemberProperty},
     *         update the property by {@link GuiTypeMemberProperty#executeSet(Object, Object)}</li>
     *      <li> if parent is a property {@link GuiReprPropertyPane}
     *            or a collection element {@link GuiReprCollectionElement},
     *             obtain the parent source of the parent, and update the parent with the source and newValue.
     *             This will be done by parent's {@link #updateWithParentSource(GuiMappingContext, Object, ObjectSpecifier)}</li>
     *      <li> otherwise call {@link GuiTypeValue#writeValue(Object, Object)} or a similar method</li>
     *  </ul>
     * @param context the target context
     * @param parentSource the property owner
     * @param newValue a new value to be set to the property
     * @param specifier the specifier of the value
     * @return newValue which will need to be passed to {@link GuiMappingContext#updateSourceFromGui(Object)},  or null if error
     * @throws Throwable an error while updating
     */
    public Object update(GuiMappingContext context, GuiSourceValue parentSource,
                                  Object newValue, ObjectSpecifier specifier) throws Throwable {
        if (context.isTypeElementProperty()) {
            GuiTypeMemberProperty prop = context.getTypeElementAsProperty();
            return fromSource(context.execute(() ->
                    prop.executeSet(toParentSource(context, parentSource), toSource(newValue))));
        } else if (context.isParentPropertyPane() || context.isParentCollectionElement()) {
            return context.getParentValuePane()
                    .updateWithParentSource(context.getParent(), newValue, specifier.getParent());
        } else if (context.isTypeElementValue() || context.isTypeElementObject() || context.isTypeElementCollection()) {
            GuiSourceValue prev = context.getSource();
            GuiTypeValue val = context.getTypeElementValue();
            return context.execute(() -> { //in the branch, parentSource is handled as the value of the repr. so, use toSource
                if (prev.isNone() && parentSource.isNone()) {
                    return fromSource(val.writeValue(toSource(newValue)));
                } else if (prev.isNone() && !parentSource.isNone()) {
                    return fromSource(val.writeValueInheritedValue(toSource(parentSource.getValue()), toSource(newValue)));
                } else if (!prev.isNone() && parentSource.isNone()) {
                    return fromSource(val.writeValue(prev.getValue(), toSource(newValue)));
                } else {
                    return fromSource(val.writeValue(prev.getValue(), toSource(parentSource.getValue()), toSource(newValue)));
                }
            });

        }
        return null;
    }

    /***
     * @return if true, {@link #updateFromGui(GuiMappingContext, Object, ObjectSpecifier, GuiTaskClock)} automatically add the value to the preferences.
     *    the default impl. returns true.
     */
    public boolean isHistoryValueSupported() {
        return true;
    }

    /**
     * @param value the tested value, or null at loading
     * @return if true, actually the value is stored to the preferences store
     */
    public boolean isHistoryValueStored(Object value) {
        return isHistoryValueSupported();
    }

    /**
     * currently checked by {@link #isHistoryValueSupported(GuiMappingContext)}
     * @param context the context object
     * @param value stored value
     * @return true if the context supports storing the value as a history
     * @since 1.2
     */
    public boolean isHistoryValueStored(GuiMappingContext context, Object value) {
        return isHistoryValueSupported(context);
    }

    /**
     * checks whether the context supports the value history feature or not.
     *  it can be controlled by the annotation parameter  "history".
     * @param context the context object
     * @return true if the context supports storing history values
     * @since 1.2
     */
    public boolean isHistoryValueSupported(GuiMappingContext context) {
        if (context.isTypeElementProperty() &&
            !context.getTypeElementAsProperty().isHistoryValueSupported()) {
            return false;
        } else {
            return isEditable(context) && isHistoryValueSupported();
        }
    }

    /**
     *
     * @param context the context of the repr.
     * @return the result of {@link GuiTypeMemberProperty#isWritable()} and so on.
     */
    public boolean isEditable(GuiMappingContext context) {
        if (context.isTypeElementProperty()) {
            return context.getTypeElementAsProperty().isWritable();
        } else if (context.isParentPropertyPane()) {
            return context.getParentPropertyPane().isEditableFromChild(context);
        } else if (context.isTypeElementValue() || context.isTypeElementObject() || context.isTypeElementCollection()) {
            return context.getTypeElementValue().isWritable(context.getSource());
        } else {
            return true;
        }
    }

    /**
     * called from a GUI element in order to update its value.
     * subclass can change to returned type and convert the value to the type.
     * a typical use case is just down-casting and converting null to an empty object.
     *
     * @param context the context of the repr.
     * @param value the current value
     * @return the updated value
     */
    public Object toUpdateValue(GuiMappingContext context, Object value) {
        return value;
    }

    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        return null;
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        return target;
    }

    /**
     * @param context the target context
     * @return true means {@link #fromJson(GuiMappingContext, Object, Object)} takes a map with
     *          the entry named {@link GuiMappingContext#getName()}
     */
    public boolean isFromJsonTakingMapWithContextNameEntry(GuiMappingContext context) {
        return false;
    }

    public Object createNewValue(GuiMappingContext context) throws Throwable {
        return context.execute(() -> context.getTypeElementValue().createNewValue());
    }

    /**
     * cast <code>o</code> if <code>o</code> is a non-null and specified type <code>cls</code>,
     *     otherwise call <code>v</code>.
     *  used in {@link #toJson(GuiMappingContext, Object)}.
     * @param cls the required class <code>T</code>
     * @param o the current value, nullable
     * @param v the falling-back supplier
     * @param <T> the required type
     * @return <code>o</code> or the result of <code>v</code>
     */
    public static <T> T castOrMake(Class<T> cls, Object o, Supplier<T> v) {
        return cls.isInstance(o) ? cls.cast(o) : v.get(); //isInstance(null) -> false
    }

    @Override
    public String toString() {
        return toStringHeader();
    }

    /**
     * @param o a raw-object which can be directly passed to type-element's executing methods
     * @return a wrapped object for clients of the representation
     */
    public Object fromSource(Object o) {
        return o;
    }

    /**
     * @param o a wrapped object for clients of the representation
     * @return a raw-object which can be directly passed to type-element's executing methods
     */
    public Object toSource(Object o) {
        return o;
    }

    /**
     * pair object for a value and a String name, can be converted to a JSON object member.
     */
    public static class NamedValue {
        public String name;
        public Object value;

        public NamedValue(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public Map<String, Object> toJson() {
            return toJson(value);
        }

        public Map<String, Object> toJson(Object value) {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put(name, value);
            return map;
        }

        public void putTo(Map<String, Object> m) {
            m.put(name, value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NamedValue that = (NamedValue) o;
            return Objects.equals(name, that.name) &&
                    Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }

        @Override
        public String toString() {
            return "{" + name + ":" + value + "}";
        }
    }

    /**
     * an auxiliary information for specifying a property value.
     *  A typical example is an index of a list.
     *  <p>
     *  For regular properties, {@link GuiReprValue#NONE} is sufficient.
     *  <p>
     *   However, consider a list property, like
     *   <pre>
     *       class A { List&lt;B&gt; prop1; }
     *       class B { List&lt;C&gt; prop2; }
     *       class C { String prop3; }
     *   </pre>
     *   To specify a value of <code>prop3</code> from the top <code>A</code>,
     *     we can write <code>a.prop1.get(i).prop2.get(j).prop3</code>.
     *    <p>
     *      This class represents the expression of the property chain with indices:
     *       <code>NONE.childIndex(i).child(false).childIndex(j).child(false)</code>
     */
    public static class ObjectSpecifier {
        protected ObjectSpecifier parent;
        protected boolean usingCache;

        public ObjectSpecifier(ObjectSpecifier parent, boolean usingCache) {
            this.parent = parent;
            this.usingCache = usingCache;
        }

        /**
         * @return true if the obtained property value can be a cached value
         */
        public boolean isUsingCache() {
            return usingCache;
        }

        public ObjectSpecifier getParent() {
            return parent;
        }

        /**
         * @return index of an element, default is 0
         */
        public int getIndex() {
            return 0;
        }

        /**
         * @return true if the specifier is an index. default is false
         */
        public boolean isIndex() {
            return false;
        }

        /**
         * @param index the next index
         * @return created index specifier as a child of this specifier.
         */
        public ObjectSpecifierIndex childIndex(int index) {
            return new ObjectSpecifierIndex(this, index);
        }

        /**
         * @param usingCache specifying using cache values
         * @return created (non-index) specifier as a child of this specifier
         */
        public ObjectSpecifier child(boolean usingCache) {
            return new ObjectSpecifier(this, usingCache);
        }

        @Override
        public String toString() {
            String self = usingCache ? "O" : "o";
            if (parent == null) {
                return self;
            } else if (parent == this) {
                return "..." + self;
            } else {
                return parent + "." + self;
            }
        }
    }

    /**
     * the indexed specifier for specifying an element in a list
     */
    public static class ObjectSpecifierIndex extends ObjectSpecifier {
        protected int index;

        public ObjectSpecifierIndex(ObjectSpecifier parent, int index) {
            super(parent, false);
            this.index = index;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isIndex() {
            return true;
        }

        @Override
        public String toString() {
            if (parent != null && parent != this) {
                return parent + "[" + index + "]";
            } else if (parent == this) {
                return "...[" + index + "]";
            } else {
                return "?[" + index + "]";
            }
        }
    }

    /**
     * special specifier for non-indexed property values without caching.
     * the parent of the specifier always this instance (cyclic).
     */
    public static ObjectSpecifierNothing NONE = new ObjectSpecifierNothing(false);

    public static ObjectSpecifierNothing NONE_WITH_CACHE = new ObjectSpecifierNothing(true);

    /**
     * special specifier, can be obtained by {@link #NONE}
     */
    public static class ObjectSpecifierNothing extends ObjectSpecifier {
        public ObjectSpecifierNothing(boolean usingCache) {
            super(null, usingCache);
            this.parent = this;
        }

        @Override
        public String toString() {
            return "*";
        }
    }

    /**
     * @return a lambda for returning {@link #NONE}
     */
    public static Supplier<ObjectSpecifier> getNoneSupplier() {
        return () -> NONE;
    }
}
