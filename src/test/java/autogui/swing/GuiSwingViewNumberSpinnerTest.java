package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueNumberSpinner;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;

public class GuiSwingViewNumberSpinnerTest extends GuiSwingTestCase {
    public static void main(String[] args) throws Exception {
        new GuiSwingViewNumberSpinnerTest().test();
    }
    @Test
    public void test() throws Exception {
        GuiSwingViewNumberSpinner s = new GuiSwingViewNumberSpinner();

        TestNumberProp p = new TestNumberProp();
        GuiMappingContext context = new GuiMappingContext(p);
        context.setRepresentation(new GuiReprValueNumberSpinner());
        context.updateSourceFromRoot();

        JComponent comp = runGet(() -> {
            JComponent component = s.createView(context);
            testFrame(component);
            return component;
        });

        GuiSwingViewNumberSpinner.PropertyNumberSpinner spinner = runQuery(comp, query(GuiSwingViewNumberSpinner.PropertyNumberSpinner.class, 0));
        Assert.assertEquals(123.0f, spinner.getValue());

        run(() -> spinner.setValue(456.7f));

        Thread.sleep(1000);

        Assert.assertEquals(456.7f, p.number);
    }

    public static class TestNumberProp extends GuiTypeMemberProperty {
        public Number number;
        public TestNumberProp() {
            super("hello");
            setType(new GuiTypeValue(Float.class));
            number = 123.0f;
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, number);
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            number = (Number) value;
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }
}
