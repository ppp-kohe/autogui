package org.autogui.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.JsonReader;
import org.autogui.base.JsonWriter;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.HashMap;

public class GuiSwingViewTabbedPaneTest extends GuiSwingTestCase {
    GuiTypeBuilder builder;
    GuiTypeObject typeObject;
    GuiMappingContext context;
    TestObj obj;

    GuiMappingContext contextTab1;
    GuiMappingContext contextTab2;

    GuiSwingViewTabbedPane tabbedPane;

    JFrame frame;

    public GuiSwingViewTabbedPaneTest() {}

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);
        obj = new TestObj();
        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        tabbedPane = new GuiSwingViewTabbedPane(GuiSwingMapperSet.getDefaultMapperSet());

        contextTab1 = context.getChildByName("tab1");
        contextTab2 = context.getChildByName("tab2");
    }

    @After
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded public TabObj1 tab1 = new TabObj1();
        @GuiIncluded public TabObj2 tab2 = new TabObj2();

        public TestObj() {}
        @GuiIncluded
        public void action() {
        }
    }

    @GuiIncluded
    public static class TabObj1 {
        @GuiIncluded public String prop;
        public TabObj1() {}
    }

    @GuiIncluded
    public static class TabObj2 {
        @GuiIncluded public int prop;
        public TabObj2() {}
    }

    public GuiSwingViewTabbedPane.ObjectTabbedPane create() {
        JComponent comp = tabbedPane.createView(context, GuiReprValue.getNoneSupplier());
        frame = createFrame(comp);
        return GuiSwingView.findChildByType(comp, GuiSwingViewTabbedPane.ObjectTabbedPane.class);
    }

    /////////////////////////

    @Test
    public void testTabPropUpdate() {
        GuiSwingViewTabbedPane.ObjectTabbedPane pane = runGet(this::create);
        GuiSwingViewObjectPane.ObjectPane obj1 = runGet(() -> GuiSwingView.findChildByType(
                pane.getTabbedPane().getComponentAt(0), GuiSwingViewObjectPane.ObjectPane.class));

        obj.tab1.prop = "hello";
        context.updateSourceFromRoot();

        GuiSwingView.ValuePane<?> propPane = obj1.getChildByName("prop");
        Assert.assertEquals("after update prop in tab",
                "hello",
                runGet(propPane::getSwingViewValue));
    }

    @Test
    public void testTabPrefsLoad() {
        GuiPreferences parentPrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        HashMap<String,Object> values = new HashMap<>();
        values.put("selectedIndex", 1);
        parentPrefs.getValueStore().putString("$tab", JsonWriter.create().write(values).toSource());

        GuiSwingViewTabbedPane.ObjectTabbedPane pane = runGet(this::create);
        run(() -> pane.loadSwingPreferences(parentPrefs));

        Assert.assertEquals("selected tab from loaded prefs",
                1, (long) runGet(() -> pane.getTabbedPane().getSelectedIndex()));
    }

    @Test
    public void testTabPrefsSave() {
        GuiPreferences parentPrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        context.setPreferences(parentPrefs);
        GuiSwingViewTabbedPane.ObjectTabbedPane pane = runGet(this::create);
        pane.setPreferencesUpdater(GuiSwingPreferences.PreferencesUpdateEvent::save);

        run(() -> pane.getTabbedPane().setSelectedIndex(1));

        Assert.assertEquals("saved selected tab by event",
                1,
                JsonReader.create(parentPrefs.getValueStore().getString("$tab", "{}")).parseObject().get("selectedIndex"));

        GuiPreferences savePrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        run(() -> pane.saveSwingPreferences(savePrefs));

        Assert.assertEquals("saved selected tab by savePrefs",
                1,
                JsonReader.create(savePrefs.getValueStore().getString("$tab", "{}")).parseObject().get("selectedIndex"));
    }
}
