package org.autogui.test.base.mapping;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiReprValueBooleanCheckBox;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GuiReprValueBooleanCheckBoxTest {
    GuiReprValueBooleanCheckBox checkBox;

    GuiTypeBuilder builder;
    GuiTypeValue type;
    GuiTypeValue typeObj;

    GuiMappingContext contextValue;
    GuiMappingContext contextObj;

    public GuiReprValueBooleanCheckBoxTest() {
    }

    @BeforeEach
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
        assertTrue(checkBox.match(ctx), "match with primitive");

        GuiMappingContext ctx2 = new GuiMappingContext(typeObj, true);
        assertTrue(checkBox.match(ctx2), "match with object");
    }

    @Test
    public void testValueUpdate() throws Throwable {
        contextObj.setSource(GuiMappingContext.GuiSourceValue.of(false));
        assertEquals(Boolean.TRUE, checkBox.update(contextObj, GuiMappingContext.NO_SOURCE, true, GuiReprValue.NONE), "updated value obj");
    }

    @Test
    public void testValueUpdateNull() throws Throwable {
        contextObj.setSource(GuiMappingContext.GuiSourceValue.of(null));
        assertNull(checkBox.update(contextObj, GuiMappingContext.NO_SOURCE, null, GuiReprValue.NONE), "updated value null");
    }

    @Test
    public void testValueGetBooleanValue() {
        assertEquals(true, checkBox.getBooleanValue("true"), "getBooleanValue with true returns true");

        assertEquals(false, checkBox.getBooleanValue("false"), "getBooleanValue with false returns false");

        assertEquals(false, checkBox.getBooleanValue("0"), "getBooleanValue with 0 returns true");

        assertEquals(false, checkBox.getBooleanValue("0000"), "getBooleanValue with 0s returns true");

        assertEquals(true, checkBox.getBooleanValue("12390"), "getBooleanValue with number returns true");

        assertNull(checkBox.getBooleanValue("hello"), "getBooleanValue with String returns null");

        assertNull(checkBox.getBooleanValue(null), "getBooleanValue with null returns null");
    }

    //------------------

    @Test
    public void testValueToJson() {
        assertEquals(true, checkBox.toJson(contextValue, true), "toJson returns same value as arg");
    }

    @Test
    public void testValueToJsonIllegal() {
        assertNull(checkBox.toJson(contextValue, 123), "toJson returns null for illegal arg");
    }

    @Test
    public void testValueFromJson() {
        assertFalse(checkBox.isJsonSetter(), "isJsonSetter is false for boolean");
        assertEquals(true, checkBox.fromJson(contextValue, null, true), "fromJson returns same value as arg");
    }

    @Test
    public void testValueFromJsonIllegal() {
        assertNull(checkBox.fromJson(contextValue, null, 123), "fromJson returns null for illegal arg");
    }


    @Test
    public void testValueToHumanReadableString() {
        assertEquals("true", checkBox.toHumanReadableString(contextValue, true), "toHumanReadableString returns same value as arg");
    }

    @Test
    public void testValueToHumanReadableStringNull() {
        assertEquals("null", checkBox.toHumanReadableString(contextValue, null), "toHumanReadableString returns null for null");
    }

    @Test
    public void testValueFromHumanReadableString() {
        assertEquals(true, checkBox.fromHumanReadableString(contextValue, "true"), "fromHumanReadableString returns same value as args");

        assertEquals(false, checkBox.fromHumanReadableString(contextValue, "false"), "fromHumanReadableString with 0 returns false");

        assertEquals(true, checkBox.fromHumanReadableString(contextValue, "123490"), "fromHumanReadableString with num returns true");

        assertNull(checkBox.fromHumanReadableString(contextValue, "hello"), "fromHumanReadableString with some string returns false");
    }

    @Test
    public void testValueFromHumanReadableStringNull() {
        assertNull(checkBox.fromHumanReadableString(contextValue, null), "fromHumanReadableString returns null for null");
    }


}
