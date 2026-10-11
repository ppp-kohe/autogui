package org.autogui.swing.table;

import org.autogui.base.mapping.*;
import org.autogui.swing.*;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingView.SpecifierManagerDefault;
import org.autogui.swing.table.GuiSwingTableColumn.SpecifierManagerIndex;
import org.autogui.swing.table.GuiSwingTableColumnCollection.DynamicColumnFactoryCollectionRoot;
import org.autogui.swing.table.GuiSwingTableColumnCollection.DynamicColumnFactoryComposite;
import org.autogui.swing.table.GuiSwingTableColumnCollection.ObjectTableColumnSizeConcrete;
import org.autogui.swing.table.ObjectTableColumn.TableMenuComposite;
import org.autogui.swing.table.ObjectTableModelColumns.DynamicColumnFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * the default implementation of table-column-set associated with {@link GuiReprCollectionElement}.
 *
 */
public class GuiSwingTableColumnSetDefault implements GuiSwingTableColumnSet {
    protected GuiSwingMapperSet columnMappingSet;

    public GuiSwingTableColumnSetDefault(GuiSwingMapperSet columnMappingSet) {
        this.columnMappingSet = columnMappingSet;
    }

    /**
     * the class supports 2 cases:
     * <ul>
     *     <li>for a collection-element under a table: <code>class Tbl { List&lt;E&gt; table; }</code>.
     *            it supposes the root element directly called from {@link GuiSwingViewCollectionTable},
     *              and then specifierManager is given as the specifier factory for the element (== rowSpecifier).
     *            it creates a {@link DynamicColumnFactoryCollectionRoot} and
     *             use specifierManager as the parent specifier for sub-columns or sub-lists</li>
     *     <li> for an object-composition under a table: <code>List&lt;E&gt; table; class E { ... }</code>.
     *            this is the case of recursive call.
     *            it creates a sub-specifier manager with the parentSpecifier and a {@link DynamicColumnFactoryComposite}.
     *            Note the nested cases are not treated because a sub-object is composed by a property-pane: property(obj)
     *            </li>
     * </ul>
     * for both cases, sub-contexts are tested with {@link GuiSwingMapperSet}.
     * <ul>
     *     <li>{@link GuiSwingTableColumn}: a static column. <code>V of List&lt;V&gt; or List&lt;E&gt; and class E { V fld; }</code>.
     *       it calls {@link GuiSwingTableColumn#createColumn(GuiMappingContext, GuiSwingTableColumn.SpecifierManagerIndex, GuiSwingView.SpecifierManager)}
     *         and adds the column as a static column</li>
     *     <li>{@link GuiSwingTableColumnDynamic}: a dynamic column, a nested collection, or a property-pane.
     *            <code>List&lt;List&lt;E&gt;&gt;</code> or <code>List&lt;E&gt; class E { C prop; }</code>.
     *       it calls {@link GuiSwingTableColumnDynamic#createColumnDynamic(GuiMappingContext, GuiSwingTableColumnSet.TableColumnHost, GuiSwingTableColumn.SpecifierManagerIndex, GuiSwingView.SpecifierManager)}</li>
     *     <li>{@link GuiSwingTableColumnSet}: a composition of static columns. <code>List&lt;E&gt; and class E {...}</code>
     *         it causes a recursive call as the above 2nd case</li>
     *     <li>a list action: <code>act(List&lt;E&gt;)</code>. it add the context as an action.
     *          Note other actions are treated by {@link #createColumnActions(GuiMappingContext, GuiSwingTableColumnSet.TableSelectionSource)}</li>
     * </ul>
     * @param context the context, initially a collection element
     * @param parentModel the target of adding columns, passed by sender
     * @param rowSpecifier row-specifier for the table
     * @param parentSpecifier the specifier manager of the parent
     * @param specifierManager optional specifier given by the caller
     */
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
            if (subView instanceof GuiSwingTableColumn column) {
                ObjectTableColumn columnStatic = column.createColumn(subContext, rowSpecifier, subManager);
                if (columnStatic != null) {
                    model.addColumnStatic(columnStatic);
                }
            } else if (subView instanceof GuiSwingTableColumnDynamic subViewDyn) {
                subViewDyn.createColumnDynamic(
                        subContext, model, rowSpecifier, subManager);
            } else if (subView instanceof GuiSwingTableColumnSet set) {

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

    /**
     * support actions of an element object:
     *  <pre>
     *      List&lt;E&gt;
     *      class E { C obj; void action1() {...} }
     *      class C {        void action2() {...} }
     *   </pre>
     * @param context the context usually associated with {@link GuiReprCollectionElement}
     * @param source the table source for actions.
     *               the selected items will become targets of returned actions.
     * @return list of created actions
     */
    @Override
    public List<Action> createColumnActions(GuiMappingContext context, TableSelectionSource source) {
        List<Action> actions = new ArrayList<>();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMappingSet.viewTableColumn(subContext);
            if (subView instanceof GuiSwingAction action) {
                TableSelectionAction a = new TableSelectionAction(subContext, source);
                actions.add(a);

                GuiMappingContext tableContext = context.getAscendantTableContext(false);
                if (tableContext != null) { //table context
                    a.addActionResultHandlersForList(tableContext, columnMappingSet);
                }

            } else if (subView instanceof GuiSwingTableColumnSet subViewColSet) {
                actions.addAll(subViewColSet.createColumnActions(subContext, source));
            }
        }
        return actions;
    }

