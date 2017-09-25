package autogui.swing.table;

import autogui.swing.GuiSwingView;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public class ObjectTableCellEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
    protected JComponent component;

    /** component must be {@link autogui.swing.GuiSwingView.ValuePane}*/
    public ObjectTableCellEditor(JComponent component) {
        this.component = component;
    }

    @Override
    public Object getCellEditorValue() {
        if (component instanceof GuiSwingView.ValuePane) {
            return ((GuiSwingView.ValuePane) component).getSwingViewValue();
        } else {
            return null;
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        if (component instanceof GuiSwingView.ValuePane) {
            ((GuiSwingView.ValuePane) component).setSwingViewValue(value);
        }
        return component;
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        if (e instanceof MouseEvent) {
            return ((MouseEvent) e).getClickCount() >= 2;
        } else if (e instanceof KeyEvent) {
            return((KeyEvent) e).getKeyCode() == KeyEvent.VK_ENTER;
        } else {
            return true;
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            component.setForeground(table.getSelectionForeground());
            component.setBackground(table.getSelectionBackground());
        } else {
            component.setForeground(table.getForeground());
            component.setBackground(table.getBackground());
        }
        if (hasFocus) {
            component.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
        } else {
            component.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        }
        if (component instanceof GuiSwingView.ValuePane) {
            ((GuiSwingView.ValuePane) component).setSwingViewValue(value);
        }
        return component;
    }
}
