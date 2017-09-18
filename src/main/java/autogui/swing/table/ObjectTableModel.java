package autogui.swing.table;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ObjectTableModel extends AbstractTableModel {
    protected Supplier<List<?>> sourceSupplier;
    protected List<ObjectTableColumn> columns;
    protected JTable table;

    protected List<?> source;

    protected DefaultTableColumnModel columnModel;
    /** cached computed values */
    protected Object[][] data;

    protected Consumer<Runnable> futureWaiter = Runnable::run;

    public static Object NULL_CELL = new Object();

    public ObjectTableModel(Supplier<List<?>> sourceSupplier) {
        this(sourceSupplier, new ArrayList<>());
    }

    public ObjectTableModel(Supplier<List<?>> sourceSupplier, List<ObjectTableColumn> columns) {
        this(sourceSupplier, columns, null);
    }

    public ObjectTableModel(Supplier<List<?>> sourceSupplier, List<ObjectTableColumn> columns, JTable table) {
        this.columns = columns;
        this.sourceSupplier = sourceSupplier;
        this.table = table;
        columnModel = new DefaultTableColumnModel();
        setSourceFromSupplier();
    }

    public void setFutureWaiter(Consumer<Runnable> futureWaiter) {
        this.futureWaiter = futureWaiter;
    }

    public Consumer<Runnable> getFutureWaiter() {
        return futureWaiter;
    }

    public void setSourceFromSupplier() {
        source = sourceSupplier.get();
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

    public List<ObjectTableColumn> getColumns() {
        return columns;
    }

    public List<?> getSource() {
        return source;
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

    public JScrollPane initTableScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        int width = getColumns().stream()
                .mapToInt(e -> e.getTableColumn().getWidth())
                .sum();
        scrollPane.setPreferredSize(new Dimension(width, Math.max(scrollPane.getPreferredSize().height, 100)));
        return scrollPane;
    }

    ////////////

    @Override
    public String getColumnName(int column) {
        return super.getColumnName(column);
    }

    @Override
    public int getRowCount() {
        return source == null ? 0 : source.size();
    }

    @Override
    public int getColumnCount() {
        return columnModel.getColumnCount();
    }

    public void buildDataArray() {
        List<?> src = source;
        if (src == null) {
            src = Collections.emptyList();
        }

        int rows = src.size();
        int cols = columnModel.getColumnCount();

        if (data == null ||
                data.length != rows ||
                data.length > 0 && data[0].length != cols) {
            data = new Object[rows][cols];
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

    /** executed under event thread */
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

    public Object takeValueFromSource(Object[] rowData, int rowIndex, int columnIndex) {
        Object rowObject = source.get(rowIndex);
        Object cellObject = columns.get(columnIndex)
                .getCellValue(rowObject, rowIndex, columnIndex);

        if (cellObject != null && cellObject instanceof Future<?>) {
            rowData[columnIndex] = NULL_CELL;
            futureWaiter.accept(() -> takeValueFromSourceFuture(rowData, rowIndex, columnIndex, (Future<?>) cellObject));
            return NULL_CELL;
        } else if (cellObject == null) {
            rowData[columnIndex] = NULL_CELL;
            return NULL_CELL;
        } else {
            rowData[columnIndex] = cellObject;
            return cellObject;
        }
    }

    public void takeValueFromSourceFuture(Object[] rowData, int rowIndex, int columnIndex, Future<?> future) {
        try {
            Object cellObject = future.get(1, TimeUnit.SECONDS);
            if (cellObject == null) {
                cellObject = NULL_CELL;
            }
            final Object o = cellObject;
            SwingUtilities.invokeLater(() -> {
                rowData[columnIndex] = o;
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

    /** executed under event thread */
    public void setValueAtWithError(Object aValue, int rowIndex, int columnIndex) {
        Object[] rowData = data[rowIndex];
        rowData[columnIndex] = (aValue == null ? NULL_CELL : aValue);

        Object rowObject = source.get(rowIndex);
        offerValueForSource(rowObject, aValue, rowIndex, columnIndex);
    }

    public void offerValueForSource(Object rowObject, Object aValue, int rowIndex, int columnIndex) {
        Future<?> future = getColumns().get(columnIndex)
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
    public void refreshRow(int rowIndex) {
        clearRowData(rowIndex);
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public void clearRowData(int rowIndex) {
        Object[] rowData = data[rowIndex];
        //clear row data for re-taking value from source
        for (int i = 0, l = rowData.length; i < l; ++i) {
            rowData[i] = null;
        }
    }

    /** executed under event thread */
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

    /** executed under event thread */
    public void refreshColumns(Collection<Integer> columnIndexes) {
        refreshColumns(columnIndexes.stream()
                .mapToInt(Integer::intValue)
                .toArray());
    }

    /** executed under event thread */
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
        return getColumns().get(columnIndex)
                .getTableColumn().getCellEditor() != null;
    }
}
