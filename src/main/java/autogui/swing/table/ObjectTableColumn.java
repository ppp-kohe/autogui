package autogui.swing.table;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Comparator;
import java.util.concurrent.Future;

public class ObjectTableColumn {
    protected TableColumn tableColumn;
    protected int rowHeight;
    protected Comparator<?> comparator;

    public TableColumn getTableColumn() {
        return tableColumn;
    }

    public void setTableColumn(TableColumn tableColumn) {
        this.tableColumn = tableColumn;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
    }

    public Comparator<?> getComparator() {
        return comparator;
    }

    public void setComparator(Comparator<?> comparator) {
        this.comparator = comparator;
    }

    /**
     *
     * @param rowObject the row object at rowIndex
     * @param rowIndex   the row index
     * @param columnIndex the column index
     * @return the value at columnIndex of rowObject.
     *  it might be {@link java.util.concurrent.Future}, and then
     *    the value will be specially treated as getting the value of the future as the cell value.
     */
    public Object getCellValue(Object rowObject, int rowIndex, int columnIndex) {
        return rowObject;
    }

    /**
     *
     * @param rowObject the row object at rowIndex
     * @param rowIndex  the row index
     * @param columnIndex the column index
     * @param newColumnValue the new value to be set
     * @return a future object for checking completion of the updating or null
     */
    public Future<?> setCellValue(Object rowObject, int rowIndex, int columnIndex, Object newColumnValue) {
        return null;
    }

    public static class ObjectTableColumnRowIndex extends ObjectTableColumn {
        public ObjectTableColumnRowIndex() {
            tableColumn = new TableColumn(0, 64, new NumberRenderer(), null);
            tableColumn.setHeaderValue("#");
        }

        @Override
        public Object getCellValue(Object rowObject, int rowIndex, int columnIndex) {
            return rowIndex;
        }
    }

    public static class NumberRenderer extends DefaultTableCellRenderer {
        public NumberRenderer() {
            setHorizontalAlignment(JLabel.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                    row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            return this;
        }
    }
}
