package autogui.base.mapping;

import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;

import java.util.HashMap;
import java.util.Map;

public class GuiReprObjectPane extends GuiReprValue {
    protected GuiRepresentation subRepresentation;

    public GuiReprObjectPane(GuiRepresentation subRepresentation) {
        this.subRepresentation = subRepresentation;
    }

    public GuiReprObjectPane() {
        this(GuiRepresentation.NONE);
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementObject() && !context.isRecursive()) {
            context.setRepresentation(this);
            for (GuiMappingContext subContext : context.createChildCandidates()) {
                if (subRepresentation.match(subContext)) {
                    subContext.addToParent();
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return Map: { propertyName: propertyJson }
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        return toJsonFromObject(context, source);
    }

    public static Object toJsonFromObject(GuiMappingContext context, Object source) {
        Map<String, Object> map = new HashMap<>(context.getChildren().size());
        for (GuiMappingContext subContext : context.getChildren()) {
            if (subContext.isTypeElementProperty()) {
                GuiRepresentation subRepr = subContext.getRepresentation();
                try {
                    Object prevValue = subContext.getSource();
                    Object nextValue = subContext.execute(() ->
                            subContext.getTypeElementAsProperty().executeGet(source, prevValue));
                    if (nextValue != null && nextValue.equals(GuiTypeValue.NO_UPDATE)) {
                        nextValue = prevValue;
                    }
                    Object subObj = subRepr.toJsonWithNamed(subContext, nextValue);
                    if (subObj != null) {
                        map.put(subContext.getName(), subObj);
                    }
                } catch (Throwable ex) {
                    //nothing
                }
            }
        }
        return map;
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        return fromJsonToObject(context, target, json);
    }

    @SuppressWarnings("unchecked")
    public static Object fromJsonToObject(GuiMappingContext context, Object target, Object json) {
        if (json instanceof Map<?,?>) {
            Map<String,?> jsonMap = (Map<String,?>) json;
            try {
                if (target == null) {
                    Class<?> valType = ((GuiReprValue) context.getRepresentation()).getValueType(context);
                    target = valType.getConstructor().newInstance();
                }
                for (GuiMappingContext subContext : context.getChildren()) {
                    if (subContext.isTypeElementProperty()) {
                        GuiRepresentation subRepr = subContext.getRepresentation();
                        try {
                            Object jsonEntry = jsonMap.get(subContext.getName());
                            if (jsonEntry != null) {
                                Object t = target;
                                subContext.execute(() -> {
                                    GuiTypeMemberProperty prop = subContext.getTypeElementAsProperty();
                                    Object subPrev = prop.executeGet(t, null);
                                    Object subObj = subRepr.fromJson(subContext, subPrev, jsonEntry);
                                    if (subObj != null && subPrev != subObj) {
                                        prop.executeSet(t, subObj);
                                    }
                                    return null;
                                });
                            }
                        } catch (Throwable ex) {
                            //nothing
                        }
                    }
                }
            } catch (Throwable ex) {
                //nothing
            }
        }
        return target;
    }

    @Override
    public boolean isJsonSetter() {
        return true;
    }
}
