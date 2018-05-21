package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingView;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;

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
                    col.addColumn(subContext, column);
                }
                //TODO Object -> GuiSwingTableColumnSet
            }
        }
        return col;
    }

    public static class ObjectTableColumnDynamicCollection implements ObjectTableColumnDynamicFactory {
        protected GuiMappingContext context;
        protected SpecifierManagerIndex rowSpecifierIndex;
        protected SpecifierManagerIndex columnSpecifierIndex;
        protected GuiSwingView.SpecifierManager elementSpecifier;

        protected List<ContextAndColumn> columnStatic = new ArrayList<>();

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

        public void addColumn(GuiMappingContext context, GuiSwingTableColumn column) {
            columnStatic.add(new ContextAndColumn(context, column));
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
            int elemIndex = columnIndex.getIndex() / columnStatic.size(); //TODO OK?
            columnSpecifierIndex.setIndex(elemIndex);
            ContextAndColumn cc = columnStatic.get(col);
            return new ObjectTableColumnCollectionWrapper(cc.column.createColumn(
                    cc.context, columnSpecifierIndex, columnSpecifierIndex), //TODO the specifier would be changed for Object
                    context.getChildren().get(0), //elementContext
                    elemIndex,
                    columnSpecifierIndex);
        }
    }

    public static class ContextAndColumn {
        public GuiMappingContext context;
        public GuiSwingTableColumn column;

        public ContextAndColumn(GuiMappingContext context, GuiSwingTableColumn column) {
            this.context = context;
            this.column = column;
        }
    }

    public static class ObjectTableColumnCollectionWrapper extends ObjectTableColumn {
        protected ObjectTableColumn column;
        protected GuiMappingContext elementContext;
        protected int elementIndex;
        protected SpecifierManagerIndex elementSpecifier;
        //TODO convert the returned column to a wrapper that can extract column value from a column list and supply to the returned wrapped column

        public ObjectTableColumnCollectionWrapper(ObjectTableColumn column,
                                                  GuiMappingContext elementContext, int elementIndex,
                                                  SpecifierManagerIndex elementSpecifier) {
            this.column = column;
            this.elementContext = elementContext;
            this.elementIndex = elementIndex;
            this.elementSpecifier = elementSpecifier;
        }

        @Override
        public Object getCellValue(Object rowObject, int rowIndex, int columnIndex) {
            try {
                GuiReprValue.ObjectSpecifier specifier = elementSpecifier.getSpecifierWithSettingIndex(elementIndex);
                Object colValue = elementContext.getReprValue()
                        .getValueWithoutNoUpdate(elementContext, GuiMappingContext.GuiSourceValue.of(rowObject), specifier);
                return column.getCellValue(colValue, rowIndex, columnIndex);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Future<?> setCellValue(Object rowObject, int rowIndex, int columnIndex, Object newColumnValue) {
            //TODO how to set?
            return column.setCellValue(rowObject, rowIndex, columnIndex, newColumnValue);
        }

        @Override
        public void setTableColumn(TableColumn tableColumn) {
            column.setTableColumn(tableColumn);
        }

        @Override
        public int getRowHeight() {
            return column.getRowHeight();
        }

        @Override
        public void setRowHeight(int rowHeight) {
            column.setRowHeight(rowHeight);
        }

        @Override
        public Comparator<?> getComparator() {
            return column.getComparator();
        }

        @Override
        public void setComparator(Comparator<?> comparator) {
            column.setComparator(comparator);
        }

        @Override
        public void shutdown() {
            column.shutdown();
        }

        @Override
        public TableColumn getTableColumn() {
            return column.getTableColumn();
        }

        @Override
        public List<TableMenuComposite> getCompositesForRows() {
            return column.getCompositesForRows();
        }

        @Override
        public List<TableMenuComposite> getCompositesForCells() {
            return column.getCompositesForCells();
        }
    }
}
