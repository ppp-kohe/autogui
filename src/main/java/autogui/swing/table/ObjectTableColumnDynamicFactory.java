package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;

import java.util.List;

public interface ObjectTableColumnDynamicFactory {
    int getColumnCount(GuiMappingContext context, List<?> source);
    List<ObjectTableColumn> createColumns(GuiMappingContext context, List<?> source, ObjectColumnIndex index, int size);
}
