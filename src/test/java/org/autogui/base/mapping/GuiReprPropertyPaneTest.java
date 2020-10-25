package org.autogui.base.mapping;

import org.autogui.GuiIncluded;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeMemberProperty;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.base.type.GuiUpdatedValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

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

    @Before
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
    }

    @Test
    public void testPropMatchCreation() {
        Assert.assertEquals("obj creates prop as its wrapped reprs",
                "value",
                contextProp.getName());
        Assert.assertTrue("obj creates prop",
                contextProp.getRepresentation() instanceof GuiReprPropertyPane);

        Assert.assertTrue("prop pane is str",
                contextValue.getRepresentation() instanceof GuiReprValueStringField);
    }

    @Test
    public void testValueGetUpdatedValueProp() throws Throwable {
        Assert.assertEquals("prop value",
                GuiUpdatedValue.of("hello"),
                contextProp.getReprValue()
                    .getUpdatedValue(contextProp, GuiReprValue.NONE));
    }

    @Test
    public void testValueGetUpdatedValuePropChild() throws Throwable {
        Assert.assertEquals("prop child value",
                GuiUpdatedValue.of("hello"),
                contextValue.getReprValue()
                        .getUpdatedValue(contextValue, GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdate() throws Throwable {
        contextValue.getReprValue()
                .updateFromGui(contextValue, "world", GuiReprValue.NONE, viewClock.increment().copy());

        Assert.assertEquals("prop child value updates parent prop",
                "world",
                obj.value);
    }

    @Test
    public void testValueIsEditableUnderProp() {
        Assert.assertFalse("isEditable for value under read-only prop returns false",
                contextReadOnly.getReprValue().isEditable(contextReadOnly));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValueToJson() {
        Object p = contextNum.getRepresentation()
                    .toJson(contextNum, new GuiReprValue.NamedValue("anything", new BigInteger("1234567890123")));
        Map<String,Object> m = (Map<String,Object>) p;
        Assert.assertEquals("toJson with NamedValue", "1234567890123", m.get(contextNum.getName()));

        Object p2 = contextNum.getRepresentation()
                .toJson(contextNum, new GuiReprValue.NamedValue("anything", new BigInteger("1234567890123")));
        Map<String,Object> m2 = (Map<String,Object>) p2;
        Assert.assertEquals("toJson with value", "1234567890123", m2.get(contextNum.getName()));

        Map<String,Object> m3 = (Map<String,Object>) contextNum.getRepresentation().toJson(contextNum, null);
        Assert.assertNull("toJson null returns {p:null}", m3.get("num")); //null for BigIngger is null
    }

    @Test
    public void testValueToHumanReadableString() {
        String s = contextNum.getRepresentation()
                .toHumanReadableString(contextNum, new GuiReprValue.NamedValue("anything", new BigInteger("1234567890123")));
        Assert.assertEquals("toHRS with NamedValue returns formatted value", "1,234,567,890,123", s);

        String s2 = contextNum.getRepresentation()
                .toHumanReadableString(contextNum, new BigInteger("1234567890123"));
        Assert.assertEquals("toHRS with value", "1,234,567,890,123", s2);
    }

    @Test
    public void testValueFromJson() {
        Assert.assertTrue("isJsonSetter", contextNum.getRepresentation().isJsonSetter());
        Assert.assertTrue("isFromJsonTakingMapWithContextNameEntry",
                ((GuiReprPropertyPane) contextNum.getRepresentation()).isFromJsonTakingMapWithContextNameEntry(contextNum));

        Map<String,Object> json = new HashMap<>();
        json.put("num", "1234567890123");
        Object val = contextNum.getRepresentation()
                .fromJson(contextNum, new GuiReprValue.NamedValue("anything", new BigInteger("1234")), json);

        Assert.assertEquals("fromJson with NamedValue target returns NamedValue(propName,v)",
                new GuiReprValue.NamedValue("num", new BigInteger("1234567890123")), val);

        Object val2 = contextNum.getRepresentation()
                .fromJson(contextNum, new BigInteger("1234"), json);

        Assert.assertEquals("fromJson with value target returns value",
                new BigInteger("1234567890123"), val2);
    }

    @Test
    public void testValueFromHumanReadableString() {
        Object obj = contextNum.getRepresentation()
                .fromHumanReadableString(contextNum, "1,234,567,890,123");
        Assert.assertEquals("fromHRS", new BigInteger("1234567890123"), obj);
    }
}
