package org.autogui.swing;

import org.autogui.GuiInits;
import org.autogui.base.annotation.GuiDefaultInits;
import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.*;
import org.autogui.base.type.GuiTypeMemberProperty;
import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.prefs.GuiSwingPrefsApplyOptions;
import org.autogui.swing.prefs.GuiSwingPrefsSupports;
import org.autogui.swing.table.*;
import org.autogui.swing.table.ToStringCopyCell.ToStringCopyForCellsAction;
import org.autogui.swing.util.*;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.io.Serial;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * a swing view for {@link GuiReprCollectionTable}
 *
 * <h2>swing-value</h2>
 * {@link CollectionTable#getSwingViewValue()}:
 * latest set {@link List}.
 *
 * <h2>history-value</h2>
 *  Currently unsupported.
 *
 * <h2>string-transfer</h2>
 * entire copying is supported by owned {@link GuiSwingViewPropertyPane.PropertyPane}.
 *  the entire copying is handled by {@link GuiReprPropertyPane#toHumanReadableString(GuiMappingContext, Object)} -&gt;
 *  {@link GuiReprObjectPane#toHumanReadableStringFromObject(GuiMappingContext, Object)}  -&gt;
 *    {@link GuiReprCollectionElement#toHumanReadableString(GuiMappingContext, Object)}.
 *  <p>
 *      selected rows or cells can be achieved by {@link ToStringCopyCell}
 *      based on {@link TableTargetCellAction}.
 */
@SuppressWarnings("this-escape")
public class GuiSwingViewCollectionTable implements GuiSwingView {
    protected GuiSwingMapperSet columnMapperSet;


    public GuiSwingViewCollectionTable(GuiSwingMapperSet columnMapperSet) {
        this.columnMapperSet = columnMapperSet;
    }

    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        SpecifierManager tableSpecifier = GuiSwingView.specifierManager(parentSpecifier);
        CollectionTable table = new CollectionTable(context, tableSpecifier);
        GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier = table.getRowSpecifier();
        List<Action> actions = new ArrayList<>();

        for (GuiMappingContext elementContext : context.getChildren()) {
            GuiSwingElement subView = columnMapperSet.viewTableColumn(elementContext);
            if (subView instanceof GuiSwingTableColumnSet columnSet) {
                columnSet.createColumns(elementContext, table.getObjectTableModel().getColumns(), rowSpecifier, tableSpecifier, rowSpecifier);

                actions.addAll(columnSet.createColumnActions(elementContext, table));
            }
        }

        if (context.hasParent()) {
            GuiMappingContext parent = context.getParent();
            if (parent.isTypeElementProperty() && parent.hasParent()) {
                parent = parent.getParent();
            }
            for (GuiMappingContext siblingContext : parent.getChildren()) {
                if (siblingContext.getRepresentation() instanceof GuiReprActionList listAction) {
                    ObjectTableModelColumns.DynamicColumnContainer dc = table.getObjectTableModel().getColumns().getRootContainer(); //currently always returns non-null container
                    if (dc != null) {
                        dc.addRootListActionContext(siblingContext);

                    } else {
                        GuiSwingTableColumnSetDefault.TableSelectionListAction createdAction = null;
                        if (listAction.isSelectionRowIndicesAction(siblingContext)) {
                            createdAction = new GuiSwingTableColumnSetDefault.TableSelectionListAction(siblingContext,
                                    table.getSelectionSourceForRowIndices());
                        } else if (listAction.isSelectionRowAndColumnIndicesAction(siblingContext)) {
                            createdAction = new GuiSwingTableColumnSetDefault.TableSelectionListAction(siblingContext,
                                    table.getSelectionSourceForRowAndColumnIndices());
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
        }

        actions.addAll(table.getObjectTableModel().getColumns()
                .getDynamicColumnsActions(new TableTargetCellForJTable(table), table));

        return table.setupAfterAddingColumns(actions);
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }


    public static class CollectionTable extends JTable
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<List<?>>,
                        GuiSwingTableColumnSet.TableSelectionSource, GuiSwingPrefsSupports.PreferencesUpdateSupport,
                        SettingsWindowClient {
        private static final long sionUID = 1L;
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected List<?> source;
        protected PopupExtensionCollection popup;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected List<Action> actions = new ArrayList<>();
        protected List<GuiSwingTableColumnSetDefault.TableSelectionListAction> autoSelectionActions = new ArrayList<>();
        protected int autoSelectionDepth;
        protected volatile EditingRunner selectionRunner;

        protected TablePreferencesUpdater preferencesUpdater;
        protected List<Integer> lastSelectionActionIndices = Collections.emptyList();
        protected TableSelectionSourceForIndices selectionSourceForRowIndices;
        protected TableSelectionSourceForIndices selectionSourceForRowAndColumnIndices;

        protected PopupExtensionCollectionColumnHeader popupColumnHeader;
        protected List<PopupCategorized.CategorizedMenuItem> columnHeaderMenuItems;

        protected MenuBuilder.MenuLabel infoLabel;

        protected JToolBar actionToolBar;

        protected GuiTaskClock viewClock = new GuiTaskClock(true);

        protected SettingsWindow settingsWindow;

        protected boolean customHighlighting;
        /** @since 1.3 */
        protected boolean customRowHeightByUser;
        /** @since 1.3 */
        protected int rowHeightByProgram;
        /** some row-height is customized by {@link #setRowHeight(int, int)}. cleared by {@link #setRowHeight(int)}
         * @since 1.6 */
        protected boolean customRowHeightIndividual;
        /** @since 1.6 */
        protected RowHeightSetAction rowHeightSetAction;


        public CollectionTable(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            init();
        }

        public void init() {
            initColor();
            initName();
            initModel();
            initContextUpdate();
            initPopup();
            initGrid();
            initSelection();
            initSelectionSource();
            initSelectionClear();
            initValue();
            initRowHeight();
            initPreferencesUpdater();
            initDragDrop();
            initFocus();
            initAfter();
        }

        public void initColor() {
            UIManagerUtil u = UIManagerUtil.getInstance();
            //the returned color is not a UIResource; the color will be passed to cell components by ObjectTableColumnValue.setTableColor
            // and Nimbus LAF painter uses the background-color (if the color is a UIResource, it will be replaced by the style color)
            setBackground(u.getTableBackground());
            setForeground(u.getLabelForeground());
        }

        public void initName() {
            setName(context.getName());
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
            GuiSwingView.setDescriptionToolTipText(context, this);
        }

        public void initModel() {
            ObjectTableModel model = new GuiSwingTableModelCollection(context, this::getSpecifier, this::getSource);
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
            var ui = UIManagerUtil.getInstance();
            customHighlighting = ui.isTableCustomHighlighting();
            setGridColor(getAutoGridColor());
            setShowVerticalLines(false);
            setShowHorizontalLines(true);
            setRowMargin(Math.max(1, ui.getScaledSizeInt(1)));
        }

        private Color getAutoGridColor() {
            UIManagerUtil ui = UIManagerUtil.getInstance();
            Color grid = ui.getTableAlternateRowColor();
            Color background = ui.getTableBackground();
            if (grid == null || Objects.equals(grid, background)) {
                grid = TextCellRenderer.getGridColor(background);
            }
            return grid;
        }

        public void initSelection() {
            selectionRunner = new EditingRunner(200, this::runAutoSelectionActions);
            setCellSelectionEnabled(true);
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }

        public void initSelectionSource() {
            selectionSourceForRowIndices = new TableSelectionSourceForIndices(this, false);
            selectionSourceForRowAndColumnIndices = new TableSelectionSourceForIndices(this, true);
        }

        public void initSelectionClear() {
            UnSelectAction unSelectAction = new UnSelectAction(this);
            getInputMap().put(PopupExtension.getKeyStroke(KeyEvent.VK_ESCAPE, 0), unSelectAction.getValue(Action.NAME));
            getActionMap().put(unSelectAction.getValue(Action.NAME), unSelectAction);
        }

        public void initRowHeight() {
            setRowHeight(getRowHeight() + UIManagerUtil.getInstance().getScaledSizeInt(16)); //adding margin top+bottom: later ObjectTableModel.initTableRowHeight updates the value based on columns
            rowHeightSetAction = new RowHeightSetAction(this);
            initRowHeightDefault();
        }

        /**
         * set initial row-height settings from {@link GuiInits}; reusing setting methods of {@link PreferencesForTable}
         * @since 1.8
         */
        protected void initRowHeightDefault() {
            PreferencesForTable.createRowHeightFromInits(getInits())
                    .applyRowHeightTo(this);
        }

        /**
         * @return {@link GuiInits} attached to the (parent) table property context, or default
         * @since 1.8
         */
        protected GuiInits getInits() {
            GuiTypeMemberProperty prop = null;
            if (context.isTypeElementProperty()) {
                prop = context.getTypeElementAsProperty();
            } else if (context.isParentPropertyPane()) {
                prop = context.getParent().getTypeElementAsProperty();
            }
            return prop == null ? GuiDefaultInits.get() : prop.getInits();
        }

        public void initValue() {
            update(context, context.getSource().getValue(), context.getContextClock().copy());
        }

        public void initPreferencesUpdater() {
            //preferencesUpdater = new TablePreferencesUpdater(this, context);
            preferencesUpdater = new TablePreferencesUpdaterWithDynamic(this, context);
        }

        public void initDragDrop() {
            //TODO drag drop
            setTransferHandler(new ToStringCollectionTransferHandler(this));
        }

        public void initFocus() {
            GuiSwingView.setupCopyAndPasteActions(this);
            setFocusable(true);
        }

        public void initAfter() {
            selectionRunner.setEnabled(true);
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(
                        Arrays.asList(
                                infoLabel,
                                new ContextRefreshAction(context, this),
                                new ToStringCopyAction(this, context),
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

            setupAfterAddingColumns();

            if (this.actions.isEmpty()) {
                return initTableScrollPane();
            } else {
                JPanel pane = new GuiSwingViewWrapper.ValueWrappingPane<>(initTableScrollPane());
                pane.add(initActionToolBar(this.actions), BorderLayout.PAGE_START);
                return pane;
            }
        }

        /**
         * setup related components for the table and actions
         * @since 1.1
         */
        public void setupAfterAddingColumns() {
            this.actions.stream()
                    .filter(GuiSwingTableColumnSetDefault.TableSelectionListAction.class::isInstance)
                    .map(GuiSwingTableColumnSetDefault.TableSelectionListAction.class::cast)
                    .filter(GuiSwingTableColumnSetDefault.TableSelectionListAction::isAutomaticSelectionAction)
                    .forEach(this.autoSelectionActions::add);

            ObjectTableModel model = getObjectTableModel();
            model.initTableWithoutScrollPane(this);

            GuiSwingView.setupKeyBindingsForStaticMenuItems(this); //here, after actions are fixed

            preferencesUpdater.setUpListeners();

            popupColumnHeader = new PopupExtensionCollectionColumnHeader(this,
                    new PopupCategorized(this::getColumnHeaderMenuItems));

            getPopup().setupCompositeKeyMap();
        }

        public GuiSwingViewWrapper.ValueScrollPane<?> initTableScrollPane() {
            GuiSwingViewWrapper.ValueScrollPane<?> scrollPane = new GuiSwingViewWrapper.ValueScrollPane<>(this, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
            return !getInits().table().dynamicColumnAutoResize() &&
                    !getObjectTableModel().getColumns().isStaticColumns();
        }

        public JToolBar initActionToolBar(List<Action> actions) {
            actionToolBar = new JToolBar();
            actionToolBar.setFloatable(false);
            actionToolBar.setOpaque(false);

            getSelectionModel().addListSelectionListener(this::runListSelection);
            addMouseListener(new MouseAdapter() { //mouse click as selection updating: if no selection change, this also causes handlers
                @Override
                public void mouseReleased(MouseEvent e) {
                    selectionRunner.schedule(e);
                }
            });
            actions.forEach(a -> initAction(actionToolBar, a));

            if (popup != null) {
                popup.addListenersTo(actionToolBar);
            }
            new ToolBarHiddenMenu().addTo(actionToolBar);
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
            ActionEvent e = es.stream()
                    .filter(ActionEvent.class::isInstance)
                    .map(ActionEvent.class::cast)
                    .findFirst()
                    .orElse(null);
            this.autoSelectionActions.forEach(a ->
                    a.actionPerformedBySelection(e));
        }

        public void initAction(JToolBar actionToolBar, Action action) {
            actionToolBar.add(new GuiSwingIcons.ActionButton(action));
            action.setEnabled(false);

            String name = (String) action.getValue(Action.NAME);
            if (name != null) {
                getActionMap().put(name, action);
            }
        }

        @Override
        public void setSettingsWindow(SettingsWindow settingsWindow) {
            this.settingsWindow = settingsWindow;
            getObjectTableModel().getColumnsWithContext().setSettingsWindow(settingsWindow);
        }

        @Override
        public SettingsWindow getSettingsWindow() {
            return settingsWindow;
        }

        public List<Action> getActions() {
            return actions;
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        public GuiSwingTableModelCollection getObjectTableModel() {
            return (GuiSwingTableModelCollection) getModel();
        }

        public List<?> getSource() {
            return source; //TODO support array
        }

        public TableSelectionSourceForIndices getSelectionSourceForRowAndColumnIndices() {
            return selectionSourceForRowAndColumnIndices;
        }

        public TableSelectionSourceForIndices getSelectionSourceForRowIndices() {
            return selectionSourceForRowIndices;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingDeferredRunner.invokeLater(() -> setSwingViewValue((List<?>) newValue, contextClock));
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
            GuiSwingView.updateViewClockSync(viewClock, context);
            setSwingViewValue(value);
            updateFromGui(value, viewClock);
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
                updateFromGui(value, viewClock);
            }
        }

        public void updateFromGui(Object v, GuiTaskClock viewClock) {
            GuiSwingView.updateFromGui(this, v, viewClock);
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> updater) {
            this.preferencesUpdater.setUpdater(updater);
            getObjectTableModel().getColumnsWithContext().setPreferencesUpdater(updater);
        }

        @Override
        public void shutdownSwingView() {
            getObjectTableModel().getColumns().shutdown();
            selectionRunner.shutdown();
        }

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
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
            List<Integer> selectedIndices = new ArrayList<>();
            if (source != null) {
                for (int i = sel.getMinSelectionIndex(), max = sel.getMaxSelectionIndex(); i <= max; ++i) {
                    if (i >= 0 && sel.isSelectedIndex(i)) {
                        int modelIndex = convertRowIndexToModel(i);
                        selectedIndices.add(modelIndex);
                        selected.add(source.get(modelIndex));
                    }
                }
            }
            lastSelectionActionIndices = selectedIndices;
            return selected;
        }

        @Override
        public void selectionActionFinished(boolean autoSelection, GuiSwingTableColumnSet.TableSelectionChange change) {
            if (autoSelection) {
                autoSelectionDepth++;
            }

            int rows = getRowCount();
            int[] selectedModelRowsIndices = lastSelectionActionIndices.stream()
                    .filter(i -> i >= 0 && i < rows)
                    .mapToInt(Integer::intValue).toArray();
            getObjectTableModel().refreshRows(selectedModelRowsIndices);

            //after execution an action, source will be updated by invokeLater
            // change contains info. of the updated source, so the following code intends to be executed after the updating
            SwingDeferredRunner.invokeLater(() -> {
                try {
                    //re-selection
                    changeSelection(selectedModelRowsIndices, change);
                    requestFocusInWindow(); //focusing
                } finally {
                    if (autoSelection) {
                        autoSelectionDepth--;
                    }
                }
            });
        }

        public void changeSelection(int[] selectedModelRowsIndices, GuiSwingTableColumnSet.TableSelectionChange change) {
            switch (change) {
                case GuiSwingTableColumnSet.TableSelectionChangeNothing tableSelectionChangeNothing ->
                        changeSelectionNothing(selectedModelRowsIndices, change);
                case GuiSwingTableColumnSet.TableSelectionChangeIndices tableSelectionChangeIndices ->
                        changeSelectionIndices(selectedModelRowsIndices, tableSelectionChangeIndices);
                case GuiSwingTableColumnSet.TableSelectionChangeValues tableSelectionChangeValues ->
                        changeSelectionValues(selectedModelRowsIndices, tableSelectionChangeValues);
                case GuiSwingTableColumnSet.TableSelectionChangeIndicesRowAndColumn tableSelectionChangeIndicesRowAndColumn ->
                        changeSelectionIndicesRowAndColumns(selectedModelRowsIndices, tableSelectionChangeIndicesRowAndColumn);
                case null, default -> System.err.println("Unknown selection change type: " + change);
            }
        }

        public void changeSelectionNothing(int[] selectedModelRowsIndices, GuiSwingTableColumnSet.TableSelectionChange change) {
            /*
            ListSelectionModel sel = getSelectionModel();
            sel.setValueIsAdjusting(true);
            int rows = getRowCount();
            IntStream.of(selectedModelRowsIndices)
                    .map(this::convertRowIndexToView)
                    .filter(i -> 0 <= i && i < rows)
                    .forEach(i -> sel.addSelectionInterval(i, i));
            sel.setValueIsAdjusting(false);
            */
        }

        public void changeSelectionIndices(int[] selectedModelRowsIndices, GuiSwingTableColumnSet.TableSelectionChangeIndices change) {
            ListSelectionModel sel = getSelectionModel();
            int rows = getRowCount();

            ListSelectionModel colSel = getColumnModel().getSelectionModel();
            int[] cols = colSel.getSelectedIndices();

            Collection<Integer> is = change.indices;

            Set<Integer> update = new HashSet<>(is);
            IntStream.of(selectedModelRowsIndices).forEach(update::remove);

            getObjectTableModel().refreshRows(update.stream()
                    .mapToInt(Integer::intValue)
                    .filter(i -> i >= 0 && i < rows)
                    .sorted()
                    .toArray());
            Arrays.stream(cols)
                    .forEach(col -> colSel.addSelectionInterval(col, col));
            if (cols.length == 0) {
                //selecting all columns
                int colSize = getColumnCount();
                setColumnSelectionInterval(0, colSize - 1);
            }

            sel.setValueIsAdjusting(true);
            sel.clearSelection();
            is.stream()
                    .filter(i -> i >= 0 && i < rows)
                    .mapToInt(this::convertRowIndexToView)
                    .forEach(i -> sel.addSelectionInterval(i, i));
            sel.setValueIsAdjusting(false);
            changeSelectionFinish();
        }

        public void changeSelectionValues(int[] selectedModelRowsIndices, GuiSwingTableColumnSet.TableSelectionChangeValues change) {
            //TODO updating source by action?
            changeSelectionIndices(selectedModelRowsIndices, new GuiSwingTableColumnSet.TableSelectionChangeIndices(change.values.stream()
                    .map(source::indexOf)
                    .collect(Collectors.toList())));
            changeSelectionFinish();
        }

        public void changeSelectionIndicesRowAndColumns(int[] selectedModelRowsIndices, GuiSwingTableColumnSet.TableSelectionChangeIndicesRowAndColumn change) {
            ListSelectionModel sel = getSelectionModel();
            Collection<int[]> is = change.indices;
            int rows = getRowCount();
            int cols = getColumnCount();
            sel.setValueIsAdjusting(true);
            sel.clearSelection();
            is.stream()
                    .filter(p -> p[0] >= 0 && p[0] < rows && p[1] >= 0 && p[1] < cols)
                    .map(p -> new int[] {convertRowIndexToView(p[0]), convertColumnIndexToView(p[1])})
                    .forEach(p -> changeSelection(p[0], p[1], true, false));
            sel.setValueIsAdjusting(false);
            changeSelectionFinish();
        }

        private void changeSelectionFinish() {
            int[] sel = getSelectionModel().getSelectedIndices();
            Rectangle visiRec = getVisibleRect();
            if (sel.length > 0) {
                int n = sel[0];
                Rectangle cellRec = getCellRect(n, 0, true);
                int cy = (int) cellRec.getCenterY();
                if (cy < visiRec.y || visiRec.getMaxY() < cy) {
                    visiRec.y = cellRec.y;
                    scrollRectToVisible(visiRec);
                }
            }
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            try {
                options.begin(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
                GuiSwingView.loadPreferencesDefault(this, prefs, options);
                GuiPreferences targetPrefs = prefs.getDescendant(getSwingViewContext());
                options.apply(preferencesUpdater, targetPrefs);
                getObjectTableModel().getColumnsWithContext().loadSwingPreferences(targetPrefs, options);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
                options.end(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
            }
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            try {
                GuiSwingView.savePreferencesDefault(this, prefs);
                GuiPreferences targetPrefs = prefs.getDescendant(getSwingViewContext());
                PreferencesForTable p = preferencesUpdater.getPrefs();
                p.saveTo(targetPrefs);
                getObjectTableModel().getColumnsWithContext().saveSwingPreferences(targetPrefs);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (customHighlighting) {
                Graphics2D g2 = (Graphics2D) g;
                paintSelectedRows(g2);
            }
        }

        protected void paintSelectedRows(Graphics2D g2) {
            TextCellRenderer.paintRowsBorderSelection(this, g2);
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

        public List<PopupCategorized.CategorizedMenuItem> getColumnHeaderMenuItems() {
            if (columnHeaderMenuItems == null) {
                ColumnResizeModeSwitchAction switchAction = new ColumnResizeModeSwitchAction(this);
                columnHeaderMenuItems = PopupCategorized.getMenuItems(Arrays.asList(
                        new ColumnFitAllWidthAction(this, popupColumnHeader::getTargetColumn,
                                switchAction::updateSelected),
                        switchAction,
                        new ColumnOrderResetAction(this),
                        rowHeightSetAction
                ));
            }
            TableColumn targetColumn;
            ObjectTableColumn objColumn;
            if ((targetColumn = popupColumnHeader.getTargetColumn()) != null &&
                    (objColumn = getObjectTableModel().getColumns().getColumnOrNull(targetColumn.getModelIndex())) != null) {
                return PopupCategorized.getMenuItems(
                        columnHeaderMenuItems,
                        objColumn.getHeaderMenuItems(this));
            }
            return columnHeaderMenuItems;
        }

        @Override
        public int convertColumnIndexToView(int modelColumnIndex) {
            return getObjectTableModel().getColumns().convertColumnModelToView(modelColumnIndex);
        }

        /**
         * update by context and refresh by {@link ObjectTableModel#refreshData()}
         * @since 1.1
         */
        @Override
        public void updateSwingViewSource() {
            ValuePane.super.updateSwingViewSource();
            SwingDeferredRunner.invokeLater(getObjectTableModel()::refreshData);
        }

        /**
         * update by context and refresh by {@link ObjectTableModel#refreshData()}
         * @since 1.1
         */
        @Override
        public void updateSwingViewSourceFromRoot() {
            ValuePane.super.updateSwingViewSourceFromRoot();
            SwingDeferredRunner.invokeLater(getObjectTableModel()::refreshData);
        }

        @Override
        public void setCellEditor(TableCellEditor anEditor) {
            TableCellEditor oldEditor = getCellEditor();
            //suppose setCellEditor(null) at stopping and setCellEditor(nonNullEditor) at starting
            if (oldEditor != null && anEditor == null) {
                GuiSwingActionDefault.ActionPreparation.get(this).unregister(this);
            } else if (oldEditor == null && anEditor != null) {
                GuiSwingActionDefault.ActionPreparation.get(this).register(this, () -> {
                    if (isEditing()) {
                        getCellEditor().stopCellEditing();
                    }
                });
            }
            super.setCellEditor(anEditor);
        }

        @Override
        public void selectionActionPrepare() {
            GuiSwingActionDefault.ActionPreparation.prepareAction(this);
        }

        @Override
        public void setRowHeight(int rowHeight) {
            rowHeightByProgram = rowHeight;
            customRowHeightByUser = false;
            customRowHeightIndividual = false;
            super.setRowHeight(rowHeight);
        }

        /**
         * setting the custom row-height
         * @param rowHeight the new row-height
         * @since 1.3
         */
        public void setRowHeightByUser(int rowHeight) {
            customRowHeightByUser = true;
            super.setRowHeight(rowHeight);
        }

        @Override
        public void setRowHeight(int row, int rowHeight) {
            customRowHeightIndividual = true;
            super.setRowHeight(row, rowHeight);
        }

        /**
         * @return true if it has the value set by {@link #setRowHeightByUser(int)}
         * @since 1.3
         */
        public boolean hasCustomRowHeightByUser() {
            return customRowHeightByUser;
        }

        /**
         * @return the last row-height set by {@link #setRowHeight(int)}
         * @since 1.3
         */
        public int getRowHeightByProgram() {
            return rowHeightByProgram;
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            return null; //rely on table-header
        }

        @Override
        protected JTableHeader createDefaultTableHeader() {
            return new CollectionTableHeader(getColumnModel()); //customize for tool-tip
        }

        /**
         * overrides for preserving custom-row-heights if e is a total refreshing:
         *  If {@link TableModelEvent#getLastRow()} is {@link Integer#MAX_VALUE},
         *   it indicates updating of all rows.
         *   Then, the default impl. of {@link JTable} clears custom all-row-heights set by {@link #setRowHeight(int, int)}.
         *   This causes unintended row-heights expansion by re-painting after the method.
         *  So, the overriding impl. saves before super call,
         *       and resets the saved values before re-painting.
         * @param e a model-event
         */
        @Override
        public void tableChanged(TableModelEvent e) {
            if (e.getLastRow() == Integer.MAX_VALUE && customRowHeightIndividual) {
                int[] allRowHeights = IntStream.range(0, getRowCount())
                        .map(this::getRowHeight)
                        .toArray();
                super.tableChanged(e);
                IntStream.range(0, Math.min(allRowHeights.length, getRowCount()))
                        .forEach(r -> setRowHeight(r, Math.max(1, allRowHeights[r])));
            } else {
                super.tableChanged(e);
            }
        }

        /**
         * obtains an object for row-height customizing
         * @return the action instance associated to the table
         * @since 1.6
         */
        public RowHeightSetAction getRowHeightSetAction() {
            return rowHeightSetAction;
        }
    }

    /**
     * table-header for collection-table: customizing tool-tips
     * @since 1.4
     */
    public static class CollectionTableHeader extends JTableHeader {
        public CollectionTableHeader() {
        }

        public CollectionTableHeader(TableColumnModel cm) {
            super(cm);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            JTable table = getTable();
            return getToolTipText(table, event);
        }

        public static String getToolTipText(JTable table, MouseEvent event) {
            if (table != null) {
                List<String> texts = new ArrayList<>(3);

                Point p = event.getPoint();
                int col = table.getTableHeader().columnAtPoint(p);
                if (col != -1) {
                    TableColumn model = table.getColumnModel().getColumn(col);
                    var hv = model.getHeaderValue();
                    if (hv != null) {
                        texts.add(Objects.toString(hv));
                    }
                    JComponent source = getSourceComponentForToolTip(table, model);
                    if (source != null) {
                        String rowText = source.getToolTipText();
                        if (rowText != null) {
                            texts.add(rowText);
                        }
                    }
                }
                String tableText = table.getToolTipText();
                if (tableText != null) {
                    texts.add(tableText);
                }
                return String.join(": ", texts);
            } else {
                return null;
            }
        }

        public static JComponent getSourceComponentForToolTip(JTable table, TableColumn model) {
            TableCellRenderer renderer = model.getCellRenderer();
            JComponent source = null;
            if (renderer instanceof JComponent) {
                source = (JComponent) renderer;
            } else {
                try {
                    Component c = renderer.getTableCellRendererComponent(table, null, false, false, 0, model.getModelIndex());
                    if (c instanceof JComponent) {
                        source = (JComponent) c;
                    }
                } catch (Exception ex) {
                    //ignore
                }
            }
            return source;
        }
    }

    /**
     * the row-height setting pane: displayed in the header context-menu
     * @since 1.3
     */
    public static class RowHeightSetAction extends JPanel
            implements PopupCategorized.CategorizedMenuItemComponent {
        @Serial private static final long serialVersionUID = 1L;

        protected JTable table;
        protected JCheckBox enabled;
        protected SpinnerNumberModel numModel;
        protected JSpinner num;
        /** @since 1.6 */
        protected JRadioButton radioFit;
        /** @since 1.6 */
        protected JRadioButton radioFixed;
        /** @since 1.6 */
        protected JComponent rowHeightSettingPane;
        /** @since 1.6 */
        protected TableRowHeightFitter fitter;
        /** @since 1.6 */
        protected List<Consumer<RowHeightSetAction>> updateListeners = Collections.emptyList();

        public RowHeightSetAction(JTable table) {
            super(new BorderLayout());
            var ui = UIManagerUtil.getInstance();

            this.table = table;
            this.fitter = new TableRowHeightFitter(table);
            fitter.addListenersToTable();
            fitter.setEnabled(false);
            setOpaque(false);

            enabled = new JCheckBox("Row Height");
            enabled.addActionListener(this::update);
            if (table instanceof CollectionTable) {
                enabled.setSelected(((CollectionTable) table).hasCustomRowHeightByUser());
            }
            radioFit = new JRadioButton("Fit to Content");
            radioFit.addActionListener(this::update);
            radioFixed = new JRadioButton("Fixed Size");
            radioFixed.addActionListener(this::update);

            numModel = new SpinnerNumberModel(table.getRowHeight(), 5, Short.MAX_VALUE, 1);
            numModel.addChangeListener(this::update);
            num = new JSpinner(numModel);
            if (num.getEditor() instanceof JSpinner.NumberEditor) {
                ((JSpinner.NumberEditor) num.getEditor()).getTextField()
                        .addActionListener(this::updateBySpinnerAction);
            }

            int margin = ui.getScaledSizeInt(10);
            int halfMargin = ui.getScaledSizeInt(5);
            setBorder(BorderFactory.createEmptyBorder(halfMargin, margin, halfMargin, margin));
            JComponent pane = ResizableFlowLayout.create(true)
                    .withMargin(margin)
                    .add(enabled)
                    .add(rowHeightSettingPane = ResizableFlowLayout.create(false)
                            .add(ResizableFlowLayout.create(true)
                                    .add(radioFixed)
                                    .add(num)
                                    .withMargin(margin)
                                    .getContainer())
                            .withMargin(margin)
                            .add(radioFit)
                            .getContainer()).getContainer();
            add(pane);
            var settingGroup = new ButtonGroup();
            settingGroup.add(radioFit);
            settingGroup.add(radioFixed);
            radioFixed.setSelected(true);
            enabled.addItemListener(ce ->
                updateEnabled(enabled.isSelected(), rowHeightSettingPane));
            updateEnabled(enabled.isSelected(), rowHeightSettingPane);
        }

        /**
         * {@link Component#setEnabled(boolean)} for the component tree
         * @param on the flag
         * @param comp traversed container
         * @since 1.6
         */
        protected void updateEnabled(boolean on, Container comp) {
            comp.setEnabled(on);
            for (Component child : comp.getComponents()) {
                if (child instanceof Container) {
                    updateEnabled(on, (Container) child);
                }
            }
        }

        @Override
        public JComponent getMenuItem() {
            return this;
        }

        public void update(ActionEvent e) {
            updateHeight();
        }
        public void update(ChangeEvent e) {
            updateHeight();
        }

        public void updateBySpinnerAction(ActionEvent e) {
            enabled.setSelected(true);
            updateHeight();
        }

        public void updateHeight() {
            if (enabled.isSelected()) {
                if (radioFit.isSelected()) {
                    fitter.setEnabled(true);
                    SwingDeferredRunner.invokeLater(fitter::fitAll);
                } else if (radioFixed.isSelected()) {
                    fitter.setEnabled(false);
                    int height = numModel.getNumber().intValue();
                    if (table instanceof CollectionTable) {
                        ((CollectionTable) table).setRowHeightByUser(height);
                    } else {
                        table.setRowHeight(height);
                    }
                }
            } else {
                fitter.setEnabled(false);
                if (table instanceof CollectionTable colTable) {
                    colTable.setRowHeight(colTable.getRowHeightByProgram());
                }
            }
            updateListeners.forEach(u -> u.accept(this));
        }

        @Override
        public String getCategory() {
            return CATEGORY_COLUMN_RESIZE;
        }

        /**
         *
         * @return true if "Row Height" is enabled and "Fit to Content" is selected.
         * @since 1.6
         */
        public boolean isRowHeightFitToContent() {
            return enabled.isSelected() && radioFit.isSelected();
        }

        /**
         * @param fit if true, enable "Row Height" and select "Fit to Content"
         * @since 1.6
         */
        public void setRowHeightFitToContent(boolean fit) {
            radioFit.setSelected(fit);
            if (fit) {
                radioFixed.setSelected(false);
                enabled.setSelected(true);
            }
            updateHeight();
        }

        /**
         * @param l the added listener
         * @since 1.6
         */
        public void addUpdateListener(Consumer<RowHeightSetAction> l) {
            if (updateListeners.isEmpty()) {
                updateListeners = new ArrayList<>(1);
            }
            updateListeners.add(l);
        }

        /**
         * @return registered listeners. the direct list reference of the field
         * @since 1.6
         */
        public List<Consumer<RowHeightSetAction>> getUpdateListeners() {
            return updateListeners;
        }
    }

    /**
     * a table source for obtaining indices instead of row items.
     * This can be supplied for {@link GuiSwingTableColumnSetDefault.TableSelectionListAction}.
     */
    public static class TableSelectionSourceForIndices implements GuiSwingTableColumnSet.TableSelectionSource {
        protected CollectionTable table;
        protected TableTargetCellForJTable cellTargets;
        protected boolean rowAndColumns;

        public TableSelectionSourceForIndices(CollectionTable table, boolean rowAndColumns) {
            this.table = table;
            cellTargets = new TableTargetCellForCollectionTable(table);
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
            table.getSelectedItems(); //saving selected indices

            if (rowAndColumns) {
                //List<int[]>
                return cellTargets.getSelectedRowAllCellIndicesAsList();
            } else {
                //List<Integer>
                return IntStream.of(cellTargets.getSelectedRows())
                        .boxed()
                        .collect(Collectors.toList());
            }
        }

        @Override
        public void selectionActionFinished(boolean autoSelection, GuiSwingTableColumnSet.TableSelectionChange change) {
            table.selectionActionFinished(autoSelection, change);
        }

        @Override
        public void selectionActionPrepare() {
            table.selectionActionPrepare();
        }
    }

    public static class TableTargetCellForCollectionTable extends TableTargetCellForJTable {
        public TableTargetCellForCollectionTable(CollectionTable table) {
            super(table);
        }

        public CollectionTable getCollectionTable() {
            return (CollectionTable) getTable();
        }

        @Override
        public int[] convertViewToData(int viewRow, int viewColumn) {
            int[] idx = super.convertViewToData(viewRow, viewColumn);
            ObjectTableColumn column = getCollectionTable().getObjectTableModel().getColumns().getColumnAt(idx[1]);

            int[] subIndex = column.columnIndexToValueIndex(idx[1]); //subIndex = {column, prop}
            if (subIndex == null) {
                return null;
            } else {
                int[] res = new int[1 + subIndex.length];
                res[0] = idx[0];
                System.arraycopy(subIndex, 0, res, 1, subIndex.length);
                return res;
            }
        }
    }

    /**
     * enable only row-sorter and disable column-width and column-ordering which are treated
     *  by {@link GuiSwingTableModelCollection.GuiSwingTableModelColumns}.
     *
     */
    public static class TablePreferencesUpdaterWithDynamic extends TablePreferencesUpdater {
        public TablePreferencesUpdaterWithDynamic(JTable table, GuiMappingContext context) {
            super(table, context);
        }

        @Override
        public void setUpListeners() {
            table.getRowSorter().addRowSorterListener(this);
            setUpListenersRowHeight();
        }

        @Override
        public void apply(GuiPreferences p) {
            savingDisabled = true;
            try {
                prefs.loadFrom(p);
                prefs.applyRowSortTo(table);
                prefs.applyRowHeightTo(table);
            } finally {
                savingDisabled = false;
            }
        }

        @Override
        public void applyJson(Map<String, Object> json) {
            savingDisabled = true;
            try {
                prefs.setJson(json);
                prefs.applyRowSortTo(table);
                prefs.applyRowHeightTo(table);
            } finally {
                savingDisabled = false;
            }
        }
    }

    public static class TablePreferencesUpdater implements TableColumnModelListener, RowSorterListener {
        protected JTable table;
        protected GuiMappingContext context;
        protected PreferencesForTable prefs;
        protected Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> updater;

        protected boolean savingDisabled = false;

        public TablePreferencesUpdater(JTable table, GuiMappingContext context) {
            this.table = table;
            this.context = context;
            prefs = new PreferencesForTable();
        }

        public void setUpListeners() {
            table.getColumnModel().addColumnModelListener(this);
            table.getRowSorter().addRowSorterListener(this);
            setUpListenersRowHeight();
        }

        /**
         * @since 1.3
         */
        public void setUpListenersRowHeight() {
            table.addPropertyChangeListener("rowHeight", this::rowHeightChanged);
            if (table instanceof CollectionTable) {
                ((CollectionTable) table).getRowHeightSetAction()
                        .addUpdateListener(a -> rowHeightChanged(null));
            }
        }

        public void setPrefs(PreferencesForTable prefs) {
            this.prefs = prefs;
        }

        public void setUpdater(Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> updater) {
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
                updater.accept(new GuiSwingPrefsSupports.PreferencesUpdateEvent(context, prefs));
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

        /**
         *
         * @param e the event, can be null
         * @since 1.3
         */
        public void rowHeightChanged(PropertyChangeEvent e) {
            if (!savingDisabled) {
                prefs.setRowHeightFrom(table);
                sendToUpdater();
            }
        }
    }

    public static class PreferencesForTable implements GuiSwingPrefsSupports.PreferencesByJsonEntry {
        protected List<Integer> columnOrder = new ArrayList<>();
        protected List<Integer> columnWidth = new ArrayList<>();
        protected List<PreferencesForTableRowSort> rowSort = new ArrayList<>();
        protected int sizeLimit = 100;
        /** @since 1.3 */
        protected int rowHeight = -1;
        /** @since 1.6 */
        protected boolean rowCustom;
        /** @since 1.6 */
        protected boolean rowFitToContent;

        /**
         * @param inits the source inits
         * @return creates and sets settings related to the row-height; can be set by {@link #applyRowHeightTo(JTable)}
         * @since 1.8
         */
        public static PreferencesForTable createRowHeightFromInits(GuiInits inits) {
            PreferencesForTable t = new PreferencesForTable();
            t.setRowCustom(inits.table().rowFitToContent() || inits.table().rowHeight() > 0);
            t.setRowFitToContent(inits.table().rowFitToContent());
            t.setRowHeight(inits.table().rowHeight());
            return t;
        }

        public PreferencesForTable() {}

        /**
         * @return the direct reference to the columnOrder list
         * @since 1.7
         */
        public List<Integer> getColumnOrder() {
            return columnOrder;
        }

        /**
         * @return the direct reference to the columnWidth list
         * @since 1.7
         */
        public List<Integer> getColumnWidth() {
            return columnWidth;
        }

        /**
         * @return the direct reference to the rowSort list
         * @since 1.7
         */
        public List<PreferencesForTableRowSort> getRowSort() {
            return rowSort;
        }

        /**
         * @return rowHeight
         * @since 1.7
         */
        public int getRowHeight() {
            return rowHeight;
        }

        /**
         * @param rowHeight a new rowHeight value
         * @since 1.7
         */
        public void setRowHeight(int rowHeight) {
            this.rowHeight = rowHeight;
        }

        /**
         * @return the property value
         * @since 1.7
         */
        public boolean isRowCustom() {
            return rowCustom;
        }

        /**
         * @param rowCustom a new value
         * @since 1.7
         */
        public void setRowCustom(boolean rowCustom) {
            this.rowCustom = rowCustom;
        }

        /**
         * @return the property value
         * @since 1.7
         */
        public boolean isRowFitToContent() {
            return rowFitToContent;
        }

        /**
         * @param rowFitToContent a new value
         * @since 1.7
         */
        public void setRowFitToContent(boolean rowFitToContent) {
            this.rowFitToContent = rowFitToContent;
        }

        public void applyTo(JTable table) {
            TableColumnModel columnModel = table.getColumnModel();
            applyColumnWidthTo(table, columnModel);
            applyColumnOrderTo(table, columnModel);
            applyRowSortTo(table);
            applyRowHeightTo(table);
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
                if (0 <= row.getColumn() && row.getColumn() < len) {
                    keys.add(new RowSorter.SortKey(row.getColumn(), order));
                }
            }
            if (!keys.isEmpty()) {
                table.getRowSorter().setSortKeys(keys);
            }
        }

        /**
         * @param table the target table
         * @since 1.3
         */
        public void applyRowHeightTo(JTable table) {
            if (rowCustom) {
                if (table instanceof CollectionTable) {
                    ((CollectionTable) table).getRowHeightSetAction().setRowHeightFitToContent(rowFitToContent);
                }
                if (rowHeight > 0) {
                    if (table instanceof CollectionTable) {
                        ((CollectionTable) table).setRowHeightByUser(rowHeight);
                    } else {
                        table.setRowHeight(rowHeight);
                    }
                }
            }
        }

        public void setColumnOrderFrom(JTable table) {
            TableColumnModel columnModel = table.getColumnModel();
            columnOrder.clear();
            for (int i = 0, len = columnModel.getColumnCount(); i < len && i < sizeLimit; ++i) {
                columnOrder.add(table.convertColumnIndexToModel(i));
            }
        }

        public void setColumnWidthFrom(JTable table) {
            TableColumnModel columnModel = table.getColumnModel();
            columnWidth.clear();
            for (int i = 0, len = columnModel.getColumnCount(); i < len && i < sizeLimit; ++i) {
                columnWidth.add(columnModel.getColumn(table.convertColumnIndexToView(i)).getWidth());
            }
        }

        public void setRowSortFrom(JTable table) {
            rowSort.clear();
            for (RowSorter.SortKey key : table.getRowSorter().getSortKeys()) {
                if (rowSort.size() >= sizeLimit) {
                    break;
                }
                PreferencesForTableRowSort row = new PreferencesForTableRowSort(key.getColumn(), key.getSortOrder().name());
                rowSort.add(row);
            }
        }

        /**
         * @param table the source table
         * @since 1.3
         */
        public void setRowHeightFrom(JTable table) {
            if (table instanceof CollectionTable colTable) {
                rowFitToContent = colTable.getRowHeightSetAction().isRowHeightFitToContent();
                if (colTable.hasCustomRowHeightByUser()) {
                    rowHeight = colTable.getRowHeight();
                } else {
                    rowHeight = -1;
                }
            } else {
                rowHeight = table.getRowHeight();
            }
            rowCustom = (rowFitToContent || rowHeight > 0);
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
            map.put("rowCustom", rowCustom);
            map.put("rowFitToContent", rowFitToContent);
            map.put("rowHeight", rowHeight);
            return map;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setJson(Object json) {
            if (json instanceof Map<?,?> map) {
                GuiSwingPrefsSupports.setAsList(columnOrder, map, Integer.class, "columnOrder");
                GuiSwingPrefsSupports.setAsList(columnWidth, map, Integer.class, "columnWidth");
                GuiSwingPrefsSupports.setAsList(rowSort, map, Map.class, "rowSort", PreferencesForTableRowSort::new);
                rowCustom = GuiSwingPrefsSupports.getAs(map, Boolean.class, "rowCustom", false);
                rowFitToContent = GuiSwingPrefsSupports.getAs(map, Boolean.class, "rowFitToContent", false);
                rowHeight = GuiSwingPrefsSupports.getAs(map, Integer.class, "rowHeight", -1);
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

        public PreferencesForTableRowSort(int column, SortOrder order) {
            this(column, order.name());
        }

        public PreferencesForTableRowSort(Map<?, ?> map) {
            column = GuiSwingPrefsSupports.getAs(map, Integer.class, "column", 0);
            order = GuiSwingPrefsSupports.getAs(map, String.class, "order", "");
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
        @Serial private static final long serialVersionUID = 1L;
        protected JTable table;

        public SelectAllAction(JTable table) {
            putValue(NAME, "Select All");

            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_A,
                            PopupExtension.getMenuShortcutKeyMask()));
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiSwingActionDefault.ActionPreparation.prepareAction(table);
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
            super(pane, keyMatcher, null, new DefaultPopupGetter(pane));
            new MenuKeySelector().addToMenu(menu.get());

            this.table = pane;

            var tableActions = new PopupCategorized(items, null,
                                        new ObjectTableModel.MenuBuilderWithEmptySeparator());
            var columnBuilder = new CollectionColumnMenuSupplier(this);
            var cellBuilder = new CollectionCellMenuSupplier(this);
            setMenuBuilder((filter, m) -> builder(filter, m, tableActions, columnBuilder, cellBuilder));

            menu.get().addPopupMenuListener(this);
            showingTimer = new Timer(100, e -> showing = false);
            showingTimer.setRepeats(false);
        }

        /**
         * the building impl. for a collection-table; will be set {@link #setMenuBuilder(PopupMenuBuilder)} with sub-builders
         * @param filter the given filter
         * @param m the target menu
         * @param tableActions  the action builder for "Table"
         * @param columnBuilder the action builder for "Target Column"
         * @param cellBuilder   the action builder for "Selected Cells" and "Selected Rows"
         * @since 1.8
         */
        protected void builder(PopupMenuFilter filter, Consumer<Object> m,
                               PopupCategorized tableActions, CollectionColumnMenuSupplier columnBuilder, CollectionCellMenuSupplier cellBuilder) {
            if (table.getSelectedColumnCount() > 1) {
                cellBuilder.buildCells(filter, m);
                buildPopup(m,
                        menuToItems(
                                columnBuilder.buildAsSubMenu(filter),
                                buildTableActionsAsSubMenu(tableActions, filter),
                                cellBuilder.buildRowsAsSubMenu(filter)));
            } else {
                columnBuilder.build(filter, m);
                buildPopup(m,
                        menuToItems(
                                buildTableActionsAsSubMenu(tableActions, filter),
                                cellBuilder.buildCellsAsSubMenu(filter),
                                cellBuilder.buildRowsAsSubMenu(filter)));
            }
        }

        private List<? extends PopupCategorized.CategorizedMenuItem> menuToItems(JMenu... menu) {
            return Arrays.stream(menu)
                    .map(PopupCategorized.CategorizedMenuItemComponentDefault::new)
                    .toList();
        }

        private void buildPopup(Consumer<Object> menu, List<?>... items) {
            new PopupCategorized(() -> PopupCategorized.getMenuItems(items))
                    .build(new MenuSeparator(filter), menu);
        }

        /**
         *
         * @param tableActions the given actions
         * @param filter  the for building the actions
         * @return a new sub-menu with building the tableActions with the filter
         * @since 1.8
         */
        protected JMenu buildTableActionsAsSubMenu(PopupCategorized tableActions, PopupMenuFilter filter) {
            JMenu subMenu = new JMenu("Table");
            tableActions.build(filter, new MenuBuilder.MenuAppender(subMenu));
            return subMenu;
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

        @Override
        public void setupMenu() {
            Point p = ((DefaultPopupMenu) getMenu()).getPopupLocationOnTargetPane();
            updatePopupLocation(p);
            super.setupMenu();
        }

        public CollectionTable getTable() {
            return table;
        }

        @Override
        public void mousePressed(MouseEvent e) { //disabled
            if (!e.isPopupTrigger() && showing) { //closing the popup menu, and then nothing
                e.consume();
                return;
            }
            int viewColumn = table.columnAtPoint(e.getPoint());
            lastClickColumnIndex = table.convertColumnIndexToModel(viewColumn);
            if (e.isPopupTrigger()) {
                targetColumnIndex = lastClickColumnIndex;
            }
            super.mousePressed(e);
        }

        /**
         * @param p location with the table coordinate
         * @since 1.3
         */
        public void updatePopupLocation(Point p) {
            int viewColumn = table.columnAtPoint(p);
            lastClickColumnIndex = table.convertColumnIndexToModel(viewColumn);
            targetColumnIndex = lastClickColumnIndex;
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
            return table.getObjectTableModel().getColumns().getColumnOrNull(targetColumnIndex);
        }

        public List<ObjectTableColumn> getTargetColumns() {
            ObjectTableModel model = table.getObjectTableModel();
            return IntStream.of(table.getSelectedColumns())
                    .map(table::convertColumnIndexToModel)
                    .mapToObj(model.getColumns()::getColumnAt)
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
//                        System.err.println("bind action:" +
//                                keyAction.getValue(Action.NAME) + "  key:" +
//                                keyAction.getValue(Action.ACCELERATOR_KEY));
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

        /**
         * @param filter the given filter
         * @return a created JMenu including items created by {@link #build(PopupExtension.PopupMenuFilter, Consumer)}
         * @since 1.8
         */
        public JMenu buildAsSubMenu(PopupExtension.PopupMenuFilter filter) {
            JMenu colMenu = new JMenu("Target Column");
            build(filter, new MenuBuilder.MenuAppender(colMenu));
            return colMenu;
        }

            @Override
        public void build(PopupExtension.PopupMenuFilter filter, Consumer<Object> menu) {
            ObjectTableColumn column = popup.getTargetColumn();
            ObjectTableColumn.PopupMenuBuilderSource src = (column == null ? null : column.getMenuBuilderSource());
            if (src != null) {
                PopupExtension.PopupMenuBuilder builder = src.getMenuBuilder(popup.getTable());
                if (builder != null) {
                    builder.build(new MenuSeparator("Column: " + column.getTableColumn().getHeaderValue(), filter).withoutSeparator(), menu);
                }
            }
        }
    }


    public static class MenuSeparator implements PopupExtension.PopupMenuFilter {
        protected PopupExtension.PopupMenuFilter filter;
        protected boolean beforeReturned = false;
        protected String title;
        /** @since 1.8 */
        protected boolean separator = true;

        public MenuSeparator(PopupExtension.PopupMenuFilter filter) {
            this.filter = filter;
        }

        public MenuSeparator(String title, PopupExtension.PopupMenuFilter filter) {
            this.title = title;
            this.filter = filter;
        }

        /**
         * @return this with separator = false
         * @since 1.8
         */
        public MenuSeparator withoutSeparator() {
            separator = false;
            return this;
        }

        @Override
        public Object convert(Object item) {
            return filter.convert(item);
        }

        @Override
        public List<Object> aroundItems(boolean before) {
            if (before && !beforeReturned) {
                List<Object> bs = new ArrayList<>(filter.aroundItems(true));
                Object s = (separator ? filter.convert(new JPopupMenu.Separator()) : null);
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

        /**
         * constructs menu-items for "Selected Cells"
         * @param filter the given filter
         * @param menu the target item holder
         * @since 1.8
         */
        public void buildCells(PopupExtension.PopupMenuFilter filter, Consumer<Object> menu) {
            buildCellsOrRows(filter, menu, false);
        }

        /**
         * @param filter the given filter
         * @return a created JMenu including items by {@link #buildCellsOrRows(PopupExtension.PopupMenuFilter, Consumer, boolean)}
         *  with false: "Selected Cells"
         * @since 1.8
         */
        public JMenu buildCellsAsSubMenu(PopupExtension.PopupMenuFilter filter) {
            JMenu row = new JMenu("Selected Cells");
            buildCellsOrRows(filter, new MenuBuilder.MenuAppender(row), false);
            return row;
        }

        /**
         * @param filter the given filter
         * @return a created JMenu including items by {@link #buildCellsOrRows(PopupExtension.PopupMenuFilter, Consumer, boolean)}
         *  with true: "Selected Rows"
         * @since 1.8
         */
        public JMenu buildRowsAsSubMenu(PopupExtension.PopupMenuFilter filter) {
            JMenu row = new JMenu("Selected Rows");
            buildCellsOrRows(filter, new MenuBuilder.MenuAppender(row), true);
            return row;
        }

        @Override
        public void build(PopupExtension.PopupMenuFilter filter, Consumer<Object> menu) {
            buildCellsOrRows(filter, menu, false);
            buildCellsOrRows(filter, menu, true);
        }

        /**
         * @param filter a menu-filter
         * @param menu the mnu host
         * @param rows if true, creates menus for selected-rows, instead, menus for selected-cells
         * @since 1.8
         */
        protected void buildCellsOrRows(PopupExtension.PopupMenuFilter filter, Consumer<Object> menu, boolean rows) {
            CollectionTable table = popup.getTable();
            List<ObjectTableColumn> targetCols = rows ?
                    table.getObjectTableModel().getColumns().getColumns() :
                    popup.getTargetColumns();
            filter = new ObjectTableModel.CollectionRowsAndCellsActionBuilder(table, filter);
            table.getObjectTableModel().getBuilderForRowsOrCells(table, targetCols, rows)
                    .build(new MenuSeparator(filter).withoutSeparator(), menu);
        }
    }

    public static class ToStringCollectionTransferHandler extends TransferHandler {
        @Serial private static final long serialVersionUID = 1L;
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
                List<?> v = pane.getSwingViewValue();
                String data = pane.executeContextTask(
                        () -> pane.getSwingViewContext().getRepresentation()
                        .toHumanReadableString(pane.getSwingViewContext(), v), null)
                        .getValueOr(null, null);
                if (data != null) {
                    return new StringSelection(data);
                } else {
                    return null;
                }
            } else {
                //manual composition of to-string action for columns of selected cells.
                List<ToStringCopyCell.TableMenuCompositeToStringCopy> composites = pane.getPopup().getTargetColumns().stream()
                            .flatMap(col -> col.getCompositesForCells().stream()
                                    .filter(ToStringCopyCell.TableMenuCompositeToStringCopy.class::isInstance)
                                    .map(ToStringCopyCell.TableMenuCompositeToStringCopy.class::cast))
                            .collect(Collectors.toList());
                ToStringCopyForCellsAction action = new ToStringCopyForCellsAction(pane.getSwingViewContext(), composites, true);
                String data = action.getString(action.getSelectedCells(new TableTargetCellForJTable(pane)));
                return new StringSelection(data);

            }
        }
    }

    public static class UnSelectAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected JTable table;

        public UnSelectAction(JTable table) {
            super("Clear Selection");

            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_A,
                            PopupExtension.getMenuShortcutKeyMask(),
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
        @Serial private static final long serialVersionUID = 1L;
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

    public static class PopupExtensionCollectionColumnHeader extends PopupExtension implements MouseListener {
        /** @since 1.3 */
        protected Point targetPoint;
        protected TableColumn targetColumn;

        public PopupExtensionCollectionColumnHeader(JTable table, PopupMenuBuilder menuBuilder) {
            super(table, PopupExtension.getDefaultKeyMatcher(), menuBuilder, new DefaultPopupGetter(table.getTableHeader()));
        }

        public JTable getPaneTable() {
            return (JTable) getPane();
        }

        @Override
        public void addListenersTo(JComponent pane) {
            getMenu().addPopupMenuListener(new PopupMenuListenerForSetup(this) {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    Point p = ((DefaultPopupMenu) getMenu()).getPopupLocationOnTargetPane();
                    updateTargetPoint(p);
                    super.popupMenuWillBecomeVisible(e);
                }
            });
        }

        private void updateTargetPoint(Point p) {
            targetPoint = p;
            targetColumn = null;
        }

        @Override
        public void mousePressed(MouseEvent e) { //disabled
            updateTargetPoint(e.getPoint());
            super.mousePressed(e);
        }

        public TableColumn getTargetColumn() {
            if (targetColumn == null) {
                if (targetPoint == null) {
                    JTable table = getPaneTable();
                    int column = table.getSelectedColumn();
                    if (0 <= column && column < table.getColumnCount()) {
                        targetColumn = table.getColumnModel().getColumn(column);
                    }
                } else {
                    targetColumn = getTargetColumn(targetPoint);
                }
            }
            return targetColumn;
        }

        /**
         * @param p the clicked point of source component
         * @return the column under the point
         * @since 1.3
         */
        public TableColumn getTargetColumn(Point p) {
            JTable table = getPaneTable();
            TableColumnModel model = table.getColumnModel();
            JTableHeader header = table.getTableHeader();
            int i = Math.min(model.getColumnCount() - 1,
                    (int) (p.getX() / (header.getWidth() / (double) model.getColumnCount())));
            int inc = 0;
            int n = 0;
            while (0 <= i && i < model.getColumnCount() && n < model.getColumnCount()) {
                Rectangle r = header.getHeaderRect(i);
                if (r.contains(p)) {
                    return model.getColumn(i);
                } else if (inc == 0) {
                    inc = r.getMaxX() < p.getX() ? 1 : -1;
                }
                i += inc;
                ++n;
            }
            return null;
        }
    }

    public static String CATEGORY_COLUMN_RESIZE = MenuBuilder.getCategoryImplicit("Column Width");
    public static String CATEGORY_COLUMN_ORDER = MenuBuilder.getCategoryImplicit("Column Order");

    public static class ColumnFitAllWidthAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected JTable table;
        protected Supplier<TableColumn> targetColumn;
        protected Runnable afterRunner;

        public ColumnFitAllWidthAction(JTable table, Supplier<TableColumn> targetColumn, Runnable afterRunner) {
            putValue(NAME, "Set All Column Width to This");
            this.table = table;
            this.targetColumn = targetColumn;
            this.afterRunner = afterRunner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TableColumn c = targetColumn.get();
            if (c != null) {
                table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                int w = c.getWidth();
                Collections.list(table.getColumnModel().getColumns())
                        .forEach(otherCol -> otherCol.setPreferredWidth(w));
                if (afterRunner != null) {
                    afterRunner.run();
                }
            }
        }

        @Override
        public String getCategory() {
            return CATEGORY_COLUMN_RESIZE;
        }
    }

    public static class ColumnResizeModeSwitchAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemActionCheck {
        @Serial private static final long serialVersionUID = 1L;
        protected JTable table;

        public ColumnResizeModeSwitchAction(JTable table) {
            putValue(NAME, "Auto Resize Column Width");
            this.table = table;
            updateSelected();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isSelected()) {
                table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            } else {
                table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            }
            updateSelected();
        }

        public boolean isSelected() {
            return table.getAutoResizeMode() != JTable.AUTO_RESIZE_OFF;
        }

        public void updateSelected() {
            putValue(SELECTED_KEY, isSelected());
        }


        @Override
        public String getCategory() {
            return CATEGORY_COLUMN_RESIZE;
        }
    }

    public static class ColumnOrderResetAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected JTable table;

        public ColumnOrderResetAction(JTable table) {
            putValue(NAME, "Reset Column Order");
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TableColumnModel model = table.getColumnModel();
            List<TableColumn> cols = new ArrayList<>(Collections.list(model.getColumns()));

            Map<Integer,Integer> modelToView = new HashMap<>(cols.size());

            IntStream.range(0, cols.size())
                    .forEach(i -> modelToView.put(cols.get(i).getModelIndex(), i));

            cols.sort(Comparator.comparing(TableColumn::getModelIndex));

            for (TableColumn col : cols) {
                int mi = col.getModelIndex();
                int vi = modelToView.get(mi);
                model.moveColumn(vi, mi);
                modelToView.remove(mi);

                for (Map.Entry<Integer,Integer> entry : modelToView.entrySet()) {
                    int i = entry.getValue();
                    if (mi <= i && i < vi) { //always mi <= vi
                        entry.setValue(i + 1);
                    }
                }
            }
        }

        @Override
        public String getCategory() {
            return CATEGORY_COLUMN_ORDER;
        }
    }
}
