package org.autogui.swing.table;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.*;
import org.autogui.base.mapping.GuiMappingContext.GuiSourceValue;
import org.autogui.base.mapping.GuiReprCollectionTable.TableTargetCell;
import org.autogui.base.type.GuiTypeCollection;
import org.autogui.swing.GuiSwingElement;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingView.SpecifierManagerDefault;
import org.autogui.swing.prefs.GuiSwingPrefsApplyOptions;
import org.autogui.swing.prefs.GuiSwingPrefsSupports;
import org.autogui.swing.table.GuiSwingTableColumn.ObjectTableColumnWithContext;
import org.autogui.swing.table.GuiSwingTableColumn.SpecifierManagerIndex;
import org.autogui.swing.table.GuiSwingTableColumnSet.DynamicColumnHost;
import org.autogui.swing.table.GuiSwingTableColumnSet.TableColumnHost;
import org.autogui.swing.table.GuiSwingTableColumnSet.TableSelectionChange;
import org.autogui.swing.table.GuiSwingTableColumnSet.TableSelectionSource;
import org.autogui.swing.table.GuiSwingTableColumnSetDefault.TableSelectionAction;
import org.autogui.swing.table.GuiSwingTableColumnSetDefault.TableSelectionListAction;
import org.autogui.swing.table.ObjectTableModelColumns.DynamicColumnContainer;
import org.autogui.swing.table.ObjectTableModelColumns.DynamicColumnFactory;
import org.autogui.swing.table.ObjectTableModelColumns.ObjectTableColumnSize;
import org.autogui.swing.table.ObjectTableModelColumns.ObjectTableColumnSizeComposite;
import org.autogui.swing.util.SettingsWindow;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.io.PrintStream;
import java.io.Serial;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * a factory of dynamic column factories for nested collections:
 *   the instance will be registered as a factory of {@link GuiReprCollectionTable}
 * <p>
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

    /**
     * create a {@link DynamicColumnFactoryList} and add it to the model by {@link TableColumnHost#addColumnDynamic(ObjectTableModelColumns.DynamicColumnFactory)}
     * @param context the context of the sub-list, associated with a collection-table
     * @param model the adding target
     * @param rowSpecifier the table root row-specifier
     * @param parentSpecifier the parent specifier factory of the context
     */
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
        SpecifierManagerIndex elementSpecifier = col.getElementIndex();

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
        model.addColumnDynamic(col);
    }

    /** another dynamic factory for supporting property values */
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
                //if (target.isNonEmpty())
                model.addColumnDynamic(target);
            //}
        }
    }

    /** the base class for dynamic column factories:
     *   the class provide implementations of methods defined in
     *     {@link DynamicColumnFactory} and {@link DynamicColumnHost} */
    public static class DynamicColumnFactoryBase {
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;

        protected DynamicColumnFactory parentFactory;
        protected SpecifierManagerIndex elementIndex;
        protected List<SpecifierManagerIndex> indexSet;
        protected List<GuiMappingContext> actionContexts = new ArrayList<>();

        protected TableColumnHost model;

        public DynamicColumnFactoryBase(GuiMappingContext context, SpecifierManager specifierManager,
                                        TableColumnHost model,
                                        SpecifierManagerIndex elementIndex) {
            this.context = context;
            this.specifierManager = specifierManager;
            this.model = model;
            this.elementIndex = elementIndex;
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

        public void setElementIndex(SpecifierManagerIndex elementIndex) {
            this.elementIndex = elementIndex;
        }

        public SpecifierManagerIndex getElementIndex() {
            return elementIndex;
        }

        public void addActionContext(GuiMappingContext context) {
            actionContexts.add(context);
        }

        public List<GuiMappingContext> getActionContexts() {
            return actionContexts;
        }

        /**
         * @return compose parent's specifiers and {@link #getElementIndex()}
         */
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
            SpecifierManagerIndex index = getElementIndex();
            if (index != null){
                indexSet.add(index);
            }
        }

        /**
         *
         * @param sender the self factory
         * @param children children of the factory
         * @param selection a regular table selection which will be wrapped by
         *                  a sub-list based selection such as {@link TableSelectionSourceDynamicForList}
         * @return created actions from {@link #getActionContexts()} and
         *     aggregation of recursive results of children
         */
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

        public TableSelectionListAction toAction(GuiMappingContext actionContext, DynamicColumnFactory sender,
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

        public TableSelectionListAction toActionForTarget(GuiMappingContext actionContext, DynamicColumnFactory sender,
                                        List<DynamicColumnFactory> children,
                                        TableTargetCell selection) {
            return new TableSelectionAction(actionContext,
                        new TableSelectionSourceDynamic(getModel(), selection, sender, getTargetName(this.context)));
        }

        /**
         * collect sub-factories as arguments and
         *  create an aggregated action which takes all selection items of lists of collected factories.
         *  the result action executes the action method in multiple times for each list.
         *  <pre>
         *     List&lt;E&gt; a,b;
         *     //a click of the button of the action might cause 2 executions of the method with "a" and "b"
         *     void action(List&lt;E&gt; s, String n) {
         *         if (n.equals("a")) ... else ...
         *     }
         *  </pre>
         * @param actionContext a context associated with an action
         * @param sender the self factory
         * @param children children of the sender
         * @param selection the regular section of a table
         * @return a created action or null
         */
        public TableSelectionListAction toActionForList(GuiMappingContext actionContext, DynamicColumnFactory sender,
                                      List<DynamicColumnFactory> children,
                                      TableTargetCell selection) {
            List<DynamicColumnFactoryList> sourceFactories = children.stream()
                    .flatMap(f -> collectListActions(actionContext, f).stream())
                    .collect(Collectors.toList());
            if (!sourceFactories.isEmpty()) {
                DynamicColumnFactoryList list = sourceFactories.getFirst(); //all factories have a same type
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

        /**
         * collect argument-factories from a child-factory.
         * <pre>
         *     class Tbl {
         *         List&lt;C&gt; table;
         *     }
         *     class C {   //associated with a {@link DynamicColumnFactoryList}
         *         List&lt;E&gt; a; //childFactory 1 : each property associated with {@link DynamicColumnFactoryComposite}
         *         List&lt;E&gt; b; //childFactory 2
         *         void action(List&lt;E&gt; s, String name) {...} //associated with the actionContext
         *     }
         * </pre>
         * @param actionContext a context associated with an action
         * @param childFactory the tested factory whether elements can be the argument of the action
         * @return  matched factories
         */
        public List<DynamicColumnFactoryList> collectListActions(GuiMappingContext actionContext, DynamicColumnFactory childFactory) {
            List<DynamicColumnFactoryList> result = new ArrayList<>();
            if (childFactory instanceof DynamicColumnFactoryList list) {
                GuiReprActionList listAction = actionContext.getReprActionList();
                GuiMappingContext listContext = list.getContext();

                if (listAction.isSelectionRowIndicesAction(actionContext)) {
                    result.add(list);
                } else if (listAction.isSelectionRowAndColumnIndicesAction(actionContext)) {
                    result.add(list);
                } else if (listAction.isSelectionAction(actionContext, listContext)) {
                    result.add(list);
                }
            } else if (childFactory instanceof DynamicColumnFactoryComposite composite) {
                if (composite.getContext().getRepresentation() instanceof GuiReprPropertyPane) {
                    for (DynamicColumnFactory prop : composite.getFactories()) {
                        result.addAll(collectListActions(actionContext, prop));
                    }
                }
            }
            return result;
        }

        /**
         * @param indexInjection indices for the value, including a row index
         * @return the column item with setting the indices.
         *   So, the specifier managers for the factory must be same instances contained in the indexInjection
         */
        public Object getValue(Map<SpecifierManagerIndex, Integer> indexInjection) {
            try {
                indexInjection.forEach(SpecifierManagerIndex::setIndex);
                GuiMappingContext context = getContext();
                return context.getReprValue().getUpdatedValueWithoutNoUpdate(context,
                        getSpecifierManager().getSpecifier()); //the created specifier is an immutable path, and can be passed to the task executed in another thread
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        /**
         * @param parent a parent object
         * @return obtains a value by the representation of the context.
         *    the representation will be a some property object (instead of a list).
         *    Then the upper index injections can be ignored because the upper value is supplied as parent.
         */
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
        public boolean isNonEmpty() {
            return !getActionContexts().isEmpty() || elementFactory != null;
        }

        @Override
        public ObjectTableColumnSize getColumnSize(Object c) {
            if (elementFactory instanceof ObjectTableColumnSizeConcrete) { //for List<Value>
                return getColumnSizeForConcrete(c);
            } else {
                return getColumnSizeForObjectList(c);
            }
        }

        /**
         * @param c a list object
         * @return the size of the list
         */
        public int getValueSize(Object c) {
            if (context.isReprCollectionTable()) {
                try {
                    return context.getReprCollectionTable().getValueCollectionSize(context, GuiSourceValue.of(c), specifierManager.getSpecifier());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                if (c instanceof Collection<?>) {
                    return ((Collection<?>) c).size();
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
        public void forEachValue(Object c, Consumer<Object> body) {
            SpecifierManagerIndex valueIndex = this.elementIndex;
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

        @SuppressWarnings("unchecked")
        public Object getValueAt(Object c, int i) {
            SpecifierManagerIndex valueIndex = this.elementIndex;
            if (context.isReprCollectionTable()) {
                GuiReprCollectionTable table = context.getReprCollectionTable();
                if (valueIndex == null) {
                    valueIndex = new SpecifierManagerIndex(specifierManager::getSpecifier);
                }
                try {
                    GuiSourceValue col = GuiSourceValue.of(c);
                    return table.getValueCollectionElement(context,
                            col, valueIndex.getSpecifierWithSettingIndex(i), GuiMappingContext.NO_SOURCE)
                            .getValue();
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                if (c instanceof List<?>) {
                    return ((List<Object>) c).get(i);
                } else if (c != null && c.getClass().isArray()) {
                    return Array.get(c, i);
                } else if (c != null) {
                    throw new IllegalArgumentException("forEachValue " + c);
                } else {
                    return null;
                }
            }
        }

        /**
         * @param c a concrete value such as a primitive.
         *         so it will be ignored as creating (a column x the size of the list)  for the value.
         * @return an {@link ObjectTableColumnSize}
         */
        public ObjectTableColumnSize getColumnSizeForConcrete(Object c) {
            ObjectTableColumnSize size = elementFactory.getColumnSize(null);
            size.setSize(getValueSize(c));
            size.setElementSpecifierIndex(getElementIndex());
            return size;
        }

        /**
         * @param c a list object
         * @return a {@link ObjectTableColumnSizeComposite} with
         *    children created by {@link #getElementFactory()}.{@link #getColumnSize(Object)} for each element in the list c.
         *    Currently, the children are expanded by the size of the list
         */
        public ObjectTableColumnSize getColumnSizeForObjectList(Object c) {
            ObjectTableColumnSizeComposite size = new ObjectTableColumnSizeComposite(new ArrayList<>(getValueSize(c)));
            forEachValue(c, e -> size.add(
                    elementFactory == null ?
                            new ObjectTableColumnSizeComposite(Collections.emptyList()) :
                            elementFactory.getColumnSize(e)));
            size.setElementSpecifierIndex(getElementIndex());
            return size;
        }

        @Override
        public void addColumnDynamic(DynamicColumnFactory d) {
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

        @Override
        public String getDisplayName() {
            return "";
        }

        public void debugPrint(int depth) {
            PrintStream out = System.err;
            String indent = IntStream.range(0, depth)
                    .mapToObj(i -> "   ")
                    .collect(Collectors.joining());
            out.println(indent + this);
            out.println(indent + "   : name=" + getDisplayName() + ", context=" + getContext());
            out.println(indent + "   : index=" + elementIndex + ", indexSpecs=" + getIndexSpecifiers());
            if (elementFactory != null) {
                elementFactory.debugPrint(depth + 1);
            }
        }
    }

    /**
     * size-factory for root List&lt;List&lt;T&gt;&gt;.
     * the factory is only created for each root list the outside of a list
     */
    public static class DynamicColumnFactoryCollectionRoot extends DynamicColumnFactoryList
            implements ObjectTableModelColumns.DynamicColumnFactoryRoot {
        protected List<GuiMappingContext> rootActions = new ArrayList<>();

        public DynamicColumnFactoryCollectionRoot(GuiMappingContext tableContext, SpecifierManager specifierManager,
                                                  TableColumnHost model,
                                                  SpecifierManagerIndex rowSpecifier) {
            super(tableContext, specifierManager, model, rowSpecifier);
        }

        /**
         * @param c the root list
         * @return an {@link ObjectTableColumnSizeComposite}
         *   with a single child which is combined by
         *     {@link ObjectTableColumnSizeComposite#set(ObjectTableModelColumns.ObjectTableColumnSize)} for elements of the list
         */
        @Override
        public ObjectTableColumnSize getColumnSize(Object c) {
            ObjectTableColumnSizeComposite root = new ObjectTableColumnSizeComposite(new ArrayList<>(1));
            ObjectTableColumnSize[] element = new ObjectTableColumnSize[] { null };

            forEachValue(c, e -> {
                ObjectTableColumnSize next = elementFactory == null ? null : elementFactory.getColumnSize(e);
                if (element[0] == null) {
                    element[0] = next;
                } else if (next != null) {
                    element[0].set(next);
                }
            });
            if (element[0] != null) {
                root.add(element[0]);
            }
            root.setElementSpecifierIndex(getElementIndex());
            return root;
        }

        @Override
        public boolean isStaticColumns() {
            return elementFactory == null || elementFactory.isStaticColumns();
        }

        @Override
        public boolean isNonEmpty() {
            return true; //always true for
        }

        @Override
        public void addRootAction(GuiMappingContext actionContext) {
            rootActions.add(actionContext);
        }

        /**
         * <pre>
         *     class Tbl { //tableOwner: the target of the action
         *         List&lt;List&lt;E&gt;&gt; table;
         *         void action(List&lt;List&lt;E&gt;&gt; l) { ... }
         *     }
         * </pre>
         * @param selection selection for cells
         * @param tableSource the regular selection of the table, used for selection-updating
         * @return actions from registered rootActions
         */
        @GuiIncluded
        public List<Action> getRootActions(TableTargetCell selection, TableSelectionSource tableSource) {
            GuiMappingContext tableOwner = context.getParent();
            SpecifierManager rootOwnerSpecifier;
            SpecifierManager tableOwnerSpecifier =
                    () -> getSpecifierManager()
                            .getSpecifier() //for table
                            .getParent();    //for table owner: maybe property
            if (tableOwner.getRepresentation() instanceof GuiReprPropertyPane) { //property(collection)
                tableOwner = tableOwner.getParent();
                rootOwnerSpecifier = () -> tableOwnerSpecifier.getSpecifier().getParent();
            } else {
                rootOwnerSpecifier = tableOwnerSpecifier;
            }
            DynamicColumnFactory rootOwner = new DynamicColumnFactoryComposite(
                    tableOwner, rootOwnerSpecifier, model); //create a temporary factory for root-list owner
            return rootActions.stream()
                    .map(a -> withSelectionChange(toAction(a, rootOwner, Collections.singletonList(this), selection),
                            tableSource))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        public TableSelectionListAction withSelectionChange(TableSelectionListAction a, TableSelectionSource source) {
            if (a != null) {
                if (context.isReprCollectionTable()){
                    a.setSelectionChangeFactoryFromContext(context);
                }
                TableSelectionSource actionSource = a.getSource();
                if (actionSource instanceof TableSelectionSourceDynamicBase) { //set the selection changer only for root-actions
                    ((TableSelectionSourceDynamicBase) actionSource).setTableSource(source);
                }
            }
            return a;
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
        public boolean isNonEmpty() {
            return !getActionContexts().isEmpty() || !factories.isEmpty();
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

        public ObjectTableColumnSize getColumnSizeForMember(Object obj, DynamicColumnFactory memberFactory) {
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

        @Override
        public String getDisplayName() {
            if (context.isTypeElementProperty()) {
                return context.getDisplayName();
            } else {
                return "";
            }
        }

        public void debugPrint(int depth) {
            PrintStream out = System.err;
            String indent = IntStream.range(0, depth)
                    .mapToObj(i -> "   ")
                    .collect(Collectors.joining());
            out.println(indent + this);
            out.println(indent + "   : name=" + getDisplayName() + ", context=" + getContext());
            out.println(indent + "   : index=" + elementIndex + ", indexSpecs=" + getIndexSpecifiers());
            factories.forEach(f -> f.debugPrint(depth + 1));
        }
    }

    /**
     * a {@link ObjectTableColumnSize} of a concrete value.
     * the class is also a {@link DynamicColumnFactory}, as a prototype of each size.
     */
    public static class ObjectTableColumnSizeConcrete extends ObjectTableColumnSize implements DynamicColumnFactory, Cloneable {
        protected GuiSwingTableColumn column;
        protected DynamicColumnFactoryBase factoryBase; //shared by clone
        protected SpecifierManager parentSpecifier;

        /**
         * @param size the size of the column. 1 for the prototype
         * @param context the context of the column
         * @param column a (static) column factory for the value
         * @param parentSpecifier the parent specifier factory
         * @param model a column host. usually a parent factory
         */
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

        /**
         * create columns up to {@link #size()}. removes existing columns in the container if it overs {@link #size()}
         * @param targetContainer the adding target
         */
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

        /**
         * create a column from the static column factory and wraps
         *   the returned {@link ObjectTableColumnWithContext} with {@link ObjectTableColumnCollectionWrapper}
         * @param targetContainer the adding target
         * @param indexInSize the index of the created column in the target container
         */
        public void createSingle(DynamicColumnContainer targetContainer, int indexInSize) {
            ObjectTableColumn c = column.createColumn(factoryBase.getContext(), null, parentSpecifier);
            if (c instanceof ObjectTableColumnWithContext) {
                SpecifierManagerIndex rootRowSpec = getIndexSpecifiers().getFirst(); //top spec is the rowSpecifier
                Map<SpecifierManagerIndex,Integer> indexInjections = toIndexInjection(indexInSize);
                factoryBase.setSpecifierManager(((ObjectTableColumnWithContext) c).getSpecifierManager());
                targetContainer.add(new ObjectTableColumnCollectionWrapper(getHeaderName(indexInjections),
                        (ObjectTableColumnWithContext) c,
                        indexInjections,
                        rootRowSpec,
                        toIndices(indexInjections)));
            } else {
                targetContainer.add(c);
            }
        }

        @Override
        public DynamicColumnFactory getParentFactory() {
            return factoryBase.getParentFactory();
        }

        public String getHeaderName(Map<SpecifierManagerIndex,Integer> indexInjection) {
            DynamicColumnFactory f = this;
            String name = "";
            while (f != null) {
                String n = f.getDisplayName();
                SpecifierManagerIndex i = f.getElementIndex();
                if (i != null) {
                    Integer idx = indexInjection.get(i);
                    if (idx != null) {
                        n = n + "[" + idx + "]";
                    }
                }

                if (!n.isEmpty()) {
                    if (name.isEmpty()) {
                        name = n;
                    } else {
                        if (name.startsWith("[")) {
                            name = n + name;
                        } else {
                            name = n + "." + name;
                        }
                    }
                }
                f = f.getParentFactory();
            }
            return name;
        }

        @Override
        public String getDisplayName() {
            GuiMappingContext context = factoryBase.getContext();
            return context.getDisplayName();
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
        public SpecifierManagerIndex getElementIndex() {
            return factoryBase.getElementIndex();
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

        @Override
        public void debugPrint(int depth) {
            PrintStream out = System.err;
            String indent = IntStream.range(0, depth)
                    .mapToObj(i -> "   ")
                    .collect(Collectors.joining());
            out.println(indent + this + " : " + Arrays.toString(toIndices(Collections.emptyMap())));
            out.println(indent + " : name=" + getDisplayName() + ", context=" + factoryBase.getContext());
            out.println(indent + " : index=" + factoryBase.getElementIndex() + ", indexSpecs=" + getIndexSpecifiers());
            out.println(indent + " : column=" + column );
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(size=" + size() + ", name=" + getDisplayName() + ")";
        }
    }

    /**
     * a dynamic version of column
     */
    public static class ObjectTableColumnCollectionWrapper extends ObjectTableColumn
        implements ObjectTableColumnWithContext {
        protected ObjectTableColumn column;
        protected SpecifierManagerIndex rowSpecifier;
        protected Map<SpecifierManagerIndex, Integer> indexInjection;
        /**
         * suppose {(parentIndices, ...,) elementIndex, propertyIndex}
         */
        protected int[] indices;

        public ObjectTableColumnCollectionWrapper(String headerName, ObjectTableColumnWithContext column,
                                                  Map<SpecifierManagerIndex, Integer> indexInjection,
                                                  SpecifierManagerIndex rowSpecifier,
                                                  int[] indices) {
            this.column = column.asColumn();
            this.indexInjection = indexInjection;
            this.indices = indices;
            this.rowSpecifier = rowSpecifier;
            if (this.column != null) {
                this.column.withHeaderValue(headerName);//this.column.getTableColumn().getHeaderValue() +
                        //Arrays.toString(Arrays.copyOfRange(indices, 1, indices.length)));
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
        public void setPreferencesUpdater(Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> updater) {
            columnWithContext().setPreferencesUpdater(updater);
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            columnWithContext().saveSwingPreferences(prefs);
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            columnWithContext().loadSwingPreferences(prefs, options);
        }

        /**
         *
         * @param rowIndex the row index
         * @param columnIndex the column index. ignored
         * @return  a created specifier with setting the row index and indexInjection.
         *    the row index can be variable, and the column index (sub-indices except for the row index) is fixed as a dynamically created column.
         */
        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier(int rowIndex, int columnIndex) {
            rowSpecifier.setIndex(rowIndex);
            indexInjection.forEach(SpecifierManagerIndex::setIndex);
            return column.getSpecifier(rowIndex, columnIndex);
        }

        @Override
        public Object getCellValue(Object rowObject, int rowIndex, int columnIndex, GuiReprValue.ObjectSpecifier specifier) {
            return getCellValueFromContext(rowIndex, columnIndex, specifier);
        }

        @Override
        public Future<?> setCellValue(Object rowObject, int rowIndex, int columnIndex, Object newColumnValue, GuiReprValue.ObjectSpecifier specifier) {
            return setCellValueFromContext(rowIndex, columnIndex, newColumnValue, specifier);
        }

        @Override
        public Object getCellValueFromContext(int rowIndex, int columnIndex, GuiReprValue.ObjectSpecifier specifier) {
            try {
                return column.getCellValueFromContext(rowIndex, columnIndex, specifier);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Future<?> setCellValueFromContext(int rowIndex, int columnIndex, Object newColumnValue, GuiReprValue.ObjectSpecifier specifier) {
            try {
                return column.setCellValueFromContext(rowIndex, columnIndex, newColumnValue, specifier);
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
                    requiredSpecs.subList(Math.min(1, requiredSpecs.size()), requiredSpecs.size())); //exclude rowSpecifier
        }

        public Map<SpecifierManagerIndex, Integer> getIndexInjection(
                int rowIndex,
                List<SpecifierManagerIndex> requiredSpecs) {
            Map<SpecifierManagerIndex, Integer> spec = new LinkedHashMap<>(requiredSpecs.size() + 1);
            //suppose requiredSpec[0] is rowSpecifier
            if (!requiredSpecs.isEmpty()) {
                spec.put(requiredSpecs.getFirst(), rowIndex);
            }
            requiredSpecs.forEach(s -> {
                Integer n = indexInjection.get(s);
                if (n != null) {
                    spec.put(s, n);
                }
            });
            return spec;
        }

        public Map<SpecifierManagerIndex, Integer> getIndexInjectionForRowAndColumnIndices(int rowIndex,
                                                                                           List<SpecifierManagerIndex> requiredSpecs) {
            Map<SpecifierManagerIndex, Integer> spec = new LinkedHashMap<>(requiredSpecs.size() + 1);
            //suppose requiredSpec[0] is rowSpecifier
            if (!requiredSpecs.isEmpty()) {
                spec.put(requiredSpecs.getFirst(), rowIndex);
            }
            spec.putAll(indexInjection); //all indices
            return spec;
        }
    }

    /** the base class for dynamic table-selection-sources */
    public static class TableSelectionSourceDynamicBase {
        protected TableColumnHost model;
        protected TableTargetCell selection;
        protected TableSelectionSource tableSource;

        public TableSelectionSourceDynamicBase(TableColumnHost model, TableTargetCell selection) {
            this.model = model;
            this.selection = selection;
        }


        /**
         *  The method first obtains {@link ObjectTableColumn} of selected {row,column}.
         *  <p>
         *    If the column is a {@link ObjectTableColumnCollectionWrapper}, then it was created as a dynamic column.
         *     The column has indexSpecifiers for obtaining the column value: {rowSpecifier,columnSpecifier,index1,index2, ...}.
         *       The action's target or arguments only requires the former part of the specs.
         *        e.g. <pre>
         *            class Tbl {
         *              List&lt;List&lt;E&gt;&gt; table;
         *              void act(List&lt;List&lt;E&gt;&gt; s) {...}
         *            }
         *            class E { List&lt;Float&gt; fs;}
         *        </pre>
         *           then the specifier of the column "fs[n]" is {rowSpec,colSpec,indexSpecOfFs},
         *               but the action "act" requires only {rowSpec}. //not {rowSpec,ColSpec}. currently, it is recognized as just {@code act(List<X>)}
         *  <p>
         *    If the column is another {@link ObjectTableColumn} type, then it was created as a static column.
         *     The column's specifier becomes {rowSpecifier}.
         *
         * @param factory    source of required specifiers :
         *                    <ul>
         *                      <li>For List&lt;E&gt; and action(List&lt;E&gt;), {rowSpecifier} become the required specifiers.</li>
         *                      <li>For List&lt;List&lt;E&gt;&gt; and class E{ action() }, {rowSpecifier,columnSpecifier} become the required specs.</li>
         *                    </ul>
         *
         * @param targetName name of the target field. for List&lt;V&gt; f1; List&lt;V&gt; f2; and action(List&lt;V&gt;,String targetName), the name is "f1" or "f2"
         * @param occurrences a set for ignoring duplicated entries
         * @param resultGen   action for each occurrence
         */
        public void select(DynamicColumnFactory factory, String targetName,
                           Set<TargetAndSpecifierMap> occurrences, Consumer<TargetAndSpecifierMap> resultGen) {
            List<SpecifierManagerIndex> indexSpecifiers = factory == null ? Collections.emptyList() :
                    factory.getIndexSpecifiers();
            selection.getSelectedCellIndices().forEach(cell -> {
                ObjectTableColumn column = model.getColumnAt(cell[1]);
                if (column instanceof ObjectTableColumnCollectionWrapper colWrapper) {
                    if (colWrapper.containsIndexInjections(indexSpecifiers)) {
                        TargetAndSpecifierMap t = new TargetAndSpecifierMap(targetName,
                                selectSpecifierMapForCollectionWrapper((ObjectTableColumnCollectionWrapper) column, cell, indexSpecifiers));
                        if (occurrences.add(t)) {
                            resultGen.accept(t);
                        }
                    }
                } else { //for non-dynamic columns
                    TargetAndSpecifierMap t = new TargetAndSpecifierMap(targetName,
                            selectSpecifierMapForStaticColumn(column, cell, indexSpecifiers));
                    if (occurrences.add(t)) {
                        resultGen.accept(t);
                    }
                }
            });
        }

        public Map<SpecifierManagerIndex, Integer> selectSpecifierMapForCollectionWrapper(ObjectTableColumnCollectionWrapper column,
                                                                                         int[] cell,
                                                                                         List<SpecifierManagerIndex> indexSpecifiers) {
            return column.getIndexInjection(cell[0], indexSpecifiers);
        }

        public Map<SpecifierManagerIndex, Integer> selectSpecifierMapForStaticColumn(ObjectTableColumn column,
                                                                                     int[] cell,
                                                                                     List<SpecifierManagerIndex> indexSpecifiers) {

            if (!indexSpecifiers.isEmpty()) {
                //rowIndex
                return Collections.singletonMap(indexSpecifiers.getFirst(), cell[0]);
            } else {
                return Collections.emptyMap();
            }
        }

        public void selectionActionFinished(boolean autoSelection, TableSelectionChange change) {
            //selection change: currently support selection change only for root-action
            if (tableSource != null) {
                tableSource.selectionActionFinished(autoSelection, change);
            }
        }

        public void setTableSource(TableSelectionSource tableSource) {
            this.tableSource = tableSource;
        }

        /**
         * @since 1.2
         */
        public void selectionActionPrepare() {
            if (tableSource != null) {
                tableSource.selectionActionPrepare();
            }
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
        protected DynamicColumnFactory targetFactory; //factory of a nested list
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
        protected List<DynamicColumnFactoryList> argumentFactories; //factories of field candidates

        public TableSelectionSourceDynamicForList(TableColumnHost model,
                                                  TableTargetCell selection,
                                                  DynamicColumnFactory targetFactory,
                                                  List<DynamicColumnFactoryList> argumentFactories) {
            super(model, selection);
            this.targetFactory = targetFactory;
            this.argumentFactories = argumentFactories;
        }

        /**
         * the name might be changed for argument factory, and
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

                List<SpecifierManagerIndex> cellSpecs =  toCellIndexSpecs(targetSpecs,
                        elementFactory == null ?
                                argumentFactory.getIndexSpecifiers() : elementFactory.getIndexSpecifiers());
                select(getSelectionFactory(argumentFactory),
                        targetName,
                        occurrences,
                        t -> {//targetIndexSpecifiers is a sub-set of argumentIndexSpecifiers
                                Object value = getIndexValue(argumentFactory, t, cellSpecs);
                                if (value != null) {
                                    targetToArgumentMap.computeIfAbsent(targetFactory.getValue(t.specMap), k -> new ArrayList<>())
                                            .add(value);
                                }
                        });
            }
            List<TargetAndArgumentList> result = new ArrayList<>();
            targetNameToTargetToArgumentMap.forEach((targetName, targetToArgMap) ->
                targetToArgMap.forEach((target, args) ->
                        result.add(new TargetAndArgumentList(target, args, targetName))));
            return result;
        }

        public DynamicColumnFactory getSelectionFactory(DynamicColumnFactoryList argumentFactory) {
            return argumentFactory;
        }

        public List<SpecifierManagerIndex> toCellIndexSpecs(List<SpecifierManagerIndex> targetSpecs, List<SpecifierManagerIndex> argSpecs) {
            return argSpecs;
        }

        //in the class, it returns an element value
        public Object getIndexValue(DynamicColumnFactoryList argumentFactory, TargetAndSpecifierMap t, List<SpecifierManagerIndex> cellSpecs) {
            if (argumentFactory.getElementFactory() == null) {
                //for non-dynamic column
                Object list = argumentFactory.getValue(t.specMap);
                Integer idx = t.specMap.get(cellSpecs.getFirst());
                return argumentFactory.getValueAt(list, idx);
            } else {
                return argumentFactory.getElementFactory().getValue(t.specMap);
            }
        }
    }

    /**
     * a selected target field name and specifiers as an intermediate state of {@link TableSelectionSourceDynamicForList}.
     */
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

        public int[] toIndicesOtherThan(List<SpecifierManagerIndex> specs) {
            return specMap.entrySet().stream() //specMap is ordered
                        .filter(e -> !specs.contains(e.getKey()))
                        .mapToInt(Map.Entry::getValue)
                        .toArray();
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /** a result value of {@link TableSelectionSourceDynamicForList} with additional information */
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

    /**
     * an action using selection of dynamic columns
     */
    public static class TableSelectionListDynamicAction extends TableSelectionListAction  {
        @Serial private static final long serialVersionUID = 1L;

        public TableSelectionListDynamicAction(GuiMappingContext context, TableSelectionSourceDynamicForList source) {
            super(context, source);
        }

        @Override
        protected Object actionPerformedBody(List<?> src, String targetName, GuiReprValue.ObjectSpecifier targetSpec) {
            List<Object> values = new ArrayList<>(src.size());
            boolean expand = false;
            GuiMappingContext context = getContext();
            if (context.isTypeElementActionList() &&
                    context.getTypeElementAsActionList().getReturnType() instanceof GuiTypeCollection) {
                expand = true;
            }
            GuiReprActionList action = context.getReprActionList();
            for (Object v : src) {
                if (v instanceof TargetAndArgumentList ta) {
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
        public Object getIndexValue(DynamicColumnFactoryList argumentFactory, TargetAndSpecifierMap t, List<SpecifierManagerIndex> cellSpecs) {
            return t.specMap.get(cellSpecs.getFirst());
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
     *        t.list.get(0).get(0).act(asList({0}),      "l1"); //a
     *        t.list.get(0).get(0).act(asList({0}),      "l2"); //b
     *        t.list.get(0).get(1).act(asList({0},{1}),"l1"); //c,d
     *
     *        t.list.get(1).get(0).act(asList({0}),      "l2"); //e
     *        t.list.get(1).get(1).act(asList({1}),      "l1"); //f
     *        t.list.get(1).get(1).act(asList({0}),      "l2"); //g
     *   </pre>
     *   Both "l1" and "l2" has indexSpecs "[i][j][k]" (e.g. list.get(i).get(j).l1.get(k)),
     *     but "i" and "j" will be ignored for the action "act" in "E" and
     *        each element of the argument becomes {k}.
     *      This is intended to keep the int[] having {row,column,...} observed by the target of the action (here "E").
     *       The user can distinguish "i" and "j" by the target instance of "E".
     */
    public static class TableSelectionSourceDynamicForRowAndColumnIndices extends TableSelectionSourceDynamicForIndices {
        public TableSelectionSourceDynamicForRowAndColumnIndices(TableColumnHost model, TableTargetCell selection,
                                                     DynamicColumnFactory targetFactory, List<DynamicColumnFactoryList> argumentFactories) {
            super(model, selection, targetFactory, argumentFactories); //argumentFactories are root-list candidates
        }

        @Override
        public DynamicColumnFactory getSelectionFactory(DynamicColumnFactoryList argumentFactory) {
            return argumentFactory.getElementFactory(); //for each candidate, required specs become its specifiers: {...targetSpecs,...argListSpec,argElemRowSpec,argElemIdxAsColumnSpec|,...}
        }

        @Override
        public Map<SpecifierManagerIndex, Integer> selectSpecifierMapForCollectionWrapper(ObjectTableColumnCollectionWrapper column,
                                                                                          int[] cell,
                                                                                          List<SpecifierManagerIndex> indexSpecifiers) {
            return column.getIndexInjectionForRowAndColumnIndices(cell[0], indexSpecifiers); //use all specs of the column. the indexSpecifiers is used only for the first spec: {rowSpec,...}
        }

        @Override
        public List<SpecifierManagerIndex> toCellIndexSpecs(List<SpecifierManagerIndex> targetSpecs, List<SpecifierManagerIndex> argSpecs) {
            return targetSpecs; //change the semantics of the returned value: specs for exclusion
        }

        @Override
        public Object getIndexValue(DynamicColumnFactoryList argumentFactory, TargetAndSpecifierMap t, List<SpecifierManagerIndex> cellSpecsExcluded) {
            int[] idx = t.toIndicesOtherThan(cellSpecsExcluded);
            if (idx.length == 0 || Arrays.stream(idx).anyMatch(i -> i == -1)) {
                return null;
            } else {
                return idx;
            }
        }
    }


}
