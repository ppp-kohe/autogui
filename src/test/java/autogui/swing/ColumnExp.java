package autogui.swing;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class ColumnExp extends GuiSwingTestCase implements TableColumnModelListener {
    public static void main(String[] args) {
        ColumnExp e = new ColumnExp();
        e.run();
    }

    JTable table;
    DataModel model;
    ColumnModel columnModel;

    void view() {
        model = new DataModel();
        columnModel = new ColumnModel();
        table = new JTable(model, columnModel);
        //table.setAutoCreateColumnsFromModel(true);
        columnModel.addColumnModelListener(this);

        JToolBar bar = new JToolBar();
        {
            bar.add(new AddRowsAction());
            bar.add(new AddColsAction());
            bar.add(new MoveColAction());
            bar.add(new RemoveColsAction());
        }
        JComponent pane = new JPanel(new BorderLayout());
        pane.add(bar, BorderLayout.NORTH);
        pane.add(new JScrollPane(table), BorderLayout.CENTER);

        createFrame(pane);
    }

    public void run() {
        run(this::view);
    }

    @Override
    public void columnAdded(TableColumnModelEvent e) {
        System.err.println("columnAdded: " + e.getFromIndex() + "," + e.getToIndex());
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
        System.err.println("columnRemoved: " + e.getFromIndex() + "," + e.getToIndex());
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        System.err.println("columnMoved: " + e.getFromIndex() + "," + e.getToIndex());
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
        System.err.println("columnMarginChanged: " + e);
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        System.err.println("selectionChanged: " + e.getValueIsAdjusting() + "," + e.getFirstIndex() + "," + e.getLastIndex());
    }

    class AddRowsAction extends AbstractAction {
        public AddRowsAction() {
            putValue(NAME, "Add Rows");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            model.setRows(model.getRows() + 10);
        }
    }

    class AddColsAction extends AbstractAction {
        public AddColsAction() {
            putValue(NAME, "Add Cols");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //model.setCols(model.getCols() + 10);
            for (int i = 0; i < 10; ++i) {
                TableColumn col = new TableColumn(columnModel.getColumnCount());
                col.setHeaderValue("col" + columnModel.getColumnCount());
                columnModel.addColumn(col);
            }
            //model.setCols(columnModel.getColumnCount());
            //table.getTableHeader().setPreferredSize(new Dimension(100, 100));
            table.getTableHeader().resizeAndRepaint();
        }
    }

    class MoveColAction extends AbstractAction {
        public MoveColAction() {
            putValue(NAME, "Move Cols: 3 -> 8");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            columnModel.moveColumn(3, 8);
        }
    }

    class RemoveColsAction extends AbstractAction {
        public RemoveColsAction() {
            putValue(NAME, "Remove Cols");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            List<TableColumn> cols = new ArrayList<>();
            for (int i = 0, l = columnModel.getColumnCount(); i < l; i += 2) {
                cols.add(columnModel.getColumn(i));
            }
            cols.forEach(columnModel::removeColumn);
        }
    }

    ////////////////////

    static class DataModel extends AbstractTableModel {
        int rows;
        int cols;

        public void setRows(int n) {
            rows = n;
            fireTableDataChanged();
        }

        public void setCols(int cols) {
            this.cols = cols;
            fireTableStructureChanged();
        }

        public int getRows() {
            return rows;
        }

        public int getCols() {
            return cols;
        }

        @Override
        public int getRowCount() {
            return rows;
        }

        @Override
        public int getColumnCount() {
            return cols;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return "<" + rowIndex + "," + columnIndex + ">";
        }
    }

    static class ColumnModel extends DefaultTableColumnModel {

    }
}
