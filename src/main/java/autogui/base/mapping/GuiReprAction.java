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

    @Override
    public boolean checkAndUpdateSource(GuiMappingContext context) {
        return false;
    }

    public void executeAction(GuiMappingContext context) {
        try {
            Object target = context.getParentValuePane().getUpdatedValue(context.getParent(), true);
            context.getTypeElementAsAction().execute(target);
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
    }
}
