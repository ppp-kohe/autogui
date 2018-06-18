package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingPreferences;
import autogui.swing.GuiSwingView;
import autogui.swing.util.SettingsWindow;

import javax.swing.table.TableColumn;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GuiSwingTableColumnCollection implements GuiSwingTableColumnDynamic {
    protected GuiSwingMapperSet columnMapperSet;

    public GuiSwingTableColumnCollection(GuiSwingMapperSet columnMapperSet) {
        this.columnMapperSet = columnMapperSet;
    }

    @Override
    public DynamicColumnFactory createColumnDynamic(GuiMappingContext context, GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
                                                    GuiSwingView.SpecifierManager parentSpecifier, boolean tableTop) {
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
                        //DynamicColumnCollection: columnSpecifierIndex = ...Index(tableSpecifier...)
                       subContext : GuiReprValueStringField
                         //cc.column.createColumn(cc.context, ..., rowSpecifier:null, parentSpecifier:columnSpecifierIndex)
         */
        GuiSwingView.SpecifierManagerDefault tableSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);

        DynamicColumnFactoryCollection col = new DynamicColumnFactoryCollection(context, rowSpecifier, tableSpecifier);
        GuiSwingTableColumn.SpecifierManagerIndex elementSpecifier = col.getElementSpecifierIndex();

        for (GuiMappingContext elementCollection : context.getChildren()) { //always a single-element context
            GuiSwingElement elemView = columnMapperSet.viewTableColumn(elementCollection);
            if (elemView instanceof GuiSwingTableColumnSet) { //and, usually element-repr machines to the column-set
                ((GuiSwingTableColumnSet) elemView).createColumnsForDynamicCollection(elementCollection,
                        col, elementSpecifier);
            } else {
                for (GuiMappingContext subContext : elementCollection.getChildren()) {
                    GuiSwingElement subView = columnMapperSet.viewTableColumn(subContext);
                    if (subView instanceof GuiSwingTableColumn) {
                        GuiSwingTableColumn column = (GuiSwingTableColumn) subView;
                        col.addColumn(subContext, column, elementSpecifier);
                    } else if (subView instanceof GuiSwingTableColumnDynamic) {
                        col.addColumnDynamic(subContext, (GuiSwingTableColumnDynamic) subView, elementSpecifier);
                    }
                }
            }
        }
        return col;
    }

    /**
     * size-factory for List&lt;E&gt;
     */
    public static class DynamicColumnFactoryList implements DynamicColumnFactory, GuiSwingTableColumnSet.DynamicColumnHost {
        protected DynamicColumnFactory elementFactory;
        protected GuiSwingTableColumn.SpecifierManagerIndex elementSpecifierIndex;

        public DynamicColumnFactoryList(GuiSwingTableColumn.SpecifierManagerIndex elementSpecifierIndex) {
            this.elementSpecifierIndex = elementSpecifierIndex;
        }

        public GuiSwingTableColumn.SpecifierManagerIndex getElementSpecifierIndex() {
            return elementSpecifierIndex;
        }

        @Override
        public ObjectTableColumnSize getColumnSize(Object c) {
            if (elementFactory instanceof ObjectTableColumnSizeConcrete) { //for List<Value>
                return getColumnSizeForConcrete(c);
            } else {
                return getColumnSizeForObjectList(c);
            }
        }

        public ObjectTableColumnSize getColumnSizeForConcrete(Object c) {
            ObjectTableColumnSize size = elementFactory.getColumnSize(null);

            int n = 0;
            if (c instanceof Collection<?>) {
                n = ((Collection) c).size();
            } else if (c != null && c.getClass().isArray()) {
                n = Array.getLength(c);
            }
            size.setSize(n);
            size.setElementSpecifierIndex(elementSpecifierIndex);
            return size;
        }

        public ObjectTableColumnSize getColumnSizeForObjectList(Object c) {
            if (c instanceof List<?>) {
                List<?> list = (List<?>) c;
                ObjectTableColumnSizeComposite elements = new ObjectTableColumnSizeComposite(new ArrayList<>(list.size()));
                for (Object e : list) {
                    elements.add(elementFactory.getColumnSize(e));
                }
                return elements;
            } else if (c != null && c.getClass().isArray()) {
                int l = Array.getLength(c);
                ObjectTableColumnSizeComposite elements = new ObjectTableColumnSizeComposite(new ArrayList<>(l));
                for (int i = 0; i < l; ++i) {
                    elements.add(elementFactory.getColumnSize(Array.get(c, i)));
                }
                return elements;
            } else {
                return new ObjectTableColumnSizeComposite(Collections.emptyList());
            }
        }

        @Override
        public void addColumn(GuiMappingContext context, GuiSwingTableColumn column,
                              GuiSwingView.SpecifierManager parentSpecifier) {
            add(new ObjectTableColumnSizeConcrete(1, context, column, parentSpecifier));
        }

        @Override
        public void addColumnDynamic(GuiMappingContext context, GuiSwingTableColumnDynamic d,
                                     GuiSwingView.SpecifierManager parentSpecifier) {

            add(d.createColumnDynamic(context, null, parentSpecifier, false));
        }

        @Override
        public void add(DynamicColumnFactory d) {
            if (elementFactory != null) {
                //something-wrong
            }
            elementFactory = d;
        }

        public DynamicColumnFactory getElementFactory() {
            return elementFactory;
        }


    }

    /**
     * size-factory for class C { T0 f0; T1 f0; ... }
     */
    public static class DynamicColumnFactoryComposite implements DynamicColumnFactory, GuiSwingTableColumnSet.DynamicColumnHost {
        protected List<DynamicColumnFactory> factories = new ArrayList<>();

        @Override
        public ObjectTableColumnSize getColumnSize(Object c) {
            return new ObjectTableColumnSizeComposite(factories.stream()
                    .map(f -> f.getColumnSize(c))
                    .collect(Collectors.toList()));
        }

        @Override
        public void addColumn(GuiMappingContext context, GuiSwingTableColumn column, GuiSwingView.SpecifierManager parentSpecifier) {
            factories.add(new ObjectTableColumnSizeConcrete(1, context, column, parentSpecifier));
        }

        @Override
        public void addColumnDynamic(GuiMappingContext context, GuiSwingTableColumnDynamic d, GuiSwingView.SpecifierManager parentSpecifier) {
            factories.add(d.createColumnDynamic(context, null, parentSpecifier, false));
        }

        @Override
        public void add(DynamicColumnFactory d) {
            factories.add(d);
        }
    }


    public static class ObjectTableColumnSizeConcrete extends ObjectTableColumnSize implements DynamicColumnFactory {
        protected GuiMappingContext context;
        protected GuiSwingTableColumn column;
        public GuiSwingView.SpecifierManager parentSpecifier;

        public ObjectTableColumnSizeConcrete(int size, GuiMappingContext context, GuiSwingTableColumn column, GuiSwingView.SpecifierManager parentSpecifier) {
            this.size = size;
            this.context = context;
            this.column = column;
            this.parentSpecifier = parentSpecifier;
        }

        @Override
        public ObjectTableColumn createColumn(ObjectTableColumnIndex index) {
            super.createColumn(index); //set elementSpecifierIndex
            /*
            ObjectTableColumnCollectionWrapper w = new ObjectTableColumnCollectionWrapper(context,
                    column.createColumn(context, null, parentSpecifier),
                    context.getChildren().get(0), //elementCon text
                    elemIndex, propIndex, indexes,
                    elementSpecifierIndex);
            w.setSize(this);
            return w;*/
            //TODO
            return null;
        }

        /**
         * @param c a value contained a list
         * @return a copy of this: this is a prototype
         */
        @Override
        public ObjectTableColumnSize getColumnSize(Object c) {
            return new ObjectTableColumnSizeConcrete(size, context, column, parentSpecifier);
        }
    }

    /**
     * size-factory for root List&lt;List&lt;T&gt;&gt;
     */
    public static class DynamicColumnFactoryCollection extends DynamicColumnFactoryList {
        protected GuiMappingContext context;
        protected GuiSwingTableColumn.SpecifierManagerIndex rowSpecifierIndex;
        protected GuiSwingView.SpecifierManager tableSpecifier;

        public DynamicColumnFactoryCollection(GuiMappingContext context,
                                       GuiSwingTableColumn.SpecifierManagerIndex rowSpecifierIndex,
                                       GuiSwingView.SpecifierManager tableSpecifier) {
            super(new GuiSwingTableColumn.SpecifierManagerIndex(tableSpecifier::getSpecifier));
            this.context = context;
            this.rowSpecifierIndex = rowSpecifierIndex;
            this.tableSpecifier = tableSpecifier;
        }

        public GuiSwingView.SpecifierManager getTableSpecifier() {
            return tableSpecifier;
        }

        @Override
        public ObjectTableColumnSize getColumnSize(Object c) {
            if (c instanceof List<?>) {
                List<?> list = (List<?>) c;
                ObjectTableColumnSizeComposite elements = new ObjectTableColumnSizeComposite(new ArrayList<>());
                for (Object e : list) {
                    elements.set(super.getColumnSize(e));
                }
                return elements;
            } else if (c != null && c.getClass().isArray()) {
                int l = Array.getLength(c);
                ObjectTableColumnSizeComposite elements = new ObjectTableColumnSizeComposite(new ArrayList<>());
                for (int i = 0; i < l; ++i) {
                    elements.set(super.getColumnSize(Array.get(c, i)));
                }
                return elements;
            } else {
                return new ObjectTableColumnSizeComposite(Collections.emptyList());
            }
        }

        /*

        public int getColumnCount(Object collection) {
            if (collection instanceof List<?>) {
                int columns = 0;
                for (Object e : (List<?>) collection) {
                    if (e instanceof List<?>) {
                        columns = Math.max(columns, ((List<?>) e).size());
                    }
                }
                return columns * columnFactories.size();
            } else {
                return 0;
            }
        }

        @Override
        public ObjectTableColumn createColumn(ObjectTableColumnIndex columnIndex) {
            int propIndex = columnIndex.getIndex() % columnFactories.size();
            int elemIndex = columnIndex.getIndex() / columnFactories.size(); //TODO OK?

            int[] indexes = toIndexes(columnIndex, elemIndex, propIndex);

            columnSpecifierIndex.setIndex(elemIndex);
            ContextAndColumn cc = columnFactories.get(propIndex);
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
        }*/
    }


    public static class ObjectTableColumnSizeCollection extends ObjectTableColumnSizeComposite {
        protected ObjectTableColumnSize parent;

        public ObjectTableColumnSizeCollection(int capacity) {
            super(new ArrayList<>(capacity));
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isComposition() {
            return false;
        }

        @Override
        public List<ObjectTableColumnSize> getChildren() {
            return children;
        }

        @Override
        public ObjectTableColumnSize getParent() {
            return parent;
        }

        //TODO @Override
        public void setParent(ObjectTableColumnSize parent) {
            this.parent = parent;
        }

        public void set(int i, ObjectTableColumnSize newSize) {
            if (i < children.size()) {
                ObjectTableColumnSize ex = children.get(i);
                int diff = newSize.size() - ex.size();

                if (ex.isComposition()) {
                    if (diff > 0) {
                        size += diff;
                    }
                    ((ObjectTableColumnSizeCollection) ex).set((ObjectTableColumnSizeCollection) newSize);
                } else {
                    if (diff > 0) {
                        size += diff;
                        children.set(i, newSize);
                    }
                }
            } else {
                size += newSize.size();
                children.add(newSize);
            }
        }

        public void set(ObjectTableColumnSizeCollection newSize) {
            //TODO
        }

        public void add(ObjectTableColumnSize subSize) {
            size += subSize.size();
            children.add(subSize);
        }

        @Override
        public ObjectTableColumn createColumn(ObjectTableColumnIndex index) {
            return null;
        }


    }
    /*
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
    }*/

    public static class ObjectTableColumnCollectionWrapper extends ObjectTableColumn
        implements GuiSwingTableColumn.ObjectTableColumnWithContext {
        protected GuiMappingContext context;
        protected ObjectTableColumn column;
        protected GuiMappingContext elementContext;
        protected int elementIndex;
        protected int propertyIndex;
        /**
         * suppose {(parentIndexes, ...,) elementIndex, propertyIndex}
         */
        protected int[] indexes;
        protected GuiSwingTableColumn.SpecifierManagerIndex elementSpecifier;

        public ObjectTableColumnCollectionWrapper(GuiMappingContext context,
                                                  ObjectTableColumn column,
                                                  GuiMappingContext elementContext, int elementIndex, int propertyIndex,
                                                  int[] indexes, GuiSwingTableColumn.SpecifierManagerIndex elementSpecifier) {
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
        public void setSettingsWindow(SettingsWindow settingWindow) {
            column.setSettingsWindow(settingWindow);
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            column.setPreferencesUpdater(updater);
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            column.saveSwingPreferences(prefs);
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs) {
            column.loadSwingPreferences(prefs);
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
