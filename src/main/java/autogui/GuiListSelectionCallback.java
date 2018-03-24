package autogui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * indicates that the method is automatically called when some cells of a related table are selected
 * <pre>
 *     &#64GuiIncluded public class Table {
 *         &#64GuiIncluded public List&lt;E&gt; list;
 *         ...
 *         &#64;GuiListSelectionCallback &#64GuiIncluded
 *         public void select(List&lt;E&gt; itemsInList) {
 *             ...
 *         }
 *     }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface GuiListSelectionCallback {
}
