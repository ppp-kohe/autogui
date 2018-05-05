package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingView;

import java.util.List;

public class GuiSwingTableColumnCollection implements GuiSwingTableColumn {

    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          GuiSwingView.SpecifierManager parentSpecifier) {
        return null;
    }

    @Override
    public ObjectTableColumnDynamicFactory createColumnDynamic(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                                               GuiSwingView.SpecifierManager parentSpecifier) {
        return new ObjectTableColumnDynamicCollection(context, rowSpecifier, parentSpecifier);
    }

    public static class ObjectTableColumnDynamicCollection implements ObjectTableColumnDynamicFactory {
        protected GuiMappingContext context;
        protected SpecifierManagerIndex rowSpecifierIndex;
        protected GuiSwingView.SpecifierManager parentManager;
        protected GuiSwingTableColumn elementColumn;

        public ObjectTableColumnDynamicCollection(GuiMappingContext context,
                                                  SpecifierManagerIndex rowSpecifierIndex,
                                                  GuiSwingView.SpecifierManager parentManager) {
            this.context = context;
            this.rowSpecifierIndex = rowSpecifierIndex;
            this.parentManager = parentManager;
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
            //TODO
            return elementColumn.createColumn(context, rowSpecifierIndex,
                    new SpecifierManagerIndex(
                            rowSpecifierIndex::getSpecifier,
                            columnIndex.getIndex()));
        }
    }
}
