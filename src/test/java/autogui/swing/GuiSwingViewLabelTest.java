package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueLabel;
import autogui.base.type.GuiTypeCollection;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.swing.util.NamedPane;
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
        GUiSwingViewLabel label = new GUiSwingViewLabel();
        GuiReprValueLabel repr = new GuiReprValueLabel();

        GuiMappingContext context = new GuiMappingContext(obj, repr);

        JComponent c = runGet(() -> {
            JComponent comp = label.createView(context);

            context.updateSourceFromRoot();

            testFrame(comp);

            return comp;
        });

        GUiSwingViewLabel.PropertyLabel lbl = runQuery(c, query(GUiSwingViewLabel.PropertyLabel.class, 0));
        Assert.assertEquals("value-1", runGet(lbl::getText));
    }

    public static class TestObj extends GuiTypeMemberProperty {
        int count;
        public TestObj() {
            super("hello");
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
