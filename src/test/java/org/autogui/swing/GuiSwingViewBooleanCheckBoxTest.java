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
import java.awt.datatransfer.StringSelection;

public class GuiSwingViewBooleanCheckBoxTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        GuiSwingViewBooleanCheckBoxTest test = new GuiSwingViewBooleanCheckBoxTest();
        test.setUp();
        test.testViewBooleanValuePopupCopy();
    }

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;
    TestObj obj;

    GuiSwingViewBooleanCheckBox box;

    JFrame frame;

    public GuiSwingViewBooleanCheckBoxTest() {}

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);

        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextProp = context.getChildByName("value");

        box = new GuiSwingViewBooleanCheckBox();

    }

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded
        public boolean value;
        public TestObj() {}
    }

    /////////

    public GuiSwingViewBooleanCheckBox.PropertyCheckBox create() {
        JComponent c = box.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findChildByType(c,
                        GuiSwingViewBooleanCheckBox.PropertyCheckBox.class);
    }

    @Test
    public void testViewBooleanValueUpdate() {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

        run(this::runWait);
        obj.value = true;
        contextProp.updateSourceFromRoot();

        Assert.assertTrue("listener update",
                runGet(propBox::isSelected));
    }

    @Test
    public void testViewBooleanValueClick() {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

        run(propBox::doClick);

        Assert.assertTrue("update after click",
                obj.value);
    }

    @Test
    public void testViewBooleanValuePopupRefresh() {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

        run(() -> {
            obj.value = true;
            GuiSwingView.ContextRefreshAction a = findMenuItemAction(propBox.getSwingStaticMenuItems(),
                    GuiSwingView.ContextRefreshAction.class);
            a.actionPerformed(null);
        });

        Assert.assertTrue("refresh menu reflects model state",
                runGet(propBox::isSelected));
    }

    @Test
    public void testViewBooleanValuePopupCopy() {
        withClipLock(() -> {
            GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

            GuiSwingView.ToStringCopyAction a = runGet(() -> findMenuItemAction(propBox.getSwingStaticMenuItems(),
                    GuiSwingView.ToStringCopyAction.class));

            run(() ->
                a.actionPerformed(null));

            Assert.assertEquals("copy text",
                    "false",
                    runGet(this::getClipboardText));

            run(() -> propBox.setSwingViewValue(true));

            run(() ->
                a.actionPerformed(null));

            Assert.assertEquals("copy text after update",
                    "true",
                    runGet(this::getClipboardText));
        });
    }

    @Test
    public void testViewBooleanValueTransferHandlerImport() {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

        TransferHandler.TransferSupport h = new TransferHandler.TransferSupport(propBox,
                new StringSelection("true"));

        Assert.assertTrue("importable",
                runGet(() -> propBox.getTransferHandler()
                    .canImport(h)));

        Assert.assertTrue("importData",
                runGet(() ->
                propBox.getTransferHandler()
                        .importData(h)));

        Assert.assertTrue("paste by transferHandler",
                runGet(() -> obj.value));

        Assert.assertTrue("paste by transferHandler, update selection",
                runGet(propBox::isSelected));
    }

    @Test
    public void testViewBooleanValueTransferHandlerExport() {
        withClipLock(() -> {
            System.err.println("beforeCreate");
            GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);
            System.err.println("beforeRun");
            run(() ->
                    propBox.getTransferHandler()
                            .exportToClipboard(propBox,
                                    Toolkit.getDefaultToolkit().getSystemClipboard(),
                                    TransferHandler.COPY));
            System.err.println("beforeRunGet");
            Assert.assertEquals("exportData",
                    "false",
                    runGet(this::getClipboardText));
        });
    }
}
