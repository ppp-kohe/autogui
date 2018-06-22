package autogui.swing.table;

import autogui.base.mapping.*;
import autogui.swing.*;
import autogui.swing.util.*;

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
import java.util.*;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * a table-column with {@link GuiMappingContext}.
 *
 */
public class ObjectTableColumnValue extends ObjectTableColumn
        implements GuiSwingTableColumn.ObjectTableColumnWithContext {
    protected GuiMappingContext context;
    protected GuiSwingTableColumn.SpecifierManagerIndex specifierIndex;
    protected GuiSwingView.SpecifierManager specifierManager;
    protected int contextIndex = -1;
    protected TableCellRenderer renderer;
    protected TableCellEditor editor;

    /**
     * the representation of the context must be a sub-type of {@link GuiReprValue}.
     * view must be a {@link autogui.swing.GuiSwingView.ValuePane}
     * @param context the associated context
     * @param view the component for both editor and renderer
     */
    public ObjectTableColumnValue(GuiMappingContext context, GuiSwingTableColumn.SpecifierManagerIndex specifierIndex,
                                  GuiSwingView.SpecifierManager specifierManager, JComponent view) {
        this(context, specifierIndex, specifierManager, view, view);
    }

    public ObjectTableColumnValue(GuiMappingContext context, GuiSwingTableColumn.SpecifierManagerIndex specifierIndex,
                                  GuiSwingView.SpecifierManager specifierManager, JComponent view, JComponent editorView) {
        this(context, specifierIndex, specifierManager, new ObjectTableCellRenderer(view, specifierIndex),
                editorView == null ? null : new ObjectTableCellEditor(editorView, view == editorView, specifierIndex));
        setRowHeight(view.getPreferredSize().height + UIManagerUtil.getInstance().getScaledSizeInt(4));
    }

    public ObjectTableColumnValue(GuiMappingContext context, GuiSwingTableColumn.SpecifierManagerIndex specifierIndex,
                                  GuiSwingView.SpecifierManager specifierManager, TableCellRenderer renderer, TableCellEditor editor) {
        this.context = context;
        this.renderer = renderer;
        this.editor = editor;
        this.specifierManager = specifierManager;
        this.specifierIndex = specifierIndex;

        GuiRepresentation parentRepr =  context.getParentRepresentation();
        if (parentRepr instanceof GuiReprCollectionElement) {
            this.contextIndex = ((GuiReprCollectionElement) parentRepr).getFixedColumnIndex(context.getParent(), context);
        }

        int size = UIManagerUtil.getInstance().getScaledSizeInt(64);
        setTableColumn(new TableColumn(0, size, renderer, editorForColumn()));
        getTableColumn().setMinWidth(size);
        getTableColumn().setHeaderValue(context.getDisplayName());

        if (renderer instanceof ObjectTableCellRenderer) {
            ((ObjectTableCellRenderer) renderer).setOwnerColumn(this);
        }
    }

    protected TableCellEditor editorForColumn() {
        return context.getReprValue().isEditable(context) ? editor : null;
    }

    @Override
    public GuiSwingView.SpecifierManager getSpecifierManager() {
        return specifierManager;
    }

    @Override
    public void setSettingsWindow(SettingsWindow settingWindow) {
        setForComponents(GuiSwingView.SettingsWindowClient.class,
                p -> p.setSettingsWindow(settingWindow), renderer, editor);
    }

    /**
     * @return null
     */
    @Override
    public SettingsWindow getSettingsWindow() {
        return null;
    }

    @Override
    public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
        setForComponents(GuiSwingPreferences.PreferencesUpdateSupport.class,
                p -> p.setPreferencesUpdater(updater), renderer, editor);
    }

    @Override
    public void loadSwingPreferences(GuiPreferences prefs) {
        setForComponents(GuiSwingView.ValuePane.class,
                p -> p.loadSwingPreferences(prefs), renderer, editor);
    }

    @Override
    public void saveSwingPreferences(GuiPreferences prefs) {
        setForComponents(GuiSwingView.ValuePane.class,
                p -> p.saveSwingPreferences(prefs), renderer, editor);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        setForComponents(GuiSwingView.ValuePane.class,
                GuiSwingView.ValuePane::shutdownSwingView, renderer, editor);
    }

    @Override
    public void setColumnViewUpdater(Consumer<ObjectTableColumn> updater) {
        setForComponents(ColumnViewUpdateSource.class,
                p -> p.setColumnViewUpdater(() -> updater.accept(this)), renderer);
    }

    @Override
    public void viewUpdateAsDynamic(ObjectTableColumn source) {
        setForComponents(ColumnViewUpdateTarget.class,
                p -> p.columnViewUpdateAsDynamic(source), renderer);
    }

    /**
     * an updater holder for renderer component: the component of {@link ObjectTableCellRenderer} may implement the interface
     */
    public interface ColumnViewUpdateSource {
        void setColumnViewUpdater(Runnable updater);
    }

    public interface ColumnViewUpdateTarget {
        void columnViewUpdateAsDynamic(ObjectTableColumn source);
    }

    @Override
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
        GuiReprValue field = context.getReprValue();
        try {
            if (specifierIndex != null) {
                specifierIndex.setIndex(rowIndex);
            }
            return field.getValueWithoutNoUpdate(context, GuiMappingContext.GuiSourceValue.of(rowObject), specifierManager.getSpecifier());
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
        try {
            if (specifierIndex != null) {
                specifierIndex.setIndex(rowIndex);
            }
            GuiReprValue reprValue = context.getReprValue();
            reprValue.addHistoryValue(context, newColumnValue);
            if (reprValue.isEditable(context)) {
                reprValue.update(context, GuiMappingContext.GuiSourceValue.of(rowObject), newColumnValue, specifierManager.getSpecifier());
            }
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
        return null;
    }

    @Override
    public Object getCellValueFromContext(int rowIndex, int columnIndex) {
        GuiReprValue field = context.getReprValue();
        try {
            if (specifierIndex != null) {
                specifierIndex.setIndex(rowIndex);
            }
            return field.getUpdatedValueWithoutNoUpdate(context, specifierManager.getSpecifier());
            //the columnIndex is an index on the view model, so it passes contextIndex as the context's column index
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
            return null;
        }
    }

    @Override
    public Future<?> setCellValueFromContext(int rowIndex, int columnIndex, Object newColumnValue) {
        try {
            if (specifierIndex != null) {
                specifierIndex.setIndex(rowIndex);
            }
            GuiReprValue reprValue = context.getReprValue();
            reprValue.addHistoryValue(context, newColumnValue);
            if (reprValue.isEditable(context)) {
                reprValue.updateWithParentSource(context, newColumnValue, specifierManager.getSpecifier());
            }
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
        return null;
    }

    /** interface for renderer and editor, in order to setting up some properties like preferences and setting-windows */
    public interface ObjectTableColumnCellView {
        JComponent getComponent();

        default boolean isSkippingSet() {
            return false;
        }

        default <CompType> void setForComponent(Class<CompType> componentType, Consumer<CompType> setter) {
            JComponent c = getComponent();
            if (!isSkippingSet() && componentType.isInstance(c)) {
                setter.accept(componentType.cast(c));
            }
        }
    }

    public static <CompType> void setForComponents(Class<CompType> componentType, Consumer<CompType> setter, Object... views) {
        for (Object view : views) {
            if (view instanceof ObjectTableColumnCellView) {
                ((ObjectTableColumnCellView) view).setForComponent(componentType, setter);
            }
        }
    }

    /**
     * a cell renderer with {@link autogui.swing.GuiSwingView.ValuePane}
     */
    public static class ObjectTableCellRenderer implements TableCellRenderer, PopupMenuBuilderSource, ObjectTableColumnCellView {
        protected JComponent component;
        protected ObjectTableColumn ownerColumn;
        protected GuiSwingTableColumn.SpecifierManagerIndex specifierIndex;

        /**
         * @param component the renderer component, must be a {@link GuiSwingView.ValuePane},
         *                   also may be a {@link ColumnViewUpdateSource}
         * @param specifierIndex specifier for the row, nullable
         */
        public ObjectTableCellRenderer(JComponent component, GuiSwingTableColumn.SpecifierManagerIndex specifierIndex) {
            this.component = component;
            this.specifierIndex = specifierIndex;
        }

        @Override
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
            if (specifierIndex != null) {
                specifierIndex.setIndex(row);
            }

            setTableColor(table, component, isSelected);
            Consumer<Object> valuePane = getMenuTargetPane();
            if (valuePane != null) {
                valuePane.accept(value);
            }
            ObjectTableColumn.setCellBorder(table, component, row, column);

            return component;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Consumer<Object> getMenuTargetPane() {
            if (component instanceof GuiSwingView.ValuePane) {
                return ((GuiSwingView.ValuePane) component)::setSwingViewValue;
            } else {
                return null;
            }
        }

        @Override
        public PopupExtension.PopupMenuBuilder getMenuBuilder(JTable table) {
            if (component instanceof GuiSwingView.ValuePane) {
                PopupExtension.PopupMenuBuilder rendererPaneOriginalBuilder = ((GuiSwingView.ValuePane) component).getSwingMenuBuilder();;

                if (rendererPaneOriginalBuilder instanceof PopupCategorized) {
                    ((PopupCategorized) rendererPaneOriginalBuilder).setMenuBuilder(
                            new ObjectTableModel.MenuBuilderWithEmptySeparator());
                }

                return new ObjectTableColumnActionBuilder(table, getOwnerColumn(), rendererPaneOriginalBuilder);
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
        if (component instanceof GuiSwingViewLabel.PropertyLabel) {
            ((GuiSwingViewLabel.PropertyLabel) component).setSelected(isSelected);
        }
    }

    /**
     * a cell-editor with {@link autogui.swing.GuiSwingView.ValuePane}
     */
    public static class ObjectTableCellEditor extends AbstractCellEditor implements TableCellEditor, ObjectTableColumnCellView {
        protected JComponent component;
        protected int clickCount = 2;
        protected boolean skipShutDown;
        protected GuiSwingTableColumn.SpecifierManagerIndex specifierIndex;

        /**
         * @param component the editor component, must be a {@link autogui.swing.GuiSwingView.ValuePane}
         * @param skipShutDown if true, {@link #shutdown()} process for the component will be skipped
         */
        public ObjectTableCellEditor(JComponent component, boolean skipShutDown, GuiSwingTableColumn.SpecifierManagerIndex specifierIndex) {
            this.component = component;
            this.skipShutDown = skipShutDown;
            this.specifierIndex = specifierIndex;
            if (component instanceof GuiSwingView.ValuePane<?>) {
                ((GuiSwingView.ValuePane<?>) component).addSwingEditFinishHandler(this::stopCellEditing);
            }
        }

        @Override
        public JComponent getComponent() {
            return component;
        }

        @Override
        public boolean isSkippingSet() {
            return skipShutDown;
        }

        @Override
        public Object getCellEditorValue() {
            if (component instanceof GuiSwingView.ValuePane<?>) {
                return ((GuiSwingView.ValuePane<?>) component).getSwingViewValue();
            } else {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (specifierIndex != null) {
                specifierIndex.setIndex(row);
            }
            if (component instanceof GuiSwingView.ValuePane<?>) {
                GuiSwingView.ValuePane<Object> pane = (GuiSwingView.ValuePane<Object>) component;
                pane.setSwingViewValue(value);
//                if (table.getModel() instanceof ObjectTableModel) {
//                    ObjectTableModel model = (ObjectTableModel) table.getModel() ;
//                    List<?> rows = model.getSource();
//                    if (rows != null && row < rows.size()) {
//                        Object rowObject = rows.get(row);
//                        //it can give (model, rowObject, row) to the pane as auxiliary info. for updating the context
//                    }
//                }
            }
            UIManagerUtil ui = UIManagerUtil.getInstance();
            component.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(0, ui.getScaledSizeInt(5), 0, ui.getScaledSizeInt(3)),
                    BorderFactory.createCompoundBorder(
                            getTableFocusBorder(),
                            BorderFactory.createEmptyBorder(ui.getScaledSizeInt(1), ui.getScaledSizeInt(5), ui.getScaledSizeInt(1), ui.getScaledSizeInt(2)))));
            return component;
        }

        @Override
        public boolean stopCellEditing() {
            return super.stopCellEditing();
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
        return UIManagerUtil.getInstance().getTableFocusCellHighlightBorder();
    }

    /**
     * a popup-menu builder returned by {@link ObjectTableCellRenderer}
     *  for wrapping an original menu-builder (not intended for a table)
     *    with {@link autogui.swing.table.ObjectTableModel.CollectionRowsAndCellsActionBuilder}.
      */
    public static class ObjectTableColumnActionBuilder implements PopupExtension.PopupMenuBuilder {
        protected JTable table;
        protected ObjectTableColumn column;
        protected PopupExtension.PopupMenuBuilder paneOriginalBuilder;

        public ObjectTableColumnActionBuilder(JTable table, ObjectTableColumn column, PopupExtension.PopupMenuBuilder paneOriginalBuilder) {
            this.table = table;
            this.column = column;
            this.paneOriginalBuilder = paneOriginalBuilder;
        }

        @Override
        public void build(PopupExtension.PopupMenuFilter filter, Consumer<Object> menu) {
            if (table.getModel() instanceof ObjectTableModel) {
                paneOriginalBuilder.build(new CollectionRowsActionBuilder(table, column, filter), menu);
            } else {
                new PopupExtension.PopupMenuBuilderEmpty().build(filter, menu);
            }
        }
    }

    /**
     * an action for selecting cells of a target column and all rows
     */
    public static class ColumnSelectionAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
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

        @Override
        public String getCategory() {
            return MenuBuilder.getCategoryWithPrefix(TableTargetMenu.MENU_COLUMN_ROWS, PopupExtension.MENU_CATEGORY_SELECT);
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_SELECT;
        }
    }

    /**
     * a menu filter for converting an action to another action which supports a selected rows.
     * <ul>
     *  <li>{@link TableTargetMenu#convert(GuiReprCollectionTable.TableTargetColumn)}</li>
     *  <li>the class currently explicitly handles actions in {@link PopupExtensionText} and in {@link SearchTextFieldFilePath}.</li>
     *  <li>a {@link TableTargetCellAction} is converted to a {@link TableTargetExecutionAction}.</li>
     * </ul>
     */
    public static class CollectionRowsActionBuilder implements PopupExtension.PopupMenuFilter {
        protected JTable table;
        protected ObjectTableColumn column;
        protected GuiReprCollectionTable.TableTargetColumn target;
        protected PopupExtension.PopupMenuFilter filter;
        protected boolean afterReturned;

        public CollectionRowsActionBuilder(JTable table, ObjectTableColumn column, PopupExtension.PopupMenuFilter filter) {
            this.table = table;
            this.column = column;
            this.filter = filter;
            target = new TableTargetColumnForJTable(table, column.getTableColumn().getModelIndex());
        }

        @Override
        public Object convert(Object o) {
            Object r;
            if (o instanceof Action) {
                r = convertAction((Action) o);
            } else if (o instanceof TableTargetMenu) {
                r = ((TableTargetMenu) o).convert(target);
            } else if (o instanceof JMenuItem) {
                r = convertAction(((JMenuItem) o).getAction());
            } else {
                r = o;
            }
            r = clearKeyStroke(r);
            if (r != null) {
                return filter.convert(r);
            } else {
                return null;
            }
        }

        public Object convertAction(Action a) {
            if (a instanceof TableTargetColumnAction) {
                return new TableTargetExecutionAction((TableTargetColumnAction) a, target);

            } else if (a instanceof PopupExtensionText.TextCopyAllAction) {
                return new TableTargetInvocationAction(a, target,
                        (e, t) -> ((PopupExtensionText.TextCopyAllAction) a).actionPerformedOnTable(e,
                                t.getSelectedCellValues()));

            } else if (a instanceof PopupExtensionText.TextPasteAllAction) {
                return new TableTargetInvocationAction(a, target,
                        (e, t) -> ((PopupExtensionText.TextPasteAllAction) a)
                                .pasteLines(t::setSelectedCellValuesLoop));

            } else if (a instanceof PopupExtensionText.TextOpenBrowserAction) {
                return new TableTargetInvocationAction(a, target,
                        (e, t) -> ((PopupExtensionText.TextOpenBrowserAction) a)
                                .openList(t.getSelectedCellValues()));

            } else if (a instanceof SearchTextFieldFilePath.FileListEditAction) {
                return new TableTargetInvocationAction(a, target,
                        (e, t) -> ((SearchTextFieldFilePath.FileListEditAction) a)
                                .run(t::setSelectedCellValuesLoop));

            } else if (a instanceof SearchTextFieldFilePath.FileListAction) {
                return new TableTargetInvocationAction(a, target,
                        (e, t) -> ((SearchTextFieldFilePath.FileListAction) a)
                                .run(t.getSelectedCellValues().stream()
                                        .map(Path.class::cast)
                                        .collect(Collectors.toList())));

            } else if (a instanceof GuiSwingActionDefault.ExecutionAction) {
                return new TableRowsRepeatAction(table, column, a);
            } else {
                return null; //disabled
            }
        }

        public Object clearKeyStroke(Object o) {
            if (o instanceof Action) {
                ((Action) o).putValue(Action.ACCELERATOR_KEY, null);
            } else if (o instanceof JMenu) {
                Arrays.stream(((JMenu) o).getComponents())
                        .forEach(this::clearKeyStroke);
            } else if (o instanceof AbstractButton) {
                clearKeyStroke(((AbstractButton) o).getAction());
            }
            return o;
        }

        @Override
        public List<Object> aroundItems(boolean before) {
            if (!before && !afterReturned) {
                List<Object> afters = new ArrayList<>(filter.aroundItems(false));
                Object a = filter.convert(new ColumnSelectionAction(table, column.getTableColumn().getModelIndex()));
                if (a != null) {
                    afters.add(a);
                }
                afterReturned = true;
                return afters;
            } else {
                return filter.aroundItems(before);
            }
        }
    }

    public static class ActionDelegate<TargetActionType extends Action> extends AbstractAction {
        protected TargetActionType action;
        protected Map<String, Object> values;
        protected static Object NULL = new Object();

        public ActionDelegate(TargetActionType action) {
            this.action = action;
        }

        public TargetActionType getAction() {
            return action;
        }

        @Override
        public Object getValue(String key) {
            if (values != null && values.containsKey(key)) {
                Object v = values.get(key);
                if (v == NULL) {
                    v = null;
                }
                return v;
            }
            return action.getValue(key);
        }

        @Override
        public void putValue(String key, Object value) {
            if (values == null) {
                values = new HashMap<>();
            }
            if (value == null) {
                value = NULL;
            }
            values.put(key, value);
        }

        @Override
        public void setEnabled(boolean b) {
            action.setEnabled(b);
        }

        @Override
        public boolean isEnabled() {
            return action.isEnabled();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.actionPerformed(e);
        }
    }

    /**
     * a wrapper class for {@link TableTargetColumnAction}
     */
    public static class TableTargetExecutionAction extends ActionDelegate<TableTargetColumnAction>
            implements PopupCategorized.CategorizedMenuItemAction {
        protected GuiReprCollectionTable.TableTargetColumn target;

        public TableTargetExecutionAction(TableTargetColumnAction action, GuiReprCollectionTable.TableTargetColumn target) {
            super(action);
            this.target = target;
        }

        @Override
        public JComponent getMenuItem() {
            return action.getMenuItemWithAction(this);
        }

        @Override
        public boolean isEnabled() {
            return action.isEnabled(target);
        }

        public GuiReprCollectionTable.TableTargetColumn getTarget() {
            return target;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.actionPerformedOnTableColumn(e, target);
        }

        @Override
        public String getCategory() {
            return MenuBuilder.getCategoryWithPrefix(TableTargetMenu.MENU_COLUMN_ROWS, action.getCategory());
        }

        @Override
        public String getSubCategory() {
            return action.getSubCategory();
        }
    }

    /**
     * an action for selected rows of a column with a lambda
     */
    public static class TableTargetInvocationAction extends ActionDelegate<Action>
            implements PopupCategorized.CategorizedMenuItemAction {
        protected GuiReprCollectionTable.TableTargetColumn target;
        protected BiConsumer<ActionEvent, GuiReprCollectionTable.TableTargetColumn> invoker;

        public TableTargetInvocationAction(Action action, GuiReprCollectionTable.TableTargetColumn target,
                                           BiConsumer<ActionEvent, GuiReprCollectionTable.TableTargetColumn> invoker) {
            super(action);
            this.target = target;
            this.invoker = invoker;
        }

        @Override
        public boolean isEnabled() {
            return !target.isSelectionEmpty();
        }

        public GuiReprCollectionTable.TableTargetColumn getTarget() {
            return target;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            invoker.accept(e, target);
        }

        @Override
        public String getCategory() {
            String category = PopupCategorized.CATEGORY_ACTION;
            if (action instanceof PopupCategorized.CategorizedMenuItem) {
                category = ((PopupCategorized.CategorizedMenuItem) action).getCategory();
            }
            return MenuBuilder.getCategoryWithPrefix(TableTargetMenu.MENU_COLUMN_ROWS, category);
        }

        @Override
        public String getSubCategory() {
            if (action instanceof PopupCategorized.CategorizedMenuItem) {
                return ((PopupCategorized.CategorizedMenuItem) action).getSubCategory();
            } else {
                return "";
            }
        }
    }

    /**
     * an action for wrapping another action.
     *   this action iterates over the selected rows and changing the target column value with the each row value.
     */
    public static class TableRowsRepeatAction extends ActionDelegate<Action> implements PopupCategorized.CategorizedMenuItemAction {
        protected JTable table;
        protected ObjectTableColumn column;

        public TableRowsRepeatAction(JTable table, ObjectTableColumn column, Action action) {
            super(action);
            this.table = table;
            this.column = column;
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
            Consumer<Object> valuePane = (source == null ? null : source.getMenuTargetPane());

            for (int row : table.getSelectedRows()) {
                Object prev = null;
                if (valuePane != null) {
                    int modelRow = table.convertRowIndexToModel(row);
                    prev = table.getModel().getValueAt(modelRow, column.getTableColumn().getModelIndex());
                    //TODO future value?
                    valuePane.accept(prev);
                }
                action.actionPerformed(e);
                if (valuePane != null) {
                    //Object next = valuePane.getSwingViewValue();
                    //TODO compare?

                }
            }
        }

        @Override
        public String getCategory() {
            String category = PopupCategorized.CATEGORY_ACTION;
            if (action instanceof PopupCategorized.CategorizedMenuItem) {
                category = ((PopupCategorized.CategorizedMenuItem) action).getCategory();
            }
            return MenuBuilder.getCategoryWithPrefix(TableTargetMenu.MENU_COLUMN_ROWS, category);
        }

        @Override
        public String getSubCategory() {
            if (action instanceof PopupCategorized.CategorizedMenuItem) {
                return ((PopupCategorized.CategorizedMenuItem) action).getSubCategory();
            } else {
                return "";
            }
        }
    }

    ///////////////


    @Override
    public List<TableMenuComposite> getCompositesForRows() {
        int index = getTableColumn().getModelIndex();
        List<TableMenuComposite> comps = new ArrayList<>(4);
        comps.add(new ToStringCopyCell.TableMenuCompositeToStringCopy(context, index));
        comps.add(new GuiSwingJsonTransfer.TableMenuCompositeJsonCopy(context, index));
        if (context.isReprValue() && context.getReprValue().isEditable(context)) {
            comps.add(new GuiSwingJsonTransfer.TableMenuCompositeJsonPaste(context, index));
            comps.add(new ToStringCopyCell.TableMenuCompositeToStringPaste(context, index));
        }
        return comps;
    }


    @Override
    public List<TableMenuComposite> getCompositesForCells() {
        int index = getTableColumn().getModelIndex();
        List<TableMenuComposite> comps = new ArrayList<>(4);
        comps.add(new ToStringCopyCell.TableMenuCompositeToStringCopy(context, index));
        comps.add(new GuiSwingJsonTransfer.TableMenuCompositeJsonCopy(context, index));
        if (context.isReprValue() && context.getReprValue().isEditable(context)) {
            comps.add(new GuiSwingJsonTransfer.TableMenuCompositeJsonPaste(context, index));
            comps.add(new ToStringCopyCell.TableMenuCompositeToStringPaste(context, index));
        }
        return comps;
    }

}
