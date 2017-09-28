package autogui.swing;

import autogui.GuiIncluded;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiRepresentation;
import autogui.base.type.*;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;

public class GuiSwingViewObjectPaneTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new GuiSwingViewObjectPaneTest().test();
    }

    @Test
    public void test() {
        GuiSwingViewObjectPane obj = new GuiSwingViewObjectPane(GuiSwingElement.getDefaultMapperSet());

        TestObjType src = new TestObjType();
        GuiMappingContext context = new GuiMappingContext(src);
        Assert.assertTrue(GuiRepresentation.getDefaultSet().match(context));
        context.updateSourceFromRoot();

        GuiSwingViewObjectPane.ObjectPane pane = runGet(() -> {
            GuiSwingViewObjectPane.ObjectPane p = (GuiSwingViewObjectPane.ObjectPane) obj.createView(context);
            JFrame frame = testFrame(p);

            return p;
        });

        GuiSwingViewBooleanCheckBox.PropertyCheckBox helloBox = runQuery(pane, query(JComponent.class, 0)
                .cat(JComponent.class, 0).cat(GuiSwingViewBooleanCheckBox.PropertyCheckBox.class, 0));
        GuiSwingViewBooleanCheckBox.PropertyCheckBox worldBox = runQuery(pane, query(JComponent.class, 0)
                .cat(JComponent.class, 1).cat(GuiSwingViewBooleanCheckBox.PropertyCheckBox.class, 0));

        Assert.assertTrue(helloBox.isSelected());
        Assert.assertFalse(worldBox.isSelected());

        run(helloBox::doClick);
        Assert.assertFalse(src.value.hello);
        Assert.assertFalse(src.value.world);

        run(worldBox::doClick);
        Assert.assertFalse(src.value.hello);
        Assert.assertTrue(src.value.world);

        JButton action = runQuery(pane, query(JToolBar.class, 0).cat(JButton.class, 0));
        System.err.println(action);
        run(action::doClick);

        Assert.assertEquals(1, src.value.testValue);

        Assert.assertTrue(helloBox.isSelected());
        Assert.assertFalse(worldBox.isSelected());
    }

    public static class TestObj {
        @GuiIncluded
        public boolean hello = true;

        @GuiIncluded
        public boolean world = false;

        public int testValue = 0;

        @GuiIncluded
        public void test() {
            System.out.println("action " + testValue);
            testValue++;
            hello = !hello;
            world = !world;
        }
    }

    public static class TestObjType extends GuiTypeObject {
        public TestObj value = new TestObj();
        public TestObjType() {
            super(TestObj.class);
            addProperties(
                    new TestObjFieldProp(true, value),
                    new TestObjFieldProp(false, value));
            addActions(new TestObjAction());
        }

        @Override
        public Object updatedValue(Object prevValue) {
            return value;
        }

        @Override
        public boolean isWritable(Object value) {
            return false;
        }
    }

    public static class TestObjAction extends GuiTypeMemberAction {
        public TestObjAction() {
            super("test", "test");
        }

        @Override
        public Object execute(Object target) throws Exception {
            ((TestObj) target).test();
            return null;
        }
    }

    public static class TestObjFieldProp extends GuiTypeMemberProperty {
        public boolean hello;
        public TestObj value;
        public TestObjFieldProp(boolean hello, TestObj obj ) {
            super(hello ? "hello" : "world");
            this.hello = hello;
            setType(new GuiTypeValue(boolean.class));
            this.value = obj;
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            if (hello) {
                this.value.hello = (Boolean) value;
            } else {
                this.value.world = (Boolean) value;
            }
            return null;
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, hello ? this.value.hello : this.value.world);
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }
}
