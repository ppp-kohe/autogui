package autogui.base.mapping;

import autogui.base.type.GuiTypeCollection;
import autogui.base.type.GuiUpdatedValue;

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
 *           (the element itself is a regular element type but the collection element repr. has higher precedence in the factory).
 *
 *        So, a sub-context becomes a single element with the {@link GuiReprCollectionElement}.
 *
 *   <p>  The sub-contexts of the sub-collection element become a singleton list of the wrapped element representation.
 *       For cases of object elements in a collection, reprs of contexts become
 *          {@link GuiReprCollectionTable} -&gt;
 *            {@link GuiReprCollectionElement}(wrapping {@link GuiReprObjectPane}) -&gt;
 *            {@link GuiReprObjectPane} -&gt; children of {@link GuiReprObjectPane}
 *
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
 *       For cases of a list of a list, {@link GuiReprCollectionElement} wraps {@link GuiReprCollectionTable}.
 *        {@link GuiReprCollectionTable} -&gt;
 *          {@link GuiReprCollectionElement} (wrapping {@link GuiReprCollectionTable}) -&gt;
 *           {@link GuiReprCollectionTable} -&gt;
 *             {@link GuiReprCollectionElement} (wrapping ...) -&gt; ...
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
 *    contexts of {@link GuiReprCollectionElement} do not have any values or have a temporary element value.
 *      {@link GuiReprCollectionElement#checkAndUpdateSource(GuiMappingContext)} do nothing,
 *  <p>
 *    A concrete GUI component for the repr., like GuiSwingViewCollectionTable,
 *       can partially obtains properties of an element of a collection as on-demand cells.
 *
 *    A GUI column obtains its model value via {@link GuiReprValue#getUpdatedValue(GuiMappingContext, ObjectSpecifier)} ,
 *      and then the repr. obtains a parent value which is an element in a list.
 *    A parent {@link GuiReprCollectionElement} provides an element value by
 *        the special {@link #getValueCollectionElement(GuiMappingContext, GuiMappingContext.GuiSourceValue,
 *                          ObjectSpecifier, GuiMappingContext.GuiSourceValue)} .
 *
 * <h3>examples</h3>
 *   <pre>
 *       class O { List&lt;E&gt; list; }
 *       class E { String prop; }
 *       class SL { List&lt;String&gt; list; }
 *   </pre>
 *
 *   Notation:
 *   <ul>
 *   <li>(t,r,s) { cs }:
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
 *     For O,
 *   <pre>
 *
 *       (Obj(O), {@link GuiReprObjectPane}, GuiSwingViewObjectPane) {
 *           (Prop(list,Coll(List&lt;E&gt;)), {@link GuiReprPropertyPane}(subRepr={@link GuiReprCollectionTable}), GuiSwingViewPropertyPane) {
 *               (Coll(List&lt;E&gt;), {@link GuiReprCollectionTable}(subRepr={@link GuiReprCollectionElement}), GuiSwingViewCollectionTable) {
 *                    (Obj(E), {@link GuiReprCollectionElement}({@link GuiReprObjectPane}), GuiSwingTableColumnSetDefault) {
 *                         (Obj(E), {@link GuiReprObjectPane}, GuiSwingTableColumnSetDefault) {
 *                              (Prop(prop,String), {@link GuiReprValueStringField}, GuiSwingTableColumnString) {}
 *                         }
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
 *                   <li><code>GuiSwingTableColumnSet</code> calls <code>createColumn(subContext,...)</code> to sub-contexts GuiSwingTableColumn
 *                      and adds the returned TableColumn to the model.</li>
 *
 *               </ul>
 *       </li>
 *       <li><code>getValue and update</code>
 *              <ul>
 *                  <li><code>ObjectTableModel</code> first obtains row list via
 *                        <code>CollectionTable#getSource()</code> :
 *                         it returns <code>source</code> previously set by <code>setSwingViewValue(Object)</code>,
 *                            with calling {@link #toUpdateValue(GuiMappingContext, Object)}.
 *                          </li>
 *
 *                  <li><code>ObjectTableModel#getValueAt(int,int)</code> obtains a row object from the obtained source list</li>
 *                  <li>and call  <code>ObjectTableColumnValue#getCellValue(rowObject,ri,ci)</code>,
 *                              which causes {@link GuiReprValue#getValueWithoutNoUpdate(GuiMappingContext, GuiMappingContext.GuiSourceValue, ObjectSpecifier)}
 *                                  with the rowObject and a row-indexed specifier.
 *              </ul>
 *       </li>
 *   </ul>
 *
 *   For SL,
 *   <pre>
 *      (Obj(SL), GuiReprObjectPane, GUiSwingViewObjectPane) {
 *          (Prop(list,Coll(List&lt;String&gt;)), GuiReprPropertyPane(subRepr=GuiReprCollectionTable), GuiSwingViewPropertyPane) {
 *              (Coll(List&lt;String&gt;), GuiReprCollectionTable(subRepr=GuiReprCollectionElement), GuiSwingViewCollectionTable) {
 *                  (Obj(String), GuiReprCollectionElement(GuiReprValueStringField), GuiSwingTableColumnSetDefault) {
 *                      (Obj(String), GuiReprValueStringField, GuiSwingTableColumnString) {}
 *                  }
 *              }
 *          }
 *      }
 *    </pre>
 *    <ul>
 *        <li><code>update</code>: For updating the String element value.
 *          <ul>
 *            <li><code>ObjectTableModel#setValueAt(v,ri,ci)</code> causes
 *             <code>ObjectTableColumnValue#setCellValue(rowObj,ri,ci,v)</code></li>
 *            <li>the repr's update is {@link GuiReprValue#update(GuiMappingContext, GuiMappingContext.GuiSourceValue, Object, ObjectSpecifier)}
 *              and it matches the case of the parent context is a collection-element,
 *                then it calls parent's {@link GuiReprValue#updateWithParentSource(GuiMappingContext, Object, ObjectSpecifier)}</li>
 *            <li>the method of the parent obtains the source of the parent of the parent, which is a list, and
 *                 call {@link GuiReprCollectionElement#update(GuiMappingContext, GuiMappingContext.GuiSourceValue, Object, ObjectSpecifier)}
 *                 as the parent update.
 *                 the method delegates {@link GuiReprCollectionTable#updateCollectionElement(GuiMappingContext, GuiMappingContext.GuiSourceValue, Object, ObjectSpecifier)}</li>
 *
 *          </ul>
 *        </li>
 *
 *    </ul>
 *
 * */
public class GuiReprCollectionTable extends GuiReprValue {
    protected GuiRepresentation subRepresentation;

    public GuiReprCollectionTable(GuiRepresentation subRepresentation) {
        this.subRepresentation = subRepresentation;
    }

    @Override
    public boolean match(GuiMappingContext context) {
        //currently allow: class O { List<E> l; }  class E { List<String> c; } //c becomes a dynamic column
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

    @Override
    public int getValueCollectionSize(GuiMappingContext context, GuiMappingContext.GuiSourceValue collection, ObjectSpecifier specifier) throws Throwable {
        GuiTypeCollection collType = context.getTypeElementCollection();
        return context.execute(() -> collType.getSize(collection.getValue()));
    }

    @Override
    public GuiUpdatedValue getValueCollectionElement(GuiMappingContext context, GuiMappingContext.GuiSourceValue collection,
                                                     ObjectSpecifier elementSpecifier, GuiMappingContext.GuiSourceValue prev) throws Throwable {
        GuiTypeCollection collType = context.getTypeElementCollection();

        try {
            return context.execute(() ->
                    prev.isNone() ?
                            collType.executeGetElement(collection.getValue(), elementSpecifier.getIndex()) :
                            collType.executeGetElement(collection.getValue(), elementSpecifier.getIndex(), prev));
        } catch (Throwable ex) {
            if (!(ex instanceof IndexOutOfBoundsException)) {
                context.errorWhileUpdateSource(ex);
            }
            //empty value
            GuiMappingContext elemContext = getElementContext(context);
            GuiMappingContext elemValueContext = elemContext.getReprCollectionElement().getElementChild(elemContext);
            return GuiUpdatedValue.of(elemValueContext.getReprValue().toUpdateValue(elemValueContext, null));
        }
    }

    @Override
    public Object updateCollectionElement(GuiMappingContext context, GuiMappingContext.GuiSourceValue collection,
                                          Object newValue, ObjectSpecifier elementSpecifier) throws Throwable {
        GuiTypeCollection collType = context.getTypeElementCollection();
        try {
            return context.execute(() ->
                    collType.executeSetElement(collection.getValue(), elementSpecifier.getIndex(), newValue));
        } catch (Throwable ex) {
            if (!(ex instanceof IndexOutOfBoundsException)) {
                context.errorWhileUpdateSource(ex);
            }
            GuiMappingContext elemContext = getElementContext(context);
            GuiMappingContext elemValueContext = elemContext.getReprCollectionElement().getElementChild(elemContext);
            Object emptyValue = elemValueContext.getReprValue().toUpdateValue(elemValueContext, null);

            //fill with the empty value and add newValue
            List<Object> values = new ArrayList<>();
            for (int i = getValueCollectionSize(context, collection, elementSpecifier.getParent()), l = elementSpecifier.getIndex();
                 i < l; ++i) {
                values.add(emptyValue);
            }
            values.add(newValue);
            return context.execute(() ->
                    collType.executeAddElements(collection.getValue(), values));
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
        List<?> list = (List<?>) source;
        List<Object> array = new ArrayList<>(list.size());
        GuiMappingContext elementContext = getElementContext(context);
        for (Object element : list) {
            Object e = elementContext.getRepresentation().toJson(elementContext, element);
            if (e != null) {
                array.add(e);
            }
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        GuiMappingContext elementContext = getElementContext(context);
        if (json instanceof List<?>) {
            List<?> listJson = (List<?>) json;

            List<Object> listTarget = GuiReprValue.castOrMake(List.class, target, () -> null);
            List<Object> listResult = new ArrayList<>(listJson.size());

            for (int i = 0, l = listJson.size(); i < l; ++i) {
                Object elementJson = listJson.get(i);
                Object elementTarget = (listTarget != null && i < listTarget.size()) ? listTarget.get(i) : null;
                Object e = elementContext.getRepresentation().fromJson(elementContext, elementTarget, elementJson);
                if (e != null) {
                    listResult.add(e);
                }
            }
            return listResult;
        }
        return null;
    }

    public GuiMappingContext getElementContext(GuiMappingContext context) {
        return context.getChildren().stream()
                .filter(GuiMappingContext::isReprCollectionElement)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("no element"));
    }

    @Override
    public boolean isJsonSetter() {
        return false;
    }

    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        List<?> list = (List<?>) source;
        List<String> res = new ArrayList<>(list.size());
        GuiMappingContext elementContext = getElementContext(context);
        for (Object o : list) {
            res.add(elementContext.getRepresentation().toHumanReadableString(elementContext, o));
        }
        return String.join("\n", res);
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
