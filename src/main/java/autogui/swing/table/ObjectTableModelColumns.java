package autogui.swing.table;

import autogui.base.mapping.GuiPreferences;
import autogui.swing.GuiSwingPreferences;
import autogui.swing.GuiSwingView;
import autogui.swing.util.SettingsWindow;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.text.Collator;
import java.util.*;
import java.util.function.Consumer;

/** column managing part of {@link ObjectTableModel} */
public class ObjectTableModelColumns
        implements GuiSwingTableColumnSet.TableColumnHost, TableColumnModelListener,
                GuiSwingView.SettingsWindowClient, GuiSwingPreferences.PreferencesUpdateSupport {
    protected DefaultTableColumnModel columnModel;
    protected List<ObjectTableColumn> columns = new ArrayList<>();
    protected List<ObjectTableColumn> staticColumns = new ArrayList<>();
    protected List<ObjectTableColumnDynamic> dynamicColumns = new ArrayList<>();
    protected List<ObjectTableColumn.TableMenuComposite> menuRowComposites = new ArrayList<>();

    protected Map<Integer,Integer> modelToView = new HashMap<>();

    protected ObjectTableModelColumnsListener updater;
    protected int viewUpdating;

    protected SettingsWindow settingsWindow;
    protected Consumer<GuiSwingPreferences.PreferencesUpdateEvent> prefsUpdater;
    protected GuiPreferences currentPreferences;

    public interface ObjectTableModelColumnsListener {
        void columnAdded(ObjectTableColumn column);
        void columnViewUpdate(ObjectTableColumn column);
    }

    public ObjectTableModelColumns(ObjectTableModelColumnsListener updater) {
        this.updater = updater;
        columnModel = new DefaultTableColumnModel();

        /*
        //debug
        columnModel.addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnAdded(TableColumnModelEvent e) {
                System.err.println("columnAdded " + e.getToIndex());
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
                System.err.println("columnAdded " + e.getFromIndex());
            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {
                System.err.println("columnAdded " + e.getFromIndex() + "->" + e.getToIndex());
            }

            @Override
            public void columnMarginChanged(ChangeEvent e) {
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {
            }
        });*/
    }

    public DefaultTableColumnModel getColumnModel() {
        return columnModel;
    }

    @Override
    public void addColumnStatic(ObjectTableColumn column) {
        int modelIndex = columns.size();
        columns.add(column);
        columnAdded(column, null);
        staticColumns.add(column);

        TableColumn tableColumn = column.getTableColumn();
        tableColumn.setModelIndex(modelIndex);
        columnModel.addColumn(tableColumn);
    }

    protected void columnAdded(ObjectTableColumn column, ObjectTableColumnDynamic d) {
        column.setColumnViewUpdater(e -> {
            if (viewUpdating <= 0) {
                ++viewUpdating;
                if (d != null) {
                    d.viewUpdate(e);
                }
            } else {
                --viewUpdating;
            }
            if (updater != null) {
                updater.columnViewUpdate(e);
            }
        });
        updater.columnAdded(column);
        if (settingsWindow != null) {
            column.setSettingsWindow(settingsWindow);
        }
        if (prefsUpdater != null) {
            column.setPreferencesUpdater(prefsUpdater);
        }
        if (currentPreferences != null) {
            column.loadSwingPreferences(currentPreferences);
        }
    }

    @Override
    public void setSettingsWindow(SettingsWindow settingsWindow) {
        this.settingsWindow = settingsWindow;
        columns.forEach(c -> c.setSettingsWindow(settingsWindow));
    }

    @Override
    public SettingsWindow getSettingsWindow() {
        return settingsWindow;
    }

    @Override
    public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
        prefsUpdater = updater;
        columns.forEach(c -> c.setPreferencesUpdater(updater));
    }

    public void loadSwingPreferences(GuiPreferences prefs) {
        currentPreferences = prefs;
        columns.forEach(c -> c.loadSwingPreferences(prefs));
    }

    public void saveSwingPreferences(GuiPreferences prefs) {
        columns.forEach(c -> c.saveSwingPreferences(prefs));
    }

    @Override
    public void addColumnDynamic(DynamicColumnFactory column) {
        dynamicColumns.add(new ObjectTableColumnDynamic(column, staticColumns.size()));
    }

    @Override
    public void addColumnRowIndex() {
        addColumnStatic(new ObjectTableColumn.ObjectTableColumnRowIndex());
    }

    @Override
    public void addMenuRowComposite(ObjectTableColumn.TableMenuComposite rowComposite) {
        menuRowComposites.add(rowComposite);
    }

    public List<ObjectTableColumn.TableMenuComposite> getMenuRowComposites() {
        return menuRowComposites;
    }

    public int getColumnCount() {
        return columns.size();
    }

    public ObjectTableColumn getColumnAt(int index) {
        return columns.get(index);
    }

    public List<ObjectTableColumn> getColumns() {
        return columns;
    }

    public List<ObjectTableColumn> getStaticColumns() {
        return staticColumns;
    }

    public List<ObjectTableColumnDynamic> getDynamicColumns() {
        return dynamicColumns;
    }

    public boolean hasDynamicColumns() {
        return !dynamicColumns.isEmpty();
    }

    public void update(Object list) {
        int startIndex = staticColumns.size();
        for (ObjectTableColumnDynamic d : dynamicColumns) {
            startIndex = d.update(startIndex, list);
            if (d.hasAdding()) {
                addColumnsDynamic(d, d.getChangingColumns());
            } else if (d.hasRemoving()) {
                removeColumnsDynamic(d.getChangingColumns());
            }
            d.clearChanges();
        }
    }

    public void addColumnsDynamic(ObjectTableColumnDynamic d, List<ObjectTableColumn> columns) {
        for (ObjectTableColumn c : columns) {
            this.columns.add(c.getTableColumn().getModelIndex(), c);
            columnAdded(c, d);
            int idx = columnModel.getColumnCount();
            columnModel.addColumn(c.getTableColumn());
            int newIndex = c.getTableColumn().getModelIndex();
            if (idx != newIndex) {
                columnModel.moveColumn(idx, newIndex);
            }
        }
    }

    public void removeColumnsDynamic(List<ObjectTableColumn> columns) {
        this.columns.removeAll(columns);
        columns.forEach(c ->
                columnModel.removeColumn(c.getTableColumn()));
        columns.forEach(ObjectTableColumn::shutdown);
    }


    public int getTotalWidth() {
        return columns.stream()
                .mapToInt(e -> e.getTableColumn().getWidth())
                .sum();
    }

    public int getRowHeight() {
        return columns.stream()
                .mapToInt(ObjectTableColumn::getRowHeight)
                .max().orElse(0);
    }

    @Override
    public void columnAdded(TableColumnModelEvent e) {
        modelToView.put(columnModel.getColumn(e.getToIndex()).getModelIndex(), e.getToIndex());
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) { }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        modelToView.put(columnModel.getColumn(e.getToIndex()).getModelIndex(), e.getToIndex());
    }

    public int convertColumnModelToView(int modelIndex) {
        Integer n = modelToView.get(modelIndex);
        if (n == null || n >= columnModel.getColumnCount() || columnModel.getColumn(n).getModelIndex() != n) {
            int i = 0;
            n = -1;
            for (Enumeration<TableColumn> iter = columnModel.getColumns(); iter.hasMoreElements();) {
                TableColumn next = iter.nextElement();
                if (next.getModelIndex() == modelIndex) {
                    n = i;
                    break;
                }
                ++i;
            }
            modelToView.put(modelIndex, n);
        }
        return n;
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) { }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) { }

    public void shutdown() {
        getColumns().forEach(ObjectTableColumn::shutdown);
    }

    public static class ObjectTableColumnDynamic {
        protected DynamicColumnFactory factory;

        protected int startIndex;
        protected ObjectTableColumnIndex index;
        protected List<ObjectTableColumn> columns;

        protected boolean changeTypeIsAdding;
        protected List<ObjectTableColumn> changingColumns;

        public ObjectTableColumnDynamic(DynamicColumnFactory factory, int startIndex) {
            this.factory = factory;
            index = new ObjectTableColumnIndex(null, startIndex, 0);
            columns = new ArrayList<>();
            changingColumns = new ArrayList<>();
        }

        public DynamicColumnFactory getFactory() {
            return factory;
        }

        public int getEndIndexExclusive() {
            return index.getTotalIndex();
        }

        public int update(int startIndex, Object list) {
            int newSize = factory.getColumnCount(list);
            int preSize = columns.size();

            updateStartIndex(startIndex, newSize);

            if (preSize < newSize) { //adding
                updateWithAdding(newSize - preSize);
            } else if (preSize > newSize) { //removing
                updateWithRemoving(preSize - newSize);
            } else {
                changingColumns.clear();
            }
            return getEndIndexExclusive();
        }

        public void updateStartIndex(int startIndex, int newSize) {
            if (this.startIndex != startIndex) {
                int modelIndex = startIndex;
                for (int i = 0, l = Math.min(newSize, columns.size()); i < l; ++i) {
                    columns.get(i).getTableColumn().setModelIndex(modelIndex);
                    ++modelIndex;
                }
                this.startIndex = startIndex;
            }
        }

        public void updateWithAdding(int addingCount) {
            changeTypeIsAdding = true;
            int preSize = columns.size();
            changingColumns.clear();
            index = new ObjectTableColumnIndex(null, startIndex + preSize, preSize);
            for (int i = 0; i < addingCount; ++i) {
                ObjectTableColumn column = factory.createColumn(index);
                column.getTableColumn().setModelIndex(index.getTotalIndex());
                columns.add(column);
                changingColumns.add(column);
                index.increment(1);
            }
        }

        public void updateWithRemoving(int removingCount) {
            changeTypeIsAdding = false;
            int newSize = columns.size() - removingCount;
            index = new ObjectTableColumnIndex(null, startIndex + newSize, newSize);
            changingColumns.clear();
            changingColumns.addAll(columns.subList(newSize, columns.size()));
            columns.removeAll(changingColumns);
        }

        public boolean hasAdding() {
            return !changingColumns.isEmpty() && changeTypeIsAdding;
        }

        public boolean hasRemoving() {
            return !changingColumns.isEmpty() && !changeTypeIsAdding;
        }

        public List<ObjectTableColumn> getChangingColumns() {
            return changingColumns;
        }

        public void clearChanges() {
            changingColumns.clear();
        }

        public void viewUpdate(ObjectTableColumn source) {
            columns.stream()
                    .filter(c -> c != source)
                    .forEach(c -> c.viewUpdateAsDynamic(source));
        }
    }

    public static class TableRowSorterDynamic extends TableRowSorter<ObjectTableModel> {
        public TableRowSorterDynamic(ObjectTableModel model) {
            super(model);
            setSortsOnUpdates(true);
        }

        @Override
        public Comparator<?> getComparator(int column) {
            Comparator<?> c = getModel().getColumns().getColumnAt(column).getComparator();
            if (c == null) {
                return Collator.getInstance();
            } else {
                return c;
            }
        }

        @Override
        public void setComparator(int column, Comparator<?> comparator) {
            //nothing
        }

        @Override
        protected boolean useToString(int column) {
            if (getComparator(column) != null) {
                return false;
            }
            Class<?> cls = getModel().getColumns().getColumnAt(column).getValueType();
            if (cls != null) {
                return cls.equals(String.class) || (!Comparable.class.isAssignableFrom(cls));
            } else {
                return true;
            }
        }
    }
}
