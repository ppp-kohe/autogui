package autogui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * indicates that the method returns a list of elements or indexes of elements
 *    which become selected elements of a target list after the execution of the method.
 * <pre>
 *   &#64;GuiIncluded public class Table {
 *         &#64;GuiIncluded public List&lt;E&gt; list;
 *         ...
 *         &#64;GuiListSelectionUpdater &#64;GuiIncluded
 *         public List&lt;E&gt; select() {
 *             ...
 *         }
 *         &#64;GuiListSelectionUpdater(index = true) &#64;GuiIncluded
 *         public List&lt;Integer&gt; selectRowIndexes() {
 *             ...
 *         }
 *         &#64;GuiListSelectionUpdater(index = true) &#64;GuiIncluded
 *         public List&lt;int[]&gt; selectRowAndColumnsIndexes() {
 *             ...
 *         }
 *
 *
 *         &#64;GuiIncluded public class E {
 *              ...
 *              &#64;GuiListSelectionUpdater &#64;GuiIncluded
 *              public List&lt;E&gt; select() {
 *                  ...
 *              }
 *         }
 *   }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface GuiListSelectionUpdater {
    boolean index() default false;
}
