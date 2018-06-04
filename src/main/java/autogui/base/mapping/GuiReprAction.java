package autogui.base.mapping;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * The abstract action component for an action
 */
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

    /** the action executor relying on {@link GuiMappingContext#execute(Callable)}. the target is obtained from the parent context
     * @param context  the context
     * */
    public void executeAction(GuiMappingContext context) {
        try {
            Object target = context.getParentValuePane().getUpdatedValueWithoutNoUpdate(context.getParent(), GuiReprValue.NONE_WITH_CACHE);
            context.execute(() -> context.getTypeElementAsAction().execute(target));
            context.updateSourceFromRoot();
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
    }

    /** the action executor relying on {@link GuiMappingContext#execute(Callable)}.
     * @param context the context
     * @param targets  the targets
     * */
    public void executeActionForTargets(GuiMappingContext context, List<?> targets) {
        try {
            for (Object target : targets) {
                context.execute(() -> context.getTypeElementAsAction().execute(target));
            }
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

    @Override
    public String toString() {
        return toStringHeader();
    }
}
