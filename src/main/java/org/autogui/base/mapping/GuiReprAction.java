package org.autogui.base.mapping;

import org.autogui.GuiListSelectionUpdater;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * The abstract action component for an action
 * <pre>
 *     &#64;GuiIncluded
 *     public class C {
 *         ...
 *         &#64;GuiIncluded
 *         public void action() {
 *             ...
 *         }
 *     }
 * </pre>
 * an action can be selection-updater by attaching {@link GuiListSelectionUpdater}
 */
public class GuiReprAction implements GuiRepresentation {
    public GuiReprAction() {}
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
     * @param targetSpecifier the specifier of the target
     * @return result of method execution or null
     * */
    public Object executeAction(GuiMappingContext context, GuiReprValue.ObjectSpecifier targetSpecifier) {
        Object result = null;
        try {
            Object target = context.getParentValuePane().getUpdatedValueWithoutNoUpdate(context.getParent(), targetSpecifier);
            result = (target == null ? null :
                    context.execute(() -> context.getTypeElementAsAction().execute(target)));
            context.updateSourceFromRoot();
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
        return result;
    }

    /** the action executor relying on {@link GuiMappingContext#execute(Callable)}.
     * @param context the context
     * @param targets  the targets
     * @return results of each executions, might include null elements.
     * if some of executions cause an exception, partially constructed results will be returned
     * */
    public List<Object> executeActionForTargets(GuiMappingContext context, List<?> targets) {
        List<Object> results = new ArrayList<>(targets.size());
        try {
            for (Object target : targets) {
                results.add(target == null ? null :
                        context.execute(() -> context.getTypeElementAsAction().execute(target)));
            }
            context.updateSourceFromRoot();
            return results;
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
            return results;
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

    /**
     * @param context the context of the action
     * @return true if the target method has the annotation GuiListSelectionCallback
     */
    public boolean isSelectionAction(GuiMappingContext context) {
        return context.getTypeElementAsAction().isSelectionAction();
    }

    @Override
    public String toString() {
        return toStringHeader();
    }

    /**
     *
     * @param context the context of the action
     * @param tableContext the table context in order to obtain the element-type of the list
     * @return the target method returns a list of the element-type List&lt;E&gt;
     *         and has the annotation GuiListSelectionUpdater
     */
    public boolean isSelectionChangeAction(GuiMappingContext context, GuiMappingContext tableContext) {
        return GuiReprActionList.isSelectionChangeActionForActions(context, tableContext);
    }

    /* //actions for an element-type do not support selection-changes with indices.
       // it cannot determine the target property;
       // a property name (like List<int[]> act(String prop)) might be useless
       //  because the element class can be used some other new classes.
       // List<Integer> and List<int[]> is intended for primitive elements
       //  which cannot have a new method and thus never contain GuiReprAction
       */

    /**
     *  checking an action can be a selection updater for a table
     * @param context the action context
     * @param tableContext the linked table-context
     * @return true if the target action returning <code>List&lt;Integer&gt;</code> with {@link GuiListSelectionUpdater#index()}=true
     *          and the parent of tableContext is target of the action (specified by {@link GuiListSelectionUpdater#target()} or the method name "select...")
     * @since 1.5
     */
    public boolean isSelectionChangeRowIndicesAction(GuiMappingContext context, GuiMappingContext tableContext) {
        return GuiReprActionList.isSelectionChangeRowIndicesActionForActions(context) &&
                GuiReprActionList.isSelectionChangeTargetForActions(context, tableContext);
    }

    /**
     *  checking an action can be a selection updater for a table
     * @param context the action context
     * @param tableContext the linked table-context
     * @return true if the target action returning <code>List&lt;int[]&gt;</code> with {@link GuiListSelectionUpdater} with <code>index=true</code>,
     *          and the parent of tableContext is target of the action (specified by {@link GuiListSelectionUpdater#target()} or the method name "select...")
     * @since 1.5
     */
    public boolean isSelectionChangeRowAndColumnIndicesAction(GuiMappingContext context, GuiMappingContext tableContext) {
        return GuiReprActionList.isSelectionChangeRowAndColumnIndicesActionForActions(context) &&
                GuiReprActionList.isSelectionChangeTargetForActions(context, tableContext);
    }
}
