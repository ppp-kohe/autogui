package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingPreferences;
import autogui.swing.GuiSwingView;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuiSwingTableColumnCollection implements GuiSwingTableColumnDynamic {
    protected GuiSwingMapperSet columnMapperSet;

    public GuiSwingTableColumnCollection(GuiSwingMapperSet columnMapperSet) {
        this.columnMapperSet = columnMapperSet;
    }

    @Override
    public DynamicColumnFactory createColumnDynamic(GuiMappingContext context,
                                                    GuiSwingTableColumnSet.TableColumnHost model,
                                                    GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
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

        DynamicColumnFactoryList col = tableTop ?
                new DynamicColumnFactoryCollectionRoot(context, tableSpecifier, model, rowSpecifier) :
                new DynamicColumnFactoryList(context, tableSpecifier, model, rowSpecifier);
        GuiSwingTableColumn.SpecifierManagerIndex elementSpecifier = col.getIndex();

        for (GuiMappingContext elementCollection : context.getChildren()) { //always a single-element context
            GuiSwingElement elemView = columnMapperSet.viewTableColumn(elementCollection);
            if (elemView instanceof GuiSwingTableColumnSet) { //and, usually element-repr machines to the column-set
                ((GuiSwingTableColumnSet) elemView).createColumnsForDynamicCollection(elementCollection,
                        col, model, rowSpecifier, elementSpecifier);
                /*
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
                */
            }
        }
        return col;
    }

    public static class DynamicColumnFactoryBase {
        protected GuiMappingContext context;
        protected GuiSwingView.SpecifierManager specifierManager;

        protected DynamicColumnFactory parentFactory;
        protected GuiSwingTableColumn.SpecifierManagerIndex index;
        protected List<GuiSwingTableColumn.SpecifierManagerIndex> indexSet;
        protected List<GuiMappingContext> actionContexts = new ArrayList<>();

        protected GuiSwingTableColumnSet.TableColumnHost model;
        protected GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier;

        public DynamicColumnFactoryBase(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager,
                                        GuiSwingTableColumnSet.TableColumnHost model,
                                        GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier) {
            this.context = context;
            this.specifierManager = specifierManager;
            this.model = model;
            this.rowSpecifier = rowSpecifier;
        }

        public GuiMappingContext getContext() {
            return context;
        }

        public void setContext(GuiMappingContext context) {
            this.context = context;
        }

        public void setSpecifierManager(GuiSwingView.SpecifierManager specifierManager) {
            this.specifierManager = specifierManager;
        }

        public GuiSwingView.SpecifierManager getSpecifierManager() {
            return specifierManager;
        }

        public void setParentFactory(DynamicColumnFactory parent) {
            this.parentFactory = parent;
        }

        public DynamicColumnFactory getParentFactory() {
            return parentFactory;
        }

        public void setRowSpecifier(GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier) {
            this.rowSpecifier = rowSpecifier;
        }

        public GuiSwingTableColumn.SpecifierManagerIndex getRowSpecifier() {
            return rowSpecifier;
        }

        public void setModel(GuiSwingTableColumnSet.TableColumnHost model) {
            this.model = model;
        }

        public GuiSwingTableColumnSet.TableColumnHost getModel() {
            return model;
        }

        public void setIndex(GuiSwingTableColumn.SpecifierManagerIndex index) {
            this.index = index;
        }

        public GuiSwingTableColumn.SpecifierManagerIndex getIndex() {
            return index;
        }

        public void addActionContext(GuiMappingContext context) {
            actionContexts.add(context);
        }

        public List<GuiMappingContext> getActionContexts() {
            return actionContexts;
        }

        public List<GuiSwingTableColumn.SpecifierManagerIndex> getIndexSpecifiers() {
            if (indexSet == null) {
                indexSet = new ArrayList<>();
                DynamicColumnFactory p = getParentFactory();
                if (p != null) {
                    indexSet.addAll(p.getIndexSpecifiers());
                }
                addIndexToList(indexSet);
                ((ArrayList<GuiSwingTableColumn.SpecifierManagerIndex>) indexSet).trimToSize();
            }
            return indexSet;
        }

        protected void addIndexToList(List<GuiSwingTableColumn.SpecifierManagerIndex> indexSet) {
            GuiSwingTableColumn.SpecifierManagerIndex index = getIndex();
            if (index != null){
                indexSet.add(index);
            }
        }

        public List<Action> getActions(DynamicColumnFactory sender, List<DynamicColumnFactory> children,
                                       GuiReprCollectionTable.TableTargetCell selection) {
            return Stream.concat(
                        getActionContexts().stream()
                            .map(a -> toAction(a, sender, selection))
                            .filter(Objects::nonNull),
                        children.stream()
                            .flatMap(d -> d.getActions(selection).stream()))
                    .collect(Collectors.toList());
        }

        public Action toAction(GuiMappingContext context, DynamicColumnFactory sender, GuiReprCollectionTable.TableTargetCell selection) {
            if (context.isReprAction()) {
                return new GuiSwingTableColumnSetDefault.TableSelectionAction(context,
                        new TableSelectionSourceDynamic(getModel(), getRowSpecifier(), selection, sender, context.getName()));
                //TODO targetName ok?
            } else if (context.isReprActionList()) {
                return null; //TODO
            } else {
                return null;
            }
        }

        public Object getValue(Map<GuiSwingTableColumn.SpecifierManagerIndex, Integer> indexInjection) {
            try {
                indexInjection.forEach(GuiSwingTableColumn.SpecifierManagerIndex::setIndex);
                GuiMappingContext context = getContext();
                return context.getReprValue().getUpdatedValueWithoutNoUpdate(context,
                        getSpecifierManager().getSpecifier());
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * size-factory for List&lt;E&gt;
     */
    public static class DynamicColumnFactoryList extends DynamicColumnFactoryBase
            implements DynamicColumnFactory, GuiSwingTableColumnSet.DynamicColumnHost {
        protected DynamicColumnFactory elementFactory;
        public DynamicColumnFactoryList(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager,
                                        GuiSwingTableColumnSet.TableColumnHost model,
                                        GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier) {
            super(context, specifierManager, model, rowSpecifier);
            setIndex(new GuiSwingTableColumn.SpecifierManagerIndex(specifierManager::getSpecifier));
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
            size.setElementSpecifierIndex(getIndex());
            return size;
        }

        public ObjectTableColumnSize getColumnSizeForObjectList(Object c) {
            ObjectTableColumnSize size;
            if (c instanceof List<?>) {
                List<?> list = (List<?>) c;
                ObjectTableColumnSizeComposite elements = new ObjectTableColumnSizeComposite(new ArrayList<>(list.size()));
                for (Object e : list) {
                    elements.add(elementFactory.getColumnSize(e));
                }
                size = elements;
            } else if (c != null && c.getClass().isArray()) {
                int l = Array.getLength(c);
                ObjectTableColumnSizeComposite elements = new ObjectTableColumnSizeComposite(new ArrayList<>(l));
                for (int i = 0; i < l; ++i) {
                    elements.add(elementFactory.getColumnSize(Array.get(c, i)));
                }
                size = elements;
            } else {
                size = new ObjectTableColumnSizeComposite(Collections.emptyList());
            }
            size.setElementSpecifierIndex(getIndex());
            return size;
        }

        @Override
        public void addColumn(GuiMappingContext context, GuiSwingTableColumn column,
                              GuiSwingView.SpecifierManager parentSpecifier) {
            addColumnDynamic(new ObjectTableColumnSizeConcrete(1, context, column, parentSpecifier, model, rowSpecifier));
        }

        @Override
        public void addColumnDynamic(DynamicColumnFactory d) {
            if (elementFactory != null) {
                //something-wrong
            }
            elementFactory = d;
            d.setParentFactory(this);
        }

        public DynamicColumnFactory getElementFactory() {
            return elementFactory;
        }

        @Override
        public List<Action> getActions(GuiReprCollectionTable.TableTargetCell selection) {
            return getActions(this,
                    elementFactory == null ? Collections.emptyList() : Collections.singletonList(elementFactory),
                    selection);
        }
    }

    /**
     * size-factory for root List&lt;List&lt;T&gt;&gt;
     */
    public static class DynamicColumnFactoryCollectionRoot extends DynamicColumnFactoryList {
        public DynamicColumnFactoryCollectionRoot(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager,
                                                  GuiSwingTableColumnSet.TableColumnHost model,
                                                  GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier) {
            super(context, specifierManager, model, rowSpecifier);
        }

        @Override
        public ObjectTableColumnSize getColumnSize(Object c) {
            ObjectTableColumnSize size;
            if (c instanceof List<?>) {
                List<?> list = (List<?>) c;
                ObjectTableColumnSizeComposite elements = new ObjectTableColumnSizeComposite(new ArrayList<>());
                for (Object e : list) {
                    elements.set(super.getColumnSize(e));
                }
                size = elements;
            } else if (c != null && c.getClass().isArray()) {
                int l = Array.getLength(c);
                ObjectTableColumnSizeComposite elements = new ObjectTableColumnSizeComposite(new ArrayList<>());
                for (int i = 0; i < l; ++i) {
                    elements.set(super.getColumnSize(Array.get(c, i)));
                }
                size = elements;
            } else {
                size = new ObjectTableColumnSizeComposite(Collections.emptyList());
            }
            size.setElementSpecifierIndex(getIndex());
            return size;
        }
    }

    /**
     * size-factory for class C { T0 f0; T1 f0; ... }
     */
    public static class DynamicColumnFactoryComposite extends DynamicColumnFactoryBase
            implements DynamicColumnFactory, GuiSwingTableColumnSet.DynamicColumnHost {
        protected List<DynamicColumnFactory> factories = new ArrayList<>();

        public DynamicColumnFactoryComposite(GuiMappingContext context,
                                             GuiSwingView.SpecifierManager specifierManager,
                                             GuiSwingTableColumnSet.TableColumnHost model,
                                             GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier) {
            super(context, specifierManager, model, rowSpecifier);
        }

        @Override
        public ObjectTableColumnSize getColumnSize(Object c) {
            return new ObjectTableColumnSizeComposite(factories.stream()
                    .map(f -> f.getColumnSize(c))
                    .collect(Collectors.toList()));
        }

        @Override
        public void addColumn(GuiMappingContext context, GuiSwingTableColumn column, GuiSwingView.SpecifierManager parentSpecifier) {
            addColumnDynamic(new ObjectTableColumnSizeConcrete(1, context, column, parentSpecifier, model, rowSpecifier));
        }

        @Override
        public void addColumnDynamic(DynamicColumnFactory d) {
            factories.add(d);
            d.setParentFactory(this);
        }

        @Override
        public List<Action> getActions(GuiReprCollectionTable.TableTargetCell selection) {
            return getActions(this, factories, selection);
        }
    }




    public static class ObjectTableColumnSizeConcrete extends ObjectTableColumnSize implements DynamicColumnFactory, Cloneable {
        protected GuiSwingTableColumn column;
        protected DynamicColumnFactoryBase factoryBase;
        protected GuiSwingView.SpecifierManager parentSpecifier;

        public ObjectTableColumnSizeConcrete(int size, GuiMappingContext context, GuiSwingTableColumn column,
                                             GuiSwingView.SpecifierManager parentSpecifier,
                                             GuiSwingTableColumnSet.TableColumnHost model,
                                             GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier) {
            factoryBase = new DynamicColumnFactoryBase(context,
                    new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier),
                    model, rowSpecifier);
            this.size = size;
            this.column = column;
            this.parentSpecifier = parentSpecifier;
        }

        @Override
        public void setParentFactory(DynamicColumnFactory parent) {
            factoryBase.setParentFactory(parent);
        }

        @Override
        public void create(ObjectTableModelColumns.DynamicColumnContainer targetContainer) {
            targetContainer.moveExistingColumns();
            int existing = targetContainer.getColumnsInSize().size();
            int diff = size() - existing;

            if (diff < 0) {
                targetContainer.removeColumnsFromEnd(-diff);
            } else if (diff > 0) {
                for (int n = 0; n < diff; ++n) {
                    createSingle(targetContainer, existing + n);
                }
            }
        }

        public void createSingle(ObjectTableModelColumns.DynamicColumnContainer targetContainer, int indexInSize) {
            ObjectTableColumn c = column.createColumn(factoryBase.getContext(), null, parentSpecifier);
            if (c instanceof GuiSwingTableColumn.ObjectTableColumnWithContext) {
                factoryBase.setSpecifierManager(((GuiSwingTableColumn.ObjectTableColumnWithContext) c).getSpecifierManager());
                targetContainer.add(new ObjectTableColumnCollectionWrapper(
                        (GuiSwingTableColumn.ObjectTableColumnWithContext) c,
                        //context.getChildren().get(0),
                        toIndexInjection(indexInSize),
                        toIndexes()));
            } else {
                targetContainer.add(c);
            }
        }

        /**
         * @param c a value contained a list
         * @return a copy of this: this is a prototype
         */
        @Override
        public ObjectTableColumnSize getColumnSize(Object c) {
            try {
                return (ObjectTableColumnSizeConcrete) super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public List<GuiSwingTableColumn.SpecifierManagerIndex> getIndexSpecifiers() {
            return factoryBase.getIndexSpecifiers();
        }

        @Override
        public Object getValue(Map<GuiSwingTableColumn.SpecifierManagerIndex, Integer> indexInjection) {
            return factoryBase.getValue(indexInjection);
        }

        @Override
        public List<Action> getActions(GuiReprCollectionTable.TableTargetCell selection) {
            return factoryBase.getActions(this, Collections.emptyList(), selection);
        }
    }

    public static class ObjectTableColumnCollectionWrapper extends ObjectTableColumn
        implements GuiSwingTableColumn.ObjectTableColumnWithContext {
        protected ObjectTableColumn column;
        protected Map<GuiSwingTableColumn.SpecifierManagerIndex, Integer> indexInjection;
        /**
         * suppose {(parentIndexes, ...,) elementIndex, propertyIndex}
         */
        protected int[] indexes;

        public ObjectTableColumnCollectionWrapper(GuiSwingTableColumn.ObjectTableColumnWithContext column,
                                                  Map<GuiSwingTableColumn.SpecifierManagerIndex, Integer> indexInjection,
                                                  int[] indexes) {
            this.column = column.asColumn();
            this.indexInjection = indexInjection;
            this.indexes = indexes;
            if (this.column != null) {
                this.column.withHeaderValue(this.column.getTableColumn().getHeaderValue() + Arrays.toString(indexes));
            }
        }

        @Override
        public GuiMappingContext getContext() {
            return ((GuiSwingTableColumn.ObjectTableColumnWithContext) column).getContext();
        }

        @Override
        public GuiSwingView.SpecifierManager getSpecifierManager() {
            return ((GuiSwingTableColumn.ObjectTableColumnWithContext) column).getSpecifierManager();
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
                indexInjection.forEach(GuiSwingTableColumn.SpecifierManagerIndex::setIndex);
                return column.getCellValueFromContext(rowIndex, columnIndex);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Future<?> setCellValue(Object rowObject, int rowIndex, int columnIndex, Object newColumnValue) {
            try {
                indexInjection.forEach(GuiSwingTableColumn.SpecifierManagerIndex::setIndex);
                return column.setCellValueFromContext(rowIndex, columnIndex, newColumnValue);
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

        public Map<GuiSwingTableColumn.SpecifierManagerIndex, Integer> getIndexInjection(
                Collection<GuiSwingTableColumn.SpecifierManagerIndex> requiredSpecs) {
            Map<GuiSwingTableColumn.SpecifierManagerIndex, Integer> spec = new LinkedHashMap<>(requiredSpecs.size() + 1);
            requiredSpecs.forEach(s -> {
                Integer n = indexInjection.get(s);
                if (n != null) {
                    spec.put(s, n);
                }
            });
            return spec;
        }
    }

    public static class TableSelectionSourceDynamic implements GuiSwingTableColumnSet.TableSelectionSource {
        protected GuiSwingTableColumnSet.TableColumnHost model;
        protected GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier;
        protected GuiReprCollectionTable.TableTargetCell selection;
        protected DynamicColumnFactory factory;
        protected String targetName;

        public TableSelectionSourceDynamic(GuiSwingTableColumnSet.TableColumnHost model,
                                           GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
                                           GuiReprCollectionTable.TableTargetCell selection,
                                           DynamicColumnFactory factory,
                                           String targetName) {
            this.model = model;
            this.rowSpecifier = rowSpecifier;
            this.selection = selection;
            this.factory = factory;
            this.targetName = targetName;
        }

        @Override
        public String getTargetName() {
            return targetName;
        }

        @Override
        public boolean isSelectionEmpty() {
            return selection.isSelectionEmpty();
        }

        @Override
        public List<?> getSelectedItems() {
            List<Object> result = new ArrayList<>();
            List<GuiSwingTableColumn.SpecifierManagerIndex> indexSpecifiers = factory.getIndexSpecifiers();
            Set<Map<GuiSwingTableColumn.SpecifierManagerIndex,Integer>> occurrences = new HashSet<>();
            selection.getSelectedRowAllCellIndexesStream().forEach(cell -> {
                ObjectTableColumn column = model.getColumnAt(cell[1]);
                if (column instanceof ObjectTableColumnCollectionWrapper) {
                    Map<GuiSwingTableColumn.SpecifierManagerIndex,Integer> specMap = ((ObjectTableColumnCollectionWrapper) column).getIndexInjection(indexSpecifiers);
                    specMap.put(rowSpecifier, cell[0]);
                    if (occurrences.add(specMap)) {
                        result.add(factory.getValue(specMap));
                    }
                } else {
                    //TODO regular row?
                }
            });
            return result;
        }

        @Override
        public void selectionActionFinished(boolean autoSelection, GuiSwingTableColumnSet.TableSelectionChange change) {
            //TODO selection change
        }
    }

}
