package autogui.base.type;

import autogui.GuiIncluded;
import autogui.GuiListSelectionCallback;
import autogui.GuiListSelectionUpdater;

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
    protected GuiTypeElement returnType;

    public GuiTypeMemberAction(String name, GuiTypeElement returnType, String methodName) {
        super(name);
        this.returnType = returnType;
        this.methodName = methodName;
    }

    public GuiTypeMemberAction(String name, GuiTypeElement returnType, Method method) {
        this(name, returnType, method.getName());
        this.method = method;
    }

    public Method getMethod() {
        if (method == null) {
            GuiTypeBuilder b = new GuiTypeBuilder();
            method = findOwnerMethod(methodName, b::isActionMethod);
        }
        return method;
    }

    public GuiTypeElement getReturnType() {
        return returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    /**
     * executes the action method with a target by reflection
     * @param target the target of the action
     * @return the return value of the execution
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

    public boolean isSelectionIndexAction() {
        Method method = getMethod();
        if (method != null) {
            return method.isAnnotationPresent(GuiListSelectionCallback.class) &&
                    method.getAnnotation(GuiListSelectionCallback.class).index();
        } else {
            return false;
        }
    }

    public boolean isSelectionChangeAction() {
        Method method = getMethod();
        if (method != null) {
            return method.isAnnotationPresent(GuiListSelectionUpdater.class);
        } else {
            return false;
        }
    }

    public boolean isSelectionChangeIndexAction() {
        Method method = getMethod();
        if (method != null) {
            return method.isAnnotationPresent(GuiListSelectionUpdater.class) &&
                    method.getAnnotation(GuiListSelectionUpdater.class).index();
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "action(" + name + "," + returnType  +")";
    }

    @Override
    public String getDescription() {
        Method method = getMethod();
        if (method != null) {
            GuiIncluded included = method.getAnnotation(GuiIncluded.class);
            if (included != null) {
                return included.description();
            }
        }
        return "";
    }

    @Override
    public String getAcceleratorKeyStroke() {
        Method method = getMethod();
        if (method != null) {
            GuiIncluded included = method.getAnnotation(GuiIncluded.class);
            if (included != null) {
                return included.keyStroke();
            }
        }
        return "";
    }
}
