package autogui.base.mapping;

import java.util.List;

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
            context.updateSourceFromRoot();
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
    }

    public void executeActionForTargets(GuiMappingContext context, List<?> targets) {
        try {
            for (Object target : targets) {
                context.getTypeElementAsAction().execute(target);
            }
            context.updateSourceFromRoot();
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
    }
}
