package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiMappingContext.GuiSourceValue;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprCollectionTable;
import org.autogui.base.mapping.GuiReprValue.ObjectSpecifier;
import org.autogui.swing.GuiSwingJsonTransfer;
import org.autogui.swing.prefs.GuiSwingPrefsSupports.PreferencesUpdateEvent;
import org.autogui.swing.prefs.GuiSwingPrefsSupports;
import org.autogui.swing.prefs.GuiSwingPrefsSupports.PreferencesUpdateSupport;
import org.autogui.swing.GuiSwingTaskRunner;
import org.autogui.swing.GuiSwingView.SettingsWindowClient;
import org.autogui.swing.GuiSwingViewCollectionTable.CollectionTable;
import org.autogui.swing.prefs.GuiSwingPrefsApplyOptions;
import org.autogui.swing.table.GuiSwingTableColumn.ObjectTableColumnWithContext;
import org.autogui.swing.table.GuiSwingTableColumn.SpecifierManagerIndex;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.util.PopupExtensionText;
import org.autogui.swing.util.SettingsWindow;

import javax.swing.*;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * a table-model with {@link GuiMappingContext}
 */
@SuppressWarnings("this-escape")
public class GuiSwingTableModelCollection extends ObjectTableModel {
    @Serial private static final long serialVersionUID = 1L;
    protected GuiMappingContext elementContext;
    protected Supplier<ObjectSpecifier> tableSpecifier;
    protected SpecifierManagerIndex rowSpecifierManager;

    public GuiSwingTableModelCollection(GuiMappingContext context, Supplier<ObjectSpecifier> tableSpecifier,
                                      Supplier<Object> source) {
        super(new GuiSwingTaskRunner(context));
        this.tableSpecifier = tableSpecifier;
        this.rowSpecifierManager = new SpecifierManagerIndex(tableSpecifier);
        setElementContextFromContext();
        setSource(source);
    }

    public GuiMappingContext getContext() {
        return runner.getContext();
    }

    @Override
    public void initColumns() {
        columns = new GuiSwingTableModelColumns(this);
    }

    public GuiSwingTableModelColumns getColumnsWithContext() {
        return (GuiSwingTableModelColumns) columns;
    }

    protected void setElementContextFromContext() {
        GuiMappingContext context = getContext();
        elementContext = context.getReprCollectionTable().getElementContext(context);
    }

