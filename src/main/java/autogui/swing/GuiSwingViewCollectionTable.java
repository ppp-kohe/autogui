package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprActionList;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.table.*;
import autogui.swing.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <h3>representation</h3>
 * {@link GuiReprCollectionTable}
 *
 * <h3>{@link CollectionTable#getSwingViewValue()}</h3>
 * latest set {@link List}.
 *
 * <h3>history-value</h3>
 *  Currently unsupported.
 *
 * <h3>string-transfer</h3>
 * entire copying is supported by owned {@link autogui.swing.GuiSwingViewPropertyPane.PropertyPane}.
 *  the entire copying is handled by {@link autogui.base.mapping.GuiReprPropertyPane#toHumanReadableString(GuiMappingContext, Object)} -&gt;
 *  {@link autogui.base.mapping.GuiReprObjectPane#toHumanReadableStringFromObject(GuiMappingContext, Object)}  -&gt;
 *    {@link autogui.base.mapping.GuiReprCollectionElement#toHumanReadableString(GuiMappingContext, Object)}.
 *  <p>
 *      selected rows or cells can be achieved by {@link autogui.swing.table.ToStringCopyCell}
 *      based on {@link autogui.swing.table.TableTargetCellAction}.
 */
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

        if (context.hasParent()) {
            GuiMappingContext parent = context.getParent();
            if (parent.isTypeElementProperty() && parent.hasParent()) {
                parent = parent.getParent();
            }
            for (GuiMappingContext siblingContext : parent.getChildren()) {
                if (siblingContext.getRepresentation() instanceof GuiReprActionList) {
                    GuiReprActionList listAction = (GuiReprActionList) siblingContext.getRepresentation();
                    if (listAction.isSelectionAction(siblingContext, context)) {
                        actions.add(new GuiSwingTableColumnSetDefault.TableSelectionListAction(siblingContext, table));
                        //isAutomaticSelectionAction(context) is included
                    } else if (listAction.isAutomaticSelectionRowIndexesAction(siblingContext)) {
                        actions.add(new GuiSwingTableColumnSetDefault.TableSelectionListAction(siblingContext,
                                table.getSelectionSourceForRowIndexes()));
                    } else if (listAction.isAutomaticSelectionRowAndColumnIndexesAction(siblingContext)) {
                        actions.add(new GuiSwingTableColumnSetDefault.TableSelectionListAction(siblingContext,
                                table.getSelectionSourceForRowAndColumnIndexes()));
                    }
                }
            }
        }
        return table.setupAfterAddingColumns(actions);
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static class CollectionTable extends JTable
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<List<?>>,
                        GuiSwingTableColumnSet.TableSelectionSource, GuiSwingPreferences.PreferencesUpdateSupport {
        protected GuiMappingContext context;
        protected List<?> source;
        protected PopupExtensionCollection popup;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected List<Action> actions = new ArrayList<>();
        protected List<GuiSwingTableColumnSetDefault.TableSelectionListAction> autoSelectionActions = new ArrayList<>();
        protected int autoSelectionDepth;
        protected ScheduledTaskRunner.EditingRunner selectionRunner;

        protected TablePreferencesUpdater preferencesUpdater;
        protected List<Integer> lastSelectionActionIndexes = Collections.emptyList();
        protected TableSelectionSourceForIndexes selectionSourceForRowIndexes;
        protected TableSelectionSourceForIndexes selectionSourceForRowAndColumnIndexes;

        public CollectionTable(GuiMappingContext context) {
            this.context = context;
            init();
        }

        public void init() {
            initName();
            initModel();
            initContextUpdate();
            initPopup();
            initGrid();
            initSelection();
            initSelectionSource();
            initSelectionClear();
            initValue();
            initPreferencesUpdater();
            initDragDrop();
            initFocus();
        }

        public void initName() {
            setName(context.getName());
            GuiSwingView.setDescriptionToolTipText(context, this);
        }

        public void initModel() {
            ObjectTableModel model = new ObjectTableModel(this::getSource);
            model.setTable(this);
            setModel(model);
            setColumnModel(model.getColumnModel());
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initPopup() {
            JComponent label = GuiSwingContextInfo.get().getInfoLabel(context);
            List<JComponent> items = new ArrayList<>();
            items.add(label);
            items.add(new JMenuItem(new ContextRefreshAction(context)));
            items.add(new JMenuItem(new SelectAllAction(this)));

            MouseListener[] listeners = getMouseListeners();
            Arrays.stream(listeners).forEach(this::removeMouseListener);
            popup = new PopupExtensionCollection(this, PopupExtension.getDefaultKeyMatcher(), items);
            Arrays.stream(listeners).forEach(this::addMouseListener);
            //improve precedence of the popup listener
        }

        public void initGrid() {
            setGridColor(getBackground());
            setShowGrid(false);
        }

        public void initSelection() {
            selectionRunner = new ScheduledTaskRunner.EditingRunner(200, this::runAutoSelectionActions);
            setCellSelectionEnabled(true);
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }

        public void initSelectionSource() {
            selectionSourceForRowIndexes = new TableSelectionSourceForIndexes(this, false);
            selectionSourceForRowAndColumnIndexes = new TableSelectionSourceForIndexes(this, true);
        }

        public void initSelectionClear() {
            UnSelectAction unSelectAction = new UnSelectAction(this);
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), unSelectAction.getValue(Action.NAME));
            getActionMap().put(unSelectAction.getValue(Action.NAME), unSelectAction);
        }

        public void initValue() {
            update(context, context.getSource());
        }

        public void initPreferencesUpdater() {
            preferencesUpdater = new TablePreferencesUpdater(this, context);
        }

        public void initDragDrop() {
            //TODO drag drop
            setTransferHandler(new ToStringCollectionTransferHandler(this));
        }

        public void initFocus() {
            GuiSwingView.setupCopyAndPasteActions(this);
            setFocusable(true);
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(); //TODO
            }
            return menuItems;
        }

        public PopupExtensionCollection getPopup() {
            return popup;
        }

        public JComponent setupAfterAddingColumns(List<Action> actions) {
            this.actions.addAll(actions);

            actions.stream()
                    .filter(GuiSwingTableColumnSetDefault.TableSelectionListAction.class::isInstance)
                    .map(GuiSwingTableColumnSetDefault.TableSelectionListAction.class::cast)
                    .filter(GuiSwingTableColumnSetDefault.TableSelectionListAction::isAutomaticSelectionAction)
                    .forEach(this.autoSelectionActions::add);

            ObjectTableModel model = getObjectTableModel();
            model.initTableWithoutScrollPane(this);

            getColumnModel().addColumnModelListener(preferencesUpdater);
            getRowSorter().addRowSorterListener(preferencesUpdater);

            if (actions.isEmpty()) {
                return initTableScrollPane();
            } else {
                JPanel pane = new GuiSwingView.ValueWrappingPane(initTableScrollPane());
                pane.add(initActionToolBar(actions), BorderLayout.PAGE_START);
                return pane;
            }
        }

        public ValueScrollPane initTableScrollPane() {
            ValueScrollPane scrollPane = new GuiSwingView.ValueScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            initTableScrollPane(scrollPane);
            return scrollPane;
        }

        public void initTableScrollPane(JScrollPane scrollPane) {
            int width = getObjectTableModel().getColumns().stream()
                    .mapToInt(e -> e.getTableColumn().getWidth())
                    .sum();
            scrollPane.setPreferredSize(new Dimension(width, Math.max(scrollPane.getPreferredSize().height, 100)));
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(10);
        }

        public JToolBar initActionToolBar(List<Action> actions) {
            JToolBar actionToolBar = new JToolBar();
            actionToolBar.setFloatable(false);
            actionToolBar.setOpaque(false);

            getSelectionModel().addListSelectionListener(this::runListSelection);
            actions.forEach(a -> initAction(actionToolBar, a));

            return actionToolBar;
        }

        public void runListSelection(ListSelectionEvent e) {
            if (autoSelectionDepth <= 0) {
                autoSelectionDepth++;
                try {
                    boolean enabled = !isSelectionEmpty();
                    actions.forEach(a -> a.setEnabled(enabled));

                    if (enabled && !e.getValueIsAdjusting()) { //excludes events under mouse-pressing state
                        selectionRunner.schedule(e);
                    }
                } finally {
                    autoSelectionDepth--;
                }
            }
        }

        public void runAutoSelectionActions(List<Object> es) {
            this.autoSelectionActions.forEach(
                    GuiSwingTableColumnSetDefault.TableSelectionListAction::actionPerformedBySelection);
        }

        public void initAction(JToolBar actionToolBar, Action action) {
            actionToolBar.add(new GuiSwingIcons.ActionButton(action));
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

        public TableSelectionSourceForIndexes getSelectionSourceForRowAndColumnIndexes() {
            return selectionSourceForRowAndColumnIndexes;
        }

        public TableSelectionSourceForIndexes getSelectionSourceForRowIndexes() {
            return selectionSourceForRowIndexes;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue((List<?>) newValue));
        }

        @Override
        public List<?> getSwingViewValue() {
            return getSource();
        }

        @Override
        public void setSwingViewValue(List<?> value) {
            GuiReprCollectionTable repr = (GuiReprCollectionTable) context.getRepresentation();
            source = repr.toUpdateValue(context, value);
            getObjectTableModel().setSourceFromSupplier();
        }

        @Override
        public void setSwingViewValueWithUpdate(List<?> value) {
            setSwingViewValue(value);
            GuiReprCollectionTable table = (GuiReprCollectionTable) getContext().getRepresentation();
            if (table.isEditable(context)) {
                table.updateFromGui(getContext(), value);
            }
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            this.preferencesUpdater.setUpdater(updater);
        }

        @Override
        public void shutdown() {
            getObjectTableModel().getColumns()
                    .forEach(ObjectTableColumn::shutdown);
            selectionRunner.shutdown();
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
            List<Integer> selectedIndexes = new ArrayList<>();
            if (source != null) {
                for (int i = sel.getMinSelectionIndex(), max = sel.getMaxSelectionIndex(); i <= max; ++i) {
                    if (i >= 0 && sel.isSelectedIndex(i)) {
                        int modelIndex = convertRowIndexToModel(i);
                        selectedIndexes.add(modelIndex);
                        selected.add(source.get(modelIndex));
                    }
                }
            }
            lastSelectionActionIndexes = selectedIndexes;
            return selected;
        }

        @Override
        public void selectionActionFinished(boolean autoSelection) {
            try {
                if (autoSelection) {
                    autoSelectionDepth++;
                }
                ListSelectionModel sel = getSelectionModel();

                int rows = getRowCount();
                int[] selectedRowsIndexes = lastSelectionActionIndexes.stream()
                        .filter(i -> i >= 0 && i < rows)
                        .mapToInt(Integer::intValue).toArray();
                getObjectTableModel().refreshRows(selectedRowsIndexes);

                //re-selection
                sel.setValueIsAdjusting(true);
                IntStream.of(selectedRowsIndexes)
                    .map(this::convertRowIndexToView)
                    .forEach(i -> sel.addSelectionInterval(i, i));
                sel.setValueIsAdjusting(false);
            } finally {
                if (autoSelection) {
                    autoSelectionDepth--;
                }
            }
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }

        @Override
        public void loadPreferences(GuiPreferences prefs) {
            GuiSwingView.loadPreferencesDefault(this, prefs);
            GuiPreferences targetPrefs = prefs.getDescendant(getContext());
            preferencesUpdater.apply(targetPrefs);
        }

        @Override
        public void savePreferences(GuiPreferences prefs) {
            GuiSwingView.savePreferencesDefault(this, prefs);
            GuiPreferences targetPrefs = prefs.getDescendant(getContext());
            PreferencesForTable p = preferencesUpdater.getPrefs();
            p.saveTo(targetPrefs);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            paintSelectedRows(g2);
        }

        protected void paintSelectedRows(Graphics2D g2) {
            Rectangle rect = g2.getClipBounds();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double top = rect.getY();
            double bottom = rect.getMaxY();
            double x = rect.getX();

            int start = rowAtPoint(new Point((int) x, (int) top));
            int end = rowAtPoint(new Point((int) x, (int) bottom));
            if (end < 0) {
                end = getRowCount() - 1;
            }

            g2.setColor(getSelectionBackground());
            g2.setStroke(new BasicStroke(1));

            ListSelectionModel sel = getSelectionModel();
            int cols = getColumnCount();
            for (int i = start; i <= end; ++i) {
                if (sel.isSelectedIndex(i)) {
                    Rectangle row = null;
                    for (int c = 0; c < cols; ++c) {
                        Rectangle r = getCellRect(i, c, false);
                        if (row == null) {
                            row= r;
                        } else {
                            row.add(r);
                        }
                    }
                    if (row != null) {
                        g2.draw(new RoundRectangle2D.Float(row.x + 1, row.y + 1, row.width - 3, row.height - 3, 5, 5));
                    }
                }
            }
        }
    }

    /**
     * a table source for obtaining indexes instead of row items.
     * This can be supplied for {@link autogui.swing.table.GuiSwingTableColumnSetDefault.TableSelectionListAction}.
     */
    public static class TableSelectionSourceForIndexes implements GuiSwingTableColumnSet.TableSelectionSource {
        protected CollectionTable table;
        protected TableTargetCellForJTable cellTargets;
        protected boolean rowAndColumns;

        public TableSelectionSourceForIndexes(CollectionTable table, boolean rowAndColumns) {
            this.table = table;
            cellTargets = new TableTargetCellForJTable(table);
            this.rowAndColumns = rowAndColumns;
        }

        @Override
        public boolean isSelectionEmpty() {
            return table.isSelectionEmpty();
        }

        @Override
        public List<?> getSelectedItems() {
            table.getSelectedItems(); //saving selected indexes

            if (rowAndColumns) {
                //List<int[]>
                return cellTargets.getSelectedRowAllCellIndexesStream()
                        .collect(Collectors.toList());
            } else {
                //List<Integer>
                return cellTargets.getSelectedRows()
                        .boxed()
                        .collect(Collectors.toList());
            }
        }

        @Override
        public void selectionActionFinished(boolean autoSelection) {
            table.selectionActionFinished(autoSelection);
        }
    }

    public static class TablePreferencesUpdater implements TableColumnModelListener, RowSorterListener {
        protected JTable table;
        protected GuiMappingContext context;
        protected PreferencesForTable prefs;
        protected Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater;

        protected boolean savingDisabled = false;

        public TablePreferencesUpdater(JTable table, GuiMappingContext context) {
            this.table = table;
            this.context = context;
            prefs = new PreferencesForTable();
        }

        public void setPrefs(PreferencesForTable prefs) {
            this.prefs = prefs;
        }

        public void setUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            this.updater = updater;
        }

        public void apply(GuiPreferences p) {
            savingDisabled = true;
            prefs.loadFrom(p);
            prefs.applyTo(table);
            savingDisabled = false;
        }

        public void applyJson(Map<String,Object> json) {
            savingDisabled = true;
            prefs.setJson(json);
            prefs.applyTo(table);
            savingDisabled = false;
        }

        public PreferencesForTable getPrefs() {
            return prefs;
        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {
            prefs.setColumnOrderFrom(table);
            sendToUpdater();
        }

        public void sendToUpdater() {
            if (updater != null) {
                updater.accept(new GuiSwingPreferences.PreferencesUpdateEvent(context, prefs));
            }
        }

        @Override
        public void columnMarginChanged(ChangeEvent e) {
            if (!savingDisabled) {
                prefs.setColumnWidthFrom(table);
                sendToUpdater();
            }
        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {}

        @Override
        public void columnRemoved(TableColumnModelEvent e) {}

        @Override
        public void columnAdded(TableColumnModelEvent e) {}

        @Override
        public void sorterChanged(RowSorterEvent e) {
            if (!savingDisabled) {
                prefs.setRowSortFrom(table);
                sendToUpdater();
            }
        }
    }

    public static class PreferencesForTable implements GuiSwingPreferences.PreferencesByJsonEntry {
        protected List<Integer> columnOrder = new ArrayList<>();
        protected List<Integer> columnWidth = new ArrayList<>();
        protected List<PreferencesForTableRowSort> rowSort = new ArrayList<>();

        public void applyTo(JTable table) {
            TableColumnModel columnModel = table.getColumnModel();
            applyColumnWidthTo(table, columnModel);
            applyColumnOrderTo(table, columnModel);
        }

        public void applyColumnWidthTo(JTable table, TableColumnModel columnModel) {
            for (int i = 0, len = Math.min(table.getColumnCount(), columnWidth.size()); i < len; ++i) { //i is model index
                columnModel.getColumn(table.convertColumnIndexToView(i)).setPreferredWidth(columnWidth.get(i));
            }
        }

        public void applyColumnOrderTo(JTable table, TableColumnModel columnModel) {
            for (int i = 0, len = Math.min(table.getColumnCount(), columnOrder.size()); i < len; ++i) {
                int from = columnOrder.get(i);
                if (from >= 0 && from < table.getColumnCount()) {
                    columnModel.moveColumn(table.convertColumnIndexToView(from), i);
                }
            }
        }

        public void applyRowSortTo(JTable table) {
            List<RowSorter.SortKey> keys = new ArrayList<>(rowSort.size());
            for (int i = 0, len = Math.min(table.getColumnCount(), rowSort.size()); i < len; ++i) {
                PreferencesForTableRowSort row = rowSort.get(i);
                SortOrder order = SortOrder.valueOf(row.getOrder());
                keys.add(new RowSorter.SortKey(row.getColumn(), order));
            }
            table.getRowSorter().setSortKeys(keys);
        }

        public void setColumnOrderFrom(JTable table) {
            TableColumnModel columnModel = table.getColumnModel();
            columnOrder.clear();
            for (int i = 0, len = columnModel.getColumnCount(); i < len; ++i) {
                columnOrder.add(table.convertColumnIndexToModel(i));
            }
        }

        public void setColumnWidthFrom(JTable table) {
            TableColumnModel columnModel = table.getColumnModel();
            columnWidth.clear();
            for (int i = 0, len = columnModel.getColumnCount(); i < len; ++i) {
                columnWidth.add(columnModel.getColumn(table.convertColumnIndexToView(i)).getWidth());
            }
        }

        public void setRowSortFrom(JTable table) {
            rowSort.clear();
            for (RowSorter.SortKey key : table.getRowSorter().getSortKeys()) {
                PreferencesForTableRowSort row = new PreferencesForTableRowSort(key.getColumn(), key.getSortOrder().name());
                rowSort.add(row);
            }
        }

        @Override
        public String getKey() {
            return "$table";
        }

        @Override
        public Map<String,Object> toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("columnOrder", columnOrder);
            map.put("columnWidth", columnWidth);
            map.put("rowSort", rowSort.stream()
                                .map(PreferencesForTableRowSort::toJson)
                                .collect(Collectors.toList()));
            return map;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setJson(Object json) {
            if (json instanceof Map<?,?>) {
                Map<String,?> map = (Map<String,?>) json;
                Object order = map.get("columnOrder");
                if (order != null) {
                    columnOrder.clear();
                    ((List<?>) order)
                            .forEach(i -> columnOrder.add((Integer) i));
                }

                Object width = map.get("columnWidth");
                if (width != null) {
                    columnWidth.clear();
                    ((List<?>) width)
                            .forEach(i -> columnWidth.add((Integer) i));
                }

                Object rowSortObj = map.get("rowSort");
                if (rowSortObj != null) {
                    rowSort.clear();
                    ((List<?>) rowSortObj).stream()
                            .map(e -> new PreferencesForTableRowSort((Map<String, Object>) e))
                            .forEach(rowSort::add);
                }
            }
        }
    }

    public static class PreferencesForTableRowSort {
        protected int column;
        protected String order;

        public PreferencesForTableRowSort(int column, String order) {
            this.column = column;
            this.order = order;
        }

        public PreferencesForTableRowSort(Map<String, Object> map) {
            column = (Integer) map.getOrDefault("column", 0);
            order = (String) map.getOrDefault("order", "");
        }

        public void setColumn(int column) {
            this.column = column;
        }

        public int getColumn() {
            return column;
        }

        public void setOrder(String order) {
            this.order = order;
        }

        public String getOrder() {
            return order;
        }

        public Map<String,Object> toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("column", column);
            map.put("order", order);
            return map;
        }

    }

    public static class SelectAllAction extends AbstractAction {
        protected JTable table;

        public SelectAllAction(JTable table) {
            putValue(NAME, "Select All");
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            table.selectAll();
        }
    }

    /**
     * menu creation for selected rows and columns.
     * <p>
     *     For columns, it uses {@link CollectionColumnMenuSupplier}.
     * */
    public static class PopupExtensionCollection extends PopupExtension implements PopupMenuListener {
        protected CollectionTable table;
        protected int targetColumnIndex = -1;
        protected int lastClickColumnIndex = 0;

        protected boolean showing;
        protected Timer showingTimer;

        public PopupExtensionCollection(CollectionTable pane, Predicate<KeyEvent> keyMatcher, List<JComponent> items) {
            super(pane, keyMatcher, null);
            this.table = pane;

            Supplier<List<JComponent>> otherActions =  () -> {
                List<Action> tableActions = table.getActions();
                List<JComponent> comps = new ArrayList<>(items.size() + tableActions.size());
                comps.addAll(items);
                tableActions.stream()
                        .map(JMenuItem::new)
                        .forEach(comps::add);
                return comps;
            };
            PopupMenuBuilder columnBuilder = new CollectionColumnMenuSupplier();
            PopupMenuBuilder cellBuilder = new CollectionCellMenuSupplier();

            setMenuBuilder((s, m) -> {
                otherActions.get().forEach(m::accept);
                columnBuilder.build(s, m);
                cellBuilder.build(s, m);
            });

            menu.get().addPopupMenuListener(this);
            showingTimer = new Timer(100, e -> {
                showing = false;
            });
            showingTimer.setRepeats(false);
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            showing = true;
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            showingTimer.start(); //force to cause the hiding process after mousePressed
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            showingTimer.start();
        }

        public CollectionTable getTable() {
            return table;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!e.isPopupTrigger() && showing) { //closing the popup menu, and then nothing
                e.consume();
                return;
            }
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

        /** @return model index*/
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

        public List<ObjectTableColumn> getTargetColumns() {
            ObjectTableModel model = table.getObjectTableModel();
            return IntStream.of(table.getSelectedColumns())
                    .map(table::convertColumnIndexToModel)
                    .mapToObj(model.getColumns()::get)
                    .collect(Collectors.toList());
        }
    }

    /**
     * it reacts to only {@link PopupExtensionCollection}
     *    in order to obtains selected column by {@link PopupExtensionCollection#getTargetColumn()}.
     *    The target column is the column under the mouse pointer, or one of selected column when a key stroke happened.
     *    A returned {@link ObjectTableColumn} can obtain a menu source by {@link ObjectTableColumn#getMenuBuilderSource()}.
     * */
    public static class CollectionColumnMenuSupplier implements PopupExtension.PopupMenuBuilder {
        @Override
        public void build(PopupExtensionSender sender, Consumer<Object> menu) {
            if (sender instanceof PopupExtensionCollection) {
                ObjectTableColumn column = ((PopupExtensionCollection) sender).getTargetColumn();
                ObjectTableColumn.PopupMenuBuilderSource src = (column == null ? null : column.getMenuBuilderSource());
                if (src != null) {
                    PopupExtension.PopupMenuBuilder builder = src.getMenuBuilder();
                    if (builder != null) {
                        builder.build(sender, new MenuTitleAppender("Column: " + column.getTableColumn().getHeaderValue(), menu));
                    }
                }
            }
        }

    }

    public static class MenuTitleAppender implements Consumer<Object> {
        protected Consumer<Object> appender;
        protected boolean added = false;
        protected String title;

        public MenuTitleAppender(String title, Consumer<Object> appender) {
            this.title = title;
            this.appender = appender;
        }

        @Override
        public void accept(Object o) {
            if (title != null && !added) {
                appender.accept(new JPopupMenu.Separator());
                appender.accept(MenuBuilder.get().createLabel(title));
                added = true;
            }
            appender.accept(o);
        }
    }

    public static class CollectionCellMenuSupplier implements PopupExtension.PopupMenuBuilder {
        @Override
        public void build(PopupExtensionSender sender, Consumer<Object> menu) {
            if (sender instanceof PopupExtensionCollection) {
                PopupExtensionCollection collSender = (PopupExtensionCollection) sender;
                CollectionTable table = collSender.getTable();

                List<ObjectTableColumn> allCols = table.getObjectTableModel().getColumns();
                table.getObjectTableModel().getBuilderForRowsOrCells(table, allCols, true)
                        .build(sender, new MenuTitleAppender("Rows", menu));

                List<ObjectTableColumn> targetCols = collSender.getTargetColumns();
                table.getObjectTableModel().getBuilderForRowsOrCells(table, targetCols, false)
                        .build(sender, new MenuTitleAppender("Cells", menu));
            }
        }
    }

    public static class ToStringCollectionTransferHandler extends TransferHandler {
        protected CollectionTable pane;

        public ToStringCollectionTransferHandler(CollectionTable pane) {
            this.pane = pane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return false;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {

            if (pane.isSelectionEmpty()) {
                String data = pane.getContext().getRepresentation()
                        .toHumanReadableString(pane.getContext(), pane.getSwingViewValue());
                return new StringSelection(data);
            } else {
                //manual composition of to-string action for columns of selected cells.
                List<ToStringCopyCell.TableMenuCompositeToStringValue> composites = pane.getPopup().getTargetColumns().stream()
                            .flatMap(col -> col.getCompositesForCells().stream()
                                    .filter(ToStringCopyCell.TableMenuCompositeToStringValue.class::isInstance)
                                    .map(ToStringCopyCell.TableMenuCompositeToStringValue.class::cast))
                            .collect(Collectors.toList());
                String data = new ToStringCopyCell.ToStringCopyForCellsAction(composites, true)
                    .getString(new TableTargetCellForJTable(pane));
                return new StringSelection(data);

            }
        }
    }

    public static class UnSelectAction extends AbstractAction {
        protected JTable table;

        public UnSelectAction(JTable table) {
            super("clear-selection");
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            table.clearSelection();
        }
    }
}
