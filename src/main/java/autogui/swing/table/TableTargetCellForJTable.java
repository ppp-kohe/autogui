package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    public IntStream getSelectedRows() {
        return IntStream.of(table.getSelectedRows())
                .map(table::convertRowIndexToModel);
    }

    @Override
    public void setCellValues(List<GuiReprCollectionTable.CellValue> values) {
        TableModel model = table.getModel();
        values.forEach(e ->
                model.setValueAt(e.value, e.row, e.column));
    }

    @Override
    public void setCellValues(Stream<int[]> pos, Function<int[], Object> posToValue) {
        TableModel model = table.getModel();
        pos.forEach(p -> {
                Object v = posToValue.apply(p);
                if (v != null) {
                    model.setValueAt(v, p[0], p[1]);
                }
        });
    }

    @Override
    public Stream<int[]> getSelectedCellIndexesStream() {
        int[] rows = table.getSelectedRows();
        int[] cols = table.getSelectedColumns();
        return IntStream.of(rows)
                .boxed()
                .flatMap(r -> IntStream.of(cols)
                    .mapToObj(c -> new int[] {
                            table.convertRowIndexToModel(r),
                            table.convertColumnIndexToModel(c)
                    }));
    }

    @Override
    public Stream<int[]> getSelectedRowAllCellIndexesStream() {
        int[] rows = table.getSelectedRows();
        return IntStream.of(rows)
                .boxed()
                .flatMap(r -> IntStream.range(0, table.getRowCount())
                    .mapToObj(c -> new int[] {
                            table.convertRowIndexToModel(r),
                            table.convertColumnIndexToModel(c)
                    }));
    }

    @Override
    public List<GuiReprCollectionTable.CellValue> getSelectedCells() {
        return getCellsByCellIndexes(getSelectedCellIndexesStream());
    }

    @Override
    public List<GuiReprCollectionTable.CellValue> getSelectedRowAllCells() {
        return getCellsByCellIndexes(getSelectedRowAllCellIndexesStream());
    }

    public List<GuiReprCollectionTable.CellValue> getCellsByCellIndexes(Stream<int[]> idx) {
        TableModel model = table.getModel();
        return idx
                .map(pos -> new GuiReprCollectionTable.CellValue(pos[0], pos[1],
                        model.getValueAt(pos[0], pos[1])))
                .collect(Collectors.toList());
    }

    /** the model of the table must be {@link ObjectTableModel} */
    @Override
    public List<Object> getSelectedRowValues() {
        ObjectTableModel m = (ObjectTableModel) table.getModel();
        return getSelectedRows()
                .sorted()
                .mapToObj(m.getSource()::get)
                .collect(Collectors.toList());
    }

    public JTable getTable() {
        return table;
    }
}
