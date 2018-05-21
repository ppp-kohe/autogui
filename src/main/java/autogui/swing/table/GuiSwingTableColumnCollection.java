package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingView;

import java.util.ArrayList;
import java.util.List;

public class GuiSwingTableColumnCollection implements GuiSwingTableColumn {
    protected GuiSwingMapperSet columnMapperSet;

    public GuiSwingTableColumnCollection(GuiSwingMapperSet columnMapperSet) {
        this.columnMapperSet = columnMapperSet;
    }

    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          GuiSwingView.SpecifierManager parentSpecifier) {
        return null;
    }

    @Override
    public ObjectTableColumnDynamicFactory createColumnDynamic(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                                               GuiSwingView.SpecifierManager parentSpecifier) {
        ObjectTableColumnDynamicCollection col = new ObjectTableColumnDynamicCollection(context, rowSpecifier,
                new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier));
        for (GuiMappingContext elementCollection : context.getChildren()) {
            for (GuiMappingContext subContext : elementCollection.getChildren()) {
                GuiSwingElement subView = columnMapperSet.viewTableColumn(subContext);
                if (subView instanceof GuiSwingTableColumn) {
                    GuiSwingTableColumn column = (GuiSwingTableColumn) subView;
                    col.addColumn(column);
                }
            }
        }
        return col;
    }

    public static class ObjectTableColumnDynamicCollection implements ObjectTableColumnDynamicFactory {
        protected GuiMappingContext context;
        protected SpecifierManagerIndex rowSpecifierIndex;
        protected SpecifierManagerIndex columnSpecifierIndex;
        protected GuiSwingView.SpecifierManager elementSpecifier;

        protected List<GuiSwingTableColumn> columnStatic = new ArrayList<>();

        public ObjectTableColumnDynamicCollection(GuiMappingContext context,
                                                  SpecifierManagerIndex rowSpecifierIndex,
                                                  GuiSwingView.SpecifierManager elementSpecifier) {
            this.context = context;
            this.rowSpecifierIndex = rowSpecifierIndex;
            this.columnSpecifierIndex = new SpecifierManagerIndex(elementSpecifier::getSpecifier);
            this.elementSpecifier = elementSpecifier;
        }

        public SpecifierManagerIndex getColumnSpecifierIndex() {
            return columnSpecifierIndex;
        }

        public void addColumn(GuiSwingTableColumn column) {
            columnStatic.add(column);
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
                return columns * columnStatic.size(); //TODO currently no dynamic
            } else {
                return 0;
            }
        }

        @Override
        public ObjectTableColumn createColumn(ObjectTableColumnIndex columnIndex) {
            int col = columnIndex.getIndex() % columnStatic.size();
            columnSpecifierIndex.setIndex(columnIndex.getIndex() / columnStatic.size());
            return columnStatic.get(col).createColumn(
                    context.getChildren().get(0) //elementContext
                            .getChildren().get(col), columnSpecifierIndex, columnSpecifierIndex);
        }
    }
}
