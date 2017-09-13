package autogui.base.mapping;

public interface GuiRepresentation {
    /** match the representation with the source of the context, and if succeed,
     *   it sets this representation to the context, and it might create sub-contexts for recursive matches */
    boolean match(GuiMappingContext context);

    GuiReprNone NONE = new GuiReprNone();

    class GuiReprNone implements GuiRepresentation {
        @Override
        public boolean match(GuiMappingContext context) {
            return false;
        }
    }

    static GuiReprSet getDefaultSet() {
        GuiReprSet set = new GuiReprSet();

        set.add(new GuiReprNumberSpinner(),
                new GuiReprBooleanCheckbox(),
                new GuiReprStringField());

        set.add(new GuiReprObjectPane(set),
                new GuiReprPropertyPane(set),
                new GuiReprAction(),
                new GuiReprValueLabel());

        return set;
    }
}
