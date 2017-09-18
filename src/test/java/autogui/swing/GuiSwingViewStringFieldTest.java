package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueStringField;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;
import autogui.swing.util.SearchTextField;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;

public class GuiSwingViewStringFieldTest extends GuiSwingTestCase {
    public static void main(String[] args) throws Exception {
        new GuiSwingViewStringFieldTest().test();
    }

    @Test
    public void test() throws Exception {
        GuiSwingViewStringField fld = new GuiSwingViewStringField();

        TestStringProp prop = new TestStringProp();
        GuiMappingContext context = new GuiMappingContext(prop);
        context.setRepresentation(new GuiReprValueStringField());
        context.updateSourceFromRoot();

        JComponent c = (JComponent) runGet(() -> {
            JComponent comp = fld.createView(context);
            testFrame(comp).setSize(400, 100);
            return comp;
        });

        SearchTextField t = runQuery(c, query(SearchTextField.class, 0));

        run(() -> t.getField().setText("hello"));

        Thread.sleep(1000);

        Assert.assertEquals("hello", prop.value);

        prop.value = "HELLO";
        run(context::updateSourceFromRoot);
    }

    public static class TestStringProp extends GuiTypeMemberProperty {
        public String value;
        public TestStringProp() {
            super("hello");
            setType(new GuiTypeValue(String.class));
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            this.value = (String) value;
            return null;
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, this.value);
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }
}
