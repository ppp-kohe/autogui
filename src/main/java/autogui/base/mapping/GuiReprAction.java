package autogui.base.mapping;

public class GuiReprAction implements GuiRepresentation {
    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementAction()) {
            context.setRepresentation(this);
            return true;
        } else {
            return false;
        }
    }
}
