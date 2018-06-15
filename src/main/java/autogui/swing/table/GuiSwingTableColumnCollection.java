package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingView;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

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
        /*
        tableContext: GuiReprCollectionTable  -> GuiSwingTableCollection
             //rowSpecifier = table.getRowSpecifier(); ... columnSet.createColumns(elementContext, rowSpecifier, rowSpecifier)
            elementContext: GuiReprCollectionElement(GuiReprCollectionTable) -> GuiSwingTableColumnSetDefault
                 //subManager = new SpecifierManagerDefault(rowSpecifier...); ... column.createColumnDynamic(subContext, rowSpecifier, subManager)
                  //so, subManager is the specifier for the collection element wrapping a table
                context: GuiReprCollectionTable   -> GuiSwingTableColumnCollection
                     //tableSpecifier = ...Default(subManager...)
                      //so the tableSpecifier is the specifier for the table
                    elementContext: GuiReprCollectionElement(GuiReprValueStringField)
                        //ObjectTableColumnDynamicCollection: columnSpecifierIndex = ...Index(tableSpecifier...)
                       subContext : GuiReprValueStringField
                         //cc.column.createColumn(cc.context, ..., rowSpecifier:null, parentSpecifier:columnSpecifierIndex)
         */
        GuiSwingView.SpecifierManagerDefault tableSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);
        ObjectTableColumnDynamicCollection col = new ObjectTableColumnDynamicCollection(context, rowSpecifier, tableSpecifier);
        SpecifierManagerIndex elementSpecifier = col.getColumnSpecifierIndex();

        for (GuiMappingContext elementCollection : context.getChildren()) {
            GuiSwingElement elemView = columnMapperSet.viewTableColumn(elementCollection);
            if (elemView instanceof GuiSwingTableColumnSet) {
                ((GuiSwingTableColumnSet) elemView).createColumnsForDynamicCollection(elementCollection,
                        col, elementSpecifier);
            } else {
                for (GuiMappingContext subContext : elementCollection.getChildren()) {
                    GuiSwingElement subView = columnMapperSet.viewTableColumn(subContext);
                    if (subView instanceof GuiSwingTableColumn) {
                        GuiSwingTableColumn column = (GuiSwingTableColumn) subView;
                        col.addColumn(subContext, column, elementSpecifier);
                    }
                }
            }
        }
        return col;
    }

    public static class ObjectTableColumnDynamicCollection implements ObjectTableColumnDynamicFactory {
        protected GuiMappingContext context;
        protected SpecifierManagerIndex rowSpecifierIndex;
        protected GuiSwingView.SpecifierManager tableSpecifier;
        protected SpecifierManagerIndex columnSpecifierIndex;

        protected List<ContextAndColumn> columnStatic = new ArrayList<>();

        public ObjectTableColumnDynamicCollection(GuiMappingContext context,
                                                  SpecifierManagerIndex rowSpecifierIndex,
                                                  GuiSwingView.SpecifierManager tableSpecifier) {
            this.context = context;
            this.rowSpecifierIndex = rowSpecifierIndex;
            this.tableSpecifier = tableSpecifier;
            this.columnSpecifierIndex = new SpecifierManagerIndex(tableSpecifier::getSpecifier);
        }

        public GuiSwingView.SpecifierManager getTableSpecifier() {
            return tableSpecifier;
        }

        public SpecifierManagerIndex getColumnSpecifierIndex() {
            return columnSpecifierIndex;
        }

        public void addColumn(GuiMappingContext context, GuiSwingTableColumn column,
                              GuiSwingView.SpecifierManager parentSpecifier) {
            columnStatic.add(new ContextAndColumn(context, column, parentSpecifier));
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
                return columns * columnStatic.size();
            } else {
                return 0;
            }
        }

        @Override
        public ObjectTableColumn createColumn(ObjectTableColumnIndex columnIndex) {
            int propIndex = columnIndex.getIndex() % columnStatic.size();
            int elemIndex = columnIndex.getIndex() / columnStatic.size(); //TODO OK?

            int[] indexes = toIndexes(columnIndex, elemIndex, propIndex);

            columnSpecifierIndex.setIndex(elemIndex);
            ContextAndColumn cc = columnStatic.get(propIndex);
            return new ObjectTableColumnCollectionWrapper(cc.context,
                    cc.column.createColumn(cc.context, null, cc.parentSpecifier),
                    context.getChildren().get(0), //elementCon text
                    elemIndex, propIndex, indexes,
                    columnSpecifierIndex);
        }

        public int[] toIndexes(ObjectTableColumnIndex columnIndex, int elemIndex, int propIndex) {
            int[] indexes = columnIndex.toIndexes();
            int[] indexesWithElem = Arrays.copyOf(indexes, indexes.length + 1);
            indexesWithElem[indexes.length - 1] = elemIndex;
            indexesWithElem[indexes.length] = propIndex;
            return indexesWithElem;
        }
    }

    public static class ContextAndColumn {
        public GuiMappingContext context;
        public GuiSwingTableColumn column;
        public GuiSwingView.SpecifierManager parentSpecifier;

        public ContextAndColumn(GuiMappingContext context, GuiSwingTableColumn column,
                                GuiSwingView.SpecifierManager parentSpecifier) {
            this.context = context;
            this.column = column;
            this.parentSpecifier = parentSpecifier;
        }
    }

    public static class ObjectTableColumnCollectionWrapper extends ObjectTableColumn
        implements ObjectTableColumnWithContext {
        protected GuiMappingContext context;
        protected ObjectTableColumn column;
        protected GuiMappingContext elementContext;
        protected int elementIndex;
        protected int propertyIndex;
        /**
         * suppose {(parentIndexes, ...,) elementIndex, propertyIndex}
         */
        protected int[] indexes;
        protected SpecifierManagerIndex elementSpecifier;

        public ObjectTableColumnCollectionWrapper(GuiMappingContext context,
                                                  ObjectTableColumn column,
                                                  GuiMappingContext elementContext, int elementIndex, int propertyIndex,
                                                  int[] indexes, SpecifierManagerIndex elementSpecifier) {
            this.context = context;
            this.column = column;
            this.elementContext = elementContext;
            this.elementIndex = elementIndex;
            this.propertyIndex = propertyIndex;
            this.indexes = indexes;
            this.elementSpecifier = elementSpecifier;
            if (column != null) {
                column.withHeaderValue(column.getTableColumn().getHeaderValue() + " [" + elementIndex + "]");
            }
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }

        @Override
        public void setColumnViewUpdater(Consumer<ObjectTableColumn> updater) {
            column.setColumnViewUpdater(o -> updater.accept(this)); //convert column to this wrapper
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
            try {
                GuiReprValue.ObjectSpecifier specifier = elementSpecifier.getSpecifierWithSettingIndex(elementIndex);
                Object colValue = elementContext.getReprValue()
                        .getValueWithoutNoUpdate(elementContext, GuiMappingContext.GuiSourceValue.of(rowObject), specifier);
                column.setCellValue(colValue, rowIndex, columnIndex, newColumnValue);

                return null;
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public int[] columnIndexToValueIndex(int columnIndex) {
            return indexes;
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

        @Override
        public void viewUpdateAsDynamic(ObjectTableColumn source) {
            column.viewUpdateAsDynamic(source);
        }
    }
}
