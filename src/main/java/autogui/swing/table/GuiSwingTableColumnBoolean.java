package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewBooleanCheckBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.EventObject;

/**
 * a column factory for {@link Boolean}.
 *
 * <p>
 * Both editor and renderer are realized by {@link autogui.swing.GuiSwingViewBooleanCheckBox.PropertyCheckBox}.
 */
public class GuiSwingTableColumnBoolean implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox view = new GuiSwingViewBooleanCheckBox.PropertyCheckBox(context);
        view.setHorizontalAlignment(SwingConstants.CENTER);
        view.setBorderPainted(true);
        view.setOpaque(true);


        GuiSwingViewBooleanCheckBox.PropertyCheckBox editor = new GuiSwingViewBooleanCheckBox.PropertyCheckBox(context);
        editor.setHorizontalAlignment(SwingConstants.CENTER);
        editor.setBorderPainted(true);
        editor.setOpaque(true);

        ObjectTableColumnValue column = new ObjectTableColumnValue(context,
                new ObjectTableColumnValue.ObjectTableCellRenderer(view),
                new CheckBoxEditor(editor, view == editor));
        column.withComparator(Comparator.comparing(Boolean.class::cast));

        return column;
    }

    /**
     * an editor for a boolean value
     */
    public static class CheckBoxEditor extends ObjectTableColumnValue.ObjectTableCellEditor {
        public CheckBoxEditor(JComponent component, boolean skipShutDown) {
            super(component, skipShutDown);
            setClickCount(0);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
            ObjectTableColumnValue.setTableColor(table, component, isSelected);
            return c;
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            if (e instanceof MouseEvent && ((MouseEvent) e).getComponent() instanceof JTable) {
                //alignment of component is CENTER,
                // and its bounds are sized by preferred size, that might be smaller than the cell.
                JTable table = (JTable) ((MouseEvent) e).getComponent();
                Point p = ((MouseEvent) e).getPoint();
                int row = table.rowAtPoint(p);
                int col = table.columnAtPoint(p);
                Rectangle cellRect = table.getCellRect(row, col, true);
                Point internalPoint = new Point(p.x - cellRect.x, p.y - cellRect.y);

                Dimension checkBoxSize = component.getPreferredSize();

                Rectangle checkBoxRect = new Rectangle(
                        cellRect.width / 2 - checkBoxSize.width / 2,
                        cellRect.height / 2 - checkBoxSize.height / 2,
                        checkBoxSize.width, checkBoxSize.height);

                if (!checkBoxRect.contains(internalPoint)) {
                    return false;
                }
            }
            return super.isCellEditable(e);
        }

    }
}
