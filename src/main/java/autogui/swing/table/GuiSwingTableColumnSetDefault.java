package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprAction;
import autogui.base.mapping.GuiReprActionList;
import autogui.swing.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

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
    public void createColumns(GuiMappingContext context, TableColumnHost model,
                              GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
                              GuiSwingView.SpecifierManager specifierManager) {
        if (context.isReprCollectionElement() && context.isParentCollectionTable()) {
            model.addColumnRowIndex();
        }

        GuiSwingView.SpecifierManager subManager = context.isReprCollectionElement() ?
                specifierManager : //specifier for collection-element is an index, which will be specifierManager (usually rowSpecifier) created in the parent table
                new GuiSwingView.SpecifierManagerDefault(specifierManager::getSpecifier);  //specifier for object-pane is a sub-spec of a parent

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

                ObjectTableColumnDynamicFactory columnFactory = column.createColumnDynamic(subContext, rowSpecifier, subManager);
                if (columnFactory != null) {
                    model.addColumnDynamic(columnFactory);
                }
            } else if (subView instanceof GuiSwingTableColumnSet) {
                GuiSwingTableColumnSet set = (GuiSwingTableColumnSet) subView;

                set.createColumns(subContext, model, rowSpecifier, subManager);
            }
        }
    }

    @Override
    public List<Action> createColumnActions(GuiMappingContext context, TableSelectionSource source) {
        List<Action> actions = new ArrayList<>();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMappingSet.viewTableColumn(subContext);
            if (subView instanceof GuiSwingAction) {
                actions.add(new TableSelectionAction(subContext, source));
            } else if (subView instanceof GuiSwingTableColumnSet) {
                actions.addAll(((GuiSwingTableColumnSet) subView).createColumnActions(subContext, source));
            }
        }
        return actions;
    }

    @Override
    public void createColumnsForDynamicCollection(GuiMappingContext context,
                                        GuiSwingTableColumnCollection.ObjectTableColumnDynamicCollection collection,
                                        GuiSwingView.SpecifierManager parentSpecifier) {
        GuiSwingView.SpecifierManager subSpecifier = context.isReprCollectionElement() ?
                parentSpecifier :
                new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement view = columnMappingSet.viewTableColumn(subContext);
            if (view instanceof GuiSwingTableColumn) {
                GuiSwingTableColumn column = (GuiSwingTableColumn) view;
                collection.addColumn(subContext, column, subSpecifier);
            } else if (view instanceof GuiSwingTableColumnSet) {
                ((GuiSwingTableColumnSet) view).createColumnsForDynamicCollection(subContext,
                        collection, subSpecifier);
            }
        }
    }

    /**
     * an action for executing an {@link GuiReprActionList} with selected targets.
     *  instances of the class is created by a collection-table instead of the column-set,
     *   because of a list-action is created as a sibling node of the target collection;
     * <pre>
     *     class T {
     *         public List&lt;E&gt; collectionProp;
     *         public void action(List&lt;E&gt; selected) { ... } //a sibling member of the collection
     *     }
     * </pre>
     */
    public static class TableSelectionListAction extends GuiSwingActionDefault.ExecutionAction {
        protected TableSelectionSource source;

        public TableSelectionListAction(GuiMappingContext context, TableSelectionSource source) {
            super(context);
            this.source = source;
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

        @Override
        public void actionPerformed(ActionEvent e) {
            actionPerformedAround(false);
        }

        /**
         * action runner for automatic selection
         */
        public void actionPerformedBySelection() {
            actionPerformedAround(true);
        }

        @Override
        public void executeAction() {
            actionPerformedAround(false);
        }

        protected void actionPerformedAround(boolean autoSelection) {
            setEnabled(false);
            try {
                actionPerformedBody();
            } finally {
                source.selectionActionFinished(autoSelection);
                setEnabled(true);
            }
        }

        protected void actionPerformedBody() {
            ((GuiReprActionList) context.getRepresentation())
                    .executeActionForList(context, source.getSelectedItems());
        }

        /**
         * @return if true, the action will be called automatically when some cells are selected
         */
        public boolean isAutomaticSelectionAction() {
            return ((GuiReprActionList) context.getRepresentation())
                    .isAutomaticSelectionAction(context);
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
     * */
    public static class TableSelectionAction extends TableSelectionListAction {
        public TableSelectionAction(GuiMappingContext context, TableSelectionSource source) {
            super(context, source);
        }

        @Override
        protected void actionPerformedBody() {
            ((GuiReprAction) context.getRepresentation())
                    .executeActionForTargets(context, source.getSelectedItems());
        }

        @Override
        public boolean isAutomaticSelectionAction() {
            return ((GuiReprAction) context.getRepresentation())
                    .isSelectionAction(context);
        }
    }
}
