package org.autogui.swing.table;

import org.autogui.base.mapping.GuiReprCollectionTable;
import org.autogui.base.mapping.GuiReprCollectionTable.CellValue;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * an implementation of {@link GuiReprCollectionTable.TableTargetCell} for {@link JTable}.
 */
public class TableTargetCellForJTable implements GuiReprCollectionTable.TableTargetCell {
    protected JTable table;

    public TableTargetCellForJTable(JTable table) {
        this.table = table;
    }

    @Override
    public boolean isSelectionEmpty() {
        return table.getSelectionModel().isSelectionEmpty();
    }

    @Override
    public int[] getSelectedRows() {
        return getSelectedRowsView()
                .map(table::convertRowIndexToModel)
                .toArray();
    }

    public IntStream getSelectedRowsView() {
        int rows = table.getRowCount();
        return IntStream.of(table.getSelectedRows())
                .filter(r -> 0 <= r && r < rows);
    }

    @Override
    public void setCellValues(List<CellValue> values) {
        TableModel model = table.getModel();
        values.forEach(e ->
                model.setValueAt(e.value, e.row, e.column));
    }

    @Override
    public void setCellValues(Iterable<int[]> pos, Function<int[], Object> posToValue) {
        TableModel model = table.getModel();
        pos.forEach(p -> {
                Object v = posToValue.apply(p);
                if (v != null) {
                    model.setValueAt(v, p[0], p[1]);
                }
        });
    }

    @Override
    public Iterable<int[]> getSelectedCellIndices() {
        int[] cols = table.getSelectedColumns();
        return getSelectedRowsView()
                .boxed()
                .flatMap(r -> IntStream.of(cols)
                        .filter(c -> table.isCellSelected(r, c))
                        .mapToObj(c -> convertViewToData(r, c)))
                        .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<int[]> getSelectedRowAllCellIndices() {
        return getSelectedRowsView()
                .boxed()
                .flatMap(r -> IntStream.range(0, table.getColumnCount())
                        .mapToObj(c -> convertViewToData(r, c)))
                        .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public int[] convertViewToData(int viewRow, int viewColumn) {
        return new int[] {
                table.convertRowIndexToModel(viewRow),
                table.convertColumnIndexToModel(viewColumn)
        };
    }

    @Override
    public List<CellValue> getSelectedCells() {
        return getCellsByCellIndices(getSelectedCellIndices());
    }

    @Override
    public List<CellValue> getSelectedRowAllCells() {
        return getCellsByCellIndices(getSelectedRowAllCellIndices());
    }

    public List<CellValue> getCellsByCellIndices(Iterable<int[]> idx) {
        TableModel model = table.getModel();
        return StreamSupport.stream(idx.spliterator(), false)
                .map(pos -> new CellValue(pos[0], pos[1],
                        model.getValueAt(pos[0], pos[1])))
                .collect(Collectors.toList());
    }

    /** the model of the table must be {@link ObjectTableModel} */
    @Override
    public List<Object> getSelectedRowValues() {
        ObjectTableModel m = (ObjectTableModel) table.getModel();
        return IntStream.of(getSelectedRows())
                .sorted()
                .mapToObj(m::getRowAtIndex) //TODO delayed
                .collect(Collectors.toList());
    }

    public JTable getTable() {
        return table;
    }
}
