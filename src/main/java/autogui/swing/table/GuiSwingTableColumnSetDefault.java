package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprAction;
import autogui.base.mapping.GuiReprActionList;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.*;
import autogui.swing.GuiSwingView.SpecifierManager;
import autogui.swing.GuiSwingView.SpecifierManagerDefault;
import autogui.swing.table.GuiSwingTableColumn.SpecifierManagerIndex;
import autogui.swing.table.GuiSwingTableColumnCollection.DynamicColumnFactoryCollectionRoot;
import autogui.swing.table.GuiSwingTableColumnCollection.DynamicColumnFactoryComposite;
import autogui.swing.table.GuiSwingTableColumnCollection.ObjectTableColumnSizeConcrete;
import autogui.swing.table.ObjectTableColumn.TableMenuComposite;
import autogui.swing.table.ObjectTableModelColumns.DynamicColumnFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * the default implementation of table-column-set associated with {@link autogui.base.mapping.GuiReprCollectionElement}.
 *
 */
public class GuiSwingTableColumnSetDefault implements GuiSwingTableColumnSet {
    protected GuiSwingMapperSet columnMappingSet;

    public GuiSwingTableColumnSetDefault(GuiSwingMapperSet columnMappingSet) {
        this.columnMappingSet = columnMappingSet;
    }

    @Override
    public void createColumns(GuiMappingContext context, TableColumnHost parentModel,
                              SpecifierManagerIndex rowSpecifier,
                              SpecifierManager parentSpecifier,
                              SpecifierManager specifierManager) { //optional

        DynamicColumnHost model;
        SpecifierManager subManager;
        if (context.isReprCollectionElement() && context.isParentCollectionTable()) { //specifierManager != null
            parentModel.addColumnRowIndex();
            model = new DynamicColumnFactoryCollectionRoot(context.getParent(), parentSpecifier, parentModel, rowSpecifier);
            subManager = specifierManager; //specifier for collection-element is an index, which will be specifierManager (usually rowSpecifier) created in the parent table
        } else {
            subManager = new SpecifierManagerDefault(parentSpecifier::getSpecifier);  //specifier for object-pane is a sub-spec of a parent (specifierManager will be null)
            model = new DynamicColumnFactoryComposite(context, subManager, parentModel);
        }

        for (GuiMappingContext subContext : context.getChildren()) {
            //context: GuiReprCollectionElement(...) { subContext: GuiReprObjectPane { ... }  }
            //context: GuiReprCollectionElement(...) { subContext: GuiReprCollectionTable { subSubContext: GuiReprCollectionElement(...) }  }
            GuiSwingElement subView = columnMappingSet.viewTableColumn(subContext);
            if (subView instanceof GuiSwingTableColumn) {
                GuiSwingTableColumn column = (GuiSwingTableColumn) subView;
                ObjectTableColumn columnStatic = column.createColumn(subContext, rowSpecifier, subManager);
                if (columnStatic != null) {
                    model.addColumnStatic(columnStatic);
                }
            } else if (subView instanceof GuiSwingTableColumnDynamic) {
                ((GuiSwingTableColumnDynamic) subView).createColumnDynamic(
                        subContext, model, rowSpecifier, subManager);
            } else if (subView instanceof GuiSwingTableColumnSet) {
                GuiSwingTableColumnSet set = (GuiSwingTableColumnSet) subView;

                set.createColumns(subContext, model, rowSpecifier, subManager, null);

            } else if (subContext.isReprActionList()) {
                //for List<C> list; and class C { List<E> es; a1(List<E>){...}  a2(){...} }
                //   a1 will be added by dynamic-actions mechanism
                //   (a2 is covered by createColumnActions)
                model.addActionContext(subContext);
            }
        }

        getMenuRowComposites(context)
                .forEach(model::addMenuRowComposite);

        //if (model.isNonEmpty() /*&& model instanceof ObjectTableModelColumns.DynamicColumnFactory*/)
        parentModel.addColumnDynamic((DynamicColumnFactory) model); //always append to the parent
    }

    public List<TableMenuComposite> getMenuRowComposites(GuiMappingContext context) {
        List<TableMenuComposite> comps = new ArrayList<>(4);
        comps.add(new ToStringCopyCell.TableMenuCompositeToStringCopy(context, -1));
        comps.add(new GuiSwingJsonTransfer.TableMenuCompositeJsonCopy(context, -1));
        if (context.isReprValue() && context.getReprValue().isEditable(context)) {
            comps.add(new GuiSwingJsonTransfer.TableMenuCompositeJsonPaste(context, -1));
            comps.add(new ToStringCopyCell.TableMenuCompositeToStringPaste(-1));
        }
        return comps;
    }

