package org.autogui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * indicates that the method is automatically called when some cells of a related table are selected
 * <pre>
 *     &#64;GuiIncluded public class Table {
 *         &#64;GuiIncluded public List&lt;E&gt; list;
 *         ...
 *         &#64;GuiListSelectionCallback &#64;GuiIncluded
 *         public void select(List&lt;E&gt; itemsInList) {
 *             ...
 *         }
 *         &#64;GuiListSelectionCallback(index=true) &#64;GuiIncluded
 *         public void selectRowIndices(List&lt;Integer&gt; rows) {
 *             ...
 *         }
 *         &#64;GuiListSelectionCallback(index=true) &#64;GuiIncluded
 *         public void selectRowAndColumnIndices(List&lt;int[]&gt; rowAndColumn) {
 *             ...
 *         }
 *
 *         &#64;GuiIncluded public class E {
 *              ...
 *              &#64;GuiListSelectionCallback &#64;GuiIncluded
 *              public void select() {
 *                  ...
 *              }
 *         }
 *     }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface GuiListSelectionCallback {
    /**
     * determines the type of parameter list.
     * @return if true, the target method must take
     *   a list of row-indices (List&lt;Integer&gt;) or
     *   a list of  a list of row-and-column-indices (List&lt;int[]&gt;).
     *   if false, the target method must take a list of table elements
     *     (List&lt;E&gt; and E will be used for matching the table property).
     */
    boolean index() default false;
}
