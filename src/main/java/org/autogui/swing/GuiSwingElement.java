package org.autogui.swing;

import java.util.List;

/**
 * the super interface for GUI factories, managed by {@link GuiSwingMapperSet}
 * */
public interface GuiSwingElement {
    interface FeatureCollection {
        List<?> getSwingViewSubNodes();
        default List<?> getSwingViewSubNodesSameDisplay() {
            return getSwingViewSubNodes();
        }
    }
}
