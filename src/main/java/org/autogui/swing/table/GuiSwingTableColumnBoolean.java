package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingViewBooleanCheckBox.PropertyCheckBox;
import org.autogui.swing.GuiSwingViewWrapper;
import org.autogui.swing.util.TextCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;

/**
 * a column factory for {@link Boolean}.
 *
 * <p>
 * Both editor and renderer are realized by {@link PropertyCheckBox}.
 */
public class GuiSwingTableColumnBoolean implements GuiSwingTableColumn {
    public GuiSwingTableColumnBoolean() {}
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          SpecifierManager parentSpecifier) {
        SpecifierManager valueSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);
        PropertyCheckBox view = new ColumnCheckBox(context, valueSpecifier);

        PropertyCheckBox editor = new ColumnCheckBox(context, valueSpecifier, true);

        ObjectTableColumnValue column = new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new ObjectTableColumnValue.ObjectTableCellRenderer(view, rowSpecifier),
                new CheckBoxEditor(GuiSwingTableColumn.wrapEditor(editor), view == editor, rowSpecifier))
                .withBorderType(ObjectTableColumnValue.CellBorderType.Regular);
        column.withComparator(Comparator.comparing(Boolean.class::cast));
        column.setValueType(Boolean.class);

        return column;
    }

    /** a property-check-box for column renderer and editor */
    public static class ColumnCheckBox extends PropertyCheckBox {
        @Serial private static final long serialVersionUID = 1L;
        /** @since 1.6 */
        protected List<Runnable> finishRunners = new ArrayList<>(1);

        public ColumnCheckBox(GuiMappingContext context, SpecifierManager specifierManager) {
            this(context, specifierManager, false);
        }

        /**
         * @param context the context
         * @param specifierManager the specifier
         * @param editor  true if the component is used as an editor
         * @since 1.6
         */
        @SuppressWarnings("this-escape")
        public ColumnCheckBox(GuiMappingContext context, SpecifierManager specifierManager, boolean editor) {
            super(context, specifierManager);
            setCurrentValueSupported(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(true);
            TextCellRenderer.setCellDefaultProperties(this);
            setBorder(BorderFactory.createEmptyBorder());
            setOpaque(false); //clear background
            setText("");
            setFocusPainted(false);
            if (editor) {
                addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_SPACE && e.getModifiersEx() == 0) {
                            setSelected(!isSelected());
                            e.consume();
                            finishRunners.forEach(Runnable::run);
                        }
                    }
                });
                ObjectTableColumnValue.KeyHandlerFinishEditing.installFinishEditingKeyHandler(this, finishRunners);
            }
        }

        @Override
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            finishRunners.add(eventHandler);
            super.addSwingEditFinishHandler(eventHandler);
        }

        @Override
        public void updateFromGui(Object value, GuiTaskClock viewClock) {
            //nothing
        }

        @Override
        protected void paintComponent(Graphics g) {
            var back = getBackground();
            if (back != null) {
                g.setColor(back);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            super.paintComponent(g);
        }
    }

    /**
     * an editor for a boolean value
     */
    public static class CheckBoxEditor extends ObjectTableColumnValue.ObjectTableCellEditor {
        @Serial private static final long serialVersionUID = 1L;

        @SuppressWarnings("this-escape")
        public CheckBoxEditor(JComponent component, boolean skipShutDown, SpecifierManagerIndex specifierIndex) {
            super(component, skipShutDown, specifierIndex);
            setClickCount(0);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            ObjectTableColumnValue.setTableColor(table, component, isSelected, false, row, column);
            var checkBox = getComponentAsCheckBox();
            if (checkBox != null) {
                ObjectTableColumnValue.setTableColor(table, checkBox, isSelected, false, row, column);
            }
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        @Override
        public boolean isCellEditable(EventObject e) {
            if (e instanceof MouseEvent && ((MouseEvent) e).getComponent() instanceof JTable) {
                if (isComponentCheckBoxCenter()) {
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
            }
            return super.isCellEditable(e);
        }

        /**
         * @return the checkbox of the component, might be a wrapped
         * @since 1.6
         */
        public AbstractButton getComponentAsCheckBox() {
            if (component instanceof GuiSwingViewWrapper.ValueWrappingPane<?>) {
                return ((GuiSwingViewWrapper.ValueWrappingPane<?>) component).getSwingViewWrappedPaneAsTypeOrNull(AbstractButton.class);
            } else {
                return (component instanceof AbstractButton) ? ((AbstractButton) component) : null;
            }
        }

        /**
         * @return the checkboxes vertical alignment is center
         * @since 1.6
         */
        public boolean isComponentCheckBoxCenter() {
            var checkBox = getComponentAsCheckBox();
            return checkBox != null && checkBox.getVerticalAlignment() == SwingConstants.CENTER;
        }
    }


}
