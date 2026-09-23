package org.autogui.test.base.mapping;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.*;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeMemberProperty;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.base.type.GuiUpdatedValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GuiReprPropertyPaneTest {
    GuiReprPropertyPane prop;
    GuiReprObjectPane objPane;

    GuiTypeObject typeObject;
    GuiTypeMemberProperty typeProperty;

    GuiTypeBuilder builder;
    GuiMappingContext contextObj;
    GuiMappingContext contextProp;
    GuiMappingContext contextValue;

    GuiMappingContext contextNum;

    GuiMappingContext contextReadOnly;

    TestObjReprProp obj;
    GuiTaskClock viewClock;

    public GuiReprPropertyPaneTest() {
    }

    @BeforeEach
    public void setUp() {
        viewClock = new GuiTaskClock(true);

        builder = new GuiTypeBuilder();

        typeObject = (GuiTypeObject) builder.get(TestObjReprProp.class);
        typeProperty = (GuiTypeMemberProperty) typeObject.getMemberByName("value");

        prop = new GuiReprPropertyPane(GuiRepresentation.getDefaultSet());
        objPane = new GuiReprObjectPane(prop); //custom ObjectPane; holds PropertyPane and the prop of the obj only matches to the PropertyPane

        obj = new TestObjReprProp();
        obj.value = "hello";

        contextObj = new GuiReprObjectPaneTest.GuiMappingContextForDebug(typeObject, null, null,
                GuiMappingContext.GuiSourceValue.of(obj));

        objPane.match(contextObj);

        contextProp = contextObj.getChildByName("value");
        contextValue = contextProp.getChildByName("String");

        contextReadOnly = contextObj.getChildByName("readOnly").getChildByName("String");

        contextNum = contextObj.getChildByName("num");
    }

    @GuiIncluded
    public static class TestObjReprProp {
        @GuiIncluded
        public String value;

        @GuiIncluded
        public String getReadOnly() {
            return "read-only";
        }

        @GuiIncluded
        public BigInteger num;

        public TestObjReprProp() {
        }
    }

    @Test
    public void testPropMatchCreation() {
        assertEquals(
                "value",
                contextProp.getName(),
                "obj creates prop as its wrapped reprs");
        assertTrue(contextProp.getRepresentation() instanceof GuiReprPropertyPane,
                "obj creates prop");

        assertTrue(contextValue.getRepresentation() instanceof GuiReprValueStringField,
                "prop pane is str");
    }

    @Test
    public void testValueGetUpdatedValueProp() throws Throwable {
        assertEquals(
                GuiUpdatedValue.of("hello"),
                contextProp.getReprValue()
                        .getUpdatedValue(contextProp, GuiReprValue.NONE),
                "prop value");
    }

    @Test
    public void testValueGetUpdatedValuePropChild() throws Throwable {
        assertEquals(
                GuiUpdatedValue.of("hello"),
                contextValue.getReprValue()
                        .getUpdatedValue(contextValue, GuiReprValue.NONE),
                "prop child value");
    }

    @Test
    public void testValueUpdate() {
        contextValue.getReprValue()
                .updateFromGui(contextValue, "world", GuiReprValue.NONE, viewClock.increment().copy());

        assertEquals(
                "world",
                obj.value,
                "prop child value updates parent prop");
    }

    @Test
    public void testValueIsEditableUnderProp() {
        assertFalse(contextReadOnly.getReprValue().isEditable(contextReadOnly),
                "isEditable for value under read-only prop returns false");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValueToJson() {
        Object p = contextNum.getRepresentation()
                .toJson(contextNum, new GuiReprValue.NamedValue("anything", new BigInteger("1234567890123")));
        Map<String, Object> m = (Map<String, Object>) p;
        assertEquals("1234567890123", m.get(contextNum.getName()), "toJson with NamedValue");

        Object p2 = contextNum.getRepresentation()
                .toJson(contextNum, new GuiReprValue.NamedValue("anything", new BigInteger("1234567890123")));
        Map<String, Object> m2 = (Map<String, Object>) p2;
        assertEquals("1234567890123", m2.get(contextNum.getName()), "toJson with value");

        Map<String, Object> m3 = (Map<String, Object>) contextNum.getRepresentation().toJson(contextNum, null);
        assertNull(m3.get("num"), "toJson null returns {p:null}"); //null for BigIngger is null
    }

    @Test
    public void testValueToHumanReadableString() {
        String s = contextNum.getRepresentation()
                .toHumanReadableString(contextNum, new GuiReprValue.NamedValue("anything", new BigInteger("1234567890123")));
        assertEquals("1,234,567,890,123", s, "toHRS with NamedValue returns formatted value");

        String s2 = contextNum.getRepresentation()
                .toHumanReadableString(contextNum, new BigInteger("1234567890123"));
        assertEquals("1,234,567,890,123", s2, "toHRS with value");
    }

    @Test
    public void testValueFromJson() {
        assertTrue(contextNum.getRepresentation().isJsonSetter(), "isJsonSetter");
        assertTrue(((GuiReprPropertyPane) contextNum.getRepresentation()).isFromJsonTakingMapWithContextNameEntry(contextNum),
                "isFromJsonTakingMapWithContextNameEntry");

        Map<String, Object> json = new HashMap<>();
        json.put("num", "1234567890123");
        Object val = contextNum.getRepresentation()
                .fromJson(contextNum, new GuiReprValue.NamedValue("anything", new BigInteger("1234")), json);

        assertEquals(new GuiReprValue.NamedValue("num", new BigInteger("1234567890123")), val,
                "fromJson with NamedValue target returns NamedValue(propName,v)");

        Object val2 = contextNum.getRepresentation()
                .fromJson(contextNum, new BigInteger("1234"), json);

        assertEquals(new BigInteger("1234567890123"), val2,
                "fromJson with value target returns value");
    }

    @Test
    public void testValueFromHumanReadableString() {
        Object obj = contextNum.getRepresentation()
                .fromHumanReadableString(contextNum, "1,234,567,890,123");
        assertEquals(new BigInteger("1234567890123"), obj, "fromHRS");
    }
}
