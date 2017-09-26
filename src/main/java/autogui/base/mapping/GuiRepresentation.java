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
