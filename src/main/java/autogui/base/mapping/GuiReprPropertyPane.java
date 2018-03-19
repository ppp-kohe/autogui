package autogui.base.mapping;

import java.util.*;

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

    public boolean checkAndUpdateSourceFromChild(GuiMappingContext child) {
        Object prev = child.getSource();
        Object next = child.getParentSource();
        try {
            if (child.execute(() -> !Objects.equals(prev, next))) {
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

    public void updateFromGuiChild(GuiMappingContext child, Object newValue) {
        updateFromGui(child.getParent(), newValue);
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

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        boolean namedValue = false;
        if (target != null && target instanceof GuiReprValue.NamedValue) {
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
    public boolean isJsonSetter() {
        return true;
    }

    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        List<String> list = new ArrayList<>(1);
        GuiReprObjectPane.runSubPropertyValue(context, source,
                GuiReprObjectPane.getAddingHumanReadableStringToList(list));
        return String.join("\t", list);
    }

    @Override
    public boolean isHistoryValueStored() {
        return false;
    }

    /**
     * @return false: a property does not support history values, but the content repr. might support it.
     */
    @Override
    public boolean isHistoryValueSupported() {
        return false;
    }
}
