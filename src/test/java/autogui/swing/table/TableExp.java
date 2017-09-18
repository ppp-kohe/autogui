package autogui.swing.table;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.awt.*;

public class TableExp {
    public static void main(String[] args) {
        JFrame frame = new JFrame("table");
        {
            DefaultTableColumnModel columnModel = new DefaultTableColumnModel();

            TableColumn col1 = new TableColumn(0, 100);
            col1.setHeaderValue("Hello");
            columnModel.addColumn(col1);

            TableColumn col2 = new TableColumn(1, 100);
            col2.setHeaderValue("World");
            columnModel.addColumn(col2);

            JTable table = new JTable(new TableModel(), columnModel);
            table.setAutoCreateRowSorter(true);

            frame.add(new JScrollPane(table));
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
