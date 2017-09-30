package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewLabel;

public class GuiSwingTableColumnLabel implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        GuiSwingViewLabel.PropertyLabel view = new GuiSwingViewLabel.PropertyLabel(context);
        view.setOpaque(true);
        return new ObjectTableColumnValue(context, view);
    }
}
