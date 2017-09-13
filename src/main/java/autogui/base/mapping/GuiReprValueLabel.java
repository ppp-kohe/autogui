package autogui.base.mapping;

public class GuiReprValueLabel implements GuiRepresentation {
    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isSourceProperty()) {
            context.setRepresentation(this);
            return true;
        } else {
            return false;
        }
    }
}
