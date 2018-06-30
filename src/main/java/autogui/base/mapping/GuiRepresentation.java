package autogui.base.mapping;

import java.util.Map;
import java.util.function.Supplier;

/** abstract GUI component:
 *    most methods of the interface always take  a {@link GuiMappingContext},
 *      this is because an instance of the interface might be an singleton instance.
 *
 * */
public interface GuiRepresentation {
    /** match the representation with the typeElement of the context, and if succeed,
     *   it sets this representation to the context, and it might create sub-contexts for recursive matches      *
     * @param context the context of the repr.
     * @return the matching result
     */
    boolean match(GuiMappingContext context);

    /** invoke the associated method and check the returned value whether it is updated or not.
     *   if updated, set the source of context to the value.
     *   This is non-recursive operation; {@link GuiMappingContext} recursively calls this method.
     *    The source of parent is already updated by the order of the calls.
     * @param context the context of the repr.
     * @return the representation is updated or not
     */
    boolean checkAndUpdateSource(GuiMappingContext context);

    default boolean continueCheckAndUpdateSourceForChildren(GuiMappingContext context, boolean parentUpdate) {
        return true;
    }

    /** the source might be a {@link autogui.base.mapping.GuiReprValue.NamedValue},
     *   then it unwraps the named value, calls {@link #toJson(GuiMappingContext, Object)},
     *     and it returns Map with the name.
     *   otherwise, it simply calls toJson and returns the result.
     *
     * @param context context of the repr.
     * @param source the source of returned JSON
     * @return JSON object
     */
    default Object toJsonWithNamed(GuiMappingContext context, Object source) {
        if (source instanceof GuiReprValue.NamedValue) {
            GuiReprValue.NamedValue named = (GuiReprValue.NamedValue) source;
            return named.toJson(toJson(context, named.value));
        } else {
            return toJson(context, source);
        }
    }

    /**
     *
     * @param context the context of the repr. In most cases, the type of the context will be a property.
     * @param target the target object. if it is {@link autogui.base.mapping.GuiReprValue.NamedValue},
     *               then it obtains the value. nullable
     * @param json a {@link java.util.Map} with containing an entry for the name of the context
     * @return the returned value {@link #fromJson(GuiMappingContext, Object, Object)}.
     *   if the target is a NamedValue, the returned value will also be a NamedValue.
     */
    default Object fromJsonWithNamed(GuiMappingContext context, Object target, Object json) {
        if (json instanceof Map<?,?>) {
            Object entry = ((Map<?,?>) json).get(context.getName());
            boolean namedValue = false;
            if (target instanceof GuiReprValue.NamedValue) {
                target = ((GuiReprValue.NamedValue) target).value;
                namedValue = true;
            }
            Object ret = fromJson(context, target, entry);
            if (namedValue) {
                ret = new GuiReprValue.NamedValue(context.getName(), ret);
            }
            return ret;
        }
        return null;
    }

    /**
     * convert the source into JSON format
     * @param context a context holds the representation
     * @param source  the converted object
     * @return JSON objects representing the source;
     *    {@link String}, {@link Number}, {@link Boolean}, {@link java.util.List}, {@link java.util.Map} or null
     */
    Object toJson(GuiMappingContext context, Object source);

    /**
     * create a new value object from the JSON with treating the target as an old value,
     *    or update the target with the JSON contents.
     *  the behavior can be varied by each representation, confirmed by {@link #isJsonSetter()}.
     * @param context a context holds the representation
     * @param target the target object or null
     * @param json JSON objects representing the target
     *    {@link String}, {@link Number}, {@link Boolean}, {@link java.util.List}, {@link java.util.Map} or null
     * @return created object from the json, target if provided, or null.
     */
    Object fromJson(GuiMappingContext context, Object target, Object json);

    /** @return true means {@link #fromJson(GuiMappingContext, Object, Object)} takes a target object and set json properties to
     *   the target. otherwise, the method will return a new value, thus the target can be null */
    default boolean isJsonSetter() {
        return true;
    }

    /** method for constructing "toString" copy operations:
     *   the returned string will be separated by tabs and new-lines
     *
     * @param context the context of the repr.
     * @param source converted to string
     * @return a string representation of the source
     */
    default String toHumanReadableString(GuiMappingContext context, Object source) {
        return "" + source;
    }

    default Object fromHumanReadableString(GuiMappingContext context, String str) {
        throw new UnsupportedOperationException("unsupported fromHumanReadableString: " + context.getName() + " : \"" + str + "\"");
    }

    /** the empty definition of representation */
    GuiReprNone NONE = new GuiReprNone();

    /** the empty implementation of the representation */
    class GuiReprNone implements GuiRepresentation {
        @Override
        public boolean match(GuiMappingContext context) {
            return false;
        }
        @Override
        public boolean checkAndUpdateSource(GuiMappingContext context) {
            return false;
        }

        @Override
        public Object toJson(GuiMappingContext context, Object source) {
            return null;
        }

        @Override
        public Object fromJson(GuiMappingContext context, Object target, Object json) {
            return target;
        }

        @Override
        public String toString() {
            return toStringHeader();
        }
    }

    /**
     * @return the set of default representations.
     *  it does not includes swing-based representations.
     */
    static GuiReprSet getDefaultSet() {
        GuiReprSet set = new GuiReprSet();

        set.add(new GuiReprCollectionElement(set));

        set.add(new GuiReprValueBooleanCheckBox(),
                new GuiReprValueEnumComboBox(),
                new GuiReprValueFilePathField(),
                new GuiReprValueNumberSpinner(),
                new GuiReprValueStringField());

        set.add(new GuiReprCollectionTable(set),
                new GuiReprObjectTabbedPane(set),
                new GuiReprObjectPane(set),
                new GuiReprPropertyPane(set),
                new GuiReprAction(),
                new GuiReprActionList());

        set.add(new GuiReprValueLabel());

        return set;
    }

    default String toStringHeader() {
        return getClass().getSimpleName();
    }

    /**
     * @param task the tested task
     * @return true if the task can be executed under the task-runner of a context.
     *          the method can be used for the cases that the task is dispatched to a custom task-runner mechanism
     */
    default boolean isTaskRunnerUsedFor(Supplier<?> task) {
        return true;
    }
}
