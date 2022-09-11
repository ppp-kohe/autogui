package org.autogui.swing.table;

import org.autogui.base.mapping.*;
import org.autogui.base.mapping.GuiReprCollectionTable.TableTargetColumn;
import org.autogui.base.mapping.GuiReprValue.ObjectSpecifier;
import org.autogui.swing.GuiSwingActionDefault;
import org.autogui.swing.GuiSwingJsonTransfer;
import org.autogui.swing.GuiSwingPreferences;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingView.ValuePane;
import org.autogui.swing.GuiSwingViewLabel.PropertyLabel;
import org.autogui.swing.table.GuiSwingTableColumn.SpecifierManagerIndex;
import org.autogui.swing.util.*;
import org.autogui.swing.util.PopupCategorized.CategorizedMenuItem;
import org.autogui.swing.util.PopupCategorized.CategorizedMenuItemAction;
import org.autogui.swing.util.PopupExtension.PopupMenuBuilder;
import org.autogui.swing.util.PopupExtension.PopupMenuFilter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * a table-column with {@link GuiMappingContext}.
 *
 */
public class ObjectTableColumnValue extends ObjectTableColumn
        implements GuiSwingTableColumn.ObjectTableColumnWithContext {
    protected GuiMappingContext context;
    protected SpecifierManagerIndex specifierIndex;
    protected SpecifierManager specifierManager;
    protected int contextIndex = -1;
    protected TableCellRenderer renderer;
    protected TableCellEditor editor;
    /** @since 1.6 */
    protected boolean editorForColumnAlwaysApplying;

    /**
     * the representation of the context must be a sub-type of {@link GuiReprValue}.
     * view must be a {@link ValuePane}
     * @param context the associated context
     * @param specifierIndex the optional index for row-index (nullable)
     * @param specifierManager specifier factory for the column value
     * @param view the component for both editor and renderer
     */
    public ObjectTableColumnValue(GuiMappingContext context, SpecifierManagerIndex specifierIndex,
                                  SpecifierManager specifierManager, JComponent view) {
        this(context, specifierIndex, specifierManager, view, view);
    }

    public ObjectTableColumnValue(GuiMappingContext context, SpecifierManagerIndex specifierIndex,
                                  SpecifierManager specifierManager, JComponent view, JComponent editorView) {
        this(context, specifierIndex, specifierManager, new ObjectTableCellRenderer(view, specifierIndex),
                editorView == null ? null : new ObjectTableCellEditor(editorView, view == editorView, specifierIndex));
        setRowHeight(view.getPreferredSize().height + UIManagerUtil.getInstance().getScaledSizeInt(4));
    }

    public ObjectTableColumnValue(GuiMappingContext context, SpecifierManagerIndex specifierIndex,
                                  SpecifierManager specifierManager, TableCellRenderer renderer, TableCellEditor editor) {
        this.context = context;
        this.renderer = renderer;
        this.editor = editor;
        this.specifierManager = specifierManager;
        this.specifierIndex = specifierIndex;

        GuiRepresentation parentRepr =  context.getParentRepresentation();
        if (parentRepr instanceof GuiReprCollectionElement) {
            this.contextIndex = ((GuiReprCollectionElement) parentRepr).getFixedColumnIndex(context.getParent(), context);
        }

        int size = UIManagerUtil.getInstance().getScaledSizeInt(32);
        setTableColumn(new TableColumn(0, size, renderer, editorForColumn()));
        getTableColumn().setMinWidth(size);
        getTableColumn().setHeaderValue(context.getDisplayName());

        if (renderer instanceof ObjectTableCellRenderer) {
            ((ObjectTableCellRenderer) renderer).setOwnerColumn(this);
        }
    }

    /**
     * indicating whether the editor needs a specific margin border
     * @since 1.6
     */
    public enum CellBorderType {
        Regular(new Insets(8, 8, 8, 8)),
        Spinner(new Insets(3, 8, 8, 1)),
        ComboBox(new Insets(3, 4, 8, 8)),
        FilePath(new Insets(8, 10, 8, 8)),
        EditorPane(new Insets(8, 10, 8, 8));

        final Insets insets;
        CellBorderType(Insets insets) {
            this.insets = insets;
        }
        public Insets getMargin() {
            return insets;
        }
    }

    /**
     * specify the margin border when the editor is a {@link ObjectTableCellEditor}.
     * @param type the border type
     * @return this
     * @since 1.6
     */
    public ObjectTableColumnValue withBorderType(CellBorderType type) {
        if (editor instanceof ObjectTableCellEditor) {
            Insets margin = type.getMargin();
            ((ObjectTableCellEditor) editor).setMarginBorder(BorderFactory.createEmptyBorder(margin.top, margin.left, margin.bottom, margin.right));
        }
        return this;
    }

    /**
     * @param alwaysApplying true for {@link #editorForColumn()} always returning {@link #editor}
     * @return this
     * @since 1.6
     */
    public ObjectTableColumnValue withEditorForColumnAlwaysApplying(boolean alwaysApplying) {
        this.editorForColumnAlwaysApplying = alwaysApplying;
        return this;
    }

    protected TableCellEditor editorForColumn() {
        return (editorForColumnAlwaysApplying || context.getReprValue().isEditable(context)) ? editor : null;
    }

    @Override
    public SpecifierManager getSpecifierManager() {
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
        setForComponents(ValuePane.class,
                p -> p.loadSwingPreferences(prefs), renderer, editor);
    }

    @Override
    public void saveSwingPreferences(GuiPreferences prefs) {
        setForComponents(ValuePane.class,
                p -> p.saveSwingPreferences(prefs), renderer, editor);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        setForComponents(ValuePane.class,
                v -> v.shutdownSwingView(), renderer, editor);
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
     * an updater holder for renderer component:
     *   the component of {@link ObjectTableCellRenderer} may implement the interface.
     *    if the component updates a visual property, then it can notify the change by calling the updater
     */
    public interface ColumnViewUpdateSource {
        void setColumnViewUpdater(Runnable updater);
    }

    /**
     * an updating target for renderer component:
     *   the component of {@link ObjectTableCellRenderer} may implement the interface.
     *   once a visual property of related columns is updated, the method of the interface will be invoked.
     */
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

    @Override
    public ObjectSpecifier getSpecifier(int rowIndex, int columnIndex) {
        if (specifierIndex != null) {
            specifierIndex.setIndex(rowIndex);
        }
        SpecifierManager m = getSpecifierManager();
        if (m != null) {
            return m.getSpecifier();
        } else {
            return null;
        }
    }

    /**
     *
     * @param rowObject the row object at rowIndex
     * @param rowIndex   the row index in the list
     * @param columnIndex the column index of the view table-model of the column
     * @param specifier the specifier for the column value, can be obtained by {@link #getSpecifier(int, int)}
     * @return the column value
     */
    @Override
    public Object getCellValue(Object rowObject, int rowIndex, int columnIndex, ObjectSpecifier specifier) {
        GuiReprValue field = context.getReprValue();
        try {
            return field.getValueWithoutNoUpdate(context, GuiMappingContext.GuiSourceValue.of(rowObject), specifier);
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
     * @param specifier the specifier for the column value, can be obtained by {@link #getSpecifier(int, int)}
     * @return null
     */
    @Override
    public Future<?> setCellValue(Object rowObject, int rowIndex, int columnIndex, Object newColumnValue, ObjectSpecifier specifier) {
        try {
            GuiReprValue reprValue = context.getReprValue();
            reprValue.addHistoryValue(context, newColumnValue);
            if (reprValue.isEditable(context)) {
                reprValue.update(context, GuiMappingContext.GuiSourceValue.of(rowObject), newColumnValue, specifier);
            }
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
        return null;
    }

    @Override
    public Object getCellValueFromContext(int rowIndex, int columnIndex, ObjectSpecifier specifier) {
        GuiReprValue field = context.getReprValue();
        try {
            return field.getUpdatedValueWithoutNoUpdate(context, specifier);
            //the columnIndex is an index on the view model, so it passes contextIndex as the context's column index
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
            return null;
        }
    }

    @Override
    public Future<?> setCellValueFromContext(int rowIndex, int columnIndex, Object newColumnValue, ObjectSpecifier specifier) {
        try {
            GuiReprValue reprValue = context.getReprValue();
            reprValue.addHistoryValue(context, newColumnValue);
            if (reprValue.isEditable(context)) {
                reprValue.updateWithParentSource(context, newColumnValue, specifier);
            }
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
        return null;
    }

    @Override
    public Object toActionValue(Object value) {
        try {
            return context.getReprValue().toUpdateValue(context, value);
        } catch (Throwable ex) {
            context.errorWhileUpdateSource(ex);
        }
        return null;
    }

    @Override
    public Object fromActionValue(Object value) {
        //special handling for FilePath
        if (context.getRepresentation() instanceof GuiReprValueFilePathField &&
            value instanceof Path) {
            return ((GuiReprValueFilePathField) context.getRepresentation())
                    .toValueFromPath(context, (Path) value);
        } else {
            return super.fromActionValue(value);
        }
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
     * a cell renderer with {@link ValuePane}
     */
    public static class ObjectTableCellRenderer implements TableCellRenderer, PopupMenuBuilderSource, ObjectTableColumnCellView {
        protected JComponent component;
        protected ObjectTableColumn ownerColumn;
        protected SpecifierManagerIndex specifierIndex;

        /**
         * @param component the renderer component, must be a {@link ValuePane},
         *                   also may be a {@link ColumnViewUpdateSource}
         * @param specifierIndex specifier for the row, nullable
         */
        public ObjectTableCellRenderer(JComponent component, SpecifierManagerIndex specifierIndex) {
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

            setTableColor(table, component, isSelected, hasFocus, row, column);
            Consumer<Object> valuePane = getMenuTargetPane();
            if (valuePane != null) {
                valuePane.accept(value);
            }
            TextCellRenderer.setCellTableBorderWithMargin(table, component, isSelected, hasFocus, row, column);
            return component;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Consumer<Object> getMenuTargetPane() {
            if (component instanceof ValuePane) {
                return ((ValuePane) component)::setSwingViewValue;
            } else {
                return null;
            }
        }

        @Override
        public PopupMenuBuilder getMenuBuilder(JTable table) {
            if (component instanceof ValuePane) {
                PopupMenuBuilder rendererPaneOriginalBuilder = ((ValuePane) component).getSwingMenuBuilder();;

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
        setTableColor(table, component, isSelected, false, 0, 0);
    }

    /**
     * @param table the table
     * @param component the cell component
     * @param isSelected the row is selected if true
     * @param hasFocus the row is focused if true
     * @param row        the row index
     * @param column     the column index
     * @since 1.2
     */
    public static void setTableColor(JTable table, JComponent component, boolean isSelected, boolean hasFocus, int row, int column) {
        TextCellRenderer.setCellTableColor(table, component, isSelected, hasFocus, row, column);
        if (component instanceof PropertyLabel) {
            ((PropertyLabel) component).setSelected(isSelected);
        }
    }

    /**
     * a cell-editor with {@link ValuePane}
     */
    public static class ObjectTableCellEditor extends AbstractCellEditor implements TableCellEditor, ObjectTableColumnCellView {
        private static final long serialVersionUID = 1L;
        protected JComponent component;
        protected int clickCount = 2;
        protected boolean skipShutDown;
        protected SpecifierManagerIndex specifierIndex;
        /** @since 1.6 */
        protected Border originalBorder;
        /** @since 1.6 */
        protected Border marginBorder;

        /**
         * @param component the editor component, must be a {@link ValuePane}
         * @param skipShutDown if true, {@link #shutdown()} process for the component will be skipped
         * @param specifierIndex optional specifier for row-index (nullable)
         */
        public ObjectTableCellEditor(JComponent component, boolean skipShutDown, SpecifierManagerIndex specifierIndex) {
            this.component = component;
            this.skipShutDown = skipShutDown;
            this.specifierIndex = specifierIndex;
            if (component instanceof ValuePane<?>) {
                ((ValuePane<?>) component).addSwingEditFinishHandler(this::stopCellEditing);
            }
            this.originalBorder = component.getBorder();
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
            if (component instanceof ValuePane<?>) {
                return ((ValuePane<?>) component).getSwingViewValue();
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
            if (component instanceof ValuePane<?>) {
                ValuePane<Object> pane = (ValuePane<Object>) component;
                pane.setSwingViewValue(value);
                SwingUtilities.invokeLater(pane::requestSwingViewFocus);
//                if (table.getModel() instanceof ObjectTableModel) {
//                    ObjectTableModel model = (ObjectTableModel) table.getModel() ;
//                    List<?> rows = model.getSource();
//                    if (rows != null && row < rows.size()) {
//                        Object rowObject = rows.get(row);
//                        //it can give (model, rowObject, row) to the pane as auxiliary info. for updating the context
//                    }
//                }
            }
            TextCellRenderer.setCellTableColor(table, component, isSelected, true, row, column);
            setBorders(table, column);
            return component;
        }

        /**
         * @param marginBorder an additional border
         * @since 1.6
         */
        public void setMarginBorder(Border marginBorder) {
            this.marginBorder = marginBorder;
        }

        /**
         * @param table the target table
         * @param column column view index
         * @since 1.6
         */
        protected void setBorders(JTable table, int column) {
            component.setBorder(originalBorder);
            if (marginBorder != null) {
                TextCellRenderer.wrapBorder(component, marginBorder, false);
            }
            /*
            TextCellRenderer.wrapBorder(component,
                    TextCellRenderer.createBorder(1,
                            TextCellRenderer.isCellTableEnd(table, column, true) ? 1 : 0, 1,
                            TextCellRenderer.isCellTableEnd(table, column, false) ? 1 : 0, getCellBackgroundEditingBorder()), false);
                            */
        }

        /**
         * called from {@link #setBorders(JTable, int)}
         * @return null
         * @since 1.6
         */
        protected Color getCellBackgroundEditing() {
            return null;
        }

        /**
         *  called from {@link #setBorders(JTable, int)}         *
         * @return table-background
         * @since 1.6
         */
        protected Color getCellBackgroundEditingBorder() {
            return UIManagerUtil.getInstance().getTableBackground();
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
                KeyEvent ke = (KeyEvent) e;
                if (KeyHandlerFinishEditing.isKeyEventFinishEditing(ke)) {
                    return true;
                }
                int code = ke.getKeyCode();
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

    /**
     * a handler for finishing editor by alt+enter
     * @since 1.6
     */
    public static class KeyHandlerFinishEditing extends KeyAdapter {
        protected Supplier<List<Runnable>> finishRunners;

        public KeyHandlerFinishEditing(Supplier<List<Runnable>> finishRunners) {
            this.finishRunners = finishRunners;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            //keyPressed, instead of keyReleased: isCellEditable(e) responds for keyPressed, and then it sends keyReleased event to the component
            if (isKeyEventFinishEditing(e)) {
                finishRunners.get().forEach(Runnable::run);
                e.consume();
            }
        }

        /**
         * @param e the tested event
         * @return true if alt+enter
         */
        public static boolean isKeyEventFinishEditing(KeyEvent e) {
            return e.getKeyCode() == KeyEvent.VK_ENTER && e.getModifiersEx() == KeyEvent.ALT_DOWN_MASK;
        }

        /**
         * call {@link #installFinishEditingKeyHandler(Component, Supplier)}
         * @param c the target component
         * @param finishRunners finishers added by {@link ValuePane#addSwingEditFinishHandler(Runnable)}
         * @since 1.6
         */
        public static KeyListener installFinishEditingKeyHandler(Component c, List<Runnable> finishRunners) {
            return installFinishEditingKeyHandler(c, () -> finishRunners);
        }

        /**
         * provides general key-handling handler of finish editing
         * @param c the target component
         * @param finishRunners finishers added by {@link ValuePane#addSwingEditFinishHandler(Runnable)}
         * @since 1.6
         */
        public static KeyListener installFinishEditingKeyHandler(Component c, Supplier<List<Runnable>> finishRunners) {
            var h = new KeyHandlerFinishEditing(finishRunners);
            c.addKeyListener(h);
            return h;
        }
    }


    public static Border getTableFocusBorder() {
        return UIManagerUtil.getInstance().getTableFocusCellHighlightBorder();
    }

    /**
     * a popup-menu builder returned by {@link ObjectTableCellRenderer}
     *  for wrapping an original menu-builder (not intended for a table)
     *    with {@link ObjectTableModel.CollectionRowsAndCellsActionBuilder}.
      */
    public static class ObjectTableColumnActionBuilder implements PopupMenuBuilder {
        protected JTable table;
        protected ObjectTableColumn column;
        protected PopupMenuBuilder paneOriginalBuilder;

        public ObjectTableColumnActionBuilder(JTable table, ObjectTableColumn column, PopupMenuBuilder paneOriginalBuilder) {
            this.table = table;
            this.column = column;
            this.paneOriginalBuilder = paneOriginalBuilder;
        }

        @Override
        public void build(PopupMenuFilter filter, Consumer<Object> menu) {
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
    public static class ColumnSelectionAction extends AbstractAction implements CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
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
    public static class CollectionRowsActionBuilder implements PopupMenuFilter {
        protected JTable table;
        protected ObjectTableColumn column;
        protected TableTargetColumn target;
        protected PopupMenuFilter filter;
        protected boolean afterReturned;

        public CollectionRowsActionBuilder(JTable table, ObjectTableColumn column, PopupMenuFilter filter) {
            this.table = table;
            this.column = column;
            this.filter = filter;
            target = new TableTargetColumnForObjectColumn(column,
                    new TableTargetColumnForJTable(table, column.getTableColumn().getModelIndex()));
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

            } else if (a instanceof PopupExtensionText.TextClearAction) {
                return new TableTargetInvocationAction(a, target,
                        (e, t) -> ((PopupExtensionText.TextClearAction) a)
                                .clearLines(t::setSelectedCellValuesLoop));

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

    /** a base class for wrapping an action */
    public static class ActionDelegate<TargetActionType extends Action> extends AbstractAction {
        private static final long serialVersionUID = 1L;
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
            implements CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected TableTargetColumn target;

        public TableTargetExecutionAction(TableTargetColumnAction action, TableTargetColumn target) {
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

        public TableTargetColumn getTarget() {
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
            implements CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected TableTargetColumn target;
        protected BiConsumer<ActionEvent, TableTargetColumn> invoker;

        public TableTargetInvocationAction(Action action, TableTargetColumn target,
                                           BiConsumer<ActionEvent, TableTargetColumn> invoker) {
            super(action);
            this.target = target;
            this.invoker = invoker;
        }

        @Override
        public boolean isEnabled() {
            return !target.isSelectionEmpty();
        }

        public TableTargetColumn getTarget() {
            return target;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            invoker.accept(e, target);
        }

        @Override
        public String getCategory() {
            String category = PopupCategorized.CATEGORY_ACTION;
            if (action instanceof CategorizedMenuItem) {
                category = ((CategorizedMenuItem) action).getCategory();
            }
            return MenuBuilder.getCategoryWithPrefix(TableTargetMenu.MENU_COLUMN_ROWS, category);
        }

        @Override
        public String getSubCategory() {
            if (action instanceof CategorizedMenuItem) {
                return ((CategorizedMenuItem) action).getSubCategory();
            } else {
                return "";
            }
        }
    }

    /**
     * an action for wrapping another action.
     *   this action iterates over the selected rows and changing the target column value with the each row value.
     */
    public static class TableRowsRepeatAction extends ActionDelegate<Action> implements CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
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
            PopupMenuBuilderSource source = (column == null ? null : column.getMenuBuilderSource());
            Consumer<Object> valuePane = (source == null ? null : source.getMenuTargetPane());

            for (int row : table.getSelectedRows()) {
                Object prev = null;
                if (valuePane != null) {
                    int modelRow = table.convertRowIndexToModel(row);
                    prev = table.getModel().getValueAt(modelRow, column.getTableColumn().getModelIndex());
                    valuePane.accept(prev);
                }
                if (action instanceof GuiSwingActionDefault.ExecutionAction) {
                    ((GuiSwingActionDefault.ExecutionAction) action).actionPerformedWithoutCheckingRunning(e);
                } else {
                    action.actionPerformed(e);
                }
                if (valuePane != null) {
                    //Object next = valuePane.getSwingViewValue();
                }
            }
        }

        @Override
        public String getCategory() {
            String category = PopupCategorized.CATEGORY_ACTION;
            if (action instanceof CategorizedMenuItem) {
                category = ((CategorizedMenuItem) action).getCategory();
            }
            return MenuBuilder.getCategoryWithPrefix(TableTargetMenu.MENU_COLUMN_ROWS, category);
        }

        @Override
        public String getSubCategory() {
            if (action instanceof CategorizedMenuItem) {
                return ((CategorizedMenuItem) action).getSubCategory();
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
