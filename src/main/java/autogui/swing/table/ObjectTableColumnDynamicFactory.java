package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;

import java.util.List;

public interface ObjectTableColumnDynamicFactory {
    int getColumnCount();
    ObjectTableColumn createColumn(ObjectColumnIndex columnIndex);
}
