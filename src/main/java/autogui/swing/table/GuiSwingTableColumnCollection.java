package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;

import java.util.ArrayList;
import java.util.List;

public class GuiSwingTableColumnCollection implements ObjectTableColumnDynamicFactory {
    protected GuiSwingTableColumn column;
    @Override
    public int getColumnCount(GuiMappingContext context, List<?> source) {
        GuiMappingContext childContext = context.getChildren().get(0);
        ObjectTableColumnDynamicFactory child = column.createColumnDynamic(childContext);
        return source.stream()
                .map(e -> (List<?>) e)
                .mapToInt(e -> child.getColumnCount(childContext, e) * e.size())
                .max()
                .orElse(0);
    }

    @Override
    public List<ObjectTableColumn> createColumns(GuiMappingContext context, List<?> source, ObjectColumnIndex index, int size) {
        GuiMappingContext childContext = context.getChildren().get(0);
        ObjectTableColumnDynamicFactory child = column.createColumnDynamic(childContext);

        ArrayList<ObjectTableColumn> totalColumns = new ArrayList<>();

        //TODO repeat
        List<ObjectTableColumn> subColumns = child.createColumns(childContext, null, index.child(), size); //TODO source
        index = index.next(subColumns.size());

        totalColumns.addAll(subColumns);
        totalColumns.trimToSize();

        return totalColumns;
    }
}
