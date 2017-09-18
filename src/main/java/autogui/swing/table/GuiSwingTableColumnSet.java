package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingElement;

public interface GuiSwingTableColumnSet extends GuiSwingElement {
    void createColumns(GuiMappingContext context, ObjectTableModel model);
}
