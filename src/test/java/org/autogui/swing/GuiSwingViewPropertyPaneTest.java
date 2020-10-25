package org.autogui.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprObjectPane;
import org.autogui.base.mapping.GuiReprPropertyPane;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.util.PopupCategorized;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

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

    @Before
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

    public GuiSwingViewPropertyPane.PropertyPane create() {
        JComponent c = prop.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findChildByType(c,
                GuiSwingViewPropertyPane.PropertyPane.class);

    }

    @Test
    public void testViewPropPaneUpdate() {
        GuiSwingViewPropertyPane.PropertyPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findChildByType(pane,
                GuiSwingViewStringField.PropertyStringPane.class));

        obj.value = "Hello";
        run(contextProp::updateSourceFromRoot);

        Assert.assertEquals("as named value",
                new GuiReprValue.NamedValue("value", "Hello"),
                runGet(pane::getSwingViewValue));

        Assert.assertEquals("also update content-pane",
                "Hello",
                runGet(strPane::getSwingViewValue));
    }


    @Test
    public void testViewPropPaneSet() {
        GuiSwingViewPropertyPane.PropertyPane pane = create();

        obj.value = "Hello";
        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findChildByType(pane,
                GuiSwingViewStringField.PropertyStringPane.class));

        run(() -> pane.setSwingViewValue(
                new GuiReprValue.NamedValue("value", "World")));
        runWait(500);

        Assert.assertEquals("set content-pane text",
                "World",
                runGet(() -> strPane.getField().getText()));

        Assert.assertEquals("set value without update",
                "Hello",
                obj.value);
    }

    @Test
    public void testViewPropPaneSetWithUpdate() {
        GuiSwingViewPropertyPane.PropertyPane pane = create();

        obj.value = "Hello";
        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findChildByType(pane,
                GuiSwingViewStringField.PropertyStringPane.class));

        run(() -> pane.setSwingViewValueWithUpdate(
                new GuiReprValue.NamedValue("value", "World")));
        runWait(500);

        Assert.assertEquals("set content-pane text",
                "World",
                runGet(() -> strPane.getField().getText()));

        Assert.assertEquals("set value with update",
                "World",
                obj.value);
    }

    @Test
    public void testViewPropPanePopup() {
        GuiSwingViewPropertyPane.PropertyPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findChildByType(pane,
                GuiSwingViewStringField.PropertyStringPane.class));
        run(() -> strPane.setSwingViewValue("Hello"));

        Iterable<PopupCategorized.CategorizedMenuItem> items = runGet(() -> ((PopupCategorized) pane.getSwingMenuBuilder()).getItemSupplier().get());

        GuiSwingJsonTransfer.JsonCopyAction a = runGet(() -> findMenuItemAction(items,
                GuiSwingJsonTransfer.JsonCopyAction.class, "Property Edit", null, null));

        run(() -> a.actionPerformed(null));

        Assert.assertEquals("property json copy",
                "{\"value\":\"Hello\"}",
                runGet(() -> getClipboardText().replaceAll("\\s+", "")));
    }
}
