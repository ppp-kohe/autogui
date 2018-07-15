package autogui.test.swing;

import autogui.GuiIncluded;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprValue;
import autogui.base.type.GuiTypeBuilder;
import autogui.base.type.GuiTypeObject;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingPreferences;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewDocumentEditor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;

public class GuiSwingViewDocumentEditorTest extends GuiSwingTestCase {
    public static void main(String[] args) {
        GuiSwingViewDocumentEditorTest t = new GuiSwingViewDocumentEditorTest();
        t.setUp();
        t.testViewDocLoadSettings();
    }

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;
    TestObj obj;

    GuiSwingViewDocumentEditor doc;
    JFrame frame;

    @Before
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextProp = context.getChildByName("value");

        doc = new GuiSwingViewDocumentEditor();
    }

    @After
    public void tearDown() {
        frame.dispose();
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded
        public StringBuilder value = new StringBuilder();
    }

    //////////////

    public GuiSwingViewDocumentEditor.PropertyDocumentTextPane create() {
        JComponent c = doc.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findChildByType(c, GuiSwingViewDocumentEditor.PropertyDocumentTextPane.class);
    }

    @Test
    public void testViewDoc() {
        GuiSwingViewDocumentEditor.PropertyDocumentTextPane propText = runGet(this::create);

        run(this::runWait);
        obj.value = new StringBuilder("hello, world");
        contextProp.updateSourceFromRoot();

        Assert.assertEquals("listener update",
                "hello, world",
                runGet(propText::getText));
    }

    @Test
    public void testViewDocSetText() {
        GuiSwingViewDocumentEditor.PropertyDocumentTextPane propText = runGet(this::create);
        contextProp.updateSourceFromRoot();
        run(this::runWait);

        run(() -> propText.setText("hello, world"));
        run(this::runWait);

        Assert.assertEquals("listener update",
                "hello, world",
                obj.value.toString());
    }

    @Test
    public void testViewDocSet() {
        GuiSwingViewDocumentEditor.PropertyDocumentTextPane propText = runGet(this::create);
        contextProp.updateSourceFromRoot();
        run(() -> propText.setSwingViewValueWithUpdate(new StringBuilder("hello, world")));
        run(this::runWait);

        Assert.assertEquals("view update",
                "hello, world",
                obj.value.toString());
        Assert.assertEquals("view update: text",
                "hello, world",
                runGet(propText::getText));
    }

    @Test
    public void testViewDocLoadSettings() {
        GuiPreferences parentPrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        GuiPreferences prefs = parentPrefs.getChild(contextProp);
        prefs.getValueStore().putString("lineSpacing", "1.5");
        prefs.getValueStore().putString("fontFamily", "Serif");
        prefs.getValueStore().putString("fontSize", "24");
        prefs.getValueStore().putString("bold", "true");
        prefs.getValueStore().putString("italic", "true");
        prefs.getValueStore().putString("backgroundColor", "[0,11,12,250]");
        prefs.getValueStore().putString("foregroundColor", "[255,244,233,240]");
        prefs.getValueStore().putString("wrapText", "true");

        obj.value.append("hello, \nworld\n--- === ___ +++ --- === ___ +++ --- === ___ +++ --- === ___ +++\n");

        GuiSwingViewDocumentEditor.PropertyDocumentTextPane i = runGet(this::create);
        contextProp.updateSourceFromRoot();
        run(() -> i.loadSwingPreferences(prefs));

        run(() -> frame.setSize(400, 300));

        run(() -> {
            try {
                i.getDocument().insertString(i.getDocument().getLength() - 1, "\nnew-line", null);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        JScrollPane scroll = runGet(() -> GuiSwingViewDocumentEditor.scroll(i.getParent()));

        Assert.assertEquals("after load prefs: lineSpacing", 1.5,
                runGet(() -> i.getSettingPane().getLineSpacing().getValue()));
        Assert.assertEquals("after load prefs: fontFamily", "Serif",
                runGet(() -> i.getSettingPane().getFontFamily().getSelectedItem()));
        Assert.assertEquals("after load prefs: fontSize", 24,
                runGet(() -> i.getSettingPane().getFontSize().getValue()));
        Assert.assertEquals("after load prefs: bold", true,
                runGet(() -> i.getSettingPane().getStyleBold().getValue(Action.SELECTED_KEY)));
        Assert.assertEquals("after load prefs: italic", true,
                runGet(() -> i.getSettingPane().getStyleItalic().getValue(Action.SELECTED_KEY)));
        Assert.assertEquals("after load prefs: backgroundColor", new Color(0, 11, 12,250),
                runGet(() -> i.getSettingPane().getBackgroundColor().getColor()));
        Assert.assertEquals("after load prefs: foregroundColor", new Color(255, 244, 233,240),
                runGet(() -> i.getSettingPane().getForegroundColor().getColor()));
        Assert.assertEquals("after load prefs: wrapText", true,
                runGet(() -> i.getSettingPane().getWrapText().getValue(Action.SELECTED_KEY)));

        AttributeSet style = runGet(() -> i.getStyledDocument().getStyle(StyleContext.DEFAULT_STYLE));
        Assert.assertEquals("after load prefs: doc lineSpacing", 1.5f,
                StyleConstants.getLineSpacing(style), 0.1);
        Assert.assertEquals("after load prefs: doc fontFamily", "Serif",
                StyleConstants.getFontFamily(style));
        Assert.assertEquals("after load prefs: fontSize", 24,
                StyleConstants.getFontSize(style));
        Assert.assertTrue("after load prefs: bold", StyleConstants.isBold(style));
        Assert.assertTrue("after load prefs: italic", StyleConstants.isItalic(style));
        Assert.assertEquals("after load prefs: backgroundColor", new Color(0, 11, 12,250),
                runGet(i::getBackground)); //pane background instead of document background
        Assert.assertEquals("after load prefs: foregroundColor", new Color(255, 244, 233,240),
                StyleConstants.getForeground(style));
        Assert.assertEquals("after load prefs: wrapText scrollbar policy", (Integer) JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
                runGet(scroll::getHorizontalScrollBarPolicy));
        Assert.assertTrue("after load prefs: wrapText tracking viewport",
                runGet(i::getScrollableTracksViewportWidth));
    }

    @Test
    public void testViewDocSaveSettings() {

        GuiPreferences parentPrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        context.setPreferences(parentPrefs);

        obj.value.append("hello, \nworld\n--- === ___ +++ --- === ___ +++ --- === ___ +++ --- === ___ +++\n");
        GuiSwingViewDocumentEditor.PropertyDocumentTextPane i = runGet(this::create);
        contextProp.updateSourceFromRoot();

        run(() -> frame.setSize(400, 300));
        i.setPreferencesUpdater(GuiSwingPreferences.PreferencesUpdateEvent::save);

        run(() -> i.getSettingPane().getLineSpacing().setValue(1.5));
        run(() -> i.getSettingPane().getFontFamily().setSelectedItem("Serif"));
        run(() -> i.getSettingPane().getFontSize().setValue(24));
        run(() -> i.getSettingPane().getStyleBold().change(true));
        run(() -> i.getSettingPane().getStyleItalic().change(true));
        run(() -> i.getSettingPane().getWrapText().change( true));
        run(() -> i.getSettingPane().getBackgroundColor().setColor(new Color(0, 11, 12, 250)));
        run(() -> i.getSettingPane().getForegroundColor().setColor(new Color(255, 244, 233, 240)));

        run(this::runWait);

        JScrollPane scroll = runGet(() -> GuiSwingViewDocumentEditor.scroll(i.getParent()));

        AttributeSet style = runGet(() -> i.getStyledDocument().getStyle(StyleContext.DEFAULT_STYLE));
        Assert.assertEquals("after set settings: doc lineSpacing", 1.5f,
                StyleConstants.getLineSpacing(style), 0.1);
        Assert.assertEquals("after set settings: doc fontFamily", "Serif",
                StyleConstants.getFontFamily(style));
        Assert.assertEquals("after set settings: fontSize", 24,
                StyleConstants.getFontSize(style));
        Assert.assertTrue("after set settings: bold", StyleConstants.isBold(style));
        Assert.assertTrue("after set settings: italic", StyleConstants.isItalic(style));
        Assert.assertEquals("after set settings: backgroundColor", new Color(0, 11, 12,250),
                runGet(i::getBackground)); //pane background instead of document background
        Assert.assertEquals("after set settings: foregroundColor", new Color(255, 244, 233,240),
                StyleConstants.getForeground(style));
        Assert.assertEquals("after set settings: wrapText scrollbar policy", (Integer) JScrollPane.HORIZONTAL_SCROLLBAR_NEVER,
                runGet(scroll::getHorizontalScrollBarPolicy));
        Assert.assertTrue("after set settings: wrapText tracking viewport",
                runGet(i::getScrollableTracksViewportWidth));

        GuiPreferences prefs = parentPrefs.getChild(contextProp);
        Assert.assertEquals("after save prefs: ", "1.5",
                prefs.getValueStore().getString("lineSpacing", ""));
        Assert.assertEquals("after save prefs: ", "Serif",
                prefs.getValueStore().getString("fontFamily", ""));
        Assert.assertEquals("after save prefs: ", "24",
                prefs.getValueStore().getString("fontSize", ""));
        Assert.assertEquals("after save prefs: ", "true",
                prefs.getValueStore().getString("bold", ""));
        Assert.assertEquals("after save prefs: ", "true",
                prefs.getValueStore().getString("italic", ""));
        Assert.assertEquals("after save prefs: ", "[0,11,12,250]",
                prefs.getValueStore().getString("backgroundColor", ""));
        Assert.assertEquals("after save prefs: ", "[255,244,233,240]",
                prefs.getValueStore().getString("foregroundColor", ""));
        Assert.assertEquals("after save prefs: ", "true",
                prefs.getValueStore().getString("wrapText", ""));


        GuiPreferences savePrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        run(() -> i.saveSwingPreferences(savePrefs));

        GuiPreferences savedTarget = savePrefs.getDescendant(contextProp);
        Assert.assertEquals("after save prefs: ", "1.5",
                savedTarget.getValueStore().getString("lineSpacing", ""));
        Assert.assertEquals("after save prefs: ", "Serif",
                savedTarget.getValueStore().getString("fontFamily", ""));
        Assert.assertEquals("after save prefs: ", "24",
                savedTarget.getValueStore().getString("fontSize", ""));
        Assert.assertEquals("after save prefs: ", "true",
                savedTarget.getValueStore().getString("bold", ""));
        Assert.assertEquals("after save prefs: ", "true",
                savedTarget.getValueStore().getString("italic", ""));
        Assert.assertEquals("after save prefs: ", "[0,11,12,250]",
                savedTarget.getValueStore().getString("backgroundColor", ""));
        Assert.assertEquals("after save prefs: ", "[255,244,233,240]",
                savedTarget.getValueStore().getString("foregroundColor", ""));
        Assert.assertEquals("after save prefs: ", "true",
                savedTarget.getValueStore().getString("wrapText", ""));

    }
}
