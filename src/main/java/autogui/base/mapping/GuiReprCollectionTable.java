package autogui.base.mapping;

import autogui.base.type.GuiTypeCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * representation for {@link autogui.base.type.GuiTypeCollection}.
 *
 * <h3>matching and sub-contexts</h3>
 *  <p>
 *
 *        {@link GuiTypeCollection#getChildren()} will return the element type of the collection.
 *        Also, the {@link #subRepresentation} will take a factory of {@link GuiReprCollectionElement}.
 *         The factory will match the element type with the {@link GuiTypeCollection} as it's parent
 *           (the element is regular element type but the collection element repr. has higher precedence in the factory).
 *
 *        So, a sub-context is a single element with the {@link GuiReprCollectionElement}.
 *
 *   <p>  The sub-contexts of the sub-collection element are regular sub-contexts of the wrapped element representation.
 *        For example, if the wrapped collection element is an {@link GuiReprObjectPane}'s context,
 *          then the context of the collection element has
 *             properties ({@link GuiReprPropertyPane}) (and also actions) of the object members.
 *         As summarize, {@link GuiReprCollectionTable} -&gt;
 *            {@link GuiReprCollectionElement}(wrapping a {@link GuiRepresentation}) -&gt;
 *              member representations.
 *      <pre>
 *          [  { "e1": v1_1 , "e2":v1_2},
 *             { "e1": v2_1 , "e2":v2_2},
 *             { "e1": v3_1 , "e2":v3_2} ]
 *         =&gt;
 *         table:
 *           |    e1     |     e2      |
 *           ------------|--------------
 *           |    v1_1   |    v1_2     |
 *           |    v2_1   |    v2_2     |
 *           |    v3_1   |    v3_2     |
 *
 *      </pre>
 *
 *      <pre>
 *          [  [  v1_1 , v1_2],
 *             [  v2_1 , v2_2],
 *             [  v3_1 , v3_2] ]
 *         =&gt;
 *         table:
 *           |    0      |    1        |
 *           ------------|--------------
 *           |    v1_1   |    v1_2     |
 *           |    v2_1   |    v2_2     |
 *           |    v3_1   |    v3_2     |
 *
 *      </pre>
 *
 * <h3>accessing collection values</h3>
 * <p>
 *    The {@link GuiMappingContext#getSource()} of {@link GuiReprCollectionElement}
 *      holds a collection type, but contexts of {@link GuiReprCollectionElement} do not have any values.
 *      {@link GuiReprCollectionElement#checkAndUpdateSource(GuiMappingContext)} do nothing,
 *  <p>
 *    A concrete GUI component for the repr., like GuiSwingViewCollectionTable,
 *       can partially obtains property values of the collection as on-demand cells.
 *
 *    {@link GuiReprCollectionElement#getCellValue(GuiMappingContext, GuiMappingContext, Object, int, int)}
 *       can be used for the purpose. ObjectTableColumnValue's getCellValue(...) is an actual implementation.
 *     It takes the context of the collection element,
 *        an it's sub-context of the property of the row object,
 *           the source row object and row and column indices.
 * <h3>examples</h3>
 *   <pre>
 *       class O { List&lt;E&gt; list; }
 *       class E { String prop; }
 *   </pre>
 *
 *   Notation:
 *   <ul>
 *   <li>(T,r,s) { cs }:
 *       {@link GuiMappingContext} where
 *       t is a type {@link autogui.base.type.GuiTypeElement},
 *       r is a {@link GuiRepresentation},
 *       s is a swing component,
 *         and cs is a list of sub-contexts.
 *    </li>
 *    <li>Obj(T) : {@link autogui.base.type.GuiTypeObject}</li>
 *    <li>Prop(n,t) :{@link autogui.base.type.GuiTypeMemberProperty} </li>
 *    <li>Coll(T&lt;E&gt;) :{@link autogui.base.type.GuiTypeCollection} </li>
 *  </ul>
 *   <pre>
 *       (Obj(O), {@link GuiReprObjectPane}, GuiSwingViewObjectPane) {
 *           (Prop(list,Coll(List&lt;E&gt;)), {@link GuiReprPropertyPane}(subRepr={@link GuiReprCollectionTable}), GuiSwingViewPropertyPane) {
 *               (Coll(List&lt;E&gt;), {@link GuiReprCollectionTable}(subRepr={@link GuiReprCollectionElement}), GuiSwingViewCollectionTable) {
 *                    (Obj(E)), {@link GuiReprCollectionElement}({@link GuiReprObjectPane}), GuiSwingTableColumnSetDefault) {
 *                         (Prop(prop,String), {@link GuiReprValueStringField}, GuiSwingTableColumnString) {}
 *                    }
 *               }
 *           }
 *       }
 *   </pre>
 *   <ul>
 *       <li> <code>createView</code>:
 *               <ul>
 *                   <li><code>GuiSwingCollectionTable</code> creates a <code>CollectionTable</code> (a subclass of <code>JTable</code>):
 *                      the model is customized as it's row source is a list obtained from the context.
 *                      The table is a {@link autogui.base.mapping.GuiMappingContext.SourceUpdateListener} and
 *                         registered to the context.</li>
 *                   <li><code>GuiSwingCollectionTable</code>
 *                         constructs columns by <code>GuiSwingTableColumnSet#createColumns(...)</code></li>
 *                   <li><code>GuiSwingTableColumnSet</code> calls <code>createColumn(subContext)</code> to sub-contexts GuiSwingTableColumn
 *                      and adds the returned TableColumn to the model.</li>
 *
 *               </ul>
 *       </li>
 *       <li><code>update</code>
 *              <ul>
 *                  <li><code>ObjectTableModel</code> first obtains row list via
 *                        <code>CollectionTable#getSource()</code> :
 *                         it returns <code>source</code> previously set by <code>setSwingViewValue(Object)</code>,
 *                            with calling {@link #toUpdateValue(GuiMappingContext, Object)}.
 *                          </li>
 *                  <li><code>ObjectTableModel#getValueAt(int,int)</code> builds a table array and
 *                        sets the value obtained by
 *                              <code>ObjectTableColumnValue#getCellValue(...)</code> with a row object,
 *                              which causes {@link GuiReprCollectionElement#getCellValue(GuiMappingContext, GuiMappingContext, Object, int, int)}
 *                                 with the column repr. {@link GuiReprValue}
 *                                  and parent repr. {@link GuiReprCollectionElement},
 *                            to the specified cell value.</li>
 *              </ul>
 *       </li>
 *   </ul>
 * */
public class GuiReprCollectionTable extends GuiReprValue implements GuiRepresentation {
    protected GuiRepresentation subRepresentation;

    public GuiReprCollectionTable(GuiRepresentation subRepresentation) {
        this.subRepresentation = subRepresentation;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        if (context.isTypeElementCollection()) {
            context.setRepresentation(this);
            for (GuiMappingContext subContext : context.createChildCandidates()) {
                if (subRepresentation.match(subContext)) {
                    subContext.addToParent();
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public List<?> toUpdateValue(GuiMappingContext context, Object newValue) {
        if (newValue == null) {
            return Collections.emptyList();
        } else {
            return (List<?>) newValue;
        }
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object
     * @return List: { elementJson, ... }.  Note: null elements are skipped
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        for (GuiMappingContext elementContext : context.getChildren()) {
            Object obj = elementContext.getRepresentation().toJsonWithNamed(elementContext, source);
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        for (GuiMappingContext elementContext : context.getChildren()) {
            Object obj = elementContext.getRepresentation().fromJson(elementContext, target, json);
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    @Override
    public boolean isJsonSetter() {
        return false;
    }

    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        List<String> list = new ArrayList<>(1);
        GuiReprObjectPane.runSubCollectionValue(context, source,
                GuiReprObjectPane.getAddingHumanReadableStringToList(list));
        return String.join("\t", list);
    }

    @Override
    public boolean isHistoryValueSupported() {
        return false;
    }

    @Override
    public boolean isHistoryValueStored() {
        return false;
    }

    @Override
    public String toString() {
        return toStringHeader() + "(" + subRepresentation + ")";
    }

    /** interface for actions which handle selected-rows;
     *    an actual implementation relies on a concrete GUI component */
    public interface TableTarget {
        boolean isSelectionEmpty();

        IntStream getSelectedRows();

        List<Object> getSelectedRowValues();

        List<CellValue> getSelectedCells();

        /**
         * @return a stream of {row, column}. those indexes are from the view model,
         *    which might be different from the context's ones.
         *    The view-model has additional columns other than the context, e.g. the row-index column.
         */
        Stream<int[]> getSelectedCellIndexesStream();

        default List<Object> getSelectedCellValues() {
            return getSelectedCells().stream()
                    .map(CellValue::getValue)
                    .collect(Collectors.toList());
        }

        void setCellValues(List<CellValue> values);

        /**
         * @param pos the specified cell: {row, column}
         * @param posToValue pos: {row, column} to a cell value, if it returns null, then it skips the updating
         */
        void setCellValues(Stream<int[]> pos, Function<int[], Object> posToValue);
    }

    /**
     * a sub-type of table-target, also supporting selecting partial columns;
     *   in the type, {@link #getSelectedCells()} returns the values of the partial columns.
     *   it also supports obtaining all-cells in a row.
     */
    public interface TableTargetCell extends TableTarget {
        Stream<int[]> getSelectedRowAllCellIndexesStream();

        List<CellValue> getSelectedRowAllCells();
    }

    /** an interface for operating a specific column with selected rows */
    public interface TableTargetColumn extends TableTarget {
        /**
         * @return the value of the primary cell
         */
        Object getSelectedCellValue();

        default void setSelectedCellValuesLoop(List<?> rowValues) {
            setCellValues(getSelectedCellIndexesStream(),
                    new TableTargetColumnFillLoop(rowValues));
        }
    }

    /** a function for setting cell-values with different sized values */
    public static class TableTargetColumnFillLoop implements Function<int[],Object> {
        protected int nextIndex;
        protected List<?> values;

        public TableTargetColumnFillLoop(List<?> values) {
            this.values = values;
        }

        @Override
        public Object apply(int[] ints) {
            Object v = values.get(nextIndex % values.size());
            ++nextIndex;
            return v;
        }
    }

    /** a selected cell value */
    public static class CellValue {
        public int row;
        public int column;
        public Object value;

        public CellValue(int row, int column, Object value) {
            this.row = row;
            this.column = column;
            this.value = value;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

        public Object getValue() {
            return value;
        }
    }
}
