package autogui.swing.table;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
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
            JTable table = new JTable(model);

            JPanel pane = new JPanel(new BorderLayout());

            TestAction ta = new TestAction(table);
            table.getSelectionModel().addListSelectionListener(e -> {
                ta.setEnabled(!table.getSelectionModel().isSelectionEmpty());
            });

            JToolBar toolBar = new JToolBar();
            toolBar.add(ta);
            pane.add(toolBar, BorderLayout.PAGE_START);
            pane.add(new JScrollPane(table));

            frame.add(pane);
        }
        frame.pack();
        frame.setVisible(true);
    }

    static class TestAction extends AbstractAction {
        JTable table;

        public TestAction(JTable table) {
            putValue(NAME, "Hello");
            setEnabled(false);
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("action");
        }
    }
}
