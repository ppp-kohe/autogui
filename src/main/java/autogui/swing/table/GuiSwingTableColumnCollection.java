package autogui.swing.table;

import autogui.base.mapping.*;
import autogui.base.mapping.GuiMappingContext.GuiSourceValue;
import autogui.base.mapping.GuiReprCollectionTable.TableTargetCell;
import autogui.base.type.GuiTypeCollection;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingPreferences;
import autogui.swing.GuiSwingView.SpecifierManager;
import autogui.swing.GuiSwingView.SpecifierManagerDefault;
import autogui.swing.table.GuiSwingTableColumn.ObjectTableColumnWithContext;
import autogui.swing.table.GuiSwingTableColumn.SpecifierManagerIndex;
import autogui.swing.table.GuiSwingTableColumnSet.DynamicColumnHost;
import autogui.swing.table.GuiSwingTableColumnSet.TableColumnHost;
import autogui.swing.table.GuiSwingTableColumnSet.TableSelectionChange;
import autogui.swing.table.GuiSwingTableColumnSet.TableSelectionSource;
import autogui.swing.table.GuiSwingTableColumnSetDefault.TableSelectionAction;
import autogui.swing.table.GuiSwingTableColumnSetDefault.TableSelectionListAction;
import autogui.swing.table.ObjectTableModelColumns.DynamicColumnContainer;
import autogui.swing.table.ObjectTableModelColumns.DynamicColumnFactory;
import autogui.swing.table.ObjectTableModelColumns.ObjectTableColumnSize;
import autogui.swing.table.ObjectTableModelColumns.ObjectTableColumnSizeComposite;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * there are various cases for table-compositions
 *  <pre>
 *      class T { List&lt;Float&gt; l; } //staticColumn,  no dynamic columns
 *  </pre>
 *  <pre>
 *      class T { List&lt;E&gt; l; }
 *      class E { int x; }         //staticColumn, no dynamic columns
 *  </pre>
 *  <pre>
 *      class T { List&lt;List&lt;Float&gt;&gt; l; } //dynamicColumn
 *  </pre>
 *  <pre>
 *      class T { List&lt;List&lt;E&gt;&gt; l; }
 *      class E { int x; }         //dynamicColumn
 *  </pre>
 *  <pre>
 *      class T { List&lt;E&gt; l; }
 *      class E { int x;           //staticColumn
 *                List&lt;Float&gt; f; } //dynamicColumn
 *  </pre>
 *  <pre>
 *      class T { List&lt;E&gt; l; }
 *      class E { int x;           //staticColumn
 *                F   f; }
 *      class F { List&lt;Float&gt; fs; } //dynamicColumn
 *  </pre>
 *  for some cases, a nested member might be a first dynamicColumn source.
 *   thus, {@link GuiSwingTableColumnSetDefault} always creates {@link DynamicColumnFactory} for each composition.
 */
public class GuiSwingTableColumnCollection implements GuiSwingTableColumnDynamic {
    protected GuiSwingMapperSet columnMapperSet;

    public GuiSwingTableColumnCollection(GuiSwingMapperSet columnMapperSet) {
        this.columnMapperSet = columnMapperSet;
    }

