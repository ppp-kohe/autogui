package org.autogui.swing;

import org.autogui.swing.util.SettingsWindow;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;

public class GuiSwingWindowTest extends GuiSwingTestCase {

    TestObj obj;
    JFrame frame;

    @Before
    public void setUp() {
        obj = new TestObj();
    }

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public static class TestObj {
        String prop;
        void action() {
            System.err.println("hello");
        }
    }

    @Test
    public void testCreate() {
        GuiSwingWindow w = runGet(() -> GuiSwingWindow.creator().withTypeBuilderRelaxed()
            .withKeyBindingWithoutAutomaticBindings()
            .withLogStatusDisabled()
            .withPreferencesOnMemory()
            .withSettingWindow(SettingsWindow.get())
            .createWindow(obj));
        w.setVisible(true);
        Assert.assertEquals("source value",
                obj,
                runGet(() -> w.getContext().getSource().getValue()));
        frame = w;
    }
}
