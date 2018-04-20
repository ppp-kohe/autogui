package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingViewCollectionTable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GuiSwingTableColumnCollection implements GuiSwingTableColumn {

    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> rowSpecifier) {
        return null;
    }

    @Override
    public ObjectTableColumnDynamicFactory createColumnDynamic(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> rowSpecifier) {
        return null;
    }

    public static class ObjectTableColumnDynamicCollection implements ObjectTableColumnDynamicFactory {
        protected GuiMappingContext context;
        protected GuiSwingViewCollectionTable.SpecifierManagerIndex columnSpecifidManager;

        @Override
        public int getColumnCount() {

        }

        @Override
        public ObjectTableColumn createColumn(ObjectColumnIndex columnIndex) {
            return null;
        }
    }

    protected GuiMappingContext context;
    protected GuiSwingTableColumn column;

    @Override
    public int getColumnCount() {
        context.getParent().getReprValue().getUpdatedValueWithoutNoUpdate(context.getParent(), );
    }

    @Override
    public ObjectTableColumn createColumn(ObjectColumnIndex columnIndex) {
        return null;
    }

    @Override
    public int getColumnCount(List<?> source) {
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
