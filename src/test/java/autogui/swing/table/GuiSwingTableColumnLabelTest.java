package autogui.swing.table;

import autogui.base.mapping.*;
import autogui.base.type.*;
import autogui.swing.*;
import org.junit.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class GuiSwingTableColumnLabelTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new GuiSwingTableColumnLabelTest().test();
    }

    public GuiReprValue.ObjectSpecifier getSpecifier() {
        return GuiReprValue.NONE;
    }

    @Test
    public void test() {

        GuiReprSet set = new GuiReprSet();
        GuiReprCollectionTable repr = new GuiReprCollectionTable(set);

        set.add(new GuiReprCollectionElement(set));
        set.add(new GuiReprValueNumberSpinner());
        set.add(repr);
        set.add(new GuiReprPropertyPane(set));
        set.add(new GuiReprObjectPane(set));
        set.add(new GuiReprValueLabel());

        TestLabelList l = new TestLabelList();
        GuiMappingContext context = new GuiMappingContext(l);
        set.match(context);

        GuiMappingContext propContext = context.getChildren().get(0) //prop -> collectionTable
                .getChildren().get(0) //-> collectionElement
                    .getChildren().get(0); //-> prop ->

        propContext.setRepresentation(new GuiReprValueLabel());
        propContext.getChildren().clear();

        GuiSwingMapperSet sSet = new GuiSwingMapperSet();
        sSet.addReprClassTableColumn(GuiReprValueNumberSpinner.class, new GuiSwingTableColumnNumber());
        sSet.addReprClassTableColumn(GuiReprValueLabel.class, new GuiSwingTableColumnLabel());
        sSet.addReprClass(GuiReprCollectionElement.class, new GuiSwingTableColumnSetDefault(sSet));
        sSet.addReprClass(GuiReprCollectionTable.class, new GuiSwingViewCollectionTable(sSet));
        sSet.addReprClass(GuiReprObjectPane.class, new GuiSwingViewObjectPane(sSet));
        sSet.addReprClass(GuiReprPropertyPane.class, new GuiSwingViewPropertyPane(sSet));

        context.updateSourceFromRoot();

        JComponent component = runGet(() -> {
            GuiSwingView view = (GuiSwingView) sSet.view(context);
            JComponent comp = view.createView(context, this::getSpecifier);
            testFrame(comp);
            return comp;
        });

        GuiSwingViewCollectionTable.CollectionTable table = runQuery(component,
                query(JScrollPane.class, 0)
                        .cat(JViewport.class, 0)
                        .cat(GuiSwingViewCollectionTable.CollectionTable.class, 0));
    }

    public static class TestLabelList extends GuiTypeMemberProperty {
        public List<TestLabelElem> es = new ArrayList<>();
        public TestLabelList() {
            super("hello-list");

            es.add(new TestLabelElem("aaaa", 123.4f));
            es.add(new TestLabelElem("bbbb",  55567.8f));
            es.add(new TestLabelElem("cccc", -100));

            GuiTypeObject e = new GuiTypeObject(TestLabelElem.class);
            e.addProperties(new TestLabelStrProp(), new TestLabelProp());
            setType(new GuiTypeCollection(List.class, e));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, es);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            System.err.println("set list: " + value);
            es = (List<TestLabelElem>) value;
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    public static class TestLabelStrProp extends GuiTypeMemberProperty {
        public TestLabelStrProp() {
            super("hello");
            setType(new GuiTypeValue(String.class));
        }

        @Override
        public GuiUpdatedValue executeGet(Object target) throws Exception {
            TestLabelElem e = (TestLabelElem) target;
            System.err.println("get hello: " + target);
            return GuiUpdatedValue.of(e == null ? null : e.hello);
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestLabelElem e = (TestLabelElem) target;
            System.err.println("set hello: " + target + " " + value);
            if (e != null) {
                e.hello = (String) value;
            }
            return null;
        }
        @Override
        public boolean isWritable() {
            return false;
        }
    }
    public static class TestLabelProp extends GuiTypeMemberProperty {
        public TestLabelProp() {
            super("world");
            setType(new GuiTypeValue(Float.class));
        }

        @Override
        public GuiUpdatedValue executeGet(Object target) throws Exception {
            TestLabelElem e = (TestLabelElem) target;
            System.err.println("get world: " + target);
            return GuiUpdatedValue.of(e == null ? null : e.world);
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestLabelElem e = (TestLabelElem) target;
            System.err.println("set world: " + target + " " + value);
            if (e != null) {
                e.world = (float) value;
            }
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    public static class TestLabelElem {
        public String hello;
        public float world;

        public TestLabelElem(String hello, float world) {
            this.hello = hello;
            this.world = world;
        }
    }
}
