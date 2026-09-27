package org.autogui.test.base.mapping;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiReprValueEnumComboBox;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GuiReprValueEnumComboBoxTest {
    GuiReprValueEnumComboBox comboBox;

    GuiTypeBuilder builder;
    GuiTypeValue type;

    GuiMappingContext contextValue;

    public GuiReprValueEnumComboBoxTest() {
    }

    public enum TestEnum {
        Hello,
        World,
        hello
    }

    @BeforeEach
    public void setUp() {
        comboBox = new GuiReprValueEnumComboBox();

        builder = new GuiTypeBuilder();
        type = (GuiTypeValue) builder.get(TestEnum.class);

        contextValue = new GuiReprObjectPaneTest.GuiMappingContextForDebug(type, comboBox);
    }


    @Test
    public void testValueMatch() {
        GuiMappingContext ctx = new GuiMappingContext(type, TestEnum.Hello);
        assertTrue(comboBox.match(ctx),
                "match with enum");
    }

    @Test
    public void testValueUpdate() throws Throwable {
        contextValue.setSource(GuiMappingContext.GuiSourceValue.of(TestEnum.World));
        assertEquals(
                TestEnum.Hello,
                comboBox.update(contextValue, GuiMappingContext.NO_SOURCE,
                        TestEnum.Hello, GuiReprValue.NONE),
                "updated value obj");
    }

    @Test
    public void testValueUpdateNull() throws Throwable {
        contextValue.setSource(GuiMappingContext.GuiSourceValue.of(null));
        assertNull(
                comboBox.update(contextValue, GuiMappingContext.NO_SOURCE,
                        null, GuiReprValue.NONE),
                "updated value null");
    }

    @Test
    public void testValueGetEnumValue() {
        assertEquals(
                TestEnum.World,
                comboBox.getEnumValue(contextValue, "1"),
                "getBooleanValue with num returns ordinal value");

        assertNull(comboBox.getEnumValue(contextValue, "123"),
                "getBooleanValue with out-of-bounds num returns null");

        assertEquals(
                TestEnum.Hello,
                comboBox.getEnumValue(contextValue, "Hello"),
                "getBooleanValue with string returns named value");

        assertEquals(
                TestEnum.hello,
                comboBox.getEnumValue(contextValue, "hello"),
                "getBooleanValue with different case string returns named value");

        assertEquals(
                TestEnum.World,
                comboBox.getEnumValue(contextValue, "WORLD"),
                "getBooleanValue with string returns case-insensitive matching named value");

        assertEquals(
                TestEnum.Hello,
                comboBox.getEnumValue(contextValue, "HELLO"),
                "getBooleanValue with string returns case-insensitive matching lower indexed named value");

        assertNull(comboBox.getEnumValue(contextValue, "Unknown"),
                "getBooleanValue with other string returns null");
    }

    //------------------

    @Test
    public void testValueToJson() {
        assertEquals(
                "Hello",
                comboBox.toJson(contextValue, TestEnum.Hello),
                "toJson returns name of arg");

        assertEquals(
                "hello",
                comboBox.toJson(contextValue, TestEnum.hello),
                "toJson returns case-sensitive name of arg");
    }

    @Test
    public void testValueToJsonIllegal() {
        assertNull(comboBox.toJson(contextValue, 123),
                "toJson returns null for illegal arg");
    }

    @Test
    public void testValueFromJson() {
        assertFalse(comboBox.isJsonSetter(),
                "isJsonSetter is false for boolean");
        assertEquals(
                TestEnum.hello,
                comboBox.fromJson(contextValue, null, "hello"),
                "fromJson returns named value");
    }

    @Test
    public void testValueFromJsonIllegal() {
        assertNull(comboBox.fromJson(contextValue, null, 123),
                "fromJson returns null for illegal arg");
    }


    @Test
    public void testValueToHumanReadableString() {
        assertEquals(
                "Hello",
                comboBox.toHumanReadableString(contextValue, TestEnum.Hello),
                "toHumanReadableString returns name of arg");
    }

    @Test
    public void testValueToHumanReadableStringNull() {
        assertEquals(
                "null",
                comboBox.toHumanReadableString(contextValue, null),
                "toHumanReadableString returns null for null");
    }

    @Test
    public void testValueFromHumanReadableString() {
        assertEquals(
                TestEnum.hello,
                comboBox.fromHumanReadableString(contextValue, "hello"),
                "fromHumanReadableString returns case-sensitive named value");

        assertNull(comboBox.fromHumanReadableString(contextValue, "1"),
                "fromHumanReadableString with num returns null");


        assertNull(comboBox.fromHumanReadableString(contextValue, "unknown"),
                "fromHumanReadableString with some string returns null");
    }

    @Test
    public void testValueFromHumanReadableStringNull() {
        assertNull(comboBox.fromHumanReadableString(contextValue, null),
                "fromHumanReadableString returns null for null");
    }

    @Test
    public void testValueGetEnumConstants() {
        assertArrayEquals(
                new TestEnum[]{TestEnum.Hello, TestEnum.World, TestEnum.hello},
                comboBox.getEnumConstants(contextValue),
                "getEnumConstants returns array of enum members");
    }

}
