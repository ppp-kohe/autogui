package autogui.swing.table;

import autogui.base.mapping.*;
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
    public void createColumnDynamic(GuiMappingContext context,
                                            GuiSwingTableColumnSet.TableColumnHost model,
                                            GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
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
                        //DynamicColumnCollection: columnSpecifierIndex = ...Index(tableSpecifier...)
                       subContext : GuiReprValueStringField
                         //cc.column.createColumn(cc.context, ..., rowSpecifier:null, parentSpecifier:columnSpecifierIndex)
         */
        GuiSwingView.SpecifierManagerDefault tableSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);

        DynamicColumnFactoryList col = new DynamicColumnFactoryList(context, tableSpecifier, model);
        GuiSwingTableColumn.SpecifierManagerIndex elementSpecifier = col.getIndex();

        for (GuiMappingContext elementCollection : context.getChildren()) { //always a single-element context
            GuiSwingElement elemView = columnMapperSet.viewTableColumn(elementCollection);
            if (elemView instanceof GuiSwingTableColumnSet) { //and, usually element-repr machines to the column-set
                ((GuiSwingTableColumnSet) elemView).createColumnsForDynamicCollection(elementCollection,
                        col, col, rowSpecifier, elementSpecifier);
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
        if (col.hasDynamicColumns()) {
            model.addColumnDynamic(col);
        }
    }

    public static class GuiSwingTableColumnProperty implements GuiSwingTableColumnDynamic {
        protected GuiSwingMapperSet columnMapperSet;

        public GuiSwingTableColumnProperty(GuiSwingMapperSet columnMapperSet) {
            this.columnMapperSet = columnMapperSet;
        }

        @Override
        public void createColumnDynamic(GuiMappingContext context, GuiSwingTableColumnSet.TableColumnHost model,
                                                                                GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
                                                                                GuiSwingView.SpecifierManager parentSpecifier) {
            if (((GuiReprPropertyPane) context.getRepresentation()).getSubRepresentations() instanceof GuiReprCollectionTable) { //prop(list(E))
                GuiSwingView.SpecifierManager subSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);
                DynamicColumnFactoryComposite target = new DynamicColumnFactoryComposite(context, subSpecifier, model);
                for (GuiMappingContext subContext : context.getChildren()) {
                    GuiSwingElement view = columnMapperSet.viewTableColumn(subContext);
                    GuiSwingTableColumnSetDefault.createColumnForDynamicCollection(
                            subContext, target, target, rowSpecifier, subSpecifier, view);
                }
                if (target.hasDynamicColumns()) {
                    model.addColumnDynamic(target);
                }
            }
        }
    }

    public static class DynamicColumnFactoryBase {
        protected GuiMappingContext context;
        protected GuiSwingView.SpecifierManager specifierManager;

        protected ObjectTableModelColumns.DynamicColumnFactory parentFactory;
        protected GuiSwingTableColumn.SpecifierManagerIndex index;
        protected List<GuiSwingTableColumn.SpecifierManagerIndex> indexSet;
        protected List<GuiMappingContext> actionContexts = new ArrayList<>();

        protected GuiSwingTableColumnSet.TableColumnHost model;

        public DynamicColumnFactoryBase(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager,
                                        GuiSwingTableColumnSet.TableColumnHost model,
                                        GuiSwingTableColumn.SpecifierManagerIndex index) {
            this.context = context;
            this.specifierManager = specifierManager;
            this.model = model;
            this.index = index;
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

        public void setParentFactory(ObjectTableModelColumns.DynamicColumnFactory parent) {
            this.parentFactory = parent;
        }

        public ObjectTableModelColumns.DynamicColumnFactory getParentFactory() {
            return parentFactory;
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
                ObjectTableModelColumns.DynamicColumnFactory p = getParentFactory();
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

        public List<Action> getActions(ObjectTableModelColumns.DynamicColumnFactory sender, List<ObjectTableModelColumns.DynamicColumnFactory> children,
                                       GuiReprCollectionTable.TableTargetCell selection) {
            return Stream.concat(
                        getActionContexts().stream()
                            .map(a -> toAction(a, sender, children, selection))
                            .filter(Objects::nonNull),
                        children.stream()
                            .flatMap(d -> d.getActions(selection).stream()))
                    .collect(Collectors.toList());
        }

        public Action toAction(GuiMappingContext actionContext, ObjectTableModelColumns.DynamicColumnFactory sender,
                               List<ObjectTableModelColumns.DynamicColumnFactory> children,
                               GuiReprCollectionTable.TableTargetCell selection) {
            if (actionContext.isReprAction()) {
                return toActionForTarget(actionContext, sender, children, selection);
            } else if (actionContext.isReprActionList()) {
                return toActionForList(actionContext, sender, children, selection);
            } else {
                return null;
            }
        }

        public Action toActionForTarget(GuiMappingContext actionContext, ObjectTableModelColumns.DynamicColumnFactory sender,
                                        List<ObjectTableModelColumns.DynamicColumnFactory> children,
                                        GuiReprCollectionTable.TableTargetCell selection) {
            return new GuiSwingTableColumnSetDefault.TableSelectionAction(actionContext,
                        new TableSelectionSourceDynamic(getModel(), selection, sender, getTargetName(this.context)));
        }

        public Action toActionForList(GuiMappingContext actionContext, ObjectTableModelColumns.DynamicColumnFactory sender,
                                      List<ObjectTableModelColumns.DynamicColumnFactory> children,
                                      GuiReprCollectionTable.TableTargetCell selection) {
            GuiReprActionList listAction = actionContext.getReprActionList();
            for (ObjectTableModelColumns.DynamicColumnFactory childFactory : children) {
                if (childFactory instanceof DynamicColumnFactoryList) {
                    GuiMappingContext listContext = ((DynamicColumnFactoryList) childFactory).getContext();
                    if (listAction.isSelectionRowIndicesAction(actionContext)) {
                        //TODO
                    } else if (listAction.isSelectionRowAndColumnIndicesAction(actionContext)) {

                    } else if (listAction.isSelectionAction(actionContext, listContext)) {
                        return new GuiSwingTableColumnSetDefault.TableSelectionListAction(actionContext,
                                new TableSelectionSourceDynamic(getModel(), selection, childFactory, getTargetName(listContext)));
                    }
                }
            }
            return null;
        }


        public String getTargetName(GuiMappingContext context) {
            String targetName;
            if (context.isTypeElementCollection() && context.hasParent()) {
                targetName = context.getParent().getName();
            } else {
                targetName = context.getName();
            }
            return targetName;
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
            implements ObjectTableModelColumns.DynamicColumnFactory,
                GuiSwingTableColumnSet.DynamicColumnHost, GuiSwingTableColumnSet.TableColumnHost {
        protected ObjectTableModelColumns.DynamicColumnFactory elementFactory;

        public DynamicColumnFactoryList(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager,
                                        GuiSwingTableColumnSet.TableColumnHost model) {
            super(context, specifierManager, model,
                    new GuiSwingTableColumn.SpecifierManagerIndex(specifierManager::getSpecifier));
        }

        public DynamicColumnFactoryList(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager,
                                        GuiSwingTableColumnSet.TableColumnHost model,
                                        GuiSwingTableColumn.SpecifierManagerIndex index) {
            super(context, specifierManager, model, index);
        }

        @Override
        public void addColumnRowIndex() {
            getModel().addColumnRowIndex();
        }

        @Override
        public void addColumnStatic(ObjectTableColumn column) {
            getModel().addColumnStatic(column);
        }

        @Override
        public void addMenuRowComposite(ObjectTableColumn.TableMenuComposite rowComposite) {
            getModel().addMenuRowComposite(rowComposite);
        }

        @Override
        public ObjectTableColumn getColumnAt(int modelIndex) {
            return getModel().getColumnAt(modelIndex);
        }

        @Override
        public boolean hasDynamicColumns() {
            return elementFactory != null;
        }

        @Override
        public ObjectTableModelColumns.ObjectTableColumnSize getColumnSize(Object c) {
            if (elementFactory instanceof ObjectTableColumnSizeConcrete) { //for List<Value>
                return getColumnSizeForConcrete(c);
            } else {
                return getColumnSizeForObjectList(c);
            }
        }

        public int getValueSize(Object c) {
            if (context.isParentCollectionTable()) {
                try {
                    return context.getReprCollectionTable().getValueCollectionSize(context, GuiMappingContext.GuiSourceValue.of(c), specifierManager.getSpecifier());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                if (c instanceof Collection<?>) {
                    return ((Collection) c).size();
                } else if (c != null && c.getClass().isArray()) {
                    return Array.getLength(c);
                } else if (c == null) {
                    return 0;
                } else {
                    throw new IllegalArgumentException("getValueSize: " + c);
                }
            }
        }

        @SuppressWarnings("unchecked")
        public void forEachValue(Object c, GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager,
                                 GuiSwingTableColumn.SpecifierManagerIndex valueIndex, Consumer<Object> body) {
            if (context.isReprCollectionTable()) {
                GuiReprCollectionTable table = context.getReprCollectionTable();
                if (valueIndex == null) {
                    valueIndex = new GuiSwingTableColumn.SpecifierManagerIndex(specifierManager::getSpecifier);
                }
                try {
                    GuiMappingContext.GuiSourceValue col = GuiMappingContext.GuiSourceValue.of(c);
                    for (int i = 0, l = table.getValueCollectionSize(context, col, specifierManager.getSpecifier()); i < l; ++i) {
                        body.accept(table.getValueCollectionElement(context,
                                col, valueIndex.getSpecifierWithSettingIndex(i), GuiMappingContext.NO_SOURCE)
                                .getValue());
                    }
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                if (c instanceof List<?>) {
                    ((List<Object>) c).forEach(body);
                } else if (c != null && c.getClass().isArray()) {
                    int l = Array.getLength(c);
                    for (int i = 0; i < l; ++i) {
                        body.accept(Array.get(c, i));
                    }
                } else if (c != null) {
                    throw new IllegalArgumentException("forEachValue " + c);
                } //if c == null : nothing
            }
        }

        public ObjectTableModelColumns.ObjectTableColumnSize getColumnSizeForConcrete(Object c) {
            ObjectTableModelColumns.ObjectTableColumnSize size = elementFactory.getColumnSize(null);
            size.setSize(getValueSize(c));
            size.setElementSpecifierIndex(getIndex());
            return size;
        }

        public ObjectTableModelColumns.ObjectTableColumnSize getColumnSizeForObjectList(Object c) {
            ObjectTableModelColumns.ObjectTableColumnSizeComposite size = new ObjectTableModelColumns.ObjectTableColumnSizeComposite(new ArrayList<>(getValueSize(c)));
            forEachValue(c, this.context, this.specifierManager, this.index, e -> size.add(elementFactory.getColumnSize(e)));
            size.setElementSpecifierIndex(getIndex());
            return size;
        }

        @Override
        public void addColumn(GuiMappingContext context, GuiSwingTableColumn column,
                              GuiSwingView.SpecifierManager parentSpecifier) {
            addColumnDynamic(new ObjectTableColumnSizeConcrete(1, context, column, parentSpecifier, model));
        }

        @Override
        public void addColumnDynamic(ObjectTableModelColumns.DynamicColumnFactory d) {
            if (elementFactory != null) {
                //something-wrong
            }
            elementFactory = d;
            d.setParentFactory(this);
        }

        public ObjectTableModelColumns.DynamicColumnFactory getElementFactory() {
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
    }

    /**
     * size-factory for class C { T0 f0; T1 f0; ... }
     */
    public static class DynamicColumnFactoryComposite extends DynamicColumnFactoryBase
            implements ObjectTableModelColumns.DynamicColumnFactory,
                GuiSwingTableColumnSet.DynamicColumnHost, GuiSwingTableColumnSet.TableColumnHost {
        protected List<ObjectTableModelColumns.DynamicColumnFactory> factories = new ArrayList<>();

        public DynamicColumnFactoryComposite(GuiMappingContext context,
                                             GuiSwingView.SpecifierManager specifierManager,
                                             GuiSwingTableColumnSet.TableColumnHost model) {
            super(context, specifierManager, model, null);
        }

        @Override
        public void addColumnRowIndex() {
            getModel().addColumnRowIndex();
        }

        @Override
        public void addColumnStatic(ObjectTableColumn column) {
            getModel().addColumnStatic(column);
        }

        @Override
        public void addMenuRowComposite(ObjectTableColumn.TableMenuComposite rowComposite) {
            getModel().addMenuRowComposite(rowComposite);
        }

        @Override
        public boolean hasDynamicColumns() {
            return !factories.isEmpty();
        }

        @Override
        public ObjectTableColumn getColumnAt(int modelIndex) {
            return getModel().getColumnAt(modelIndex);
        }

        @Override
        public ObjectTableModelColumns.ObjectTableColumnSize getColumnSize(Object c) {
            return new ObjectTableModelColumns.ObjectTableColumnSizeComposite(factories.stream()
                    .map(f -> f.getColumnSize(c))
                    .collect(Collectors.toList()));
        }

        @Override
        public void addColumn(GuiMappingContext context, GuiSwingTableColumn column, GuiSwingView.SpecifierManager parentSpecifier) {
            addColumnDynamic(new ObjectTableColumnSizeConcrete(1, context, column, parentSpecifier, model));
        }

        @Override
        public void addColumnDynamic(ObjectTableModelColumns.DynamicColumnFactory d) {
            factories.add(d);
            d.setParentFactory(this);
        }

        @Override
        public List<Action> getActions(GuiReprCollectionTable.TableTargetCell selection) {
            return getActions(this, factories, selection);
        }
    }




    public static class ObjectTableColumnSizeConcrete extends ObjectTableModelColumns.ObjectTableColumnSize implements ObjectTableModelColumns.DynamicColumnFactory, Cloneable {
        protected GuiSwingTableColumn column;
        protected DynamicColumnFactoryBase factoryBase;
        protected GuiSwingView.SpecifierManager parentSpecifier;

        public ObjectTableColumnSizeConcrete(int size, GuiMappingContext context, GuiSwingTableColumn column,
                                             GuiSwingView.SpecifierManager parentSpecifier,
                                             GuiSwingTableColumnSet.TableColumnHost model) {
            factoryBase = new DynamicColumnFactoryBase(context,
                    new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier),
                    model, null);
            this.size = size;
            this.column = column;
            this.parentSpecifier = parentSpecifier;
        }

        @Override
        public void setParentFactory(ObjectTableModelColumns.DynamicColumnFactory parent) {
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
                        toIndices()));
            } else {
                targetContainer.add(c);
            }
        }

        /**
         * @param c a value contained a list
         * @return a copy of this: this is a prototype
         */
        @Override
        public ObjectTableModelColumns.ObjectTableColumnSize getColumnSize(Object c) {
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
         * suppose {(parentIndices, ...,) elementIndex, propertyIndex}
         */
        protected int[] indices;

        public ObjectTableColumnCollectionWrapper(GuiSwingTableColumn.ObjectTableColumnWithContext column,
                                                  Map<GuiSwingTableColumn.SpecifierManagerIndex, Integer> indexInjection,
                                                  int[] indices) {
            this.column = column.asColumn();
            this.indexInjection = indexInjection;
            this.indices = indices;
            if (this.column != null) {
                this.column.withHeaderValue(this.column.getTableColumn().getHeaderValue() + Arrays.toString(indices));
            }
        }

        private GuiSwingTableColumn.ObjectTableColumnWithContext columnWithContext() {
            return (GuiSwingTableColumn.ObjectTableColumnWithContext) column;
        }

        @Override
        public GuiMappingContext getContext() {
            return columnWithContext().getContext();
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
            columnWithContext().setSettingsWindow(settingWindow);
        }

        @Override
        public SettingsWindow getSettingsWindow() {
            return columnWithContext().getSettingsWindow();
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            columnWithContext().setPreferencesUpdater(updater);
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            columnWithContext().saveSwingPreferences(prefs);
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs) {
            columnWithContext().loadSwingPreferences(prefs);
        }

        @Override
        public Object getCellValue(Object rowObject, int rowIndex, int columnIndex) {
            return getCellValueFromContext(rowIndex, columnIndex);
        }

        @Override
        public Future<?> setCellValue(Object rowObject, int rowIndex, int columnIndex, Object newColumnValue) {
            return setCellValueFromContext(rowIndex, columnIndex, newColumnValue);
        }

        @Override
        public Object getCellValueFromContext(int rowIndex, int columnIndex) {
            try {
                indexInjection.forEach(GuiSwingTableColumn.SpecifierManagerIndex::setIndex);
                return column.getCellValueFromContext(rowIndex, columnIndex);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Future<?> setCellValueFromContext(int rowIndex, int columnIndex, Object newColumnValue) {
            try {
                indexInjection.forEach(GuiSwingTableColumn.SpecifierManagerIndex::setIndex);
                return column.setCellValueFromContext(rowIndex, columnIndex, newColumnValue);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public int[] columnIndexToValueIndex(int columnIndex) {
            return indices;
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
                int rowIndex,
                List<GuiSwingTableColumn.SpecifierManagerIndex> requiredSpecs) {
            Map<GuiSwingTableColumn.SpecifierManagerIndex, Integer> spec = new LinkedHashMap<>(requiredSpecs.size() + 1);
            //suppose requiredSpec[0] is rowSpecifier
            if (!requiredSpecs.isEmpty()) {
                spec.put(requiredSpecs.iterator().next(), rowIndex);
            }
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
        protected GuiReprCollectionTable.TableTargetCell selection;
        protected ObjectTableModelColumns.DynamicColumnFactory factory;
        protected String targetName;

        public TableSelectionSourceDynamic(GuiSwingTableColumnSet.TableColumnHost model,
                                           GuiReprCollectionTable.TableTargetCell selection,
                                           ObjectTableModelColumns.DynamicColumnFactory factory,
                                           String targetName) {
            this.model = model;
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
            selection.getSelectedRowAllCellIndicesStream().forEach(cell -> {
                ObjectTableColumn column = model.getColumnAt(cell[1]);
                if (column instanceof ObjectTableColumnCollectionWrapper) {
                    Map<GuiSwingTableColumn.SpecifierManagerIndex,Integer> specMap = ((ObjectTableColumnCollectionWrapper) column)
                            .getIndexInjection(cell[0], indexSpecifiers);
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
