package autogui.swing.table;

import autogui.swing.GuiSwingTestCase;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.Future;

public class ObjectTableModelTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new ObjectTableModelTest().test();
    }

    @Test
    public void test() {
        List<String> list = new ArrayList<>();
        list.add("hello");
        list.add("world");
        list.add("!!!");
        JScrollPane p = runGet(() -> {
            ObjectTableModel model = new ObjectTableModel(() -> list);
            model.addColumnRowIndex();

            ObjectTableColumn col = new ObjectTableColumn();
            col.setTableColumn(new TableColumn(0, 100, new Renderer(), null));
            col.getTableColumn().setHeaderValue("Hello");
            model.addColumn(col);

            ObjectTableColumn col2 = new ObjectTableColumn() {
                @Override
                public Object getCellValue(Object rowObject, int rowIndex, int columnIndex) {
                    return boolValues.contains(rowObject);
                }

                @Override
                public Future<?> setCellValue(Object rowObject, int rowIndex, int columnIndex, Object newColumnValue) {
                    if (newColumnValue.equals(Boolean.TRUE)) {
                        boolValues.add(rowObject);
                    } else {
                        boolValues.remove(rowObject);
                    }
                    return null;
                }
            };


            col2.setTableColumn(new TableColumn(0, 100, new CheckRenderer(), new CheckEditor()));
            col2.getTableColumn().setHeaderValue("Flag");
            model.addColumn(col2);

            JScrollPane pane = model.initTableWithScroll();
            testFrame(pane);
            return pane;
        });

        JTable table = (JTable) ((JViewport) p.getComponent(0)).getView();
        Assert.assertEquals(3, runGet(() -> (Integer) table.getRowCount()).intValue());


    }

    static class Renderer extends JTextField implements TableCellRenderer {
        public Renderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder());

        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            if (hasFocus) {
                setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            } else {
                setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            }
            setText("" + value);
            return this;
        }
    }

    static class CheckRenderer extends JCheckBox implements TableCellRenderer {
        public CheckRenderer() {
            setText("Example");
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(true);
            setOpaque(true);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            if (hasFocus) {
                setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            } else {
                setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            }
            setSelected(value.equals(Boolean.TRUE));
            return this;
        }
    }

    static Set<Object> boolValues = new HashSet<>();

    static class CheckEditor extends AbstractCellEditor implements TableCellEditor {
        JCheckBox box;
        JTable table;
        int row;
        int column;
        public CheckEditor() {
            box = new JCheckBox();
            box.setText("Example");
            box.addActionListener(e -> stopCellEditing());
            box.setHorizontalAlignment(SwingConstants.CENTER);
            box.setBorderPainted(true);
            box.setOpaque(true);
            box.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    System.err.println(box.getBounds() + " : " + e.getPoint()
                            + " : " + box.getPreferredSize() + " : " + box.getSize());
                }
            });

        }
        @Override
        public Object getCellEditorValue() {
            return box.isSelected();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.table = table;
            this.row = row;
            this.column = column;
            System.err.println(row + " : " + column);
            box.setSelected(value.equals(Boolean.TRUE));
            return box;
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            if (e instanceof MouseEvent) {
                if (table != null) {
                    Rectangle rect = table.getCellRect(row, column, false);
                    System.err.println("rect " + rect);
                    Point p = ((MouseEvent) e).getPoint();
                    int x = p.x - rect.x;
                    int y = p.y - rect.y;
                    Dimension size = box.getPreferredSize();
                    //alignment center
                    int boxX = rect.width / 2 - size.width / 2;
                    int boxY = rect.height / 2 - size.height / 2;

                    if (new Rectangle(boxX, boxY, size.width, size.height).contains(new Point(x, y))) {
                        System.err.println("hit");
                        return true;
                    } else {
                        return false;
                    }
                }
                System.err.println(((MouseEvent) e).getPoint());
                return ((MouseEvent) e).getClickCount() >= 1;
            } else if (e instanceof KeyEvent) {
                int code = ((KeyEvent) e).getKeyCode();
                return code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE;
            } else {
                return true;
            }
        }
    }
}
