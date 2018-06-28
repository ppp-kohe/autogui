package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiMappingContext.GuiSourceValue;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.base.mapping.GuiReprValue.ObjectSpecifier;
import autogui.swing.GuiSwingJsonTransfer;
import autogui.swing.GuiSwingPreferences;
import autogui.swing.GuiSwingPreferences.PreferencesUpdateEvent;
import autogui.swing.GuiSwingPreferences.PreferencesUpdateSupport;
import autogui.swing.GuiSwingView.SettingsWindowClient;
import autogui.swing.GuiSwingViewCollectionTable.CollectionTable;
import autogui.swing.table.GuiSwingTableColumn.ObjectTableColumnWithContext;
import autogui.swing.table.GuiSwingTableColumn.SpecifierManagerIndex;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionText;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * a table-model with {@link GuiMappingContext}
 */
public class GuiSwingTableModelCollection extends ObjectTableModel {

    protected GuiMappingContext context;
    protected GuiMappingContext elementContext;
    protected Supplier<ObjectSpecifier> tableSpecifier;
    protected SpecifierManagerIndex rowSpecifierManager;

    public GuiSwingTableModelCollection(GuiMappingContext context, Supplier<ObjectSpecifier> tableSpecifier,
                                      Supplier<Object> source) {
        this.context = context;
        this.tableSpecifier = tableSpecifier;
        this.rowSpecifierManager = new SpecifierManagerIndex(tableSpecifier);
        setElementContextFromContext();
        setSource(source);
    }

    public GuiMappingContext getContext() {
        return context;
    }

    @Override
    public void initColumns() {
        columns = new GuiSwingTableModelColumns(this);
    }

    public GuiSwingTableModelColumns getColumnsWithContext() {
        return (GuiSwingTableModelColumns) columns;
    }

    protected void setElementContextFromContext() {
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
            PreferencesForTableColumnWidth w = new PreferencesForTableColumnWidth();
            w.loadFrom(column.getContext().getPreferences());
            w.applyTo(column.asColumn());

            if (d == null) { //non-dynamic column: the ordering might be incorrect due to the lack of dynamic column re-ordering
                PreferencesForTableColumnOrder o = new PreferencesForTableColumnOrder();
                o.loadFrom(column.getContext().getPreferences());
                //o.modelIndex == column.tableModel.modelIndex
                if (!o.applyTo(this)) {
                    pendingOrders.add(o);
                }
            }
        }

        public void applyPrefsToNonContext(ObjectTableColumn column) {
            nonContextWidth.applyTo(column);
            if (!nonContextOrder.applyTo(this, column.getTableColumn().getModelIndex())) {
                PreferencesForTableColumnOrder o = nonContextOrder.get(column.getTableColumn().getModelIndex());
                if (o != null) {
                    pendingOrders.add(o);
                }
            }
        }

        public void loadPrefsTo(GuiPreferences parentPrefs, ObjectTableColumnWithContext column) {
            GuiPreferences prefs = parentPrefs.getDescendant(column.getContext());
            PreferencesForTableColumnWidth w = new PreferencesForTableColumnWidth();
            w.loadFrom(prefs);
            w.applyTo(column.asColumn());

            if (getStaticColumns().contains(column.asColumn())) { //static column
                PreferencesForTableColumnOrder o = new PreferencesForTableColumnOrder();
                o.loadFrom(prefs);
                //o.modelIndex == column.tableModel.modelIndex
                if (!o.applyTo(this)) {
                    pendingOrders.add(o);
                }
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
            currentPreferences = prefs;
            setColumns(ObjectTableColumnWithContext.class, c -> c.loadSwingPreferences(prefs));

            nonContextWidth.loadFrom(prefs);
            nonContextOrder.loadFrom(prefs);
            for (ObjectTableColumn c : getColumns()) {
                if (!(c instanceof ObjectTableColumnWithContext)) {
                    applyPrefsToNonContext(c);
                } else {
                    loadPrefsTo(prefs, (ObjectTableColumnWithContext) c);
                }
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

    public static class PreferencesForTableColumnOrderStatic implements GuiSwingPreferences.PreferencesByJsonEntry {
        protected Map<Integer, PreferencesForTableColumnOrder> modelIndexToOrder = new LinkedHashMap<>();

        @Override
        public String getKey() {
            return "$columnWidth";
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
            modelIndexToOrder.forEach((k,v) -> {
                map.put(Integer.toString(k), v.toJson());
            });
            return map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?,?>) {
                Map<String,Object> map = (Map<String,Object>) json;
                map.forEach((k,v) -> {
                    try {
                        int n = Integer.valueOf(k);
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

    public static class PreferencesForTableColumnOrder implements GuiSwingPreferences.PreferencesByJsonEntry {
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
            if (json instanceof Map<?,?>) {
                Map<String,Object> map = (Map<String,Object>) json;
                modelIndex = (Integer) map.getOrDefault("modelIndex", -1);
                viewIndex = (Integer) map.getOrDefault("viewIndex", -1);
            }
        }
    }

    public static class PreferencesForTableColumnWidthStatic implements GuiSwingPreferences.PreferencesByJsonEntry {
        protected Map<Integer, PreferencesForTableColumnWidth> modelIndexToWidth = new LinkedHashMap<>();

        @Override
        public String getKey() {
            return "$columnWidth";
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
            modelIndexToWidth.forEach((k,v) -> {
                map.put(Integer.toString(k), v.toJson());
            });
            return map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?,?>) {
                Map<String,Object> map = (Map<String,Object>) json;
                map.forEach((k,v) -> {
                    try {
                        int n = Integer.valueOf(k);
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

    public static class PreferencesForTableColumnWidth implements GuiSwingPreferences.PreferencesByJsonEntry {
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
            if (json instanceof Map<?,?>) {
                Map<String,Object> map = (Map<String,Object>) json;
                width = (Integer) map.getOrDefault("width", -1);
            }
        }
    }


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
        public Consumer<Object> getMenuTargetPane() {
            return null;
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
