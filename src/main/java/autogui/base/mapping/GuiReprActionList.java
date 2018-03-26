package autogui.base.mapping;

import autogui.base.type.GuiTypeElement;
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
            context.updateSourceFromRoot(context);
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

    public boolean isAutomaticSelectionAction(GuiMappingContext context) {
        return context.getTypeElementAsAction().isSelectionAction();
    }

    public boolean isSelectionAction(GuiMappingContext context, GuiMappingContext tableContext) {
        return context.isTypeElementActionList() &&
                context.getTypeElementAsActionList().getElementType()
                .equals(tableContext.getTypeElementCollection().getElementType());
    }

    /**
     * <pre>
     *     &#64;GuiListSelectionCallback(index=true)
     *     public void select(List&lt;Integer&gt; rows) {...}
     * </pre>
     * @param context the action context
     * @return true if the action is automatic selection and taking a list of row indexes
     */
    public boolean isAutomaticSelectionRowIndexesAction(GuiMappingContext context) {
        if (context.isTypeElementActionList() &&
                context.getTypeElementAsActionList().isSelectionIndexAction()) {
            GuiTypeElement elementType = context.getTypeElementAsActionList().getElementType();
            return elementType.equals(new GuiTypeValue(Integer.class));
        } else {
            return false;
        }
    }

    /**
     * <pre>
     *     &#64;GuiListSelectionCallback(index=true)
     *     public void select(List&lt;int[]&gt; rows) {...}
     * </pre>
     * @param context the action context
     * @return true if the action is automatic selection and taking a list of row and column pair indexes
     */
    public boolean isAutomaticSelectionRowAndColumnIndexesAction(GuiMappingContext context) {
        if (context.isTypeElementActionList() &&
                context.getTypeElementAsActionList().isSelectionIndexAction()) {
            GuiTypeElement elementType = context.getTypeElementAsActionList().getElementType();
            return elementType.equals(new GuiTypeValue(int[].class));
        } else {
            return false;
        }
    }
}
