package org.autogui.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiPreferences;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.Map;

public class GuiSwingRootPaneTest extends GuiSwingTestCase {
    JFrame frame;

    TestObj obj;
    TestObjPrivate objPrivate;

    public GuiSwingRootPaneTest() {}

    @Before
    public void setUp() {
        obj = new TestObj();
        objPrivate = new TestObjPrivate();
    }

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
        }
    }


    @GuiIncluded
    public static class TestObj {
        @GuiIncluded public String prop;
        @GuiIncluded public void action() {
            System.err.println("hello");
        }

        public TestObj() {}
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
        rootPane.getContext().updateSourceFromRoot();
        runWait();

        Assert.assertEquals("root source",
                obj,
                runGet(() -> rootPane.getContext().getSource().getValue()));

        Assert.assertEquals("prop is child of rootPane after update",
                "hello",
                runGet(() -> rootPane.getChildByName("prop").getSwingViewValue()));
    }

    @Test
    public void testCreateWithoutKeyBindings() {
        GuiSwingRootPane rootPane = runGet(() -> GuiSwingRootPane.creator().withKeyBindingWithoutAutomaticBindings()
                .create(obj));
        frame = runGet(() -> createFrame(rootPane));

        Assert.assertNull("no key binding",
                runGet(() -> rootPane.getActionByName("action").getValue(Action.ACCELERATOR_KEY))); //it has recommended key meta-a
        Assert.assertTrue("no key bindings",
                rootPane.getKeyBinding().getAssignedFromDispatcher().isEmpty());
    }

    @Test
    public void testCreateRelaxed() {
        GuiSwingRootPane rootPane = runGet(() -> GuiSwingRootPane.createForObjectRelaxed(objPrivate));
        frame = runGet(() -> createFrame(rootPane));

        objPrivate.prop = "hello";
        rootPane.getContext().updateSourceFromRoot();
        runWait();

        Assert.assertEquals("root source",
                objPrivate,
                runGet(() -> rootPane.getContext().getSource().getValue()));

        Assert.assertEquals("prop is child of rootPane after update",
                "hello",
                runGet(() -> rootPane.getChildByName("prop").getSwingViewValue()));
    }

    @Test
    public void testCreateWithoutLog() {
        GuiSwingRootPane rootPane = runGet(() -> GuiSwingRootPane.creator().withLogStatusDisabled()
                .create(obj));
        frame = runGet(() -> createFrame(rootPane));
        System.err.println("no logging");
        Assert.assertNull("no log window", runGet(rootPane::getLogWindow));
        Assert.assertNull("no log updater", runGet(rootPane::getLogPreferencesUpdater));
    }

    @Test
    public void testCreateWithPreferencesOnMemory() {
        GuiSwingRootPane rootPane = runGet(() -> GuiSwingRootPane.creator().withPreferencesOnMemory()
                .create(obj));
        frame = runGet(() -> createFrame(rootPane));

        Assert.assertTrue("store on memory",
                runGet(() -> rootPane.getContext().getPreferences().getValueStore() instanceof GuiPreferences.GuiValueStoreOnMemory));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCleanUp() {
        GuiSwingRootPane rootPane = runGet(() -> GuiSwingRootPane.creator().create(obj));
        frame = runGet(() -> createFrame(rootPane));
        obj.prop = "hello!!!";
        run(() -> rootPane.getContext().updateSourceFromRoot());

        GuiPreferences prefs = runGet(() -> new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), rootPane.getContext()));
        run(() -> rootPane.savePreferences(prefs));
        Map<String,Object> map = prefs.toJson();
        Assert.assertTrue("save $preferencesWindow", map.containsKey("$preferencesWindow"));
        Assert.assertTrue("save $logWindow", map.containsKey("$logWindow"));
        Assert.assertTrue("save $fileDialog", map.containsKey("$fileDialog"));
        Assert.assertTrue("save prop", map.containsKey("prop"));
        Map<String,Object> objMap = (Map<String,Object>) map.get("prop");
        Assert.assertEquals("String prop JSON source",
                "\"hello!!!\"", objMap.get("$value"));

        run(rootPane::cleanUp);
    }
}
