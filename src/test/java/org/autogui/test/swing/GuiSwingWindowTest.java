package org.autogui.test.swing;

import org.autogui.swing.GuiSwingWindow;
import org.autogui.swing.util.SettingsWindow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GuiSwingWindowTest extends GuiSwingTestCase {

    TestObj obj;
    JFrame frame;

    public GuiSwingWindowTest() {
    }

    @BeforeEach
    public void setUp() {
        obj = new TestObj();
    }

    @AfterEach
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

        public TestObj() {
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
        assertEquals(
                obj,
                runGet(() -> w.getSwingViewContext().getSource().getValue()), "source value");
        frame = w;
    }
}
