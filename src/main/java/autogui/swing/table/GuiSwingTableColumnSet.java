package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingView;

import javax.swing.*;
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
        void addColumnDynamic(ObjectTableModelColumns.DynamicColumnFactory columnFactory);
        void addMenuRowComposite(ObjectTableColumn.TableMenuComposite rowComposite);

        boolean hasDynamicColumns();

        ObjectTableColumn getColumnAt(int modelIndex);
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
    class TableSelectionChangeIndices implements TableSelectionChange {
        public Collection<Integer> indices;

        public TableSelectionChangeIndices(Collection<Integer> indices) {
            this.indices = indices;
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
     * list of selected {row, column} indices
     */
    class TableSelectionChangeIndicesRowAndColumn implements TableSelectionChange {
        public Collection<int[]> indices;

        public TableSelectionChangeIndicesRowAndColumn(Collection<int[]> indices) {
            this.indices = indices;
        }
    }

    static TableSelectionChangeNothing getNoChange(Object obj) {
        return NO_CHANGE;
    }

    @SuppressWarnings("unchecked")
    static TableSelectionChangeIndices createChangeIndices(Object obj) { //List<Integer>
        return new TableSelectionChangeIndices((Collection<Integer>) obj);
    }

    @SuppressWarnings("unchecked")
    static TableSelectionChangeIndicesRowAndColumn createChangeIndicesRowAndColumn(Object obj) { //List<int[]>
        return new TableSelectionChangeIndicesRowAndColumn((Collection<int[]>) obj);
    }

    @SuppressWarnings("unchecked")
    static TableSelectionChangeValues createChangeValues(Object obj) { //List<?>
        return new TableSelectionChangeValues((Collection<Object>) obj);
    }

    void createColumnsForDynamicCollection(GuiMappingContext context,
                                           DynamicColumnHost collection,
                                           GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
                                           GuiSwingView.SpecifierManager parentSpecifier);

    interface DynamicColumnHost extends TableColumnHost {
        void addActionContext(GuiMappingContext context);
    }
}
