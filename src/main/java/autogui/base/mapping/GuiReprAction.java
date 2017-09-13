package autogui.base.mapping;

public class GuiReprAction implements GuiRepresentation {
    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isSourceAction()) {
            context.setRepresentation(this);
            return true;
        } else {
            return false;
        }
    }
}
