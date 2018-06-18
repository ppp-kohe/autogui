package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * the dynamic version of {@link GuiSwingTableColumn}, creating a factory instead of a column
 */
public interface GuiSwingTableColumnDynamic extends GuiSwingElement {
    DynamicColumnFactory createColumnDynamic(GuiMappingContext context,
                                             GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
                                             GuiSwingView.SpecifierManager parentSpecifier, boolean tableTop);

    /**
     * a factory of size info. which becomes a set of factories of each concrete columns */
    interface DynamicColumnFactory {
        ObjectTableColumnSize getColumnSize(Object c);
    }

    /**
     * a size information of hierarchical composition of sub-columns
     *
     *  <pre>
     *      Size ::= { Size, Size, ... }  //composition
     *             | Int                  //concrete-column
     *
     *      {a,b,c}.set({a',b',c',d}) ::= {a.set(a'),b.set(b'),c.set(c'),d}
     *            n.set(n')           ::= max(n,n')
     *
     *         size(List&lt;V&gt; l) where V is a value-type
     *                                ::= l.size()
     *         size(List&lt;E&gt; s)        ::= { size(s.get(0)), size(s.get(1)), ... }
     *         size(C c) and class C { T0 f0; T1 f1;...; }
     *                                ::= { size(f0), size(f1), ... }
     *         size(V v)              ::= 1
     *  </pre>
     *
     *  <pre>
     *    //examples:
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
     *      class E { List&lt;Float&gt; l1, l2; } //E({l1_1,l1_2},{l2_1,l2_2})
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
    abstract class ObjectTableColumnSize {
        protected int size;
        protected ObjectTableColumnSize parent;
        protected GuiSwingTableColumn.SpecifierManagerIndex elementSpecifierIndex;

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
            error("getChildren", null);
            return Collections.emptyList();
        }

        public ObjectTableColumnSize getParent() {
            return parent;
        }

        public ObjectTableColumn createColumn(ObjectTableColumnIndex index) {
            if (elementSpecifierIndex != null) {
                elementSpecifierIndex.setIndex(index.getIndex());
            }
            return null;
        }

        public void setElementSpecifierIndex(GuiSwingTableColumn.SpecifierManagerIndex elementSpecifierIndex) {
            this.elementSpecifierIndex = elementSpecifierIndex;
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
            return getClass().getSimpleName() + "(size=" + size + ", isComposition()=" + isComposition() + ")";
        }
    }

    class ObjectTableColumnSizeComposite extends ObjectTableColumnSize {
        protected List<ObjectTableColumnSize> children;

        public ObjectTableColumnSizeComposite(List<ObjectTableColumnSize> children) {
            this.children = children;
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
        }

        @Override
        public void set(ObjectTableColumnSize newSize) {
            if (newSize.isComposition()) {
                List<ObjectTableColumnSize> newChildren = newSize.getChildren();
                for (int i = 0, l = Math.min(children.size(), newChildren.size()); i < l; ++i) {
                    children.get(i).set(newChildren.get(i));
                }

                for (int i = children.size(), l = newChildren.size(); i < l; ++i) {
                    children.add(newChildren.get(i));
                }
            } else {
                error("set", newSize);
            }
        }
    }
}
