package org.autogui.base.type;

import java.lang.reflect.Method;
import java.util.List;

/**
 * an action taking a list: processing the selected items in a table
 * <code>
 *  public R m(List&lt;E&gt; selectedItems) { ... }
 *  </code>.
 *  the type of the argument is List, and its super types except for Object (Collection and Iterable).
 *  no children.
 */
public class GuiTypeMemberActionList extends GuiTypeMemberAction {
    protected GuiTypeElement elementType;
    protected boolean takingTargetName;

    public GuiTypeMemberActionList(String name, GuiTypeElement returnType, GuiTypeElement elementType, String methodName,
                                   boolean takingTargetName) {
        super(name, returnType, methodName);
        this.elementType = elementType;
        this.takingTargetName = takingTargetName;
    }

    public GuiTypeMemberActionList(String name, GuiTypeElement returnType, GuiTypeElement elementType, Method method,
                                   boolean takingTargetName) {
        super(name, returnType, method);
        this.elementType = elementType;
        this.takingTargetName = takingTargetName;
    }

    public GuiTypeElement getElementType() {
        return elementType;
    }

    @Override
    public Method getMethod() {
        if (method == null) {
            GuiTypeBuilder b = new GuiTypeBuilder();
            method = findOwnerMethod(methodName, b::isActionListMethod);
        }
        return method;
    }

    public boolean isTakingTargetName() {
        return takingTargetName;
    }

    /**
     * execute the action method with items
     * @param target the target of the action
     * @param selectedItems the argument for the action
     * @return the returned value of the action
     * @throws Exception thrown in the action
     */
    public Object execute(Object target, List<?> selectedItems) throws Exception {
        Method method = getMethod();
        if (method != null) {
            return method.invoke(target, selectedItems);
        } else {
            throw new UnsupportedOperationException("no method: " + methodName);
        }
    }

    /**
     * execute the action method with items and a name
     * @param target the target of the action
     * @param selectedItems the argument for the action
     * @param targetName the name of target list
     * @return the returned value of the action
     * @throws Exception thrown in the action
     */
    public Object execute(Object target, List<?> selectedItems, String targetName) throws Exception {
        Method method = getMethod();
        if (method != null) {
            return method.invoke(target, selectedItems, targetName);
        } else {
            throw new UnsupportedOperationException("no method: " + methodName);
        }
    }

    @Override
    public String toString() {
        return "actionList(" + name + ", " + returnType + "," + elementType + ")";
    }
}
