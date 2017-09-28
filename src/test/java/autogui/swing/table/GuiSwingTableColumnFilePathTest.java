package autogui.swing.table;

import autogui.base.mapping.*;
import autogui.base.type.GuiTypeCollection;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeObject;
import autogui.base.type.GuiTypeValue;
import autogui.swing.*;
import org.junit.Test;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GuiSwingTableColumnFilePathTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new GuiSwingTableColumnFilePathTest().test();
    }

    @Test
    public void test() {
        GuiReprSet set = new GuiReprSet();
        GuiReprCollectionTable repr = new GuiReprCollectionTable(set);

        set.add(new GuiReprCollectionElement(set));
        set.add(new GuiReprValueFilePathField());
        set.add(new GuiReprValueStringField());
        set.add(repr);
        set.add(new GuiReprPropertyPane(set));
        set.add(new GuiReprObjectPane(set));

        TestPathList l = new TestPathList();
        GuiMappingContext context = new GuiMappingContext(l);
        set.match(context);

        GuiSwingMapperSet sSet = new GuiSwingMapperSet();
        sSet.addReprClassTableColumn(GuiReprValueFilePathField.class, new GuiSwingTableColumnFilePath());
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

        GuiSwingViewCollectionTable.CollectionTable table = runQuery(component, query(JScrollPane.class, 0)
                .cat(JViewport.class, 0).cat(GuiSwingViewCollectionTable.CollectionTable.class, 0));
        table.getSelectionModel().addListSelectionListener(e ->
                System.out.println(table.getSelectionModel().getMinSelectionIndex() + "-"  +table.getSelectionModel().getMaxSelectionIndex()));
    }


    public static class TestPathList extends GuiTypeMemberProperty {
        public List<TestPathElem> es = new ArrayList<>();
        public TestPathList() {
            super("hello-list");

            es.add(new TestPathElem("aaaa", Paths.get("src","main")));
            es.add(new TestPathElem("bbbb", Paths.get("src", "main", "java")));
            es.add(new TestPathElem("cccc", Paths.get("src", "test")));

            GuiTypeObject e = new GuiTypeObject(TestPathElem.class);
            e.addProperties(new TestPathStrProp(), new TestPathProp());
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
            es = (List<TestPathElem>) value;
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    public static class TestPathStrProp extends GuiTypeMemberProperty {
        public TestPathStrProp() {
            super("hello");
            setType(new GuiTypeValue(String.class));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            TestPathElem e = (TestPathElem) target;
            System.err.println("get hello: " + target + " prev: " + prevValue);
            if (e != null) {
                return compareGet(prevValue, e.hello);
            } else {
                return compareGet(prevValue, null);
            }
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestPathElem e = (TestPathElem) target;
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
    public static class TestPathProp extends GuiTypeMemberProperty {
        public TestPathProp() {
            super("world");
            setType(new GuiTypeValue(Path.class));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            TestPathElem e = (TestPathElem) target;
            System.err.println("get hello: " + target + " prev: " + prevValue);
            if (e != null) {
                return compareGet(prevValue, e.world);
            } else {
                return compareGet(prevValue, null);
            }
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestPathElem e = (TestPathElem) target;
            System.err.println("set hello: " + target + " " + value);
            if (e != null) {
                e.world = (Path) value;
            }
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    public static class TestPathElem {
        public String hello;
        public Path world;

        public TestPathElem(String hello, Path world) {
            this.hello = hello;
            this.world = world;
        }
    }
}
