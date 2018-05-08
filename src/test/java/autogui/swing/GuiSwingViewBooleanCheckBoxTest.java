package autogui.swing;

import autogui.GuiIncluded;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.type.GuiTypeBuilder;
import autogui.base.type.GuiTypeObject;
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


    @GuiIncluded
    public static class TestObj {
        @GuiIncluded
        public boolean value;
    }

    /////////

    public GuiSwingViewBooleanCheckBox.PropertyCheckBox create() {
        JComponent c = box.createView(contextProp, () -> GuiReprValue.NONE);
        frame = createFrame(c);
        return GuiSwingViewBooleanCheckBox.PropertyCheckBox.class.cast(
                GuiSwingView.findChild(c,
                        GuiSwingViewBooleanCheckBox.PropertyCheckBox.class::isInstance));
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
        GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

        GuiSwingView.ToStringCopyAction a = runGet(() -> findMenuItemAction(propBox.getSwingStaticMenuItems(),
                GuiSwingView.ToStringCopyAction.class));

        run(() -> {
            a.actionPerformed(null);
        });

        Assert.assertEquals("copy text",
                "false",
                runGet(this::getClipboardText));

        run(() -> propBox.setSwingViewValue(true));

        run(() -> {
            a.actionPerformed(null);
        });

        Assert.assertEquals("copy text after update",
                "true",
                runGet(this::getClipboardText));

    }

    @Test
    public void testViewBooleanValueTransferHandlerImport() {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

        Assert.assertTrue("importData",
                runGet(() ->
                propBox.getTransferHandler()
                        .importData(new TransferHandler.TransferSupport(propBox,
                                new StringSelection("true")))));

        Assert.assertTrue("paste by transferHandler",
                runGet(() -> obj.value));

        Assert.assertTrue("paste by transferHandler, update selection",
                runGet(propBox::isSelected));
    }

    @Test
    public void testViewBooleanValueTransferHandlerExport() {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox propBox = runGet(this::create);

        run(() ->
                propBox.getTransferHandler()
                        .exportToClipboard(propBox,
                                Toolkit.getDefaultToolkit().getSystemClipboard(),
                                TransferHandler.COPY));
        Assert.assertEquals("exportData",
                "false",
                getClipboardText());
    }
}
