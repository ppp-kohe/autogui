package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;

import javax.swing.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * a impl. of {@link autogui.base.mapping.GuiReprCollectionTable.TableTargetColumn} with a {@link JTable}.
 */
public class TableTargetColumnForJTable extends TableTargetCellForJTable
        implements GuiReprCollectionTable.TableTargetColumn {
    /** model index */
    protected int column;

    public TableTargetColumnForJTable(JTable table, int column) {
        super(table);
        this.column = column;
    }

    @Override
    public Object getSelectedCellValue() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        } else {
            int modelRow = table.convertRowIndexToModel(row);
            return table.getModel().getValueAt(modelRow, column);
        }
    }

    @Override
    public Stream<int[]> getSelectedCellIndexesStream() {
        int[] rows = table.getSelectedRows();
        return IntStream.of(rows)
                .boxed()
                .map(r -> new int[]{
                        table.convertRowIndexToModel(r),
                        column
                });
    }
}
