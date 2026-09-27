package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingViewBooleanCheckBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    public GuiSwingViewBooleanCheckBoxTest() {
    }

    @BeforeEach
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);

        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextProp = context.getChildByName("value");

        box = new GuiSwingViewBooleanCheckBox();

    }

    @AfterEach
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

        public TestObj() {
        }
    }

    public GuiSwingViewBooleanCheckBox.PropertyCheckBox create() {
        JComponent c = box.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findDescendantByType(c,
                GuiSwingViewBooleanCheckBox.PropertyCheckBox.class);
    }

    @Test
    public void testViewBooleanValueUpdate() {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

        run(this::runWait);
        obj.value = true;
        contextProp.updateSourceFromRoot();

        assertTrue(runGet(propBox::isSelected),
                "listener update");
    }

    @Test
    public void testViewBooleanValueClick() {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

        run(propBox::doClick);

        assertTrue(obj.value,
                "update after click");
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

        assertTrue(runGet(propBox::isSelected),
                "refresh menu reflects model state");
    }

    @Test
    public void testViewBooleanValuePopupCopy() {
        withClipLock(() -> {
            GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

            GuiSwingView.ToStringCopyAction a = runGet(() -> findMenuItemAction(propBox.getSwingStaticMenuItems(),
                    GuiSwingView.ToStringCopyAction.class));

            run(() ->
                    a.actionPerformed(null));

            assertEquals(
                    "false",
                    runGet(this::getClipboardText), "copy text");

            run(() -> propBox.setSwingViewValue(true));

            run(() ->
                    a.actionPerformed(null));

            assertEquals(
                    "true",
                    runGet(this::getClipboardText), "copy text after update");
        });
    }

    @Test
    public void testViewBooleanValueTransferHandlerImport() {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

        TransferHandler.TransferSupport h = new TransferHandler.TransferSupport(propBox,
                new StringSelection("true"));

        assertTrue(
                runGet(() -> propBox.getTransferHandler()
                        .canImport(h)), "importable");

        assertTrue(
                runGet(() ->
                        propBox.getTransferHandler()
                                .importData(h)), "importData");

        assertTrue(runGet(() -> obj.value),
                "paste by transferHandler");

        assertTrue(runGet(propBox::isSelected),
                "paste by transferHandler, update selection");
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
            assertEquals(
                    "false",
                    runGet(this::getClipboardText), "exportData");
        });
    }
}
