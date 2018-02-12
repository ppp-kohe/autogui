package autogui.base.mapping;

import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

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
        BiConsumer<GuiMappingContext, Object> processor = (s, nextValue) -> {
            Object subObj = s.getRepresentation().toJsonWithNamed(s, nextValue);
            if (subObj != null) {
                map.put(s.getName(), subObj);
            }
        };
        boolean collection = false;
        for (GuiMappingContext subContext : context.getChildren()) {
            if (subContext.isTypeElementProperty()) {
                runSubPropertyValue(subContext, source, processor);
            } else if (subContext.isTypeElementCollection()) {
                runSubCollectionValue(subContext, source, processor);
                collection = true;
            }
        }
        if (map.size() == 1 && collection) { //collection: not object{property}, but property{collection},
             // then the name of the context will be the type name of the collection element.
            //    Note s is subContext of context of the property.
            return map.values().iterator().next();
        } else {
            return map;
        }
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

    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        return toHumanReadableStringFromObject(context, source);
    }

    public static String toHumanReadableStringFromObject(GuiMappingContext context, Object source) {
        List<String> strs = new ArrayList<>(context.getChildren().size());
        BiConsumer<GuiMappingContext, Object> processor = (s, n) -> {
            if (n instanceof NamedValue) {
                n = ((NamedValue) n).value;
            }
            strs.add(s.getRepresentation().toHumanReadableString(s, n));
        };
        for (GuiMappingContext subContext : context.getChildren()) {
            if (subContext.isTypeElementProperty()) {
                runSubPropertyValue(subContext, source, processor);
            } else if (subContext.isTypeElementCollection()) {
                runSubCollectionValue(subContext, source, processor);
            }
        }
        return String.join("\t", strs);
    }

    public static void runSubPropertyValue(GuiMappingContext subContext, Object source, BiConsumer<GuiMappingContext, Object> subAndNext) {
        try {
            Object prevValue = subContext.getSource();
            Object nextValue = subContext.execute(() ->
                    subContext.getTypeElementAsProperty().executeGet(source, prevValue));
            if (nextValue != null && nextValue.equals(GuiTypeValue.NO_UPDATE)) {
                nextValue = prevValue;
            }
            subAndNext.accept(subContext, nextValue);
        } catch (Throwable ex) {
            //nothing
        }
    }

    public static void runSubCollectionValue(GuiMappingContext subContext, Object source, BiConsumer<GuiMappingContext, Object> subAndNext) {
        //Object prevValue = subContext.getSource();
        for (GuiMappingContext listElementContext : subContext.getChildren()) {
            if (listElementContext.isCollectionElement()) {
                subAndNext.accept(listElementContext, source);
            }
        }
    }
}
