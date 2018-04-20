package autogui.base.mapping;

import autogui.base.type.GuiTypeElement;
import autogui.base.type.GuiTypeValue;

import java.util.*;
import java.util.function.BiConsumer;

/** a property member definition: [propertyName: [  propertyValueField  ] ]
 * */
public class GuiReprPropertyPane extends GuiReprValue {
    protected GuiRepresentation subRepresentations;

    public GuiReprPropertyPane(GuiRepresentation subRepresentations) {
        this.subRepresentations = subRepresentations;
    }

    public GuiReprPropertyPane() {
        this(GuiRepresentation.NONE);
    }

    public void setSubRepresentations(GuiRepresentation subRepresentations) {
        this.subRepresentations = subRepresentations;
    }

    public GuiRepresentation getSubRepresentations() {
        return subRepresentations;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementProperty()) {
            GuiReprPropertyPane rule = createForContext(context);
            boolean matched = false;

            for (GuiMappingContext subContext : context.createChildCandidates()) {
                if (subRepresentations.match(subContext)) {
                    //now, subContext holds a matched representation, and obtain it and use as the actual sub-repr
                    rule.setSubRepresentations(subContext.getRepresentation());
                    subContext.addToParent();
                    matched = true;
                }
            }

            if (matched) {
                context.setRepresentation(rule);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public GuiReprPropertyPane createForContext(GuiMappingContext context) {
        return new GuiReprPropertyPane();
    }

    /*
    public boolean checkAndUpdateSourceFromChild(GuiMappingContext child) {
        Object prev = child.getSource();
        Object next = child.getParentSource();
        try {
            if (child.execute(() -> !equals(child, prev, next))) {
                child.setSource(next);
                return true;
            } else {
                return false;
            }
        } catch (Throwable ex) {
            child.errorWhileUpdateSource(ex);
            return false;
        }
    }

    public boolean equals(GuiMappingContext context, Object prev, Object next) {
        if (context.isTypeElementProperty()) {
            return equalsWithType(context.getTypeElementAsProperty().getType(), prev, next);
        } else {
            return equalsWithType(context.getTypeElement(), prev, next);
        }
    }

    public boolean equalsWithType(GuiTypeElement type, Object prev, Object next) {
        if (type != null && type instanceof GuiTypeValue) {
            return ((GuiTypeValue) type).equals(prev, next);
        } else {
            return Objects.equals(prev, next);
        }
    }*/

    public void updateFromGuiChild(GuiMappingContext child, Object newValue, ObjectSpecifier specifier) {
        updateFromGui(child.getParent(), newValue, specifier);
    }

    public Object updateFromChild(GuiMappingContext child, Object parentSource, Object newValue, ObjectSpecifier specifier) {
        return update(child.getParent(), parentSource, newValue, specifier);
    }

    public boolean isEditableFromChild(GuiMappingContext context) {
        return isEditable(context.getParent());
    }

    @Override
    public Object toJsonWithNamed(GuiMappingContext context, Object source) {
        return toJson(context, source);
    }

    @Override
    public Object fromJsonWithNamed(GuiMappingContext context, Object target, Object json) {
        return fromJson(context, target, json);
    }

    /**
     * @param context a context holds the representation
     * @param source  the converted object
     * @return Map: { propertyName: propertyJson, ... }
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        if (source instanceof GuiReprValue.NamedValue) {
            GuiReprValue.NamedValue named = (GuiReprValue.NamedValue) source;
            return toJsonProperty(context, named.value);
        } else {
            return toJsonProperty(context, source);
        }
    }

    public Object toJsonProperty(GuiMappingContext context, Object source) {
        Map<String, Object> map = new HashMap<>();
        for (GuiMappingContext subContext : context.getChildren()) {
            map.put(context.getName(),
                    subContext.getRepresentation().toJson(subContext, source));
        }
        return map;
    }

    /**
     *
     * @param context the target context
     * @param target the target value,
     *               which might be a {@link autogui.base.mapping.GuiReprValue.NamedValue} or
     *                the value of the property.
     *                Note: the target is not a property owner object.
     * @param json a {@link Map} json
     * @return the property value or a {@link autogui.base.mapping.GuiReprValue.NamedValue} if the target is also the one.
     */
    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        boolean namedValue = false;
        if (target instanceof GuiReprValue.NamedValue) {
            target = ((GuiReprValue.NamedValue) target).value;
            namedValue = true;
        }
        Object ret = fromJsonProperty(context, target, json);
        if (namedValue) {
            ret = new GuiReprValue.NamedValue(context.getName(), ret);
        }
        return ret;
    }

    public Object fromJsonProperty(GuiMappingContext context, Object target, Object json) {
        if (json instanceof Map<?,?>) {
            Object entry = ((Map<?,?>) json).get(context.getName());
            Object ret = null;
            for (GuiMappingContext subContext : context.getChildren()) {
                ret = subContext.getRepresentation().fromJson(subContext, target, entry);
            }
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public boolean isFromJsonTakingMapWithContextNameEntry(GuiMappingContext context) {
        return true;
    }

    @Override
    public boolean isJsonSetter() {
        return true;
    }

    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        List<String> list = new ArrayList<>(1);
        BiConsumer<GuiMappingContext, Object> adder = GuiReprObjectPane.getAddingHumanReadableStringToList(list);
        for (GuiMappingContext child : context.getChildren()) {
            adder.accept(child, source);
        }
        return String.join("\t", list);
    }

    /**
     * @return false: a property does not support history values, but the content repr. might support it.
     */
    @Override
    public boolean isHistoryValueSupported() {
        return false;
    }

    @Override
    public String toString() {
        return toStringHeader() + "(" + subRepresentations + ")";
    }
}
