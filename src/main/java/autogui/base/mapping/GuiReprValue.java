package autogui.base.mapping;

import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

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
     *       and the parent is already checkAndUpdateSource and a new value is supplied
     */
    @Override
    public boolean checkAndUpdateSource(GuiMappingContext context) {
        try {
            Object next = getUpdatedValue(context, false);
            if (next != null && next.equals(GuiTypeValue.NO_UPDATE)) {
                return false;
            } else {
                context.setSource(next);
                return true;
            }
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
            return false;
        }
    }

    /**
     * obtains the current value of the context
     * @param context  target context
     * @param executeParent  indicates whether recursively invoke the method for the parent if the parent is a property pane.
     *                        If it is checking process, then the order is from root to bottom,
     *                         and it might be parent is already set.
     * @return the current value (nullable) or {@link GuiTypeValue#NO_UPDATE}
     * @throws Exception might be caused by executing method invocations.
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
        return null;
    }

    public Object getParentSource(GuiMappingContext context, boolean executeParent) throws Throwable {
        if (executeParent) {
            if (context.isParentPropertyPane()) {
                return context.getParentPropertyPane()
                        .getUpdatedValue(context.getParent(), true);
            } else if (context.isParentCollectionElement()) {
                throw new UnsupportedOperationException("parent is a collection: it requires an index: " + context); //TODO
            } else if (context.isParentValuePane()) {
                return context.getParentValuePane()
                        .getUpdatedValue(context.getParent(), true);
            } else {
                return context.getParentSource();
            }
        } else {
            return context.getParentSource();
        }
    }


    public void updateFromGui(GuiMappingContext context, Object newValue) {
        context.getPreferences().addHistoryValue(newValue);

        if (context.isTypeElementProperty()) {
            Object src = context.getParentSource();
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
        } else if (context.isParentCollectionElement()) {
            //TODO nothing?
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

    /** subclass can change to returned type and convert the value to the type.
     * a typical use case is just down-casting and converting null to an empty object. */
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

    public static <T> T castOrMake(Class<T> cls, Object o, Supplier<T> v) {
        return o != null && cls.isInstance(o) ? cls.cast(o) : v.get();
    }

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
    }

}
