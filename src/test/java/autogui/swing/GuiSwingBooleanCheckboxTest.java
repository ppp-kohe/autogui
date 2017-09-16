package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueBooleanCheckbox;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;

public class GuiSwingBooleanCheckboxTest extends GuiSwingTestCase {
    @Test
    public void testValue() {
        GuiSwingViewBooleanCheckbox box = new GuiSwingViewBooleanCheckbox();

        TestBooleanValue value = new TestBooleanValue();
        GuiMappingContext context = new GuiMappingContext(value);
        context.setRepresentation(new GuiReprValueBooleanCheckbox());
        context.updateSourceFromRoot();

        GuiSwingViewBooleanCheckbox.PropertyCheckBox propertyPane = runGet(() -> {
            GuiSwingViewBooleanCheckbox.PropertyCheckBox p = (GuiSwingViewBooleanCheckbox.PropertyCheckBox) box.createView(context);
            JFrame frame = testFrame(p);
            return p;
        });
        Assert.assertTrue(runGet(propertyPane::isSelected));

        run(propertyPane::doClick);
        Assert.assertFalse(value.value);
    }

    @Test
    public void testProp() {
        GuiSwingViewBooleanCheckbox box = new GuiSwingViewBooleanCheckbox();

        TestBooleanProp prop = new TestBooleanProp();
        GuiMappingContext context = new GuiMappingContext(prop);
        context.setRepresentation(new GuiReprValueBooleanCheckbox());
        context.updateSourceFromRoot();

        GuiSwingViewBooleanCheckbox.PropertyCheckBox propertyPane = runGet(() -> {
            GuiSwingViewBooleanCheckbox.PropertyCheckBox p = (GuiSwingViewBooleanCheckbox.PropertyCheckBox) box.createView(context);
            JFrame frame = testFrame(p);
            return p;
        });

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
