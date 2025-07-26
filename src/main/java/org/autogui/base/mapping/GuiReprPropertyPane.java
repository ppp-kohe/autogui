package org.autogui.base.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** a property member definition: [propertyName: [  propertyValueField  ] ].
 * <p>
 * the representation matches any context which has a property-member-type.
 *  However, value representations (string, file, enum, boolean and number) directly support the property-member-type and
 *     precedes this representation.
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

    @Override
    public Object toSource(Object o) {
        return ((GuiReprValue) subRepresentations).toSource(o);
    }

    @Override
    public Object fromSource(Object o) {
        return ((GuiReprValue) subRepresentations).fromSource(o);
    }

    public GuiReprPropertyPane createForContext(GuiMappingContext context) {
        return new GuiReprPropertyPane();
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
        if (source instanceof NamedValue named) {
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
     *               which might be a {@link GuiReprValue.NamedValue} or
     *                the value of the property.
     *                Note: the target is not a property owner object.
     * @param json a {@link Map} json
     * @return the property value or a {@link GuiReprValue.NamedValue} if the target is also the one.
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
    public TreeString toHumanReadableStringTree(GuiMappingContext context, Object source) {
        List<TreeString> list = new ArrayList<>(1);
        BiConsumer<GuiMappingContext, Object> adder = GuiReprObjectPane.getAddingHumanReadableStringToList(list);
        for (GuiMappingContext child : context.getChildren()) {
            adder.accept(child, source);
        }
        return new TreeStringComposite(list, false);
    }

    @Override
    public Object fromHumanReadableString(GuiMappingContext context, String str) {
        String[] cols = str.split("\\t", -1);
        int i = 0;
        Object result = null;
        List<GuiMappingContext> cs = context.getChildren();
        for (String s : cols) {
            if (i < cs.size()) {
                GuiMappingContext sub = cs.get(i);
                result = sub.getRepresentation().fromHumanReadableString(sub, s); //last one
            }
            ++i;
        }
        return result;
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
