package org.autogui.base.type;

import java.util.Collections;
import java.util.List;

/**
 * the super-type for describing information of a type, a property, or an action.
 */
public interface GuiTypeElement {
    /**
     * @return a simple name of the element
     */
    String getName();

    /** @return sub-elements of the type tree */
    default List<GuiTypeElement> getChildren() {
        return Collections.emptyList();
    }

    /**
     * @return short description customized by an annotation parameter
     */
    String getDescription();

    /**
     * @return accelerator key chair or ""
     */
    String getAcceleratorKeyStroke();
}
