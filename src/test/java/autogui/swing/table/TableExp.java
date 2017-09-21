package autogui.swing.table;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TableExp {
    public static void main(String[] args) {
        JFrame frame = new JFrame("table");
        {
            ObjectTableModel model = new ObjectTableModel(() ->
                    IntStream.range(0, 100)
                        .mapToObj(i ->
                                IntStream.range(0, 10).mapToObj(j -> "data-" + i + "-" + j)
                                    .collect(Collectors.toList()))
                        .collect(Collectors.toList()),
                    IntStream.range(0, 10)
                        .mapToObj(j -> {
                            return ObjectTableColumn.<List<Object>,Object>createLabel("hello",
                                    o -> o.get(j),
                                    (o,v) -> o.set(j, v));
                        })
                        .collect(Collectors.toList()));
            frame.add(model.initTableWithScroll());
        }
        frame.pack();
        frame.setVisible(true);
    }

    static class TableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return 100;
        }

        @Override
        public int getColumnCount() {
            return 10;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return "data-" + columnIndex + "-" + rowIndex;
        }
    }
}
