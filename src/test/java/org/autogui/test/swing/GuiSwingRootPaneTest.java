package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.swing.GuiSwingRootPane;
import org.autogui.swing.GuiSwingView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GuiSwingRootPaneTest extends GuiSwingTestCase {
    JFrame frame;

    TestObj obj;
    TestObjPrivate objPrivate;

    public GuiSwingRootPaneTest() {
    }

    @BeforeEach
    public void setUp() {
        obj = new TestObj();
        objPrivate = new TestObjPrivate();
    }

    @AfterEach
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
        }
    }


    @GuiIncluded
    public static class TestObj {
        @GuiIncluded
        public String prop;

        @GuiIncluded
        public void action() {
            System.err.println("hello");
        }

        public TestObj() {
        }
    }

    static class TestObjPrivate {
        String prop;

        void action() {
            System.err.println("hello");
        }
    }

    @Test
    public void testCreate() {
        GuiSwingRootPane rootPane = runGet(() -> GuiSwingRootPane.createForObject(obj));
        frame = runGet(() -> createFrame(rootPane));

        obj.prop = "hello";
        rootPane.getSwingViewContext().updateSourceFromRoot();
        runWait();

        assertEquals(obj, runGet(() -> rootPane.getSwingViewContext().getSource().getValue()), "root source");

        assertEquals("hello", runGet(() -> GuiSwingView.getDescendantByName(rootPane, "prop")).getSwingViewValue(), "prop is child of rootPane after update");
    }

    @Test
    public void testCreateWithoutKeyBindings() {
        GuiSwingRootPane rootPane = runGet(() -> GuiSwingRootPane.creator().withKeyBindingWithoutAutomaticBindings().create(obj));
        frame = runGet(() -> createFrame(rootPane));

        assertNull(runGet(() -> GuiSwingView.getActionByName(rootPane, "action").getValue(Action.ACCELERATOR_KEY)), "no key binding"); //it has recommended key meta-a
        assertTrue(rootPane.getKeyBinding().getAssignedFromDispatcher().isEmpty(), "no key bindings");
    }

    @Test
    public void testCreateRelaxed() {
        GuiSwingRootPane rootPane = runGet(() -> GuiSwingRootPane.createForObjectRelaxed(objPrivate));
        frame = runGet(() -> createFrame(rootPane));

        objPrivate.prop = "hello";
        rootPane.getSwingViewContext().updateSourceFromRoot();
        runWait();

        assertEquals(objPrivate, runGet(() -> rootPane.getSwingViewContext().getSource().getValue()), "root source");

        assertEquals("hello", runGet(() -> GuiSwingView.getDescendantByName(rootPane, "prop").getSwingViewValue()), "prop is child of rootPane after update");
    }

    @Test
    public void testCreateWithoutLog() {
        GuiSwingRootPane rootPane = runGet(() -> GuiSwingRootPane.creator().withLogStatusDisabled().create(obj));
        frame = runGet(() -> createFrame(rootPane));
        System.err.println("no logging");
        assertNull(runGet(rootPane::getLogWindow), "no log window");
        assertNull(runGet(rootPane::getLogPreferencesUpdater), "no log updater");
    }

    @Test
    public void testCreateWithPreferencesOnMemory() {
        GuiSwingRootPane rootPane = runGet(() -> GuiSwingRootPane.creator().withPreferencesOnMemory().create(obj));
        frame = runGet(() -> createFrame(rootPane));

        assertTrue(runGet(() -> rootPane.getSwingViewContext().getPreferences().getValueStore() instanceof GuiPreferences.GuiValueStoreOnMemory), "store on memory");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCleanUp() {
        GuiSwingRootPane rootPane = runGet(() -> GuiSwingRootPane.creator().create(obj));
        frame = runGet(() -> createFrame(rootPane));
        obj.prop = "hello!!!";
        run(() -> rootPane.getSwingViewContext().updateSourceFromRoot());

        GuiPreferences prefs = runGet(() -> new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), rootPane.getSwingViewContext()));
        run(() -> rootPane.savePreferences(prefs));
        Map<String, Object> map = prefs.toJson();
        assertTrue(map.containsKey("$preferencesWindow"), "save $preferencesWindow");
        assertTrue(map.containsKey("$logWindow"), "save $logWindow");
        assertTrue(map.containsKey("$fileDialog"), "save $fileDialog");
        assertTrue(map.containsKey("prop"), "save prop");
        Map<String, Object> objMap = (Map<String, Object>) map.get("prop");
        assertEquals("\"hello!!!\"", objMap.get("$value"), "String prop JSON source");

        run(rootPane::cleanUp);
    }
}
