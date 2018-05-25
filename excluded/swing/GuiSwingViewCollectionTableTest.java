package autogui.swing;

import autogui.base.mapping.*;
import autogui.base.type.*;
import autogui.swing.table.GuiSwingTableColumnBoolean;
import autogui.swing.table.GuiSwingTableColumnSetDefault;
import autogui.swing.table.GuiSwingTableColumnString;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class GuiSwingViewCollectionTableTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new GuiSwingViewCollectionTableTest().test();
    }

    public GuiReprSet getTestSet() {
        GuiReprSet set = new GuiReprSet();
        GuiReprCollectionTable repr = new GuiReprCollectionTable(set);

        set.add(new GuiReprCollectionElement(set));
        set.add(new GuiReprValueBooleanCheckBox());
        set.add(new GuiReprValueStringField());
        set.add(repr);
        set.add(new GuiReprPropertyPane(set));
        set.add(new GuiReprObjectPane(set));
        set.add(new GuiReprAction());
        set.add(new GuiReprActionList());

        return set;
    }

    @Test
    public void testBuild() {
        GuiReprSet set = getTestSet();

        TestList l = new TestList();
        GuiMappingContext context = new GuiMappingContext(l);
        Assert.assertTrue(set.match(context));

        //property List<TestElem> hello-list
        Assert.assertTrue(context.getRepresentation() instanceof GuiReprPropertyPane);

        //property List<TestElem> hello-list -> collection List TestElem
        GuiMappingContext propTypeContext = context.getChildren().get(0);
        Assert.assertTrue(propTypeContext.getRepresentation() instanceof GuiReprCollectionTable);

        //collection List TestElem -> collection-element object TestElem
        GuiMappingContext colContext = propTypeContext.getChildren().get(0);
        Assert.assertTrue(colContext.getRepresentation() instanceof GuiReprCollectionElement);

        //collection-element object TestElem
        GuiReprCollectionElement elRepr = (GuiReprCollectionElement) colContext.getRepresentation();
        Assert.assertTrue(elRepr.getRepresentation() instanceof GuiReprObjectPane);

        //collection-element object TestElem -> { property String hello;  property boolean world; }
        GuiMappingContext helloContext = colContext.getChildren().get(0);
        GuiMappingContext worldContext = colContext.getChildren().get(1);

        Assert.assertTrue(helloContext.getRepresentation() instanceof GuiReprValueStringField);
        Assert.assertTrue(worldContext.getRepresentation() instanceof GuiReprValueBooleanCheckBox);
    }

    public GuiReprValue.ObjectSpecifier getSpecifier() {
        return GuiReprValue.NONE;
    }

    @Test
    public void test() {
        GuiReprSet set = getTestSet();

        TestList l = new TestList();
        GuiMappingContext context = new GuiMappingContext(l);
        set.match(context);

        GuiSwingMapperSet sSet = new GuiSwingMapperSet();
        sSet.addReprClassTableColumn(GuiReprValueBooleanCheckBox.class, new GuiSwingTableColumnBoolean());
        sSet.addReprClassTableColumn(GuiReprValueStringField.class, new GuiSwingTableColumnString());
        sSet.addReprClass(GuiReprCollectionElement.class, new GuiSwingTableColumnSetDefault(sSet));
        sSet.addReprClass(GuiReprCollectionTable.class, new GuiSwingViewCollectionTable(sSet));
        sSet.addReprClass(GuiReprObjectPane.class, new GuiSwingViewObjectPane(sSet));
        sSet.addReprClass(GuiReprPropertyPane.class, new GuiSwingViewPropertyPane(sSet));
        sSet.addReprClass(GuiReprAction.class, new GuiSwingActionDefault());
        sSet.addReprClass(GuiReprActionList.class, new GuiSwingActionDefault());

        context.updateSourceFromRoot();

        JComponent component = runGet(() -> {
            GuiSwingView view = (GuiSwingView) sSet.view(context);
            JComponent comp = view.createView(context, this::getSpecifier);
            testFrame(comp);
            return comp;
        });

        GuiSwingViewCollectionTable.CollectionTable table = runQuery(component,
                query(JPanel.class, 0)
                .cat(JScrollPane.class, 0)
                .cat(JViewport.class, 0)
                .cat(GuiSwingViewCollectionTable.CollectionTable.class, 0));
        table.getSelectionModel().addListSelectionListener(e ->
                System.out.println(table.getSelectionModel().getMinSelectionIndex() + "-"  +table.getSelectionModel().getMaxSelectionIndex()));
    }

    public static class TestList extends GuiTypeMemberProperty {
        public List<TestElem> es = new ArrayList<>();
        public TestList() {
            super("hello-list");

            es.add(new TestElem("aaaa", true));
            es.add(new TestElem("bbbb", false));
            es.add(new TestElem("cccc", true));

            GuiTypeObject e = new GuiTypeObject(TestElem.class);
            e.addProperties(new TestStrProp(), new TestBoolProp());
            e.addActions(new TestAction());
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
            es = (List<TestElem>) value;
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    public static class TestStrProp extends GuiTypeMemberProperty {
        public TestStrProp() {
            super("hello");
            setType(new GuiTypeValue(String.class));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            TestElem e = (TestElem) target;
            System.err.println("get hello: " + target + " prev: " + prevValue);
            if (e != null) {
                return compareGet(prevValue, e.hello);
            } else {
                return compareGet(prevValue, null);
            }
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestElem e = (TestElem) target;
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
    public static class TestBoolProp extends GuiTypeMemberProperty {
        public TestBoolProp() {
            super("world");
            setType(new GuiTypeValue(boolean.class));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            TestElem e = (TestElem) target;
            System.err.println("get hello: " + target + " prev: " + prevValue);
            if (e != null) {
                return compareGet(prevValue, e.world);
            } else {
                return compareGet(prevValue, null);
            }
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            TestElem e = (TestElem) target;
            System.err.println("set hello: " + target + " " + value);
            if (e != null) {
                e.world = (boolean) value;
            }
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }

    public static class TestAction extends GuiTypeMemberAction {
        public TestAction() {
            super("run", (String) null);
        }

        @Override
        public Object execute(Object target) throws Exception {
            System.err.println("run " + target);
            ((TestElem) target).run();
            return null;
        }
    }

    public static class TestElem {
        public String hello;
        public boolean world;

        public TestElem(String hello, boolean world) {
            this.hello = hello;
            this.world = world;
        }

        public void run() {
            hello += "!";
            world = !world;
        }
    }
}
