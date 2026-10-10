package org.autogui.base.type;

import org.autogui.GuiIncluded;
import org.autogui.GuiInits;
import org.autogui.GuiListSelectionCallback;
import org.autogui.GuiListSelectionUpdater;
import org.autogui.base.annotation.GuiDefaultInits;

import java.lang.reflect.Method;
import java.util.List;

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
    /** @since 1.9 */ protected EditFeature editFeature;

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
            method = findOwnerMethod(methodName, hasEditFeature() ? b::isActionEditMethod : b::isActionMethod);
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

    /**
     * obtaining the target of selection updater
     * @return non-empty name from {@link GuiListSelectionUpdater#target()}, the suffix of the method name "select...", or null
     * @since 1.5
     */
    public String getSelectionChangeTargetName() {
        Method method = getMethod();
        if (method != null && method.isAnnotationPresent(GuiListSelectionUpdater.class)) {
            String tgt = method.getAnnotation(GuiListSelectionUpdater.class).target();
            if (tgt != null && !tgt.isEmpty()) {
                return tgt;
            }
        }
        if (methodName != null) { //"selectXyz"
            return new GuiTypeBuilder().getNameSuffix("select", methodName);
        }
        return null;
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

    /**
     * @return obtains the attached annotation to the method or default value
     * @since 1.8
     */
    public GuiInits getInits() {
        return GuiDefaultInits.get(getMethod());
    }

    /**
     * @param editFeature the edit feature
     * @since 1.9
     */
    public void setEditFeature(EditFeature editFeature) {
        this.editFeature = editFeature;
    }

    /**
     * @return if non-null value, the edit feature is enabled
     * @since 1.9
     */
    public EditFeature getEditFeature() {
        return editFeature;
    }

    public boolean hasEditFeature() {
        return editFeature != null;
    }

    /**
     * the edit-feature object for an action;
     *  containing info. about "completeN(T editor, boolean ok)"
     * @since 1.9
     */
    public static class EditFeature {
        protected String completeEditMethodName;
        protected Method completeEditMethod;

        public EditFeature() {
            this(null, null);
        }

        /**
         * @param completeEditMethod a method called after editing
         * @param completeEditMethodName the name of the completeEditMethod
         */
        public EditFeature(String completeEditMethodName, Method completeEditMethod) {
            this.completeEditMethodName = completeEditMethodName;
            this.completeEditMethod = completeEditMethod;
        }

        public EditFeature(String completeEditMethodName) {
            this(completeEditMethodName, null);
        }

        public boolean hasCompleteEditMethod() {
            return completeEditMethod != null && completeEditMethodName != null;
        }

        public boolean isCompleteEditMethodTakingOk() {
            return completeEditMethod != null && completeEditMethod.getParameterCount() == 2 && completeEditMethod.getParameterTypes()[1].equals(boolean.class);
        }

        public String completeEditMethodName() {
            return completeEditMethodName;
        }

        public Method completeEditMethod() {
            return completeEditMethod;
        }

        public void setCompleteEditMethod(Method completeEditMethod) {
            this.completeEditMethod = completeEditMethod;
        }

        public boolean hasCompleteEditMethodWithSet(GuiTypeMemberAction owner) {
            return completeEditMethodWithSet(owner) != null;
        }

        public Method completeEditMethodWithSet(GuiTypeMemberAction owner) {
            if (completeEditMethodName != null && completeEditMethod == null) {
                GuiTypeBuilder b = new GuiTypeBuilder();
                completeEditMethod = owner.findOwnerMethod(completeEditMethodName, b::isCompleteEditMethod);
            }
            return completeEditMethod;
        }
    }

    @Override
    public List<GuiTypeElement> getChildren() {
        if (getEditFeature() != null) {
            return List.of(returnType);
        } else {
            return List.of();
        }
    }

    /**
     * @return true if it {@link #hasEditFeature()} and it has the complete-edit method.
     * @since 1.9
     */
    public boolean hasCompleteEditMethod() {
        return hasEditFeature() && getEditFeature().hasCompleteEditMethodWithSet(this);
    }

    /**
     * invokes the completion method with given arguments
     * @param target the target object for the method
     * @param editor  the editor argument
     * @param ok the ok argument (or if the completion does not take the flag, ignores it)
     * @throws Exception the execution might cause an exception
     * @since 1.9
     */
    public void executeCompleteEdit(Object target, Object editor, boolean ok) throws Exception {
        if (editFeature != null && editFeature.hasCompleteEditMethodWithSet(this)) {
            if (editFeature.isCompleteEditMethodTakingOk()) {
                executeCompleteEditWithOk(target, editor, ok, editFeature);
            } else {
                executeCompleteEdit(target, editor, editFeature);
            }
        }
    }

    /**
     * invokes the completion method for the given feature with the OK flag
     * @param target the target object for the method
     * @param editor the editor argument
     * @param ok the ok argument
     * @param f the editor feature
     * @throws Exception a caused exception
     * @since 1.9
     */
    protected void executeCompleteEditWithOk(Object target, Object editor, boolean ok, EditFeature f) throws Exception {
        f.completeEditMethodWithSet(this).invoke(target, editor, ok);
    }

    /**
     *  invokes the completion method for the given feature without the OK flag
     * @param target  the target object for the method
     * @param editor the editor argument
     * @param f the editor feature
     * @throws Exception a caused exception
     * @since 1.9
     */
    protected void executeCompleteEdit(Object target, Object editor, EditFeature f) throws Exception {
        f.completeEditMethodWithSet(this).invoke(target, editor);
    }
}
