package autogui.swing;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TableExp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new TableExp()::run);
    }

    public void run() {
        JFrame frame = new JFrame("test");
        {

            DefaultTableModel model = new DefaultTableModel(0, 1);
            model.addRow(new Object[] {"a"});
            model.addRow(new Object[] {"b"});
            model.addRow(new Object[] {"c"});
            JTable table = new JTable(model);
            {
                TableCellRenderer r = table.getTableHeader().getDefaultRenderer();
                table.getColumnModel().getColumn(0).setHeaderRenderer(new TableCellRenderer() {
                    JPanel pane = new JPanel();

                    {
                    }
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        pane.setLayout(new BorderLayout());
                        pane.removeAll();
                        Component comp = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        pane.setPreferredSize(comp.getPreferredSize());
                        pane.add(comp);
                        return pane;
                    }
                });
                JPopupMenu menu = new JPopupMenu();
                table.getTableHeader().addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.isPopupTrigger()) {
                            menu.removeAll();
                            menu.add("Hello");
                            menu.add("World");
                            e.consume();
                            menu.show(table.getTableHeader(), e.getX(), e.getY());
                        }
                    }
                });

                table.setAutoCreateRowSorter(true);

            }


            JScrollPane scrollPane = new JScrollPane(table);
            frame.setContentPane(scrollPane);
        }
        frame.setSize(500, 300);
        frame.setVisible(true);
    }

    class Header extends JTableHeader {

    }
}
