package autogui.base.type;

import java.lang.reflect.Method;
import java.util.List;

/**
 * an action taking a list: processing the selected items in a table
 * <code>
 *  public R m(List&lt;E&gt; selectedItems) { ... }
 *  </code>.
 *  the type of the argument is List, and its super types except for Object (Collection and Iterable).
 *
 *  no children.
 */
public class GuiTypeMemberActionList extends GuiTypeMemberAction {
    protected GuiTypeElement elementType;

    public GuiTypeMemberActionList(String name, GuiTypeElement returnType, GuiTypeElement elementType, String methodName) {
        super(name, returnType, methodName);
        this.elementType = elementType;
    }

    public GuiTypeMemberActionList(String name, GuiTypeElement returnType, GuiTypeElement elementType, Method method) {
        super(name, returnType, method);
        this.elementType = elementType;
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

    @Override
    public String toString() {
        return "actionList(" + name + ", " + returnType + "," + elementType + ")";
    }
}
