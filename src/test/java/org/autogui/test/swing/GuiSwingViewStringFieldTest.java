package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingViewStringField;
import org.autogui.swing.util.PopupExtensionText;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

public class GuiSwingViewStringFieldTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        GuiSwingViewStringFieldTest test = new GuiSwingViewStringFieldTest();
        test.setUp();
        test.testViewStringValueUpdate();
    }

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;
    TestObj obj;

    GuiSwingViewStringField field;

    JFrame frame;

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextProp = context.getChildByName("value");

        field = new GuiSwingViewStringField();
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
        public String value;
    }

    //////////

    public GuiSwingViewStringField.PropertyStringPane create() {
        JComponent c = field.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findChildByType(c, GuiSwingViewStringField.PropertyStringPane.class);
    }

    @Test
    public void testViewStringValueUpdate() {
        GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

        run(this::runWait);
        obj.value = "hello";
        contextProp.updateSourceFromRoot();

        Assert.assertEquals("listener update",
                "hello",
                runGet(propField::getSwingViewValue));
    }

    @Test
    public void testViewStringValueType() {
        GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);
        run(() -> propField.getField().setText("hello"));
        runWait(600);
        Assert.assertEquals("update after set and waiting >500ms",
                "hello",
                obj.value);
    }

    @Test
    public void testViewStringValueCopy() {
        GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

        run(() -> propField.setSwingViewValue("hello"));
        runWait();
        run(() -> {
            propField.getField().setSelectionStart(2);
            propField.getField().setSelectionEnd(4);
        });

        run(() -> {
            PopupExtensionText.TextCopyAction a = findMenuItemAction(propField.getMenuItems().get(),
                    PopupExtensionText.TextCopyAction.class);
            a.actionPerformed(null);
        });

        Assert.assertEquals("copy selected range",
                "ll", getClipboardText());
    }

    @Test
    public void testViewStringValueCut() {
        GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

        run(() -> propField.setSwingViewValue("hello"));
        runWait();
        run(() -> {
            propField.getField().setSelectionStart(2);
            propField.getField().setSelectionEnd(4);
        });

        run(() -> {
            PopupExtensionText.TextCutAction a = findMenuItemAction(propField.getMenuItems().get(),
                    PopupExtensionText.TextCutAction.class);
            a.actionPerformed(null);
        });

        Assert.assertEquals("cut selected range",
                "ll", getClipboardText());
        Assert.assertEquals("after cut",
                "heo",
                runGet(propField::getSwingViewValue));
        runWait(600);
        Assert.assertEquals("update after paste",
                "heo",
                obj.value);
    }

    @Test
    public void testViewStringValueCopyAll() {
        GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

        run(() -> propField.setSwingViewValue("hello"));
        runWait();
        run(() -> {
            propField.getField().setSelectionStart(2);
            propField.getField().setSelectionEnd(4);
        });

        run(() -> {
            PopupExtensionText.TextCopyAllAction a = findMenuItemAction(propField.getMenuItems().get(),
                    PopupExtensionText.TextCopyAllAction.class);
            a.actionPerformed(null);
        });

        Assert.assertEquals("copy entire text",
                "hello", getClipboardText());
    }

    @Test
    public void testViewStringValuePaste() {
        GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

        run(() -> propField.setSwingViewValue("hello"));
        runWait();
        run(() -> {
            propField.getField().setSelectionStart(2);
            propField.getField().setSelectionEnd(4);
        });

        run(() ->setClipboardText("LL"));
        run(() -> {
            PopupExtensionText.TextPasteAction a = findMenuItemAction(propField.getMenuItems().get(),
                    PopupExtensionText.TextPasteAction.class);
            a.actionPerformed(null);
        });
        runWait();
        Assert.assertEquals("paste text",
                "heLLo", runGet(propField::getSwingViewValue));
        runWait(600);
        Assert.assertEquals("update after paste",
                "heLLo",
                obj.value);
    }

    @Test
    public void testViewStringValuePasteAll() {
        GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

        run(() -> propField.setSwingViewValue("hello"));
        runWait();
        run(() -> {
            propField.getField().setSelectionStart(2);
            propField.getField().setSelectionEnd(4);
        });

        run(() ->setClipboardText("LL"));
        run(() -> {
            PopupExtensionText.TextPasteAllAction a = findMenuItemAction(propField.getMenuItems().get(),
                    PopupExtensionText.TextPasteAllAction.class);
            a.actionPerformed(null);
        });
        runWait();
        Assert.assertEquals("paste text",
                "LL", runGet(propField::getSwingViewValue));
        runWait(600);
        Assert.assertEquals("update after paste",
                "LL",
                obj.value);
    }

    @Test
    public void testViewStringValueSelectAll() {
        GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

        run(() -> propField.setSwingViewValue("hello"));
        runWait();
        run(() -> {
            propField.getField().setSelectionStart(2);
            propField.getField().setSelectionEnd(4);
        });

        run(() -> {
            PopupExtensionText.TextSelectAllAction a = findMenuItemAction(propField.getMenuItems().get(),
                    PopupExtensionText.TextSelectAllAction.class);
            a.actionPerformed(null);
        });
        runWait();
        Assert.assertEquals("selection start by select-all",
                0,
                (int) runGet(() -> propField.getField().getSelectionStart()));
        Assert.assertEquals("selection end by select-all",
                5,
                (int) runGet(() -> propField.getField().getSelectionEnd()));
    }
}