    /**
     * <ul>
     *     <li>a collection element in a nested list called from {@link GuiSwingTableColumnCollection}:
     *         it uses the collection as a parent</li>
     *     <li>a composition: <code>List&lt;List&lt;E&gt;&gt;</code>.
     *         it creates a {@link DynamicColumnFactoryComposite} as a parent and add it to the collection</li>
     * </ul>
     *
     * @param context the source context
     * @param collection the adding target of created columns and actions
     * @param rowSpecifier a row specifier manager of the top table
     * @param parentSpecifier a parent specifier given by the caller
     */
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
        if (view instanceof GuiSwingTableColumn column) { //a value-type : List<List<String>>
            ObjectTableColumnSizeConcrete c = new ObjectTableColumnSizeConcrete(1, subContext, column, subSpecifier, target);
//            if (target.isUnderMultidimentionalList()) {
//
//            } else {
                target.addColumnDynamic(c);
//            }
        } else if (view instanceof GuiSwingTableColumnDynamic viewDyn) { //further collection: List<List<List<...>>>
            viewDyn.createColumnDynamic(subContext, target, rowSpecifier, subSpecifier);
        } else if (view instanceof GuiSwingTableColumnSet viewSet) {   //regular object: List<List<Obj>>
            viewSet.createColumnsForDynamicCollection(subContext,
                    target, rowSpecifier, subSpecifier);
        } else if (subContext.isReprAction() || subContext.isReprActionList()) {
            target.addActionContext(subContext);
        }
    }

    /**
     * the execution-parameter with the aut-selection flag
     * @since 1.9
     */
    public static class ExecutionActionParametersTableSelectionList extends GuiSwingActionDefault.ExecutionActionParameters {
        protected boolean autoSelection;
        public ExecutionActionParametersTableSelectionList(ActionEvent originalEvent, GuiReprValue.ObjectSpecifier specifier, boolean autoSelection) {
            super(originalEvent, specifier);
            this.autoSelection = autoSelection;
        }

        public boolean isAutoSelection() {
            return autoSelection;
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
        @Serial private static final long serialVersionUID = 1L;

        protected ActionResultTableSelectionListActionSelectionChange selectionChange;

        @SuppressWarnings("this-escape")
        public TableSelectionListAction(GuiMappingContext context, TableSelectionSource source) {
            super(context, () -> GuiReprValue.NONE_WITH_CACHE); //unused specifier:
                                    // for spec path outside of List, NONE... can be used
                                    // for spec path inside of complex List<List<...E...>>, the spec will be used
            selectionChange = new ActionResultTableSelectionListActionSelectionChange(context, source, GuiSwingTableColumnSet::getNoChange);
            addActionResultHandlers(List.of(selectionChange));

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
            return selectionChange.getTable();
        }

        public Function<Object, TableSelectionChange> getSelectionChangeFactory() {
            return selectionChange.getConversion();
        }

        public boolean isSelectionChange() {
            return selectionChange.isSelectionChange();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            actionPerformedAround(
                    new ExecutionActionParametersTableSelectionList(e, this.targetSpecifier.getSpecifier(), false));
        }

        @Override
        public void actionPerformedWithoutCheckingRunning(ActionEvent e) {
            actionPerformedAroundWithoutCheckingRunning(
                    new ExecutionActionParametersTableSelectionList(e, this.targetSpecifier.getSpecifier(), false));
        }

        /** action runner for automatic selection */
        public void actionPerformedBySelection() {
            actionPerformedBySelection(null);
        }

        /**
         * action runner for automatic selection
         * @param e an optional action-event
         * @since 1.8
         */
        public void actionPerformedBySelection(ActionEvent e) {
            actionPerformedAroundWithoutCheckingRunning(
                    new ExecutionActionParametersTableSelectionList(e, this.targetSpecifier.getSpecifier(), true));
        }

        @Override
        public Object executeAction(GuiReprValue.ObjectSpecifier specifier, ActionEvent e) {
            return actionPerformedAround(new ExecutionActionParametersTableSelectionList(e, specifier, false));
        }

        protected Object actionPerformedAround(ExecutionActionParametersTableSelectionList parameters) {
            if (!running.getAndSet(true)) {
                getSource().selectionActionPrepare();
                return actionPerformedAroundWithoutCheckingRunning(parameters);
            } else {
                System.err.printf("already running action \"%s\" \n", getValue(NAME));
                return null;
            }
        }

        public Object actionPerformedAroundWithoutCheckingRunning(ExecutionActionParametersTableSelectionList parameters) {
            List<?> selection = getSource().getSelectedItems();
            String targetName = getSource().getTargetName();
            Supplier<Object> body = () -> executeContextTask(
                    () -> actionPerformedBody(selection, targetName, parameters.getSpecifier()),
                    r -> {
                        running.set(false);
                        afterActionPerformedWithoutCheckingRunning(parameters, r);
                    });
            if (!parameters.isAutoSelection() && needToConfirm) {
                return executeWithConfirmReturn(parameters.getOriginalEvent(), body, () -> {
                    running.set(false);
                    return null;
                });
            } else {
                return body.get();
            }
        }

        protected Object actionPerformedBody(List<?> selection, String targetName, GuiReprValue.ObjectSpecifier targetSpec) {
            GuiMappingContext context = getContext();
            var res = ((GuiReprActionList) context.getRepresentation())
                    .executeActionForList(context, selection, targetName, targetSpec);
            if (isSelectionChange() && res != null) {
                return res.result();
            } else {
                return res;
            }
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
            selectionChange.setSelectionChangeFactory(selectionChangeFactory);
        }

        public void setSelectionChangeFactoryFromContext(GuiMappingContext tableContext) {
            selectionChange.setSelectionChangeFactoryFromContext(tableContext);
        }

        /**
         * add a new {@link org.autogui.swing.table.GuiSwingTableColumnCollection.ActionResultHandlerEditStarterForList}
         *   if the context has an editor action feature.
         * @param tableContext the table-context
         * @param mapperSet the view factory for the editor view.
         * @since 1.9
         */
        public void addActionResultHandlersForList(GuiMappingContext tableContext, GuiSwingMapperSet mapperSet) {
            setSelectionChangeFactoryFromContext(tableContext);
            var actionContext = getContext();
            if (actionContext.isReprActionOrActionListEditAction()) {
                var editorContext = GuiSwingActionDefault.createEditorContext(actionContext);
                if (mapperSet != null && mapperSet.view(editorContext) instanceof GuiSwingView paneSource) {
                    addActionResultHandlers(List.of(
                            new GuiSwingTableColumnCollection.ActionResultHandlerEditStarterForList(actionContext, editorContext, paneSource)));
                }
            }
        }
    }

    /**
     * the table-selection result action
     * @since 1.9
     */
    public static class ActionResultTableSelectionListActionSelectionChange extends GuiSwingActionDefault.ActionResultTableSelectionConversion {
        protected boolean selectionChange;

        public ActionResultTableSelectionListActionSelectionChange(GuiMappingContext context, TableSelectionSource table, Function<Object, TableSelectionChange> conversion) {
            super(context, table, conversion);
            this.selectionChange = false;
        }

        public boolean isSelectionChange() {
            return selectionChange;
        }

        @Override
        protected boolean isAutoSelection(GuiSwingActionDefault.ExecutionActionParameters parameters) {
            if (parameters instanceof ExecutionActionParametersTableSelectionList paramList) {
                return paramList.isAutoSelection();
            } else {
                return false;
            }
        }

        public void setSelectionChangeFactory(Function<Object, TableSelectionChange> selectionChangeFactory) {
            this.conversion = selectionChangeFactory;
        }

        public void setSelectionChangeFactoryFromContext(GuiMappingContext tableContext) {
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
     * {@link #isSelectionChange()} == true:
     * <pre>
     *     class E {
     *         ...
     *         &#64;GuiListSelectionUpdater
     *         public Collection&lt;E&gt; action() { ... }
     *     }
     * </pre>
     * */
    public static class TableSelectionAction extends TableSelectionListAction {
        @Serial private static final long serialVersionUID = 1L;

        public TableSelectionAction(GuiMappingContext context, TableSelectionSource source) {
            super(context, source);
        }

        @Override
        protected Object actionPerformedBody(List<?> selection, String targetName, GuiReprValue.ObjectSpecifier targetSpec) {
            GuiMappingContext context = getContext();
            List<GuiReprAction.TargetAndResult> os = ((GuiReprAction) context.getRepresentation())
                    .executeActionForTargets(context, selection);

            if (isSelectionChange()) {
                List<Object> flat = new ArrayList<>();
                for (GuiReprAction.TargetAndResult o : os) {
                    if (o.result() instanceof Collection<?> resList) {
                        flat.addAll(resList);
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
