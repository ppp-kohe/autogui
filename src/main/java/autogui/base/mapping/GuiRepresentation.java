package autogui.base.mapping;

public interface GuiRepresentation {
    /** match the representation with the typeElement of the context, and if succeed,
     *   it sets this representation to the context, and it might create sub-contexts for recursive matches */
    boolean match(GuiMappingContext context);

    /** invoke the associated method and check the returned value whether it is updated or not.
     *   if updated, update the source of context.
     *   This is non-recursive operation; {@link GuiMappingContext} recursively calls this method.
     *    The source of parent is already updated by the order of the calls. */
    boolean checkAndUpdateSource(GuiMappingContext context);

    default boolean continueCheckAndUpdateSourceForChildren(GuiMappingContext context, boolean parentUpdate) {
        return true;
    }

    /** the source might be a {@link autogui.base.mapping.GuiReprValue.NamedValue},
     *   then it unwraps the named value, calls {@link #toJson(GuiMappingContext, Object)},
     *     and it returns Map with the name.
     *   otherwise, it simply calls toJson and returns the result.
     * */
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
     * @param context a context holds the representation
     * @param source  the converted object
     * @return JSON objects representing the source;
     *    {@link String}, {@link Number}, {@link Boolean}, {@link java.util.List}, {@link java.util.Map} or null
     */
    Object toJson(GuiMappingContext context, Object source);

    Object fromJson(GuiMappingContext context, Object target, Object json);

    /** true means {@link #fromJson(GuiMappingContext, Object, Object)} takes a target object and set json properties to
     *   the target. otherwise, the method will return a new value, thus the target can be null */
    default boolean isJsonSetter() {
        return true;
    }

    /** method for constructing "toString" copy operations:
     *   the returned string will be separated by tabs and new-lines */
    default String toHumanReadableString(GuiMappingContext context, Object source) {
        return "" + source;
    }

    GuiReprNone NONE = new GuiReprNone();

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
    }

    static GuiReprSet getDefaultSet() {
        GuiReprSet set = new GuiReprSet();

        set.add(new GuiReprCollectionElement(set));

        set.add(new GuiReprValueBooleanCheckBox(),
                new GuiReprValueEnumComboBox(),
                new GuiReprValueFilePathField(),
                new GuiReprValueNumberSpinner(),
                new GuiReprValueStringField());

        set.add(new GuiReprCollectionTable(set),
                new GuiReprObjectPane(set),
                new GuiReprPropertyPane(set),
                new GuiReprAction(),
                new GuiReprActionList());

        set.add(new GuiReprValueLabel());

        return set;
    }
}
