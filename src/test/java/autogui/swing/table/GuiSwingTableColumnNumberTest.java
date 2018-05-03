package autogui.swing.table;

import autogui.base.mapping.*;
import autogui.base.type.GuiTypeCollection;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeObject;
import autogui.base.type.GuiTypeValue;
import autogui.swing.*;
import org.junit.Test;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class GuiSwingTableColumnNumberTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new GuiSwingTableColumnNumberTest().test();
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
        set.add(new GuiReprValueStringField());
        set.add(repr);
        set.add(new GuiReprPropertyPane(set));
        set.add(new GuiReprObjectPane(set));

        TestNumList l = new TestNumList();
        GuiMappingContext context = new GuiMappingContext(l);
        set.match(context);

        GuiSwingMapperSet sSet = new GuiSwingMapperSet();
        sSet.addReprClassTableColumn(GuiReprValueNumberSpinner.class, new GuiSwingTableColumnNumber());
        sSet.addReprClassTableColumn(GuiReprValueStringField.class, new GuiSwingTableColumnString());
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

    public static class TestNumList extends GuiTypeMemberProperty {
        public List<TestNumElem> es = new ArrayList<>();
        public TestNumList() {
            super("hello-list");

            es.add(new TestNumElem("aaaa", 123.4f));
            es.add(new TestNumElem("bbbb",  55567.8f));
            es.add(new TestNumElem("cccc", -100));

            GuiTypeObject e = new GuiTypeObject(TestNumElem.class);
            e.addProperties(new TestNumStrProp(), new TestNumProp());
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
            es = (List<TestNumElem>) value;
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    public static class TestNumStrProp extends GuiTypeMemberProperty {
        public TestNumStrProp() {
            super("hello");
            setType(new GuiTypeValue(String.class));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            TestNumElem e = (TestNumElem) target;
            System.err.println("get hello: " + target + " prev: " + prevValue);
            if (e != null) {
                return compareGet(prevValue, e.hello);
            } else {
                return compareGet(prevValue, null);
            }
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestNumElem e = (TestNumElem) target;
            System.err.println("set hello: " + target + " " + value);
            if (e != null) {
                e.hello = (String) value;
            }
            return null;
        }
        @Override
        public boolean isWritable() {
            return true;
        }
    }
    public static class TestNumProp extends GuiTypeMemberProperty {
        public TestNumProp() {
            super("world");
            setType(new GuiTypeValue(Float.class));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            TestNumElem e = (TestNumElem) target;
            System.err.println("get world: " + target + " prev: " + prevValue);
            if (e != null) {
                return compareGet(prevValue, e.world);
            } else {
                return compareGet(prevValue, null);
            }
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestNumElem e = (TestNumElem) target;
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

    public static class TestNumElem {
        public String hello;
        public float world;

        public TestNumElem(String hello, float world) {
            this.hello = hello;
            this.world = world;
        }
    }
}
