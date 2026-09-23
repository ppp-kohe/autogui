package org.autogui.test.base.annotation;

import org.autogui.GuiInits;
import org.autogui.base.annotation.GuiDefaultInits;
import org.autogui.base.annotation.GuiInitWindow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GuiDefaultsInitsTest {
    @org.junit.jupiter.api.Test
    public void testNew() {
        var i = new GuiDefaultInits();
        var is = i.getClass().getAnnotation(GuiInits.class);
        assertEquals(0, is.window().height(), "default");
    }

    @Test
    public void testGet() {
        var is = GuiDefaultInits.get();
        assertEquals(0, is.window().height(), "default");
    }

    @GuiInits(window = @GuiInitWindow(height = 123))
    public static class Example {
    }

    @GuiInits(window = @GuiInitWindow(height = 456))
    public void exampleMethod() {
    }

    public static class ExampleNoInits {
    }

    @Test
    public void testGetOrNull() throws Exception {
        assertNull(GuiDefaultInits.getOrNull(null), "null for null");
        assertEquals(123, GuiDefaultInits.getOrNull(Example.class).window().height(), "get attached for class");
        assertEquals(456, GuiDefaultInits.getOrNull(getClass().getMethod("exampleMethod")).window().height(), "get attached for method");
        assertNull(GuiDefaultInits.getOrNull(ExampleNoInits.class), "null for not attached");
    }

    @Test
    public void testGetWithElement() throws Exception {
        assertEquals(0, GuiDefaultInits.get(null).window().height(), "default for null");
        assertEquals(123, GuiDefaultInits.get(Example.class).window().height(), "get attached for class");
        assertEquals(456, GuiDefaultInits.get(getClass().getMethod("exampleMethod")).window().height(), "get attached for method");
        assertEquals(0, GuiDefaultInits.get(ExampleNoInits.class).window().height(), "default for not attached");
    }
}
