package org.autogui.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.JsonReader;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.base.mapping.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        pane = new GuiReprObjectPane(GuiSwingMapperSet.getReprDefaultSet());

        obj = new TestObj();
        context = new GuiMappingContext(typeObject, obj);

        pane.match(context);

        objPane = new GuiSwingViewObjectPane(GuiSwingMapperSet.getDefaultMapperSet());
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
    }

    @GuiIncluded
    public static class TestSubObj {
        @GuiIncluded
        public String value;
    }

    public GuiSwingViewObjectPane.ObjectPane create() {
        JComponent c = objPane.createView(context, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findChildByType(c, GuiSwingViewObjectPane.ObjectPane.class);
    }

    @Test
    public void testViewObjCreate() {
        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

        Assert.assertEquals("string property",
                "hello",
                strPane.getSwingViewContext().getName());
        Assert.assertTrue("string property",
                strPane.getSwingViewContext().getRepresentation() instanceof GuiReprValueStringField);


        Assert.assertEquals("boolean property",
                "world",
                boolBox.getSwingViewContext().getName());
        Assert.assertTrue("boolean property",
                boolBox.getSwingViewContext().getRepresentation() instanceof GuiReprValueBooleanCheckBox);
    }

    @Test
    public void testViewObjUpdate() {
        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

        obj.hello = "Hello";
        obj.world = true;
        context.updateSourceFromRoot();

        Assert.assertEquals("updated str prop value",
                "Hello",
                runGet(strPane::getSwingViewValue));

        Assert.assertEquals("updated boolean prop value",
                true,
                runGet(boolBox::getSwingViewValue));
    }

    @Test
    public void testViewObjUpdateObj() {
        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

        TestObj o = new TestObj();
        o.hello = "HELLO";
        o.world = true;
        run(() -> pane.setSwingViewValueWithUpdate(o));

        Assert.assertEquals("updated str prop value",
                "HELLO",
                runGet(strPane::getSwingViewValue));

        Assert.assertEquals("updated boolean prop value",
                true,
                runGet(boolBox::getSwingViewValue));
    }

    @Test
    public void testViewObjSetProp() {
        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

        run(() -> strPane.setSwingViewValueWithUpdate("Hello"));
        run(() -> boolBox.setSwingViewValueWithUpdate(true));
        runWait();

        Assert.assertEquals("set str prop value",
                "Hello",
                obj.hello);

        Assert.assertTrue("set boolean prop value",
                obj.world);

    }

    @Test
    public void testViewObjCopyAsJson() {
        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

        GuiSwingView.ValuePane<Object> subPane1 = GuiSwingView.findChild(pane,
                v -> v.getSwingViewContext().getName().equals("obj1"));
        GuiSwingView.ValuePane<Object> subPane2 = GuiSwingView.findChild(pane,
                v -> v.getSwingViewContext().getName().equals("obj2"));

        GuiSwingViewStringField.PropertyStringPane subStrPane1 = runGet(() ->
                GuiSwingView.findChildByType(subPane1.asSwingViewComponent(), GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewStringField.PropertyStringPane subStrPane2 = runGet(() ->
                GuiSwingView.findChildByType(subPane2.asSwingViewComponent(), GuiSwingViewStringField.PropertyStringPane.class));

        run(() -> boolBox.setSwingViewValueWithUpdate(true));
        run(() -> strPane.setSwingViewValueWithUpdate("value1"));
        run(() -> subStrPane1.setSwingViewValueWithUpdate("value2"));
        run(() -> subStrPane2.setSwingViewValueWithUpdate("value3"));
        runWait();

        GuiSwingJsonTransfer.JsonCopyAction a = runGet(() -> findMenuItemAction(pane.getSwingStaticMenuItems(),
                GuiSwingJsonTransfer.JsonCopyAction.class));
        run(() -> a.actionPerformed(null));


        Assert.assertEquals("copy as json",
                "{" +
                        "\"hello\":\"value1\"," +
                        "\"world\":true," +
                        "\"obj1\":{\"value\":\"value2\"}," +
                        "\"obj2\":{\"value\":\"value3\"}" +
                        "}",
                runGet(this::getClipboardText).replaceAll("\\s+", ""));
    }

    @Test
    public void testViewObjPasteJson() {
        GuiSwingViewObjectPane.ObjectPane pane = create();

        run(() -> setClipboardText("{" +
                "\"obj2\":{\"value\":\"HELLO\"}," +
                "\"world\":true," +
                "\"obj1\":{\"value\":\"WORLD\"}," +
                "\"hello\":\"Hello\"" +
                "}"));

        GuiSwingJsonTransfer.JsonPasteAction a = runGet(() -> findMenuItemAction(pane.getSwingStaticMenuItems(),
                GuiSwingJsonTransfer.JsonPasteAction.class));
        run(() -> a.actionPerformed(null));
        runWait();

        Assert.assertEquals("after paste json",
                "Hello",
                obj.hello);

        Assert.assertTrue("after paste json",
                obj.world);

        Assert.assertEquals("after paste json",
                "HELLO",
                obj.obj2.value);

        Assert.assertEquals("after paste json",
                "WORLD",
                obj.obj1.value);
    }

    @Test
    public void testViewObjAction() {
        GuiSwingViewObjectPane.ObjectPane pane = create();
        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewStringField.PropertyStringPane.class));
        run(() -> strPane.setSwingViewValueWithUpdate("Hello"));
        runWait();

        GuiSwingActionDefault.ExecutionAction a = runGet(() -> findMenuItemAction(pane.getSwingStaticMenuItems(),
                GuiSwingActionDefault.ExecutionAction.class));
        run(() -> a.actionPerformed(null));

        Assert.assertEquals("execute action",
                "Hello!",
                obj.hello);

        Assert.assertEquals("after execute action",
                1,
                obj.actionCount);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testViewObjPrefsSetSplit() {
        GuiPreferences prefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        context.setPreferences(prefs);

        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingPreferences swingPrefs = runGet(() -> new GuiSwingPreferences(context, pane));
        EditWait wait = editWait(swingPrefs.getUpdater());

        run(() -> pane.getSplitPanes().get(0).setDividerLocation(0.7));
        wait.awaitNextFinish();

        Map<String,Object> obj = runGet(prefs::toJson);

        Assert.assertTrue("divider location prefs converted as json string: " + obj.get("$split"),
                obj.get("$split") instanceof String);

        List<Map<String,Object>> list = (List<Map<String,Object>>) JsonReader
                .create((String) obj.get("$split")).parseValue();

        Assert.assertTrue("divider location prefs",
                list.get(0).containsKey("dividerLocation"));

        Assert.assertTrue("divider location prefs",
                (Boolean) list.get(0).get("horizontal"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testViewObjPrefsSetHistory()  {
        Instant startTime = Instant.now();

        GuiPreferences prefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        context.setPreferences(prefs);

        GuiSwingViewObjectPane.ObjectPane pane = create();

        GuiSwingViewStringField.PropertyStringPane strPane = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewBooleanCheckBox.PropertyCheckBox boolBox = runGet(() ->
                GuiSwingView.findChildByType(pane, GuiSwingViewBooleanCheckBox.PropertyCheckBox.class));

        GuiSwingView.ValuePane<Object> subPane1 = GuiSwingView.findChild(pane,
                v -> v.getSwingViewContext().getName().equals("obj1"));
        GuiSwingView.ValuePane<Object> subPane2 = GuiSwingView.findChild(pane,
                v -> v.getSwingViewContext().getName().equals("obj2"));

        GuiSwingViewStringField.PropertyStringPane subStrPane1 = runGet(() ->
                GuiSwingView.findChildByType(subPane1.asSwingViewComponent(), GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingViewStringField.PropertyStringPane subStrPane2 = runGet(() ->
                GuiSwingView.findChildByType(subPane2.asSwingViewComponent(), GuiSwingViewStringField.PropertyStringPane.class));

        GuiSwingPreferences swingPrefs = runGet(() -> new GuiSwingPreferences(context, pane));

        EditWait wait = editWait(swingPrefs.getUpdater());
        run(() -> strPane.setSwingViewValueWithUpdate("value1"));

        run(() -> boolBox.setSwingViewValueWithUpdate(true));
        run(() -> subStrPane1.setSwingViewValueWithUpdate("value2"));

        run(() -> subStrPane2.setSwingViewValueWithUpdate("value3"));

        run(() -> subStrPane2.setSwingViewValueWithUpdate("value4"));

        Duration endTime = Duration.between(startTime, Instant.now());

        wait.awaitNextFinish(); //above edits are immediately affected
        Map<String,Object> obj = runGet(prefs::toJson);
        System.out.println(obj);

        Assert.assertEquals("str prop history value is json string",
                "\"value1\"",
                mapGet(obj, "hello", "$history", "0", "value"));
        Assert.assertEquals("str prop history index",
                "0",
                mapGet(obj, "hello", "$history", "0", "index"));

        Instant time1 = Instant.parse((String) mapGet(obj, "hello", "$history", "0", "time"));

        Assert.assertTrue("str prop history time",
                Duration.between(startTime, time1).compareTo(endTime) < 0);

        ///////////////////////

        Assert.assertEquals("bool prop history value is json string",
                "true",
                mapGet(obj, "world", "$history", "1", "value"));
        Assert.assertEquals("bool prop history index",
                "1",
                mapGet(obj, "world", "$history", "1", "index"));

        Instant time2 = Instant.parse((String) mapGet(obj, "world", "$history", "1", "time"));

        Assert.assertTrue("str prop history time",
                Duration.between(startTime, time2).compareTo(endTime) < 0);

        ///////////////////////

        Assert.assertEquals("sub-obj string prop history value is json string",
                "\"value3\"",
                mapGet(obj, "obj2", "TestSubObj", "value", "$history", "0", "value"));
        Assert.assertEquals("sub-obj string prop history index",
                "0",
                mapGet(obj, "obj2", "TestSubObj", "value", "$history", "0", "index"));

        Instant time3 = Instant.parse((String)
                mapGet(obj, "obj2", "TestSubObj", "value", "$history", "0", "time"));

        Assert.assertTrue("str prop history time",
                Duration.between(startTime, time3).compareTo(endTime) < 0);

        ///////////////////////

        Assert.assertEquals("sub-obj string prop history value is json string",
                "\"value4\"",
                mapGet(obj, "obj2", "TestSubObj", "value", "$history", "1", "value"));
        Assert.assertEquals("sub-obj string prop history index",
                "1",
                mapGet(obj, "obj2", "TestSubObj", "value", "$history", "1", "index"));

        Instant time4 = Instant.parse((String)
                mapGet(obj, "obj2", "TestSubObj", "value", "$history", "1", "time"));

        Assert.assertTrue("str prop history time",
                Duration.between(startTime, time4).compareTo(endTime) < 0);

    }

    @SuppressWarnings("unchecked")
    public Object mapGet(Object prefs, String... keys) {
        Object p = prefs;
        for (String k : keys) {
            p = ((Map<String,Object>) p).get(k);
        }
        return p;
    }
}
