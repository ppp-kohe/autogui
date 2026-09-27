package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingJsonTransfer;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingViewEnumComboBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuiSwingViewEnumComboBoxTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        GuiSwingViewEnumComboBoxTest test = new GuiSwingViewEnumComboBoxTest();
        test.setUp();
        test.testViewEnumValueSelect();
    }

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;
    TestObj obj;

    GuiSwingViewEnumComboBox box;

    JFrame frame;

    public GuiSwingViewEnumComboBoxTest() {
    }

    @BeforeEach
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);

        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextProp = context.getChildByName("value");

        box = new GuiSwingViewEnumComboBox();
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
        public TestEnum value;

        public TestObj() {
        }
    }

    public enum TestEnum {
        Hello, World
    }

    //------------------


    public GuiSwingViewEnumComboBox.PropertyEnumComboBox create() {
        JComponent c = box.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findDescendantByType(c, GuiSwingViewEnumComboBox.PropertyEnumComboBox.class);
    }

    @Test
    public void testViewEnumValueUpdate() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        run(this::runWait);
        obj.value = TestEnum.Hello;
        contextProp.updateSourceFromRoot();

        assertEquals(TestEnum.Hello, runGet(propBox::getSwingViewValue), "listener update");

    }

    @Test
    public void testViewEnumValueItems() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        assertEquals(2, runGet(propBox::getItemCount).longValue(), "item size");

    }

    @Test
    public void testViewEnumValueSelect() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        run(() -> propBox.setSelectedIndex(1));
        run(this::runWait);
        assertEquals(TestEnum.World, obj.value, "update after select");
    }


    @Test
    public void testViewEnumValueSetValue() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        run(() -> propBox.setSwingViewValueWithUpdate(TestEnum.World));
        run(this::runWait);
        assertEquals(TestEnum.World, obj.value, "update after set");
    }

    @Test
    public void testViewEnumValueCopyAsJson() {
        withClipLock(() -> {
            GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

            run(() -> propBox.setSelectedIndex(0));

            GuiSwingJsonTransfer.JsonCopyAction a = runGet(() -> findMenuItemAction(propBox.getSwingStaticMenuItems(), GuiSwingJsonTransfer.JsonCopyAction.class));
            run(() -> a.actionPerformed(null));

            assertEquals("\"Hello\"", runGet(this::getClipboardText), "copy as json");
        });
    }

    @Test
    public void testViewEnumValuePasteJson() {
        withClipLock(() -> {
            GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

            run(() -> setClipboardText("{\"value\":\"Hello\"}"));
            GuiSwingJsonTransfer.JsonPasteAction a = runGet(() -> findMenuItemAction(propBox.getSwingStaticMenuItems(), GuiSwingJsonTransfer.JsonPasteAction.class));
            run(() -> a.actionPerformed(null));

            assertEquals(TestEnum.Hello, runGet(propBox::getSwingViewValue), "selected after paste {name:enumMemberName}");

            assertEquals(0, runGet(propBox::getSelectedIndex).intValue(), "selected index");
        });
    }

    @Test
    public void testViewEnumValueTransferHandlerImport() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        TransferHandler.TransferSupport t = new TransferHandler.TransferSupport(propBox, new StringSelection("Hello"));
        assertTrue(runGet(() -> propBox.getTransferHandler().canImport(t)), "importable");

        assertTrue(runGet(() -> propBox.getTransferHandler().importData(t)), "import");

        assertEquals(TestEnum.Hello, runGet(propBox::getSwingViewValue), "after import");
    }

    @Test
    public void testViewEnumValueTransferHandlerImportIgnoringCase() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        TransferHandler.TransferSupport t = new TransferHandler.TransferSupport(propBox, new StringSelection("HeLLo"));

        assertTrue(runGet(() -> propBox.getTransferHandler().importData(t)), "import");

        assertEquals(TestEnum.Hello, runGet(propBox::getSwingViewValue), "after import");
    }

    @Test
    public void testViewEnumValueTransferHandlerImportOrdinal() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        TransferHandler.TransferSupport t = new TransferHandler.TransferSupport(propBox, new StringSelection("1"));

        assertTrue(runGet(() -> propBox.getTransferHandler().importData(t)), "import");

        assertEquals(TestEnum.World, runGet(propBox::getSwingViewValue), "after import");
    }

    @Test
    public void testViewEnumValueTransferHandlerExport() {
        withClipLock(() -> {
            GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

            run(() -> propBox.setSwingViewValueWithUpdate(TestEnum.World));
            run(() -> propBox.getTransferHandler().exportToClipboard(propBox, Toolkit.getDefaultToolkit().getSystemClipboard(), TransferHandler.COPY));
            assertEquals("World", runGet(this::getClipboardText), "exportData: name of enum");
        });
    }
}
