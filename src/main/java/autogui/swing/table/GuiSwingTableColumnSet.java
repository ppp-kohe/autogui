package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingView;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * an interface of a set of {@link GuiSwingTableColumn}.
 */
public interface GuiSwingTableColumnSet extends GuiSwingElement {
    void createColumns(GuiMappingContext context, TableColumnHost model,
                       GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
                       GuiSwingView.SpecifierManager specifierManager);

    interface TableColumnHost {
        void addColumnRowIndex();
        void addColumnStatic(ObjectTableColumn column);
        void addColumnDynamic(ObjectTableColumnDynamicFactory columnFactory);
        void addMenuRowComposite(ObjectTableColumn.TableMenuComposite rowComposite);
    }

    /**
     * create actions for sub-contexts associated to {@link autogui.base.mapping.GuiReprAction}.
     * @param context the context usually associated with {@link autogui.base.mapping.GuiReprCollectionElement}
     * @param source the table source for actions.
     *               the selected items will become targets of returned actions.
     * @return the created actions
     */
    List<Action> createColumnActions(GuiMappingContext context, TableSelectionSource source);

    /** an action target */
    interface TableSelectionSource {
        String getTargetName();
        boolean isSelectionEmpty();
        List<?> getSelectedItems();

        /**
         * the finisher for the action: the table implements the method in order to refresh
         *  selected rows.
         *  However, naive executions of the action by list selection changes will cause infinite loop.
         *  To avoid this, an impl. of the method needs to check the flag and
         *     avoid handling list selection events caused by the method.
         * @param autoSelection true if the action is automatically executed by selection changes
         * @param change specification of selection changes
         */
        void selectionActionFinished(boolean autoSelection, TableSelectionChange change);
    }

    interface TableSelectionChange { }

    TableSelectionChangeNothing NO_CHANGE = new TableSelectionChangeNothing();

    /** indicates that the selection is not changed */
    class TableSelectionChangeNothing implements TableSelectionChange { }

    /**
     * index list of selected rows
     */
    class TableSelectionChangeIndexes implements TableSelectionChange {
        public Collection<Integer> indexes;

        public TableSelectionChangeIndexes(Collection<Integer> indexes) {
            this.indexes = indexes;
        }
    }

    /**
     * list of selected row objects
     */
    class TableSelectionChangeValues implements TableSelectionChange {
        public Collection<Object> values;

        public TableSelectionChangeValues(Collection<Object> values) {
            this.values = values;
        }
    }

    /**
     * list of selected {row, column} indexes
     */
    class TableSelectionChangeIndexesRowAndColumn implements TableSelectionChange {
        public Collection<int[]> indexes;

        public TableSelectionChangeIndexesRowAndColumn(Collection<int[]> indexes) {
            this.indexes = indexes;
        }
    }

    static TableSelectionChangeNothing getNoChange(Object obj) {
        return NO_CHANGE;
    }

    @SuppressWarnings("unchecked")
    static TableSelectionChangeIndexes createChangeIndexes(Object obj) { //List<Integer>
        return new TableSelectionChangeIndexes((Collection<Integer>) obj);
    }

    @SuppressWarnings("unchecked")
    static TableSelectionChangeIndexesRowAndColumn createChangeIndexesRowAndColumn(Object obj) { //List<int[]>
        return new TableSelectionChangeIndexesRowAndColumn((Collection<int[]>) obj);
    }

    @SuppressWarnings("unchecked")
    static TableSelectionChangeValues createChangeValues(Object obj) { //List<?>
        return new TableSelectionChangeValues((Collection<Object>) obj);
    }

    void createColumnsForDynamicCollection(GuiMappingContext context,
                                           GuiSwingTableColumnCollection.ObjectTableColumnDynamicCollection collection,
                                           GuiSwingView.SpecifierManager parentSpecifier);
}
