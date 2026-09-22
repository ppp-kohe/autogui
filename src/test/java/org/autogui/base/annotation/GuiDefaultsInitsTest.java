package org.autogui.base.annotation;

import org.autogui.GuiInits;
import org.junit.Assert;
import org.junit.Test;

public class GuiDefaultsInitsTest {
    @Test
    public void testNew() {
        var i = new GuiDefaultInits();
        var is = i.getClass().getAnnotation(GuiInits.class);
        Assert.assertEquals("default", 0, is.window().height());
    }

    @Test
    public void testGet() {
        var is = GuiDefaultInits.get();
        Assert.assertEquals("default", 0, is.window().height());
    }

    @GuiInits(window = @GuiInitWindow(height = 123))
    public static class Example {
    }

    @GuiInits(window = @GuiInitWindow(height = 456))
    public void exampleMethod() {
    }

    public static class ExampleNoInits {}

    @Test
    public void testGetOrNull() throws Exception {
        Assert.assertNull("null for null", GuiDefaultInits.getOrNull(null));
        Assert.assertEquals("get attached for class", 123, GuiDefaultInits.getOrNull(Example.class).window().height());
        Assert.assertEquals("get attached for method", 456, GuiDefaultInits.getOrNull(getClass().getMethod("exampleMethod")).window().height());
        Assert.assertNull("null for not attached", GuiDefaultInits.getOrNull(ExampleNoInits.class));
    }

    @Test
    public void testGetWithElement() throws Exception {
        Assert.assertEquals("default for null", 0, GuiDefaultInits.get(null).window().height());
        Assert.assertEquals("get attached for class", 123, GuiDefaultInits.get(Example.class).window().height());
        Assert.assertEquals("get attached for method", 456, GuiDefaultInits.get(getClass().getMethod("exampleMethod")).window().height());
        Assert.assertEquals("default for not attached", 0, GuiDefaultInits.get(ExampleNoInits.class).window().height());
    }
}
