package org.autogui.test.base.type;

import org.autogui.GuiIncluded;
import org.autogui.base.type.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuiTypeMemberPropertyTest {
    GuiTypeBuilder builder;
    GuiTypeObject objType;
    GuiTypeMemberProperty property;
    GuiTypeMemberProperty propertyMethod;

    TestObj obj;

    public GuiTypeMemberPropertyTest() {
    }

    @BeforeEach
    public void setUp() {
        builder = new GuiTypeBuilder();
        objType = (GuiTypeObject) builder.get(TestObj.class);

        property = (GuiTypeMemberProperty) objType.getMemberByName("value");
        propertyMethod = (GuiTypeMemberProperty) objType.getMemberByName("str");

        obj = new TestObj();
        obj.value = 123;
        obj.str = "hello";
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded(description = "hello-desc")
        public int value;

        public String str;

        @GuiIncluded(description = "getter", keyStroke = "X")
        public String getStr() {
            return str;
        }

        @GuiIncluded(description = "setter", keyStroke = "Y")
        public void setStr(String s) {
            this.str = s;
        }

        public TestObj() {
        }
    }

    @Test
    public void testPropName() {
        assertEquals("value", property.getName(), "property name is the field name");
    }

    @Test
    public void testPropType() {
        assertEquals(builder.get(int.class), property.getType(), "property type is the field type");
    }

    @Test
    public void testPropChildren() {
        assertEquals(Collections.singletonList(builder.get(int.class)), property.getChildren(), "property children is its type");
    }

    @Test
    public void testPropFieldExecuteGet() throws Throwable {
        assertEquals(GuiUpdatedValue.of(123), property.executeGet(obj), "property executeGet returns the field value");
    }

    @Test
    public void testPropMethodExecuteGet() throws Throwable {
        assertEquals(GuiUpdatedValue.of("hello"), propertyMethod.executeGet(obj), "property executeGet returns the method returned value");
    }

    @Test
    public void testPropFieldExecuteGetNull() throws Throwable {
        assertEquals(GuiUpdatedValue.NO_UPDATE, property.executeGet(null), "property executeGet null returns no-update");
    }

    @Test
    public void testPropMethodExecuteGetNull() throws Throwable {
        assertEquals(GuiUpdatedValue.NO_UPDATE, propertyMethod.executeGet(null), "property method executeGet null returns no-update");
    }

    @Test
    public void testPropExecuteGetPrevSame() throws Throwable {
        assertEquals(GuiUpdatedValue.NO_UPDATE, property.executeGet(obj, 123), "property executeGet with same value of prevValue returns no-update");
    }

    @Test
    public void testPropExecuteGetPrev() throws Throwable {
        assertEquals(GuiUpdatedValue.of(123), property.executeGet(obj, 456), "property executeGet with diff value of prevValue returns the value");
    }

    @Test
    public void testPropExecuteSet() throws Throwable {
        assertEquals(456, property.executeSet(obj, 456), "property executeSet returns the value");

        assertEquals(456, obj.value, "property executeSet updates the field value");
    }

    @Test
    public void testPropMethodExecuteSet() throws Throwable {
        assertEquals("world", propertyMethod.executeSet(obj, "world"), "property method executeSet returns the value");

        assertEquals("world", obj.str, "property method executeSet updates the field value");

    }

    @Test
    public void testPropWritable() {
        assertTrue(property.isWritable(), "property field is writable");
    }

    @Test
    public void testPropMethodWritable() {
        assertTrue(property.isWritable(), "property with setter is writable");
    }

    @Test
    public void testPropDescription() {
        assertEquals("hello-desc", property.getDescription(), "property desc from @GuiIncluded(desc)");
    }


    @Test
    public void testPropDescriptionMethods() {
        assertEquals("getter\tsetter", propertyMethod.getDescription(), "property desc combined from @GuiIncluded(desc)s: getter, setter with tab");
    }

    @Test
    public void testPropAcceleratorKeyStroke() {
        assertEquals("X", propertyMethod.getAcceleratorKeyStroke(), "property key is selected from getter, setter");
    }

    @Test
    public void testPropGetField() {
        var p = new GuiTypeMemberProperty("str", null, null, "str", new GuiTypeValue(String.class));
        p.setOwner(new GuiTypeObject(TestObj.class));
        var fld = p.getField();
        assertEquals("str", fld.getName(), "getField from name");
        assertEquals(String.class, fld.getType(), "getField from name String");
    }
}
