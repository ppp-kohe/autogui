package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.table.GuiSwingTableColumnSet;
import autogui.swing.table.GuiSwingTableColumnSetDefault;
import autogui.swing.table.ObjectTableColumn;
import autogui.swing.table.ObjectTableModel;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionText;
import autogui.swing.util.SearchTextFieldFilePath;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.stream.Collectors;

public class GuiSwingViewCollectionTable implements GuiSwingView {
    protected GuiSwingMapperSet columnMapperSet;


    public GuiSwingViewCollectionTable(GuiSwingMapperSet columnMapperSet) {
        this.columnMapperSet = columnMapperSet;
    }

    @Override
    public JComponent createView(GuiMappingContext context) {
        CollectionTable table = new CollectionTable(context);
        List<Action> actions = new ArrayList<>();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement subView = columnMapperSet.view(subContext);
            if (subView != null && subView instanceof GuiSwingTableColumnSet) {
                GuiSwingTableColumnSet columnSet = (GuiSwingTableColumnSet) subView;

                columnSet.createColumns(subContext, table.getObjectTableModel());

                actions.addAll(columnSet.createColumnActions(subContext, table));
            }
        }

        if (context.getParent() != null) {
            for (GuiMappingContext siblingContext : context.getParent().getChildren()) {
                if (siblingContext.isTypeElementActionList() &&
                    siblingContext.getTypeElementAsActionList().getElementType()
                            .equals(context.getTypeElementCollection().getElementType())) {
                    //takes multiple selected items
                    actions.add(new GuiSwingTableColumnSetDefault.TableSelectionListAction(siblingContext, table));
                }
            }
        }
        return table.initAfterAddingColumns(actions);
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static class CollectionTable extends JTable
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane,
                        GuiSwingTableColumnSet.TableSelectionSource {
        protected GuiMappingContext context;
        protected List<?> source;
        protected PopupExtensionCollection popup;
        protected List<Action> actions = new ArrayList<>();

        public CollectionTable(GuiMappingContext context) {
            this.context = context;

            ObjectTableModel model = new ObjectTableModel(this::getSource);
            model.setTable(this);
            setModel(model);
            setColumnModel(model.getColumnModel());

            context.addSourceUpdateListener(this);

            JComponent label = GuiSwingContextInfo.get().getInfoLabel(context);
            List<JComponent> items = new ArrayList<>();
            items.add(label);
            //TODO ?
            popup = new PopupExtensionCollection(this, PopupExtension.getDefaultKeyMatcher(), items);

            setCellSelectionEnabled(true);
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            update(context, context.getSource());
        }

        public JComponent initAfterAddingColumns(List<Action> actions) {
            this.actions.addAll(actions);
            ObjectTableModel model = getObjectTableModel();
            model.initTableWithoutScrollPane(this);
            if (actions.isEmpty()) {
                return model.initTableScrollPane(this);
            } else {
                JPanel pane = new JPanel(new BorderLayout());
                pane.add(initActionToolBar(actions), BorderLayout.PAGE_START);
                pane.add(model.initTableScrollPane(this), BorderLayout.CENTER);
                return pane;
            }
        }

        public JToolBar initActionToolBar(List<Action> actions) {
            JToolBar actionToolBar = new JToolBar();
            actionToolBar.setFloatable(false);
            actionToolBar.setOpaque(false);

            getSelectionModel().addListSelectionListener(e -> {
                boolean enabled = !isSelectionEmpty();
                actions.forEach(a -> a.setEnabled(enabled));
            });

            actions.forEach(a -> initAction(actionToolBar, a));

            return actionToolBar;
        }

        public void initAction(JToolBar actionToolBar, Action action) {
            actionToolBar.add(new GuiSwingViewObjectPane.ActionButton(action));
            action.setEnabled(false);

            String name = (String) action.getValue(Action.NAME);
            if (name != null) {
                getActionMap().put(name, action);
            }
        }

        public List<Action> getActions() {
            return actions;
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        public ObjectTableModel getObjectTableModel() {
            return (ObjectTableModel) getModel();
        }

        public List<?> getSource() {
            return source;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return getSource();
        }

        @Override
        public void setSwingViewValue(Object value) {
            GuiReprCollectionTable repr = (GuiReprCollectionTable) context.getRepresentation();
            source = repr.toUpdateValue(context, value);
            getObjectTableModel().setSourceFromSupplier();
        }

        /////////////////

        @Override
        public boolean isSelectionEmpty() {
            return getSelectionModel().isSelectionEmpty();
        }

        @Override
        public List<?> getSelectedItems() {
            ListSelectionModel sel = getSelectionModel();
            List<Object> selected = new ArrayList<>();
            if (source != null) {
                for (int i = sel.getMinSelectionIndex(), max = sel.getMaxSelectionIndex(); i <= max; ++i) {
                    if (i >= 0 && sel.isSelectedIndex(i)) {
                        selected.add(source.get(convertRowIndexToModel(i)));
                    }
                }
            }
            return selected;
        }

        @Override
        public void selectionActionFinished() {
            ListSelectionModel sel = getSelectionModel();
            List<Integer> is = new ArrayList<>();
            for (int i = sel.getMinSelectionIndex(), max = sel.getMaxSelectionIndex(); i <= max; ++i) {
                if (i >= 0 && sel.isSelectedIndex(i)) {
                    is.add(convertRowIndexToModel(i));
                }
            }

            getObjectTableModel().refreshRows(is.stream()
                    .mapToInt(Integer::intValue).toArray());
        }
    }

    public static class PopupExtensionCollection extends PopupExtension {
        protected CollectionTable table;
        protected int targetColumnIndex = -1;
        protected int lastClickColumnIndex = 0;

        public PopupExtensionCollection(CollectionTable pane, Predicate<KeyEvent> keyMatcher, List<JComponent> items) {
            super(pane, keyMatcher, null);
            this.table = pane;
            setMenuBuilder(new CollectionColumnMenuSupplier(table, () -> {
                List<JComponent> comps = new ArrayList<>();
                comps.addAll(items);
                table.getActions().stream()
                        .map(JMenuItem::new)
                        .forEach(comps::add);
                return comps;
            }));
        }

        public CollectionTable getTable() {
            return table;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            int viewColumn = table.columnAtPoint(e.getPoint());
            lastClickColumnIndex = table.convertColumnIndexToView(viewColumn);
            if (e.isPopupTrigger()) {
                targetColumnIndex = lastClickColumnIndex;
            }
            super.mousePressed(e);
        }

        @Override
        public void showByKey(KeyEvent e, Component comp) {
            int row = table.getSelectedRow();
            int col = table.getSelectedColumn();
            if (col < 0) {
                col = lastClickColumnIndex;
            } else {
                targetColumnIndex = table.convertColumnIndexToModel(col);
            }
            if (row != -1) {
                Rectangle rect = table.getCellRect(row, col, true);
                int x = rect.x + rect.width / 3;
                int y = rect.y + rect.height;
                show(comp, x, y);
            } else {
                super.showByKey(e, comp);
            }
        }

        /** model index*/
        public int getTargetColumnIndex() {
            return targetColumnIndex;
        }

        public ObjectTableColumn getTargetColumn() {
            if (targetColumnIndex >= 0 &&
                    targetColumnIndex < table.getObjectTableModel().getColumnCount()) {
                return table.getObjectTableModel().getColumns().get(targetColumnIndex);
            } else {
                return null;
            }
        }
    }

    public static class CollectionColumnMenuSupplier implements PopupExtension.PopupMenuBuilder {
        protected CollectionTable table;
        protected Supplier<? extends Collection<JComponent>> items;

        public CollectionColumnMenuSupplier(CollectionTable table, Supplier<? extends Collection<JComponent>> items) {
            this.table = table;
            this.items = items;
        }

        @Override
        public void build(PopupExtension sender, Consumer<Object> menu) {
            if (items != null) {
                items.get().forEach(menu::accept);
            }

            if (sender instanceof PopupExtensionCollection) {
                ObjectTableColumn column = ((PopupExtensionCollection) sender).getTargetColumn();
                ObjectTableColumn.PopupMenuBuilderSource src = (column == null ? null : column.getMenuBuilderSource());
                if (src != null) {
                    PopupExtension.PopupMenuBuilder builder = src.getMenuBuilder();
                    if (builder != null) {
                        builder.build(sender, new CollectionRowsActionBuilder(table, column, menu));
                    }
                }
            }
        }

    }

    public static class CollectionRowsActionBuilder implements Consumer<Object> {
        protected CollectionTable table;
        protected ObjectTableColumn column;
        protected Consumer<Object> menu;
        protected TableTarget target;

        public CollectionRowsActionBuilder(CollectionTable table, ObjectTableColumn column, Consumer<Object> menu) {
            this.table = table;
            this.column = column;
            this.menu = menu;
            target = new TableTarget(table, table.getObjectTableModel().getColumns().indexOf(column));
        }

        @Override
        public void accept(Object o) {
            if (o instanceof Action) {
                addAction((Action) o);
            } else if (o instanceof JMenuItem) {
                Action action = ((JMenuItem) o).getAction();
                addAction(action);
            } else {
                menu.accept(o);
            }
        }

        public void addAction(Action a) {
            if (a instanceof TableTargetAction) {
                menu.accept(new TableTargetExecutionAction((TableTargetAction) a, target));

            } else if (a instanceof PopupExtensionText.TextCopyAllAction) {
                menu.accept(new TableTargetInvocationAction(a, target,
                        (e, t) -> ((PopupExtensionText.TextCopyAllAction) a).actionPerformedOnTable(e,
                                t.getSelectedCellValues().values())));

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
                                .run(t.getSelectedCellValues().values().stream()
                                        .map(Path.class::cast)
                                        .collect(Collectors.toList()))));

            } else if (a instanceof GuiSwingActionDefault.ExecutionAction) {
                menu.accept(new CollectionRowsAction(table, column, a));
            }
        }
    }

    public static class CollectionRowsAction extends AbstractAction {
        protected CollectionTable table;
        protected ObjectTableColumn column;
        protected Action action;

        public CollectionRowsAction(CollectionTable table, ObjectTableColumn column, Action action) {
            this.table = table;
            this.column = column;
            this.action = action;
            putValue(NAME, action.getValue(NAME));
            putValue(Action.LARGE_ICON_KEY, action.getValue(LARGE_ICON_KEY));
        }

        @Override
        public boolean isEnabled() {
            return !table.isSelectionEmpty() && action.isEnabled();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ObjectTableColumn.PopupMenuBuilderSource source = (column == null ? null : column.getMenuBuilderSource());
            ValuePane valuePane = (source == null ? null : source.getMenuTargetPane());

            for (int row : table.getSelectedRows()) {
                Object prev = null;
                if (valuePane != null) {
                    int modelRow = table.convertRowIndexToModel(row);
                    prev = table.getObjectTableModel().getValueAt(modelRow, table.getObjectTableModel().getColumns().indexOf(column));
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

    public static class TableTargetExecutionAction extends AbstractAction {
        protected TableTargetAction action;
        protected TableTarget target;

        public TableTargetExecutionAction(TableTargetAction action, TableTarget target) {
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

        public TableTarget getTarget() {
            return target;
        }

        public TableTargetAction getAction() {
            return action;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.actionPerformedOnTable(e, target);
        }
    }

    public static class TableTargetInvocationAction extends AbstractAction {
        protected Action action;
        protected TableTarget target;
        protected BiConsumer<ActionEvent, TableTarget> invoker;

        public TableTargetInvocationAction(Action action, TableTarget target, BiConsumer<ActionEvent, TableTarget> invoker) {
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

        public TableTarget getTarget() {
            return target;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            invoker.accept(e, target);
        }
    }

    public interface TableTargetAction extends Action {
        void actionPerformedOnTable(ActionEvent e, TableTarget target);
    }

    public static class TableTarget {
        protected JTable table;
        /** model index */
        protected int column;

        public TableTarget(JTable table, int column) {
            this.table = table;
            this.column = column;
        }

        public boolean isSelectionEmpty() {
            return table.getSelectionModel().isSelectionEmpty();
        }

        public Object getSelectedCellValue() {
            int row = table.getSelectedRow();
            if (row < 0) {
                return null;
            } else {
                int modelRow = table.convertRowIndexToModel(row);
                return table.getModel().getValueAt(modelRow, column);
            }
        }

        public Map<Integer,Object> getSelectedCellValues() {
            int[] rows = table.getSelectedRows();
            Map<Integer, Object> map = new TreeMap<>();
            TableModel model = table.getModel();
            for (int row : rows) {
                int modelRow = table.convertRowIndexToView(row);
                map.put(modelRow, model.getValueAt(modelRow, column));
            }
            return map;
        }

        public List<Integer> getSelectedRows() {
            return Arrays.stream(table.getSelectedRows())
                    .map(table::convertRowIndexToModel)
                    .sorted()
                    .boxed()
                    .collect(Collectors.toList());
        }

        public void setCellValues(Map<Integer,?> rowToValues) {
            TableModel model = table.getModel();
            rowToValues.forEach((modelRow, value) ->
                    model.setValueAt(value, modelRow, column));
        }

        public void setSelectedCellValues(Function<Integer, Object> rowToNewValue) {
            TableModel model = table.getModel();
            for (int row : getSelectedRows()) {
                int modelRow = table.convertRowIndexToModel(row);
                model.setValueAt(rowToNewValue.apply(modelRow), modelRow, column);
            }
        }

        public void setSelectedCellValuesLoop(List<?> rowValues) {
            if (!rowValues.isEmpty()) {
                TableModel model = table.getModel();
                int size = rowValues.size();
                int i = 0;
                for (int row : getSelectedRows()) {
                    int modelRow = table.convertRowIndexToModel(row);
                    Object value = rowValues.get(i % size);
                    model.setValueAt(value, modelRow, column);
                    ++i;
                }
            }
        }
    }
}
