package autogui.swing;

import autogui.base.mapping.GuiMappingContext;

import javax.swing.*;

public interface GuiSwingView extends GuiSwingElement {
    JComponent createView(GuiMappingContext context);

    default boolean isComponentResizable(GuiMappingContext context) {
        return false;
    }
}
