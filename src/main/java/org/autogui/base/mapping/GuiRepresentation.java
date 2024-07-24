package org.autogui.base.mapping;

import java.util.Map;
import java.util.function.Supplier;

/** abstract GUI component:
 *    most methods of the interface always take  a {@link GuiMappingContext},
 *      this is because an instance of the interface might be a singleton instance.
 * */
public interface GuiRepresentation {
    /** match the representation with the typeElement of the context, and if succeeded,
     *   it sets this representation to the context, and it might create sub-contexts for recursive matches      *
     * @param context the context of the repr.
     * @return the matching result
     */
    boolean match(GuiMappingContext context);

    /**
     * {@link #match(GuiMappingContext)} and {@link #setNotifiersTree(GuiMappingContext)} as initialization process
     * @param context the context of the repr.
     * @return the matching result
     * @since 1.2
     */
    default boolean matchAndSetNotifiersAsInit(GuiMappingContext context) {
        if (context.getRepresentation() == null) {
            boolean r = match(context);
            if (r) {
                setNotifiersTree(context);
            }
            return r;
        } else {
            return false;
        }
    }

    /**
     * sets notifiers for the entire tree by {@link GuiMappingContext#setNotifiers(Object)}
     * @param context the context of the repr.
     * @since 1.2
     */
    default void setNotifiersTree(GuiMappingContext context) {
        GuiMappingContext.GuiSourceValue src = context.getSource();
        if (!src.isNone()) {
            context.setNotifiers(src.getValue());
        }
        context.getChildren().forEach(this::setNotifiersTree);
    }

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

    /** the source might be a {@link GuiReprValue.NamedValue},
     *   then it unwraps the named value, calls {@link #toJson(GuiMappingContext, Object)},
     *     and it returns Map with the name.
     *   otherwise, it simply calls toJson and returns the result.
     *
     * @param context context of the repr.
     * @param source the source of returned JSON
     * @return JSON object
     */
    default Object toJsonWithNamed(GuiMappingContext context, Object source) {
        if (source instanceof GuiReprValue.NamedValue named) {
            return named.toJson(toJson(context, named.value));
        } else {
            return toJson(context, source);
        }
    }

    /**
     *
     * @param context the context of the repr. In most cases, the type of the context will be a property.
     * @param target the target object. if it is {@link GuiReprValue.NamedValue},
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
        public GuiReprNone() {}
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
     *  it does not include swing-based representations.
     *  Instead, GuiSwingMapperSet includes all representations.
     */
    static GuiReprSet getDefaultSet() {
        GuiReprSet set = new GuiReprSet();

        set.add(createCollectionElement(set));

        set.add(createValueBooleanCheckBox(),
                createValueEnumComboBox(),
                createValueFilePathField(),
                createValueNumberSpinner(),
                createValueStringField());

        set.add(createCollectionTable(set),
                createObjectTabbedPane(set),
                createObjectPane(set),
                createPropertyPane(set),
                createAction(),
                createActionList());

        set.add(createValueLabel());

        return set;
    }

    /** @return new representation
     * @param owner the owner which can contains the returned repr itself
     * @since 1.7 */
    static GuiReprCollectionElement createCollectionElement(GuiRepresentation owner) {
        return new GuiReprCollectionElement(owner);
    }
    /** @return new representation
     * @since 1.7 */
    static GuiReprValueBooleanCheckBox createValueBooleanCheckBox() {
        return new GuiReprValueBooleanCheckBox();
    }
    /** @return new representation
     * @since 1.7 */
    static GuiReprValueEnumComboBox createValueEnumComboBox() {
        return new GuiReprValueEnumComboBox();
    }
    /** @return new representation
     * @since 1.7 */
    static GuiReprValueFilePathField createValueFilePathField() {
        return new GuiReprValueFilePathField();
    }

    /** @return new representation wihtout a number-type;
     *    {@link GuiReprValueNumberSpinner#match(GuiMappingContext)} can determine the number-type from the type of the given context.
     *   Also {@link GuiReprValueNumberSpinner#createNumberSpinner(GuiReprValueNumberSpinner.NumberType)} can re-create with concretizing the number-type.
     * @see #createValueNumberSpinner(GuiReprValueNumberSpinner.NumberType)
     * @since 1.7 */
    static GuiReprValueNumberSpinner createValueNumberSpinner() {
        return new GuiReprValueNumberSpinner();
    }

    /**
     * @param numType the number-type defined in {@link GuiReprValueNumberSpinner}, like {@link GuiReprValueNumberSpinner#INT}
     * @return new representation with the number-type
     */
    static GuiReprValueNumberSpinner createValueNumberSpinner(GuiReprValueNumberSpinner.NumberType numType) {
        return new GuiReprValueNumberSpinner(numType, null);
    }

    /** @return new representation
     * @since 1.7 */
    static GuiReprValueStringField createValueStringField() {
        return new GuiReprValueStringField();
    }

    /** @return new representation
     * @param owner the owner which can contains the returned repr itself
     * @since 1.7 */
    static GuiReprCollectionTable createCollectionTable(GuiRepresentation owner) {
        return new GuiReprCollectionTable(owner);
    }
    /** @return new representation
     * @param owner the owner which can contains the returned repr itself
     * @since 1.7 */
    static GuiReprObjectTabbedPane createObjectTabbedPane(GuiRepresentation owner) {
        return new GuiReprObjectTabbedPane(owner);
    }
    /** @return new representation
     * @param owner the owner which can contains the returned repr itself
     * @since 1.7 */
    static GuiReprObjectPane createObjectPane(GuiRepresentation owner) {
        return new GuiReprObjectPane(owner);
    }
    /** @return new representation
     * @param owner the owner which can contains the returned repr itself
     * @since 1.7 */
    static GuiReprPropertyPane createPropertyPane(GuiRepresentation owner) {
        return new GuiReprPropertyPane(owner);
    }
    /** @return new representation
     * @since 1.7 */
    static GuiReprAction createAction() {
        return new GuiReprAction();
    }
    /** @return new representation
     * @since 1.7 */
    static GuiReprActionList createActionList() {
        return new GuiReprActionList();
    }
    /** @return new representation
     * @since 1.7 */
    static GuiReprValueLabel createValueLabel() {
        return new GuiReprValueLabel();
    }

    /**
     * the repr type is abstract for any values and it will not be directly included in the default set.
     * @return new representation
     * @since 1.7
     */
    static GuiReprValue createValue() {
        return new GuiReprValue();
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

    /**
     * do shutting-down process if the target object is an AutoCloseable.
     * @param context the context of the repr.
     * @param target the target object, obtained from source value of the context
     */
    default void shutdown(GuiMappingContext context, Object target) { }
}
