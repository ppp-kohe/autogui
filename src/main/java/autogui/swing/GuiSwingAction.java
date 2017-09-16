package autogui.swing;

import autogui.base.mapping.GuiMappingContext;

import javax.swing.*;

public interface GuiSwingAction extends GuiSwingElement {
    Action createAction(GuiMappingContext context);
}
