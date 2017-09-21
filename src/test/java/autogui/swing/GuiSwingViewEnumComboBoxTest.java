package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueEnumComboBox;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;

public class GuiSwingViewEnumComboBoxTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new GuiSwingViewEnumComboBoxTest().test();
    }
    @Test
    public void test() {
        GuiSwingViewEnumComboBox box = new GuiSwingViewEnumComboBox();

        TestEnumProp prop = new TestEnumProp();
        GuiMappingContext context = new GuiMappingContext(prop, new GuiReprValueEnumComboBox());
        context.updateSourceFromRoot();

        JComponent component = runGet(() -> {
            JComponent comp = box.createView(context);
            testFrame(comp);
            return comp;
        });

        GuiSwingViewEnumComboBox.PropertyEnumComboBox comboBox = runQuery(component, query(GuiSwingViewEnumComboBox.PropertyEnumComboBox.class, 0));
        System.err.println(comboBox.getSelectedItem());
        Assert.assertEquals(TestEnum.World, runGet(comboBox::getSelectedItem));

        prop.value = TestEnum.Hello;
        context.updateSourceFromRoot();

        System.err.println(comboBox.getSelectedItem());
        Assert.assertEquals(TestEnum.Hello, runGet(comboBox::getSelectedItem));

        run(() -> comboBox.setSelectedIndex(2));
        Assert.assertEquals(TestEnum.Again, prop.value);
    }

    public enum TestEnum {
        Hello,
        World,
        Again
    }

    public static class TestEnumProp extends GuiTypeMemberProperty {
        public TestEnum value = TestEnum.World;
        public TestEnumProp() {
            super("hello");
            setType(new GuiTypeValue(TestEnum.class));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, value);
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            this.value = (TestEnum) value;
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }
}
