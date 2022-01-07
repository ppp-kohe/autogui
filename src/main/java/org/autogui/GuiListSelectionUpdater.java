package org.autogui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * indicates that the method returns a list of elements or indices of elements
 *    which become selected elements of a target list after the execution of the method.
 * <pre>
 *   &#64;GuiIncluded public class Table {
 *         &#64;GuiIncluded public List&lt;E&gt; list;
 *         ...
 *         &#64;GuiListSelectionUpdater &#64;GuiIncluded
 *         public List&lt;E&gt; select() { //the "list" property becomes the target by the element type E
 *             ...
 *         }
 *         &#64;GuiListSelectionUpdater(index = true, target = "list") &#64;GuiIncluded
 *         public List&lt;Integer&gt; selectRowIndices() { //the "list" property becomes the target by the target parameter
 *             ...
 *         }
 *         &#64;GuiListSelectionUpdater(index = true, target = "list") &#64;GuiIncluded
 *         public List&lt;int[]&gt; selectRowAndColumnsIndices() { //the "list" property becomes the target by the target parameter
 *             ...
 *         }
 *
 *         &#64;GuiListSelectionUpdater(index = true) &#64;GuiIncluded
 *         public List&lt;Integer&gt; selectList() { //the "list" property becomes the target by the method name "selectList" -&gt; "list"
 *             ...
 *         }
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

    /**
     * specifying the updating target list by its name
     * @return the name of a list property or the empty string for obtaining the name from the attached method name ("selectXyz" as "xyz")
     * @since 1.5
     */
    String target() default "";
}
