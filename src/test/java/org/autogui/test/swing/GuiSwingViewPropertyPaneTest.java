package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprObjectPane;
import org.autogui.base.mapping.GuiReprPropertyPane;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.*;
import org.autogui.swing.util.PopupCategorized;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GuiSwingViewPropertyPaneTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        GuiSwingViewPropertyPaneTest test = new GuiSwingViewPropertyPaneTest();
        test.setUp();
        test.testViewPropPanePopup();
    }

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;

    TestObj obj;

    GuiSwingViewPropertyPane prop;

    JFrame frame;

    public GuiSwingViewPropertyPaneTest() {
    }

    @BeforeEach
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);

        GuiReprPropertyPane propPane = new GuiReprPropertyPane(GuiSwingMapperSet.getReprDefaultSet());
        GuiReprObjectPane objPane = new GuiReprObjectPane(propPane);

        objPane.match(context);

        contextProp = context.getChildByName("value");

        prop = new GuiSwingViewPropertyPane(GuiSwingMapperSet.getDefaultMapperSet());
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

    public GuiSwingViewPropertyPane.PropertyPane create() {
        JComponent c = prop.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findDescendantByType(c, GuiSwingViewPropertyPane.PropertyPane.class);

    }

    @Test
    public void testViewPropPaneUpdate() {
        GuiSwingViewPropertyPane.PropertyPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        obj.value = "Hello";
        run(contextProp::updateSourceFromRoot);

        assertEquals(new GuiReprValue.NamedValue("value", "Hello"), runGet(pane::getSwingViewValue), "as named value");

        assertEquals("Hello", runGet(strPane::getSwingViewValue), "also update content-pane");
    }


    @Test
    public void testViewPropPaneSet() {
        GuiSwingViewPropertyPane.PropertyPane pane = create();

        obj.value = "Hello";
        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        EditWait wait = editWait(strPane.getEditingRunner());
        run(() -> pane.setSwingViewValue(new GuiReprValue.NamedValue("value", "World")));
        wait.awaitNextFinish();

        assertEquals("World", runGet(() -> strPane.getField().getText()), "set content-pane text");

        assertEquals("Hello", obj.value, "set value without update");
    }

    @Test
    public void testViewPropPaneSetWithUpdate() {
        GuiSwingViewPropertyPane.PropertyPane pane = create();

        obj.value = "Hello";
        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        EditWait wait = editWait(strPane.getEditingRunner());

        run(() -> pane.setSwingViewValueWithUpdate(new GuiReprValue.NamedValue("value", "World")));
        wait.awaitNextFinish();

        assertEquals("World", runGet(() -> strPane.getField().getText()), "set content-pane text");

        assertEquals("World", obj.value, "set value with update");
    }

    @Test
    public void testViewPropPanePopup() {
        withClipLock(() -> {
            GuiSwingViewPropertyPane.PropertyPane pane = create();

            GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));
            run(() -> strPane.setSwingViewValue("Hello"));

            Iterable<PopupCategorized.CategorizedMenuItem> items = runGet(() -> ((PopupCategorized) pane.getSwingMenuBuilder()).getItemSupplier().get());

            GuiSwingJsonTransfer.JsonCopyAction a = runGet(() -> findMenuItemAction(items, GuiSwingJsonTransfer.JsonCopyAction.class, "Property Edit", null, null));

            run(() -> a.actionPerformed(null));

            assertEquals("{\"value\":\"Hello\"}", runGet(() -> getClipboardText().replaceAll("\\s+", "")), "property json copy");
        });
    }
}
