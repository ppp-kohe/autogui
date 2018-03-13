package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionElement;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingActionDefault;
import autogui.swing.GuiSwingJsonTransfer;
import autogui.swing.GuiSwingView;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionText;
import autogui.swing.util.SearchTextFieldFilePath;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ObjectTableColumnValue extends ObjectTableColumn {
    protected GuiMappingContext context;
    protected int contextIndex = -1;

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
        this.contextIndex = ((GuiReprCollectionElement) context.getParent().getRepresentation())
                .getFixedColumnIndex(context.getParent(), context);

        GuiReprValue value = (GuiReprValue) context.getRepresentation();
        setTableColumn(new TableColumn(0, 64, renderer,
                value.isEditable(context) ? editor : null));
        getTableColumn().setHeaderValue(context.getDisplayName());

        if (renderer instanceof ObjectTableCellRenderer) {
            ((ObjectTableCellRenderer) renderer).setOwnerColumn(this);
        }
    }

    public GuiMappingContext getContext() {
        return context;
    }

    public int getContextIndex() {
        return contextIndex;
    }

    /**
     *
     * @param rowObject the row object at rowIndex
     * @param rowIndex   the row index in the list
     * @param columnIndex the column index of the view table-model of the column
     * @return the column value
     */
    @Override
    public Object getCellValue(Object rowObject, int rowIndex, int columnIndex) {
        GuiReprValue field = (GuiReprValue) context.getRepresentation();
        GuiReprCollectionElement col = (GuiReprCollectionElement) context.getParent().getRepresentation();
        try {
            return field.toUpdateValue(context,
                    col.getCellValue(context.getParent(), context, rowObject, rowIndex, this.contextIndex));
               //the columnIndex is an index on the view model, so it passes contextIndex as the context's column index
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
            return null;
        }
    }

    /**
     *
     * @param rowObject the row object at rowIndex
     * @param rowIndex  the row index in the list
     * @param columnIndex the column index of the view table-model of the column
     * @param newColumnValue the new value to be set
     * @return null
     */
    @Override
    public Future<?> setCellValue(Object rowObject, int rowIndex, int columnIndex, Object newColumnValue) {
        GuiReprCollectionElement col = (GuiReprCollectionElement) context.getParent().getRepresentation();
        col.updateCellFromGui(context.getParent(), context, rowObject, rowIndex, this.contextIndex, newColumnValue);
        return null;
    }

    public static class ObjectTableCellRenderer implements TableCellRenderer, PopupMenuBuilderSource {
        protected JComponent component;
        protected ObjectTableColumn ownerColumn;

        /** component must be {@link GuiSwingView.ValuePane }*/
        public ObjectTableCellRenderer(JComponent component) {
            this.component = component;
        }

        public JComponent getComponent() {
            return component;
        }

        public void setOwnerColumn(ObjectTableColumn ownerColumn) {
            this.ownerColumn = ownerColumn;
        }

        public ObjectTableColumn getOwnerColumn() {
            return ownerColumn;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setTableColor(table, component, isSelected);
            GuiSwingView.ValuePane<Object> valuePane = getMenuTargetPane();
            if (valuePane != null) {
                valuePane.setSwingViewValue(value);
            }

            component.setBorder(BorderFactory.createMatteBorder(0, 10, 0, 5, component.getBackground()));
            return component;
        }

        @SuppressWarnings("unchecked")
        @Override
        public GuiSwingView.ValuePane<Object> getMenuTargetPane() {
            if (component instanceof GuiSwingView.ValuePane) {
                return (GuiSwingView.ValuePane<Object>) component;
            } else {
                return null;
            }
        }

        @Override
        public PopupExtension.PopupMenuBuilder getMenuBuilder() {
            if (component instanceof GuiSwingView.ValuePane) {
                PopupExtension.PopupMenuBuilder rendererPaneOriginalBuilder = ((GuiSwingView.ValuePane) component).getSwingMenuBuilder();;
                return new ObjectTableColumnActionBuilder(getOwnerColumn(), rendererPaneOriginalBuilder);
            } else {
                return null;
            }
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
                return ((GuiSwingView.ValuePane<?>) component).getSwingViewValue();
            } else {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (component instanceof GuiSwingView.ValuePane) {
                ((GuiSwingView.ValuePane<Object>) component).setSwingViewValue(value);
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


    public static class ObjectTableColumnActionBuilder implements PopupExtension.PopupMenuBuilder {
        protected ObjectTableColumn column;
        protected PopupExtension.PopupMenuBuilder paneOriginalBuilder;

        public ObjectTableColumnActionBuilder(ObjectTableColumn column, PopupExtension.PopupMenuBuilder paneOriginalBuilder) {
            this.column = column;
            this.paneOriginalBuilder = paneOriginalBuilder;
        }

        @Override
        public void build(PopupExtension sender, Consumer<Object> menu) {
            JComponent pane = sender.getPane();
            if (pane instanceof JTable &&
                    ((JTable) pane).getModel() instanceof ObjectTableModel) {
                JTable table = (JTable) pane;

                paneOriginalBuilder.build(sender, new CollectionRowsActionBuilder(table, column, menu));

                addColumnSelection(table, menu);
            }
        }

        protected void addColumnSelection(JTable table, Consumer<Object> menu) {
            menu.accept(new ColumnSelectionAction(table, column.getTableColumn().getModelIndex()));
        }
    }

    public static class ColumnSelectionAction extends AbstractAction {
        protected JTable table;
        protected int column;

        public ColumnSelectionAction(JTable table, int column) {
            putValue(NAME, "Select Column All Cells");
            this.table = table;
            this.column = column;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            table.getColumnModel().getSelectionModel().setSelectionInterval(column, column);
            table.getSelectionModel().setSelectionInterval(0, table.getRowCount());
        }
    }

    public static class CollectionRowsActionBuilder implements Consumer<Object> {
        protected JTable table;
        protected ObjectTableColumn column;
        protected Consumer<Object> menu;
        protected GuiReprCollectionTable.TableTargetColumn target;

        public CollectionRowsActionBuilder(JTable table, ObjectTableColumn column, Consumer<Object> menu) {
            this.table = table;
            this.column = column;
            this.menu = menu;
            target = new TableTargetColumnForJTable(table, column.getTableColumn().getModelIndex());
        }

        @Override
        public void accept(Object o) {
            if (o instanceof Action) {
                addAction((Action) o);
            } else if (o instanceof TableTargetMenu) {
                menu.accept(((TableTargetMenu) o).convert(target));
            } else if (o instanceof JMenuItem) {
                Action action = ((JMenuItem) o).getAction();
                addAction(action);
            } else {
                menu.accept(o);
            }
        }

        public void addAction(Action a) {
            if (a instanceof TableTargetColumnAction) {
                menu.accept(new TableTargetExecutionAction((TableTargetColumnAction) a, target));

            } else if (a instanceof PopupExtensionText.TextCopyAllAction) {
                menu.accept(new TableTargetInvocationAction(a, target,
                        (e, t) -> ((PopupExtensionText.TextCopyAllAction) a).actionPerformedOnTable(e,
                                t.getSelectedCellValues())));

            } else if (a instanceof PopupExtensionText.TextPasteAllAction) {
                menu.accept(new TableTargetInvocationAction(a, target,
                        (e, t) -> ((PopupExtensionText.TextPasteAllAction) a)
                                .pasteLines(t::setSelectedCellValuesLoop)));

            } else if (a instanceof SearchTextFieldFilePath.FileListEditAction) {
                menu.accept(new TableTargetInvocationAction(a, target,
                        (e, t) -> ((SearchTextFieldFilePath.FileListEditAction) a)
                                .run(t::setSelectedCellValuesLoop)));

            } else if (a instanceof SearchTextFieldFilePath.FileListAction) {
                menu.accept(new TableTargetInvocationAction(a, target,
                        (e, t) -> ((SearchTextFieldFilePath.FileListAction) a)
                                .run(t.getSelectedCellValues().stream()
                                        .map(Path.class::cast)
                                        .collect(Collectors.toList()))));

            } else if (a instanceof GuiSwingActionDefault.ExecutionAction) {
                menu.accept(new TableRowsRepeatAction(table, column, a));
            }
            //else : disabled
        }
    }

    public static class TableTargetExecutionAction extends AbstractAction {
        protected TableTargetColumnAction action;
        protected GuiReprCollectionTable.TableTargetColumn target;

        public TableTargetExecutionAction(TableTargetColumnAction action, GuiReprCollectionTable.TableTargetColumn target) {
            this.action = action;
            this.target = target;
        }

        @Override
        public boolean isEnabled() {
            return !target.isSelectionEmpty();
        }

        @Override
        public Object getValue(String key) {
            return action.getValue(key);
        }

        public GuiReprCollectionTable.TableTargetColumn getTarget() {
            return target;
        }

        public TableTargetColumnAction getAction() {
            return action;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.actionPerformedOnTableColumn(e, target);
        }
    }

    public static class TableTargetInvocationAction extends AbstractAction {
        protected Action action;
        protected GuiReprCollectionTable.TableTargetColumn target;
        protected BiConsumer<ActionEvent, GuiReprCollectionTable.TableTargetColumn> invoker;

        public TableTargetInvocationAction(Action action, GuiReprCollectionTable.TableTargetColumn target, BiConsumer<ActionEvent, GuiReprCollectionTable.TableTargetColumn> invoker) {
            this.action = action;
            this.target = target;
            this.invoker = invoker;
        }

        @Override
        public boolean isEnabled() {
            return !target.isSelectionEmpty();
        }

        @Override
        public Object getValue(String key) {
            return action.getValue(key);
        }

        public GuiReprCollectionTable.TableTargetColumn getTarget() {
            return target;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            invoker.accept(e, target);
        }
    }


    public static class TableRowsRepeatAction extends AbstractAction {
        protected JTable table;
        protected ObjectTableColumn column;
        protected Action action;

        public TableRowsRepeatAction(JTable table, ObjectTableColumn column, Action action) {
            this.table = table;
            this.column = column;
            this.action = action;
            putValue(NAME, action.getValue(NAME));
            putValue(Action.LARGE_ICON_KEY, action.getValue(LARGE_ICON_KEY));
        }

        @Override
        public boolean isEnabled() {
            return !table.getSelectionModel().isSelectionEmpty() && action.isEnabled();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ObjectTableColumn.PopupMenuBuilderSource source = (column == null ? null : column.getMenuBuilderSource());
            GuiSwingView.ValuePane<Object> valuePane = (source == null ? null : source.getMenuTargetPane());

            for (int row : table.getSelectedRows()) {
                Object prev = null;
                if (valuePane != null) {
                    int modelRow = table.convertRowIndexToModel(row);
                    prev = table.getModel().getValueAt(modelRow, column.getTableColumn().getModelIndex());
                    //TODO future value?
                    valuePane.setSwingViewValue(prev);
                }
                action.actionPerformed(e);
                if (valuePane != null) {
                    Object next = valuePane.getSwingViewValue();
                    //TODO compare?

                }
            }
        }
    }

    ///////////////


    @Override
    public List<TableMenuComposite> getCompositesForRows() {
        int index = getTableColumn().getModelIndex();
        return Arrays.asList(
                new ToStringCopyCell.TableMenuCompositeToStringValue(context, index),
                new GuiSwingJsonTransfer.TableMenuCompositeJsonCopy(context, index));
    }


    @Override
    public List<TableMenuComposite> getCompositesForCells() {
        int index = getTableColumn().getModelIndex();
        return Arrays.asList(
                new ToStringCopyCell.TableMenuCompositeToStringValue(context, index),
                new GuiSwingJsonTransfer.TableMenuCompositeJsonCopy(context, index));
    }

}
