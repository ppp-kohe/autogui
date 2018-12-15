package org.autogui.test.base.mapping;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.*;
import org.autogui.base.type.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.Callable;

public class GuiReprObjectPaneTest {
    GuiReprObjectPane objPane;
    GuiTypeBuilder builder;
    GuiTypeObject typeObject;
    GuiTypeMemberProperty typeProperty;

    TestReprObjPane objRepr;

    GuiMappingContext contextObj;
    GuiMappingContext contextStr;
    GuiMappingContext contextX;
    TestUpdater testUpdater;

    GuiTaskClock viewClock;

    @Before
    public void setUp() {
        viewClock = new GuiTaskClock(true);;
        objPane = new GuiReprObjectPane(GuiRepresentation.getDefaultSet());

        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestReprObjPane.class);
        typeProperty = (GuiTypeMemberProperty) typeObject.getMemberByName("value");

        objRepr = new TestReprObjPane();
        objRepr.value = "hello";
        objRepr.x = 123;

        contextObj = new GuiMappingContextForDebug(typeObject, objPane, null, GuiMappingContext.GuiSourceValue.of(objRepr));
        objPane.match(contextObj);

        contextStr = contextObj.getChildByName("value");
        contextX = contextObj.getChildByName("x");

        testUpdater = new TestUpdater();
        contextX.addSourceUpdateListener(testUpdater);

    }

    @GuiIncluded
    public static class TestReprObjPane {
        @GuiIncluded(index = 0)
        public String value;

        @GuiIncluded(index = 1)
        public int x;
    }

    public static class TestUpdater implements GuiMappingContext.SourceUpdateListener  {
        public List<Object> newValues = new ArrayList<>();

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock clock) {
            newValues.add(newValue);
        }
    }

    static class GuiMappingContextForDebug extends GuiMappingContext {

        public GuiMappingContextForDebug(GuiTypeElement typeElement, Object source) {
            super(typeElement, source);
        }

        public GuiMappingContextForDebug(GuiTypeElement typeElement, GuiRepresentation representation) {
            super(typeElement, representation);
        }

        public GuiMappingContextForDebug(GuiTypeElement typeElement, GuiRepresentation representation,
                                         GuiMappingContext parent, GuiSourceValue source) {
            super(typeElement, representation, parent, source);
        }

        @Override
        public void updateSourceFromGuiByThisDelayed() {
            updateSourceFromRoot(this); //immediate
        }

        @Override
        public <T> T execute(Callable<T> task) throws Throwable {
            return task.call();
        }

        @Override
        public GuiMappingContext createChildCandidate(GuiTypeElement typeElement) {
            return new GuiMappingContextForDebug(typeElement, null, this, NO_SOURCE);
        }
    }


    @Test
    public void testObjectPaneMatch() {
        GuiMappingContext ctx = new GuiMappingContext(typeObject, objPane, objRepr);
        Assert.assertTrue("match with obj type",
                objPane.match(ctx));

        Assert.assertEquals("match sets repr",
                objPane,
                ctx.getRepresentation());

        GuiMappingContext contextProp = ctx.getChildren().get(0);
        Assert.assertEquals("match creates a sub-context with prop repr",
                typeProperty, contextProp.getTypeElement());

        Assert.assertTrue("match creates a sub-context with prop supported pane",
                contextProp.getRepresentation() instanceof GuiReprValueStringField);

    }

    @Test
    public void testValueGetParentSource() throws Throwable {
        Assert.assertEquals("getParentSource returns parent source obj",
                GuiMappingContext.GuiSourceValue.of(objRepr),
                contextStr.getReprValue().getParentSource(contextStr, GuiReprValue.NONE));
    }

    @Test
    public void testValueGetUpdatedValue() throws Throwable {
        Assert.assertEquals("getUpdatedValue returns prop value",
                GuiUpdatedValue.of("hello"),
                contextStr.getReprValue()
                        .getUpdatedValue(contextStr, GuiReprValue.NONE));
    }

    @Test
    public void testValueGetUpdatedValueWithoutNoUpdate() throws Throwable {
        Assert.assertEquals("getUpdatedValueWithoutNoUpdate returns current prop",
                "hello",
                contextStr.getReprValue()
                        .getUpdatedValueWithoutNoUpdate(contextStr, GuiReprValue.NONE));
    }

    @Test
    public void testValueCheckAndUpdateSourceProp() {
        Assert.assertTrue("checkAndUpdateSource returns true",
                contextStr.getReprValue().checkAndUpdateSource(contextStr));

        Assert.assertEquals("checkAndUpdateSource sets source",
                GuiMappingContext.GuiSourceValue.of("hello"),
                contextStr.getSource());
    }

    @Test
    public void testValueCheckAndUpdateSourceObjNone() {
        Assert.assertFalse("checkAndUpdateSource returns false for no-update",
                contextObj.getRepresentation().checkAndUpdateSource(contextObj));
    }

    @Test
    public void testValueUpdateFromGui() {
        contextStr.getReprValue()
                .updateFromGui(contextStr, "world", GuiReprValue.NONE, viewClock.increment().copy());

        Assert.assertEquals("after updateFromGui, updated fields (from NO_SOURCE) are notified",
                Collections.singletonList(123),
                testUpdater.newValues);
    }


    @Test
    public void testValueUpdateFromGuiNoUpdate() {
        contextX.setSource(GuiMappingContext.GuiSourceValue.of(123));

        contextStr.getReprValue()
                .updateFromGui(contextStr, "world", GuiReprValue.NONE, viewClock.increment().copy());

        Assert.assertEquals("after updateFromGui, no changed values notify nothing",
                Collections.emptyList(),
                testUpdater.newValues);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValueToJson() {
        Object json = contextObj.getRepresentation().toJson(contextObj, objRepr);
        Map<String,Object> map = (Map<String,Object>) json;
        Assert.assertEquals("toJson obj key for str", "hello", map.get("value"));
        Assert.assertEquals("toJson obj key for int", 123, map.get("x"));
        Assert.assertEquals("toJson obj entries", 2, map.size());

        Object json2 = contextObj.getRepresentation().toJson(contextObj, null);
        Map<String,Object> map2 = (Map<String,Object>) json2;
        Assert.assertNull("toJson null for str: toJson str returns null, then the map skips putting", map2.get("value"));
        Assert.assertEquals("toJson null for int: toJson int returns 0, so the map puts the entry", 0, map2.get("x"));
        Assert.assertEquals("toJson null entries", 1, map2.size());
    }

    @Test
    public void testValueFromJson() {
        Map<String,Object> json = new HashMap<>();
        json.put("value", "!!!");
        json.put("x", 456);
        Object obj = contextObj.getRepresentation().fromJson(contextObj, null, json);
        TestReprObjPane newVal = (TestReprObjPane) obj;
        Assert.assertNotNull("fromJson null returns new obj", newVal);
        Assert.assertEquals("fromJson str prop", "!!!", newVal.value);
        Assert.assertEquals("fromJson int prop", 456, newVal.x);

        Assert.assertTrue("obj is jsonSetter", contextObj.getRepresentation().isJsonSetter());
        obj = contextObj.getRepresentation().fromJson(contextObj, objRepr, json);
        Assert.assertEquals("fromJson obj returns target", objRepr, obj);
        Assert.assertEquals("fromJson str prop", "!!!", objRepr.value);
        Assert.assertEquals("fromJson int prop", 456, objRepr.x);
    }

    @Test
    public void testValueFromJsonIncomplete() {
        Map<String,Object> json = new HashMap<>();
        json.put("value", "!!!");
        Object obj = contextObj.getRepresentation().fromJson(contextObj, null, json);
        TestReprObjPane newVal = (TestReprObjPane) obj;
        Assert.assertNotNull("fromJson null returns new obj", newVal);
        Assert.assertEquals("fromJson str prop", "!!!", newVal.value);
        Assert.assertEquals("fromJson int prop", 0, newVal.x);

    }

    @Test
    public void testValueToHumanReadableString() {
        String str = contextObj.getRepresentation().toHumanReadableString(contextObj, objRepr);
        Assert.assertEquals("toHRS", "hello\t123", str);
    }

    @Test
    public void testValueFromHumanReadableString() {
        Object obj = contextObj.getRepresentation().fromHumanReadableString(contextObj, "!!!\t456");
        Assert.assertNotNull("fromHRS new obj", obj);
        TestReprObjPane newVal = (TestReprObjPane) obj;
        Assert.assertEquals("fromHRS str", "!!!", newVal.value);
        Assert.assertEquals("fromHRS int", 456, newVal.x);
    }

    @Test
    public void testValueFromHumanReadableStringIncomplete() {
        Object obj = contextObj.getRepresentation().fromHumanReadableString(contextObj, "!!!");
        Assert.assertNotNull("fromHRS new obj", obj);
        TestReprObjPane newVal = (TestReprObjPane) obj;
        Assert.assertEquals("fromHRS str", "!!!", newVal.value);
        Assert.assertEquals("fromHRS int", 0, newVal.x);
    }
}
