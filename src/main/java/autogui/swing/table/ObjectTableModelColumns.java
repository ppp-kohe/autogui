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
    protected List<DynamicColumnContainer> dynamicColumns = new ArrayList<>();
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

    protected void columnAdded(ObjectTableColumn column, DynamicColumnContainer d) {
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
    public void addColumnDynamic(GuiSwingTableColumnDynamic.DynamicColumnFactory column) {
        dynamicColumns.add(new DynamicColumnContainer(this, column));
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

    public List<DynamicColumnContainer> getDynamicColumns() {
        return dynamicColumns;
    }

    public boolean hasDynamicColumns() {
        return !dynamicColumns.isEmpty();
    }

    public void update(Object list) {
        int startIndex = staticColumns.size();
        for (DynamicColumnContainer d : dynamicColumns) {
            startIndex = d.update(startIndex, list);
        }
    }

    public void addColumnDynamic(DynamicColumnContainer d, ObjectTableColumn c) {
        this.columns.add(c.getTableColumn().getModelIndex(), c);
        columnAdded(c, d);
        int idx = columnModel.getColumnCount();
        columnModel.addColumn(c.getTableColumn());
        int newIndex = c.getTableColumn().getModelIndex();
        if (idx != newIndex) {
            columnModel.moveColumn(idx, newIndex);
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

    public static class DynamicColumnContainer {
        protected ObjectTableModelColumns columns;
        protected GuiSwingTableColumnDynamic.DynamicColumnFactory factory;

        protected List<ObjectTableColumn> columnsInSize;
        protected List<DynamicColumnContainer> children;

        protected int lastIndex;

        protected int indexInSize;

        public DynamicColumnContainer(ObjectTableModelColumns columns, GuiSwingTableColumnDynamic.DynamicColumnFactory factory) {
            this.factory = factory;
            this.columns = columns;
        }

        public DynamicColumnContainer(ObjectTableModelColumns columns, int lastIndex) {
            this.lastIndex = lastIndex;
            this.columns = columns;
        }

        public GuiSwingTableColumnDynamic.DynamicColumnFactory getFactory() {
            return factory;
        }

        public int update(int startIndex, Object list) {
            lastIndex = startIndex;
            GuiSwingTableColumnDynamic.ObjectTableColumnSize newSize = factory.getColumnSize(list);
            newSize.create(this);
            return lastIndex;
        }

        public void add(ObjectTableColumn column) {
            column.getTableColumn().setModelIndex(lastIndex);
            if (columnsInSize == null) {
                columnsInSize = new ArrayList<>();
            }
            columns.addColumnDynamic(this, column);
            ++lastIndex;
        }

        public void removeColumnsFromEnd(int removingSize) {
            if (removingSize > 0) {
                List<ObjectTableColumn> cs = getColumnsInSize();
                int exSize = cs.size();
                columns.removeColumnsDynamic(cs.subList(exSize - removingSize, exSize));
            }
        }

        public int getLastIndexAfterUpdate() {
            if (columnsInSize instanceof ArrayList<?>) { //the method will be called after creation
                ((ArrayList<ObjectTableColumn>) columnsInSize).trimToSize();
            }
            return getLastIndex();
        }

        public int getLastIndex() {
            return lastIndex;
        }

        public void setLastIndex(int lastIndex) {
            this.lastIndex = lastIndex;
        }

        public DynamicColumnContainer getChild(int i) {
            if (children == null) {
                children = new ArrayList<>();
            }
            while (i >= children.size()) {
                children.add(null);
            }
            DynamicColumnContainer c = children.get(i);
            if (c == null) {
                c = new DynamicColumnContainer(columns, lastIndex);
                children.set(i, c);
            }
            return c;
        }

        public int getChildSize() {
            if (children == null) {
                return 0;
            } else {
                return children.size();
            }
        }

        public void removeChildrenFromEnd(int removingSize) {
            if (children != null) {
                int s = children.size();
                for (int i = s - removingSize; i < s; ++i) {
                    DynamicColumnContainer c = children.get(i);
                    if (c != null) {
                        c.removeColumnsFromEnd(c.getColumnsInSize().size());
                        c.removeChildrenFromEnd(c.getChildSize());
                    }
                }
                for (int i = 0 ; i < removingSize; ++i) {
                    children.remove(s - i);
                }
            }
        }

        public List<ObjectTableColumn> getColumnsInSize() {
            if (columnsInSize == null) {
                return Collections.emptyList();
            } else {
                return columnsInSize;
            }
        }


        public void moveExistingColumns() {
            if (columnsInSize != null) {
                for (ObjectTableColumn column : columnsInSize) {
                    if (column.getTableColumn().getModelIndex() != lastIndex) {
                        column.getTableColumn().setModelIndex(lastIndex);
                    }
                    ++lastIndex;
                }
            }
        }

        public void viewUpdate(ObjectTableColumn source) {
            getColumnsInSize().stream()
                    .filter(c -> c != source)
                    .findFirst()
                    .ifPresent(c -> c.viewUpdateAsDynamic(source));
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
