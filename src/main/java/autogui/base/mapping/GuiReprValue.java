package autogui.base.mapping;

import autogui.base.type.GuiTypeValue;

import java.util.Objects;

public class GuiReprValue implements GuiRepresentation {
    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementProperty()) {
            Class<?> cls = context.getTypeElementPropertyTypeAsClass();
            if (matchValueType(cls)) {
                context.setRepresentation(this);
                return true;
            } else {
                return false;
            }
        } else if (context.isTypeElementValue()) {
            Class<?> cls = context.getTypeElementValueAsClass();
            if (matchValueType(cls)) {
                context.setRepresentation(this);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean matchValueType(Class<?> cls) {
        return true;
    }

    /**
     * * the class supposes the parent is a {@link GuiReprPropertyPane}: [propName: [objectPane]].
     *  then, the parent of source and this source is a same value;
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
        } catch (Exception ex) {
            context.errorWhileUpdateSource(ex);
            return false;
        }
    }

    /**
     * obtains current value of the context
     * @param context  target context
     * @param executeParent  indicates whether recursively invoke the method for the parent if the parent is a property pane.
     *                        If it is checking process, then the order is from root to bottom,
     *                         and it might be parent is already set.
     * @return the current value (nullable) or {@link GuiTypeValue#NO_UPDATE}
     * @throws Exception might be caused by executing method invocations.
     */
    public Object getUpdatedValue(GuiMappingContext context, boolean executeParent) throws Exception {
        Object prev = context.getSource();
        if (context.isTypeElementProperty()) {
            Object src = context.getParentSource();
            return context.getTypeElementAsProperty().executeGet(src, prev);
        } else {
            if (context.isParentPropertyPane()) {
                if (executeParent) {
                    return context.getParentPropertyPane()
                            .getUpdatedValue(context.getParent(), true);
                } else {
                    return context.getParent().getSource();
                }
            } else if (context.isTypeElementValue() || context.isTypeElementObject() || context.isTypeElementCollection()) {
                return context.getTypeElementValue().updatedValue(prev);
            }
        }
        return null;
    }


    public void updateFromGui(GuiMappingContext context, Object newValue) {
        if (context.isTypeElementProperty()) {
            Object src = context.getParentSource();
            try {
                context.getTypeElementAsProperty().executeSet(src, newValue);
                context.updateSourceFromGui(newValue);
            } catch (Exception ex) {
                context.errorWhileUpdateSource(ex);
            }
        } else if (context.isParentPropertyPane()) {
            context.getParentPropertyPane().updateFromGuiChild(context, newValue);
        } else if (context.isTypeElementValue() || context.isTypeElementObject() || context.isTypeElementCollection()) {
            Object prev = context.getSource();
            Object next = context.getTypeElementValue().writeValue(prev, newValue);
            context.updateSourceFromGui(next);
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
}
