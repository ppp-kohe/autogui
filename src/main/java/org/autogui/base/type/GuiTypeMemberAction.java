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
     * @since 1.9
     */
    public static class EditFeature {
        protected String editedMethodName;
        protected Method editedMethod;

        public EditFeature() {
            this(null, null);
        }

        /**
         * @param editedMethod a method called after editing
         * @param editedMethodName the name of the editedMethod
         */
        public EditFeature(String editedMethodName, Method editedMethod) {
            this.editedMethodName = editedMethodName;
            this.editedMethod = editedMethod;
        }

        public EditFeature(String editedMethodName) {
            this(editedMethodName, null);
        }

        public boolean hasEditedMethod() {
            return editedMethod != null && editedMethodName != null;
        }

        public boolean isEditedMethodTakingOk() {
            return editedMethod != null && editedMethod.getParameterCount() == 2 && editedMethod.getParameterTypes()[1].equals(boolean.class);
        }

        public String editedMethodName() {
            return editedMethodName;
        }

        public Method editedMethod() {
            return editedMethod;
        }

        public void setEditedMethod(Method editedMethod) {
            this.editedMethod = editedMethod;
        }

        public boolean hasEditedMethodWithSet(GuiTypeMemberAction owner) {
            return editedMethodWithSet(owner) != null;
        }

        public Method editedMethodWithSet(GuiTypeMemberAction owner) {
            if (editedMethodName != null && editedMethod == null) {
                GuiTypeBuilder b = new GuiTypeBuilder();
                editedMethod = owner.findOwnerMethod(editedMethodName, b::isEditedMethod);
            }
            return editedMethod;
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

    public boolean hasEditedMethod() {
        return hasEditFeature() && getEditFeature().hasEditedMethodWithSet(this);
    }

    public void executeEcited(Object target, Object editor, boolean ok) throws Exception {
        if (editFeature != null && editFeature.hasEditedMethodWithSet(this)) {
            if (editFeature.isEditedMethodTakingOk()) {
                executeEditedWithOk(target, editor, ok, editFeature);
            } else {
                executeEdited(target, editor, editFeature);
            }
        }
    }

    protected void executeEditedWithOk(Object target, Object editor, boolean ok, EditFeature f) throws Exception {
        f.editedMethodWithSet(this).invoke(target, editor, ok);
    }

    protected void executeEdited(Object target, Object editor, EditFeature f) throws Exception {
        f.editedMethodWithSet(this).invoke(target, editor);
    }
}
