package org.autogui.test.base.mapping;

import org.autogui.GuiIncluded;
import org.autogui.base.log.GuiLogEntry;
import org.autogui.base.log.GuiLogEntryString;
import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiReprValueStringField;
import org.autogui.base.type.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    GuiMappingContext contextValueNoPrefs;
    GuiMappingContext contextProp;

    GuiMappingContext contextReadOnly;

    GuiPreferences prefs;

    List<GuiLogEntry> logEntries = new ArrayList<>();

    public GuiReprValueStringFieldTest() {
    }

    @BeforeEach
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
        prefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), contextValue);
        contextValue.setPreferences(prefs);

        contextProp = new GuiReprObjectPaneTest.GuiMappingContextForDebug(property, fld);

        contextValueNoPrefs = new GuiReprObjectPaneTest.GuiMappingContextForDebug(type, fld, contextProp, GuiMappingContext.GuiSourceValue.of(obj));
        contextValueNoPrefs.addToParent();
        prefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory() {
            @Override
            public void flush() {
                throw new IllegalArgumentException("flush");
            }
        }, contextValueNoPrefs);
        contextValueNoPrefs.setPreferences(prefs);


        contextReadOnly = new GuiMappingContext(propertyReadOnly, fld);

        GuiLogManager.setManager(new GuiLogManager() {
            @Override
            protected <LogType extends GuiLogEntry> LogType logCreated(LogType log) {
                logEntries.add(log);
                return super.logCreated(log);
            }
        });
    }

    @GuiIncluded
    public static class TestObjRepr {
        @GuiIncluded
        public String value;

        @GuiIncluded
        public String getReadOnly() {
            return "hello";
        }

        public TestObjRepr() {
        }
    }

    @Test
    public void testValueMatch() {
        GuiMappingContext ctx = new GuiMappingContext(type, value);
        assertTrue(fld.match(ctx), "match with typed context");
        assertEquals(fld, ctx.getRepresentation(), "match sets contexts repr");
    }


    @Test
    public void testValueMatchProp() {
        GuiMappingContext ctx = new GuiMappingContext(property, value);
        assertTrue(fld.match(ctx), "match prop with typed context");
        assertEquals(fld, ctx.getRepresentation(), "match prop sets contexts repr");
    }

    @Test
    public void testValueGetValueNoParentNoPrev() throws Throwable {
        assertEquals(GuiUpdatedValue.NO_UPDATE, fld.getValue(contextValue, GuiMappingContext.NO_SOURCE, GuiReprValue.NONE, GuiMappingContext.NO_SOURCE), "getValue with no-source parent and no-source prev return no-update");
    }


    @Test
    public void testValueGetValueParentNoPrev() throws Throwable {
        assertEquals(GuiUpdatedValue.of("hello"), fld.getValue(contextValue, GuiMappingContext.GuiSourceValue.of("hello"), GuiReprValue.NONE, GuiMappingContext.NO_SOURCE), "getValue with a parent and no-source prev return no-update");
    }


    @Test
    public void testValueGetValueParentPrevSame() throws Throwable {
        assertEquals(GuiUpdatedValue.NO_UPDATE, fld.getValue(contextValue, GuiMappingContext.GuiSourceValue.of("hello"), GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")), "getValue with a parent and same prev return no-update");
    }

    @Test
    public void testValueGetValueNoParentPrev() throws Throwable {
        assertEquals(GuiUpdatedValue.NO_UPDATE, fld.getValue(contextValue, GuiMappingContext.NO_SOURCE, GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")), "getValue with no-parent and a prev return no-update");
    }

    @Test
    public void testValueGetValueParentPrevDiff() throws Throwable {
        assertEquals(GuiUpdatedValue.of("world"), fld.getValue(contextValue, GuiMappingContext.GuiSourceValue.of("world"), GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")), "getValue with a parent and diff prev return the parent");
    }

    @Test
    public void testValueGetValuePropNoParentNoPrev() throws Throwable {
        assertEquals(GuiUpdatedValue.NO_UPDATE, fld.getValue(contextProp, GuiMappingContext.NO_SOURCE, GuiReprValue.NONE, GuiMappingContext.NO_SOURCE), "getValue with no parent and no prev return no-update");
    }

    @Test
    public void testValueGetValuePropParentPrevDiff() throws Throwable {
        assertEquals(GuiUpdatedValue.of("hello"), fld.getValue(contextProp, GuiMappingContext.GuiSourceValue.of(obj), GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("world")), "getValue with a parent and diff prev return the prop value");
    }

    @Test
    public void testValueGetValuePropNoParentPrev() throws Throwable {
        assertEquals(GuiUpdatedValue.NO_UPDATE, fld.getValue(contextProp, GuiMappingContext.NO_SOURCE, GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("world")), "getValue with no-parent and prev return no-update");
    }


    @Test
    public void testValueGetValuePropParentNoPrev() throws Throwable {
        assertEquals(GuiUpdatedValue.of("hello"), fld.getValue(contextProp, GuiMappingContext.GuiSourceValue.of(obj), GuiReprValue.NONE, GuiMappingContext.NO_SOURCE), "getValue with a parent and diff prev return the prop value");
    }

    @Test
    public void testValueGetValuePropParentPrevSame() throws Throwable {
        assertEquals(GuiUpdatedValue.NO_UPDATE, fld.getValue(contextProp, GuiMappingContext.GuiSourceValue.of(obj), GuiReprValue.NONE, GuiMappingContext.GuiSourceValue.of("hello")), "getValue with a parent and same prev return no-update");
    }

    @Test
    public void testValueUpdateNone() throws Throwable {
        assertEquals("hello", fld.update(contextValue, GuiMappingContext.NO_SOURCE, "hello", GuiReprValue.NONE), "update returns newValue");
    }

    @Test
    public void testValueUpdate() throws Throwable {
        contextValue.setSource(GuiMappingContext.GuiSourceValue.of("hello"));
        assertEquals("hello", fld.update(contextValue, GuiMappingContext.NO_SOURCE, "hello", GuiReprValue.NONE), "update with same prev source returns newValue");
    }

    @Test
    public void testValueUpdateNoneParent() throws Throwable {
        assertEquals("hello", fld.update(contextValue, GuiMappingContext.GuiSourceValue.of("hello"), "hello", GuiReprValue.NONE), "update returns newValue");
    }

    @Test
    public void testValueUpdateParent() throws Throwable {
        contextValue.setSource(GuiMappingContext.GuiSourceValue.of("hello"));
        assertEquals("hello", fld.update(contextValue, GuiMappingContext.GuiSourceValue.of("hello"), "hello", GuiReprValue.NONE), "update with same prev source returns newValue");
    }

    @Test
    public void testValueUpdateProp() throws Throwable {
        assertEquals("world", fld.update(contextProp, GuiMappingContext.GuiSourceValue.of(obj), "world", GuiReprValue.NONE), "update returns newValue");
        assertEquals("world", obj.value, "update updates field");
    }

    @Test
    public void testValueGetParentSourceNone() throws Throwable {
        assertEquals(GuiMappingContext.NO_SOURCE, fld.getParentSource(contextValue, GuiReprValue.NONE), "getParentSource with no parent returns none");
    }

    @Test
    public void testValueGetValueWithoutNoUpdate() throws Throwable {
        assertEquals("hello", fld.getValueWithoutNoUpdate(contextValue, GuiMappingContext.GuiSourceValue.of("hello"), GuiReprValue.NONE), "getValueWithoutNoUpdate returns parent value");
    }

    @Test
    public void testValueGetValueWithoutNoUpdateNone() throws Throwable {
        assertNull(fld.getValueWithoutNoUpdate(contextValue, GuiMappingContext.NO_SOURCE, GuiReprValue.NONE), "getValueWithoutNoUpdate with no parent returns null");
    }

    //------------------

    @Test
    public void testValueIsEditable() {
        assertTrue(fld.isEditable(contextProp), "isEditable prop returns true");
    }

    @Test
    public void testValueIsEditableValue() {
        assertTrue(fld.isEditable(contextValue), "isEditable value returns true");
    }

    @Test
    public void testValueIsEditableReadOnly() {
        assertFalse(fld.isEditable(contextReadOnly), "isEditable returns false for read-only prop");
    }

    //------------------

    @Test
    public void testValueToJson() {
        assertEquals(value, fld.toJson(contextValue, value), "toJson returns same value as arg");
    }

    @Test
    public void testValueToJsonIllegal() {
        assertNull(fld.toJson(contextValue, 123), "toJson returns null for illegal arg");
    }

    @Test
    public void testValueToJsonWithNamed() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("hello", value);
        assertEquals(map, fld.toJsonWithNamed(contextValue, new GuiReprValue.NamedValue("hello", value)), "toJsonWithNamed returns value of NamedValue arg");
    }

    @Test
    public void testValueToJsonWithNamedNonNamedValue() {
        assertEquals(value, fld.toJsonWithNamed(contextValue, value), "toJsonWithNamed returns same value as non NamedValue arg");
    }

    @Test
    public void testValueFromJson() {
        assertFalse(fld.isJsonSetter(), "isJsonSetter is false for String");
        assertEquals(value, fld.fromJson(contextValue, null, value), "fromJson returns same value as arg");
    }

    @Test
    public void testValueFromJsonIllegal() {
        assertNull(fld.fromJson(contextValue, null, 123), "fromJson returns null for illegal arg");
    }

    @Test
    public void testValueFromJsonWithNamed() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("value", value);
        assertEquals(new GuiReprValue.NamedValue("value", value), fld.fromJsonWithNamed(contextProp, new GuiReprValue.NamedValue("value", "?"), map), "fromJson returns NamedValue for NamedValue target and Map value");
    }

    @Test
    public void testValueFromJsonWithNamedNonNamedValue() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("value", value);
        assertEquals(value, fld.fromJsonWithNamed(contextProp, "hello", map), "fromJson returns non NamedValue for non NamedValue target and Map value");
    }


    @Test
    public void testValueFromJsonWithNamedNonMapTarget() {
        assertNull(fld.fromJsonWithNamed(contextProp, "hello", "hello"), "fromJson returns null for non Map value");
    }


    @Test
    public void testValueToHumanReadableString() {
        assertEquals(value, fld.toHumanReadableString(contextValue, value), "toHumanReadableString returns same value as arg");
    }

    @Test
    public void testValueToHumanReadableStringNull() {
        assertEquals("null", fld.toHumanReadableString(contextValue, null), "toHumanReadableString returns null for null");
    }

    @Test
    public void testValueFromHumanReadableString() {
        assertEquals(value, fld.fromHumanReadableString(contextValue, value), "fromHumanReadableString returns same value as args");
    }

    @Test
    public void testValueFromHumanReadableStringNull() {
        assertNull(fld.fromHumanReadableString(contextValue, null), "fromHumanReadableString returns null for null");
    }


    @Test
    public void testAddHistoryValueError() {
        fld.addHistoryValue(contextValueNoPrefs, "INVALID");

        assertEquals(1, logEntries.size(), "flush cause error for test");
        assertTrue(logEntries.getFirst() instanceof GuiLogEntryString, "string log for illegal argument");
    }

    @Test
    public void testCheckAndUpdateSource() {
        contextProp.setSource(GuiMappingContext.GuiSourceValue.of(new TestObjRepr()));

        contextValueNoPrefs.setSource(GuiMappingContext.GuiSourceValue.of("hello"));
        fld.checkAndUpdateSource(contextValueNoPrefs);

        assertEquals(1, logEntries.size(), "flush cause error for test");
        assertTrue(logEntries.getFirst() instanceof GuiLogEntryString, "string log for illegal argument");
    }

    @Test
    public void testAddHistoryValue() {
        fld.addHistoryValue(contextValue, "hello");
        assertEquals(1, contextValue.getPreferences().getHistoryValues().size(), "histories");

        var hist = contextValue.getPreferences().getHistoryValues().getFirst();
        assertEquals("hello", hist.getValue(), "histroy value");
    }
}
