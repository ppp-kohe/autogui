package autogui.base.type;

import autogui.GuiListSelectionCallback;

import java.lang.reflect.Method;

/**
 * a type information for
 * <code>
 *     public R m() { ... }
 * </code>.
 * no children
 */
public class GuiTypeMemberAction extends GuiTypeMember {
    protected String methodName;
    protected Method method;
    public GuiTypeMemberAction(String name, String methodName) {
        super(name);
        this.methodName = methodName;
    }

    public GuiTypeMemberAction(String name, Method method) {
        this(name, method.getName());
        this.method = method;
    }

    public Method getMethod() {
        if (method == null) {
            GuiTypeBuilder b = new GuiTypeBuilder();
            method = findOwnerMethod(methodName, b::isActionMethod);
        }
        return method;
    }

    public String getMethodName() {
        return methodName;
    }

    /**
     * @param target the target of the action
     * @return execute the action method
     * @throws Exception the exception from the method
     * */
    public Object execute(Object target) throws Exception {
        Method method = getMethod();
        if (method != null) {
            return method.invoke(target);
        } else {
            throw new UnsupportedOperationException("no method: " + methodName);
        }
    }

    public boolean isSelectionAction() {
        Method method = getMethod();
        if (method != null) {
            return method.isAnnotationPresent(GuiListSelectionCallback.class);
        } else {
            return false;
        }
    }
}
