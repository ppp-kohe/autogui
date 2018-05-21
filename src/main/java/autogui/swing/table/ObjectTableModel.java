package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * a table-model based on a list of row objects
 */
public class ObjectTableModel extends AbstractTableModel implements GuiSwingTableColumnSet.TableColumnHost {
    protected List<ObjectTableColumn> columns = new ArrayList<>();
    protected List<ObjectTableColumn> staticColumns = new ArrayList<>();
    protected List<ObjectTableColumnDynamic> dynamicColumns = new ArrayList<>();
    protected boolean dynamicColumnSizeNeedsUpdate;
    protected JTable table;
    protected Supplier<Object> source;

    protected DefaultTableColumnModel columnModel;
    /** cached computed values */
    protected Object[][] data;

    protected Consumer<Runnable> futureWaiter = Runnable::run;

    public static Object NULL_CELL = new Object();


    public void setFutureWaiter(Consumer<Runnable> futureWaiter) {
        this.futureWaiter = futureWaiter;
    }

    public Consumer<Runnable> getFutureWaiter() {
        return futureWaiter;
    }

    public void setTable(JTable table) {
        this.table = table;
    }

    public JTable getTable() {
        return table;
    }

    public DefaultTableColumnModel getColumnModel() {
        if (columnModel == null) {
            columnModel = new DefaultTableColumnModel();
            //TODO debug
            columnModel.addColumnModelListener(new TableColumnModelListener() {
                @Override
                public void columnAdded(TableColumnModelEvent e) {
                    System.err.println("columnAdded " + e.getToIndex());
                }

                @Override
                public void columnRemoved(TableColumnModelEvent e) {
                    System.err.println("columnAdded " + e.getFromIndex());
                }

                @Override
                public void columnMoved(TableColumnModelEvent e) {
                    System.err.println("columnAdded " + e.getFromIndex() + "->" + e.getToIndex());
                }

                @Override
                public void columnMarginChanged(ChangeEvent e) {
                }

                @Override
                public void columnSelectionChanged(ListSelectionEvent e) {
                }
            });
        }
        return columnModel;
    }

    @Override
    public void addColumnRowIndex() {
        addColumnStatic(new ObjectTableColumn.ObjectTableColumnRowIndex());
    }

    @Override
    public void addColumnStatic(ObjectTableColumn column) {
        int index = columns.size();
        columns.add(column);
        column.getTableColumn().setModelIndex(index);
        getColumnModel().addColumn(column.getTableColumn());
        staticColumns.add(column);
    }

    @Override
    public void addColumnDynamic(ObjectTableColumnDynamicFactory columnFactory) {
        dynamicColumns.add(new ObjectTableColumnDynamic(columnFactory));
        dynamicColumnSizeNeedsUpdate = true;
    }

    public void addColumnFromDynamic(int index, ObjectTableColumn column) {
        columns.add(index, column);
    }

    public void removeColumnFromDynamic(ObjectTableColumn column) {
        columns.remove(column);
    }

    public List<ObjectTableColumn> getColumns() {
        return columns;
    }

    public ObjectTableColumn getColumnAt(int columnIndex) {
        if (columnIndex < staticColumns.size()) {
            return staticColumns.get(columnIndex);
        } else {
            return getColumnDynamic(columnIndex)
                    .getColumnAt(columnIndex);
        }
    }

    public ObjectTableColumnDynamic getColumnDynamic(int columnIndex) {
        for (ObjectTableColumnDynamic d : dynamicColumns) {
            if (d.containsIndex(columnIndex)) {
                return d;
            }
        }
        return null;
    }

    @Override
    public int getColumnCount() {
        if (dynamicColumns.isEmpty()) {
            System.err.println("getColumnCount: " + staticColumns.size());
            return staticColumns.size();
        } else {
            ObjectTableColumnDynamic d = dynamicColumns.get(dynamicColumns.size() - 1);
            System.err.println("getColumnCount !: " + d.getEndIndexExclusive());
            return d.getEndIndexExclusive();
        }
    }

    public int getColumnCountUpdated() {
        dynamicColumnSizeNeedsUpdate = false;
        int i = staticColumns.size();
        Object list = getCollectionFromSource();
        for (ObjectTableColumnDynamic d : dynamicColumns) {
            ObjectTableColumnDynamicDiff diff = d.update(i, list);
            diff.applyTo(this);
            i = diff.endIndexExclusive;
        }
        System.err.println("getColumnUpdated: " + i);
        return i;
    }

