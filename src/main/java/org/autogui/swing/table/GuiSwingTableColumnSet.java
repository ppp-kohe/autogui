package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.swing.GuiSwingElement;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.table.GuiSwingTableColumn.SpecifierManagerIndex;
import org.autogui.swing.table.ObjectTableColumn.TableMenuComposite;
import org.autogui.swing.table.ObjectTableModelColumns.DynamicColumnFactory;
import org.autogui.base.mapping.GuiReprAction;
import org.autogui.base.mapping.GuiReprCollectionElement;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

/**
 * an interface of a set of {@link GuiSwingTableColumn}.
 */
public interface GuiSwingTableColumnSet extends GuiSwingElement {
    /**
     * @param context the context, initially a collection element
     * @param model the target for appending created columns
     * @param rowSpecifier row-specifier for the table
     * @param parentSpecifier the specifier manager of the parent
     * @param specifierManager optional specifier given by the caller
     */
    void createColumns(GuiMappingContext context, TableColumnHost model,
                       SpecifierManagerIndex rowSpecifier,
                       SpecifierManager parentSpecifier,
                       SpecifierManager specifierManager);

    /** the target for adding created columns. {@link ObjectTableModelColumns} becomes the top instance */
    interface TableColumnHost {
        /** add a row index column as a static column: always delegate to the top */
        void addColumnRowIndex();

        /**
         * add a static column: always delegate to the top
         * @param column the added column
         */
        void addColumnStatic(ObjectTableColumn column);

        /**
         * add a dynamic factory as a child to the container
         * @param columnFactory the added factory
         */
        void addColumnDynamic(DynamicColumnFactory columnFactory);

        /**
         * add a menu composite: always delegate to the top
         * @param rowComposite the added composite
         */
        void addMenuRowComposite(TableMenuComposite rowComposite);

        boolean isNonEmpty();

        /**
         * obtains a column: always delegate to the top
         * @param modelIndex a model index of the column (not a child index)
         * @return  a column of the modelIndex in the table
         */
        ObjectTableColumn getColumnAt(int modelIndex);
    }

    /**
     * create actions for sub-contexts associated to {@link GuiReprAction}.
     * @param context the context usually associated with {@link GuiReprCollectionElement}
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
         * the preparation for the action: the table stops editing
         * @since 1.2
         */
        void selectionActionPrepare();

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

    /** the root interface for selection change*/
    interface TableSelectionChange { }

    TableSelectionChangeNothing NO_CHANGE = new TableSelectionChangeNothing();

    /** indicates that the selection is not changed */
    class TableSelectionChangeNothing implements TableSelectionChange {
        public TableSelectionChangeNothing() {}
    }

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

    /**
     * the method is called for creating column factories under nested table structure
     * @param context the source context
     * @param collection the adding target of created columns and actions
     * @param rowSpecifier a row specifier manager of the top table
     * @param parentSpecifier a parent specifier given by the caller
     */
    void createColumnsForDynamicCollection(GuiMappingContext context,
                                           DynamicColumnHost collection,
                                           SpecifierManagerIndex rowSpecifier,
                                           SpecifierManager parentSpecifier);

    /** a sub-interface of {@link TableColumnHost} which can support adding action contexts */
    interface DynamicColumnHost extends TableColumnHost {
        void addActionContext(GuiMappingContext context);
    }
}
