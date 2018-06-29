package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;
import autogui.base.mapping.GuiReprCollectionTable.CellValue;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * an implementation of {@link autogui.base.mapping.GuiReprCollectionTable.TableTargetCell} for {@link JTable}.
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
    public IntStream getSelectedRows() {
        return getSelectedRowsView().map(table::convertRowIndexToModel);
    }

    public IntStream getSelectedRowsView() {
        return IntStream.of(table.getSelectedRows());
    }

    @Override
    public void setCellValues(List<CellValue> values) {
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
    public Stream<int[]> getSelectedCellIndicesStream() {
        int[] cols = table.getSelectedColumns();
        return getSelectedRowsView()
                .boxed()
                .flatMap(r -> IntStream.of(cols)
                        .filter(c -> table.isCellSelected(r, c))
                        .mapToObj(c -> convertViewToData(r, c)))
                        .filter(Objects::nonNull);
    }

    @Override
    public Stream<int[]> getSelectedRowAllCellIndicesStream() {
        return getSelectedRowsView()
                .boxed()
                .flatMap(r -> IntStream.range(0, table.getColumnCount())
                        .filter(c -> table.isCellSelected(r, c))
                        .mapToObj(c -> convertViewToData(r, c)))
                        .filter(Objects::nonNull);
    }

    public int[] convertViewToData(int viewRow, int viewColumn) {
        return new int[] {
                table.convertRowIndexToModel(viewRow),
                table.convertColumnIndexToModel(viewColumn)
        };
    }

    @Override
    public List<CellValue> getSelectedCells() {
        return getCellsByCellIndices(getSelectedCellIndicesStream());
    }

    @Override
    public List<CellValue> getSelectedRowAllCells() {
        return getCellsByCellIndices(getSelectedRowAllCellIndicesStream());
    }

    public List<CellValue> getCellsByCellIndices(Stream<int[]> idx) {
        TableModel model = table.getModel();
        return idx
                .map(pos -> new CellValue(pos[0], pos[1],
                        model.getValueAt(pos[0], pos[1])))
                .collect(Collectors.toList());
    }

    /** the model of the table must be {@link ObjectTableModel} */
    @Override
    public List<Object> getSelectedRowValues() {
        ObjectTableModel m = (ObjectTableModel) table.getModel();
        return getSelectedRows()
                .sorted()
                .mapToObj(m::getRowAtIndex) //TODO delayed
                .collect(Collectors.toList());
    }

    public JTable getTable() {
        return table;
    }
}