    @Override
    public Object getRowAtIndex(int row) {
        Object collection = getCollectionFromSource();
        try {
            ObjectSpecifier specifier = rowSpecifierManager.getSpecifierWithSettingIndex(row);
            return elementContext.getReprValue()
                    .getValueWithoutNoUpdate(elementContext, GuiSourceValue.of(collection), specifier);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int getRowCountUpdated() {
        Object collection = getCollectionFromSource();
        try {
            return elementContext.getReprValue()
                    .getValueCollectionSize(elementContext, GuiSourceValue.of(collection), tableSpecifier.get());
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object getCollectionFromSource() {
        return super.getCollectionFromSource();
        /*
        if (collection == null) {
            try {
                collection = context.getReprValue()
                        .getUpdatedValueWithoutNoUpdate(context, tableSpecifier.get());
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
        return collection;
        */
    }

    public SpecifierManagerIndex getRowSpecifierManager() {
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

    /** a subclass of {@link ObjectTableModelColumns} with supporting settings and preferences */
    public static class GuiSwingTableModelColumns extends ObjectTableModelColumns
            implements PreferencesUpdateSupport, SettingsWindowClient {

        protected SettingsWindow settingsWindow;
        protected Consumer<PreferencesUpdateEvent> prefsUpdater;
        protected GuiPreferences currentPreferences;
        protected GuiSwingTableModelCollection tableModel;

        protected PreferencesForTableColumnOrderStatic nonContextOrder;
        protected PreferencesForTableColumnWidthStatic nonContextWidth;

        protected List<PreferencesForTableColumnOrder> pendingOrders;

        public GuiSwingTableModelColumns(GuiSwingTableModelCollection tableModel) {
            super(tableModel);
            this.tableModel = tableModel;
            nonContextOrder = new PreferencesForTableColumnOrderStatic();
            nonContextWidth = new PreferencesForTableColumnWidthStatic();
            pendingOrders = new ArrayList<>();
        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {
            super.columnMoved(e);

            int to = e.getToIndex();
            if (to != e.getFromIndex()) {
                TableColumn c = columnModel.getColumn(to);
                ObjectTableColumn model = columns.get(c.getModelIndex());
                PreferencesForTableColumnOrder order = new PreferencesForTableColumnOrder(c.getModelIndex(), to);
                if (!(model instanceof ObjectTableColumnWithContext)) { //non-context columns: saved in the table prefs
                    nonContextOrder.put(model.getTableColumn().getModelIndex(), order);
                    if (prefsUpdater != null) {
                        this.prefsUpdater.accept(new PreferencesUpdateEvent(getPrefsContextForColumn(model), nonContextOrder));
                    }
                } else {
                    if (prefsUpdater != null) {
                        this.prefsUpdater.accept(new PreferencesUpdateEvent(getPrefsContextForColumn(model), order));
                    }
                }
            }
        }

        public GuiMappingContext getPrefsContextForColumn(ObjectTableColumn column) {
            if (column instanceof ObjectTableColumnWithContext) {
                return ((ObjectTableColumnWithContext) column).getContext();
            } else {
                return tableModel.getContext();
            }
        }

        @Override
        protected void columnAdded(ObjectTableColumn column, DynamicColumnContainer d) {
            super.columnAdded(column, d);

            if (settingsWindow != null && column instanceof SettingsWindowClient) {
                ((SettingsWindowClient) column).setSettingsWindow(settingsWindow);
            }
            if (prefsUpdater != null && column instanceof PreferencesUpdateSupport) {
                ((PreferencesUpdateSupport) column).setPreferencesUpdater(prefsUpdater);
            }
            if (currentPreferences != null && column instanceof ObjectTableColumnWithContext) {
                ((ObjectTableColumnWithContext) column).loadSwingPreferences(currentPreferences);
            }

            pendingOrders.removeIf(o -> o.applyTo(this));

            if (!(column instanceof ObjectTableColumnWithContext)) {
                applyPrefsToNonContext(column);
            } else {
                applyPrefsTo((ObjectTableColumnWithContext) column, d);
            }


            column.getTableColumn().addPropertyChangeListener(e -> {
                if (e.getPropertyName().equals("width")) {
                    columnWidthUpdated(column, ((Number) e.getNewValue()).intValue());
                }
            });
        }

        public void columnWidthUpdated(ObjectTableColumn column, int width) {
            //both dynamic and non-dynamic columns are stored in each prefs of context:
            // thus, dynamic ones will be merged to a single entry and only the last update is saved.
            if (prefsUpdater != null) {
                PreferencesForTableColumnWidth w = new PreferencesForTableColumnWidth(width);
                if (!(column instanceof ObjectTableColumnWithContext)) { //for row-index, ...
                    nonContextWidth.put(column.getTableColumn().getModelIndex(), w);
                    prefsUpdater.accept(new PreferencesUpdateEvent(
                            getPrefsContextForColumn(column), nonContextWidth));
                } else {
                    prefsUpdater.accept(new PreferencesUpdateEvent(
                            getPrefsContextForColumn(column), w));
                }
            }
        }

        public void applyPrefsTo(ObjectTableColumnWithContext column, DynamicColumnContainer d) {
            GuiPreferences prefs = column.getContext().getPreferences();
            try (var lock = prefs.lock()) {
                lock.use();
                PreferencesForTableColumnWidth w = new PreferencesForTableColumnWidth();
                w.loadFrom(prefs);
                w.applyTo(column.asColumn());

                if (d == null) { //non-dynamic column: the ordering might be incorrect due to the lack of dynamic column re-ordering
                    PreferencesForTableColumnOrder o = new PreferencesForTableColumnOrder();
                    o.loadFrom(prefs);
                    //o.modelIndex == column.tableModel.modelIndex
                    if (!o.applyTo(this)) {
                        pendingOrders.add(o);
                    }
                }
            }
        }

        public void applyPrefsToNonContext(ObjectTableColumn column) {
            applyPrefsToNonContext(column, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
        }

        /**
         * @param column the target column
         * @param options processor
         * @since 1.7
         */
        public void applyPrefsToNonContext(ObjectTableColumn column, GuiSwingPrefsApplyOptions options) {
            try {
                options.begin(column, null, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
                options.applyTo(nonContextWidth, column);
                if (!options.applyTo(nonContextOrder, this, column.getTableColumn().getModelIndex())) {
                    PreferencesForTableColumnOrder o = nonContextOrder.get(column.getTableColumn().getModelIndex());
                    if (o != null) {
                        pendingOrders.add(o);
                    }
                }
            } finally {
                options.end(column, null, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
            }
        }

        public void loadPrefsTo(GuiPreferences parentPrefs, ObjectTableColumnWithContext column) {
            loadPrefsTo(parentPrefs, column, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
        }

        /**
         * @param parentPrefs parent prefs for the column
         * @param column the target column
         * @param options the processor
         * @since 1.7
         */
        public void loadPrefsTo(GuiPreferences parentPrefs, ObjectTableColumnWithContext column, GuiSwingPrefsApplyOptions options) {
            try {
                options.begin(column, parentPrefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
                GuiPreferences prefs = parentPrefs.getDescendant(column.getContext());
                PreferencesForTableColumnWidth w = new PreferencesForTableColumnWidth();
                options.loadFromAndApplyTo(w, column.asColumn(), prefs);

                if (getStaticColumns().contains(column.asColumn())) { //static column
                    PreferencesForTableColumnOrder o = new PreferencesForTableColumnOrder();
                    //o.modelIndex == column.tableModel.modelIndex
                    if (!options.loadFromAndApplyTo(o, this, prefs)) {
                        pendingOrders.add(o);
                    }
                }
            } finally {
                options.end(column, parentPrefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
            }
        }

        @Override
        public void setSettingsWindow(SettingsWindow settingsWindow) {
            this.settingsWindow = settingsWindow;
            setColumns(SettingsWindowClient.class, c -> c.setSettingsWindow(settingsWindow));
        }

        private <T> void setColumns(Class<T> type, Consumer<T> setter) {
            columns.stream()
                    .filter(type::isInstance)
                    .map(type::cast)
                    .forEach(setter);
        }

        @Override
        public SettingsWindow getSettingsWindow() {
            return settingsWindow;
        }

        @Override
        public void setPreferencesUpdater(Consumer<PreferencesUpdateEvent> updater) {
            prefsUpdater = updater;
            setColumns(PreferencesUpdateSupport.class, c -> c.setPreferencesUpdater(updater));
        }

        public void loadSwingPreferences(GuiPreferences prefs) {
            loadSwingPreferences(prefs, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
        }

        /**
         * @param prefs the preferences
         * @param options the prefs processor
         * @since 1.7
         */
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            try {
                options.begin(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
                if (options.isSavingAsCurrentPreferencesInColumns()) {
                    currentPreferences = prefs;
                }
                setColumns(ObjectTableColumnWithContext.class, c -> c.loadSwingPreferences(prefs, options));

                options.loadFrom(nonContextWidth, prefs);
                options.loadFrom(nonContextOrder, prefs);
                for (ObjectTableColumn c : getColumns()) {
                    if (!(c instanceof ObjectTableColumnWithContext)) {
                        applyPrefsToNonContext(c, options);
                    } else {
                        loadPrefsTo(prefs, (ObjectTableColumnWithContext) c, options);
                    }
                }
            } finally {
                options.end(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
            }
        }

        public void saveSwingPreferences(GuiPreferences prefs) {
            setColumns(ObjectTableColumnWithContext.class, c -> c.saveSwingPreferences(prefs));

            for (ObjectTableColumn c : getColumns()) {
                PreferencesForTableColumnWidth w = new PreferencesForTableColumnWidth(c.getTableColumn().getWidth());
                int mi = c.getTableColumn().getModelIndex();
                int vi = convertColumnModelToView(mi);
                PreferencesForTableColumnOrder o = null;
                if (mi != vi) {
                    o = new PreferencesForTableColumnOrder(mi, vi);
                }
                if (!(c instanceof ObjectTableColumnWithContext)) {
                    nonContextWidth.put(mi, w);
                    if (o != null) {
                        nonContextOrder.put(mi, o);
                    }
                } else {
                    GuiPreferences subPref = prefs.getDescendant(((ObjectTableColumnWithContext) c).getContext());
                    w.saveTo(subPref);
                    if (o != null) {
                        o.saveTo(subPref);
                    }
                }
            }
            nonContextWidth.saveTo(prefs);
            nonContextOrder.saveTo(prefs);
        }

        @Override
        public void addColumnRowIndex() {
            addColumnStatic(new ObjectTableColumnRowIndexWithActions());
        }
    }

    /**
     * preferences for ordering of non-context table columns, attached to a list
     * <pre>
     *    "$columnOrder": { "0": { "modelIndex":m, "viewIndex":i},... }
     * </pre>
     */
    public static class PreferencesForTableColumnOrderStatic implements GuiSwingPrefsSupports.PreferencesByJsonEntry {
        protected Map<Integer, PreferencesForTableColumnOrder> modelIndexToOrder = new LinkedHashMap<>();

        public PreferencesForTableColumnOrderStatic() {}

        /**
         * @return the direct reference to the map (LinkedHashMap)
         * @since 1.7
         */
        public Map<Integer, PreferencesForTableColumnOrder> getModelIndexToOrderDirect() {
            return modelIndexToOrder;
        }

        @Override
        public String getKey() {
            return "$columnOrder";
        }

        public void put(int modelIndex, PreferencesForTableColumnOrder w) {
            modelIndexToOrder.put(modelIndex, w);
        }

        public boolean applyTo(ObjectTableModelColumns columns, int modelIndex) {
            PreferencesForTableColumnOrder o = modelIndexToOrder.get(modelIndex);
            if (o != null) {
                return o.applyTo(columns);
            } else {
                return true;
            }
        }

        public PreferencesForTableColumnOrder get(int modelIndex) {
            return modelIndexToOrder.get(modelIndex);
        }

        @Override
        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            modelIndexToOrder.forEach((k,v) ->
                map.put(Integer.toString(k), v.toJson()));
            return map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?,?>) {
                Map<String,Object> map = (Map<String,Object>) json;
                modelIndexToOrder.clear();
                map.forEach((k,v) -> {
                    try {
                        int n = Integer.parseInt(k);
                        PreferencesForTableColumnOrder w = new PreferencesForTableColumnOrder();
                        w.setJson(v);
                        modelIndexToOrder.put(n, w);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }

    }

    /**
     * an ordering index of a column
     * <pre>
     *     "$columnOrder": { "modelIndex":m, "viewIndex":v}
     * </pre>
     */
    public static class PreferencesForTableColumnOrder implements GuiSwingPrefsSupports.PreferencesByJsonEntry {
        protected int modelIndex;
        protected int viewIndex;

        public PreferencesForTableColumnOrder() {
            this(-1, -1);
        }

        public PreferencesForTableColumnOrder(int modelIndex, int viewIndex) {
            this.modelIndex = modelIndex;
            this.viewIndex = viewIndex;
        }

        /**
         * @return the property value
         * @since 1.7
         */
        public int getModelIndex() {
            return modelIndex;
        }

        /**
         * @param modelIndex the new property value
         * @since 1.7
         */
        public void setModelIndex(int modelIndex) {
            this.modelIndex = modelIndex;
        }

        /**
         * @return the property value
         * @since 1.7
         */
        public int getViewIndex() {
            return viewIndex;
        }

        /**
         * @param viewIndex the new property value
         * @since 1.7
         */
        public void setViewIndex(int viewIndex) {
            this.viewIndex = viewIndex;
        }

        /**
         *
         * @param columns the columns containing the target
         * @return true if successfully moved. while adding columns,
         *           the target index might beyond bounds.
         */
        public boolean applyTo(ObjectTableModelColumns columns) {
            if (0 <= modelIndex && modelIndex < columns.getColumnCount() &&
                    0 <= viewIndex && viewIndex < columns.getColumnCount()) {
                ObjectTableColumn column = columns.getColumnAt(modelIndex);
                columns.moveColumn(column, viewIndex);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getKey() {
            return "$columnOrder";
        }

        @Override
        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("modelIndex", modelIndex);
            map.put("viewIndex", viewIndex);
            return map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?,?> map) {
                modelIndex = GuiSwingPrefsSupports.getAs(map, Integer.class, "modelIndex", -1);
                viewIndex = GuiSwingPrefsSupports.getAs(map, Integer.class, "viewIndex", -1);
            }
        }
    }

    /**
     * preferences for width of non-context table columns, attached to a list
     * <pre>
     *     "$columnWidth": {"0":{ "width":w },... }
     * </pre>
     */
    public static class PreferencesForTableColumnWidthStatic implements GuiSwingPrefsSupports.PreferencesByJsonEntry {
        protected Map<Integer, PreferencesForTableColumnWidth> modelIndexToWidth = new LinkedHashMap<>();
        public PreferencesForTableColumnWidthStatic() {}
        @Override
        public String getKey() {
            return "$columnWidth";
        }

        /**
         * @return direct reference to the map (actually a LinkedHashMap)
         * @since 1.7
         */
        public Map<Integer, PreferencesForTableColumnWidth> getModelIndexToWidthDirect() {
            return modelIndexToWidth;
        }

        public void put(int modelIndex, PreferencesForTableColumnWidth w) {
            modelIndexToWidth.put(modelIndex, w);
        }

        public void applyTo(ObjectTableColumn column) {
            PreferencesForTableColumnWidth w = modelIndexToWidth.get(column.getTableColumn().getModelIndex());
            if (w != null) {
                w.applyTo(column);
            }
        }

        @Override
        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            modelIndexToWidth.forEach((k,v) ->
                map.put(Integer.toString(k), v.toJson()));
            return map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?,?>) {
                Map<String,Object> map = (Map<String,Object>) json;
                modelIndexToWidth.clear();
                map.forEach((k,v) -> {
                    try {
                        int n = Integer.parseInt(k);
                        PreferencesForTableColumnWidth w = new PreferencesForTableColumnWidth();
                        w.setJson(v);
                        modelIndexToWidth.put(n, w);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }
    }

    /**
     * a width info. of a column
     * <pre>
     *     "$columnWidth": {"width":w}
     * </pre>
     */
    public static class PreferencesForTableColumnWidth implements GuiSwingPrefsSupports.PreferencesByJsonEntry {
        protected int width;

        public PreferencesForTableColumnWidth() {
            this(-1);
        }

        public PreferencesForTableColumnWidth(int width) {
            this.width = width;
        }

        public void applyTo(ObjectTableColumn column) {
            if (width > 0) {
                column.getTableColumn().setPreferredWidth(width);
            }
        }

        /**
         * @return the property value
         * @since 1.7
         */
        public int getWidth() {
            return width;
        }

        /**
         * @param width update the property value
         * @since 1.7
         */
        public void setWidth(int width) {
            this.width = width;
        }

        @Override
        public String getKey() {
            return "$columnWidth";
        }

        @Override
        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("width", width);
            return map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?,?> map) {
                width = GuiSwingPrefsSupports.getAs(map, Integer.class, "width", -1);
            }
        }
    }

    /**
     * a row-index column with an actions including toString and JSON
     */
    public static class ObjectTableColumnRowIndexWithActions
            extends ObjectTableColumn.ObjectTableColumnRowIndex implements ObjectTableColumn.PopupMenuBuilderSource {
        public ObjectTableColumnRowIndexWithActions() {
            withComparator(new GuiSwingTableColumnNumber.NumberComparator());
        }

        @Override
        public List<TableMenuComposite> getCompositesForRows() {
            int index = getTableColumn().getModelIndex();
            return Arrays.asList(
                    new ToStringCopyCell.TableMenuCompositeToStringCopy(index),
                    new ToStringCopyCell.TableMenuCompositeToStringPaste(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonCopy(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonPaste(index));
        }

        @Override
        public List<TableMenuComposite> getCompositesForCells() {
            int index = getTableColumn().getModelIndex();
            return Arrays.asList(
                    new ToStringCopyCell.TableMenuCompositeToStringCopy(index),
                    new ToStringCopyCell.TableMenuCompositeToStringPaste(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonCopy(index),
                    new GuiSwingJsonTransfer.TableMenuCompositeJsonPaste(index));
        }

        @Override
        public PopupMenuBuilderSource getMenuBuilderSource() {
            return this;
        }

        @Override
        public PopupExtension.PopupMenuBuilder getMenuBuilder(JTable table) {
            return new ObjectTableColumnValue.ObjectTableColumnActionBuilder(table, this,
                    new PopupCategorized(() -> Collections.singletonList(new NumberCopyAction()), null,
                            new ObjectTableModel.MenuBuilderWithEmptySeparator()));
        }
    }

    /**
     * an action for copying an index number to the clip-board
     */
    public static class NumberCopyAction extends PopupExtensionText.TextCopyAllAction
            implements TableTargetColumnAction {
        @Serial private static final long serialVersionUID = 1L;

        public NumberCopyAction() {
            super(null);
            putValue(NAME, "Copy Indices");
            putValue(ACCELERATOR_KEY, null);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            actionPerformedOnTable(e, target.getSelectedCells().stream()
                    .map(GuiReprCollectionTable.CellValue::getValue)
                    .collect(Collectors.toList()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (src instanceof JLabel) {
                String text = ((JLabel) src).getText();
                copy(text);
            }
        }
    }
}
