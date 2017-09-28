package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueBooleanCheckBox;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class GuiSwingViewBooleanCheckBoxTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new GuiSwingViewBooleanCheckBoxTest().testProp();
    }

    @Test
    public void testValue() {
        GuiSwingViewBooleanCheckBox box = new GuiSwingViewBooleanCheckBox();

        TestBooleanValue value = new TestBooleanValue();
        GuiMappingContext context = new GuiMappingContext(value);
        context.setRepresentation(new GuiReprValueBooleanCheckBox());
        context.updateSourceFromRoot();

        JComponent pp = runGet(() -> {
            JComponent p = box.createView(context);
            JFrame frame = testFrame(p);
            return p;
        });

        GuiSwingViewBooleanCheckBox.PropertyCheckBox propertyPane = runQuery(pp, query(GuiSwingViewBooleanCheckBox.PropertyCheckBox.class, 0));


        Assert.assertTrue(runGet(propertyPane::isSelected));

        run(propertyPane::doClick);
        Assert.assertFalse(value.value);
    }

    @Test
    public void testProp() {
        GuiSwingViewBooleanCheckBox box = new GuiSwingViewBooleanCheckBox();

        TestBooleanProp prop = new TestBooleanProp();
        GuiMappingContext context = new GuiMappingContext(prop);
        context.setRepresentation(new GuiReprValueBooleanCheckBox());
        context.updateSourceFromRoot();

        JComponent pp = runGet(() -> {
            JComponent p = box.createView(context);
            JFrame frame = testFrame(p);

            JCheckBox c = new JCheckBox() {
                @Override
                protected void processMouseEvent(MouseEvent e) {
                    super.processMouseEvent(e);
                }
            };
            return p;
        });

        GuiSwingViewBooleanCheckBox.PropertyCheckBox propertyPane = runQuery(pp, query(GuiSwingViewBooleanCheckBox.PropertyCheckBox.class, 0));

        Assert.assertTrue(runGet(propertyPane::isSelected));

        run(propertyPane::doClick);
        Assert.assertFalse(prop.booleanValue().value);

        Assert.assertEquals("hello", propertyPane.getText());

    }

    public static class TestBooleanValue extends GuiTypeValue {
        public boolean value = true;
        public int updateCount;

        public TestBooleanValue() {
            super(Boolean.class);
        }

        @Override
        public Object updatedValue(Object prevValue) {
            return value;
        }

        @Override
        public Object writeValue(Object prevValue, Object newValue) {
            this.value = (Boolean) newValue;
            ++updateCount;
            System.err.println("update: " + updateCount + " : " + value);
            return newValue;
        }
    }

    public static class TestBooleanProp extends GuiTypeMemberProperty {
        public TestBooleanProp() {
            super("hello");
            setType(new TestBooleanValue());
        }

        public TestBooleanValue booleanValue() {
            return ((TestBooleanValue) getType());
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, booleanValue().value);
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            booleanValue().value = (Boolean) value;
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }
}
