package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.GuiSwingJsonTransfer;
import autogui.swing.GuiSwingView;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionText;
import autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * a table-column with additional info.
 *
 */
public class ObjectTableColumn {
    protected TableColumn tableColumn;
    protected int rowHeight;
    protected Comparator<?> comparator;
    protected Class<?> valueType = String.class;

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

    public void shutdown() {

    }

    public Class<?> getValueType() {
        return valueType;
    }

    public void setValueType(Class<?> valueType) {
        this.valueType = valueType;
    }

    public void setColumnViewUpdater(Consumer<ObjectTableColumn> updater) { }

    public void viewUpdateAsDynamic(ObjectTableColumn source) { }


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

    public int[] columnIndexToValueIndex(int columnIndex) {
        return new int[] {columnIndex};
    }

    //////////////// setter for table column

    public ObjectTableColumn withValueType(Class<?> valueType) {
        setValueType(valueType);
        return this;
    }

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

    /**
     *  Note: the actions added by the builder will only work with single (last) value of the renderer,
     *    set by {@link autogui.swing.GuiSwingView.ValuePane#setSwingViewValue(Object)}.
     *    So when the action is invoked,
     *       it will need to iterate over the selected rows, set the target value and invoke the action for each row.
     *       After the each action invoked, it might need to check the value.
     *
     * @return {@link TableColumn#getCellRenderer()} if the renderer is an {@link PopupMenuBuilderSource} itself.
     */
    public PopupMenuBuilderSource getMenuBuilderSource() {
        TableCellRenderer r = getTableColumn().getCellRenderer();
        if (r instanceof PopupMenuBuilderSource) {
            return (PopupMenuBuilderSource) r;
        } else {
            return null;
        }
    }

    /**
     * a menu-builder holder
     */
    public interface PopupMenuBuilderSource {
        default GuiSwingView.ValuePane<Object> getMenuTargetPane() {
            return null;
        }

        PopupExtension.PopupMenuBuilder getMenuBuilder(JTable table);
    }

    ////////////////

    public List<TableMenuComposite> getCompositesForRows() {
        return Collections.emptyList();
    }

    public List<TableMenuComposite> getCompositesForCells() {
        return Collections.emptyList();
    }

    /** menu items which the column can process */
    public interface TableMenuComposite {
        /** @return the "key" object for the composite shared by the same menu composites */
        TableMenuCompositeShared getShared();
    }

    /**
     * a shared key for a menu composite
     */
    public interface TableMenuCompositeShared  {
        /**
         * actually composite the selected columns if row is false, or all columns if row is true.
         *  The built actions include {@link TableTargetCellAction}s,
         *    and their actionPerformed(ActionEvent) will never be called.
         *    So you can just throw an exception in the method.
         * @param table the target table
         * @param columns the selected target columns: also if row is true, row-fixed composites are included.
         *                For setting up key-bindings, a table calls the methods with empty columns.
         * @param row true if the target are all columns of a row, or false if the target are only selected columns
         * @return menu items for composite items
         */
        List<PopupCategorized.CategorizedMenuItem> composite(JTable table, List<TableMenuComposite> columns, boolean row);
    }


    ////////////////

    /**
     * a top-column displaying an row-index number
     */
    public static class ObjectTableColumnRowIndex extends ObjectTableColumn {
        public ObjectTableColumnRowIndex() {
            tableColumn = new TableColumn(0, UIManagerUtil.getInstance().getScaledSizeInt(64), new NumberRenderer(this), null);
            tableColumn.setHeaderValue("#");
            setValueType(Number.class);
            withComparator(new GuiSwingTableColumnNumber.NumberComparator());
        }

        @Override
        public Object getCellValue(Object rowObject, int rowIndex, int columnIndex) {
            return rowIndex;
        }

        @Override
        public List<TableMenuComposite> getCompositesForRows() {
            int index = getTableColumn().getModelIndex();
            return Arrays.asList(
                    new ToStringCopyCell.TableMenuCompositeToStringCopy(index),
                    new ToStringCopyCell.TableMenuCompositeToStringPaste(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonCopy(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonPaste(index));
        }

        @Override
        public List<TableMenuComposite> getCompositesForCells() {
            int index = getTableColumn().getModelIndex();
            return Arrays.asList(
                    new ToStringCopyCell.TableMenuCompositeToStringCopy(index),
                    new ToStringCopyCell.TableMenuCompositeToStringPaste(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonCopy(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonPaste(index));
        }

        @Override
        public int[] columnIndexToValueIndex(int columnIndex) {
            return null;
        }
    }

    public static void setCellBorder(JTable table, JComponent cell, int row, int column) {
        boolean leftEnd = (column == 0);
        boolean rightEnd = (table.getColumnCount() == column + 1);
        UIManagerUtil ui = UIManagerUtil.getInstance();
        int h = Math.max(1, ui.getScaledSizeInt(1));
        int w = Math.max(1, ui.getScaledSizeInt(2));

        int iw = ui.getScaledSizeInt(5);
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(h, leftEnd ? w : 0, h * 2, rightEnd ? w : 0, table.getBackground()),
                BorderFactory.createMatteBorder(h, iw * 2, h, iw, cell.getBackground())));

    }

    /**
     * a renderer for index numbers
     */
    public static class NumberRenderer extends DefaultTableCellRenderer
            implements PopupMenuBuilderSource {
        protected ObjectTableColumn column;

        public NumberRenderer(ObjectTableColumn column) {
            this.column = column;
            setHorizontalAlignment(JLabel.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                    row, column);
            setCellBorder(table, this, row, column);
            return this;
        }

        @Override
        public PopupExtension.PopupMenuBuilder getMenuBuilder(JTable table) {
            return new ObjectTableColumnValue.ObjectTableColumnActionBuilder(table, column,
                    new PopupCategorized(() -> Collections.singletonList(new NumberCopyAction()), null,
                            new ObjectTableModel.MenuBuilderWithEmptySeparator()));
        }
    }

    /**
     * an action for copying an index number to the clip-board
     */
    public static class NumberCopyAction extends PopupExtensionText.TextCopyAllAction
            implements TableTargetColumnAction {

        public NumberCopyAction() {
            super(null);
            putValue(NAME, "Copy Indexes");
            putValue(ACCELERATOR_KEY, null);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            actionPerformedOnTable(e, target.getSelectedCells().stream()
                .map(GuiReprCollectionTable.CellValue::getValue)
                .collect(Collectors.toList()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (src instanceof JLabel) {
                String text = ((JLabel) src).getText();
                copy(text);
            }
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

    /**
     * a column class for specifying operations with lambdas.
     * The class does not care about rendering and editing.
     * @param <ObjType> the target row-object type
     * @param <PropType> the column type
     */
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
