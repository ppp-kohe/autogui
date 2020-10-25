package org.autogui.base.mapping;

import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GuiReprValueBooleanCheckBoxTest {
    GuiReprValueBooleanCheckBox checkBox;

    GuiTypeBuilder builder;
    GuiTypeValue type;
    GuiTypeValue typeObj;

    GuiMappingContext contextValue;
    GuiMappingContext contextObj;

    @Before
    public void setUp() {
        checkBox = new GuiReprValueBooleanCheckBox();

        builder = new GuiTypeBuilder();
        type = (GuiTypeValue) builder.get(boolean.class);
        typeObj = (GuiTypeValue) builder.get(Boolean.class);

        contextValue = new GuiReprObjectPaneTest.GuiMappingContextForDebug(type, checkBox);
        contextObj = new GuiReprObjectPaneTest.GuiMappingContextForDebug(typeObj, checkBox);
    }

    @Test
    public void testValueMatch() {
        GuiMappingContext ctx = new GuiMappingContext(type, true);
        Assert.assertTrue("match with primitive",
                checkBox.match(ctx));

        GuiMappingContext ctx2 = new GuiMappingContext(typeObj, true);
        Assert.assertTrue("match with object",
                checkBox.match(ctx2));
    }

    @Test
    public void testValueUpdate() throws Throwable {
        contextObj.setSource(GuiMappingContext.GuiSourceValue.of(false));
        Assert.assertEquals("updated value obj",
                Boolean.TRUE,
                checkBox.update(contextObj, GuiMappingContext.NO_SOURCE,
                        true, GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdateNull() throws Throwable {
        contextObj.setSource(GuiMappingContext.GuiSourceValue.of(null));
        Assert.assertNull("updated value null",
                checkBox.update(contextObj, GuiMappingContext.NO_SOURCE,
                        null, GuiReprValue.NONE));
    }

    @Test
    public void testValueGetBooleanValue() {
        Assert.assertEquals("getBooleanValue with true returns true",
                true,
                checkBox.getBooleanValue("true"));

        Assert.assertEquals("getBooleanValue with false returns false",
                false,
                checkBox.getBooleanValue("false"));

        Assert.assertEquals("getBooleanValue with 0 returns true",
                false,
                checkBox.getBooleanValue("0"));

        Assert.assertEquals("getBooleanValue with 0s returns true",
                false,
                checkBox.getBooleanValue("0000"));

        Assert.assertEquals("getBooleanValue with number returns true",
                true,
                checkBox.getBooleanValue("12390"));

        Assert.assertNull("getBooleanValue with String returns null",
                checkBox.getBooleanValue("hello"));

        Assert.assertNull("getBooleanValue with null returns null",
                checkBox.getBooleanValue(null));
    }
    //////////

    @Test
    public void testValueToJson() {
        Assert.assertEquals("toJson returns same value as arg",
                true,
                checkBox.toJson(contextValue, true));
    }

    @Test
    public void testValueToJsonIllegal() {
        Assert.assertNull("toJson returns null for illegal arg",
                checkBox.toJson(contextValue, 123));
    }

    @Test
    public void testValueFromJson() {
        Assert.assertFalse("isJsonSetter is false for boolean",
                checkBox.isJsonSetter());
        Assert.assertEquals("fromJson returns same value as arg",
                true,
                checkBox.fromJson(contextValue, null, true));
    }

    @Test
    public void testValueFromJsonIllegal() {
        Assert.assertNull("fromJson returns null for illegal arg",
                checkBox.fromJson(contextValue, null, 123));
    }


    @Test
    public void testValueToHumanReadableString() {
        Assert.assertEquals("toHumanReadableString returns same value as arg",
                "true",
                checkBox.toHumanReadableString(contextValue, true));
    }

    @Test
    public void testValueToHumanReadableStringNull() {
        Assert.assertEquals("toHumanReadableString returns null for null",
                "null",
                checkBox.toHumanReadableString(contextValue, null));
    }

    @Test
    public void testValueFromHumanReadableString() {
        Assert.assertEquals("fromHumanReadableString returns same value as args",
                true,
                checkBox.fromHumanReadableString(contextValue, "true"));

        Assert.assertEquals("fromHumanReadableString with 0 returns false",
                false,
                checkBox.fromHumanReadableString(contextValue, "false"));

        Assert.assertEquals("fromHumanReadableString with num returns true",
                true,
                checkBox.fromHumanReadableString(contextValue, "123490"));

        Assert.assertNull("fromHumanReadableString with some string returns false",
                checkBox.fromHumanReadableString(contextValue, "hello"));
    }

    @Test
    public void testValueFromHumanReadableStringNull() {
        Assert.assertNull("fromHumanReadableString returns null for null",
                checkBox.fromHumanReadableString(contextValue, null));
    }



}
