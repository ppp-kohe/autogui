package autogui.swing;

import autogui.base.mapping.GuiMappingContext;

import javax.swing.*;
import java.util.List;

public interface GuiSwingAction extends GuiSwingElement {
    Action createAction(GuiMappingContext context, GuiSwingView.ValuePane<?> pane,
                        List<GuiSwingViewCollectionTable.CollectionTable> tables);
}
