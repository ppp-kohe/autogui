package org.autogui.swing.util;

import org.autogui.base.mapping.ScheduledTaskRunner;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * customizing table-rows based on their minimum-size.
 *  <pre>
 *      var f = new TableRowHeightFitter(table);
 *      f.addListenersToTable();
 *  </pre>
 *  for customizing table-column width, you will need to call <code>table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);</code>
 */
public class TableRowHeightFitter {
    protected JTable table;
    protected int[][] cellHeights = new int[1][1];
    protected Graphics tester;

    protected boolean enabled = true;

    public static boolean DEBUG = false;

    public static class RowHeightUpdate {
        public TableColumn column;
        public int fromRow = -1;
        public int toRowExclusive = -1;

        public RowHeightUpdate() {
            this(null);
        }

        public RowHeightUpdate(TableColumn column) {
            this.column = column;
        }

        public RowHeightUpdate(TableColumn column, int fromRow, int toRowExclusive) {
            this.column = column;
            this.fromRow = fromRow;
            this.toRowExclusive = toRowExclusive;
        }

        public boolean isAllColumns() {
            return column == null;
        }

        public boolean isAllRows() {
            return fromRow == -1;
        }

        @Override
        public String toString() {
            return "column[" + (column == null ? "*" : column.getModelIndex()) + "]";
        }
    }

    protected ScheduledTaskRunner<RowHeightUpdate> columnEventHandler = new ScheduledTaskRunner<>(100, this::fitByEvent,
            Executors.newScheduledThreadPool(0)); //create another auto closing cached pool

    public TableRowHeightFitter(JTable table) {
        this.table = table;
    }

    public void addListenersToTable() {
        addListenerToTableModel(table.getModel());
        addListenerToTableColumnModel(table.getColumnModel());
    }

    public TableModelListener addListenerToTableModel(TableModel model) {
        TableModelListener listener = this::scheduleFitByEvent;
        model.addTableModelListener(listener);
        if (DEBUG) {
            model.addTableModelListener(e -> {
                System.err.printf("tableChange %s col=%d row=(%d,%d)%n",
                        (e.getType() == TableModelEvent.INSERT ? "INS" : e.getType() == TableModelEvent.DELETE ? "DEL" : e.getType() == TableModelEvent.UPDATE ? "UP" : "" + e.getType()),
                        e.getColumn(), e.getFirstRow(), e.getLastRow());
            });
        }
        return listener;
    }

    public void scheduleFitByEvent(TableModelEvent event) {
        if (event.getType() == TableModelEvent.UPDATE) {
            TableColumn column = null;
            int col = event.getColumn();
            if (0 <= col && col < table.getColumnCount()) {
                column = table.getColumnModel().getColumn(event.getColumn());
            }
            int fr = event.getFirstRow();
            int tr = (Integer.MAX_VALUE == event.getLastRow() ? event.getLastRow() : (event.getLastRow() + 1));
            if (fr <= 0 && table.getRowCount() <= tr) {
                scheduleFitAll();
            } else {
                columnEventHandler.schedule(new RowHeightUpdate(column, fr, tr));
            }
        } else {
            scheduleFitAll();
        }
    }

