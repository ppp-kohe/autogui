package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiTaskClock;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingView.SpecifierManager;
import autogui.swing.GuiSwingViewBooleanCheckBox.PropertyCheckBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.EventObject;

/**
 * a column factory for {@link Boolean}.
 *
 * <p>
 * Both editor and renderer are realized by {@link PropertyCheckBox}.
 */
public class GuiSwingTableColumnBoolean implements GuiSwingTableColumn {

    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          SpecifierManager parentSpecifier) {
        SpecifierManager valueSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);
        PropertyCheckBox view = new ColumnCheckBox(context, valueSpecifier);

        PropertyCheckBox editor = new ColumnCheckBox(context, valueSpecifier);

        ObjectTableColumnValue column = new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new ObjectTableColumnValue.ObjectTableCellRenderer(view, rowSpecifier),
                new CheckBoxEditor(editor, view == editor, rowSpecifier));
        column.withComparator(Comparator.comparing(Boolean.class::cast));
        column.setValueType(Boolean.class);

        return column;
    }

    /** a property-check-box for column renderer and editor */
    public static class ColumnCheckBox extends PropertyCheckBox {
        private static final long serialVersionUID = 1L;

        public ColumnCheckBox(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
            setCurrentValueSupported(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(true);
            setOpaque(true);
            setText("");
        }

        @Override
        public void updateFromGui(Object value, GuiTaskClock viewClock) {
            //nothing
        }
    }

    /**
     * an editor for a boolean value
     */
    public static class CheckBoxEditor extends ObjectTableColumnValue.ObjectTableCellEditor {
        private static final long serialVersionUID = 1L;

        public CheckBoxEditor(JComponent component, boolean skipShutDown, SpecifierManagerIndex specifierIndex) {
            super(component, skipShutDown, specifierIndex);
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
