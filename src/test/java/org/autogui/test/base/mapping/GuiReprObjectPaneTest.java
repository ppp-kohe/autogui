package org.autogui.test.base.mapping;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.*;
import org.autogui.base.type.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

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

    public GuiReprObjectPaneTest() {
    }

    @BeforeEach
    public void setUp() {
        viewClock = new GuiTaskClock(true);
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

        public TestReprObjPane() {
        }
    }

    public static class TestUpdater implements GuiMappingContext.SourceUpdateListener {
        public List<Object> newValues = new ArrayList<>();

        public TestUpdater() {
        }

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
        assertTrue(objPane.match(ctx),
                "match with obj type");

        assertEquals(
                objPane,
                ctx.getRepresentation(),
                "match sets repr");

        GuiMappingContext contextProp = ctx.getChildren().getFirst();
        assertEquals(typeProperty, contextProp.getTypeElement(),
                "match creates a sub-context with prop repr");

        assertTrue(contextProp.getRepresentation() instanceof GuiReprValueStringField,
                "match creates a sub-context with prop supported pane");

    }

    @Test
    public void testValueGetParentSource() throws Throwable {
        assertEquals(
                GuiMappingContext.GuiSourceValue.of(objRepr),
                contextStr.getReprValue().getParentSource(contextStr, GuiReprValue.NONE),
                "getParentSource returns parent source obj");
    }

    @Test
    public void testValueGetUpdatedValue() throws Throwable {
        assertEquals(
                GuiUpdatedValue.of("hello"),
                contextStr.getReprValue()
                        .getUpdatedValue(contextStr, GuiReprValue.NONE),
                "getUpdatedValue returns prop value");
    }

    @Test
    public void testValueGetUpdatedValueWithoutNoUpdate() throws Throwable {
        assertEquals(
                "hello",
                contextStr.getReprValue()
                        .getUpdatedValueWithoutNoUpdate(contextStr, GuiReprValue.NONE),
                "getUpdatedValueWithoutNoUpdate returns current prop");
    }

    @Test
    public void testValueCheckAndUpdateSourceProp() {
        assertTrue(contextStr.getReprValue().checkAndUpdateSource(contextStr),
                "checkAndUpdateSource returns true");

        assertEquals(
                GuiMappingContext.GuiSourceValue.of("hello"),
                contextStr.getSource(),
                "checkAndUpdateSource sets source");
    }

    @Test
    public void testValueCheckAndUpdateSourceObjNone() {
        assertFalse(contextObj.getRepresentation().checkAndUpdateSource(contextObj),
                "checkAndUpdateSource returns false for no-update");
    }

    @Test
    public void testValueUpdateFromGui() {
        contextStr.getReprValue()
                .updateFromGui(contextStr, "world", GuiReprValue.NONE, viewClock.increment().copy());

        assertEquals(
                Collections.singletonList(123),
                testUpdater.newValues,
                "after updateFromGui, updated fields (from NO_SOURCE) are notified");
    }


    @Test
    public void testValueUpdateFromGuiNoUpdate() {
        contextX.setSource(GuiMappingContext.GuiSourceValue.of(123));

        contextStr.getReprValue()
                .updateFromGui(contextStr, "world", GuiReprValue.NONE, viewClock.increment().copy());

        assertEquals(
                Collections.emptyList(),
                testUpdater.newValues,
                "after updateFromGui, no changed values notify nothing");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValueToJson() {
        Object json = contextObj.getRepresentation().toJson(contextObj, objRepr);
        Map<String, Object> map = (Map<String, Object>) json;
        assertEquals("hello", map.get("value"), "toJson obj key for str");
        assertEquals(123, map.get("x"), "toJson obj key for int");
        assertEquals(2, map.size(), "toJson obj entries");

        Object json2 = contextObj.getRepresentation().toJson(contextObj, null);
        Map<String, Object> map2 = (Map<String, Object>) json2;
        assertNull(map2.get("value"), "toJson null for str: toJson str returns null, then the map skips putting");
        assertEquals(0, map2.get("x"), "toJson null for int: toJson int returns 0, so the map puts the entry");
        assertEquals(1, map2.size(), "toJson null entries");
    }

    @Test
    public void testValueFromJson() {
        Map<String, Object> json = new HashMap<>();
        json.put("value", "!!!");
        json.put("x", 456);
        Object obj = contextObj.getRepresentation().fromJson(contextObj, null, json);
        TestReprObjPane newVal = (TestReprObjPane) obj;
        assertNotNull(newVal, "fromJson null returns new obj");
        assertEquals("!!!", newVal.value, "fromJson str prop");
        assertEquals(456, newVal.x, "fromJson int prop");

        assertTrue(contextObj.getRepresentation().isJsonSetter(), "obj is jsonSetter");
        obj = contextObj.getRepresentation().fromJson(contextObj, objRepr, json);
        assertEquals(objRepr, obj, "fromJson obj returns target");
        assertEquals("!!!", objRepr.value, "fromJson str prop");
        assertEquals(456, objRepr.x, "fromJson int prop");
    }

    @Test
    public void testValueFromJsonIncomplete() {
        Map<String, Object> json = new HashMap<>();
        json.put("value", "!!!");
        Object obj = contextObj.getRepresentation().fromJson(contextObj, null, json);
        TestReprObjPane newVal = (TestReprObjPane) obj;
        assertNotNull(newVal, "fromJson null returns new obj");
        assertEquals("!!!", newVal.value, "fromJson str prop");
        assertEquals(0, newVal.x, "fromJson int prop");

    }

    @Test
    public void testValueToHumanReadableString() {
        String str = contextObj.getRepresentation().toHumanReadableString(contextObj, objRepr);
        assertEquals("hello\t123", str, "toHRS");
    }

    @Test
    public void testValueFromHumanReadableString() {
        Object obj = contextObj.getRepresentation().fromHumanReadableString(contextObj, "!!!\t456");
        assertNotNull(obj, "fromHRS new obj");
        TestReprObjPane newVal = (TestReprObjPane) obj;
        assertEquals("!!!", newVal.value, "fromHRS str");
        assertEquals(456, newVal.x, "fromHRS int");
    }

    @Test
    public void testValueFromHumanReadableStringIncomplete() {
        Object obj = contextObj.getRepresentation().fromHumanReadableString(contextObj, "!!!");
        assertNotNull(obj, "fromHRS new obj");
        TestReprObjPane newVal = (TestReprObjPane) obj;
        assertEquals("!!!", newVal.value, "fromHRS str");
        assertEquals(0, newVal.x, "fromHRS int");
    }
}
