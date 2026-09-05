package org.autogui.base.annotation;

/**
 * initial settings for string-text-field; customizing the display-lines of the filed
 * <pre>
 *     &#64;GuiIncluded
 *     public class MyApp {
 *         &#64;GuiIncluded
 *         &#64;GuiInits(stringField = &#64;{@link GuiInitStringField}(displayLines=5))
 *         public String getMyStr() { ... } //the displayed field will have 5 lines as multi-lines height.
 *     }
 * </pre>
 * @since 1.9
 */
public @interface GuiInitStringField {
    /**
     * @return the display-lines of the text-component ;  &gt; 1 means multi-lines, and 1 means the single-line.
     */
    int displayLines() default 1;
}
