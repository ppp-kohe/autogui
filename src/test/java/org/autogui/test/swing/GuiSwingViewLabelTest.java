package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingViewLabel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GuiSwingViewLabelTest extends GuiSwingTestCase {
    GuiTypeBuilder typeBuilder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;

    TestObj obj;

    GuiSwingViewLabel label;

    JFrame frame;

    public GuiSwingViewLabelTest() {
    }

    @BeforeEach
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

        public TestObj() {
        }
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

    @AfterEach
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }


    public GuiSwingViewLabel.PropertyLabel create() {
        JComponent c = label.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findDescendantByType(c, GuiSwingViewLabel.PropertyLabel.class);
    }

    @Test
    public void testViewUpdate() {
        GuiSwingViewLabel.PropertyLabel pane = runGet(this::create);
        contextProp.updateSourceFromRoot();
        runWait();
        assertEquals(new TestValue("hello"), runGet(pane::getSwingViewValue), "label init value");

        obj.value = "<html><body>hello</body></html>";
        contextProp.updateSourceFromRoot();
        runWait();

        assertEquals("<html><body>hello</body></html>", runGet(pane::getSwingViewValue), "label updated value ignoring html");
    }

    @Test
    public void testViewGetValueAsString() {
        GuiSwingViewLabel.PropertyLabel pane = runGet(this::create);
        contextProp.updateSourceFromRoot();
        runWait();

        assertEquals("<hello>", runGet(pane::getValueAsString), "label init value");
    }

    @Test
    public void testViewToStringCopy() {
        withClipLock(() -> {
            GuiSwingViewLabel.PropertyLabel pane = runGet(this::create);
            contextProp.updateSourceFromRoot();
            runWait();

            GuiSwingViewLabel.LabelToStringCopyAction a = runGet(() -> findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingViewLabel.LabelToStringCopyAction.class));
            run(() -> a.actionPerformed(null));

            String copied = runGet(this::getClipboardText);
            assertEquals("<hello>", copied, "copy toString");
        });
    }

    @Test
    public void testViewToStringCopyJson() {
        withClipLock(() -> {
            GuiSwingViewLabel.PropertyLabel pane = runGet(this::create);
            contextProp.updateSourceFromRoot();
            runWait();

            GuiSwingViewLabel.LabelJsonCopyAction a = runGet(() -> findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingViewLabel.LabelJsonCopyAction.class));
            run(() -> a.actionPerformed(null));

            String copied = runGet(this::getClipboardText);
            assertEquals("\"<hello>\"", copied, "copy toString JSON as String");
        });
    }

    @Test
    public void testViewToStringSaveJson() {
        withClipLock(() -> {
            GuiSwingViewLabel.PropertyLabel pane = runGet(this::create);
            contextProp.updateSourceFromRoot();
            runWait();

            GuiSwingViewLabel.LabelJsonSaveAction a = runGet(() -> findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingViewLabel.LabelJsonSaveAction.class));
            Object json = runGet(() -> a.toCopiedJson(pane.getSwingViewValue()));

            String copied = runGet(this::getClipboardText);
            assertEquals("<hello>", json, "save toString JSON as String");
        });
    }

    @Test
    public void testViewTransferHandlerExport() {
        withClipLock(() -> {
            GuiSwingViewLabel.PropertyLabel i = runGet(this::create);
            contextProp.updateSourceFromRoot();
            runWait();

            run(() -> i.getTransferHandler().exportToClipboard(i, Toolkit.getDefaultToolkit().getSystemClipboard(), TransferHandler.COPY));

            assertEquals("<hello>", runGet(this::getClipboardText), "after export");
        });
    }
}
