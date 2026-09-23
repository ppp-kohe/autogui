package org.autogui.test.base.type;

import org.autogui.GuiIncluded;
import org.autogui.GuiInits;
import org.autogui.base.annotation.GuiInitWindow;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeCollection;
import org.autogui.base.type.GuiTypeValue;
import org.autogui.base.type.GuiUpdatedValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GuiTypeValueTest {

    GuiTypeBuilder builder;
    GuiTypeValue intType;

    public GuiTypeValueTest() {
    }

    @BeforeEach
    public void setUp() {
        builder = new GuiTypeBuilder();
        intType = (GuiTypeValue) builder.get(int.class);
    }

    @Test
    public void testValueIntValue() {
        assertEquals(GuiUpdatedValue.NO_UPDATE, intType.getValue(), "always no-update");
    }

    @Test
    public void testValueIntValueInherited() {
        assertEquals(GuiUpdatedValue.of(123), intType.getValue(123), "returns inherited");
    }

    @Test
    public void testValueIntUpdatedValue() {
        assertEquals(GuiUpdatedValue.NO_UPDATE, intType.updatedValue(123), "always no-update");
    }

    @Test
    public void testValueIntUpdatedValueInherited() {
        assertEquals(GuiUpdatedValue.of(456), intType.updatedValue(123, 456), "if different inherited value, returns inherited");
    }

    @Test
    public void testValueIntUpdatedValueInheritedSame() {
        assertEquals(GuiUpdatedValue.NO_UPDATE, intType.updatedValue(123, 123), "if prevValue and inherited is same value, returns no-update");
    }

    @Test
    public void testValueIntWritable() {
        assertTrue(intType.isWritable(123), "always true");
    }

    @Test
    public void testValueIntWrite() {
        assertEquals(456, intType.writeValue(456), "always newValue");
    }

    @Test
    public void testValueIntWritePrev() {
        assertEquals(456, intType.writeValue(123, 456), "always newValue");
    }

    @SuppressWarnings("all")
    @Test
    public void testValueIntEqualsSame() {
        assertTrue(intType.equals(Integer.valueOf(1234567), Integer.valueOf(1234567)), "use equals");
    }

    @SuppressWarnings("all")
    @Test
    public void testValueIntEqualsNull() {
        assertFalse(intType.equals(null, Integer.valueOf(1234567)), "use equals with null");
    }

    public static class Example {
    }

    @GuiIncluded(description = "hello", keyStroke = "shift X")
    @GuiInits(window = @GuiInitWindow(width = 123))
    public static class ExampleAnnotated implements AutoCloseable {
        @Override
        public void close() {
        }
    }

    @Test
    public void testDescription() {
        var t = new GuiTypeValue(Example.class);
        assertEquals("", t.getDescription(), "no description");

        var t2 = new GuiTypeValue(ExampleAnnotated.class);
        assertEquals("hello", t2.getDescription(), "description");

        var t3 = new GuiTypeValue("UnknownType");
        assertEquals("", t3.getAcceleratorKeyStroke(), "no class description");
    }

    @Test
    public void testAcceleratorKeyStroke() {
        var t = new GuiTypeValue(Example.class);
        assertEquals("", t.getAcceleratorKeyStroke(), "no key");

        var t2 = new GuiTypeValue(ExampleAnnotated.class);
        assertEquals("shift X", t2.getAcceleratorKeyStroke(), "key");

        var t3 = new GuiTypeValue("UnknownType");
        assertEquals("", t3.getAcceleratorKeyStroke(), "no class key");
    }

    @Test
    public void testInits() {
        var t = new GuiTypeValue(Example.class);
        assertEquals(0, t.getInits().window().width(), "no inits");

        var t2 = new GuiTypeValue(ExampleAnnotated.class);
        assertEquals(123, t2.getInits().window().width(), "inits");

        var t3 = new GuiTypeValue("UnknownType");
        assertEquals(0, t3.getInits().window().width(), "no class inits");
    }

    @Test
    public void testCreateValue() {
        var t = new GuiTypeValue(Example.class);
        var obj = t.createNewValue();
        assertNotNull(obj, "create by default constructor");
    }

    @Test
    public void testCreateValueNoConstructor() {
        try {
            var obj = intType.createNewValue();
            assertNull(obj, "null by no default constructor");
        } catch (Exception e) {
            fail("no constructor");
        }
    }

    @Test
    public void testAutoClosable() {
        var t = new GuiTypeValue(ExampleAnnotated.class);
        assertTrue(t.isAutoCloseable(), "autocloseable");
    }

    @Test
    public void testEquals() {
        var map = new HashMap<Object, String>();
        map.put(new GuiTypeValue(Example.class), "a");
        map.put(new GuiTypeValue(ExampleAnnotated.class), "b");
        map.put(new GuiTypeValue(Example.class.getName()), "c");
        map.put(Example.class.getName(), "d"); //same hash but diffrent equality

        assertEquals(3, map.size(), "equals by name");
        assertEquals("c", map.get(new GuiTypeValue(Example.class)), "get by type");
        assertEquals("c", map.get(new GuiTypeValue(Example.class.getName())), "get by name");
        assertEquals("d", map.get(Example.class.getName()), "get by name str");
    }

    @Test
    public void testHashCode() {
        assertEquals(new GuiTypeCollection(List.class, new GuiTypeValue(Integer.class)).hashCode(), new GuiTypeCollection(List.class, new GuiTypeValue(Integer.class)).hashCode(), "hashCode");
    }

    @Test
    public void testChildren() {
        assertEquals(List.of(), intType.getChildren(), "no children");
    }

}
