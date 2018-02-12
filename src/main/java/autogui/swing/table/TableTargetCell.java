package autogui.swing.table;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TableTargetCell {
    protected JTable table;

    public TableTargetCell(JTable table) {
        this.table = table;
    }

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

    public Stream<int[]> getSelectedRowCellIndexesStream() {
        int[] rows = table.getSelectedRows();
        return IntStream.of(rows)
                .boxed()
                .flatMap(r -> IntStream.range(0, table.getRowCount())
                    .mapToObj(c -> new int[] {
                            table.convertRowIndexToModel(r),
                            table.convertColumnIndexToModel(c)
                    }));
    }

    public List<CellValue> getSelectedCells() {
        return getCellsByCellIndexes(getSelectedCellIndexesStream());
    }

    public List<CellValue> getSelectedRowCells() {
        return getCellsByCellIndexes(getSelectedRowCellIndexesStream());
    }

    public List<CellValue> getCellsByCellIndexes(Stream<int[]> idx) {
        TableModel model = table.getModel();
        return idx
                .map(pos -> new CellValue(pos[0], pos[1],
                        model.getValueAt(pos[0], pos[1])))
                .collect(Collectors.toList());
    }

    public static class CellValue {
        public int row;
        public int column;
        public Object value;

        public CellValue(int row, int column, Object value) {
            this.row = row;
            this.column = column;
            this.value = value;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

        public Object getValue() {
            return value;
        }
    }
}
