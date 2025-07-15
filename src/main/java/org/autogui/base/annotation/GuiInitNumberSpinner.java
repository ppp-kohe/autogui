package org.autogui.base.annotation;

/**
 * <pre>
 *     &#64;GuiIncluded(description="help message")
 *     &#64;GuiInits(numberSpinner = &#64;{@link GuiInitNumberSpinner}(format="#,##0.0"))
 *        //the annotation can specify the number format of the spinner; e.g. 54.321 -&gt; 54.3.
 *     public float getMyNum() { ... }
 * </pre>
 * @since 1.8
 */
public @interface GuiInitNumberSpinner {
    /**
     * @return the number format source followd by {@link java.text.DecimalFormat} or "" (default)
     */
    String format() default "";

    /**
     * @return the source of the minimum number; parsed as the target number-type with the default format, or "" (default)
     */
    String minimum() default "";

    /**
     * @return the source of the maximum number; parsed as the target number-type with the default format, or "" (default)
     */
    String maximum() default "";

    /**
     * @return the source of the step-size number; parsed as the target number-type with the default format, or "" (default)
     */
    String stepSize() default "";
}
