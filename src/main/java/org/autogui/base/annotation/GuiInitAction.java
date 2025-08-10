package org.autogui.base.annotation;

/**
 * initial settings for actions: it can enables confirmation dialog before execution
 * <pre>
 *     &#64;GuiIncluded(description="help message")
 *     &#64;GuiInits(action = &#64;{@link GuiInitAction}(confirm=true))
 *        //show a confirmation dialog before running the action method
 *        //the dialog includes the message specified the above "description" of GuiIncluded
 *     public void myAction() {
 *         ...
 *     }
 * </pre>
 * @see org.autogui.GuiInits
 * @since 1.8
 */
public @interface GuiInitAction {
    /**
     * @return if true, the attached action needs to confirm before an execution.
     *   Note: the setting only work for explicit execution, not for auto-selection.
     */
    boolean confirm() default false;
}
