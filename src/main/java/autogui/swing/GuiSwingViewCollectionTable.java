package autogui.swing;

import autogui.base.log.GuiLogManager;
import autogui.base.mapping.*;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.table.*;
import autogui.swing.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * a swing view for {@link GuiReprCollectionTable}
 *
 * <h3>swing-value</h3>
 * {@link CollectionTable#getSwingViewValue()}:
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
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        CollectionTable table = new CollectionTable(context, new SpecifierManagerDefault(parentSpecifier));
        GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier = table.getRowSpecifier();
        List<Action> actions = new ArrayList<>();

        for (GuiMappingContext elementContext : context.getChildren()) {
            GuiSwingElement subView = columnMapperSet.viewTableColumn(elementContext);
            if (subView instanceof GuiSwingTableColumnSet) {
                GuiSwingTableColumnSet columnSet = (GuiSwingTableColumnSet) subView;

                columnSet.createColumns(elementContext, table.getObjectTableModel().getColumns(), rowSpecifier, rowSpecifier);

                actions.addAll(columnSet.createColumnActions(elementContext, table));
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

                    GuiSwingTableColumnSetDefault.TableSelectionListAction createdAction = null;
                    if (listAction.isSelectionRowIndexesAction(siblingContext)) {
                        createdAction = new GuiSwingTableColumnSetDefault.TableSelectionListAction(siblingContext,
                                table.getSelectionSourceForRowIndexes());
                    } else if (listAction.isSelectionRowAndColumnIndexesAction(siblingContext)) {
                        createdAction = new GuiSwingTableColumnSetDefault.TableSelectionListAction(siblingContext,
                                table.getSelectionSourceForRowAndColumnIndexes());
                    } else if (listAction.isSelectionAction(siblingContext, context)) {
                        createdAction = new GuiSwingTableColumnSetDefault.TableSelectionListAction(siblingContext, table);
                    }

                    if (createdAction != null) {
                        actions.add(createdAction);
                        createdAction.setSelectionChangeFactoryFromContext(context);
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
        protected SpecifierManager specifierManager;
        protected List<?> source;
        protected PopupExtensionCollection popup;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected List<Action> actions = new ArrayList<>();
        protected List<GuiSwingTableColumnSetDefault.TableSelectionListAction> autoSelectionActions = new ArrayList<>();
        protected int autoSelectionDepth;
        protected EditingRunner selectionRunner;

        protected TablePreferencesUpdater preferencesUpdater;
        protected List<Integer> lastSelectionActionIndexes = Collections.emptyList();
        protected TableSelectionSourceForIndexes selectionSourceForRowIndexes;
        protected TableSelectionSourceForIndexes selectionSourceForRowAndColumnIndexes;

        protected MenuBuilder.MenuLabel infoLabel;

        protected JToolBar actionToolBar;

        protected GuiTaskClock viewClock = new GuiTaskClock(true);

        public CollectionTable(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
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
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
            GuiSwingView.setDescriptionToolTipText(context, this);
        }

        public void initModel() {
            ObjectTableModel model = new ObjectTableModelCollection(context, this::getSpecifier, this::getSource);
            model.setTable(this);
            setModel(model);
            setColumnModel(model.getColumnModel());
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initPopup() {
            MouseListener[] listeners = getMouseListeners();
            Arrays.stream(listeners).forEach(this::removeMouseListener);
            popup = new PopupExtensionCollection(this, PopupExtension.getDefaultKeyMatcher(),
                    this::getSwingStaticMenuItems);
            Arrays.stream(listeners).forEach(this::addMouseListener);
            //improve precedence of the popup listener

        }

        public void initGrid() {
            setGridColor(getBackground());
            setShowGrid(false);
        }

        public void initSelection() {
            selectionRunner = new EditingRunner(200, this::runAutoSelectionActions);
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
            update(context, context.getSource().getValue(), context.getContextClock().copy());
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
                menuItems = PopupCategorized.getMenuItems(
                        Arrays.asList(
                                infoLabel,
                                new ContextRefreshAction(context),
                                new SelectAllAction(this),
                                new UnSelectAction(this)),
                        getActions());
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

            GuiSwingView.setupKeyBindingsForStaticMenuItems(this); //here, after actions are fixed

            getColumnModel().addColumnModelListener(preferencesUpdater);
            getRowSorter().addRowSorterListener(preferencesUpdater);

            new ColumnContextMenu(e -> System.out.println(e)).installTo(this); //TODO

            getPopup().setupCompositeKeyMap();

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
            int width = getObjectTableModel().getColumns().getTotalWidth();
            if (isAutoResizeOff()) {
                setAutoResizeMode(AUTO_RESIZE_OFF);
            }
            UIManagerUtil ui = UIManagerUtil.getInstance();
            scrollPane.setPreferredSize(new Dimension(width, Math.max(scrollPane.getPreferredSize().height, ui.getScaledSizeInt(100))));
            scrollPane.getVerticalScrollBar().setUnitIncrement(ui.getScaledSizeInt(16));
            scrollPane.getHorizontalScrollBar().setUnitIncrement(ui.getScaledSizeInt(16));
        }

        protected boolean isAutoResizeOff() {
            return getObjectTableModel().getColumns().hasDynamicColumns();
        }

        public JToolBar initActionToolBar(List<Action> actions) {
            actionToolBar = new JToolBar();
            actionToolBar.setFloatable(false);
            actionToolBar.setOpaque(false);

            getSelectionModel().addListSelectionListener(this::runListSelection);
            actions.forEach(a -> initAction(actionToolBar, a));

            if (popup != null) {
                popup.addListenersTo(actionToolBar);
            }
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

        public ObjectTableModelCollection getObjectTableModel() {
            return (ObjectTableModelCollection) getModel();
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
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingUtilities.invokeLater(() -> setSwingViewValue((List<?>) newValue, contextClock));
        }

        @Override
        public List<?> getSwingViewValue() {
            return getSource();
        }

        @Override
        public void setSwingViewValue(List<?> value) {
            viewClock.increment();
            setSwingViewValueWithoutIncrementClock(value);
        }

        private void setSwingViewValueWithoutIncrementClock(List<?> value) {
            GuiReprCollectionTable repr = (GuiReprCollectionTable) context.getRepresentation();
            source = repr.toUpdateValue(context, value);
            getObjectTableModel().refreshColumns();
            getObjectTableModel().refreshData();
            resizeAndRepaint();
        }

        @Override
        public void setSwingViewValueWithUpdate(List<?> value) {
            setSwingViewValue(value);
            GuiSwingView.updateFromGui(this, value, viewClock);
        }

        @Override
        public void setSwingViewValue(List<?> value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setSwingViewValueWithoutIncrementClock(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(List<?> value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                setSwingViewValueWithoutIncrementClock(value);
                GuiSwingView.updateFromGui(this, value, viewClock);
            }
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            this.preferencesUpdater.setUpdater(updater);
        }

        @Override
        public void shutdownSwingView() {
            getObjectTableModel().getColumns().getColumns()
                    .forEach(ObjectTableColumn::shutdown);
            selectionRunner.shutdown();
        }

        /////////////////


        public JToolBar getActionToolBar() {
            return actionToolBar;
        }

        @Override
        public String getTargetName() {
            if (context.isTypeElementCollection() && context.hasParent()) {
                return context.getParent().getName();
            } else {
                return context.getName();
            }
        }

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
        public void selectionActionFinished(boolean autoSelection, GuiSwingTableColumnSet.TableSelectionChange change) {
            if (autoSelection) {
                autoSelectionDepth++;
            }

            int rows = getRowCount();
            int[] selectedModelRowsIndexes = lastSelectionActionIndexes.stream()
                    .filter(i -> i >= 0 && i < rows)
                    .mapToInt(Integer::intValue).toArray();
            getObjectTableModel().refreshRows(selectedModelRowsIndexes);

            //after execution an action, source will be updated by invokeLater
            // change contains info. of the updated source, so the following code intends to be executed after the updating
            SwingUtilities.invokeLater(() -> {
                try {
                    //re-selection
                    changeSelection(selectedModelRowsIndexes, change);
                } finally {
                    if (autoSelection) {
                        autoSelectionDepth--;
                    }
                }
            });
        }

        public void changeSelection(int[] selectedModelRowsIndexes, GuiSwingTableColumnSet.TableSelectionChange change) {
            if (change instanceof GuiSwingTableColumnSet.TableSelectionChangeNothing) {
                changeSelectionNothing(selectedModelRowsIndexes, change);

            } else if (change instanceof GuiSwingTableColumnSet.TableSelectionChangeIndexes) {
                changeSelectionIndexes(selectedModelRowsIndexes, (GuiSwingTableColumnSet.TableSelectionChangeIndexes) change);

            } else if (change instanceof GuiSwingTableColumnSet.TableSelectionChangeValues) {
                changeSelectionValues(selectedModelRowsIndexes, (GuiSwingTableColumnSet.TableSelectionChangeValues) change);

            } else if (change instanceof GuiSwingTableColumnSet.TableSelectionChangeIndexesRowAndColumn) {
                changeSelectionIndexesRowAndColumns(selectedModelRowsIndexes, (GuiSwingTableColumnSet.TableSelectionChangeIndexesRowAndColumn) change);

            } else {
                System.err.println("Unknown selection change type: " + change);
            }
        }

        public void changeSelectionNothing(int[] selectedModelRowsIndexes, GuiSwingTableColumnSet.TableSelectionChange change) {
            ListSelectionModel sel = getSelectionModel();
            sel.setValueIsAdjusting(true);
            IntStream.of(selectedModelRowsIndexes)
                    .map(this::convertRowIndexToView)
                    .forEach(i -> sel.addSelectionInterval(i, i));
            sel.setValueIsAdjusting(false);
        }

        public void changeSelectionIndexes(int[] selectedModelRowsIndexes, GuiSwingTableColumnSet.TableSelectionChangeIndexes change) {
            ListSelectionModel sel = getSelectionModel();
            int rows = getRowCount();

            Collection<Integer> is = change.indexes;
            sel.setValueIsAdjusting(true);
            sel.clearSelection();
            is.stream()
                    .filter(i -> i >= 0 && i < rows)
                    .mapToInt(this::convertRowIndexToView)
                    .forEach(i -> sel.addSelectionInterval(i, i));
            sel.setValueIsAdjusting(false);

            Set<Integer> update = new HashSet<>(is);
            IntStream.of(selectedModelRowsIndexes).forEach(update::remove);

            getObjectTableModel().refreshRows(update.stream()
                    .mapToInt(Integer::intValue)
                    .filter(i -> i >= 0 && i < rows)
                    .sorted()
                    .toArray());
        }

        public void changeSelectionValues(int[] selectedModelRowsIndexes, GuiSwingTableColumnSet.TableSelectionChangeValues change) {
            //TODO updating source by action?
            changeSelectionIndexes(selectedModelRowsIndexes, new GuiSwingTableColumnSet.TableSelectionChangeIndexes(change.values.stream()
                    .map(source::indexOf)
                    .collect(Collectors.toList())));
        }

        public void changeSelectionIndexesRowAndColumns(int[] selectedModelRowsIndexes, GuiSwingTableColumnSet.TableSelectionChangeIndexesRowAndColumn change) {
            ListSelectionModel sel = getSelectionModel();
            Collection<int[]> is = change.indexes;
            int rows = getRowCount();
            int cols = getColumnCount();
            sel.setValueIsAdjusting(true);
            sel.clearSelection();
            is.stream()
                    .filter(p -> p[0] >= 0 && p[0] < rows && p[1] >= 0 && p[1] < cols)
                    .map(p -> new int[] {convertRowIndexToView(p[0]), convertColumnIndexToView(p[1])})
                    .forEach(p -> changeSelection(p[0], p[1], true, false));
            sel.setValueIsAdjusting(false);

        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs) {
            try {
                GuiSwingView.loadPreferencesDefault(this, prefs);
                GuiPreferences targetPrefs = prefs.getDescendant(getSwingViewContext());
                preferencesUpdater.apply(targetPrefs);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            try {
                GuiSwingView.savePreferencesDefault(this, prefs);
                GuiPreferences targetPrefs = prefs.getDescendant(getSwingViewContext());
                PreferencesForTable p = preferencesUpdater.getPrefs();
                p.saveTo(targetPrefs);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
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

            UIManagerUtil ui = UIManagerUtil.getInstance();
            int unitSize = Math.max(1, ui.getScaledSizeInt(1));
            int size = ui.getScaledSizeInt(3);
            int arc = ui.getScaledSizeInt(5);

            g2.setColor(getSelectionBackground());
            g2.setStroke(new BasicStroke(unitSize));

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
                        g2.draw(new RoundRectangle2D.Float(row.x + unitSize, row.y + unitSize, row.width - size, row.height - size, arc, arc));
                    }
                }
            }
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }

        public GuiSwingTableColumn.SpecifierManagerIndex getRowSpecifier() {
            return getObjectTableModel().getRowSpecifierManager();
        }

        @Override
        public void setKeyStrokeString(String keyStrokeString) {
            infoLabel.setAdditionalInfo(keyStrokeString);
        }
    }

    public static class ObjectTableModelCollection extends ObjectTableModel {
        protected GuiMappingContext context;
        protected GuiMappingContext elementContext;
        protected Supplier<GuiReprValue.ObjectSpecifier> tableSpecifier;
        protected GuiSwingTableColumn.SpecifierManagerIndex rowSpecifierManager;

        public ObjectTableModelCollection(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> tableSpecifier,
                                          Supplier<Object> source) {
            this.context = context;
            this.tableSpecifier = tableSpecifier;
            this.rowSpecifierManager = new GuiSwingTableColumn.SpecifierManagerIndex(tableSpecifier);
            setElementContextFromContext();
            setSource(source);
        }

        protected void setElementContextFromContext() {
            elementContext = context.getReprCollectionTable().getElementContext(context);
        }

        @Override
        public Object getRowAtIndex(int row) {
            Object collection = getCollectionFromSource();
            try {
                GuiReprValue.ObjectSpecifier specifier = rowSpecifierManager.getSpecifierWithSettingIndex(row);
                return elementContext.getReprValue()
                        .getValueWithoutNoUpdate(elementContext, GuiMappingContext.GuiSourceValue.of(collection), specifier);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public int getRowCountUpdated() {
            Object collection = getCollectionFromSource();
            try {
                return elementContext.getReprValue()
                            .getValueCollectionSize(elementContext, GuiMappingContext.GuiSourceValue.of(collection), tableSpecifier.get());
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Object getCollectionFromSource() {
            Object collection = super.getCollectionFromSource();
            if (collection == null) {
                try {
                    collection = context.getReprValue()
                            .getUpdatedValueWithoutNoUpdate(context, tableSpecifier.get());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            }
            return collection;
        }

        public GuiSwingTableColumn.SpecifierManagerIndex getRowSpecifierManager() {
            return rowSpecifierManager;
        }

        @Override
        public void columnAdded(ObjectTableColumn column) {
            super.columnAdded(column);
            JTable table = getTable();
            if (table instanceof CollectionTable) {
                ((CollectionTable) table).getPopup().setupCompositeKeyMapByAddingColumn(column);
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
        public String getTargetName() {
            return table.getTargetName();
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
        public void selectionActionFinished(boolean autoSelection, GuiSwingTableColumnSet.TableSelectionChange change) {
            table.selectionActionFinished(autoSelection, change);
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
            try {
                prefs.loadFrom(p);
                prefs.applyTo(table);
            } finally {
                savingDisabled = false;
            }
        }

        public void applyJson(Map<String,Object> json) {
            savingDisabled = true;
            try {
                prefs.setJson(json);
                prefs.applyTo(table);
            } finally {
                savingDisabled = false;
            }
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

    public static class SelectAllAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected JTable table;

        public SelectAllAction(JTable table) {
            putValue(NAME, "Select All");

            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_A,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            table.selectAll();
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_SELECT;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_SELECT;
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

        protected Set<ObjectTableColumn.TableMenuCompositeShared> existingShared;

        public PopupExtensionCollection(CollectionTable pane, Predicate<KeyEvent> keyMatcher, Supplier<? extends Collection<PopupCategorized.CategorizedMenuItem>> items) {
            super(pane, keyMatcher, null);
            this.table = pane;

            PopupCategorized tableActions = new PopupCategorized(items, null,
                                        new ObjectTableModel.MenuBuilderWithEmptySeparator());
            PopupMenuBuilder columnBuilder = new CollectionColumnMenuSupplier(this);
            PopupMenuBuilder cellBuilder = new CollectionCellMenuSupplier(this);

            setMenuBuilder((filter, m) -> {
                tableActions.build(filter, m);
                columnBuilder.build(filter, m);
                cellBuilder.build(filter, m);
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
                return table.getObjectTableModel().getColumns().getColumns().get(targetColumnIndex);
            } else {
                return null;
            }
        }

        public List<ObjectTableColumn> getTargetColumns() {
            ObjectTableModel model = table.getObjectTableModel();
            return IntStream.of(table.getSelectedColumns())
                    .map(table::convertColumnIndexToModel)
                    .mapToObj(model.getColumns().getColumns()::get)
                    .collect(Collectors.toList());
        }

        ////////////

        public void setupCompositeKeyMap() {
            existingShared = new LinkedHashSet<>();
            table.getObjectTableModel().getColumns().getColumns().forEach(c ->
                c.getCompositesForCells().forEach(cellComp ->
                    existingShared.add(cellComp.getShared())));

            existingShared.forEach(this::setupCompositeKeyMapForShared);
        }

        public void setupCompositeKeyMapForShared(ObjectTableColumn.TableMenuCompositeShared shared) {
            //call composite to the shared with empty columns
            shared.composite(table, Collections.emptyList(), false).forEach(item -> {
                Action a = PopupCategorized.getMenuItemAction(item);
                if (a != null) {

                    TableCompositesAction keyAction =
                            new TableCompositesAction(table,
                                    (String) a.getValue(Action.NAME),
                                    (KeyStroke) a.getValue(Action.ACCELERATOR_KEY),
                                    shared,
                                    item.getCategory(), item.getSubCategory());
                    if (GuiSwingView.setupKeyBindingsForStaticMenuItemAction(table, keyAction, act -> false)) {
                        System.err.println("bind action:" +
                                keyAction.getValue(Action.NAME) + "  key:" +
                                keyAction.getValue(Action.ACCELERATOR_KEY));
                    }
                }
            });
        }

        public void setupCompositeKeyMapByAddingColumn(ObjectTableColumn newColumn) {
            if (existingShared != null) {
                newColumn.getCompositesForCells().forEach(cellComp -> {
                    if (existingShared.add(cellComp.getShared())) {
                        setupCompositeKeyMapForShared(cellComp.getShared());
                    }
                });
            }
        }
    }

    /**
     * it reacts to only {@link PopupExtensionCollection}
     *    in order to obtains selected column by {@link PopupExtensionCollection#getTargetColumn()}.
     *    The target column is the column under the mouse pointer, or one of selected column when a key stroke happened.
     *    A returned {@link ObjectTableColumn} can obtain a menu source by {@link ObjectTableColumn#getMenuBuilderSource()}.
     * */
    public static class CollectionColumnMenuSupplier implements PopupExtension.PopupMenuBuilder {
        protected PopupExtensionCollection popup;

        public CollectionColumnMenuSupplier(PopupExtensionCollection popup) {
            this.popup = popup;
        }

        @Override
        public void build(PopupExtension.PopupMenuFilter filter, Consumer<Object> menu) {
            ObjectTableColumn column = popup.getTargetColumn();
            ObjectTableColumn.PopupMenuBuilderSource src = (column == null ? null : column.getMenuBuilderSource());
            if (src != null) {
                PopupExtension.PopupMenuBuilder builder = src.getMenuBuilder(popup.getTable());
                if (builder != null) {
                    builder.build(new MenuSeparator("Column: " + column.getTableColumn().getHeaderValue(), filter), menu);
                }
            }
        }
    }


    public static class MenuSeparator implements PopupExtension.PopupMenuFilter {
        protected PopupExtension.PopupMenuFilter filter;
        protected boolean beforeReturned = false;
        protected String title;

        public MenuSeparator(PopupExtension.PopupMenuFilter filter) {
            this.filter = filter;
        }

        public MenuSeparator(String title, PopupExtension.PopupMenuFilter filter) {
            this.title = title;
            this.filter = filter;
        }

        @Override
        public Object convert(Object item) {
            return filter.convert(item);
        }

        @Override
        public List<Object> aroundItems(boolean before) {
            if (before && !beforeReturned) {
                List<Object> bs = new ArrayList<>(filter.aroundItems(true));
                Object s = filter.convert(new JPopupMenu.Separator());
                if (s != null) {
                    bs.add(s);
                }
                Object l = (title == null ? null : filter.convert(MenuBuilder.get().createLabel(title)));
                if (l != null) {
                    bs.add(l);
                }
                beforeReturned = false;
                return bs;
            } else {
                return filter.aroundItems(before);
            }
        }
    }

    public static class CollectionCellMenuSupplier implements PopupExtension.PopupMenuBuilder {
        protected PopupExtensionCollection popup;

        public CollectionCellMenuSupplier(PopupExtensionCollection popup) {
            this.popup = popup;
        }

        @Override
        public void build(PopupExtension.PopupMenuFilter filter, Consumer<Object> menu) {
            CollectionTable table = popup.getTable();

            filter = new ObjectTableModel.CollectionRowsAndCellsActionBuilder(table, filter);

            List<ObjectTableColumn> targetCols = popup.getTargetColumns();
            table.getObjectTableModel().getBuilderForRowsOrCells(table, targetCols, false)
                    .build(new MenuSeparator(filter), menu);

            List<ObjectTableColumn> allCols = table.getObjectTableModel().getColumns().getColumns();
            table.getObjectTableModel().getBuilderForRowsOrCells(table, allCols, true)
                    .build(new MenuSeparator(filter), menu);

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
                String data = pane.getSwingViewContext().getRepresentation()
                        .toHumanReadableString(pane.getSwingViewContext(), pane.getSwingViewValue());
                return new StringSelection(data);
            } else {
                //manual composition of to-string action for columns of selected cells.
                List<ToStringCopyCell.TableMenuCompositeToStringCopy> composites = pane.getPopup().getTargetColumns().stream()
                            .flatMap(col -> col.getCompositesForCells().stream()
                                    .filter(ToStringCopyCell.TableMenuCompositeToStringCopy.class::isInstance)
                                    .map(ToStringCopyCell.TableMenuCompositeToStringCopy.class::cast))
                            .collect(Collectors.toList());
                String data = new ToStringCopyCell.ToStringCopyForCellsAction(composites, true)
                    .getString(new TableTargetCellForJTable(pane));
                return new StringSelection(data);

            }
        }
    }

    public static class UnSelectAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected JTable table;

        public UnSelectAction(JTable table) {
            super("Clear Selection");

            putValue(Action.ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_A,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() |
                                    InputEvent.SHIFT_DOWN_MASK));
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            table.clearSelection();
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_SELECT;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_SELECT;
        }
    }

    public static class TableCompositesAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected CollectionTable table;
        protected ObjectTableColumn.TableMenuCompositeShared shared;
        protected String category;
        protected String subCategory;

        public TableCompositesAction(CollectionTable table, String name, KeyStroke key,
                                     ObjectTableColumn.TableMenuCompositeShared shared,
                                     String category, String subCategory) {
            super(name);
            putValue(ACCELERATOR_KEY, key);
            this.shared = shared;
            this.table = table;
            this.category = category;
            this.subCategory = subCategory;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String name = (String) getValue(Action.NAME);
            table.getObjectTableModel().getBuildersForRowsOrCells(table, table.getPopup().getTargetColumns(), false).stream()
                    .map(PopupCategorized::getMenuItemAction)
                    .filter(Objects::nonNull)
                    .filter(TableTargetCellAction.class::isInstance)
                    .filter(a -> Objects.equals(name, a.getValue(Action.NAME)))
                    .map(TableTargetCellAction.class::cast)
                    .findFirst()
                    .ifPresent(a -> a.actionPerformedOnTableCell(e, new TableTargetCellForJTable(table)));
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getSubCategory() {
            return subCategory;
        }
    }

    public static class ColumnContextMenu implements MouseListener {
        protected Consumer<TableColumn> runner;
        protected JTable table;

        public ColumnContextMenu(Consumer<TableColumn> runner) {
            this.runner = runner;
        }

        public void installTo(JTable table) {
            this.table = table;
            table.getTableHeader().addMouseListener(this);

        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                TableColumn c = getClickedColumn(e);

            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                TableColumn c = getClickedColumn(e);

            }
        }

        public TableColumn getClickedColumn(MouseEvent e) {
            TableColumnModel model = table.getColumnModel();
            JTableHeader header = table.getTableHeader();
            int i = Math.min(model.getColumnCount() - 1,
                    (int) (e.getX() / (header.getWidth() / (float) model.getColumnCount())));
            int inc = 0;
            while (0 <= i && i < model.getColumnCount()) {
                Rectangle r = header.getHeaderRect(i);
                if (r.contains(e.getPoint())) {
                    return model.getColumn(i);
                } else if (inc == 0) {
                    inc = r.getMaxX() < e.getX() ? 1 : -1;
                }
                i += inc;
            }
            return null;
        }


        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mouseExited(MouseEvent e) { }
    }
}
