package org.autogui.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class GuiSwingViewLabelTest extends GuiSwingTestCase {
    GuiTypeBuilder typeBuilder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;

    TestObj obj;

    GuiSwingViewLabel label;

    JFrame frame;

    @Before
    public void setUp() {

        typeBuilder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) typeBuilder.get(TestObj.class);

        obj = new TestObj();
        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextProp = context.getChildByName("value");

        label = new GuiSwingViewLabel();

    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded
        public Object value = new TestValue("hello");
    }

    public static class TestValue {
        String value;

        public TestValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "<" + value + ">";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestValue testValue = (TestValue) o;
            return Objects.equals(value, testValue.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }


    public GuiSwingViewLabel.PropertyLabel create() {
        JComponent c = label.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findChildByType(c, GuiSwingViewLabel.PropertyLabel.class);
    }

    @Test
    public void testViewUpdate() {
        GuiSwingViewLabel.PropertyLabel pane = runGet(this::create);
        contextProp.updateSourceFromRoot();
        runWait();
        Assert.assertEquals("label init value",
                new TestValue("hello"),
                runGet(pane::getSwingViewValue));

        obj.value = "<html><body>hello</body></html>";
        contextProp.updateSourceFromRoot();
        runWait();

        Assert.assertEquals("label updated value ignoring html",
                "<html><body>hello</body></html>",
                runGet(pane::getSwingViewValue));
    }

    @Test
    public void testViewGetValueAsString() {
        GuiSwingViewLabel.PropertyLabel pane = runGet(this::create);
        contextProp.updateSourceFromRoot();
        runWait();

        Assert.assertEquals("label init value",
                "<hello>",
                runGet(pane::getValueAsString));
    }

    @Test
    public void testViewToStringCopy() {
        GuiSwingViewLabel.PropertyLabel pane = runGet(this::create);
        contextProp.updateSourceFromRoot();
        runWait();

        GuiSwingViewLabel.LabelToStringCopyAction a = runGet(() ->
                findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingViewLabel.LabelToStringCopyAction.class));
        run(() -> a.actionPerformed(null));

        String copied = runGet(this::getClipboardText);
        Assert.assertEquals("copy toString",
                "<hello>",
                copied);
    }

    @Test
    public void testViewToStringCopyJson() {
        GuiSwingViewLabel.PropertyLabel pane = runGet(this::create);
        contextProp.updateSourceFromRoot();
        runWait();

        GuiSwingViewLabel.LabelJsonCopyAction a = runGet(() ->
                findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingViewLabel.LabelJsonCopyAction.class));
        run(() -> a.actionPerformed(null));

        String copied = runGet(this::getClipboardText);
        Assert.assertEquals("copy toString JSON as String",
                "\"<hello>\"",
                copied);
    }

    @Test
    public void testViewToStringSaveJson() {
        GuiSwingViewLabel.PropertyLabel pane = runGet(this::create);
        contextProp.updateSourceFromRoot();
        runWait();

        GuiSwingViewLabel.LabelJsonSaveAction a = runGet(() ->
                findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingViewLabel.LabelJsonSaveAction.class));
        Object json = runGet(() -> a.toCopiedJson(pane.getSwingViewValue()));

        String copied = runGet(this::getClipboardText);
        Assert.assertEquals("save toString JSON as String",
                "<hello>",
                json);
    }

    @Test
    public void testViewTransferHandlerExport() {
        GuiSwingViewLabel.PropertyLabel i = runGet(this::create);
        contextProp.updateSourceFromRoot();
        runWait();

        run(() ->
                i.getTransferHandler()
                        .exportToClipboard(i,
                                Toolkit.getDefaultToolkit().getSystemClipboard(),
                                TransferHandler.COPY));

        Assert.assertEquals("after export",
                "<hello>",
                runGet(this::getClipboardText));
    }
}
