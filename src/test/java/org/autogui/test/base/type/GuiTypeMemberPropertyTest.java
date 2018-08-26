package org.autogui.test.base.type;

import org.autogui.GuiIncluded;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeMemberProperty;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.base.type.GuiUpdatedValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class GuiTypeMemberPropertyTest {
    GuiTypeBuilder builder;
    GuiTypeObject objType;
    GuiTypeMemberProperty property;
    GuiTypeMemberProperty propertyMethod;

    TestObj obj;

    @Before
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
    }

    @Test
    public void testPropName() {
        Assert.assertEquals("property name is the field name",
                "value", property.getName());
    }

    @Test
    public void testPropType() {
        Assert.assertEquals("property type is the field type",
                builder.get(int.class),
                property.getType());
    }

    @Test
    public void testPropChildren() {
        Assert.assertEquals("property children is its type",
                Collections.singletonList(builder.get(int.class)),
                property.getChildren());
    }

    @Test
    public void testPropFieldExecuteGet() throws Throwable {
        Assert.assertEquals("property executeGet returns the field value",
                GuiUpdatedValue.of(123),
                property.executeGet(obj));
    }

    @Test
    public void testPropMethodExecuteGet() throws Throwable {
        Assert.assertEquals("property executeGet returns the method returned value",
                GuiUpdatedValue.of("hello"),
                propertyMethod.executeGet(obj));
    }

    @Test
    public void testPropFieldExecuteGetNull() throws Throwable {
        Assert.assertEquals("property executeGet null returns no-update",
                GuiUpdatedValue.NO_UPDATE,
                property.executeGet(null));
    }

    @Test
    public void testPropMethodExecuteGetNull() throws Throwable {
        Assert.assertEquals("property method executeGet null returns no-update",
                GuiUpdatedValue.NO_UPDATE,
                propertyMethod.executeGet(null));
    }

    @Test
    public void testPropExecuteGetPrevSame() throws Throwable {
        Assert.assertEquals("property executeGet with same value of prevValue returns no-update",
                GuiUpdatedValue.NO_UPDATE,
                property.executeGet(obj, 123));
    }

    @Test
    public void testPropExecuteGetPrev() throws Throwable {
        Assert.assertEquals("property executeGet with diff value of prevValue returns the value",
                GuiUpdatedValue.of(123),
                property.executeGet(obj, 456));
    }

    @Test
    public void testPropExecuteSet() throws Throwable {
        Assert.assertEquals("property executeSet returns the value",
                456,
                property.executeSet(obj, 456));

        Assert.assertEquals("property executeSet updates the field value",
                456, obj.value);
    }

    @Test
    public void testPropMethodExecuteSet() throws Throwable {
        Assert.assertEquals("property method executeSet returns the value",
                "world",
                propertyMethod.executeSet(obj, "world"));

        Assert.assertEquals("property method executeSet updates the field value",
                "world", obj.str);

    }

    @Test
    public void testPropWritable() {
        Assert.assertTrue("property field is writable",
                property.isWritable());
    }

    @Test
    public void testPropMethodWritable() {
        Assert.assertTrue("property with setter is writable",
                property.isWritable());
    }

    @Test
    public void testPropDescription() {
        Assert.assertEquals("property desc from @GuiIncluded(desc)",
                "hello-desc",
                property.getDescription());
    }


    @Test
    public void testPropDescriptionMethods() {
        Assert.assertEquals("property desc combined from @GuiIncluded(desc)s: getter, setter with tab",
                "getter\tsetter",
                propertyMethod.getDescription());
    }

    @Test
    public void testPropAcceleratorKeyStroke() {
        Assert.assertEquals("property key is selected from getter, setter",
                "X",
                propertyMethod.getAcceleratorKeyStroke());
    }
}