    //////////// table initialization

    public JScrollPane initTableWithScroll() {
        JTable table = new JTable(this, getColumnModel());
        return initTable(table);
    }

    public JScrollPane initTable(JTable table) {
        initTableWithoutScrollPane(table);
        return initTableScrollPane(table);
    }

    public void initTableWithoutScrollPane(JTable table) {
        if (table.getModel() != this) {
            table.setModel(this);
        }
        if (table.getColumnModel() != this.getColumnModel()) {
            table.setColumnModel(getColumnModel());
        }
        table.setAutoCreateRowSorter(true);
        initTableRowSorter(table);
        initTableRowHeight(table);
    }

    public void initTableRowSorter(JTable table) {
        /* TODO dynamically updates sorter
        RowSorter<? extends TableModel> sorter = table.getRowSorter();
        if (sorter instanceof TableRowSorter<?>) {
            TableRowSorter<? extends TableModel> tableSorter = (TableRowSorter<? extends TableModel>) sorter;
            tableSorter.setSortsOnUpdates(true);

            for (int i = 0, l = getColumns().size(); i < l; ++i) {
                Comparator<?> comp = getColumns().get(i).getComparator();
                if (comp != null) {
                    tableSorter.setComparator(i, comp);
                }
            }
        }*/
    }

    public void initTableRowHeight(JTable table) {
        int height = Math.max(table.getRowHeight() + 3,
                getColumns().stream()
                .mapToInt(ObjectTableColumn::getRowHeight)
                .max().orElse(0));
        table.setRowHeight(height);
    }

    public JScrollPane initTableScrollPane(JComponent table) {
        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        int width = getColumns().stream()
                .mapToInt(e -> e.getTableColumn().getWidth())
                .sum();
        scrollPane.setPreferredSize(new Dimension(width, Math.max(scrollPane.getPreferredSize().height, 100)));
        return scrollPane;
    }

    ////////////

    public void setSource(Supplier<Object> source) {
        this.source = source;
    }

    public Object getCollectionFromSource() {
        return source == null ? null : source.get();
    }

    @Override
    public int getRowCount() {
        if (data == null) {
            buildDataArray();
        }
        return data.length;
    }

    public int getRowCountUpdated() {
        Object list = getCollectionFromSource();
        if (list instanceof List<?>) {
            return ((List) list).size();
        } else {
            return 0;
        }
    }

