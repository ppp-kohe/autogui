package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable.TableTargetCell;
import autogui.swing.table.GuiSwingTableColumn.SpecifierManagerIndex;
import autogui.swing.table.ObjectTableColumn.TableMenuComposite;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** column managing part of {@link ObjectTableModel} */
public class ObjectTableModelColumns
        implements GuiSwingTableColumnSet.TableColumnHost, TableColumnModelListener {
    protected DefaultTableColumnModel columnModel;
    protected List<ObjectTableColumn> columns = new ArrayList<>();
    protected List<ObjectTableColumn> staticColumns = new ArrayList<>();
    protected List<DynamicColumnContainer> dynamicColumns = new ArrayList<>();
    protected List<TableMenuComposite> menuRowComposites = new ArrayList<>();

    protected Map<Integer,Integer> modelToView = new HashMap<>();

    protected ObjectTableModelColumnsListener updater;
    protected int viewUpdating;

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
    }

    @Override
    public void addColumnDynamic(DynamicColumnFactory column) {
        dynamicColumns.add(new DynamicColumnContainer(this, column));
    }

    @Override
    public void addColumnRowIndex() {
        addColumnStatic(new ObjectTableColumn.ObjectTableColumnRowIndex());
    }

    @Override
    public void addMenuRowComposite(TableMenuComposite rowComposite) {
        menuRowComposites.add(rowComposite);
    }

    public List<TableMenuComposite> getMenuRowComposites() {
        return menuRowComposites;
    }

    public int getColumnCount() {
        return columns.size();
    }

    @Override
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

    public boolean isNonEmpty() {
        return !dynamicColumns.isEmpty();
    }

    public DynamicColumnContainer getRootContainer() {
        return dynamicColumns.isEmpty() ? null :
                dynamicColumns.get(0);
    }

    public List<Action> getDynamicColumnsActions(TableTargetCell selection) {
        return dynamicColumns.stream()
                .flatMap(d -> d.getFactory().getActions(selection).stream())
                .collect(Collectors.toList());
    }

    public void update(Object list) {
        int startIndex = staticColumns.size();
        for (DynamicColumnContainer d : dynamicColumns) {
            startIndex = d.update(startIndex, list);
        }
    }

    public boolean isStaticColumns() {
        return dynamicColumns.stream()
                .allMatch(DynamicColumnContainer::isStaticColumns);
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
        protected DynamicColumnFactory factory;

        protected List<ObjectTableColumn> columnsInSize;
        protected List<DynamicColumnContainer> children;

        protected int lastIndex;

        public DynamicColumnContainer(ObjectTableModelColumns columns, DynamicColumnFactory factory) {
            this.factory = factory;
            this.columns = columns;
        }

        public DynamicColumnContainer(ObjectTableModelColumns columns, int lastIndex) {
            this.lastIndex = lastIndex;
            this.columns = columns;
        }

        public DynamicColumnFactory getFactory() {
            return factory;
        }

        public int update(int startIndex, Object list) {
            lastIndex = startIndex;
            ObjectTableColumnSize newSize = factory.getColumnSize(list);
            newSize.create(this); //event if staticColumns, it might need to shift modelIndex of existing columns
            System.err.println("--------------------");
            this.columns.getStaticColumns().forEach(e -> System.err.println("   static: " + e));
            factory.debugPrint(1);
            newSize.debugPrint(1);
            return lastIndex;
        }

        public boolean isStaticColumns() {
            return factory.isStaticColumns();
        }

        public void add(ObjectTableColumn column) {
            column.getTableColumn().setModelIndex(lastIndex);
            if (columnsInSize == null) {
                columnsInSize = new ArrayList<>();
            }
            columnsInSize.add(column);
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
            } else {
                c.setLastIndex(lastIndex);
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

    /**
     * a factory of size info. which becomes a set of factories of each concrete columns */
    public interface DynamicColumnFactory {
        ObjectTableColumnSize getColumnSize(Object c);

        Object getValueAsMember(Object parent);

        void setParentFactory(DynamicColumnFactory factory);

        DynamicColumnFactory getParentFactory();

        List<SpecifierManagerIndex> getIndexSpecifiers();

        SpecifierManagerIndex getIndex();

        Object getValue(Map<SpecifierManagerIndex, Integer> indexInjection);

        List<Action> getActions(TableTargetCell selection);

        boolean isStaticColumns();

        void debugPrint(int depth);

        String getDisplayName();
    }

    /**
     * a size information of hierarchical composition of sub-columns
     *
     *  <pre>
     *      Size ::= { Size, Size, ... }  //SizeComposite
     *             | Int                  //SizeConcrete
     *
     *      getColumnSize(List&lt;E&gt; l)  ::=  size(l.get(0)).set(size(l.get(1)).set(...)...
     *
     *      {a,b,c}.set({a',b',c',d}) ::= {a.set(a'),b.set(b'),c.set(c'),d}
     *      {a,b,c}.set({a',b'})      ::= {a.set(a'),b.set(b'),c}
     *            n.set(n')           ::= max(n,n')
     *
     *         size(List&lt;V&gt; l) where V is a value-type
     *                                ::= l.size()
     *         size(List&lt;E&gt; s)        ::= { size(s.get(0)), size(s.get(1)), ... }
     *         size(C c) and class C { T0 f0; T1 f1;...; }
     *                                ::= { size(c.f0), size(c.f1), ... }
     *         size(V v)              ::= 1
     *  </pre>
     *
     *  <pre>
     *      {a,b,c,...}.create(con) ::=
     *                      con.columns.forEach(c -> c.modelIndex = con.lastIndex++);
     *                      a.create(con.child(0)); con.lastIndex = con.child(0).lastIndex;
     *                      b.create(con.child(1)); con.lastIndex = con.child(1).lastIndex;
     *                      ...
     *                      con.columns.remove(con.lastIndex,...);
     *                n.create(con) ::=
     *                      con.columns.forEach(c -> c.modelIndex = con.lastIndex++);
     *                      diff = n - con.columns.size();
     *                      if diff < 0: con.columns.remove(n,n+1,...);
     *                      else       : for (...diff...) con.columns.add(new Column(con.lastIndex++));
     *
     *
     *  </pre>
     *
     *  <pre>
     *    //examples: getColumnSize
     *      List&lt;List&lt;Float&gt;&gt; l1 = asList(
     *                              asList(1.0),
     *                              asList(2.0,3.0),
     *                              asList(4.0,5.0,6.0));
     *      =&gt; 1.set(2).set(3) =&gt; 3
     *
     *      List&lt;List&lt;List&lt;Float&gt;&gt;&gt; l2 = asList(
     *                              asList(asList(1.0), asList(2.0)),
     *                              asList(asList(1.0), asList(2.0,3.0), asList(4.0,5.0)),
     *                              asList(asList(1.0), asList(2.0,3.0), asList(4.0,5.0,6.0)));
     *      =&gt; {1,1}.set({1,2,2}).set({1,2,3}) =&gt; {1,2,3}
     *
     *      class E { List&lt;Float&gt; l1, l2; } //E({l1_1,l1_2,...},{l2_1,l2_2,...})
     *      List&lt;E&gt; l3 = asList(
     *                E({1.0},         {2.0,3.0}),
     *                E({4.0,5.0,6.0}, {7.0,8.0,9.0,10.0});
     *
     *      =&gt; {1, 2}.set({3,4}) =&gt; {3,4}
     *
     *
     *      class F { int x, int y; } //F(x,y)
     *      List&lt;List&lt;F&gt;&gt; l4 = asList(
     *                      asList(F(1,2)),
     *                      asList(F(3,4), F(5,6)),
     *                      asList(F(7,8), F(9,10), F(11,12)));
     *      =&gt; {{1,1}}.set({{1,1},{1,1}}).set({{1,1},{1,1},{1,1}}) =&gt; {{1,1},{1,1},{1,1}}
     *  </pre>
     */
    public abstract static class ObjectTableColumnSize {
        protected int size;
        protected ObjectTableColumnSize parent;
        protected SpecifierManagerIndex elementSpecifierIndex;

        public int size() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public boolean isComposition() {
            return false;
        }

        public List<ObjectTableColumnSize> getChildren() {
            //error("getChildren", null);
            return Collections.emptyList();
        }

        public ObjectTableColumnSize getParent() {
            return parent;
        }

        public void setParent(ObjectTableColumnSize parent) {
            this.parent = parent;
        }

        public abstract void create(DynamicColumnContainer targetContainer);


        public void setElementSpecifierIndex(SpecifierManagerIndex elementSpecifierIndex) {
            this.elementSpecifierIndex = elementSpecifierIndex;
        }

        public SpecifierManagerIndex getElementSpecifierIndex() {
            return elementSpecifierIndex;
        }

        public Map<SpecifierManagerIndex, Integer> toIndexInjection(int index) {
            Map<SpecifierManagerIndex,Integer> is;
            if (parent != null) {
                is = parent.toIndexInjection(getIndexInParent());
            } else {
                is = new LinkedHashMap<>();
            }
            SpecifierManagerIndex i = getElementSpecifierIndex();
            if (i != null && index != -1) { //-1 for rowSpecifier, thus the map does not include rowSpecifier
                is.put(i, index);
            }
            return is;
        }

        public int getIndexInParent() {
            if (parent != null && parent.getParent() != null) { //parent.parent = null: parent is the root: -1, for rowSpecifier
                return parent.getChildren().indexOf(this);
            } else {
                return -1;
            }
        }

        public void set(ObjectTableColumnSize newSize) {
            if (!newSize.isComposition()) {
                size = Math.max(size, newSize.size());
            } else {
                error("set", newSize);
            }
        }

        protected void error(String msg, ObjectTableColumnSize error) {
            System.err.printf("something wrong: %s this=%s, error=%s\n", msg, this, error);

        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(size=" + size + ")";
        }

        public int[] toIndices() {
            List<Integer> is = new ArrayList<>();
            ObjectTableColumnSize size = getParent();
            int indexInSize = getIndexInParent();
            while (size != null) {
                is.add(indexInSize);
                indexInSize = size.getIndexInParent();
                size = size.getParent();
            }
            Collections.reverse(is);
            return is.stream()
                    .mapToInt(Integer::intValue)
                    .toArray();
        }

        public void debugPrint(int depth) {
            System.err.println(IntStream.range(0, depth)
                    .mapToObj(i -> "   ")
                    .collect(Collectors.joining()) + this.toString() +
                        Arrays.toString(toIndices()));
            for (ObjectTableColumnSize child : getChildren()) {
                child.debugPrint(depth + 1);
            }
        }
    }

    public static class ObjectTableColumnSizeComposite extends ObjectTableColumnSize {
        protected List<ObjectTableColumnSize> children;
        protected Map<SpecifierManagerIndex, Integer> injectionMapPrototype;

        public ObjectTableColumnSizeComposite(List<ObjectTableColumnSize> children) {
            this.children = children;
            int s = 0;
            for (ObjectTableColumnSize c : children) {
                s += c.size();
                c.setParent(this);
            }
            this.size = s;
        }

        @Override
        public boolean isComposition() {
            return true;
        }

        @Override
        public List<ObjectTableColumnSize> getChildren() {
            return children;
        }

        public void add(ObjectTableColumnSize childSize) {
            this.size += childSize.size();
            children.add(childSize);
            childSize.setParent(this);
        }

        /**
         * the method does not care about copying elementSpecifier of newSize
         * @param newSize another size which has same structure to this
         */
        @Override
        public void set(ObjectTableColumnSize newSize) {
            if (newSize.isComposition()) {
                List<ObjectTableColumnSize> newChildren = newSize.getChildren();
                for (int i = 0, l = Math.min(children.size(), newChildren.size()); i < l; ++i) {
                    ObjectTableColumnSize ns = newChildren.get(i);
                    ObjectTableColumnSize es = children.get(i);
                    children.get(i).set(es);
                    this.size += es.size() - ns.size();
                    es.setParent(this);
                }

                for (int i = children.size(), l = newChildren.size(); i < l; ++i) {
                    add(newChildren.get(i));
                }
                if (newSize.getElementSpecifierIndex() != null) {
                    setElementSpecifierIndex(newSize.getElementSpecifierIndex());
                }
            } else {
                error("set", newSize);
            }
        }

        @Override
        public void create(DynamicColumnContainer targetContainer) {
            targetContainer.moveExistingColumns();
            int i = 0;
            for (ObjectTableColumnSize child : getChildren()) {
                DynamicColumnContainer childContainer = targetContainer.getChild(i);
                child.create(childContainer);
                targetContainer.setLastIndex(childContainer.getLastIndexAfterUpdate());
                ++i;
            }
            int removing = targetContainer.getChildSize() - getChildren().size();
            if (removing > 0) {
                targetContainer.removeChildrenFromEnd(removing);
            }
        }

        @Override
        public Map<SpecifierManagerIndex, Integer> toIndexInjection(int index) {
            Map<SpecifierManagerIndex, Integer> is = new LinkedHashMap<>(
                    toIndexInjection());
            SpecifierManagerIndex i = getElementSpecifierIndex();
            if (i != null && index != -1) {
                is.put(i, index);
            }
            return is;
        }

        public Map<SpecifierManagerIndex, Integer> toIndexInjection() {
            if (injectionMapPrototype == null) {
                Map<SpecifierManagerIndex, Integer> map = new LinkedHashMap<>();
                ObjectTableColumnSize size = getParent();
                int index = getIndexInParent();
                while (size != null) {
                    SpecifierManagerIndex spec = size.getElementSpecifierIndex();
                    if (spec != null && index != -1) {
                        map.put(spec, index);
                    }
                    index = size.getIndexInParent();
                    size = size.getParent();
                }
                injectionMapPrototype = map;
            }
            return injectionMapPrototype;
        }

        @Override
        public void setElementSpecifierIndex(SpecifierManagerIndex elementSpecifierIndex) {
            super.setElementSpecifierIndex(elementSpecifierIndex);
            injectionMapPrototype = null;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(size=" + size + ") { " +
                    children.stream().map(Object::toString).collect(Collectors.joining(",")) + " }";
        }
    }
}
