package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprAction;
import autogui.base.mapping.GuiReprActionList;
import autogui.swing.GuiSwingAction;
import autogui.swing.GuiSwingActionDefault;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;

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
    public void createColumns(GuiMappingContext context, ObjectTableModel model) {
        model.addColumnRowIndex();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMappingSet.view(subContext);
            if (subView != null && subView instanceof GuiSwingTableColumn) {
                model.addColumn(((GuiSwingTableColumn) subView).createColumn(subContext));
            }
        }
    }

    @Override
    public List<Action> createColumnActions(GuiMappingContext context, TableSelectionSource source) {
        List<Action> actions = new ArrayList<>();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMappingSet.view(subContext);
            if (subView != null && subView instanceof GuiSwingAction) {
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
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            try {
                ((GuiReprActionList) context.getRepresentation())
                        .executeActionForList(context, source.getSelectedItems());
            } finally {
                source.selectionActionFinished();
                setEnabled(true);
            }
        }

        /**
         * @return if true, the action will be called automatically when some cells are selected
         */
        public boolean isAutomaticSelectionAction() {
            return ((GuiReprActionList) context.getRepresentation())
                    .isSelectionAction(context);
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
        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            try {
                ((GuiReprAction) context.getRepresentation())
                        .executeActionForTargets(context, source.getSelectedItems());
            } finally {
                source.selectionActionFinished();
                setEnabled(true);
            }
        }

        @Override
        public boolean isAutomaticSelectionAction() {
            return ((GuiReprAction) context.getRepresentation())
                    .isSelectionAction(context);
        }
    }
}