    public Object getRowAtIndex(int row) {
        Object list = getCollectionFromSource();
        if (list instanceof List<?>) {
            List<?> l = (List<?>) list;
            if (row < l.size()) {
                return l.get(row);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    ////////////

    @Override
    public String getColumnName(int column) {
        return super.getColumnName(column);
    }


    public boolean buildDataArray() {
        int rows = getRowCountUpdated();
        int cols = getColumnCountUpdated();

        if (data == null ||
                data.length != rows ||
                data.length > 0 && data[0].length != cols) {
            data = new Object[rows][cols];
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            if (data == null) {
                buildDataArray();
            }
            return getValueAtWithError(rowIndex, columnIndex);
        } catch (Exception ex) {
            buildDataArray();
            return getValueAtWithError(rowIndex, columnIndex);
        }
    }

    /**
     *  executed under the event thread.
     *  <p>
     *  the cell value is obtained via the specified {@link ObjectTableColumn}
     *   with the specified row of {@link #getCollectionFromSource()}.
     *  the cell value is cached and obtained by {@link #takeValueFromSource(Object[], int, int)} at the first time.
     *
     * @param rowIndex the target row
     * @param columnIndex the target column
     * @return the cell value, nullable (waiting for obtaining the value)
     */
    public Object getValueAtWithError(int rowIndex, int columnIndex) {
        Object[] rowData = data[rowIndex];
        Object cellData = rowData[columnIndex];
        if (cellData == null) {
            cellData = takeValueFromSource(rowData, rowIndex, columnIndex);
        }
        if (cellData.equals(NULL_CELL)) {
            return null;
        } else {
            return cellData;
        }
    }

    /**
     *  the column's {@link ObjectTableColumn#getCellValue(Object, int, int)} might return
     *   a {@link Future} object and
     *   then it waits completion of the task in {@link #takeValueFromSourceFuture(Object[], int, int, Future)}.
     *   The method is run in {@link #getFutureWaiter()} (default is just call the method)
     * @param rowData the row array which the returned cell value will be stored
     * @param rowIndex the row index
     * @param columnIndex the column index
     * @return the cell value, nullable
     */
    public Object takeValueFromSource(Object[] rowData, int rowIndex, int columnIndex) {
        Object rowObject = getRowAtIndex(rowIndex);
        Object cellObject = getColumnAt(columnIndex)
                .getCellValue(rowObject, rowIndex, columnIndex);

        if (cellObject instanceof Future<?>) {
            rowData[columnIndex] = NULL_CELL;
            futureWaiter.accept(() -> takeValueFromSourceFuture(rowData, rowIndex, columnIndex, (Future<?>) cellObject));
            return rowData[columnIndex];
        } else if (cellObject == null) {
            rowData[columnIndex] = NULL_CELL;
            return NULL_CELL;
        } else {
            rowData[columnIndex] = cellObject;
            return cellObject;
        }
    }

    /**
     * wait the completion of a task up to 1 sec. ,
     *   store the result to the array, and notify the update (as a later event process).
     *   If it overs the time, it just throws a runtime-exception.
     * @param rowData the stored array
     * @param rowIndex  the row index
     * @param columnIndex the column index
     * @param future the task
     */
    public void takeValueFromSourceFuture(Object[] rowData, int rowIndex, int columnIndex, Future<?> future) {
        try {
            Object cellObject = future.get(1, TimeUnit.SECONDS);
            if (cellObject == null) {
                cellObject = NULL_CELL;
            }
            rowData[columnIndex] = cellObject;
            SwingUtilities.invokeLater(() -> {
                fireTableCellUpdated(rowIndex, columnIndex);
            });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    ///////////////////////


    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            setValueAtWithError(aValue, rowIndex, columnIndex);
        } catch (Exception ex) {
            buildDataArray();
            setValueAtWithError(aValue, rowIndex, columnIndex);
        }
    }

    /** executed under event thread.
     * @param aValue the column value to be set
     * @param rowIndex the target row
     * @param columnIndex the target column
     */
    public void setValueAtWithError(Object aValue, int rowIndex, int columnIndex) {
        Object[] rowData = data[rowIndex];
        rowData[columnIndex] = (aValue == null ? NULL_CELL : aValue);

        Object rowObject = getRowAtIndex(rowIndex);
        offerValueForSource(rowObject, aValue, rowIndex, columnIndex);
    }

    public void offerValueForSource(Object rowObject, Object aValue, int rowIndex, int columnIndex) {
        Future<?> future = getColumnAt(columnIndex)
                .setCellValue(rowObject, rowIndex, columnIndex, aValue);
        futureWaiter.accept(() -> offsetValueForSourceFuture(rowObject, aValue, rowIndex, columnIndex, future));
    }

    public void offsetValueForSourceFuture(Object rowObject, Object aValue, int rowIndex, int columnIndex, Future<?> future) {
        try {
            if (future != null) {
                future.get(2, TimeUnit.SECONDS);
            }
            SwingUtilities.invokeLater(() -> refreshRow(rowIndex));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    ///////////////////////

    /** executed under event thread */
    public void refreshData() {
        if (!buildDataArray()) {
            for (int i = 0, l = data.length; i < l; ++i) {
                clearRowData(i);
            }
            fireTableRowsUpdatedAll();
        } else {
            //changed row size
            fireTableDataChanged();
        }
    }

    /** executed under event thread
     * @param rowIndex the target row index
     */
    public void refreshRow(int rowIndex) {
        clearRowData(rowIndex);
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public void clearRowData(int rowIndex) {
        Object[] rowData = data[rowIndex];
        //clear row data for re-taking value from source
        Arrays.fill(rowData, null);
    }

    /** executed under event thread
     * @param rowIndexes the target rows
     */
    public void refreshRows(int... rowIndexes) {
        int min = -1;
        int max = -1;
        for (int rowIndex : rowIndexes) {
            if (min == -1 || rowIndex < min) {
                min = rowIndex;
            }
            if (max == -1 || rowIndex > max) {
                max = rowIndex;
            }
            clearRowData(rowIndex);
        }
        if (min != -1) {
            fireTableRowsUpdated(min, max);
        }
    }

    /** executed under event thread
     * @param columnIndexes the target columns
     */
    public void refreshColumns(Collection<Integer> columnIndexes) {
        refreshColumns(columnIndexes.stream()
                .mapToInt(Integer::intValue)
                .toArray());
    }

    /** executed under event thread
     * @param columnIndexes the target columns
     */
    public void refreshColumns(int... columnIndexes) {
        for (Object[] rowData : data) {
            for (int columnIndex : columnIndexes) {
                rowData[columnIndex] = null;
            }
        }
        fireTableRowsUpdatedAll();
    }

    /** update existing rows without changing selection */
    public void fireTableRowsUpdatedAll() {
        int rows = getRowCount();
        if (rows > 0) {
            SwingUtilities.invokeLater(() -> {
                int rows2 = Math.min(rows, getRowCount());
                if (rows2 > 0) {
                    fireTableRowsUpdated(0, rows2 - 1);
                }
            });
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return getColumnAt(columnIndex)
                .getTableColumn().getCellEditor() != null;
    }

    ///////////

    public PopupExtension.PopupMenuBuilder getBuilderForRowsOrCells(JTable table, List<ObjectTableColumn> cols, boolean row) {
        return new PopupCategorized(() -> getBuildersForRowsOrCells(table, cols, row), null,
                new MenuBuilderWithEmptySeparator());
    }

    /**
     * a menu builder for {@link TableTargetCellAction}s
     */
    public interface PopupMenuBuilderForRowsOrCells {
        void build(PopupExtension.PopupMenuFilter filter, Consumer<Object> menu);
    }

    public List<PopupCategorized.CategorizedMenuItem> getBuildersForRowsOrCells(JTable table, List<ObjectTableColumn> cols, boolean row) {
        Map<ObjectTableColumn.TableMenuCompositeShared, List<ObjectTableColumn.TableMenuComposite>> rows
                = new LinkedHashMap<>();
        cols.forEach(c ->
                (row ? c.getCompositesForRows() :
                        c.getCompositesForCells()).forEach(cmp ->
                        rows.computeIfAbsent(cmp.getShared(), key -> new ArrayList<>())
                                .add(cmp)));

        return rows.entrySet().stream()
                .flatMap(e ->
                        e.getKey().composite(table, e.getValue(), row)
                                    .stream())
                .collect(Collectors.toList());

    }


    public static class MenuBuilderWithEmptySeparator extends MenuBuilder {
        @Override
        public boolean addMenuSeparator(AddingProcess process, boolean nonEmpty) {
            if (process.isSeparatorNeeded() && nonEmpty) {
                process.setSeparatorNeeded(false);
                process.getMenu().accept(createSeparator());
                return true;
            } else {
                return false;
            }

        }

        public JComponent createSeparator() {
            JComponent sep = new JPanel();
            sep.setPreferredSize(new Dimension(10, 6));
            sep.setOpaque(false);
            sep.setBorder(BorderFactory.createEmptyBorder());
            return sep;
        }
    }

    /**
     * a builder accepting a {@link TableTargetCellAction} and wrapping it to a {@link TableTargetCellExecutionAction}.
     */
    public static class CollectionRowsAndCellsActionBuilder implements PopupExtension.PopupMenuFilter {
        protected PopupExtension.PopupMenuFilter filter;
        protected GuiReprCollectionTable.TableTargetCell target;

        public CollectionRowsAndCellsActionBuilder(JTable table, PopupExtension.PopupMenuFilter filter) {
            this.filter = filter;
            target = new TableTargetCellForJTable(table);
        }

        @Override
        public Object convert(Object item) {
            if (item instanceof TableTargetCellAction) {
                return filter.convert(new TableTargetCellExecutionAction((TableTargetCellAction) item, target));
            } else {
                return null; //disabled
            }
        }
    }

    /**
     * an action for wrapping {@link TableTargetCellAction}
     */
    public static class TableTargetCellExecutionAction extends AbstractAction
            implements PopupCategorized.CategorizedMenuItemAction {
        protected TableTargetCellAction action;
        protected GuiReprCollectionTable.TableTargetCell target;

        public TableTargetCellExecutionAction(TableTargetCellAction action, GuiReprCollectionTable.TableTargetCell target) {
            this.action = action;
            this.target = target;
        }


        @Override
        public Object getValue(String key) {
            return action.getValue(key);
        }

        public TableTargetCellAction getAction() {
            return action;
        }

        public GuiReprCollectionTable.TableTargetCell getTarget() {
            return target;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.actionPerformedOnTableCell(e, target);
        }

        @Override
        public String getCategory() {
            return action.getCategory();
        }

        @Override
        public String getSubCategory() {
            return action.getSubCategory();
        }
    }

    public static class ObjectTableColumnDynamic {
        protected ObjectTableColumnDynamicFactory factory;
        protected int startIndex;
        protected ObjectTableColumnIndex index;
        protected List<ObjectTableColumn> columns;

        public ObjectTableColumnDynamic(ObjectTableColumnDynamicFactory factory) {
            this.factory = factory;
            this.columns = new ArrayList<>();
            this.index = new ObjectTableColumnIndex(null, 0, 0);
        }

        public void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndexExclusive() {
            return index.getTotalIndex();
        }

        public boolean containsIndex(int index) {
            return getStartIndex() <= index && index < getEndIndexExclusive();
        }

        public ObjectTableColumnDynamicDiff update(int startIndex, Object list) {
            this.startIndex = startIndex;
            int newSize = factory.getColumnCount(list);
            int preSize = columns.size();

            if (preSize < newSize) {
                this.index = new ObjectTableColumnIndex(null, startIndex, preSize);
                int added = newSize - preSize;
                List<ObjectTableColumn> newCols = new ArrayList<>(added);
                for (int i = 0; i < added; ++i) {
                    newCols.add(factory.createColumn(index));
                    index.increment(1);
                }
                columns.addAll(newCols);
                return new ObjectTableColumnDynamicDiff(false,
                        startIndex + preSize, startIndex + newSize, newCols);

            } else if (preSize > newSize) {
                this.index = new ObjectTableColumnIndex(null, startIndex + newSize, newSize);
                List<ObjectTableColumn> remCols = columns.subList(newSize, columns.size());
                columns.removeAll(remCols);
                return new ObjectTableColumnDynamicDiff(true, startIndex + newSize,
                        startIndex + newSize, remCols);

            } else {
                this.index = new ObjectTableColumnIndex(null, startIndex + newSize, newSize);
                return new ObjectTableColumnDynamicDiff(false,
                        startIndex + newSize,startIndex + newSize,
                        Collections.emptyList());
            }
        }

        public ObjectTableColumn getColumnAt(int columnIndex) {
            return columns.get(columnIndex - startIndex);
        }
    }

    public static class ObjectTableColumnDynamicDiff {
        /**
         * false means adding and columns are added columns, and
         *  true means removing and columns are removed columns
         */
        public boolean remove;
        /**
         * the start index of columns
         */
        public int startIndex;
        public int endIndexExclusive;
        public List<ObjectTableColumn> columns;

        public ObjectTableColumnDynamicDiff(boolean remove, int startIndex, int endIndexExclusive,
                                            List<ObjectTableColumn> columns) {
            this.remove = remove;
            this.startIndex = startIndex;
            this.endIndexExclusive = endIndexExclusive;
            this.columns = columns;
        }

        public void applyTo(ObjectTableModel model) {
            if (remove) {
                columns.forEach(model::removeColumnFromDynamic);
            } else {
                int i = startIndex;
                for (ObjectTableColumn col : columns) {
                    model.addColumnFromDynamic(i, col);
                    ++i;
                }
            }
            applyTo(model.getColumnModel());
        }

        public void applyTo(DefaultTableColumnModel model) {
            if (remove) {
                for (ObjectTableColumn col : columns) {
                    model.removeColumn(col.getTableColumn());
                }
            } else {
                int to = startIndex;
                for (ObjectTableColumn col : columns) {
                    int from = model.getColumnCount();
                    TableColumn tableColumn = col.getTableColumn();
                    tableColumn.setModelIndex(model.getColumnCount()); //TODO shift model columnIndex?
                    model.addColumn(tableColumn);
                    model.moveColumn(from, to);
                    ++to;
                }
            }
        }
    }

}
