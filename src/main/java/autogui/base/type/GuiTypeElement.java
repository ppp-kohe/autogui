package autogui.base.type;

import java.util.Collections;
import java.util.List;

/**
 * the super-type for describing information of a type, a property, or an action.
 */
public interface GuiTypeElement {
    String getName();

    /** @return sub-elements of the type tree */
    default List<GuiTypeElement> getChildren() {
        return Collections.emptyList();
    }
}
