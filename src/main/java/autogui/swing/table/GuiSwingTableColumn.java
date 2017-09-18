package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingElement;

public interface GuiSwingTableColumn extends GuiSwingElement {
    ObjectTableColumn createColumn(GuiMappingContext context);
}
