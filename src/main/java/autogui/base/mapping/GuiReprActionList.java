package autogui.base.mapping;

import autogui.base.type.GuiTypeCollection;
import autogui.base.type.GuiTypeCollectionArray;
import autogui.base.type.GuiTypeElement;
import autogui.base.type.GuiTypeValue;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * the abstract action definition for a list (an action taking a list as its argument)
 * <pre>
 *     &#64;GuiIncluded
 *     public class C {
 *         &#64;GuiIncluded
 *         public List&lt;String&gt; list = new ArrayList&lt;&gt;();
 *
 *         &#64;GuiIncluded
 *         void action(List&lt;String&gt; selectedItemsInList) {
 *             ...
 *         }
 *     }
 * </pre>
 * <p> actions taking List&lt;Integer&gt; will receive indices of selected rows.
 *   <pre>
 *     &#64;GuiIncluded
 *     public class C {
 *         &#64;GuiIncluded
 *         public List&lt;String&gt; list = new ArrayList&lt;&gt;();
 *
 *         &#64;GuiIncluded
 *         void action(List&lt;Integer&gt; selectedRowsInList) {
 *             ...
 *         }
 *     }
 * </pre>
 * For tables of List&lt;Integer&gt;, the action will always take indices of selected rows.
 *
 * <p>in order to handle multiple lists,
 *    the action can take the second argument for receiving the name of the target list.
 *
 *   <pre>
 *          &#64;GuiIncluded
 *          public List&lt;String&gt; listA = new ArrayList&lt;&gt;();
 *          &#64;GuiIncluded
 *          public List&lt;String&gt; listB = new ArrayList&lt;&gt;();
 *
 *          &#64;GuiIncluded
 *          void action(List&lt;String&gt; selectedItemsInList, String name) {
 *              if (name.equals("listA") {
 *                  ...
 *              } else {
 *                  ...
 *              }
 *          }
 *   </pre>
 *
 * <p> automatic selection callback: the action with {@link autogui.GuiListSelectionCallback}
 * will be automatically invoked when a user selects some items in the list.
 *    <pre>
 *        &#64;GuiListSelectionCallback
 *        &#64;GuiIncluded
 *        public void action(List&lt;String&gt; items) {
 *            ...
 *        }
 *    </pre>
 *
 * <p> list selection changer: the action with {@link autogui.GuiListSelectionUpdater}
 *    can specify the next selected items after execution of the action.
 *   <pre>
 *       &#64;GuiListSelectionUpdater
 *       &#64;GuiIncluded
 *       public List&lt;String&gt; action(List&lt;String&gt; items) {
 *              ...
 *       }
 *   </pre>
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
     * @param targetName the name of the target list
     * @param targetSpecifier the specifier for the target object
     * @return result of execution or null
     */
    public Object executeActionForList(GuiMappingContext context, List<?> selection, String targetName, GuiReprValue.ObjectSpecifier targetSpecifier) {
        Object result = null;
        try {
            Object target = context.getParentValuePane().getUpdatedValueWithoutNoUpdate(context.getParent(), GuiReprValue.NONE_WITH_CACHE);
            result = executeActionForList(context, target, selection, targetName);
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
        return result;
    }

    public Object executeActionForList(GuiMappingContext context, Object target, List<?> selection, String targetName) {
        Object result = null;
        try {
            if (context.getTypeElementAsActionList().isTakingTargetName()) {
                result = context.execute(() -> context.getTypeElementAsActionList().execute(target, selection, targetName));
            } else {
                result = context.execute(() -> context.getTypeElementAsActionList().execute(target, selection));
            }
            context.updateSourceFromRoot(context);
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
        return result;
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

    /**
     * <pre>
     *     //&#64;GuiListSelectionCallback(index=true) //for automatic selection
     *     public void select(List&lt;E&gt; list) {...}
     *     public void select(List&lt;E&gt; list, String propName) {...}
     * </pre>
     * @param context the action context
     * @param tableContext context for the target element of list
     * @return true if the action is taking a list of selected row values
     */
    public boolean isSelectionAction(GuiMappingContext context, GuiMappingContext tableContext) {
        return context.isTypeElementActionList() &&
                context.getTypeElementAsActionList().getElementType()
                .equals(tableContext.getTypeElementCollection().getElementType());
    }

    /**
     * <pre>
     *     //&#64;GuiListSelectionCallback(index=true) //for automatic selection
     *     public void select(List&lt;Integer&gt; rows) {...}
     *     public void select(List&lt;Integer&gt; rows, String propName) {...}
     * </pre>
     * @param context the action context
     * @return true if the action is automatic selection and taking a list of row indices
     */
    public boolean isSelectionRowIndicesAction(GuiMappingContext context) {
        if (context.isTypeElementActionList()) {
            GuiTypeElement elementType = context.getTypeElementAsActionList().getElementType();
            return elementType.equals(new GuiTypeValue(Integer.class));
        } else {
            return false;
        }
    }

    /**
     * <pre>
     *     //&#64;GuiListSelectionCallback(index=true) //for automatic selection
     *     public void select(List&lt;int[]&gt; rows) {...}
     *     public void select(List&lt;int[]&gt; rows, String propName) {...}
     * </pre>
     * @param context the action context
     * @return true if the action is automatic selection and taking a list of row and column pair indices
     */
    public boolean isSelectionRowAndColumnIndicesAction(GuiMappingContext context) {
        if (context.isTypeElementActionList()) {
            GuiTypeElement elementType = context.getTypeElementAsActionList().getElementType();
            return elementType.equals(getRowAndColumnIndicesType());
        } else {
            return false;
        }
    }

    public static GuiTypeElement getRowAndColumnIndicesType() {
        return new GuiTypeCollectionArray(int[].class, new GuiTypeValue(int.class));
    }

    /**
     * <pre>
     *     &#64;GuiListSelectionUpdater
     *     public Collection&lt;E&gt; select(List&lt;E&gt; items) { ... }
     * </pre>
     * @param context context of the action
     * @param tableContext table context
     * @return true if matched
     */
    public boolean isSelectionChangeAction(GuiMappingContext context, GuiMappingContext tableContext) {
        return isSelectionChangeActionForActions(context, tableContext);
    }

    /**
     * <pre>
     *     &#64;GuiListSelectionUpdater(index=true)
     *     public Collection&lt;Integer&gt; select(List&lt;Integer&gt; rows) { ... }
     * </pre>
     * @param context context of the action
     * @return true if matched
     */
    public boolean isSelectionChangeRowIndicesAction(GuiMappingContext context) {
        return isSelectionChangeRowIndicesActionForActions(context);
    }

    /**
     * <pre>
     *     &#64;GuiListSelectionUpdater(index=true)
     *     public Collection&lt;int[]&gt; select(List&lt;int[]&gt; indices) { ... }
     * </pre>
     * @param context context of the action
     * @return true if matched
     */
    public boolean isSelectionChangeRowAndColumnIndicesAction(GuiMappingContext context) {
        return isSelectionChangeRowAndColumnIndicesActionForActions(context);
    }

    /**
     * <pre>
     *     &#64;GuiListSelectionUpdater
     *     public Collection&lt;E&gt; select(...) { ... }
     * </pre>
     * @param context context of the action
     * @param tableContext table context
     * @return true if matched
     */
    public static boolean isSelectionChangeActionForActions(GuiMappingContext context, GuiMappingContext tableContext) {
        return (context.isTypeElementAction() || context.isTypeElementActionList()) &&
                context.getTypeElementAsAction().isSelectionChangeAction() &&
                isCollectionType(context.getTypeElementAsAction().getReturnType(),
                    tableContext.getTypeElementCollection().getElementType());
    }

    public static boolean isCollectionType(GuiTypeElement testedCollectionType, GuiTypeElement elementType) {
        if (testedCollectionType instanceof GuiTypeCollection) {
            return ((GuiTypeCollection) testedCollectionType).getElementType().equals(elementType);
        } else {
            return false;
        }
    }

    public static boolean isSelectionChangeRowIndicesActionForActions(GuiMappingContext context) {
        return (context.isTypeElementAction() || context.isTypeElementActionList()) &&
                context.getTypeElementAsAction().isSelectionChangeIndexAction() &&
                isCollectionType(context.getTypeElementAsAction().getReturnType(),
                        new GuiTypeValue(Integer.class));
    }


    public static boolean isSelectionChangeRowAndColumnIndicesActionForActions(GuiMappingContext context) {
        return (context.isTypeElementAction() || context.isTypeElementActionList()) &&
                context.getTypeElementAsAction().isSelectionChangeIndexAction() &&
                isCollectionType(context.getTypeElementAsAction().getReturnType(),
                        getRowAndColumnIndicesType());
    }


    @Override
    public String toString() {
        return toStringHeader();
    }
}
