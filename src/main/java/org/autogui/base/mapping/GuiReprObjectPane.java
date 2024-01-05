package org.autogui.base.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

/** an object representation composing member sub-representations
 * <pre>
 *     &#64;GuiIncluded
 *     public class C {
 *         &#64;GuiIncluded public String prop;
 *
 *         String v;
 *         &#64;GuiIncluded public String getValue() { return v; };
 *         &#64;GuiIncluded public void setValue(String v) { this.v = v; };
 *
 *         &#64;GuiIncluded public void action() {...};
 *         ...
 *     }
 * </pre>
 * */
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
        if (matchWithoutSetting(context)) {
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

    protected boolean matchWithoutSetting(GuiMappingContext context) {
        return context.isTypeElementObject() &&
                !context.isRecursive();
    }

    @Override
    public boolean isHistoryValueSupported() {
        return false;
    }

    @Override
    public boolean isHistoryValueStored(Object value) {
        return false;
    }

    /**
     * use {@link #toJsonFromObject(GuiMappingContext, Object)}
     * @param context a context holds the representation
     * @param source  the converted object
     * @return Map: { propertyName: propertyJson }
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        return toJsonFromObject(context, source);
    }

    /**
     * @param context the context of the caller's repr.
     * @param source the converted object
     * @return a map object ({@link LinkedHashMap}), with entries based on the child contexts.
     *   a member is constructed with {@link GuiRepresentation#toJsonWithNamed(GuiMappingContext, Object)}.
     *   Special case: if the map has only 1 collection entry,
     *     then it will return the entry value (list) instead of the map.
     *     This is because the entry name will be the type name and useless.
     */
    public static Object toJsonFromObject(GuiMappingContext context, Object source) {
        Map<String, Object> map = new LinkedHashMap<>(context.getChildren().size());
        BiConsumer<GuiMappingContext, Object> processor = (s, nextValue) -> {
            Object subObj = unwrapPropertyMap(s, s.getRepresentation().toJsonWithNamed(s, nextValue));
            if (subObj != null) {
                map.put(s.getName(), subObj);
            }
        };
        boolean collection = false;
        for (GuiMappingContext subContext : context.getChildren()) {
            if (subContext.isReprCollectionTable()) {
                runSubCollectionValue(subContext, source, processor);
                collection = true;
            } else if (subContext.isReprValue()) {
                runSubPropertyValue(subContext, source, processor);
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

    @SuppressWarnings("unchecked")
    public static Object unwrapPropertyMap(GuiMappingContext s, Object subObj) {
        if (subObj instanceof Map<?,?>) {
            Map<String,?> subMap = (Map<String,?>) subObj;
            if (subMap.size() == 1 && subMap.containsKey(s.getName())) { //not a value entry, but a property
                return subMap.get(s.getName());
            }
        }
        return subObj;
    }

    /**
     * use {@link #fromJsonToObject(GuiMappingContext, Object, Object)}
     * @param context a context holds the representation
     * @param target the target object or null
     * @param json the source JSON
     * @return the target or newly created object
     */
    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        return fromJsonToObject(context, target, json);
    }

    /**
     * constructing an object or setting up the target object from the JSON.
     *  to set properties, it relies on {@link GuiMappingContext#execute(Callable)}.
     * @param context the context of the caller's repr
     * @param target the target object, may be null, and then it creates a new object
     *                by 0-args constructor of the value type of the representation (supposing {@link GuiReprValue})
     * @param json the JSON object
     * @return the target or newly created object
     */
    @SuppressWarnings("unchecked")
    public static Object fromJsonToObject(GuiMappingContext context, Object target, Object json) {
        if (json instanceof Map<?,?>) {
            Map<String,?> jsonMap = (Map<String,?>) json;
            try {
                if (target == null) {
                    try {
                        GuiReprValue repr = getReprValue(context.getRepresentation());
                        if (repr != null) {
                            target = repr.createNewValue(context);
                        }
                    } catch (Throwable ex) {
                        context.errorWhileJson(ex);
                    }
                }
                for (GuiMappingContext subContext : context.getChildren()) {
                    if (subContext.isReprValue()) {
                        try {
                            GuiReprValue reprValue = subContext.getReprValue();
                            Object jsonEntry = jsonMap;

                            boolean hasKey = true;
                            if (!reprValue.isFromJsonTakingMapWithContextNameEntry(subContext)) {
                                String key = subContext.getName();
                                hasKey = jsonMap.containsKey(key);
                                jsonEntry = jsonMap.get(key);
                            }
                            if (hasKey) {
                                Object subNewValue = reprValue.fromJson(subContext,
                                        reprValue.getValueWithoutNoUpdate(subContext,
                                                GuiMappingContext.GuiSourceValue.of(target), GuiReprValue.NONE.child(false)), jsonEntry);
                                reprValue.update(subContext, GuiMappingContext.GuiSourceValue.of(target),
                                        subNewValue, GuiReprValue.NONE.child(false));
                            }
                        } catch (Throwable ex) {
                            subContext.errorWhileJson(ex);
                        }
                    }
                }
            } catch (Throwable ex) {
                context.errorWhileJson(ex);
            }
        }
        return target;
    }

    public static GuiReprValue getReprValue(GuiRepresentation repr) {
        if (repr instanceof GuiReprCollectionElement) {
            return getReprValue(((GuiReprCollectionElement) repr).getRepresentation());
        } else if (repr instanceof GuiReprValue) {
            return (GuiReprValue) repr;
        } else {
            return null;
        }
    }

    /**
     * use {@link #toHumanReadableStringFromObject(GuiMappingContext, Object)}
     * @param context the context of the repr.
     * @param source converted to string
     * @return the representation of the source
     */
    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        return toHumanReadableStringFromObject(context, source);
    }

    /**
     * constructing a human-readable string representation of the object.
     *   the contents consist of members of the object (listed by the context).
     *    each of them is processed by {@link #runSubPropertyValue(GuiMappingContext, Object, BiConsumer)}
     *     if it is a property. For obtaining the value of the property, it relies on {@link GuiMappingContext#execute(Callable)}.
     *    if it is a collection element, then it will be processed
     *    by {@link #runSubCollectionValue(GuiMappingContext, Object, BiConsumer)}.
     * @param context the context associated with the caller's repr
     * @param source the source object
     * @return the representation of the source
     */
    public static String toHumanReadableStringFromObject(GuiMappingContext context, Object source) {
        List<String> strings = new ArrayList<>(context.getChildren().size());
        BiConsumer<GuiMappingContext, Object> processor = getAddingHumanReadableStringToList(strings);
        for (GuiMappingContext subContext : context.getChildren()) {
            if (subContext.isTypeElementCollection()) {
                runSubCollectionValue(subContext, source, processor);
            } else if (subContext.isReprValue()) {
                runSubPropertyValue(subContext, source, processor);
            }
        }
        return String.join("\t", strings);
    }

    public static BiConsumer<GuiMappingContext, Object> getAddingHumanReadableStringToList(List<String> list) {
        return (s, n) -> {
            if (n instanceof NamedValue) {
                n = ((NamedValue) n).value;
            }
            list.add(s.getRepresentation().toHumanReadableString(s, n));
        };
    }

    public static void runSubPropertyValue(GuiMappingContext subContext, Object source, BiConsumer<GuiMappingContext, Object> subAndNext) {
        try {
            subAndNext.accept(subContext, subContext.getReprValue().getValueWithoutNoUpdate(subContext,
                    GuiMappingContext.GuiSourceValue.of(source), NONE.child(false)));
        } catch (Throwable ex) {
            subContext.errorWhileJson(ex);
        }
    }

    public static void runSubCollectionValue(GuiMappingContext subContext, Object source, BiConsumer<GuiMappingContext, Object> subAndNext) {
        //Object prevValue = subContext.getSource();
        for (GuiMappingContext listElementContext : subContext.getChildren()) {
            if (listElementContext.isReprCollectionElement()) {
                subAndNext.accept(listElementContext, source);
            }
        }
    }

    @Override
    public Object fromHumanReadableString(GuiMappingContext context, String str) {
        return fromHumanReadableStringToObject(context, str);
    }

    public static Object fromHumanReadableStringToObject(GuiMappingContext context, String str) { //the current implementation is so add-hoc
        Object target;
        try {
            GuiReprValue repr = getReprValue(context.getRepresentation());
            if (repr != null) {
                target = repr.createNewValue(context);
            } else {
                throw new RuntimeException("cannot create new instance: " + context);
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }

        String[] cols = str.split("\\t", -1);

        List<GuiMappingContext> subs = context.getChildren();
        int i = 0;
        for (String col : cols) {
            if (i < subs.size()) {
                GuiMappingContext subContext = subs.get(i);
                if (subContext.isReprValue()) {
                    try {
                        GuiReprValue reprValue = subContext.getReprValue();
                        Object subNewValue = reprValue.fromHumanReadableString(subContext, col);
                        reprValue.update(subContext, GuiMappingContext.GuiSourceValue.of(target),
                                subNewValue, GuiReprValue.NONE.child(false));
                    } catch (Throwable ex) {
                        subContext.errorWhileJson(ex);
                    }
                }
            }
            ++i;
        }
        return target;
    }

    @Override
    public void shutdown(GuiMappingContext context, Object source) {
        if (context.getTypeElementValue().isAutoCloseable() && source != null) {
            try {
                ((AutoCloseable) source).close();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public String toString() {
        return toStringHeader() + "(" + subRepresentation + ")";
    }
}
