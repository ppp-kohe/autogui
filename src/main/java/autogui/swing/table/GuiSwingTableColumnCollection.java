package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingView;

import java.util.List;

public class GuiSwingTableColumnCollection implements GuiSwingTableColumn {

    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          GuiSwingView.SpecifierManager specifierManager) {
        return null;
    }

    @Override
    public ObjectTableColumnDynamicFactory createColumnDynamic(GuiMappingContext context, SpecifierManagerIndex rowSpecifier) {
        return new ObjectTableColumnDynamicCollection(context, rowSpecifier);
    }

    public static class ObjectTableColumnDynamicCollection implements ObjectTableColumnDynamicFactory {
        protected GuiMappingContext context;
        protected SpecifierManagerIndex rowSpecifierIndex;
        protected GuiSwingTableColumn elementColumn;

        public ObjectTableColumnDynamicCollection(GuiMappingContext context,
                                                  SpecifierManagerIndex rowSpecifierIndex) {
            this.context = context;
            this.rowSpecifierIndex = rowSpecifierIndex;
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
            return elementColumn.createColumn(context, rowSpecifierIndex,
                    new SpecifierManagerIndex(
                            rowSpecifierIndex::getSpecifier,
                            columnIndex.getIndex()));
        }
    }
}
