package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable.TableTargetCell;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingTaskRunner;
import autogui.swing.GuiSwingTaskRunner.ContextTaskResult;
import autogui.swing.table.ObjectTableColumn.TableMenuComposite;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension.PopupMenuBuilder;
import autogui.swing.util.PopupExtension.PopupMenuFilter;
import autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
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
public class ObjectTableModel extends AbstractTableModel
        implements ObjectTableModelColumns.ObjectTableModelColumnsListener {
    protected JTable table;
    protected Supplier<Object> source;

    protected ObjectTableModelColumns columns;
    /** cached computed values */
    protected Object[][] data;

    protected Consumer<Runnable> futureWaiter = Runnable::run;
    protected GuiSwingTaskRunner runner;

    public static Object NULL_CELL = new Object();

    public ObjectTableModel() {
        this(new GuiSwingTaskRunner(null));
    }

    public ObjectTableModel(GuiSwingTaskRunner runner) {
        this.runner = runner;
        initColumns();
    }

    public void initColumns() {
        columns = new ObjectTableModelColumns(this);
    }

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

    ////////// columns

    @Override
    public String getColumnName(int column) {
        return super.getColumnName(column);
    }

    public DefaultTableColumnModel getColumnModel() {
        return columns.getColumnModel();
    }

    public ObjectTableColumn getColumnAt(int index) {
        return columns.getColumnAt(index);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columns.getColumnAt(columnIndex).getValueType();
    }

    public ObjectTableModelColumns getColumns() {
        return columns;
    }

    @Override
    public int getColumnCount() {
        return columns.getColumnCount();
    }

    public void refreshColumns() {
        executeContextTask(
                () -> columns.getColumnSizeForUpdate(getCollectionFromSource()),
                r -> r.executeIfPresent(
                        sizes -> SwingUtilities.invokeLater(() -> columns.update(sizes))));
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
        initTableRowSorter(table);
        initTableRowHeight(table);
    }

    public void initTableRowSorter(JTable table) {
        table.setAutoCreateRowSorter(false);
        table.setRowSorter(new ObjectTableModelColumns.TableRowSorterDynamic(this));
    }

    public void initTableRowHeight(JTable table) {
        int height = Math.max(table.getRowHeight() + UIManagerUtil.getInstance().getScaledSizeInt(3),
                getColumns().getRowHeight());
        table.setRowHeight(height);
    }

    public JScrollPane initTableScrollPane(JComponent table) {
        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(getColumns().getTotalWidth(),
                Math.max(scrollPane.getPreferredSize().height, UIManagerUtil.getInstance().getScaledSizeInt(100))));
        return scrollPane;
    }

    //////////// task

    public <RetType> ContextTaskResult<RetType> executeContextTask(Supplier<RetType> task,
                                                                   Consumer<ContextTaskResult<RetType>> afterTask) {
        return runner.executeContextTask(task, afterTask);
    }

    //////////// row

    public void setSource(Supplier<Object> source) {
        this.source = source;
    }

    public Object getCollectionFromSource() {
        return source == null ? null : source.get();
    }

    @Override
    public int getRowCount() {
        if (data == null) {
            BuildResult d = buildDataArray(this::fireTableRowsUpdatedAll);
            if (d.equals(BuildResult.Delayed)) {
                return 0;
            }
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

    //////////// values

    /** the return value of {@link #buildDataArray(Runnable)} */
    public enum BuildResult {
        Updated, NoUpdate, Delayed
    }

    public BuildResult buildDataArray(Runnable delayedAfterInEvent) {
        ContextTaskResult<int[]> size = executeContextTask(
                () -> {
                    int[] s = new int[2];
                    s[0] = getRowCountUpdated();
                    s[1] = getColumnCount();
                    return s;
                },
                r -> r.executeIfPresentWithDelay(
                        v -> SwingUtilities.invokeLater(delayedAfterInEvent)));
        if (size.isPresented()) {
            int[] s = size.getValue();
            int rows = s[0];
            int cols = s[1];
            if (data == null ||
                    data.length != rows ||
                    data.length > 0 && data[0].length != cols) {
                data = new Object[rows][cols];
                return BuildResult.Updated;
            } else {
                return BuildResult.NoUpdate;
            }
        } else {
            return BuildResult.Delayed;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            if (data == null) {
                BuildResult res = buildDataArray(() -> fireTableCellUpdated(rowIndex, columnIndex));
                if (res.equals(BuildResult.Delayed)) {
                    return null;
                }
            }
            return getValueAtWithError(rowIndex, columnIndex);
        } catch (Exception ex) {
            BuildResult res = buildDataArray(() -> fireTableCellUpdated(rowIndex, columnIndex));
            if (res.equals(BuildResult.Delayed)) {
                return null;
            } else {
                return getValueAtWithError(rowIndex, columnIndex);
            }
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
     *  the column's {@link ObjectTableColumn#getCellValue(Object, int, int, autogui.base.mapping.GuiReprValue.ObjectSpecifier)} might return
     *   a {@link Future} object and
     *   then it waits completion of the task in {@link #takeValueFromSourceFuture(Object[], int, int, Future)}.
     *   The method is run in {@link #getFutureWaiter()} (default is just call the method)
     * @param rowData the row array which the returned cell value will be stored
     * @param rowIndex the row index
     * @param columnIndex the column index
     * @return the cell value, nullable
     */
    public Object takeValueFromSource(Object[] rowData, int rowIndex, int columnIndex) {
        ObjectTableColumn column = getColumnAt(columnIndex);
        GuiReprValue.ObjectSpecifier specifier = column.getSpecifier(rowIndex, columnIndex);

        ContextTaskResult<Object> cellObject = executeContextTask(
                () -> {
                    try {
                        Object rowObject = getRowAtIndex(rowIndex);
                        return column.getCellValue(rowObject, rowIndex, columnIndex, specifier);
                    } catch (Exception ex) {
                        //TODO error reporting
                        return null;
                    }
                },
                r -> {
                    if (r.isTimeout()) {
                        SwingUtilities.invokeLater(() -> fireTableCellUpdated(rowIndex, columnIndex));
                    } else if (!r.isCancel()) {
                        SwingUtilities.invokeLater(() -> taskValueFromSourceAfter(rowData, rowIndex, columnIndex, r.getValue()));
                    }
                });
        return taskValueFromSourceAfter(rowData, rowIndex, columnIndex, cellObject.getValue());
    }

    public Object taskValueFromSourceAfter(Object[] rowData, int rowIndex, int columnIndex, Object cellObject) {
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
            BuildResult res = buildDataArray(() -> setValueAtWithError(aValue, rowIndex, columnIndex));
            if (!res.equals(BuildResult.Delayed)) {
                setValueAtWithError(aValue, rowIndex, columnIndex);
            }
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

        offerValueForSource(aValue, rowIndex, columnIndex);
    }

    public void offerValueForSource(Object aValue, int rowIndex, int columnIndex) {
        ObjectTableColumn column = getColumnAt(columnIndex);
        GuiReprValue.ObjectSpecifier specifier = column.getSpecifier(rowIndex, columnIndex);
        executeContextTask(() -> {
                    Object rowObject = getRowAtIndex(rowIndex);
                    return new OfferResult(column.setCellValue(rowObject, rowIndex, columnIndex, aValue, specifier), rowObject);
                },
                r -> r.executeIfPresent(
                        or -> futureWaiter.accept(() ->
                                    offsetValueForSourceFuture(or.rowObject, aValue, rowIndex, columnIndex, or.result))));
    }

    private static class OfferResult {
        public Future<?> result;
        public Object rowObject;

        public OfferResult(Future<?> result, Object rowObject) {
            this.result = result;
            this.rowObject = rowObject;
        }
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
        BuildResult res = buildDataArray(this::fireTableDataChanged);
        if (res.equals(BuildResult.NoUpdate)) {
            for (int i = 0, l = data.length; i < l; ++i) {
                clearRowData(i);
            }
            fireTableRowsUpdatedAll();
        } else if (!res.equals(BuildResult.Delayed)) {
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
     * @param rowIndices the target rows
     */
    public void refreshRows(int... rowIndices) {
        int min = -1;
        int max = -1;
        for (int rowIndex : rowIndices) {
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
     * @param columnIndices the target columns
     */
    public void refreshColumns(Collection<Integer> columnIndices) {
        refreshColumns(columnIndices.stream()
                .mapToInt(Integer::intValue)
                .toArray());
    }

    /** executed under event thread
     * @param columnIndices the target columns
     */
    public void refreshColumns(int... columnIndices) {
        for (Object[] rowData : data) {
            for (int columnIndex : columnIndices) {
                rowData[columnIndex] = null;
            }
        }
        fireTableRowsUpdatedAll();
    }

    @Override
    public void columnAdded(ObjectTableColumn column) {
        fireTableRowsUpdatedAll(); //no data change
    }

    @Override
    public void columnViewUpdate(ObjectTableColumn column) {
        fireTableRowsUpdatedAll(); //no data change
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

    public PopupMenuBuilder getBuilderForRowsOrCells(JTable table, List<ObjectTableColumn> cols, boolean row) {
        return new PopupCategorizedForRowsOrCells(() ->
                getBuildersForRowsOrCells(table, cols, row));
    }

    /** a menu builder for table items */
    public static class PopupCategorizedForRowsOrCells extends PopupCategorized {
        public PopupCategorizedForRowsOrCells(Supplier<? extends Collection<CategorizedMenuItem>> itemSupplier) {
            super(itemSupplier, null, new MenuBuilderForRowsOrCells());
        }

        @Override
        protected void buildMergeSubCategories(Map<String, Map<String, List<JComponent>>> subCategorizedMenuItems,
                                               Map<String, List<JComponent>> categorizedMenuItems) {
            subCategorizedMenuItems.forEach((k,map) -> {
                buildCategoryLabel(categorizedMenuItems, k, map);
                map.forEach((sk, v) ->
                        categorizedMenuItems.computeIfAbsent(MenuBuilder.getCategoryImplicit(k + ": " + sk),
                                (c) -> new ArrayList<>())
                                .addAll(v));
            });
        }

        protected void buildCategoryLabel(Map<String, List<JComponent>> categorizedMenuItems,
                                          String k,
                                          Map<String, List<JComponent>> map) {
            if (map.values().stream()
                    .mapToInt(List::size)
                    .sum() > 0) {
                //build category for entire label "Selected Cells" or "Selected Rows"
                categorizedMenuItems.put(k, Collections.singletonList(menuBuilder.createLabel(k)));
            }
        }
    }

    /** a menu builder without adding titles */
    public static class MenuBuilderForRowsOrCells extends MenuBuilderWithEmptySeparator {
        @Override
        public boolean addMenuTitle(AddingProcess process, String title) {
            return false; //the title are explicitly added by buildCategoryLabel
        }
    }

    /**
     * a menu builder for {@link TableTargetCellAction}s
     */
    public interface PopupMenuBuilderForRowsOrCells {
        void build(PopupMenuFilter filter, Consumer<Object> menu);
    }

    public List<PopupCategorized.CategorizedMenuItem> getBuildersForRowsOrCells(JTable table,
                                                                                List<ObjectTableColumn> cols, boolean row) {
        Map<ObjectTableColumn.TableMenuCompositeShared, List<TableMenuComposite>> rows
                = new LinkedHashMap<>();

        Consumer<TableMenuComposite> rowsAdder = (cmp) ->
            rows.computeIfAbsent(cmp.getShared(), key -> new ArrayList<>())
                    .add(cmp);

        cols.forEach(c ->
                (row ? c.getCompositesForRows() :
                        c.getCompositesForCells()).forEach(rowsAdder));

        if (row) {
            getColumns().getMenuRowComposites()
                    .forEach(rowsAdder);
        }

        return rows.entrySet().stream()
                .flatMap(e ->
                        e.getKey().composite(table, e.getValue(), row)
                                    .stream())
                .collect(Collectors.toList());

    }

    /** a menu builder with empty separators */
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
            UIManagerUtil ui = UIManagerUtil.getInstance();
            sep.setPreferredSize(new Dimension(ui.getScaledSizeInt(10), ui.getScaledSizeInt(6)));
            sep.setOpaque(false);
            sep.setBorder(BorderFactory.createEmptyBorder());
            return sep;
        }
    }

    /**
     * a builder accepting a {@link TableTargetCellAction} and wrapping it to a {@link TableTargetCellExecutionAction}.
     */
    public static class CollectionRowsAndCellsActionBuilder implements PopupMenuFilter {
        protected PopupMenuFilter filter;
        protected TableTargetCell target;

        public CollectionRowsAndCellsActionBuilder(JTable table, PopupMenuFilter filter) {
            this.filter = filter;
            target = new TableTargetCellForJTable(table);
        }

        @Override
        public Object convert(Object item) {
            if (item instanceof TableTargetCellAction) {
                return filter.convert(new TableTargetCellExecutionAction((TableTargetCellAction) item, target));
            } else if (item instanceof JPopupMenu.Separator) {
                return item;
            } else {
                return null; //disabled
            }
        }
    }

    /**
     * an action for wrapping {@link TableTargetCellAction}
     */
    public static class TableTargetCellExecutionAction extends ObjectTableColumnValue.ActionDelegate<TableTargetCellAction>
            implements PopupCategorized.CategorizedMenuItemAction {
        protected TableTargetCell target;

        public TableTargetCellExecutionAction(TableTargetCellAction action, TableTargetCell target) {
            super(action);
            this.target = target;
        }

        public TableTargetCell getTarget() {
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