    public TableColumnModelListener addListenerToTableColumnModel(TableColumnModel model) {
        for (int i = 0, l = model.getColumnCount(); i < l; ++i) {
            addChangeWidthListener(model.getColumn(i));
        }
        TableColumnModelListener listener = new TableColumnModelListener() {
            @Override
            public void columnAdded(TableColumnModelEvent e) {
                int newIndex = e.getToIndex();
                addChangeWidthListener(model.getColumn(newIndex));
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
                scheduleFitAll();
            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {}

            @Override
            public void columnMarginChanged(ChangeEvent e) {}

            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {}
        };
        model.addColumnModelListener(listener);
        return listener;
    }

    public ScheduledTaskRunner<RowHeightUpdate> getColumnEventHandler() {
        return columnEventHandler;
    }

    public PropertyChangeListener addChangeWidthListener(TableColumn column) {
        if (Arrays.stream(column.getPropertyChangeListeners())
                .noneMatch(this::isTableColumnWidthChangeListenerFromThis)) { //no existing listener
            var l = new TableColumnWidthChangeListener(this, column);
            column.addPropertyChangeListener(l);
            return l;
        } else {
            return null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTableColumnWidthChangeListenerFromThis(PropertyChangeListener listener) {
        if (listener instanceof TableColumnWidthChangeListener) {
            return Objects.equals(this, ((TableColumnWidthChangeListener) listener).getFitter());
        } else {
            return false;
        }
    }

    public static class TableColumnWidthChangeListener implements PropertyChangeListener {
        protected TableRowHeightFitter fitter;
        protected TableColumn column;

        public TableColumnWidthChangeListener(TableRowHeightFitter fitter, TableColumn column) {
            this.fitter = fitter;
            this.column = column;
        }

        public TableRowHeightFitter getFitter() {
            return fitter;
        }

        public TableColumn getColumn() {
            return column;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("width")) {
                fitter.scheduleFitColumn(column);
            }
        }
    }

    public void scheduleFitColumn(TableColumn column) {
        columnEventHandler.schedule(new RowHeightUpdate(column));
    }

    public void scheduleFitAll() {
        columnEventHandler.schedule(new RowHeightUpdate());
    }

    /**
     * creates a dummy graphics. it can be released by {@link #dispose()}
     * @return the created graphics
     */
    public Graphics getGraphics() {
        if (tester == null) {
            tester = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR).createGraphics();
        }
        return tester;
    }

    public void dispose() {
        if (tester != null) {
            tester.dispose();
            tester = null;
        }
    }

    /**
     * start fitting by events of columns only if {@link #isEnabled()}
     * @param columns collecting events :
     *                if it has a {@link RowHeightUpdate#isAllColumns()}=true event, all others are ignored
     *                and do {@link #fitAll()}
     */
    public void fitByEvent(List<RowHeightUpdate> columns) {
        if (isEnabled()) {
            SwingUtilities.invokeLater(() -> {
                Collection<TableColumn> cols = new HashSet<>();
                List<int[]> rowRanges = new ArrayList<>();
                boolean allRows = false;
                for (RowHeightUpdate up : columns) {
                    if (up.isAllColumns() && up.isAllRows()) {
                        fitAll();
                        return;
                    } else {
                        if (up.column != null) {
                            cols.add(up.column);
                        }
                        if (up.isAllRows()) {
                            allRows = true;
                        } else if (!allRows) {
                            rowRanges.add(new int[] {up.fromRow, up.toRowExclusive});
                        }
                    }
                }
                if (cols.isEmpty()) {
                    cols = tableAllColumns();
                }
                if (allRows) {
                    fit(cols);
                } else {
                    if (DEBUG) {
                        log("fitByEvent: rowRanges=%s cols=%s",
                                rowRanges.stream()
                                        .map(r -> String.format("(%d,%d)", r[0], r[1]))
                                        .collect(Collectors.toList()),
                                cols.stream()
                                        .mapToInt(TableColumn::getModelIndex)
                                        .boxed()
                                        .collect(Collectors.toList()));
                    }
                    fit(cols, i -> rowRanges.stream()
                            .anyMatch(r -> r[0] <= i && i < r[1]));
                }
            });
        }
    }

    /**
     * re-calculate heights of all rows
     */
    public void fitAll() {
        fit(tableAllColumns());
    }

    protected List<TableColumn> tableAllColumns() {
        var columns = table.getColumnModel();
        return IntStream.range(0, columns.getColumnCount())
                .mapToObj(columns::getColumn)
                .collect(Collectors.toList());
    }

    /**
     * update cell heights cache if needed
     */
    protected void updateCellHeightsBySizeChange() {
        int colSize = table.getColumnCount() + 1; //[0] means the max of [1],[2],...
        int rowSize = Math.max(1, table.getRowCount());

        int oldColSize = cellHeights[0].length;
        int oldRowSize = cellHeights.length;

        if (colSize != oldColSize || rowSize != oldRowSize) {
            cellHeights = new int[rowSize][colSize];
        }
    }


    public void fit(Collection<TableColumn> targetColumns) {
        fit(targetColumns, i -> true);
    }

    public void fit(Collection<TableColumn> targetColumns, IntPredicate rowUpdate) {
        updateCellHeightsBySizeChange();
        if (DEBUG) {
            log("width changed %s", targetColumns.stream()
                    .map(TableColumn::getModelIndex)
                    .sorted()
                    .collect(Collectors.toList()));
        }

        for (int i = 0, rows = table.getRowCount(); i < rows; ++i) {
            int modelIndex = table.convertRowIndexToModel(i);
            if (rowUpdate.test(modelIndex)) {
                fitRow(i, modelIndex, targetColumns);
            }
        }
    }

    protected void log(String fmt, Object... args) {
        System.err.println(String.format(fmt, args));
    }

    public void fitRow(int viewRow, int row, Collection<TableColumn> targetColumns) {
        var rowCellHeights = cellHeights[row]; //[0] means the max
        int[] oldHeights = DEBUG ? Arrays.copyOf(rowCellHeights, rowCellHeights.length) : null;
        int oldMaxRowHeight = rowCellHeights[0];
        int rowHeight = 10;
        for (var column : targetColumns) {
            int cellHeight = fitCellWithMargin(viewRow, column);
            rowCellHeights[column.getModelIndex() + 1] = cellHeight;
        }
        for (int i = 1, l = rowCellHeights.length; i < l; ++i) {
            rowHeight = Math.max(rowHeight, rowCellHeights[i]);
        }
        if (oldMaxRowHeight != rowHeight || table.getRowHeight(viewRow) != rowHeight) {
            rowCellHeights[0] = rowHeight;
            table.setRowHeight(viewRow, rowHeight);
        }
        if (DEBUG) {
            log("fitRow %s", debugInfo(row, oldHeights, rowCellHeights));
        }
    }

    public String debugInfo(int row, int[] oldHeights, int[] newHeights) {
        StringBuilder buf = new StringBuilder();
        if (row >= 0) {
            buf.append(String.format("[%,4d] ", row));
        }
        for (int i = 1; i < newHeights.length; ++i) {
            if (i == 1) {
                buf.append(", ");
            }
            buf.append(String.format("[%2d]:%-4d", i - 1, newHeights[i]));
            if (oldHeights != null && i < oldHeights.length) {
                if (oldHeights[i] != newHeights[i]) {
                    buf.append(String.format("<-%-4d", oldHeights[i]));
                }
            }
        }
        buf.append(String.format(" : max:%-4d", newHeights[0]));
        if (oldHeights != null) {
            buf.append(String.format("%s%-4d", (oldHeights[0] == newHeights[0] ? "==" : "<-"), oldHeights[0]));
        }
        return buf.toString();
    }

    public int fitCellWithMargin(int viewRow, TableColumn column) {
        int h = fitCell(viewRow, column);
        var u = UIManagerUtil.getInstance();
        return h + u.getScaledSizeInt(10);
    }

    public int fitCell(int viewRow, TableColumn column) {
        int viewColumn = table.convertColumnIndexToView(column.getModelIndex());
        var cellComp = table.prepareRenderer(table.getCellRenderer(viewRow, viewColumn), viewRow, viewColumn);
        var smallSize = new Dimension(column.getWidth(), 10);
        //clear the custom specified minSize
        cellComp.setMinimumSize(null);
        cellComp.setSize(smallSize);
        cellComp.paint(getGraphics());
        Dimension size = cellComp.getMinimumSize();
        return size.height;
    }
}
