package org.autogui.base.type;

import org.autogui.GuiIncluded;
import org.autogui.GuiInits;
import org.autogui.base.annotation.GuiInitWindow;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

public class GuiTypeValueTest {

    GuiTypeBuilder builder;
    GuiTypeValue intType;

    public GuiTypeValueTest() {}

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        intType = (GuiTypeValue) builder.get(int.class);
    }

    @Test
    public void testValueIntValue() {
        Assert.assertEquals("always no-update",
                GuiUpdatedValue.NO_UPDATE, intType.getValue());
    }

    @Test
    public void testValueIntValueInherited() {
        Assert.assertEquals("returns inherited",
                GuiUpdatedValue.of(123),
                intType.getValue(123));
    }

    @Test
    public void testValueIntUpdatedValue() {
        Assert.assertEquals("always no-update",
                GuiUpdatedValue.NO_UPDATE,
                intType.updatedValue(123));
    }

    @Test
    public void testValueIntUpdatedValueInherited() {
        Assert.assertEquals("if different inherited value, returns inherited",
                GuiUpdatedValue.of(456),
                intType.updatedValue(123, 456));
    }

    @Test
    public void testValueIntUpdatedValueInheritedSame() {
        Assert.assertEquals("if prevValue and inherited is same value, returns no-update",
                GuiUpdatedValue.NO_UPDATE,
                intType.updatedValue(123, 123));
    }

    @Test
    public void testValueIntWritable() {
        Assert.assertTrue("always true",
                intType.isWritable(123));
    }

    @Test
    public void testValueIntWrite() {
        Assert.assertEquals("always newValue",
                456,
                intType.writeValue(456));
    }

    @Test
    public void testValueIntWritePrev() {
        Assert.assertEquals("always newValue",
                456,
                intType.writeValue(123, 456));
    }

    @SuppressWarnings("all")
    @Test
    public void testValueIntEqualsSame() {
        Assert.assertTrue("use equals",
                intType.equals(Integer.valueOf(1234567), Integer.valueOf(1234567)));
    }

    @SuppressWarnings("all")
    @Test
    public void testValueIntEqualsNull() {
        Assert.assertFalse("use equals with null",
                intType.equals(null, Integer.valueOf(1234567)));
    }

    public static class Example {
    }

    @GuiIncluded(description = "hello", keyStroke = "shift X")
    @GuiInits(window = @GuiInitWindow(width = 123))
    public static class ExampleAnnotated implements AutoCloseable {
        @Override
        public void close() {}
    }

    @Test
    public void testDescription() {
        var t = new GuiTypeValue(Example.class);
        Assert.assertEquals("no description", "", t.getDescription());

        var t2 = new GuiTypeValue(ExampleAnnotated.class);
        Assert.assertEquals("description", "hello", t2.getDescription());

        var t3 = new GuiTypeValue("UnknownType");
        Assert.assertEquals("no class description", "", t3.getAcceleratorKeyStroke());
    }

    @Test
    public void testAcceleratorKeyStroke() {
        var t = new GuiTypeValue(Example.class);
        Assert.assertEquals("no key", "", t.getAcceleratorKeyStroke());

        var t2 = new GuiTypeValue(ExampleAnnotated.class);
        Assert.assertEquals("key", "shift X", t2.getAcceleratorKeyStroke());

        var t3 = new GuiTypeValue("UnknownType");
        Assert.assertEquals("no class key", "", t3.getAcceleratorKeyStroke());
    }

    @Test
    public void testInits() {
        var t = new GuiTypeValue(Example.class);
        Assert.assertEquals("no inits", 0, t.getInits().window().width());

        var t2 = new GuiTypeValue(ExampleAnnotated.class);
        Assert.assertEquals("inits", 123, t2.getInits().window().width());

        var t3 = new GuiTypeValue("UnknownType");
        Assert.assertEquals("no class inits", 0, t3.getInits().window().width());
    }

    @Test
    public void testCreateValue() {
        var t = new GuiTypeValue(Example.class);
        var obj = t.createNewValue();
        Assert.assertNotNull("create by default constructor", obj);
    }
    @Test
    public void testCreateValueNoConstructor() {
        try {
            var obj = intType.createNewValue();
            Assert.assertNull("null by no default constructor", obj);
        } catch (Exception e) {
            Assert.fail("no constructor");
        }
    }

    @Test
    public void testAutoClosable() {
        var t = new GuiTypeValue(ExampleAnnotated.class);
        Assert.assertTrue("autocloseable", t.isAutoCloseable());
    }

    @Test
    public void testEquals() {
        var map = new HashMap<Object, String>();
        map.put(new GuiTypeValue(Example.class), "a");
        map.put(new GuiTypeValue(ExampleAnnotated.class), "b");
        map.put(new GuiTypeValue(Example.class.getName()), "c");
        map.put(Example.class.getName(), "d"); //same hash but diffrent equality

        Assert.assertEquals("equals by name", 3, map.size());
        Assert.assertEquals("get by type", "c", map.get(new GuiTypeValue(Example.class)));
        Assert.assertEquals("get by name", "c", map.get(new GuiTypeValue(Example.class.getName())));
        Assert.assertEquals("get by name str", "d", map.get(Example.class.getName()));
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals("hashCode",
                new GuiTypeCollection(List.class, new GuiTypeValue(Integer.class)).hashCode(),
                new GuiTypeCollection(List.class, new GuiTypeValue(Integer.class)).hashCode());
    }

    @Test
    public void testChildren() {
        Assert.assertEquals("no children", List.of(), intType.getChildren());
    }

}