    @Override
    public void createColumnDynamic(GuiMappingContext context,
                                            TableColumnHost model,
                                            SpecifierManagerIndex rowSpecifier,
                                            SpecifierManager parentSpecifier) {
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
        SpecifierManagerDefault tableSpecifier = new SpecifierManagerDefault(parentSpecifier::getSpecifier);

        DynamicColumnFactoryList col = new DynamicColumnFactoryList(context, tableSpecifier, model);
        SpecifierManagerIndex elementSpecifier = col.getIndex();

        for (GuiMappingContext elementCollection : context.getChildren()) { //always a single-element context
            GuiSwingElement elemView = columnMapperSet.viewTableColumn(elementCollection);
            if (elemView instanceof GuiSwingTableColumnSet) { //and, usually element-repr machines to the column-set
                ((GuiSwingTableColumnSet) elemView).createColumnsForDynamicCollection(elementCollection,
                        col, rowSpecifier, elementSpecifier);
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
        public void createColumnDynamic(GuiMappingContext context, TableColumnHost model,
                                                                                SpecifierManagerIndex rowSpecifier,
                                                                                SpecifierManager parentSpecifier) {
            //GuiReprPropertyPane prop = (GuiReprPropertyPane) context.getRepresentation();
            //if (prop.getSubRepresentations() instanceof GuiReprCollectionTable) { //prop(list(E))
                SpecifierManager subSpecifier = new SpecifierManagerDefault(parentSpecifier::getSpecifier);
                DynamicColumnFactoryComposite target = new DynamicColumnFactoryComposite(context, subSpecifier, model);
                for (GuiMappingContext subContext : context.getChildren()) {
                    GuiSwingElement view = columnMapperSet.viewTableColumn(subContext);
                    GuiSwingTableColumnSetDefault.createColumnForDynamicCollection(
                            subContext, target, rowSpecifier, subSpecifier, view);
                }
                if (target.hasDynamicColumns()) {
                    model.addColumnDynamic(target);
                }
            //}
        }
    }

    public static class DynamicColumnFactoryBase {
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;

        protected DynamicColumnFactory parentFactory;
        protected SpecifierManagerIndex index;
        protected List<SpecifierManagerIndex> indexSet;
        protected List<GuiMappingContext> actionContexts = new ArrayList<>();

        protected TableColumnHost model;

        public DynamicColumnFactoryBase(GuiMappingContext context, SpecifierManager specifierManager,
                                        TableColumnHost model,
                                        SpecifierManagerIndex index) {
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

        public void setSpecifierManager(SpecifierManager specifierManager) {
            this.specifierManager = specifierManager;
        }

        public SpecifierManager getSpecifierManager() {
            return specifierManager;
        }

        public void setParentFactory(DynamicColumnFactory parent) {
            this.parentFactory = parent;
        }

        public DynamicColumnFactory getParentFactory() {
            return parentFactory;
        }

        public void setModel(TableColumnHost model) {
            this.model = model;
        }

        public TableColumnHost getModel() {
            return model;
        }

        public void setIndex(SpecifierManagerIndex index) {
            this.index = index;
        }

        public SpecifierManagerIndex getIndex() {
            return index;
        }

        public void addActionContext(GuiMappingContext context) {
            actionContexts.add(context);
        }

        public List<GuiMappingContext> getActionContexts() {
            return actionContexts;
        }

        public List<SpecifierManagerIndex> getIndexSpecifiers() {
            if (indexSet == null) {
                indexSet = new ArrayList<>();
                DynamicColumnFactory p = getParentFactory();
                if (p != null) {
                    indexSet.addAll(p.getIndexSpecifiers());
                }
                addIndexToList(indexSet);
                ((ArrayList<SpecifierManagerIndex>) indexSet).trimToSize();
            }
            return indexSet;
        }

        protected void addIndexToList(List<SpecifierManagerIndex> indexSet) {
            SpecifierManagerIndex index = getIndex();
            if (index != null){
                indexSet.add(index);
            }
        }

        public List<Action> getActions(DynamicColumnFactory sender, List<DynamicColumnFactory> children,
                                       TableTargetCell selection) {
            return Stream.concat(
                        getActionContexts().stream()
                            .map(a -> toAction(a, sender, children, selection))
                            .filter(Objects::nonNull),
                        children.stream()
                            .flatMap(d -> d.getActions(selection).stream()))
                    .collect(Collectors.toList());
        }

        public Action toAction(GuiMappingContext actionContext, DynamicColumnFactory sender,
                               List<DynamicColumnFactory> children,
                               TableTargetCell selection) {
            if (actionContext.isReprAction()) {
                return toActionForTarget(actionContext, sender, children, selection);
            } else if (actionContext.isReprActionList()) {
                return toActionForList(actionContext, sender, children, selection);
            } else {
                return null;
            }
        }

        public Action toActionForTarget(GuiMappingContext actionContext, DynamicColumnFactory sender,
                                        List<DynamicColumnFactory> children,
                                        TableTargetCell selection) {
            return new TableSelectionAction(actionContext,
                        new TableSelectionSourceDynamic(getModel(), selection, sender, getTargetName(this.context)));
        }

        public Action toActionForList(GuiMappingContext actionContext, DynamicColumnFactory sender,
                                      List<DynamicColumnFactory> children,
                                      TableTargetCell selection) {
            List<DynamicColumnFactoryList> sourceFactories = children.stream()
                    .flatMap(f -> collectListActions(actionContext, f).stream())
                    .collect(Collectors.toList());
            if (!sourceFactories.isEmpty()) {
                DynamicColumnFactoryList list = sourceFactories.get(0); //all factories have a same type
                GuiReprActionList listAction = actionContext.getReprActionList();
                GuiMappingContext listContext = list.getContext();

                if (listAction.isSelectionRowIndicesAction(actionContext)) {
                    return new TableSelectionListDynamicAction(actionContext,
                            new TableSelectionSourceDynamicForIndices(getModel(), selection, sender, sourceFactories));

                } else if (listAction.isSelectionRowAndColumnIndicesAction(actionContext)) {
                    return new TableSelectionListDynamicAction(actionContext,
                            new TableSelectionSourceDynamicForRowAndColumnIndices(getModel(), selection, sender, sourceFactories));

                } else if (listAction.isSelectionAction(actionContext, listContext)) {
                    return new TableSelectionListDynamicAction(actionContext,
                            new TableSelectionSourceDynamicForList(getModel(), selection, sender, sourceFactories));
                }
            }
            return null;
        }

        public List<DynamicColumnFactoryList> collectListActions(GuiMappingContext actionContext, DynamicColumnFactory childFactory) {
            List<DynamicColumnFactoryList> result = new ArrayList<>();
            if (childFactory instanceof DynamicColumnFactoryList) {
                GuiReprActionList listAction = actionContext.getReprActionList();
                DynamicColumnFactoryList list = (DynamicColumnFactoryList) childFactory;
                GuiMappingContext listContext = list.getContext();

                if (listAction.isSelectionRowIndicesAction(actionContext)) {
                    result.add(list);
                } else if (listAction.isSelectionRowAndColumnIndicesAction(actionContext)) {
                    result.add(list);
                } else if (listAction.isSelectionAction(actionContext, listContext)) {
                    result.add(list);
                }
            } else if (childFactory instanceof DynamicColumnFactoryComposite) {
                DynamicColumnFactoryComposite composite = (DynamicColumnFactoryComposite) childFactory;
                if (composite.getContext().getRepresentation() instanceof GuiReprPropertyPane) {
                    for (DynamicColumnFactory prop : composite.getFactories()) {
                        result.addAll(collectListActions(actionContext, prop));
                    }
                }
            }
            return result;
        }

        public Object getValue(Map<SpecifierManagerIndex, Integer> indexInjection) {
            try {
                indexInjection.forEach(SpecifierManagerIndex::setIndex);
                GuiMappingContext context = getContext();
                return context.getReprValue().getUpdatedValueWithoutNoUpdate(context,
                        getSpecifierManager().getSpecifier());
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        public Object getValueAsMember(Object parent) {
            try {
                GuiMappingContext context = getContext();
                return context.getReprValue().getValueWithoutNoUpdate(context, GuiSourceValue.of(parent),
                        getSpecifierManager().getSpecifier());
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static String getTargetName(GuiMappingContext context) {
        String targetName;
        if (context.isTypeElementCollection() && context.hasParent()) {
            targetName = context.getParent().getName();
        } else {
            targetName = context.getName();
        }
        return targetName;

    }

    /**
     * size-factory for List&lt;E&gt;
     */
    public static class DynamicColumnFactoryList extends DynamicColumnFactoryBase
            implements DynamicColumnFactory,
                DynamicColumnHost, TableColumnHost {
        protected DynamicColumnFactory elementFactory;

        public DynamicColumnFactoryList(GuiMappingContext context, SpecifierManager specifierManager,
                                        TableColumnHost model) {
            super(context, specifierManager, model,
                    new SpecifierManagerIndex(specifierManager::getSpecifier));
        }

        public DynamicColumnFactoryList(GuiMappingContext context, SpecifierManager specifierManager,
                                        TableColumnHost model,
                                        SpecifierManagerIndex index) {
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
        public ObjectTableColumnSize getColumnSize(Object c) {
            if (elementFactory instanceof ObjectTableColumnSizeConcrete) { //for List<Value>
                return getColumnSizeForConcrete(c);
            } else {
                return getColumnSizeForObjectList(c);
            }
        }

        public int getValueSize(Object c) {
            if (context.isReprCollectionTable()) {
                try {
                    return context.getReprCollectionTable().getValueCollectionSize(context, GuiSourceValue.of(c), specifierManager.getSpecifier());
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
        public void forEachValue(Object c, GuiMappingContext context, SpecifierManager specifierManager,
                                 SpecifierManagerIndex valueIndex, Consumer<Object> body) {
            if (context.isReprCollectionTable()) {
                GuiReprCollectionTable table = context.getReprCollectionTable();
                if (valueIndex == null) {
                    valueIndex = new SpecifierManagerIndex(specifierManager::getSpecifier);
                }
                try {
                    GuiSourceValue col = GuiSourceValue.of(c);
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

        public ObjectTableColumnSize getColumnSizeForConcrete(Object c) {
            ObjectTableColumnSize size = elementFactory.getColumnSize(null);
            size.setSize(getValueSize(c));
            size.setElementSpecifierIndex(getIndex());
            return size;
        }

        public ObjectTableColumnSize getColumnSizeForObjectList(Object c) {
            ObjectTableColumnSizeComposite size = new ObjectTableColumnSizeComposite(new ArrayList<>(getValueSize(c)));
            forEachValue(c, this.context, this.specifierManager, this.index, e -> size.add(elementFactory.getColumnSize(e)));
            size.setElementSpecifierIndex(getIndex());
            return size;
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
        public List<Action> getActions(TableTargetCell selection) {
            return getActions(this,
                    elementFactory == null ? Collections.emptyList() : Collections.singletonList(elementFactory),
                    selection);
        }

        @Override
        public boolean isStaticColumns() {
            return false;
        }
    }

    /**
     * size-factory for root List&lt;List&lt;T&gt;&gt;
     */
    public static class DynamicColumnFactoryCollectionRoot extends DynamicColumnFactoryList {
        public DynamicColumnFactoryCollectionRoot(GuiMappingContext context, SpecifierManager specifierManager,
                                                  TableColumnHost model,
                                                  SpecifierManagerIndex rowSpecifier) {
            super(context, specifierManager, model, rowSpecifier);
        }

        @Override
        public ObjectTableColumnSize getColumnSize(Object c) {
            ObjectTableColumnSizeComposite root = new ObjectTableColumnSizeComposite(new ArrayList<>(1));
            ObjectTableColumnSizeComposite element = new ObjectTableColumnSizeComposite(new ArrayList<>());
            forEachValue(c, this.context, this.specifierManager, this.index, e -> element.set(elementFactory.getColumnSize(e)));
            root.add(element);
            root.setElementSpecifierIndex(getIndex());
            return root;
        }

        @Override
        public boolean isStaticColumns() {
            return elementFactory.isStaticColumns();
        }
    }

    /**
     * size-factory for class C { T0 f0; T1 f0; ... }
     */
    public static class DynamicColumnFactoryComposite extends DynamicColumnFactoryBase
            implements DynamicColumnFactory,
                DynamicColumnHost, TableColumnHost {
        protected List<DynamicColumnFactory> factories = new ArrayList<>();

        public DynamicColumnFactoryComposite(GuiMappingContext context,
                                             SpecifierManager specifierManager,
                                             TableColumnHost model) {
            super(context, specifierManager, model, null);
        }

        public List<DynamicColumnFactory> getFactories() {
            return factories;
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
        public ObjectTableColumnSize getColumnSize(Object c) {
            return new ObjectTableColumnSizeComposite(factories.stream()
                    .map(f -> getColumnSizeForMember(c, f))
                    .collect(Collectors.toList()));
        }

        public ObjectTableColumnSize getColumnSizeForMember(Object obj,
                                                                                    DynamicColumnFactory memberFactory) {
            return memberFactory.getColumnSize(memberFactory.getValueAsMember(obj));
        }

        @Override
        public void addColumnDynamic(DynamicColumnFactory d) {
            factories.add(d);
            d.setParentFactory(this);
        }

        @Override
        public List<Action> getActions(TableTargetCell selection) {
            return getActions(this, factories, selection);
        }

        @Override
        public boolean isStaticColumns() {
            return factories.stream()
                    .allMatch(DynamicColumnFactory::isStaticColumns);
        }
    }


    public static class ObjectTableColumnSizeConcrete extends ObjectTableColumnSize implements DynamicColumnFactory, Cloneable {
        protected GuiSwingTableColumn column;
        protected DynamicColumnFactoryBase factoryBase;
        protected SpecifierManager parentSpecifier;

        public ObjectTableColumnSizeConcrete(int size, GuiMappingContext context, GuiSwingTableColumn column,
                                             SpecifierManager parentSpecifier,
                                             TableColumnHost model) {
            factoryBase = new DynamicColumnFactoryBase(context,
                    new SpecifierManagerDefault(parentSpecifier::getSpecifier),
                    model, null);
            this.size = size;
            this.column = column;
            this.parentSpecifier = parentSpecifier;
        }

        @Override
        public void setParentFactory(DynamicColumnFactory parent) {
            factoryBase.setParentFactory(parent);
        }

        @Override
        public void create(DynamicColumnContainer targetContainer) {
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

        public void createSingle(DynamicColumnContainer targetContainer, int indexInSize) {
            ObjectTableColumn c = column.createColumn(factoryBase.getContext(), null, parentSpecifier);
            if (c instanceof ObjectTableColumnWithContext) {
                factoryBase.setSpecifierManager(((ObjectTableColumnWithContext) c).getSpecifierManager());
                targetContainer.add(new ObjectTableColumnCollectionWrapper(
                        (ObjectTableColumnWithContext) c,
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
        public ObjectTableColumnSize getColumnSize(Object c) {
            try {
                return (ObjectTableColumnSizeConcrete) super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public List<SpecifierManagerIndex> getIndexSpecifiers() {
            return factoryBase.getIndexSpecifiers();
        }

        @Override
        public Object getValue(Map<SpecifierManagerIndex, Integer> indexInjection) {
            return factoryBase.getValue(indexInjection);
        }

        @Override
        public List<Action> getActions(TableTargetCell selection) {
            return factoryBase.getActions(this, Collections.emptyList(), selection);
        }

        @Override
        public Object getValueAsMember(Object parent) {
            return factoryBase.getValueAsMember(parent);
        }

        @Override
        public boolean isStaticColumns() {
            return true;
        }
    }

    public static class ObjectTableColumnCollectionWrapper extends ObjectTableColumn
        implements ObjectTableColumnWithContext {
        protected ObjectTableColumn column;
        protected Map<SpecifierManagerIndex, Integer> indexInjection;
        /**
         * suppose {(parentIndices, ...,) elementIndex, propertyIndex}
         */
        protected int[] indices;

        public ObjectTableColumnCollectionWrapper(ObjectTableColumnWithContext column,
                                                  Map<SpecifierManagerIndex, Integer> indexInjection,
                                                  int[] indices) {
            this.column = column.asColumn();
            this.indexInjection = indexInjection;
            this.indices = indices;
            if (this.column != null) {
                this.column.withHeaderValue(this.column.getTableColumn().getHeaderValue() +
                        Arrays.toString(Arrays.copyOfRange(indices, 1, indices.length)));
            }
        }

        private ObjectTableColumnWithContext columnWithContext() {
            return (ObjectTableColumnWithContext) column;
        }

        @Override
        public GuiMappingContext getContext() {
            return columnWithContext().getContext();
        }

        @Override
        public SpecifierManager getSpecifierManager() {
            return ((ObjectTableColumnWithContext) column).getSpecifierManager();
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
                indexInjection.forEach(SpecifierManagerIndex::setIndex);
                return column.getCellValueFromContext(rowIndex, columnIndex);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Future<?> setCellValueFromContext(int rowIndex, int columnIndex, Object newColumnValue) {
            try {
                indexInjection.forEach(SpecifierManagerIndex::setIndex);
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

        public boolean containsIndexInjections(List<SpecifierManagerIndex> requiredSpecs) {
            return indexInjection.keySet().containsAll(
                    requiredSpecs.subList(Math.min(1, requiredSpecs.size()), requiredSpecs.size()));
        }

        public Map<SpecifierManagerIndex, Integer> getIndexInjection(
                int rowIndex,
                List<SpecifierManagerIndex> requiredSpecs) {
            Map<SpecifierManagerIndex, Integer> spec = new LinkedHashMap<>(requiredSpecs.size() + 1);
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

    public static class TableSelectionSourceDynamicBase {
        protected TableColumnHost model;
        protected TableTargetCell selection;

        public TableSelectionSourceDynamicBase(TableColumnHost model, TableTargetCell selection) {
            this.model = model;
            this.selection = selection;
        }

        public void select(DynamicColumnFactory factory, String targetName,
                           Set<TargetAndSpecifierMap> occurrences, Consumer<TargetAndSpecifierMap> resultGen) {
            List<SpecifierManagerIndex> indexSpecifiers = factory.getIndexSpecifiers();
            selection.getSelectedRowAllCellIndicesStream().forEach(cell -> {
                ObjectTableColumn column = model.getColumnAt(cell[1]);
                if (column instanceof ObjectTableColumnCollectionWrapper) {
                    ObjectTableColumnCollectionWrapper colWrapper = (ObjectTableColumnCollectionWrapper) column;
                    if (colWrapper.containsIndexInjections(indexSpecifiers)) {
                        Map<SpecifierManagerIndex, Integer> specMap = ((ObjectTableColumnCollectionWrapper) column)
                                .getIndexInjection(cell[0], indexSpecifiers);
                        TargetAndSpecifierMap t = new TargetAndSpecifierMap(targetName, specMap);
                        if (occurrences.add(t)) {
                            resultGen.accept(t);
                        }
                    }
                } else {
                    //TODO regular row?

                }
            });
        }
    }

    /**
     * <pre>
     *     class T { List&lt;List&lt;E&gt;&gt; list; }
     *     class E {
     *         String n;
     *         float  v;
     *         void act() {...}
     *     }
     * </pre>
     *   the class T will be bound to a table like the following columns
     * <pre>
     *     #  | n[0]  | v[0] | ... | n[1] | v[1] | ...
     *    ----|-------|------|-----|------|------|-----
     *     0  |     a |   b  | ... |   c  |      | ...
     * </pre>
     * If a user selects the columns "a", "b", and "c",  and runs the action "act",
     *   then the following code will be executed
     *   <pre>
     *        T t = ...;
     *        t.list.get(0).get(0).act(); //a, b
     *        t.list.get(0).get(1).act(); //c
     *   </pre>
     */
    public static class TableSelectionSourceDynamic extends TableSelectionSourceDynamicBase
            implements TableSelectionSource {
        protected DynamicColumnFactory targetFactory;
        protected String targetName;

        public TableSelectionSourceDynamic(TableColumnHost model,
                                           TableTargetCell selection,
                                           DynamicColumnFactory targetFactory,
                                           String targetName) {
            super(model, selection);
            this.targetFactory = targetFactory;
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
            select(targetFactory, targetName, new HashSet<>(), t -> result.add(targetFactory.getValue(t.specMap)));
            return result;
        }

        @Override
        public void selectionActionFinished(boolean autoSelection, TableSelectionChange change) {
            //TODO selection change
        }
    }

    /**
     * <pre>
     *     class T { List&lt;List&lt;E&gt;&gt; list; }
     *     class E {
     *         String n;
     *         List&lt;Float&gt; l;
     *         void act(List&lt;Float&gt; selected) {...}
     *     }
     * </pre>
     *   the class T will be bound to a table like the following columns
     * <pre>
     *     #  | n[0] |  l[0,0]  | l[0,1] | ... | n[1] | l[1,0] | l[1,1] | ...
     *    ----|------|----------|--------|-----|------|--------|--------|-----
     *     0  |      |      a   |    b   | ... |      |   c    |        | ...
     * </pre>
     * If a user selects the columns "a", "b", and "c",  and runs the action "act",
     *   then the following code will be executed
     *   <pre>
     *        T t = ...;
     *        t.list.get(0).get(0).act(asList(
     *                         t.list.get(0).get(1).l.get(0),   //a
     *                         t.list.get(0).get(1).l.get(1))); //b
     *        t.list.get(0).get(1).act(asList(
     *                         t.list.get(0).get(1).l.get(0))); //c
     *   </pre>
     */
    public static class TableSelectionSourceDynamicForList extends TableSelectionSourceDynamicBase
            implements TableSelectionSource {
        protected DynamicColumnFactory targetFactory;
        protected List<DynamicColumnFactoryList> argumentFactories;

        public TableSelectionSourceDynamicForList(TableColumnHost model,
                                                  TableTargetCell selection,
                                                  DynamicColumnFactory targetFactory,
                                                  List<DynamicColumnFactoryList> argumentFactories) {
            super(model, selection);
            this.targetFactory = targetFactory;
            this.argumentFactories = argumentFactories;
        }

        /**
         * the name might be changed for argument factories and
         *    thus it will be included in each {@link TargetAndArgumentList}.
         *   <pre>
         *       class T { List&lt;List&lt;E&gt;&gt; list; }
         *
         *       class E {
         *           List&lt;Float&gt; l1;
         *           List&lt;Float&gt; l1;
         *
         *           void act(List&lt;Float&gt; l, String targetName) {
         *               if (targetName.equals("l1")) ... else ...
         *           }
         *       }
         *   </pre>
         * @return ""
         */
        @Override
        public String getTargetName() {
            return "";
        }

        @Override
        public boolean isSelectionEmpty() {
            return selection.isSelectionEmpty();
        }

        @Override
        public List<TargetAndArgumentList> getSelectedItems() {
            List<SpecifierManagerIndex> targetSpecs = targetFactory.getIndexSpecifiers();
            Set<TargetAndSpecifierMap> occurrences = new HashSet<>();
            Map<String, Map<Object, List<Object>>> targetNameToTargetToArgumentMap = new LinkedHashMap<>();
            for (DynamicColumnFactoryList argumentFactory : argumentFactories) {
                String targetName = GuiSwingTableColumnCollection.getTargetName(argumentFactory.getContext());
                Map<Object, List<Object>> targetToArgumentMap = targetNameToTargetToArgumentMap.computeIfAbsent(
                        targetName, n -> new LinkedHashMap<>());

                DynamicColumnFactory elementFactory = argumentFactory.getElementFactory();

                List<SpecifierManagerIndex> cellSpecs =  toCellIndexSpecs(targetSpecs, elementFactory.getIndexSpecifiers());
                if (!cellSpecs.isEmpty()) {
                    select(elementFactory,
                            targetName,
                            occurrences,
                            t -> {//targetIndexSpecifiers is a sub-set of argumentIndexSpecifiers
                                    Object value = getIndexValue(elementFactory, t, cellSpecs);
                                    if (value != null) {
                                        targetToArgumentMap.computeIfAbsent(targetFactory.getValue(t.specMap), k -> new ArrayList<>())
                                                .add(value);
                                    }
                            });
                }
            }
            List<TargetAndArgumentList> result = new ArrayList<>();
            targetNameToTargetToArgumentMap.forEach((targetName, targetToArgMap) ->
                targetToArgMap.forEach((target, args) ->
                        result.add(new TargetAndArgumentList(target, args, targetName))));
            return result;
        }

        public List<SpecifierManagerIndex> toCellIndexSpecs(List<SpecifierManagerIndex> targetSpecs, List<SpecifierManagerIndex> argSpecs) {
            return argSpecs;
        }

        public Object getIndexValue(DynamicColumnFactory elementFactory, TargetAndSpecifierMap t, List<SpecifierManagerIndex> cellSpecs) {
            return elementFactory.getValue(t.specMap);
        }

        @Override
        public void selectionActionFinished(boolean autoSelection, TableSelectionChange change) {
            //TODO selection change
        }
    }

    public static class TargetAndSpecifierMap {
        public String targetName;
        public Map<SpecifierManagerIndex, Integer> specMap;
        protected int hash;

        public TargetAndSpecifierMap(String targetName, Map<SpecifierManagerIndex, Integer> specMap) {
            this.targetName = targetName;
            this.specMap = specMap;
            hash = Objects.hash(targetName, specMap);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TargetAndSpecifierMap that = (TargetAndSpecifierMap) o;
            return Objects.equals(targetName, that.targetName) &&
                    Objects.equals(specMap, that.specMap);
        }

        public int[] toIndices(List<SpecifierManagerIndex> specs) {
            return specs.stream()
                    .mapToInt(s -> specMap.getOrDefault(s, -1))
                    .toArray();
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static class TargetAndArgumentList {
        public Object target;
        public List<Object> argument;
        public String targetName;

        public TargetAndArgumentList(Object target, List<Object> argument, String targetName) {
            this.target = target;
            this.argument = argument;
            this.targetName = targetName;
        }

        @Override
        public String toString() {
            return "(" + target + "," + argument + "," + targetName + ")";
        }
    }

    public static class TableSelectionListDynamicAction extends TableSelectionListAction  {
        public TableSelectionListDynamicAction(GuiMappingContext context, TableSelectionSourceDynamicForList source) {
            super(context, source);
        }

        @Override
        protected Object actionPerformedBody() {
            List<?> src = source.getSelectedItems();
            List<Object> values = new ArrayList<>(src.size());
            boolean expand = false;
            if (context.isTypeElementActionList() && context.getTypeElementAsActionList().getReturnType() instanceof GuiTypeCollection) {
                expand = true;
            }
            GuiReprActionList action = context.getReprActionList();
            for (Object v : source.getSelectedItems()) {
                if (v instanceof TargetAndArgumentList) {
                    TargetAndArgumentList ta = (TargetAndArgumentList) v;
                    Object ret = action.executeActionForList(context, ta.target, ta.argument, ta.targetName);
                    if (expand) {
                        values.addAll((Collection<?>) ret);
                    } else {
                        values.add(ret);
                    }
                }
            }
            return values;
        }
    }

    /**
     * <pre>
     *     class T { List&lt;List&lt;E&gt;&gt; list; }
     *     class E {
     *         String n;
     *         List&lt;Float&gt;  l1;
     *         List&lt;String&gt; l2;
     *         void act(List&lt;Integer&gt; selectedIndices, String targetName) {...}
     *     }
     * </pre>
     *   the class T will be bound to a table like the following columns
     * <pre>
     *     #  | n[0] |  l1[0,0] | ... | l2[0,0] | ... | n[1] | l1[1,0] | l1[1,1] | ... | l2[1,0] | ...
     *    ----|------|----------|-----|---------|-----|------|---------|---------|-----|---------|-----
     *     0  |      |      a   | ... |    b    | ... |      |   c     |   d     | ... |         | ...
     * </pre>
     * If a user selects the columns "a", "b", "c" and "d",  and runs the action "act",
     *   then the following code will be executed
     *   <pre>
     *        T t = ...;
     *        t.list.get(0).get(0).act(asList(0),   "l1"); //a
     *        t.list.get(0).get(0).act(asList(1),   "l2"); //b
     *        t.list.get(0).get(1).act(asList(0,1), "l1"); //c,d
     *   </pre>
     */
    public static class TableSelectionSourceDynamicForIndices extends TableSelectionSourceDynamicForList {
        public TableSelectionSourceDynamicForIndices(TableColumnHost model, TableTargetCell selection,
                                                     DynamicColumnFactory targetFactory, List<DynamicColumnFactoryList> argumentFactories) {
            super(model, selection, targetFactory, argumentFactories);
        }

        @Override
        public List<SpecifierManagerIndex> toCellIndexSpecs(List<SpecifierManagerIndex> targetSpecs, List<SpecifierManagerIndex> argSpecs) {
            if (targetSpecs.size() < argSpecs.size()) {
                return Collections.singletonList(argSpecs.get(targetSpecs.size()));
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public Object getIndexValue(DynamicColumnFactory elementFactory, TargetAndSpecifierMap t, List<SpecifierManagerIndex> cellSpecs) {
            return t.specMap.get(cellSpecs.get(0));
        }
    }

    /**
     * <pre>
     *     class T { List&lt;List&lt;E&gt;&gt; list; }
     *     class E {
     *         String n;
     *         List&lt;Float&gt;  l1;
     *         List&lt;String&gt; l2;
     *         void act(List&lt;int[]&gt; selectedIndices, String targetName) {...}
     *     }
     * </pre>
     *   the class T will be bound to a table like the following columns
     * <pre>
     *     #  | n[0] |  l1[0,0] | ... | l2[0,0] | ... | n[1] | l1[1,0] | l1[1,1] | ... | l2[1,0] | ...
     *    ----|------|----------|-----|---------|-----|------|---------|---------|-----|---------|-----
     *     0  |      |      a   | ... |    b    | ... |      |   c     |   d     | ... |         | ...
     *     1  |      |          | ... |    e    | ... |      |         |   f     | ... |    g    | ...
     * </pre>
     * If a user selects the columns "a"..."g",  and runs the action "act",
     *   then the following code will be executed
     *   <pre>
     *        T t = ...;
     *        t.list.get(0).get(0).act(asList({0,0}),      "l1"); //a
     *        t.list.get(0).get(0).act(asList({0,0}),      "l2"); //b
     *        t.list.get(0).get(1).act(asList({0,0},{0,1}),"l1"); //c,d
     *
     *        t.list.get(1).get(0).act(asList({0,0}),      "l2"); //e
     *        t.list.get(1).get(1).act(asList({0,1}),      "l1"); //f
     *        t.list.get(1).get(1).act(asList({0,0}),      "l2"); //g
     *   </pre>
     *   Both "l1" and "l2" has indexSpecs "[i][j][k]" (e.g. list.get(i).get(j).l1.get(k)),
     *     but "j" will be ignored for the action "act" in "E" and
     *        each element of the argument becomes {i,k}.
     *      This is intended to keep the int[] having {row,column,...}.
     *       The user can distinguish "j" by the target instance of "E".
     */
    public static class TableSelectionSourceDynamicForRowAndColumnIndices extends TableSelectionSourceDynamicForIndices {
        public TableSelectionSourceDynamicForRowAndColumnIndices(TableColumnHost model, TableTargetCell selection,
                                                     DynamicColumnFactory targetFactory, List<DynamicColumnFactoryList> argumentFactories) {
            super(model, selection, targetFactory, argumentFactories);
        }

        @Override
        public List<SpecifierManagerIndex> toCellIndexSpecs(List<SpecifierManagerIndex> targetSpecs, List<SpecifierManagerIndex> argSpecs) {
            int ts = targetSpecs.size();
            int as = argSpecs.size();
            if (ts > 0 && ts < as) {
                List<SpecifierManagerIndex> cellSpecs = new ArrayList<>(as - ts + 1);
                cellSpecs.add(targetSpecs.get(0));
                cellSpecs.addAll(argSpecs.subList(ts, as));
                return cellSpecs;
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public Object getIndexValue(DynamicColumnFactory elementFactory, TargetAndSpecifierMap t, List<SpecifierManagerIndex> cellSpecs) {
            return t.toIndices(cellSpecs);
        }
    }


}
