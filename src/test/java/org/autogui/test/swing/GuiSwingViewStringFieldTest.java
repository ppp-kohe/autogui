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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    public GuiSwingViewStringFieldTest() {
    }

    @BeforeEach
    public void setUp() {
        timeFactor = 1;
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextProp = context.getChildByName("value");

        field = new GuiSwingViewStringField();
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
        public String value;

        public TestObj() {
        }
    }

    //------------------

    public GuiSwingViewStringField.PropertyStringPane create() {
        JComponent c = field.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findDescendantByType(c, GuiSwingViewStringField.PropertyStringPane.class);
    }

    @Test
    public void testViewStringValueUpdate() {
        GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

        run(this::runWait);
        obj.value = "hello";
        contextProp.updateSourceFromRoot();

        assertEquals("hello", runGet(propField::getSwingViewValue), "listener update");
    }

    @Test
    public void testViewStringValueType() {
        GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);
        EditWait wait = editWait(propField.getEditingRunner());
        run(() -> propField.getField().setText("hello"));
        wait.awaitNextFinish();
        assertEquals("hello", obj.value, "update after set and waiting >500ms");
    }

    @Test
    public void testViewStringValueCopy() {
        withClipLock(() -> {
            GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

            run(() -> propField.setSwingViewValue("hello"));
            runWait();
            run(() -> {
                propField.getField().setSelectionStart(2);
                propField.getField().setSelectionEnd(4);
            });

            run(() -> {
                PopupExtensionText.TextCopyAction a = findMenuItemAction(propField.getMenuItems().get(), PopupExtensionText.TextCopyAction.class);
                a.actionPerformed(null);
            });

            assertEquals("ll", getClipboardText(), "copy selected range");
        });
    }

    @Test
    public void testViewStringValueCut() {
        withClipLock(() -> {
            GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

            run(() -> propField.setSwingViewValue("hello"));
            runWait();
            run(() -> {
                propField.getField().setSelectionStart(2);
                propField.getField().setSelectionEnd(4);
            });

            run(() -> {
                PopupExtensionText.TextCutAction a = findMenuItemAction(propField.getMenuItems().get(), PopupExtensionText.TextCutAction.class);
                a.actionPerformed(null);
            });

            assertEquals("ll", getClipboardText(), "cut selected range");
            assertEquals("heo", runGet(propField::getSwingViewValue), "after cut");
            runWait(600);
            assertEquals("heo", obj.value, "update after paste");
        });
    }

    @Test
    public void testViewStringValueCopyAll() {
        withClipLock(() -> {
            GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

            run(() -> propField.setSwingViewValue("hello"));
            runWait();
            run(() -> {
                propField.getField().setSelectionStart(2);
                propField.getField().setSelectionEnd(4);
            });

            run(() -> {
                PopupExtensionText.TextCopyAllAction a = findMenuItemAction(propField.getMenuItems().get(), PopupExtensionText.TextCopyAllAction.class);
                a.actionPerformed(null);
            });

            assertEquals("hello", getClipboardText(), "copy entire text");
        });
    }

    @Test
    public void testViewStringValuePaste() {
        withClipLock(() -> {
            GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);
            EditWait wait = editWait(propField.getEditingRunner());

            run(() -> propField.setSwingViewValue("hello"));
            runWait();
            run(() -> {
                propField.getField().setSelectionStart(2);
                propField.getField().setSelectionEnd(4);
            });

            run(() -> setClipboardText("LL"));
            run(() -> {
                PopupExtensionText.TextPasteAction a = findMenuItemAction(propField.getMenuItems().get(), PopupExtensionText.TextPasteAction.class);
                a.actionPerformed(null);
            });
            runWait();
            assertEquals("heLLo", runGet(propField::getSwingViewValue), "paste text");
            wait.awaitNextFinish();
            assertEquals("heLLo", obj.value, "update after paste");
        });
    }

    @Test
    public void testViewStringValuePasteAll() {
        withClipLock(() -> {
            GuiSwingViewStringField.PropertyStringPane propField = runGet(this::create);

            run(() -> propField.setSwingViewValue("hello"));
            runWait();
            run(() -> {
                propField.getField().setSelectionStart(2);
                propField.getField().setSelectionEnd(4);
            });

            run(() -> setClipboardText("LL"));
            run(() -> {
                PopupExtensionText.TextPasteAllAction a = findMenuItemAction(propField.getMenuItems().get(), PopupExtensionText.TextPasteAllAction.class);
                a.actionPerformed(null);
            });
            runWait();
            assertEquals("LL", runGet(propField::getSwingViewValue), "paste text");
            runWait(600);
            assertEquals("LL", obj.value, "update after paste");
        });
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
            PopupExtensionText.TextSelectAllAction a = findMenuItemAction(propField.getMenuItems().get(), PopupExtensionText.TextSelectAllAction.class);
            a.actionPerformed(null);
        });
        runWait();
        assertEquals(0, (int) runGet(() -> propField.getField().getSelectionStart()), "selection start by select-all");
        assertEquals(5, (int) runGet(() -> propField.getField().getSelectionEnd()), "selection end by select-all");
    }
}
