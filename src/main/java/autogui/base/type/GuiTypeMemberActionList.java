package autogui.base.type;

import java.lang.reflect.Method;
import java.util.List;

/**
 * <pre>
 *  public R m(List&lt;E&gt; selectedItems) { ... }
 *  </pre>
 *  the type of the argument is List, and its super types except for Object (Collection and Iterable).
 *
 *  no children.
 */
public class GuiTypeMemberActionList extends GuiTypeMemberAction {
    protected GuiTypeElement elementType;

    public GuiTypeMemberActionList(String name, GuiTypeElement elementType, String methodName) {
        super(name, methodName);
        this.elementType = elementType;
    }

    public GuiTypeMemberActionList(String name, GuiTypeElement elementType, Method method) {
        super(name, method);
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

    public Object execute(Object target, List<?> selectedItems) throws Exception {
        Method method = getMethod();
        if (method != null) {
            return method.invoke(target, selectedItems);
        } else {
            throw new UnsupportedOperationException("no method: " + methodName);
        }
    }
}
