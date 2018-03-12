package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueLabel;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;

public class GuiSwingViewLabelTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        new GuiSwingViewLabelTest().test();
    }
    @Test
    public void test() {
        TestObj obj = new TestObj();
        GuiSwingViewLabel label = new GuiSwingViewLabel();
        GuiReprValueLabel repr = new GuiReprValueLabel();

        GuiMappingContext context = new GuiMappingContext(obj, repr);

        JComponent c = runGet(() -> {
            JComponent comp = label.createView(context);

            context.updateSourceFromRoot();

            testFrame(comp);

            return comp;
        });

        GuiSwingViewLabel.PropertyLabel lbl = runQuery(c, query(GuiSwingViewLabel.PropertyLabel.class, 0));
        Assert.assertEquals("value-1", runGet(lbl::getText));
    }

    public static class TestObj extends GuiTypeMemberProperty {
        int count;
        public TestObj() {
            super("hello");
            setType(new GuiTypeValue(getClass()));
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            ++count;
            return this;
        }

        @Override
        public String toString() {
            return "value-" + count;
        }
    }
}
