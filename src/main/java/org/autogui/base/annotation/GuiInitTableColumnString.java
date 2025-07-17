package org.autogui.base.annotation;

/**
 * initial settings for a column of string
 * <pre>
 *     &#64;GuiIncluded
 *     public class MyApp {
 *          public List&lt;MyElem&gt; getMyTable() { ... }
 *     }
 *
 *     &#64;GuiIncluded
 *     public class MyElem {
 *         &#64;GuiIncluded
 *         public void setColumn1(String s) { ...} // Column 1 is editable
 *
 *         &#64;GuiIncluded
 *         &#64;GuiInits(tableColumn = &#64;{@link GuiInitTableColumnString}(editFinishByEnterAndKey=true))
 *         public String getColumn1() { ... }  //To finish editing the column, users will need to type Enter+Alt or Option key,
 *                                             // and typing the Enter key will insert a new-line.
 *     }
 * </pre>
 * @since 1.8
 */
public @interface GuiInitTableColumnString {
    /**
     * @return if true, users need to type "Enter + Alt or Option key" for finishing editing in the text-field of the column,
     *        and typing the single Enter key insert a new-line.    This is useful for multiline texts in the column.
     *   Default is false; the attribute swaps the meaning, as for false, users need to type a single "Enter key" for finishing editing,
     *     and "Enter + Alt or Option key" for inserting a new-line.
     */
    boolean editFinishByEnterAndKey() default false;
}
