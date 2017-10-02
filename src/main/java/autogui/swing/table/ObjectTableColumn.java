package autogui.swing.table;

import autogui.swing.GuiSwingView;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Comparator;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

    //////////////// setter for table column

    public ObjectTableColumn withTableColumn(TableColumn column) {
        setTableColumn(column);
        return this;
    }

    public ObjectTableColumn withRowHeight(int rowHeight) {
        setRowHeight(rowHeight);
        return this;
    }

    public ObjectTableColumn withComparator(Comparator<?> comparator) {
        setComparator(comparator);
        return this;
    }

    public ObjectTableColumn withIdentifier(Object identifier) {
        getTableColumn().setIdentifier(identifier);
        return this;
    }

    public ObjectTableColumn withHeaderValue(Object headerValue) {
        getTableColumn().setHeaderValue(headerValue);
        return this;
    }

    public ObjectTableColumn withHeaderRenderer(TableCellRenderer headerRenderer) {
        getTableColumn().setHeaderRenderer(headerRenderer);
        return this;
    }

    public ObjectTableColumn withCellRenderer(TableCellRenderer cellRenderer) {
        getTableColumn().setCellRenderer(cellRenderer);
        return this;
    }

    public ObjectTableColumn withCellEditor(TableCellEditor cellEditor) {
        getTableColumn().setCellEditor(cellEditor);
        return this;
    }

    public ObjectTableColumn withPreferredWidth(int preferredWidth) {
        getTableColumn().setPreferredWidth(preferredWidth);
        return this;
    }

    public ObjectTableColumn withMinWidth(int minWidth) {
        getTableColumn().setMinWidth(minWidth);
        return this;
    }

    public ObjectTableColumn withMaxWidth(int maxWidth) {
        getTableColumn().setMaxWidth(maxWidth);
        return this;
    }

    public ObjectTableColumn withResizable(boolean isResizable) {
        getTableColumn().setResizable(isResizable);
        return this;
    }

    /** returns {@link TableColumn#getCellRenderer()} if the renderer is an {@link PopupMenuBuilderSource} itself.
     *  <p>
     *  Note: the actions added by the builder will only work with single (last) value of the renderer,
     *    set by {@link autogui.swing.GuiSwingView.ValuePane#setSwingViewValue(Object)}.
     *    So when the action is invoked,
     *       it will need to iterate over the selected rows, set the target value and invoke the action for each row.
     *       After the each action invoked, it might need to check the value.
     *  */
    public PopupMenuBuilderSource getMenuBuilderSource() {
        TableCellRenderer r = getTableColumn().getCellRenderer();
        if (r instanceof PopupMenuBuilderSource) {
            return (PopupMenuBuilderSource) r;
        } else {
            return null;
        }
    }

    public interface PopupMenuBuilderSource {
        GuiSwingView.ValuePane getMenuTargetPane();
        PopupExtension.PopupMenuBuilder getMenuBuilder();
    }


    ////////////////

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

    public static class NumberRenderer extends DefaultTableCellRenderer  {
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

    ///////////////

    public static <O,P> ObjectTableColumnLabel<O,P>  createLabel(String headerName, Function<O, P> getter) {
        return createLabel(headerName, getter, null);
    }

    public static <O,P> ObjectTableColumnLabel<O,P>  createLabel(String headerName, Function<O, P> getter, BiConsumer<O, P> setter) {
        ObjectTableColumnLabel<O,P>  l = new ObjectTableColumnLabel<O,P>(getter, setter);
        l.getTableColumn().setHeaderValue(headerName);
        if (setter != null) {
            l.getTableColumn().setCellEditor(new DefaultCellEditor(new JTextField()));
        }
        return l;
    }

    public static class ObjectTableColumnLabel<ObjType, PropType> extends ObjectTableColumn {
        protected Function<ObjType,PropType> getter;
        protected BiConsumer<ObjType,PropType> setter;

        public ObjectTableColumnLabel(Function<ObjType, PropType> getter, BiConsumer<ObjType,PropType> setter) {
            this(getter, setter, new TableColumn());
        }

        public ObjectTableColumnLabel(Function<ObjType, PropType> getter, BiConsumer<ObjType,PropType> setter, TableColumn tableColumn) {
            this.tableColumn = tableColumn;
            this.getter = getter;
            this.setter = setter;
        }

        public Function<ObjType, PropType> getGetter() {
            return getter;
        }

        public BiConsumer<ObjType, PropType> getSetter() {
            return setter;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object getCellValue(Object rowObject, int rowIndex, int columnIndex) {
            return getter.apply((ObjType) rowObject);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Future<?> setCellValue(Object rowObject, int rowIndex, int columnIndex, Object newColumnValue) {
            if (setter != null) {
                setter.accept((ObjType) rowObject, (PropType) newColumnValue);
            }
            return null;
        }
    }




}
