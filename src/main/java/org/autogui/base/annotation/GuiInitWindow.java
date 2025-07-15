package org.autogui.base.annotation;

/**
 * initial settings for window :  determines the initial size of the window of the attached type
 * <pre>
 *     &#64;GuiIncluded
 *     &#64;GuiInits(window = &#64;{@link GuiInitWindow}(width=300, height=500))
 *     public class MyApp {  //the size of the created GUI window for the class becomes 300x500.
 *          ...
 *     }
 * </pre>
 * @since 1.8
 */
public @interface GuiInitWindow {
    /**
     * @return the width of the window; enabled when both width and {@link #height()} has valid values.
     */
    int width() default 0;
    /**
     * @return the height of the window; enabled when both {@link #width()} and height() has valid values.
     */
    int height() default 0;
}