    @Override
    public List<Action> createColumnActions(GuiMappingContext context, TableSelectionSource source) {
        List<Action> actions = new ArrayList<>();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMappingSet.viewTableColumn(subContext);
            if (subView instanceof GuiSwingAction) {
                TableSelectionAction a = new TableSelectionAction(subContext, source);
                actions.add(a);

                GuiMappingContext tableContext = getTableContext(context);
                if (tableContext != null) { //table context
                    a.setSelectionChangeFactoryFromContext(tableContext);
                }

            } else if (subView instanceof GuiSwingTableColumnSet) {
                actions.addAll(((GuiSwingTableColumnSet) subView).createColumnActions(subContext, source));
            }
        }
        return actions;
    }

    private GuiMappingContext getTableContext(GuiMappingContext context) {
        GuiMappingContext tableContext = context.getParent();
        while (tableContext != null && !tableContext.isReprCollectionTable()) {
            tableContext = tableContext.getParent();
        }
        return tableContext;
    }

    @Override
    public void createColumnsForDynamicCollection(GuiMappingContext context,
                                        DynamicColumnHost collection,
                                        SpecifierManagerIndex rowSpecifier,
                                        SpecifierManager parentSpecifier) {
        SpecifierManager subSpecifier;
        DynamicColumnHost target;
        if (context.isReprCollectionElement()) {
            subSpecifier = parentSpecifier;  //which is elementSpecifier
            target = collection;
        } else {
            subSpecifier = new SpecifierManagerDefault(parentSpecifier::getSpecifier);
            target = new DynamicColumnFactoryComposite(context, subSpecifier, collection);
        }
        for (GuiMappingContext subContext : context.getChildren()) { //for repr-collection-element, a concrete element type follows as the subContext
            GuiSwingElement view = columnMappingSet.viewTableColumn(subContext);
            createColumnForDynamicCollection(subContext, target, rowSpecifier, subSpecifier, view);
        }
        if (/*target.isNonEmpty() && */target != collection) { //always append
            collection.addColumnDynamic((DynamicColumnFactory) target);
        }
    }

    public static void createColumnForDynamicCollection(GuiMappingContext subContext,
                                                        DynamicColumnHost target,
                                                        SpecifierManagerIndex rowSpecifier,
                                                        SpecifierManager subSpecifier,
                                                        GuiSwingElement view) {
        if (view instanceof GuiSwingTableColumn) { //a value-type : List<List<String>>
            GuiSwingTableColumn column = (GuiSwingTableColumn) view;
            ObjectTableColumnSizeConcrete c = new ObjectTableColumnSizeConcrete(1, subContext, column, subSpecifier, target);
            target.addColumnDynamic(c);
        } else if (view instanceof GuiSwingTableColumnDynamic) { //further collection: List<List<List<...>>>
            ((GuiSwingTableColumnDynamic) view).createColumnDynamic(subContext, target, rowSpecifier, subSpecifier);
        } else if (view instanceof GuiSwingTableColumnSet) {   //regular object: List<List<Obj>>
            ((GuiSwingTableColumnSet) view).createColumnsForDynamicCollection(subContext,
                    target, rowSpecifier, subSpecifier);
        } else if (subContext.isReprAction() || subContext.isReprActionList()) {
            target.addActionContext(subContext);
        }
    }

    /**
     * an action for executing an {@link GuiReprActionList} with selected targets.
     *  an instance of the class is created by a collection-table instead of the column-set,
     *   because a list-action is created as a sibling node of the target collection;
     * <pre>
     *     class T {
     *         public List&lt;E&gt; collectionProp;
     *         public void action(List&lt;E&gt; selected) { ... } //a sibling member of the collection
     *     }
     * </pre>
     * <p>
     *     also, the action can return newly selected items
     * <pre>
     *         &#64;GuiListSelectionUpdater
     *         public Collection&lt;Integer&gt; action(&lt;E&gt; selected) {...}  //selected row indices
     *         &#64;GuiListSelectionUpdater(index=true)
     *         public Collection&lt;E&gt;       action(&lt;E&gt; selected) {...}  //selected row values
     *         &#64;GuiListSelectionUpdater(index=true)
     *         public Collection&lt;int[]&gt;   action(&lt;E&gt; selected) {...} //{row, column}
     * </pre>
     */
    public static class TableSelectionListAction extends GuiSwingActionDefault.ExecutionAction {
        protected TableSelectionSource source;
        protected Function<Object, TableSelectionChange> selectionChangeFactory;
        protected boolean selectionChange;

        public TableSelectionListAction(GuiMappingContext context, TableSelectionSource source) {
            super(context, () -> GuiReprValue.NONE_WITH_CACHE); //unused specifier:
                                    // for spec path outside of List, NONE... can be used
                                    // for spec path inside of complex List<List<...E...>>, the spec will be used
            this.source = source;
            selectionChangeFactory = GuiSwingTableColumnSet::getNoChange;
            if (isAutomaticSelectionAction()) {
                putValue(NAME, "*" + getValue(NAME));
                String desc = (String) getValue(Action.SHORT_DESCRIPTION);
                if (desc == null) {
                    desc = "";
                }
                putValue(Action.SHORT_DESCRIPTION,
                        desc + " The action will be automatically executed by table selection.");
            }
        }

        public TableSelectionSource getSource() {
            return source;
        }

        public Function<Object, TableSelectionChange> getSelectionChangeFactory() {
            return selectionChangeFactory;
        }

        public boolean isSelectionChange() {
            return selectionChange;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            actionPerformedAround(false, this.targetSpecifier.getSpecifier());
        }

        @Override
        public void actionPerformedWithoutCheckingRunning(ActionEvent e) {
            actionPerformedAroundWithoutCheckingRunning(false,
                    this.targetSpecifier.getSpecifier());
        }

        /**
         * action runner for automatic selection
         */
        public void actionPerformedBySelection() {
            actionPerformedAroundWithoutCheckingRunning(true,
                    this.targetSpecifier.getSpecifier());
        }

        @Override
        public Object executeAction(GuiReprValue.ObjectSpecifier specifier) {
            return actionPerformedAround(false, specifier);
        }

        protected Object actionPerformedAround(boolean autoSelection, GuiReprValue.ObjectSpecifier targetSpec) {
            if (!running.getAndSet(true)) {
                return actionPerformedAroundWithoutCheckingRunning(autoSelection, targetSpec);
            } else {
                System.err.printf("already running action \"%s\" \n", getValue(NAME));
                return null;
            }
        }

        public Object actionPerformedAroundWithoutCheckingRunning(boolean autoSelection, GuiReprValue.ObjectSpecifier targetSpec) {
            List<?> selection = source.getSelectedItems();
            String targetName = source.getTargetName();
            return executeContextTask(
                    () -> actionPerformedBody(selection, targetName, targetSpec),
                    r -> {
                        running.set(false);
                        if (!r.isPresentedWithDelay()) {
                            r.executeIfPresent(
                                    ret -> SwingUtilities.invokeLater(() ->
                                            source.selectionActionFinished(autoSelection, selectionChangeFactory.apply(ret))));
                        }
                    });
        }

        protected Object actionPerformedBody(List<?> selection, String targetName, GuiReprValue.ObjectSpecifier targetSpec) {
            GuiMappingContext context = getContext();
            return ((GuiReprActionList) context.getRepresentation())
                    .executeActionForList(context, selection, targetName, targetSpec);
        }

        /**
         * @return if true, the action will be called automatically when some cells are selected
         */
        public boolean isAutomaticSelectionAction() {
            GuiMappingContext context = getContext();
            return ((GuiReprActionList) context.getRepresentation())
                    .isAutomaticSelectionAction(context);
        }

        public void setSelectionChangeFactory(Function<Object, TableSelectionChange> selectionChangeFactory) {
            this.selectionChangeFactory = selectionChangeFactory;
        }

        public void setSelectionChangeFactoryFromContext(GuiMappingContext tableContext) {
            GuiMappingContext context = getContext();
            if (context.isReprActionList()) {
                GuiReprActionList listAction = context.getReprActionList();
                if (listAction.isSelectionChangeRowIndicesAction(context)) { //even if the element type is Integer
                    setSelectionChangeFactory(GuiSwingTableColumnSet::createChangeIndices);
                    selectionChange = true;
                } else if (listAction.isSelectionChangeRowAndColumnIndicesAction(context)) {
                    setSelectionChangeFactory(GuiSwingTableColumnSet::createChangeIndicesRowAndColumn);
                    selectionChange = true;
                } else if (listAction.isSelectionChangeAction(context, tableContext)) {
                    setSelectionChangeFactory(GuiSwingTableColumnSet::createChangeValues);
                    selectionChange = true;
                }
            } else if (context.isReprAction()) {
                GuiReprAction listAction = context.getReprAction();
                if (listAction.isSelectionChangeAction(context, tableContext)) {
                    setSelectionChangeFactory(GuiSwingTableColumnSet::createChangeValues);
                    selectionChange = true;
                }
            }
        }
    }

    /**
     * an action for execution an {@link GuiReprAction} with selected targets.
     * <pre>
     *     class T {
     *         public List&lt;E&gt; collectionProp;
     *     }
     *     class E {
     *         ...
     *         public void action() { ... }
     *     }
     * </pre>
     * <pre>
     *     class E {
     *         ...
     *         &#64;GuiListSelectionUpdater
     *         public Collection&lt;E&gt; action() { ... }
     *     }
     * </pre>
     * */
    public static class TableSelectionAction extends TableSelectionListAction {
        public TableSelectionAction(GuiMappingContext context, TableSelectionSource source) {
            super(context, source);
        }

        @Override
        protected Object actionPerformedBody(List<?> selection, String targetName, GuiReprValue.ObjectSpecifier targetSpec) {
            GuiMappingContext context = getContext();
            List<Object> os = ((GuiReprAction) context.getRepresentation())
                    .executeActionForTargets(context, selection);

            if (selectionChange) {
                List<Object> flat = new ArrayList<>();
                for (Object o : os) {
                    if (o instanceof List<?>) {
                        flat.addAll((List<?>) o);
                    }
                }
                return flat;
            } else {
                return os;
            }
        }

        @Override
        public boolean isAutomaticSelectionAction() {
            GuiMappingContext context = getContext();
            return ((GuiReprAction) context.getRepresentation())
                    .isSelectionAction(context);
        }
    }
}
