package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.JsonReader;
import org.autogui.base.mapping.*;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuiSwingViewObjectPaneTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        GuiSwingViewObjectPaneTest test = new GuiSwingViewObjectPaneTest();
        test.setUp();
        test.testViewObjPrefsSetSplit();
    }

    GuiTypeBuilder builder;

    GuiTypeObject typeObject;

    GuiMappingContext context;

    GuiReprObjectPane pane;
    GuiSwingViewObjectPane objPane;

    TestObj obj;
    JFrame frame;

    public GuiSwingViewObjectPaneTest() {
    }

    @BeforeEach
    public void setUp() {
        timeFactor = 1;
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        pane = new GuiReprObjectPane(GuiSwingMapperSet.getReprDefaultSet());

        obj = new TestObj();
        context = new GuiMappingContext(typeObject, obj);

        pane.match(context);

        objPane = new GuiSwingViewObjectPane(GuiSwingMapperSet.getDefaultMapperSet());
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
        @GuiIncluded(index = 0)
        public String hello;
        @GuiIncluded(index = 1)
        public boolean world;

        @GuiIncluded(index = 2)
        public TestSubObj obj1 = new TestSubObj();

        @GuiIncluded(index = 3)
        public TestSubObj obj2 = new TestSubObj();

        public int actionCount;

        @GuiIncluded
        public void say() {
            actionCount++;
            hello += "!";
        }

        public TestObj() {
        }
    }

    @GuiIncluded
    public static class TestSubObj {
        @GuiIncluded
        public String value;

        public TestSubObj() {
        }
    }

    public GuiSwingViewObjectPane.ObjectPane create() {
        JComponent c = objPane.createView(context, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findDescendantByType(c, GuiSwingViewObjectPane.ObjectPane.class);
    }

    @Test
    public void testViewObjCreate() {
        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

        assertEquals("hello", strPane.getSwingViewContext().getName(), "string property");
        assertTrue(strPane.getSwingViewContext().getRepresentation() instanceof GuiReprValueStringField, "string property");


        assertEquals("world", boolBox.getSwingViewContext().getName(), "boolean property");
        assertTrue(boolBox.getSwingViewContext().getRepresentation() instanceof GuiReprValueBooleanCheckBox, "boolean property");
    }

    @Test
    public void testViewObjUpdate() {
        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

        obj.hello = "Hello";
        obj.world = true;
        context.updateSourceFromRoot();

        assertEquals("Hello", runGet(strPane::getSwingViewValue), "updated str prop value");

        assertEquals(true, runGet(boolBox::getSwingViewValue), "updated boolean prop value");
    }

    @Test
    public void testViewObjUpdateObj() {
        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

        TestObj o = new TestObj();
        o.hello = "HELLO";
        o.world = true;
        run(() -> pane.setSwingViewValueWithUpdate(o));

        assertEquals("HELLO", runGet(strPane::getSwingViewValue), "updated str prop value");

        assertEquals(true, runGet(boolBox::getSwingViewValue), "updated boolean prop value");
    }

    @Test
    public void testViewObjSetProp() {
        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

        run(() -> strPane.setSwingViewValueWithUpdate("Hello"));
        run(() -> boolBox.setSwingViewValueWithUpdate(true));
        runWait();

        assertEquals("Hello", obj.hello, "set str prop value");

        assertTrue(obj.world, "set boolean prop value");

    }

    @Test
    public void testViewObjCopyAsJson() {
        withClipLock(() -> {
            GuiSwingViewObjectPane.ObjectPane pane = create();

            GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

            GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

            GuiSwingView.ValuePane<?> subPane1 = GuiSwingView.findDescendantValuePaneByType(pane, GuiSwingView.ValuePane.class, v -> v.getSwingViewContext().getName().equals("obj1"));
            GuiSwingView.ValuePane<?> subPane2 = GuiSwingView.findDescendantValuePaneByType(pane, GuiSwingView.ValuePane.class, v -> v.getSwingViewContext().getName().equals("obj2"));

            GuiSwingViewStringField.PropertyStringPane subStrPane1 = runGet(() -> GuiSwingView.findDescendantByType(subPane1.asSwingViewComponent(), GuiSwingViewStringField.PropertyStringPane.class));

            GuiSwingViewStringField.PropertyStringPane subStrPane2 = runGet(() -> GuiSwingView.findDescendantByType(subPane2.asSwingViewComponent(), GuiSwingViewStringField.PropertyStringPane.class));

            run(() -> boolBox.setSwingViewValueWithUpdate(true));
            run(() -> strPane.setSwingViewValueWithUpdate("value1"));
            run(() -> subStrPane1.setSwingViewValueWithUpdate("value2"));
            run(() -> subStrPane2.setSwingViewValueWithUpdate("value3"));
            runWait();

            GuiSwingJsonTransfer.JsonCopyAction a = runGet(() -> findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingJsonTransfer.JsonCopyAction.class));
            run(() -> a.actionPerformed(null));


            assertEquals("{" + "\"hello\":\"value1\"," + "\"world\":true," + "\"obj1\":{\"value\":\"value2\"}," + "\"obj2\":{\"value\":\"value3\"}" + "}", runGet(this::getClipboardText).replaceAll("\\s+", ""), "copy as json");
        });
    }

    @Test
    public void testViewObjPasteJson() {
        withClipLock(() -> {
            GuiSwingViewObjectPane.ObjectPane pane = create();

            run(() -> setClipboardText("{" + "\"obj2\":{\"value\":\"HELLO\"}," + "\"world\":true," + "\"obj1\":{\"value\":\"WORLD\"}," + "\"hello\":\"Hello\"" + "}"));

            GuiSwingJsonTransfer.JsonPasteAction a = runGet(() -> findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingJsonTransfer.JsonPasteAction.class));
            run(() -> a.actionPerformed(null));
            runWait();

            assertEquals("Hello", obj.hello, "after paste json");

            assertTrue(obj.world, "after paste json");

            assertEquals("HELLO", obj.obj2.value, "after paste json");

            assertEquals("WORLD", obj.obj1.value, "after paste json");
        });
    }

    @Test
    public void testViewObjAction() {
        GuiSwingViewObjectPane.ObjectPane pane = create();
        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));
        run(() -> strPane.setSwingViewValueWithUpdate("Hello"));
        runWait();

        GuiSwingActionDefault.ExecutionAction a = runGet(() -> findMenuItemAction(pane.getSwingStaticMenuItems(), GuiSwingActionDefault.ExecutionAction.class));
        run(() -> a.actionPerformed(null));

        assertEquals("Hello!", obj.hello, "execute action");

        assertEquals(1, obj.actionCount, "after execute action");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testViewObjPrefsSetSplit() {
        GuiPreferences prefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        context.setPreferences(prefs);

        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingPreferences swingPrefs = runGet(() -> new GuiSwingPreferences(context, pane));
        EditWait wait = editWait(swingPrefs.getUpdater());

        run(() -> pane.getSplitPanes().getFirst().setDividerLocation(0.7));
        wait.awaitNextFinish();

        Map<String, Object> obj = runGet(prefs::toJson);

        assertTrue(obj.get("$split") instanceof String, "divider location prefs converted as json string: " + obj.get("$split"));

        List<Map<String, Object>> list = (List<Map<String, Object>>) JsonReader.create((String) obj.get("$split")).parseValue();

        assertTrue(list.getFirst().containsKey("dividerLocation"), "divider location prefs");

        assertTrue((Boolean) list.getFirst().get("horizontal"), "divider location prefs");
    }

    @Test
    public void testViewObjPrefsSetHistory() {
        Instant startTime = Instant.now();

        GuiPreferences prefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        context.setPreferences(prefs);

        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() -> GuiSwingView.findDescendantByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

        GuiSwingView.ValuePane<?> subPane1 = GuiSwingView.findDescendantValuePaneByType(pane, GuiSwingView.ValuePane.class, v -> v.getSwingViewContext().getName().equals("obj1"));
        GuiSwingView.ValuePane<?> subPane2 = GuiSwingView.findDescendantValuePaneByType(pane, GuiSwingView.ValuePane.class, v -> v.getSwingViewContext().getName().equals("obj2"));

        GuiSwingViewStringField.PropertyStringPane subStrPane1 = runGet(() -> GuiSwingView.findDescendantByType(subPane1.asSwingViewComponent(), GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewStringField.PropertyStringPane subStrPane2 = runGet(() -> GuiSwingView.findDescendantByType(subPane2.asSwingViewComponent(), GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingPreferences swingPrefs = runGet(() -> new GuiSwingPreferences(context, pane));

        EditWait wait = editWait(swingPrefs.getUpdater());
        run(() -> strPane.setSwingViewValueWithUpdate("value1"));

        run(() -> boolBox.setSwingViewValueWithUpdate(true));
        run(() -> subStrPane1.setSwingViewValueWithUpdate("value2"));

        run(() -> subStrPane2.setSwingViewValueWithUpdate("value3"));

        run(() -> subStrPane2.setSwingViewValueWithUpdate("value4"));

        Duration endTime = Duration.between(startTime, Instant.now());

        wait.awaitNextFinish(); //above edits are immediately affected
        Map<String, Object> obj = runGet(prefs::toJson);
        System.out.println(obj);

        assertEquals("\"value1\"", mapGet(obj, "hello", "$history", "0", "value"), "str prop history value is json string");
        assertEquals("0", mapGet(obj, "hello", "$history", "0", "index"), "str prop history index");

        Instant time1 = Instant.parse((String) mapGet(obj, "hello", "$history", "0", "time"));

        assertTrue(Duration.between(startTime, time1).compareTo(endTime) < 0, "str prop history time");

        ///////////////////////

        assertEquals("true", mapGet(obj, "world", "$history", "1", "value"), "bool prop history value is json string");
        assertEquals("1", mapGet(obj, "world", "$history", "1", "index"), "bool prop history index");

        Instant time2 = Instant.parse((String) mapGet(obj, "world", "$history", "1", "time"));

        assertTrue(Duration.between(startTime, time2).compareTo(endTime) < 0, "str prop history time");

        ///////////////////////

        assertEquals("\"value3\"", mapGet(obj, "obj2", "TestSubObj", "value", "$history", "0", "value"), "sub-obj string prop history value is json string");
        assertEquals("0", mapGet(obj, "obj2", "TestSubObj", "value", "$history", "0", "index"), "sub-obj string prop history index");

        Instant time3 = Instant.parse((String) mapGet(obj, "obj2", "TestSubObj", "value", "$history", "0", "time"));

        assertTrue(Duration.between(startTime, time3).compareTo(endTime) < 0, "str prop history time");

        ///////////////////////

        assertEquals("\"value4\"", mapGet(obj, "obj2", "TestSubObj", "value", "$history", "1", "value"), "sub-obj string prop history value is json string");
        assertEquals("1", mapGet(obj, "obj2", "TestSubObj", "value", "$history", "1", "index"), "sub-obj string prop history index");

        Instant time4 = Instant.parse((String) mapGet(obj, "obj2", "TestSubObj", "value", "$history", "1", "time"));

        assertTrue(Duration.between(startTime, time4).compareTo(endTime) < 0, "str prop history time");

    }

    @SuppressWarnings("unchecked")
    public Object mapGet(Object prefs, String... keys) {
        Object p = prefs;
        for (String k : keys) {
            p = ((Map<String, Object>) p).get(k);
        }
        return p;
    }
}
