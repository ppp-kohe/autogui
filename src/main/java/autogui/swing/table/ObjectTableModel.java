package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * a table-model based on a list of row objects
 */
public class ObjectTableModel extends AbstractTableModel {
    protected List<ObjectTableColumn> columns = new ArrayList<>();
    protected List<ObjectTableColumn> staticColumns = new ArrayList<>();
    protected List<ObjectTableColumnDynamicFactory> dynamicColumns = new ArrayList<>();
    protected List<int[]> dynamicColumnSize = new ArrayList<>(); //{start,endExclusive}
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
        return columnModel;
    }

    public void addColumnRowIndex() {
        addColumn(new ObjectTableColumn.ObjectTableColumnRowIndex());
    }

    public void addColumn(ObjectTableColumn column) {
        int index = columns.size();
        columns.add(column);
        columnModel.addColumn(column.getTableColumn());
        column.getTableColumn().setModelIndex(index);
    }

    public void addColumnStatic(ObjectTableColumn column) {
        addColumn(column);
        staticColumns.add(column);
    }

    public void addColumnDynamic(ObjectTableColumnDynamicFactory columnFactory) {
        dynamicColumns.add(columnFactory);
        dynamicColumnSize.add(new int[] {0, 0});
    }

    public List<ObjectTableColumn> getColumns() {
        return columns;
    }

    public ObjectTableColumn getColumnAt(int columnIndex) {
        if (columnIndex >= columns.size()) {
            int idx = getColumnDynamicIndex(columnIndex);
            int current = getColumnDynamicIndex(columns.size() - 1);
            while (current <= idx) {
                ObjectTableColumnDynamicFactory column = dynamicColumns.get(current);
                int[] range = dynamicColumnSize.get(current);
                ObjectColumnIndex totalIndex = new ObjectColumnIndex(null, columns.size(), columns.size() - range[0]);
                int creating = range[1] - columns.size();

                for (int i = 0; i < creating; ++i) {
                    columns.add(column.createColumn(totalIndex));
                    totalIndex.increment(1);
                }
                ++current;
            }
        }
        return columns.get(columnIndex);
    }

    public int getColumnDynamicIndex(int columnIndex) {
        int idx = Collections.binarySearch(dynamicColumnSize, columnIndex, (a,b) -> {
            if (b instanceof Integer && a instanceof int[]) {
                return -compare((Integer) b, (int[]) a);
            } else if (b instanceof int[] && a instanceof Integer) {
                return compare((Integer) a, (int[]) b);
            } else {
                return 0;
            }
        });
        if (idx < 0 || idx >= dynamicColumns.size()) {
            return -1;
        } else {
            return idx;
        }
    }

    public ObjectTableColumnDynamicFactory getColumnDynamic(int columnIndex) {
        int idx = getColumnDynamicIndex(columnIndex);
        if (idx == -1) {
            return null;
        } else {
            return dynamicColumns.get(idx);
        }
    }

    private int compare(int n, int[] r) {
        if (r[0] <= n && n < r[1]) {
            return 0;
        } else if (n < r[0]) {
            return -1;
        } else {
            return 1;
        }
    }


    @Override
    public int getColumnCount() {
        int i = 0;
        int sum = staticColumns.size();
        Object list = getCollectionFromSource();
        for (ObjectTableColumnDynamicFactory columnFactory : dynamicColumns) {
            int n = columnFactory.getColumnCount(list);
            int[] r = dynamicColumnSize.get(i);
            r[0] = sum;
            sum += n;
            r[1] = sum;
            ++i;
        }
        return sum;
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
        }
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
            return ((List<?>) list).get(row);
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
        int rows = getRowCount();
        int cols = getColumnCount();

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
}
