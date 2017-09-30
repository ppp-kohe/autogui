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

public class GuiSwingTableColumnEnumTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new GuiSwingTableColumnEnumTest().test();
    }

    @Test
    public void test() {

        GuiReprSet set = new GuiReprSet();
        GuiReprCollectionTable repr = new GuiReprCollectionTable(set);

        set.add(new GuiReprCollectionElement(set));
        set.add(new GuiReprValueEnumComboBox());
        set.add(new GuiReprValueStringField());
        set.add(repr);
        set.add(new GuiReprPropertyPane(set));
        set.add(new GuiReprObjectPane(set));

        TestEnumList l = new TestEnumList();
        GuiMappingContext context = new GuiMappingContext(l);
        set.match(context);

        GuiSwingMapperSet sSet = new GuiSwingMapperSet();
        sSet.addReprClassTableColumn(GuiReprValueEnumComboBox.class, new GuiSwingTableColumnEnum());
        sSet.addReprClassTableColumn(GuiReprValueStringField.class, new GuiSwingTableColumnString());
        sSet.addReprClass(GuiReprCollectionElement.class, new GuiSwingTableColumnSetDefault(sSet));
        sSet.addReprClass(GuiReprCollectionTable.class, new GuiSwingViewCollectionTable(sSet));
        sSet.addReprClass(GuiReprObjectPane.class, new GuiSwingViewObjectPane(sSet));
        sSet.addReprClass(GuiReprPropertyPane.class, new GuiSwingViewPropertyPane(sSet));

        context.updateSourceFromRoot();

        JComponent component = runGet(() -> {
            GuiSwingView view = (GuiSwingView) sSet.view(context);
            JComponent comp = view.createView(context);
            testFrame(comp);
            return comp;
        });

        GuiSwingViewCollectionTable.CollectionTable table = runQuery(component,
                query(JScrollPane.class, 0)
                        .cat(JViewport.class, 0)
                        .cat(GuiSwingViewCollectionTable.CollectionTable.class, 0));
    }

    public static class TestEnumList extends GuiTypeMemberProperty {
        public List<TestEnumElem> es = new ArrayList<>();
        public TestEnumList() {
            super("hello-list");

            es.add(new TestEnumElem("aaaa", TestEnum.Hello));
            es.add(new TestEnumElem("bbbb",  TestEnum.World));
            es.add(new TestEnumElem("cccc", TestEnum.Again));

            GuiTypeObject e = new GuiTypeObject(TestEnumElem.class);
            e.addProperties(new TestEnumStrProp(), new TestEnumProp());
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
            es = (List<TestEnumElem>) value;
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    public static class TestEnumStrProp extends GuiTypeMemberProperty {
        public TestEnumStrProp() {
            super("hello");
            setType(new GuiTypeValue(String.class));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            TestEnumElem e = (TestEnumElem) target;
            System.err.println("get hello: " + target + " prev: " + prevValue);
            if (e != null) {
                return compareGet(prevValue, e.hello);
            } else {
                return compareGet(prevValue, null);
            }
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestEnumElem e = (TestEnumElem) target;
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

    public static class TestEnumProp extends GuiTypeMemberProperty {
        public TestEnumProp() {
            super("world");
            setType(new GuiTypeValue(TestEnum.class));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            TestEnumElem e = (TestEnumElem) target;
            System.err.println("get world: " + target + " prev: " + prevValue);
            if (e != null) {
                return compareGet(prevValue, e.world);
            } else {
                return compareGet(prevValue, null);
            }
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestEnumElem e = (TestEnumElem) target;
            System.err.println("set world: " + target + " " + value);
            if (e != null) {
                e.world = (TestEnum) value;
            }
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    public static class TestEnumElem {
        public String hello;
        public TestEnum world;

        public TestEnumElem(String hello, TestEnum world) {
            this.hello = hello;
            this.world = world;
        }
    }

    public enum TestEnum {
        Hello,
        World,
        Again
    }
}
