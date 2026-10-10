package org.autogui.swing;

import java.util.List;

/**
 * the super interface for GUI factories, managed by {@link GuiSwingMapperSet}
 * */
public interface GuiSwingElement {
    /**
     * a collection interface of non swing component
     * @since 1.9
     */
    interface FeatureCollection {
        List<?> getSwingViewSubNodes();
        default List<?> getSwingViewSubNodesSameDisplay() {
            return getSwingViewSubNodes();
        }
    }
}
