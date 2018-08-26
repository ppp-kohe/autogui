package org.autogui.swing.table;

import org.autogui.base.mapping.GuiReprCollectionTable;

import javax.swing.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * a impl. of {@link GuiReprCollectionTable.TableTargetColumn} with a {@link JTable}.
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
    public Iterable<int[]> getSelectedCellIndices() {
        return IntStream.of(getSelectedRows())
                .boxed()
                .map(r -> new int[]{
                        r,
                        column
                })
                .collect(Collectors.toList());
    }
}
