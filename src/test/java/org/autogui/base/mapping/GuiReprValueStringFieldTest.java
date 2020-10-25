package org.autogui.base.mapping;

import org.autogui.GuiIncluded;
import org.autogui.base.type.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

public class GuiReprValueStringFieldTest {

    GuiReprValueStringField fld;

    GuiTypeBuilder builder;
    GuiTypeValue type;

    GuiTypeObject typeObject;
    GuiTypeMemberProperty property;
    GuiTypeMemberProperty propertyReadOnly;

    String value;
    TestObjRepr obj;

    GuiMappingContext contextValue;
    GuiMappingContext contextProp;

    GuiMappingContext contextReadOnly;

    @Before
    public void setUp() {
        fld = new GuiReprValueStringField();

        builder = new GuiTypeBuilder();
        type = (GuiTypeValue) builder.get(String.class);

        typeObject = (GuiTypeObject) builder.get(TestObjRepr.class);
        property = (GuiTypeMemberProperty) typeObject.getMemberByName("value");

        propertyReadOnly = (GuiTypeMemberProperty) typeObject.getMemberByName("readOnly");

        value = "hello\nworld";

        obj = new TestObjRepr();
        obj.value = "hello";

        contextValue = new GuiReprObjectPaneTest.GuiMappingContextForDebug(type, fld);
        contextProp = new GuiReprObjectPaneTest.GuiMappingContextForDebug(property, fld);

        contextReadOnly = new GuiMappingContext(propertyReadOnly, fld);
    }

    @GuiIncluded
    public static class TestObjRepr {
        @GuiIncluded
        public String value;

        @GuiIncluded
        public String getReadOnly() {
            return "hello";
        }
    }

    @Test
    public void testValueMatch() {
        GuiMappingContext ctx = new GuiMappingContext(type, value);
        Assert.assertTrue("match with typed context",
                fld.match(ctx));
        Assert.assertEquals("match sets contexts repr",
                fld,
                ctx.getRepresentation());
    }


    @Test
    public void testValueMatchProp() {
        GuiMappingContext ctx = new GuiMappingContext(property, value);
        Assert.assertTrue("match prop with typed context",
                fld.match(ctx));
        Assert.assertEquals("match prop sets contexts repr",
                fld,
                ctx.getRepresentation());
    }

