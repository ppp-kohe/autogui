package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionElement;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingView;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.concurrent.Future;

public class ObjectTableColumnValue extends ObjectTableColumn {
    protected GuiMappingContext context;

    /**
     * the representation of the context must be a sub-type of {@link GuiReprValue}.
     * view must be a {@link autogui.swing.GuiSwingView.ValuePane} */
    public ObjectTableColumnValue(GuiMappingContext context, JComponent view) {
        this(context, view, view);
    }

    public ObjectTableColumnValue(GuiMappingContext context, JComponent view, JComponent editorView) {
        this(context, new ObjectTableCellRenderer(view),
                editorView == null ? null : new ObjectTableCellEditor(editorView));
        setRowHeight(view.getPreferredSize().height + 4);
    }

    public ObjectTableColumnValue(GuiMappingContext context, TableCellRenderer renderer, TableCellEditor editor) {
        this.context = context;
        GuiReprValue value = (GuiReprValue) context.getRepresentation();
        setTableColumn(new TableColumn(0, 64, renderer,
                value.isEditable(context) ? editor : null));
        getTableColumn().setHeaderValue(context.getDisplayName());
    }

    @Override
    public Object getCellValue(Object rowObject, int rowIndex, int columnIndex) {
        GuiReprValue field = (GuiReprValue) context.getRepresentation();
        GuiReprCollectionElement col = (GuiReprCollectionElement) context.getParent().getRepresentation();
        try {
            return field.toUpdateValue(context,
                    col.getCellValue(context.getParent(), context, rowObject, rowIndex, columnIndex));
        } catch (Exception ex) {
            context.errorWhileUpdateSource(ex);
            return null;
        }
    }

    @Override
    public Future<?> setCellValue(Object rowObject, int rowIndex, int columnIndex, Object newColumnValue) {
        GuiReprCollectionElement col = (GuiReprCollectionElement) context.getParent().getRepresentation();
        col.updateCellFromGui(context.getParent(), context, rowObject, rowIndex, columnIndex, newColumnValue);
        return null;
    }

    public static class ObjectTableCellRenderer implements TableCellRenderer {
        protected JComponent component;

        /** component must be {@link GuiSwingView.ValuePane }*/
        public ObjectTableCellRenderer(JComponent component) {
            this.component = component;
        }

        public JComponent getComponent() {
            return component;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setTableColor(table, component, isSelected);
            if (component instanceof GuiSwingView.ValuePane) {
                ((GuiSwingView.ValuePane) component).setSwingViewValue(value);
            }

            component.setBorder(BorderFactory.createMatteBorder(0, 10, 0, 5, component.getBackground()));
            return component;
        }
    }

    public static void setTableColor(JTable table, JComponent component, boolean isSelected) {
        if (isSelected) {
            component.setForeground(table.getSelectionForeground());
            component.setBackground(table.getSelectionBackground());
        } else {
            component.setForeground(table.getForeground());
            component.setBackground(table.getBackground());
        }
    }

    public static class ObjectTableCellEditor extends AbstractCellEditor implements TableCellEditor {
        protected JComponent component;
        protected int clickCount = 2;

        /**
         * component must be {@link autogui.swing.GuiSwingView.ValuePane}
         */
        public ObjectTableCellEditor(JComponent component) {
            this.component = component;
            if (component instanceof GuiSwingView.ValuePane) {
                ((GuiSwingView.ValuePane) component).addSwingEditFinishHandler(e -> stopCellEditing());
            }
        }

        public JComponent getComponent() {
            return component;
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
            component.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(0, 5, 0, 3),
                    BorderFactory.createCompoundBorder(
                            getTableFocusBorder(),
                            BorderFactory.createEmptyBorder(0, 5, 0, 2))));
            return component;
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            if (e instanceof MouseEvent) {
                return ((MouseEvent) e).getClickCount() >= getClickCount();
            } else if (e instanceof KeyEvent) {
                int code = ((KeyEvent) e).getKeyCode();
                return code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE;
            } else {
                return true;
            }
        }

        public void setClickCount(int clickCount) {
            this.clickCount = clickCount;
        }

        public int getClickCount() {
            return clickCount;
        }
    }


    public static Border getTableFocusBorder() {
        return UIManager.getBorder("Table.focusCellHighlightBorder");
    }
}
