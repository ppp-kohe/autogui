package autogui.base.mapping;

import autogui.base.type.GuiTypeValue;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * the abstract action definition for a list (an action taking a list as its argument)
 */
public class GuiReprActionList implements GuiRepresentation {
    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementActionList()) {
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

    /**
     * the action executor relying on {@link GuiMappingContext#execute(Callable)}.
     * @param context the context
     * @param selection the parameter
     */
    public void executeActionForList(GuiMappingContext context, List<?> selection) {
        try {
            Object target = context.getParentValuePane().getUpdatedValueWithoutNoUpdate(context.getParent(), true);
            context.execute(() -> context.getTypeElementAsActionList().execute(target, selection));
            context.updateSourceFromRoot();
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
    }

    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        return null;
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        return target;
    }

    public boolean isSelectionAction(GuiMappingContext context) {
        return context.getTypeElementAsAction().isSelectionAction();
    }
}
