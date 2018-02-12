package autogui.swing.table;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TableTargetColumn {
    protected JTable table;
    /** model index */
    protected int column;

    public TableTargetColumn(JTable table, int column) {
        this.table = table;
        this.column = column;
    }

    public boolean isSelectionEmpty() {
        return table.getSelectionModel().isSelectionEmpty();
    }

    public Object getSelectedCellValue() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        } else {
            int modelRow = table.convertRowIndexToModel(row);
            return table.getModel().getValueAt(modelRow, column);
        }
    }

    public Map<Integer,Object> getSelectedCellValues() {
        int[] rows = table.getSelectedRows();
        Map<Integer, Object> map = new TreeMap<>();
        TableModel model = table.getModel();
        for (int row : rows) {
            int modelRow = table.convertRowIndexToView(row);
            map.put(modelRow, model.getValueAt(modelRow, column));
        }
        return map;
    }

    public java.util.List<Integer> getSelectedRows() {
        return Arrays.stream(table.getSelectedRows())
                .map(table::convertRowIndexToModel)
                .sorted()
                .boxed()
                .collect(Collectors.toList());
    }

    public void setCellValues(Map<Integer,?> rowToValues) {
        TableModel model = table.getModel();
        rowToValues.forEach((modelRow, value) ->
                model.setValueAt(value, modelRow, column));
    }

    public void setSelectedCellValues(Function<Integer, Object> rowToNewValue) {
        TableModel model = table.getModel();
        for (int row : getSelectedRows()) {
            int modelRow = table.convertRowIndexToModel(row);
            model.setValueAt(rowToNewValue.apply(modelRow), modelRow, column);
        }
    }

    public void setSelectedCellValuesLoop(List<?> rowValues) {
        if (!rowValues.isEmpty()) {
            TableModel model = table.getModel();
            int size = rowValues.size();
            int i = 0;
            for (int row : getSelectedRows()) {
                int modelRow = table.convertRowIndexToModel(row);
                Object value = rowValues.get(i % size);
                model.setValueAt(value, modelRow, column);
                ++i;
            }
        }
    }
}
