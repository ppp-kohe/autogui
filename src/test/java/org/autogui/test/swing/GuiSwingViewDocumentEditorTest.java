package org.autogui.test.swing;

import org.autogui.GuiIncluded;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.type.GuiTypeBuilder;
import org.autogui.base.type.GuiTypeObject;
import org.autogui.swing.GuiSwingMapperSet;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingViewDocumentEditor;
import org.autogui.swing.prefs.GuiSwingPrefsSupports;
import org.autogui.swing.util.TextPaneBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuiSwingViewDocumentEditorTest extends GuiSwingTestCase {

    GuiTypeBuilder builder;
    GuiTypeObject typeObject;

    GuiMappingContext context;
    GuiMappingContext contextProp;
    TestObj obj;

    GuiSwingViewDocumentEditor doc;
    JFrame frame;

    public GuiSwingViewDocumentEditorTest() {
    }

    @BeforeEach
    public void setUp() {
        builder = new GuiTypeBuilder();
        typeObject = (GuiTypeObject) builder.get(TestObj.class);

        obj = new TestObj();

        context = new GuiMappingContext(typeObject, obj);
        GuiSwingMapperSet.getReprDefaultSet().match(context);

        contextProp = context.getChildByName("value");

        doc = new GuiSwingViewDocumentEditor();
    }

    @AfterEach
    public void tearDown() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    @GuiIncluded
    public static class TestObj {
        @GuiIncluded
        public StringBuilder value = new StringBuilder();

        public TestObj() {
        }
    }

    //------------------

    public GuiSwingViewDocumentEditor.PropertyDocumentTextPane create() {
        JComponent c = doc.createView(contextProp, GuiReprValue.getNoneSupplier());
        frame = createFrame(c);
        return GuiSwingView.findDescendantByType(c, GuiSwingViewDocumentEditor.PropertyDocumentTextPane.class);
    }

    @Test
    public void testViewDoc() {
        GuiSwingViewDocumentEditor.PropertyDocumentTextPane propText = runGet(this::create);

        run(this::runWait);
        obj.value = new StringBuilder("hello, world");
        contextProp.updateSourceFromRoot();

        assertEquals("hello, world", runGet(propText::getText), "listener update");
    }

    @Test
    public void testViewDocSetText() {
        GuiSwingViewDocumentEditor.PropertyDocumentTextPane propText = runGet(this::create);
        contextProp.updateSourceFromRoot();
        run(this::runWait);

        run(() -> propText.setText("hello, world"));
        run(this::runWait);

        assertEquals("hello, world", obj.value.toString(), "listener update");
    }

    @Test
    public void testViewDocSet() {
        GuiSwingViewDocumentEditor.PropertyDocumentTextPane propText = runGet(this::create);
        contextProp.updateSourceFromRoot();
        run(() -> propText.setSwingViewValueWithUpdate(new StringBuilder("hello, world")));
        run(this::runWait);

        assertEquals("hello, world", obj.value.toString(), "view update");
        assertEquals("hello, world", runGet(propText::getText), "view update: text");
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
        prefs.getValueStore().putString("backgroundCustom", "true");
        prefs.getValueStore().putString("foregroundCustom", "true");
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

        JScrollPane scroll = runGet(() -> TextPaneBuilder.scrollPane(i));

        assertEquals(1.5f, ((Number) runGet(() -> i.getSettingPane().getSpaceLine().getValue())).floatValue(), 0.1f, "after load prefs: lineSpacing");
        assertEquals("Serif", runGet(() -> i.getSettingPane().getFontFamily().getSelectedItem()), "after load prefs: fontFamily");
        assertEquals(24, runGet(() -> i.getSettingPane().getFontSize().getValue()), "after load prefs: fontSize");
        assertEquals(true, runGet(() -> i.getSettingPane().getStyleBold().isSelected()), "after load prefs: bold");
        assertEquals(true, runGet(() -> i.getSettingPane().getStyleItalic().isSelected()), "after load prefs: italic");
        assertEquals(new Color(0, 11, 12, 250), runGet(() -> i.getSettingPane().getBackgroundColor().getColor()), "after load prefs: backgroundColor");
        assertEquals(new Color(255, 244, 233, 240), runGet(() -> i.getSettingPane().getForegroundColor().getColor()), "after load prefs: foregroundColor");
        assertEquals(true, runGet(() -> i.getSettingPane().getStyleWrapLine().isSelected()), "after load prefs: backgroundCustom");
        assertEquals(true, runGet(() -> i.getSettingPane().getStyleWrapLine().isSelected()), "after load prefs: foregroundCustom");
        assertEquals(true, runGet(() -> i.getSettingPane().getStyleWrapLine().isSelected()), "after load prefs: wrapText");

        AttributeSet style = runGet(() -> i.getStyledDocument().getStyle(StyleContext.DEFAULT_STYLE));
        assertEquals(1.5f, StyleConstants.getLineSpacing(style), 0.1, "after load prefs: doc lineSpacing");
        assertEquals("Serif", StyleConstants.getFontFamily(style), "after load prefs: doc fontFamily");
        assertEquals(24, StyleConstants.getFontSize(style), "after load prefs: fontSize");
        assertTrue(StyleConstants.isBold(style), "after load prefs: bold");
        assertTrue(StyleConstants.isItalic(style), "after load prefs: italic");
        assertEquals(new Color(0, 11, 12, 250), runGet(i::getBackground), "after load prefs: backgroundColor"); //pane background instead of document background
        assertEquals(new Color(255, 244, 233, 240), StyleConstants.getForeground(style), "after load prefs: foregroundColor");
        assertEquals((Integer) JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, runGet(scroll::getHorizontalScrollBarPolicy), "after load prefs: wrapText scrollbar policy");
        assertTrue(runGet(i::getScrollableTracksViewportWidth), "after load prefs: wrapText tracking viewport");
    }

    @Test
    public void testViewDocSaveSettings() {

        GuiPreferences parentPrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        context.setPreferences(parentPrefs);

        obj.value.append("hello, \nworld\n--- === ___ +++ --- === ___ +++ --- === ___ +++ --- === ___ +++\n");
        GuiSwingViewDocumentEditor.PropertyDocumentTextPane i = runGet(this::create);
        contextProp.updateSourceFromRoot();

        run(() -> frame.setSize(400, 300));
        i.setPreferencesUpdater(GuiSwingPrefsSupports.PreferencesUpdateEvent::save);
        JScrollPane scroll = runGet(() -> TextPaneBuilder.scrollPane(i));

        run(() -> i.getStyledDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {

            }

            @Override
            public void removeUpdate(DocumentEvent e) {

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                System.err.println("update " + e);
                var s = ((StyledDocument) e.getDocument()).getStyle(StyleContext.DEFAULT_STYLE);

                System.err.println("  above=" + StyleConstants.getSpaceAbove(s) + " lineSp=" + StyleConstants.getLineSpacing(s));
            }
        }));
        runWait();

        run(() -> i.getSettingPane().getSpaceLine().setValue(1.5f));
        run(() -> i.getSettingPane().getFontFamily().setSelectedItem("Serif"));
        run(() -> i.getSettingPane().getFontSize().setValue(24));
        run(() -> i.getSettingPane().getStyleBold().setSelected(true));
        run(() -> i.getSettingPane().getStyleItalic().setSelected(true));
        run(() -> i.getSettingPane().getStyleWrapLine().setSelected(true));
        run(() -> i.getSettingPane().getBackgroundCustom().setSelected(true));
        run(() -> i.getSettingPane().getForegroundCustom().setSelected(true));
        run(() -> i.getSettingPane().getBackgroundColor().setColor(new Color(0, 11, 12, 250)));
        run(() -> i.getSettingPane().getForegroundColor().setColor(new Color(255, 244, 233, 240)));


        runWait();
        runWait();

        AttributeSet style = runGet(() -> i.getStyledDocument().getStyle(StyleContext.DEFAULT_STYLE));
        assertEquals(1.5f, StyleConstants.getLineSpacing(style), 0.1, "after set settings: doc lineSpacing");
        assertEquals("Serif", StyleConstants.getFontFamily(style), "after set settings: doc fontFamily");
        assertEquals(24, StyleConstants.getFontSize(style), "after set settings: fontSize");
        assertTrue(StyleConstants.isBold(style), "after set settings: bold");
        assertTrue(StyleConstants.isItalic(style), "after set settings: italic");
        assertEquals(new Color(0, 11, 12, 250), runGet(i::getBackground), "after set settings: backgroundColor"); //pane background instead of document background
        assertEquals(new Color(255, 244, 233, 240), StyleConstants.getForeground(style), "after set settings: foregroundColor");
        assertEquals((Integer) JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, runGet(scroll::getHorizontalScrollBarPolicy), "after set settings: wrapText scrollbar policy");
        assertTrue(runGet(i::getScrollableTracksViewportWidth), "after set settings: wrapText tracking viewport");

        GuiPreferences prefs = parentPrefs.getChild(contextProp);
        assertEquals("1.5", prefs.getValueStore().getString("lineSpacing", ""), "after save prefs: ");
        assertEquals("Serif", prefs.getValueStore().getString("fontFamily", ""), "after save prefs: ");
        assertEquals("24", prefs.getValueStore().getString("fontSize", ""), "after save prefs: ");
        assertEquals("true", prefs.getValueStore().getString("bold", ""), "after save prefs: ");
        assertEquals("true", prefs.getValueStore().getString("italic", ""), "after save prefs: ");
        assertEquals("[0,11,12,250]", prefs.getValueStore().getString("backgroundColor", ""), "after save prefs: ");
        assertEquals("[255,244,233,240]", prefs.getValueStore().getString("foregroundColor", ""), "after save prefs: ");
        assertEquals("true", prefs.getValueStore().getString("backgroundCustom", ""), "after save prefs: ");
        assertEquals("true", prefs.getValueStore().getString("foregroundCustom", ""), "after save prefs: ");
        assertEquals("true", prefs.getValueStore().getString("wrapText", ""), "after save prefs: ");


        GuiPreferences savePrefs = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
        run(() -> i.saveSwingPreferences(savePrefs));

        GuiPreferences savedTarget = savePrefs.getDescendant(contextProp);
        assertEquals("1.5", savedTarget.getValueStore().getString("lineSpacing", ""), "after save prefs: ");
        assertEquals("Serif", savedTarget.getValueStore().getString("fontFamily", ""), "after save prefs: ");
        assertEquals("24", savedTarget.getValueStore().getString("fontSize", ""), "after save prefs: ");
        assertEquals("true", savedTarget.getValueStore().getString("bold", ""), "after save prefs: ");
        assertEquals("true", savedTarget.getValueStore().getString("italic", ""), "after save prefs: ");
        assertEquals("[0,11,12,250]", savedTarget.getValueStore().getString("backgroundColor", ""), "after save prefs: ");
        assertEquals("[255,244,233,240]", savedTarget.getValueStore().getString("foregroundColor", ""), "after save prefs: ");
        assertEquals("true", savedTarget.getValueStore().getString("backgroundCustom", ""), "after save prefs: ");
        assertEquals("true", savedTarget.getValueStore().getString("foregroundCustom", ""), "after save prefs: ");
        assertEquals("true", savedTarget.getValueStore().getString("wrapText", ""), "after save prefs: ");

    }
}
