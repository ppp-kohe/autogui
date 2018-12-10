package org.autogui.test.base.mapping;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiReprValueEnumComboBox;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GuiReprValueEnumComboBoxTest {
    GuiReprValueEnumComboBox comboBox;

    GuiTypeBuilder builder;
    GuiTypeValue type;

    GuiMappingContext contextValue;

    public enum TestEnum {
        Hello,
        World,
        hello
    }

    @Before
    public void setUp() {
        comboBox = new GuiReprValueEnumComboBox();

        builder = new GuiTypeBuilder();
        type = (GuiTypeValue) builder.get(TestEnum.class);

        contextValue = new GuiReprObjectPaneTest.GuiMappingContextForDebug(type, comboBox);
    }


    @Test
    public void testValueMatch() {
        GuiMappingContext ctx = new GuiMappingContext(type, TestEnum.Hello);
        Assert.assertTrue("match with enum",
                comboBox.match(ctx));
    }

    @Test
    public void testValueUpdate() throws Throwable {
        contextValue.setSource(GuiMappingContext.GuiSourceValue.of(TestEnum.World));
        Assert.assertEquals("updated value obj",
                TestEnum.Hello,
                comboBox.update(contextValue, GuiMappingContext.NO_SOURCE,
                        TestEnum.Hello, GuiReprValue.NONE));
    }

    @Test
    public void testValueUpdateNull() throws Throwable {
        contextValue.setSource(GuiMappingContext.GuiSourceValue.of(null));
        Assert.assertNull("updated value null",
                comboBox.update(contextValue, GuiMappingContext.NO_SOURCE,
                        null, GuiReprValue.NONE));
    }

    @Test
    public void testValueGetEnumValue() {
        Assert.assertEquals("getBooleanValue with num returns ordinal value",
                TestEnum.World,
                comboBox.getEnumValue(contextValue, "1"));

        Assert.assertNull("getBooleanValue with out-of-bounds num returns null",
                comboBox.getEnumValue(contextValue, "123"));

        Assert.assertEquals("getBooleanValue with string returns named value",
                TestEnum.Hello,
                comboBox.getEnumValue(contextValue, "Hello"));

        Assert.assertEquals("getBooleanValue with different case string returns named value",
                TestEnum.hello,
                comboBox.getEnumValue(contextValue, "hello"));

        Assert.assertEquals("getBooleanValue with string returns case-insensitive matching named value",
                TestEnum.World,
                comboBox.getEnumValue(contextValue, "WORLD"));

        Assert.assertEquals("getBooleanValue with string returns case-insensitive matching lower indexed named value",
                TestEnum.Hello,
                comboBox.getEnumValue(contextValue, "HELLO"));

        Assert.assertNull("getBooleanValue with other string returns null",
                comboBox.getEnumValue(contextValue, "Unknown"));
    }
    //////////

    @Test
    public void testValueToJson() {
        Assert.assertEquals("toJson returns name of arg",
                "Hello",
                comboBox.toJson(contextValue, TestEnum.Hello));

        Assert.assertEquals("toJson returns case-sensitive name of arg",
                "hello",
                comboBox.toJson(contextValue, TestEnum.hello));
    }

    @Test
    public void testValueToJsonIllegal() {
        Assert.assertNull("toJson returns null for illegal arg",
                comboBox.toJson(contextValue, 123));
    }

    @Test
    public void testValueFromJson() {
        Assert.assertFalse("isJsonSetter is false for boolean",
                comboBox.isJsonSetter());
        Assert.assertEquals("fromJson returns named value",
                TestEnum.hello,
                comboBox.fromJson(contextValue, null, "hello"));
    }

    @Test
    public void testValueFromJsonIllegal() {
        Assert.assertNull("fromJson returns null for illegal arg",
                comboBox.fromJson(contextValue, null, 123));
    }


    @Test
    public void testValueToHumanReadableString() {
        Assert.assertEquals("toHumanReadableString returns name of arg",
                "Hello",
                comboBox.toHumanReadableString(contextValue, TestEnum.Hello));
    }

    @Test
    public void testValueToHumanReadableStringNull() {
        Assert.assertEquals("toHumanReadableString returns null for null",
                "null",
                comboBox.toHumanReadableString(contextValue, null));
    }

    @Test
    public void testValueFromHumanReadableString() {
        Assert.assertEquals("fromHumanReadableString returns case-sensitive named value",
                TestEnum.hello,
                comboBox.fromHumanReadableString(contextValue, "hello"));

        Assert.assertNull("fromHumanReadableString with num returns null",
                comboBox.fromHumanReadableString(contextValue, "1"));


        Assert.assertNull("fromHumanReadableString with some string returns null",
                comboBox.fromHumanReadableString(contextValue, "unknown"));
    }

    @Test
    public void testValueFromHumanReadableStringNull() {
        Assert.assertNull("fromHumanReadableString returns null for null",
                comboBox.fromHumanReadableString(contextValue, null));
    }

    @Test
    public void testValueGetEnumConstants() {
        Assert.assertArrayEquals("getEnumConstants returns array of enum members",
                new TestEnum[] {TestEnum.Hello, TestEnum.World, TestEnum.hello},
                comboBox.getEnumConstants(contextValue));
    }

}
