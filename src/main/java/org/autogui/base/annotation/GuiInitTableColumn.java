package org.autogui.base.annotation;

import javax.swing.*;

/**
 * initial settings for any type of table columns
 * <pre>
 *     &#64;GuiIncluded
 *     public class MyApp {
 *          public List&lt;MyElem&gt; getMyTable() { ... }
 *     }
 *
 *     &#64;GuiIncluded
 *     public class MyElem {
 *
 *         &#64;GuiIncluded
 *         &#64;GuiInits(tableColumn = &#64;{@link GuiInitTableColumn}(width=300, sortOrder=SortOder.ASCENDING))
 *         public String getColumn1() { ... }  //the width of the column associated to the property becomes 300, and the column is selected as the sorting key.
 *     }
 * </pre>
 * @since 1.8
 */
public @interface GuiInitTableColumn {
    /**
     * @return if >0, specifying the column width
     */
    int width() default 0;

    /**
     * @return if not UNSORTED, sepcifying the column as a sorting-key with the specified order.
     */
    SortOrder sortOrder() default SortOrder.UNSORTED;
}
