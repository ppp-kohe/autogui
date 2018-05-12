package autogui.swing;

import autogui.GuiIncluded;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.type.GuiTypeBuilder;
import autogui.base.type.GuiTypeObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

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

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);

        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextProp = context.getChildByName("value");

        box = new GuiSwingViewEnumComboBox();
    }

    @After
    public void tearDown() {
        frame.dispose();
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded
        public TestEnum value;
    }

    public enum TestEnum {
        Hello, World
    }

    /////////////


    public GuiSwingViewEnumComboBox.PropertyEnumComboBox create() {
        JComponent c = box.createView(contextProp, () -> GuiReprValue.NONE);
        frame = createFrame(c);
        return GuiSwingView.findChildByType(c, GuiSwingViewEnumComboBox.PropertyEnumComboBox.class);
    }

    @Test
    public void testViewEnumValueUpdate() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        run(this::runWait);
        obj.value = TestEnum.Hello;
        contextProp.updateSourceFromRoot();

        Assert.assertEquals("listener update",
                TestEnum.Hello,
                runGet(propBox::getSwingViewValue));

    }

    @Test
    public void testViewEnumValueItems() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        Assert.assertEquals("item size",
                2,
                runGet(propBox::getItemCount).longValue());

    }

    @Test
    public void testViewEnumValueSelect() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        run(() -> propBox.setSelectedIndex(1));
        run(this::runWait);
        Assert.assertEquals("update after select",
                TestEnum.World,
                obj.value);
    }


    @Test
    public void testViewEnumValueSetValue() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        run(() -> propBox.setSwingViewValueWithUpdate(TestEnum.World));
        run(this::runWait);
        Assert.assertEquals("update after set",
                TestEnum.World,
                obj.value);
    }

    @Test
    public void testViewEnumValueCopyAsJson() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        run(() -> propBox.setSelectedIndex(0));

        GuiSwingJsonTransfer.JsonCopyAction a = runGet(() ->
                findMenuItemAction(propBox.getSwingStaticMenuItems(),
                    GuiSwingJsonTransfer.JsonCopyAction.class));
        run(() -> a.actionPerformed(null));

        Assert.assertEquals("copy as json",
                "\"Hello\"",
                runGet(this::getClipboardText));
    }

    @Test
    public void testViewEnumValuePasteJson() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        run(() -> setClipboardText("{\"value\":\"Hello\"}"));
        GuiSwingJsonTransfer.JsonPasteAction a = runGet(() ->
            findMenuItemAction(propBox.getSwingStaticMenuItems(),
                    GuiSwingJsonTransfer.JsonPasteAction.class));
        run(() -> a.actionPerformed(null));

        Assert.assertEquals("selected after paste {name:enumMemberName}",
                TestEnum.Hello,
                runGet(propBox::getSwingViewValue));

        Assert.assertEquals("selected index",
                0,
                runGet(propBox::getSelectedIndex).intValue());
    }

    @Test
    public void testViewEnumValueTransferHandlerImport() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        TransferHandler.TransferSupport t = new TransferHandler.TransferSupport(propBox,
                new StringSelection("Hello"));
        Assert.assertTrue("importable",
                runGet(() -> propBox.getTransferHandler().canImport(t)));

        Assert.assertTrue("import",
                runGet(() -> propBox.getTransferHandler().importData(t)));

        Assert.assertEquals("after import",
                TestEnum.Hello,
                runGet(propBox::getSwingViewValue));
    }

    @Test
    public void testViewEnumValueTransferHandlerImportIgnoringCase() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        TransferHandler.TransferSupport t = new TransferHandler.TransferSupport(propBox,
                new StringSelection("HeLLo"));

        Assert.assertTrue("import",
                runGet(() -> propBox.getTransferHandler().importData(t)));

        Assert.assertEquals("after import",
                TestEnum.Hello,
                runGet(propBox::getSwingViewValue));
    }

    @Test
    public void testViewEnumValueTransferHandlerImportOrdinal() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        TransferHandler.TransferSupport t = new TransferHandler.TransferSupport(propBox,
                new StringSelection("1"));

        Assert.assertTrue("import",
                runGet(() -> propBox.getTransferHandler().importData(t)));

        Assert.assertEquals("after import",
                TestEnum.World,
                runGet(propBox::getSwingViewValue));
    }

    @Test
    public void testViewEnumValueTransferHandlerExport() {
        GuiSwingViewEnumComboBox.PropertyEnumComboBox propBox = runGet(this::create);

        run(() -> propBox.setSwingViewValueWithUpdate(TestEnum.World));
        run(() ->
                propBox.getTransferHandler()
                    .exportToClipboard(propBox,
                            Toolkit.getDefaultToolkit().getSystemClipboard(),
                            TransferHandler.COPY));
        Assert.assertEquals("exportData: name of enum",
                "World",
                runGet(this::getClipboardText));
    }
}
