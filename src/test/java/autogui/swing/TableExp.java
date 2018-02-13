package autogui.swing;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

public class TableExp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new TableExp()::run);
    }

    public void run() {
        JFrame frame = new JFrame("test");
        {

            ExpModel m = new ExpModel(10000, (i,j) -> {
                return "cell" + i + "," + j;
            });

            JTable table = new JTable(m) {
//                @Override
//                public Dimension getPreferredSize() {
//                    return super.getPreferredSize();
//                }
//
//                @Override
//                public void setPreferredScrollableViewportSize(Dimension size) {
//                    System.err.println("update pref " + size);
//                    super.setPreferredScrollableViewportSize(size);
//                }
//
//                @Override
//                public Dimension getPreferredScrollableViewportSize() {
//                    return getPreferredSize();
//                }
            };
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            System.out.println(table.getPreferredScrollableViewportSize());

            int prefWidth = 0;
            DefaultTableColumnModel columnModel = new DefaultTableColumnModel();
            for (int i = 0; i < m.getColumnCount(); ++i) {
                TableColumn col = new TableColumn(i, 64);
                col.setHeaderValue("col" + i);
                col.setMinWidth(100);
                //col.setMaxWidth(1000);
                col.setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        String data = "<" + value + ">";
                        return super.getTableCellRendererComponent(table, data, isSelected, hasFocus, row, column);
                    }
                });
                columnModel.addColumn(col);
                prefWidth += col.getPreferredWidth();

            }

            //table.setModel(m);
            table.setColumnModel(columnModel);
            m.addRow();
            m.fireTableDataChanged();
            //stable.setPreferredSize(new Dimension(prefWidth, table.getPreferredSize().height));

            System.err.println(table.getPreferredSize());

            /*
            {
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
*/

            JToolBar bar = new JToolBar();
            bar.add(new AddAction(table, m, 0));
            bar.add(new AddAction(table, m, 10));
            bar.add(new AddAction(table, m, 10000));

            JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            JPanel p = new JPanel(new BorderLayout());
            p.add(scrollPane);
            p.add(bar, BorderLayout.NORTH);
            frame.getContentPane().add(p);
        }
        frame.setSize(500, 300);
        frame.setVisible(true);
    }

    static class AddAction extends AbstractAction {
        JTable table;
        ExpModel model;
        int rows;

        public AddAction(JTable table, ExpModel model, int n) {
            this.table = table;
            this.model = model;
            rows = n;
            putValue(NAME, "add " + n);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < rows; ++i) {
                model.addRow();
            }
            model.clear();
        }
    }

    class Header extends JTableHeader {

    }

    class ExpModel extends AbstractTableModel {
        List<Cell[]> rows = new ArrayList<>();
        int cols;
        BiFunction<Integer,Integer,Object> value;
        ExecutorService service;

        public ExpModel(int cols, BiFunction<Integer,Integer,Object> f) {
            this.cols = cols;
            service = Executors.newSingleThreadExecutor();
            value = f;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return cols;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            try {
                Cell c = rows.get(rowIndex)[columnIndex];
                if (c == null) {
                    c = new Cell();
                    c.refresh = true;
                    rows.get(rowIndex)[columnIndex] = c;
                }
                if (c.refresh) {
                    c.value = service.submit(() -> value.apply(rowIndex, columnIndex)).get();
                    c.refresh = false;
                    if (rowIndex == getRowCount() - 1 && columnIndex == getColumnCount() - 1) {
                        System.err.println("last update");
                    }
                }
                return c.value;
            } catch (Exception ex) {
                return "error";
            }
        }

        public List<Cell[]> getRows() {
            return rows;
        }

        public void clear() {
            for (Cell[] row : rows) {
                for (Cell cell : row) {
                    if (cell != null) {
                        cell.refresh = true;

                    }
                }
            }
            fireTableDataChanged();
        }

        public void addRow() {
            Cell[] cs = new Cell[getColumnCount()];
            rows.add(cs);
        }


    }

    static class Cell {
        public Object value;
        public boolean refresh;
    }
}
