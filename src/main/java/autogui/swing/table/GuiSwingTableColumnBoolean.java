package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionElement;
import autogui.base.mapping.GuiReprValueBooleanCheckBox;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.concurrent.Future;

public class GuiSwingTableColumnBoolean implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        return new ObjectTableColumnBooleanCheckBox(context);
    }

    public static class ObjectTableColumnBooleanCheckBox extends ObjectTableColumn {
        protected GuiMappingContext context;

        public ObjectTableColumnBooleanCheckBox(GuiMappingContext context) {
            this.context = context;
            BooleanCheckBoxRenderer renderer = new BooleanCheckBoxRenderer(context);
            GuiReprValueBooleanCheckBox box = (GuiReprValueBooleanCheckBox) context.getRepresentation();

            setTableColumn(new TableColumn(0, 64, renderer,
                    box.isEditable(context) ? new DefaultCellEditor(renderer) : null));
            setComparator((Object a, Object b) -> Boolean.compare((Boolean) a, (Boolean) b));
        }

        @Override
        public Object getCellValue(Object rowObject, int rowIndex, int columnIndex) {
            GuiReprValueBooleanCheckBox check = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            GuiReprCollectionElement col = (GuiReprCollectionElement) context.getParent().getRepresentation();
            try {
                return check.toUpdateValue(context,
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
    }

    public static class BooleanCheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        protected GuiMappingContext context;

        public BooleanCheckBoxRenderer(GuiMappingContext context) {
            this.context = context;
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(true);
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
            setSelected(((GuiReprValueBooleanCheckBox) context.getRepresentation()).toUpdateValue(context, value));
            return this;
        }
    }
}
