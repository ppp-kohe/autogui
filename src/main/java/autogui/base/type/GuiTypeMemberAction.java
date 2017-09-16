package autogui.base.type;

import java.lang.reflect.Method;

/**
 * <pre>
 *     public R m() { ... }
 * </pre>
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


    public Object execute(Object target) throws Exception {
        Method method = getMethod();
        if (method != null) {
            return method.invoke(target);
        } else {
            throw new UnsupportedOperationException("no method: " + methodName);
        }
    }
}