    @Test
    public void testValueGetValueNoParentNoPrev() throws Throwable {
        Assert.assertEquals("getValue with no-source parent and no-source prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextValue, GuiMappingContext.NO_SOURCE,
                        GuiReprValue.NONE, GuiMappingContext.NO_SOURCE));
    }


    @Test
    public void testValueGetValueParentNoPrev() throws Throwable {
        Assert.assertEquals("getValue with a parent and no-source prev return no-update",
                GuiUpdatedValue.of("hello"),
                fld.getValue(contextValue, GuiMappingContext.GuiSourceValue.of("hello"),
                        GuiReprValue.NONE, GuiMappingContext.NO_SOURCE));
    }


    @Test
    public void testValueGetValueParentPrevSame() throws Throwable {
        Assert.assertEquals("getValue with a parent and same prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextValue, GuiMappingContext.GuiSourceValue.of("hello"),
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")));
    }

    @Test
    public void testValueGetValueNoParentPrev() throws Throwable {
        Assert.assertEquals("getValue with no-parent and a prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextValue, GuiMappingContext.NO_SOURCE,
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")));
    }

    @Test
    public void testValueGetValueParentPrevDiff() throws Throwable {
        Assert.assertEquals("getValue with a parent and diff prev return the parent",
                GuiUpdatedValue.of("world"),
                fld.getValue(contextValue, GuiMappingContext.GuiSourceValue.of("world"),
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")));
    }

    @Test
    public void testValueGetValuePropNoParentNoPrev() throws Throwable {
        Assert.assertEquals("getValue with no parent and no prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextProp, GuiMappingContext.NO_SOURCE,
                        GuiReprValue.NONE, GuiMappingContext.NO_SOURCE));
    }

    @Test
    public void testValueGetValuePropParentPrevDiff() throws Throwable {
        Assert.assertEquals("getValue with a parent and diff prev return the prop value",
                GuiUpdatedValue.of("hello"),
                fld.getValue(contextProp, GuiMappingContext.GuiSourceValue.of(obj),
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("world")));
    }

    @Test
    public void testValueGetValuePropNoParentPrev() throws Throwable {
        Assert.assertEquals("getValue with no-parent and prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextProp, GuiMappingContext.NO_SOURCE,
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("world")));
    }


    @Test
    public void testValueGetValuePropParentNoPrev() throws Throwable {
        Assert.assertEquals("getValue with a parent and diff prev return the prop value",
                GuiUpdatedValue.of("hello"),
                fld.getValue(contextProp, GuiMappingContext.GuiSourceValue.of(obj),
                        GuiReprValue.NONE, GuiMappingContext.NO_SOURCE));
    }

    @Test
    public void testValueGetValuePropParentPrevSame() throws Throwable {
        Assert.assertEquals("getValue with a parent and same prev return no-update",
                GuiUpdatedValue.NO_UPDATE,
                fld.getValue(contextProp, GuiMappingContext.GuiSourceValue.of(obj),
                        GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")));
    }

    @Test
    public void testValueUpdateNone() throws Throwable {
        Assert.assertEquals("update returns newValue",
                "hello",
                fld.update(contextValue, GuiMappingContext.NO_SOURCE,
                        "hello", GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdate() throws Throwable {
        contextValue.setSource(GuiMappingContext.GuiSourceValue.of("hello"));
        Assert.assertEquals("update with same prev source returns newValue",
                "hello",
                fld.update(contextValue, GuiMappingContext.NO_SOURCE,
                        "hello", GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdateNoneParent() throws Throwable {
        Assert.assertEquals("update returns newValue",
                "hello",
                fld.update(contextValue, GuiMappingContext.GuiSourceValue.of("hello"),
                        "hello", GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdateParent() throws Throwable {
        contextValue.setSource(GuiMappingContext.GuiSourceValue.of("hello"));
        Assert.assertEquals("update with same prev source returns newValue",
                "hello",
                fld.update(contextValue, GuiMappingContext.GuiSourceValue.of("hello"),
                        "hello", GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdateProp() throws Throwable {
        Assert.assertEquals("update returns newValue",
                "world",
                fld.update(contextProp, GuiMappingContext.GuiSourceValue.of(obj),
                        "world", GuiReprValue.NONE));
        Assert.assertEquals("update updates field",
                "world",
                obj.value);
    }

    @Test
    public void testValueGetParentSourceNone() throws Throwable {
        Assert.assertEquals("getParentSource with no parent returns none",
                GuiMappingContext.NO_SOURCE,
                fld.getParentSource(contextValue, GuiReprValue.NONE));
    }

    @Test
    public void testValueGetValueWithoutNoUpdate() throws Throwable {
        Assert.assertEquals("getValueWithoutNoUpdate returns parent value",
                "hello",
                fld.getValueWithoutNoUpdate(contextValue,
                        GuiMappingContext.GuiSourceValue.of("hello"),
                        GuiReprValue.NONE));
    }

    @Test
    public void testValueGetValueWithoutNoUpdateNone() throws Throwable {
        Assert.assertNull("getValueWithoutNoUpdate with no parent returns null",
                fld.getValueWithoutNoUpdate(contextValue,
                        GuiMappingContext.NO_SOURCE,
                        GuiReprValue.NONE));
    }

    //////////

    @Test
    public void testValueIsEditable() {
        Assert.assertTrue("isEditable prop returns true",
                fld.isEditable(contextProp));
    }

    @Test
    public void testValueIsEditableValue() {
        Assert.assertTrue("isEditable value returns true",
                fld.isEditable(contextValue));
    }

    @Test
    public void testValueIsEditableReadOnly() {
        Assert.assertFalse("isEditable returns false for read-only prop",
                fld.isEditable(contextReadOnly));
    }

    //////////

    @Test
    public void testValueToJson() {
        Assert.assertEquals("toJson returns same value as arg",
                value,
                fld.toJson(contextValue, value));
    }

    @Test
    public void testValueToJsonIllegal() {
        Assert.assertNull("toJson returns null for illegal arg",
                fld.toJson(contextValue, 123));
    }

    @Test
    public void testValueToJsonWithNamed() {
        HashMap<String,Object> map = new HashMap<>();
        map.put("hello", value);
        Assert.assertEquals("toJsonWithNamed returns value of NamedValue arg",
                map,
                fld.toJsonWithNamed(contextValue,
                        new GuiReprValue.NamedValue("hello", value)));
    }

    @Test
    public void testValueToJsonWithNamedNonNamedValue() {
        Assert.assertEquals("toJsonWithNamed returns same value as non NamedValue arg",
                value,
                fld.toJsonWithNamed(contextValue, value));
    }

    @Test
    public void testValueFromJson() {
        Assert.assertFalse("isJsonSetter is false for String",
                fld.isJsonSetter());
        Assert.assertEquals("fromJson returns same value as arg",
                value,
                fld.fromJson(contextValue, null, value));
    }

    @Test
    public void testValueFromJsonIllegal() {
        Assert.assertNull("fromJson returns null for illegal arg",
                fld.fromJson(contextValue, null, 123));
    }

    @Test
    public void testValueFromJsonWithNamed() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("value", value);
        Assert.assertEquals("fromJson returns NamedValue for NamedValue target and Map value",
                new GuiReprValue.NamedValue("value", value),
                fld.fromJsonWithNamed(contextProp,
                    new GuiReprValue.NamedValue("value", "?"),
                    map));
    }

    @Test
    public void testValueFromJsonWithNamedNonNamedValue() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("value", value);
        Assert.assertEquals("fromJson returns non NamedValue for non NamedValue target and Map value",
                value,
                fld.fromJsonWithNamed(contextProp,
                        "hello",
                        map));
    }


    @Test
    public void testValueFromJsonWithNamedNonMapTarget() {
        Assert.assertNull("fromJson returns null for non Map value",
                fld.fromJsonWithNamed(contextProp,
                        "hello",
                        "hello"));
    }


    @Test
    public void testValueToHumanReadableString() {
        Assert.assertEquals("toHumanReadableString returns same value as arg",
                value,
                fld.toHumanReadableString(contextValue, value));
    }

    @Test
    public void testValueToHumanReadableStringNull() {
        Assert.assertEquals("toHumanReadableString returns null for null",
                "null",
                fld.toHumanReadableString(contextValue, null));
    }

    @Test
    public void testValueFromHumanReadableString() {
        Assert.assertEquals("fromHumanReadableString returns same value as args",
                value,
                fld.fromHumanReadableString(contextValue, value));
    }

    @Test
    public void testValueFromHumanReadableStringNull() {
        Assert.assertNull("fromHumanReadableString returns null for null",
                fld.fromHumanReadableString(contextValue, null));
    }


}
