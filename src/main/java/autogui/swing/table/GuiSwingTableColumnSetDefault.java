package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprAction;
import autogui.base.mapping.GuiReprActionList;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingAction;
import autogui.swing.GuiSwingActionDefault;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
    public void createColumns(GuiMappingContext context, ObjectTableModel model,
                              Supplier<GuiReprValue.ObjectSpecifier> rowSpecifier) {
        model.addColumnRowIndex();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMappingSet.view(subContext);
            if (subView instanceof GuiSwingTableColumn) {
                GuiSwingTableColumn column = (GuiSwingTableColumn) subView;
                model.addColumnStatic(column.createColumn(subContext, rowSpecifier));

                ObjectTableColumnDynamicFactory columnFactory = column.createColumnDynamic(subContext, rowSpecifier);
                if (columnFactory != null) {
                    model.addColumnDynamic(columnFactory);
                }
            }
        }
    }

    @Override
    public List<Action> createColumnActions(GuiMappingContext context, TableSelectionSource source) {
        List<Action> actions = new ArrayList<>();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMappingSet.view(subContext);
            if (subView instanceof GuiSwingAction) {
                actions.add(new TableSelectionAction(subContext, source));
            }
        }
        return actions;
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
