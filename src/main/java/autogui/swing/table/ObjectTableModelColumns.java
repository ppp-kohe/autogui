package autogui.swing.table;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** column managing part of {@link ObjectTableModel} */
public class ObjectTableModelColumns implements GuiSwingTableColumnSet.TableColumnHost {
    protected DefaultTableColumnModel columnModel;
    protected List<ObjectTableColumn> columns = new ArrayList<>();
    protected List<ObjectTableColumn> staticColumns = new ArrayList<>();
    protected List<ObjectTableColumnDynamic> dynamicColumns = new ArrayList<>();

    public ObjectTableModelColumns() {
        columnModel = new DefaultTableColumnModel();

        //TODO debug
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
        });
    }

    public DefaultTableColumnModel getColumnModel() {
        return columnModel;
    }

    @Override
    public void addColumnStatic(ObjectTableColumn column) {
        int modelIndex = columns.size();
        columns.add(column);
        staticColumns.add(column);

        TableColumn tableColumn = column.getTableColumn();
        tableColumn.setModelIndex(modelIndex);
        columnModel.addColumn(tableColumn);
    }

    @Override
    public void addColumnDynamic(ObjectTableColumnDynamicFactory column) {
        dynamicColumns.add(new ObjectTableColumnDynamic(column, staticColumns.size()));
    }

    @Override
    public void addColumnRowIndex() {
        addColumnStatic(new ObjectTableColumn.ObjectTableColumnRowIndex());
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
                addColumnsDynamic(d.getChangingColumns());
            } else if (d.hasRemoving()) {
                removeColumnsDynamic(d.getChangingColumns());
            }
            d.clearChanges();
        }
    }

    public void addColumnsDynamic(List<ObjectTableColumn> columns) {
        for (ObjectTableColumn c : columns) {
            this.columns.add(c.getTableColumn().getModelIndex(), c);
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

    public static class ObjectTableColumnDynamic {
        protected ObjectTableColumnDynamicFactory factory;

        protected int startIndex;
        protected ObjectTableColumnIndex index;
        protected List<ObjectTableColumn> columns;

        protected boolean changeTypeIsAdding;
        protected List<ObjectTableColumn> changingColumns;

        public ObjectTableColumnDynamic(ObjectTableColumnDynamicFactory factory, int startIndex) {
            this.factory = factory;
            index = new ObjectTableColumnIndex(null, startIndex, 0);
            columns = new ArrayList<>();
            changingColumns = new ArrayList<>();
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
