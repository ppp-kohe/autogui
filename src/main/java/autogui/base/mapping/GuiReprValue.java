package autogui.base.mapping;

import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * a value component associated with {@link GuiTypeValue}.
 *  matching {@link Class} with the subclass implementing {@link #matchValueType(Class)}.
 */
public class GuiReprValue implements GuiRepresentation {
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
     * @return a property type class, a element value class, or null
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
     * * the class supposes the parent is a {@link GuiReprPropertyPane}: [propName: [objectPane]].
     *  then, the parent of source and this source are a same value;
     *       and the parent is already checkAndUpdateSource and a new value is supplied.
     *   the method uses {@link #getUpdatedValue(GuiMappingContext, boolean)} with <code>executeParent=false</code>.
     *   <p>
     *   As additional side-effect, the updated value will be added to the value-history if supported
     *  @param context the source context
     *  @return the source context is updated or not
     */
    @Override
    public boolean checkAndUpdateSource(GuiMappingContext context) {
        try {
            Object next = getUpdatedValue(context, false);
            if (next != null && next.equals(GuiTypeValue.NO_UPDATE)) {
                return false;
            } else {
                try {
                    if (isHistoryValueSupported()) {
                        context.getPreferences().addHistoryValue(next);
                    }
                } catch (Throwable ex) {
                    context.errorWhileUpdateSource(ex);
                }
                context.setSource(next);
                return true;
            }
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
            return false;
        }
    }

    /**
     * obtain the current value of the context.
     *  This is called from {@link #checkAndUpdateSource(GuiMappingContext)},
     *   and invoke the getter method for obtaining the value.
     *   <ul>
     *       <li>use <code>source</code> of the context as the previous value</li>
     *       <li>for executing {@link autogui.base.type.GuiTypeElement},
     *               use {@link GuiMappingContext#execute(Callable)}</li>
     *       <li>if this is a property, call {@link #getParentSource(GuiMappingContext, boolean)}
     *             for obtaining <code>target</code>, and call {@link GuiTypeMemberProperty#executeGet(Object, Object)}. </li>
     *       <li>if the parent is a property (suppose {@link GuiTypeMemberProperty} -&gt; {@link GuiTypeValue}),
     *             obtain a value from the parent property, and {@link GuiTypeMemberProperty#compareGet(Object, Object)}.</li>
     *       <li>if this is a value
     *             {@link GuiMappingContext#isTypeElementValue()},
     *             {@link GuiMappingContext#isTypeElementObject()}, or
     *             {@link GuiMappingContext#isTypeElementCollection()}, {@link GuiTypeValue#updatedValue(Object)}</li>
     *   </ul>
     * @param context  target context,
     * @param executeParent  indicates whether recursively invoke the method for the parent if the parent is a property pane.
     *                        If it is checking process, then the order is from root to bottom,
     *                         and it might be parent is already set.
     * @return the current value (nullable) or {@link GuiTypeValue#NO_UPDATE}
     * @throws Throwable might be caused by executing method invocations.
     */
    public Object getUpdatedValue(GuiMappingContext context, boolean executeParent) throws Throwable {
        Object prev = context.getSource();
        if (context.isTypeElementProperty()) {
            Object src = getParentSource(context, executeParent);
            GuiTypeMemberProperty prop = context.getTypeElementAsProperty();
            return context.execute(() ->
                    prop.executeGet(src, prev));
        } else {
            if (context.isParentPropertyPane()) {
                //GuiReprPropertyPane matches to GuiTypeMemberProperty, and it has compareGet(p,n)
                GuiTypeMemberProperty prop = context.getParent().getTypeElementAsProperty();
                Object obj = getParentSource(context, executeParent);
                return context.execute(() ->
                        prop.compareGet(prev, obj));
            } else if (context.isTypeElementValue() || context.isTypeElementObject() || context.isTypeElementCollection()) {
                GuiTypeValue val = context.getTypeElementValue();
                return context.execute(() ->
                        val.updatedValue(prev));
            }
        }
        return prev;
    }

    /**
     * caLl {@link #getUpdatedValue(GuiMappingContext, boolean)} and never return NO_UPDATE
     * @param context the context
     * @param executeParent indicates whether recursively invoke the method for the parent
     * @return original source (nullable), never NO_UPDATE
     * @throws Throwable might be caused by executing method invocation
     */
    public Object getUpdatedValueWithoutNoUpdate(GuiMappingContext context, boolean executeParent) throws Throwable {
        Object prev = context.getSource();
        Object ret = getUpdatedValue(context, executeParent);
        if (ret != null && ret.equals(GuiTypeValue.NO_UPDATE)) {
            ret = prev;
        }
        return ret;

    }

    /**
     * <ul>
     *     <li>if <code>executeParent=true</code> and the parent is a property,
     *            {@link GuiReprPropertyPane#getUpdatedValueWithoutNoUpdate(GuiMappingContext, boolean)}  for the parent</li>
     *     <li>if <code>executeParent=true</code>  and the parent is a collection element,
     *            it is an error</li>
     *     <li>if <code>executeParent=true</code> and the parent is a value,
     *            {@link GuiReprValue#getUpdatedValueWithoutNoUpdate(GuiMappingContext, boolean)} </li>
     *     <li>otherwise, the source value of the parent</li>
     * </ul>
     * @param context the context of the repr.
     * @param executeParent if true, it allows an action in order to obtain the value
     * @return the source value of the parent
     * @throws Throwable the action might cause an error
     */
    public Object getParentSource(GuiMappingContext context, boolean executeParent) throws Throwable {
        if (executeParent) {
            if (context.isParentPropertyPane()) {
                return context.getParentPropertyPane()
                        .getUpdatedValueWithoutNoUpdate(context.getParent(), true);
            } else if (context.isParentCollectionElement()) {
                throw new UnsupportedOperationException("parent is a collection: it requires an index: " + context); //TODO
            } else if (context.isParentValuePane()) {
                return context.getParentValuePane()
                        .getUpdatedValueWithoutNoUpdate(context.getParent(), true);
            } else {
                return context.getParentSource();
            }
        } else {
            return context.getParentSource();
        }
    }

    /**
     * call the setter by editing on GUI, and {@link GuiMappingContext#updateSourceFromGui(Object)}.
     * <ul>
     *   <li>it first adds <code>newValue</code> to the history. </li>
     *   <li>using {@link GuiMappingContext#execute(Callable)}</li>
     *   <li>if the parent is a collection element, </li>
     *   <li>if this is a property, {@link GuiMappingContext#getParentSource()} and
     *        {@link GuiMappingContext#updateSourceFromGui(Object)} with the <code>newValue</code></li>
     *   <li>if the parent is a property, {@link GuiReprPropertyPane#updateFromGuiChild(GuiMappingContext, Object)}
     *          with <code>newValue</code></li>
     *   <li>if this is a value, use {@link GuiMappingContext#getSource()} as the previous value,
     *        {@link GuiTypeValue#writeValue(Object, Object)} with the prev and <code>newValue</code>,
     *        and {@link GuiMappingContext#updateSourceFromGui(Object)}</li>
     * </ul>
     *
     *
     * @param context the context of this repr.
     * @param newValue the updated property value
     */
    public void updateFromGui(GuiMappingContext context, Object newValue) {
        try {
            if (isHistoryValueSupported()) {
                context.getPreferences().addHistoryValue(newValue);
            }
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }

        if (context.isParentCollectionElement()) {
            //
        } else if (context.isTypeElementProperty()) {
            Object src = context.getParentSource();
            if (src ==  null) {
                System.err.println("src is null: context="  +context.getRepresentation() + " : parent=" + context.getParent().getRepresentation());
            }
            try {
                GuiTypeMemberProperty prop = context.getTypeElementAsProperty();
                context.execute(() ->
                        prop.executeSet(src, newValue));
                context.updateSourceFromGui(newValue);
            } catch (Throwable ex) {
                context.errorWhileUpdateSource(ex);
            }
        } else if (context.isParentPropertyPane()) {
            context.getParentPropertyPane().updateFromGuiChild(context, newValue);
        } else if (context.isTypeElementValue() || context.isTypeElementObject() || context.isTypeElementCollection()) {
            Object prev = context.getSource();
            GuiTypeValue val = context.getTypeElementValue();
            try {
                Object next = context.execute(() -> val.writeValue(prev, newValue));
                context.updateSourceFromGui(next);
            } catch (Throwable ex) {
                context.errorWhileUpdateSource(ex);
            }
        }
    }

    /***
     * @return if true, {@link #updateFromGui(GuiMappingContext, Object)} automatically add the value to the preferences.
     *    the default impl. returns true.
     */
    public boolean isHistoryValueSupported() {
        return true;
    }

    /**
     * @return if true, actually the value is stored to the preferences store
     */
    public boolean isHistoryValueStored() {
        return isHistoryValueSupported();
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
            return false;
        }
    }

    /**
     * called from a GUI element in order to update the it's value.
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
        return o != null && cls.isInstance(o) ? cls.cast(o) : v.get();
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
    }

}
