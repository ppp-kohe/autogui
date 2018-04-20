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
        return new ObjectTableColumnDynamicCollection(context,
                new GuiSwingViewCollectionTable.SpecifierManagerIndex(rowSpecifier));
    }

    public static class ObjectTableColumnDynamicCollection implements ObjectTableColumnDynamicFactory {
        protected GuiMappingContext context;
        protected GuiSwingViewCollectionTable.SpecifierManagerIndex columnSpecifiedManager;
        protected GuiSwingTableColumn elementColumn;

        public ObjectTableColumnDynamicCollection(GuiMappingContext context,
                                                  GuiSwingViewCollectionTable.SpecifierManagerIndex columnSpecifiedManager) {
            this.context = context;
            this.columnSpecifiedManager = columnSpecifiedManager;
        }

        @Override
        public int getColumnCount(Object collection) {
            if (collection instanceof List<?>) {
                int columns = 0;
                for (Object e : (List<?>) collection) {
                    if (e instanceof List<?>) {
                        columns = Math.max(columns, ((List<?>) e).size());
                    }
                }
                return columns;
            } else {
                return 0;
            }
        }

        @Override
        public ObjectTableColumn createColumn(ObjectColumnIndex columnIndex) {
            GuiReprValue.ObjectSpecifier specifier = new GuiReprValue.ObjectSpecifierIndex(
                    columnSpecifiedManager.getTableSpecifier(),
                    columnIndex.getIndex());
            return elementColumn.createColumn(context, () -> specifier);
        }
    }
}
