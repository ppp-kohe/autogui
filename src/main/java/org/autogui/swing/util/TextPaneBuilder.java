package org.autogui.swing.util;

import org.autogui.base.JsonReader;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * a builder for edtor-panes ({@link JEditorPane} and {@link JTextPane}).
 * <p>
 * To support wrapping-lines switching, define a custom sub-class of the text-pane class with implementing {@link WrapLineSupport}
 *  and overriding some methods.
 *  @see WrapLineSupport
 * @since 1.9
 */
public class TextPaneBuilder implements SettingsWindow.SettingsWindowClientBase {
    public static final double DOCUMENT_SETTING_PANE_SPACE_ABOBE_MIN = -1f;
    public static final double DOCUMENT_SETTING_PANE_SPACE_ABOBE_MAX = 1000f;
    public static final double DOCUMENT_SETTING_PANE_SPACE_LINE_MIN = -1f;
    public static final double DOCUMENT_SETTING_PANE_SPACE_LINE_MAX = 5f;
    public static final int DOCUMENT_SETTING_PANE_FONT_SIZE_MIN = -1;
    public static final int DOCUMENT_SETTING_PANE_FONT_SIZE_MAX = 400;
    protected JEditorPane pane;
    protected PopupExtension popup;

    protected PopupExtensionText.EditActionSet editActionSet;
    protected KeyUndoManager undoManager;
    protected LineNumberPane lineNumberPane;

    protected DocumentSettingPaneBase settingPane;
    protected DocumentSettingAction settingAction;
    protected SettingsWindow settingsWindow;

    protected DocumentCharSequence<Document> contents;

    public TextPaneBuilder(JEditorPane pane) {
        this.pane = pane;
    }

    public DocumentCharSequence<Document> getContents() {
        if (contents == null) {
            contents = new DocumentCharSequence<>(pane.getDocument());
        }
        return contents;
    }

    public static Dimension getPreferredSize(JTextComponent field, boolean wrapLine) {
        Dimension dim = field.getUI().getPreferredSize(field);
        Component parent = SwingUtilities.getUnwrappedParent(field);
        if (!wrapLine && parent != null) {
            dim = new Dimension(Math.max(dim.width, parent.getSize().width), dim.height);
        }
        return dim;
    }

    /**
     * @param c the text-component
     * @return the parent scroll-pane by calling {@link #scrollPaneContainer(Container)} with the parent
     * @since 1.7
     */
    public static JScrollPane scrollPane(JComponent c) {
        return c != null ? scrollPaneContainer(c.getParent()) : null;
    }

    /**
     * @param c the parent container of a text-comonent
     * @return an upper scroll-pane
     */
    public static JScrollPane scrollPaneContainer(Container c) {
        return switch (c) {
            case JViewport jViewport -> scrollPaneContainer(c.getParent());
            case JScrollPane jScrollPane -> jScrollPane;
            case null, default -> null;
        };
    }

    /**
     * @param pane a scroll-pane or null
     * @return true if null or horizontal-scroll-bar-policy not "always"; i.e. "never" or "as_needed"
     */
    public static boolean isWrapLine(JScrollPane pane) {
        if (pane == null) {
            return true;
        } else {
            return pane.getHorizontalScrollBarPolicy() != ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
        }
    }

    /**
     * @param pane a scroll-pane or null
     * @param tested the tested wrapping-line property
     * @return true if the horizonta-scroll-bar-policy is not the tested value; if true, it should update the wrapping-line property and revalidate the layout.
     */
    public static boolean isWrapLineNotMatch(JScrollPane pane, boolean tested) {
        if (pane == null) {
            return !tested;
        } else {
            var policy = pane.getHorizontalScrollBarPolicy();
            return policy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED ||
                    ((policy == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS) == tested);
        }
    }

    /**
     * @param pane the target pane
     * @param f the new property value of wrapping-line
     * @since 1.7
     */
    public static void setWrapLine(JScrollPane pane, boolean f) {
        pane.setHorizontalScrollBarPolicy(f ?
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER :
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        pane.revalidate();
    }

    public TextPaneBuilder init() {
        initSize();
        initBorder();
        initSettingPane();
        initEditActionSet();
        initPopup();
        initKeyBindings();
        initCaret();
        initUndo();
        initLineNumberPane();
        return this;
    }

    public void initSize() {
        pane.setMinimumSize(new Dimension(1, 1));
    }

    public void initBorder() {
        pane.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 0));
    }

    public void initSettingPane() {
        settingPane = createSettingPane();
        if (settingPane != null) {
            settingAction = new DocumentSettingAction(createSettingPaneLabel(), getSettingsWindowClient(), settingPane);
            pane.addPropertyChangeListener("document", e -> {
                if (settingPane != null) {
                    settingPane.updateGuiAndTargetTextPaneFromPrefsObj();
                }
            });
            pane.addPropertyChangeListener("UI", e -> {
                if (settingPane != null) {
                    settingPane.setDefaultColorFromPane();
                }
            });
        }
    }

    /**
     * @return a setting-pane or null; if null, the feature will be disabled
     */
    protected DocumentSettingPaneBase createSettingPane() {
        return new DocumentSettingPaneBase(pane);
    }

    protected SettingsWindow.SettingsWindowClientBase getSettingsWindowClient() {
        return this;
    }

    protected JComponent createSettingPaneLabel() {
        return new JLabel("Settings");
    }

    public void initEditActionSet() {
        editActionSet = new PopupExtensionText.EditActionSet(pane);
    }

    /**
     * to disable the feature, override the method and fill with empty
     */
    public void initPopup() {
        popup = new PopupExtensionText(pane, PopupExtension.getDefaultKeyMatcher(), createPopupMenuBuilder());
        pane.setInheritsPopupMenu(true);
    }

    protected PopupExtension.PopupMenuBuilder createPopupMenuBuilder() {
        return new PopupCategorized(() ->
                PopupCategorized.getMenuItems(
                    editActionSetAsList(),
                    settingActionAsList()));
    }

    private List<Action> editActionSetAsList() {
        return editActionSet != null ? editActionSet.toActions() : List.of();
    }

    private List<Action> settingActionAsList() {
        return settingAction != null ? List.of(settingAction) : List.of();
    }

    public void initKeyBindings() {
        PopupExtensionText.putInputEditActions(pane);
        PopupExtensionText.putUnregisteredEditActions(pane);
    }

    public void initCaret() {
        pane.setCaret(new SelectionHighlightCaret());
    }

    public void initUndo() {
        undoManager = new KeyUndoManager();
        undoManager.putListenersAndActionsTo(pane);
    }

    public void initLineNumberPane() {
        lineNumberPane = createLineNumberPane(pane);
    }

    @Override
    public void setSettingsWindow(SettingsWindow settingWindow) {
        this.settingsWindow = settingWindow;
    }

    @Override
    public SettingsWindow getSettingsWindow() {
        return settingsWindow == null ? SettingsWindow.get() : settingsWindow;
    }

    public JEditorPane getPane() {
        return pane;
    }

    public PopupExtension getPopup() {
        return popup;
    }

    public KeyUndoManager getUndoManager() {
        return undoManager;
    }

    public LineNumberPane getLineNumberPane() {
        return lineNumberPane;
    }

    public DocumentSettingPaneBase getSettingPane() {
        return settingPane;
    }

    public DocumentSettingAction getSettingAction() {
        return settingAction;
    }

    /**
     *
     * @param pane the {@link #pane} field
     * @return the line-numbering pane for the pane or null; if null, the feature will be disabled.
     */
    protected LineNumberPane createLineNumberPane(JEditorPane pane) {
        var lineNumberPane = new LineNumberPane(pane);
        lineNumberPane.install();
        return lineNumberPane;
    }

    /**
     * the interface for the target pane of {@link DocumentSettingPaneBase};
     *  if the pane implements the interface, it will be called at updating the wrapping-line property from the setting pane.
     *  <p>
     *   To support the feture of switching wrapping-line, the text-pane needs to implement the interface with overriding 2 methods
     *     {@link JEditorPane#getScrollableTracksViewportWidth()} and {@link JEditorPane#getPreferredSize()}, as like the following code.
     *  <pre>
     *      class MyPane extends JEditorPane implements WrapLineSupport {
     *          boolean wrapLine;
     *          public void setWrapLine(boolean w) {
     *              this.wrapLine = w;
     *          }
     *          public boolean getScrollableTracksViewportWidth() {
     *              return wrapLine;
     *          }
     *          public Dimension getPreferredSize() {
     *              return getPreferredSize(this, wrapLine);
     *          }
     *      }
     *  </pre>
     * @since 1.9
     */
    public interface WrapLineSupport {
        void setWrapLine(boolean wrapLine);
    }

    public static class SelectionHighlightCaret extends DefaultCaret {
        @Serial
        private static final long serialVersionUID = 1L;
        final TextPaneBuilder.SelectionHighlightPainter painter = new TextPaneBuilder.SelectionHighlightPainter();

        public SelectionHighlightCaret() {}

        @Override
        protected Highlighter.HighlightPainter getSelectionPainter() {
            return painter;
        }
    }

    public static class SelectionHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        public SelectionHighlightPainter() {
            super(UIManagerUtil.getInstance().getTextPaneSelectionBackground());
        }

        @Override
        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
            try {
                if (offs0 > offs1) {
                    int tmp = offs0;
                    offs0 = offs1;
                    offs1 = tmp;
                }

                Rectangle startRect = PopupExtensionText.textComponentModelToView(c, offs0);
                Rectangle endRect = PopupExtensionText.textComponentModelToView(c, offs1);

                int startLeft = startRect.x;
                int endRight = endRect.x + endRect.width;
                int startTop = startRect.y;
                int endBottom = endRect.y + endRect.height;

                Graphics2D g2 = (Graphics2D) g;

                Color color = (getColor() == null ? c.getSelectionColor() : getColor());
                g2.setColor(color);

                int startBottom = startRect.y + startRect.height;

                if (startBottom != endBottom) {
                    endRight = c.getWidth();
                }
                Rectangle2D.Double selRect = new Rectangle2D.Double(startLeft, startTop, endRight - startLeft, startBottom - startTop);
                adjustParagraphSpacing(c, view, selRect);
                g2.fill(selRect);
                return selRect;
            } catch (Exception ex) {
                ex.printStackTrace();
                return bounds;
            }
        }

        protected void adjustParagraphSpacing(JTextComponent c, View view, Rectangle2D.Double rect) {
            var attrs = view.getAttributes();
            var above = attrs.getAttribute(StyleConstants.SpaceAbove);
            var below = attrs.getAttribute(StyleConstants.SpaceBelow);
            if (above instanceof Number aboveNum) {
                double val = aboveNum.doubleValue() + 0.001;
                rect.y -= val;
                rect.height += val;
            }
            if (below instanceof Number belowNum) {
                double val = belowNum.doubleValue() + 0.001;
                rect.height += val;
            }
        }
    }

    /**
     * a setting pane for document default stype; a GUI for {@link PreferencesForDocumentSettingBase}
     * @since 1.9
     */
    public static class DocumentSettingPaneBase extends JPanel {
        @Serial private static final long serialVersionUID = 1L;
        protected JEditorPane pane;
        protected JComboBox<String> fontFamily;
        protected JToggleButton styleBold;
        protected JToggleButton styleItalic;
        protected JToggleButton styleWrapLine;
        protected JSpinner fontSize;
        protected JSpinner spaceAbove;
        protected JSpinner spaceLine;

        protected JCheckBox foregroundCustom;
        protected SettingsWindow.ColorButton foregroundColor;
        protected JCheckBox backgroundCustom;
        protected SettingsWindow.ColorButton backgroundColor;

        protected boolean updateDisabled;
        protected Consumer<PreferencesForDocumentSettingBase> preferencesUpdater;
        protected PreferencesForDocumentSettingBase prefsObj;

        @SuppressWarnings("this-escape")
        public DocumentSettingPaneBase(JEditorPane pane) {
            this.pane = pane;
            init();
        }

        public void init() {
            initBorder();
            initFontFamily();
            initStyle();
            initFontSize();
            initSpace();
            initColor();
            initLayout();
            initPrefsObj();
            initListener();
        }

        public void initBorder() {
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int top = ui.getScaledSizeInt(3);
            int size = ui.getScaledSizeInt(10);
            setBorder(BorderFactory.createEmptyBorder(top, size, size, size));
        }

        public void initFontFamily() {
            var env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            fontFamily = new JComboBox<>(env.getAvailableFontFamilyNames());
            fontFamily.setEditable(true);
        }

        public void initStyle() {
            UIManagerUtil ui = UIManagerUtil.getInstance();
            var styleSize = new Dimension(ui.getScaledSizeInt(46), ui.getScaledSizeInt(32));

            styleBold = new JToggleButton("<html><b>B</b></html>");
            styleBold.setPreferredSize(styleSize);
            styleBold.setToolTipText("Bold");

            styleItalic = new JToggleButton("<html><i>I</i></html>");
            styleItalic.setPreferredSize(styleSize);
            styleItalic.setToolTipText("Italic");

            styleWrapLine = new JToggleButton("⏎");
            styleWrapLine.setPreferredSize(styleSize);
            styleWrapLine.setToolTipText("Wrap Line");
        }

        public void initFontSize() {
            fontSize = new JSpinner(new SpinnerNumberModel(12, DOCUMENT_SETTING_PANE_FONT_SIZE_MIN, DOCUMENT_SETTING_PANE_FONT_SIZE_MAX, 1));
        }

        public void initSpace() {
            UIManagerUtil ui = UIManagerUtil.getInstance();
            var size = new Dimension(ui.getScaledSizeInt(72), ui.getScaledSizeInt(22));

            spaceLine = new JSpinner(new SpinnerNumberModel(0.5f, DOCUMENT_SETTING_PANE_SPACE_LINE_MIN, DOCUMENT_SETTING_PANE_SPACE_LINE_MAX, 0.05f));
            spaceLine.setToolTipText("Space factor between lines");
            spaceLine.setPreferredSize(size);

            spaceAbove = new JSpinner(new SpinnerNumberModel(0.0f, DOCUMENT_SETTING_PANE_SPACE_ABOBE_MIN, DOCUMENT_SETTING_PANE_SPACE_ABOBE_MAX, 1f));
            spaceAbove.setToolTipText("Space points above paragraphs");
            spaceAbove.setPreferredSize(size);
        }

        public void initColor() {
            var oldUpdateDisabled = updateDisabled;
            updateDisabled = true;
            backgroundCustom = new JCheckBox();
            backgroundCustom.setToolTipText("Enable the custom background color");
            backgroundColor = new SettingsWindow.ColorButton(Color.white, c -> updateTargetTextPaneAndPrefsObjFromGui());
            backgroundColor.setToolTipText("Background color");

            foregroundCustom = new JCheckBox();
            foregroundCustom.setToolTipText("Enable the custom foreground color");
            foregroundColor = new SettingsWindow.ColorButton(Color.black, c -> updateTargetTextPaneAndPrefsObjFromGui());
            foregroundColor.setToolTipText("Foreground color");
            updateDisabled = oldUpdateDisabled;
        }

        public void initLayout() {
            JPanel styleRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            {
                var styleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0 ));
                {
                    styleGroup.add(styleBold);
                    styleGroup.add(styleItalic);
                    styleGroup.add(styleWrapLine);
                }
                styleRow.add(styleGroup);
                styleRow.add(new JLabel("Size:"));
                styleRow.add(fontSize);
            }
            JPanel spaceRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            {
                spaceRow.add(new JLabel("Above:"));
                spaceRow.add(spaceAbove);
                spaceRow.add(new JLabel("Line:"));
                spaceRow.add(spaceLine);
            }
            JPanel colorRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            {
                JPanel backgroundPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                {
                    backgroundPane.add(new JLabel("Back:"));
                    backgroundPane.add(backgroundCustom);
                    backgroundPane.add(backgroundColor);
                }
                colorRow.add(backgroundPane);

                colorRow.add(Box.createHorizontalStrut(UIManagerUtil.getInstance().getScaledSizeInt(10)));

                JPanel foregroundPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                {
                    foregroundPane.add(new JLabel("Text:"));
                    foregroundPane.add(foregroundCustom);
                    foregroundPane.add(foregroundColor);
                }
                colorRow.add(foregroundPane);
            }
            new SettingsWindow.LabelGroup(this)
                    .addRow("Font:", fontFamily)
                    .addRow("Style:", styleRow)
                    .addRow("Space:", spaceRow)
                    .addRow("Color:", colorRow)
                    .fitWidth();
        }

        /**
         * initialize {@link PreferencesForDocumentSettingBase} with default (non-null) values;
         *  also if it has a target text-pane, calls {@link PreferencesForDocumentSettingBase#setup(JComponent)}
         *    for reading from the pane settings and set those values to the prefsObj.
         *   The GUI of the setting-pane is updated by the prefsObj
         */
        public void initPrefsObj() {
            prefsObj = createPfersObj();
            prefsObj.setUiDefault(); //set default (non-null) props
            if (pane != null) {
                updateWrapLineAvailability();
                prefsObj.setup(pane); //set props only set by the pane
            }
            setFrom(prefsObj); //update settings UI
        }

        protected PreferencesForDocumentSettingBase createPfersObj() {
            return new PreferencesForDocumentSettingBase(pane);
        }

        public void setDefaultColorFromPane() {
            if (prefsObj != null) {
                prefsObj.setDefaultColor(pane);
                prefsObj.applyTo(pane);
            }
        }

        /**
         * setting-up listeners of GUI settings ; the style will be immediately reflected by an UI action.
         */
        public void initListener() {
            updateDisabled = true;
            fontFamily.addItemListener(this::selectFontFamily);
            styleBold.addChangeListener(this::styleChanged);
            styleItalic.addChangeListener(this::styleChanged);
            styleWrapLine.addChangeListener(this::styleChanged);
            fontSize.addChangeListener(this::styleChanged);
            spaceAbove.addChangeListener(this::styleChanged);
            spaceLine.addChangeListener(this::styleChanged);
            foregroundCustom.addChangeListener(this::styleChanged);
            backgroundCustom.addChangeListener(this::styleChanged);
            updateDisabled = false;
            initTimerForAfterRendering();
        }

        /**
         * called from {@link #initListener()} and starts an one-shot UI timer for {@link #updateGuiAndPrefsObjFromTargetTextPane()};
         *  intended to update the setting props from the target-pane after displaying it.
         */
        public void initTimerForAfterRendering() {
            Timer timer = new Timer(200, e -> updateGuiAndPrefsObjFromTargetTextPane()); //sync props after component revealed: the pane might change props
            timer.setRepeats(false);
            timer.start();
        }

        public void selectFontFamily(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateTargetTextPaneAndPrefsObjFromGui();
            }
        }

        public void styleChanged(ChangeEvent e) {
            updateTargetTextPaneAndPrefsObjFromGui();
        }

        /** update style from GUI; calls {@link #updateTargetTextPaneAndPrefsObjFromGuiBody()} */
        public void updateTargetTextPaneAndPrefsObjFromGui() {
            if (!updateDisabled) {
                updateTargetTextPaneAndPrefsObjFromGuiBody();
            }
        }

        public void updateTargetTextPaneAndPrefsObjFromGuiBody() {
            setTo(prefsObj);
            sendPreferences();
            if (pane != null) {
                updateWrapLineAvailability();
                prefsObj.applyTo(pane);
                pane.repaint();
            }
        }

        protected void updateWrapLineAvailability() {
            styleWrapLine.setEnabled(pane instanceof WrapLineSupport);
        }

        /**
         * update the prefsObj by current GUI-settings props
         * @param prefsObj the updated prefsObj
         */
        public void setTo(PreferencesForDocumentSettingBase prefsObj) {
            prefsObj.setLineSpacing(((Number) spaceLine.getValue()).floatValue());
            prefsObj.setSpaceAbove(((Number) spaceAbove.getValue()).floatValue());
            prefsObj.setFontFamily((String) fontFamily.getSelectedItem());
            prefsObj.setFontSize(((Number) fontSize.getValue()).intValue());
            prefsObj.setBold(styleBold.isSelected());
            prefsObj.setItalic(styleItalic.isSelected());
            prefsObj.setBackgroundColor(backgroundColor.getColor());
            prefsObj.setForegroundColor(foregroundColor.getColor());
            prefsObj.setBackgroundCustom(backgroundCustom.isSelected());
            prefsObj.setForegroundCustom(foregroundCustom.isSelected());
            prefsObj.setWrapText(styleWrapLine.isSelected());
        }

        /**
         * update the GUI-settings props by reading the prefsObj
         * @param prefsObj the source prefsObj
         */
        public void setFrom(PreferencesForDocumentSettingBase prefsObj) {
            try {
                var lineSpacingVal = prefsObj.getLineSpacing();
                var spaceAboveVal = prefsObj.getSpaceAbove();
                var fontFamilyVal = prefsObj.getFontFamily();
                var fontSizeVal = prefsObj.getFontSize();
                var boldVal = prefsObj.isBold();
                var italicVal = prefsObj.isItalic();
                var wrapTextVal = prefsObj.isWrapText();
                var backgroundCustomVal = prefsObj.isBackgroundCustom();
                var foregroundCustomVal = prefsObj.isForegroundCustom();
                var backgroundColorVal = prefsObj.getBackgroundColor();
                var foregroundColorVal = prefsObj.getForegroundColor();
                spaceLine.setValue(Math.max(DOCUMENT_SETTING_PANE_SPACE_LINE_MIN, Math.min(DOCUMENT_SETTING_PANE_SPACE_LINE_MAX, lineSpacingVal)));
                spaceAbove.setValue(Math.max(DOCUMENT_SETTING_PANE_SPACE_ABOBE_MIN, Math.min(DOCUMENT_SETTING_PANE_SPACE_ABOBE_MAX, spaceAboveVal)));
                fontFamily.setSelectedItem(fontFamilyVal);
                fontSize.setValue(Math.max(DOCUMENT_SETTING_PANE_FONT_SIZE_MIN, Math.min(DOCUMENT_SETTING_PANE_FONT_SIZE_MAX, fontSizeVal)));
                styleBold.setSelected(boldVal);
                styleItalic.setSelected(italicVal);
                styleWrapLine.setSelected(wrapTextVal);
                backgroundCustom.setSelected(backgroundCustomVal);
                foregroundCustom.setSelected(foregroundCustomVal);
                if (backgroundColorVal != null) {
                    backgroundColor.setColor(backgroundColorVal);
                }
                if (foregroundColorVal != null) {
                    foregroundColor.setColor(foregroundColorVal);
                }
            } catch (Exception ex) {
                System.err.println("" + ex);
            }
        }

        public void setPreferencesUpdater(Consumer<PreferencesForDocumentSettingBase> updater) {
            this.preferencesUpdater = updater;
        }

        public void sendPreferences() {
            if (preferencesUpdater != null) {
                preferencesUpdater.accept(prefsObj);
            }
        }

        public void updateGuiAndPrefsObjFromTargetTextPane() {
            if (updateDisabled) {
                return;
            }
            try {
                updateDisabled = true;
                if (pane != null) {
                    prefsObj.setFrom(pane);
                }
                setFrom(prefsObj);
            } finally {
                updateDisabled = false;
            }
        }

        public void updateGuiAndTargetTextPaneFromPrefsObj() {
            if (updateDisabled) {
                return;
            }
            try {
                updateDisabled = true;
                if (pane != null) {
                    prefsObj.applyTo(pane);
                }
                setFrom(prefsObj);
            } finally {
                updateDisabled = false;
            }
        }


        public void shutDown() {
            backgroundColor.dispose();
            foregroundColor.dispose();
        }

        public JComboBox<String> getFontFamily() {
            return fontFamily;
        }

        public JToggleButton getStyleBold() {
            return styleBold;
        }

        public JToggleButton getStyleItalic() {
            return styleItalic;
        }

        public JToggleButton getStyleWrapLine() {
            return styleWrapLine;
        }

        public JSpinner getFontSize() {
            return fontSize;
        }

        public JSpinner getSpaceAbove() {
            return spaceAbove;
        }

        public JSpinner getSpaceLine() {
            return spaceLine;
        }

        /**
         * @return the backgroundCustom checkbox
         * @since 1.2
         */
        public JCheckBox getForegroundCustom() {
            return foregroundCustom;
        }

        public SettingsWindow.ColorButton getForegroundColor() {
            return foregroundColor;
        }

        /**
         * @return the foregroundCustom checkbox
         * @since 1.2
         */
        public JCheckBox getBackgroundCustom() {
            return backgroundCustom;
        }

        public SettingsWindow.ColorButton getBackgroundColor() {
            return backgroundColor;
        }

        public JEditorPane getPane() {
            return pane;
        }

        public PreferencesForDocumentSettingBase getPrefsObj() {
            return prefsObj;
        }
    }

    /**
     * prefs obj for document-settings; currently the settings have properties of entire document styling only for editing plain-texts;
     *  minimize properties.
     *  Currently supports the following properties;
     *   <ul>
     *       <li>float {@code lineSpacing}: the factor of spaces between lines. {@link StyleConstants#LineSpacing}</li>
     *       <li>float {@code spaceAbove}: points of spaces before the paragraph. {@link StyleConstants#SpaceAbove}</li>
     *       <li>String {@code fontFamily}: the name of the font. {@link StyleConstants#FontFamily}</li>
     *       <li>int {@code fontSize}: points of the font. {@link StyleConstants#FontSize}</li>
     *       <li>boolean {@code bold}: font-style bold. {@link StyleConstants#Bold}</li>
     *       <li>boolean {@code italic}: font-style italic. {@link StyleConstants#Italic}</li>
     *       <li>boolean {@code backgroundCustom}: the flag for enabling {@code backgroundColor}</li>
     *       <li>boolean {@code foregroundCustom}: the flag for enabling {@code foregroundColor}</li>
     *       <li>Color {@code backgroundColor}: the background color. available only if {@code backgroundCustom}=true. {@link StyleConstants#Background}</li>
     *       <li>Color {@code foreground}: the text color. available only if {@code foregrooundCustom}=true. {@link StyleConstants#Foreground}</li>
     *       <li>boolean {@code wrapText}: the flag for wrapping-line; can be implemented by horizontal-scroll-bar-policy and preferred-size</li>
     *   </ul>
     *
     *   In this definition, it does not actually implements preferences interface.
     * @since 1.9
     */
    public static class PreferencesForDocumentSettingBase {
        public static final String KEY_LINE_SPACING = "lineSpacing";
        public static final String KEY_SPACE_ABOVE = "spaceAbove";
        public static final String KEY_FONT_FAMILY = "fontFamily";
        public static final String KEY_FONT_SIZE = "fontSize";
        public static final String KEY_BOLD = "bold";
        public static final String KEY_ITALIC = "italic";
        public static final String KEY_BACKGROUND_COLOR = "backgroundColor";
        public static final String KEY_FOREGROUND_COLOR = "foregroundColor";
        public static final String KEY_BACKGROUND_CUSTOM = "backgroundCustom";
        public static final String KEY_FOREGROUND_CUSTOM = "foregroundCustom";
        public static final String KEY_WRAP_TEXT = "wrapText";

        protected float lineSpacing;
        protected float spaceAbove;
        protected String fontFamily;
        protected int fontSize;
        protected boolean bold;
        protected boolean italic;
        protected Color backgroundColor;
        protected Color foregroundColor;
        protected boolean backgroundCustom;
        protected boolean foregroundCustom;
        protected boolean wrapText;

        public static final String DEFAULT_FONT_FAMILY = "";
        public static final int DEFAULT_NUM = -1;

        protected Color defaultBackground;
        protected Color defaultForeground;

        protected boolean allowNullValues;

        /**
         * initialize by null-values and black and white colors.
         */
        public PreferencesForDocumentSettingBase() {
            this(null);
        }

        /**
         * initialize with the target pane (or if null,  it sets default null values).
         * it has 2 modes,
         *    1. the property mode with no pane which allows null values for representing saved prefs values,
         *    2. the practical mode with target pane which (basically) does not allow null-values, instead, it merges actual pane's value if null-values arrived.
         * @param pane the target text-pane or null
         */
        public PreferencesForDocumentSettingBase(JEditorPane pane) {
            lineSpacing = DEFAULT_NUM;
            spaceAbove = DEFAULT_NUM;
            fontFamily = DEFAULT_FONT_FAMILY;
            fontSize = DEFAULT_NUM;
            bold = false;
            italic = false;
            backgroundColor = Color.white;
            foregroundColor = Color.black;
            backgroundCustom = false;
            foregroundCustom = false;
            wrapText = true;
            this.defaultBackground = Color.white;
            this.defaultForeground = Color.black;
            if (pane != null) {
                setDefaultColor(pane);
                allowNullValues = false;
            } else {
                allowNullValues = true;
            }
        }

        public void setDefaultColor(JComponent pane) {
            if (pane != null) {
                this.defaultBackground = pane.getBackground();
                this.defaultForeground = pane.getForeground();
            }
        }

        public float getLineSpacing() { return lineSpacing; }
        public float getSpaceAbove() {return spaceAbove; }
        public String getFontFamily() { return fontFamily; }
        public int getFontSize() { return fontSize; }
        public boolean isBold() { return bold; }
        public boolean isItalic() { return italic; }
        public Color getBackgroundColor() { return backgroundColor; }
        public Color getForegroundColor() { return foregroundColor; }
        public boolean isBackgroundCustom() { return backgroundCustom; }
        public boolean isForegroundCustom() { return foregroundCustom; }
        public boolean isWrapText() { return wrapText; }

        public void setLineSpacing(float lineSpacing) {
            if (allowNullValues || lineSpacing != DEFAULT_NUM) {
                this.lineSpacing = lineSpacing;
            }
        }
        public void setSpaceAbove(float spaceAbove) {
            if (allowNullValues || spaceAbove != DEFAULT_NUM) {
                this.spaceAbove = spaceAbove;
            }
        }
        public void setFontFamily(String fontFamily) {
            if (allowNullValues || (!Objects.equals(fontFamily, DEFAULT_FONT_FAMILY) && fontFamily != null)) {
                this.fontFamily = fontFamily;
            }
        }
        public void setFontSize(int fontSize) {
            if (allowNullValues || fontSize != DEFAULT_NUM) {
                this.fontSize = fontSize;
            }
        }
        public void setBackgroundColor(Color backgroundColor) {
            if (allowNullValues || backgroundColor != null) {
                this.backgroundColor = backgroundColor;
            }
        }
        public void setForegroundColor(Color foregroundColor) {
            if (allowNullValues || foregroundColor != null) {
                this.foregroundColor = foregroundColor;
            }
        }
        public void setBold(boolean bold) { this.bold = bold; }
        public void setItalic(boolean italic) { this.italic = italic; }
        public void setBackgroundCustom(boolean backgroundCustom) { this.backgroundCustom = backgroundCustom; }
        public void setForegroundCustom(boolean foregroundCustom) { this.foregroundCustom = foregroundCustom; }
        public void setWrapText(boolean wrapLine) { this.wrapText = wrapLine; }

        /**
         * overwrites properties by values from {@link UIManagerUtil};
         *  also {@code lineSpacing} is 0.2, {@code spaceAbove} is 2.0, no background and foregorund custom,
         *    and {@code wrapText} is true.
         */
        public void setUiDefault() {
            var ui = UIManagerUtil.getInstance();
            var font = ui.getEditorPaneFont();
            setLineSpacing(ui.getScaledSizeFloat(0.2f));
            setSpaceAbove(ui.getScaledSizeFloat(2.0f));
            setFontFamily(font.getFamily());
            setFontSize(font.getSize());
            setBold(font.isBold());
            setItalic(font.isItalic());
            setBackgroundColor(ui.getTextPaneBackground());
            setForegroundColor(ui.getTextPaneForeground());
            setBackgroundCustom(false);
            setForegroundCustom(false);
            setWrapText(true);
        }

        public static Object toJsonColor(Color c) {
            return c == null ? new ArrayList<>(0) : toJsonColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        }

        public static Object toJsonColor(int r, int g, int b, int a) {
            return new ArrayList<>(Arrays.asList(r, g, b, a));
        }

        public static Color fromJsonColorSource(String src) {
            return (src == null || src.isEmpty()) ? null : fromJsonColor(JsonReader.create(src).parseValue());
        }

        public static Color fromJsonColor(Object o) {
            if (o != null) {
                if (o instanceof List<?> list) {
                    int r = !list.isEmpty() ? ((Number) list.get(0)).intValue() : 0;
                    int g = list.size() >= 2 ? ((Number) list.get(1)).intValue() : 0;
                    int b = list.size() >= 3 ? ((Number) list.get(2)).intValue() : 0;
                    int a = list.size() >= 4 ? ((Number) list.get(3)).intValue() : 0;
                    return new Color(r, g, b, a);
                } else { //-1
                    return null;
                }
            } else {
                return null;
            }
        }

        /**
         * set propertis from the given style. if the style has no attribute-value, the property will be the null-value.
         * @param style the source style can be {@link Style}
         */
        public void setFrom(MutableAttributeSet style) {
            var lineSpacing = style.getAttribute(StyleConstants.LineSpacing);
            var spaceAbove = style.getAttribute(StyleConstants.SpaceAbove);
            var fontFamily = style.getAttribute(StyleConstants.FontFamily);
            var fontSize = style.getAttribute(StyleConstants.FontSize);
            var bold = style.getAttribute(StyleConstants.Bold);
            var italic = style.getAttribute(StyleConstants.Italic);
            var backgroundColor = style.getAttribute(StyleConstants.Background);
            var foregroundColor = style.getAttribute(StyleConstants.Foreground);
            setLineSpacing(floatStyleValue(lineSpacing));
            setSpaceAbove(floatStyleValue(spaceAbove));
            setFontFamily(styleValue(String.class, fontFamily, null));
            setFontSize(intStyleValue(fontSize));
            setBold(styleValue(Boolean.class, bold, this.bold));
            setItalic(styleValue(Boolean.class, italic, this.italic));
            setBackgroundCustom(nonNullDifferentColor(backgroundColor, defaultBackground));
            setForegroundCustom(nonNullDifferentColor(foregroundColor, defaultForeground));
            setBackgroundColor(styleValue(Color.class, backgroundColor, null));
            setForegroundColor(styleValue(Color.class, foregroundColor, null));
        }

        private boolean nonNullDifferentColor(Object color, Color defaultColor) {
            return color != null && !Objects.equals(color, defaultColor);
        }

        private float floatStyleValue(Object value) {
            if (value instanceof Number num) {
                return num.floatValue();
            } else {
                return DEFAULT_NUM;
            }
        }
        private int intStyleValue(Object value) {
            if (value instanceof Number num) {
                return num.intValue();
            } else {
                return DEFAULT_NUM;
            }
        }
        private <T> T styleValue(Class<T> type, Object value, T defVal) {
            if (!type.isInstance(value)) { //include null
                return defVal;
            } else {
                return type.cast(value);
            }
        }

        /**
         * @param pane the target-text-pane
         * @see #setFrom(MutableAttributeSet)
         */
        public void setFrom(JComponent pane) {
            var style = getTargetStyle(pane);
            if (style != null) {
                setFrom(style);
            }
            var wrapScroll = isWrapLine(scrollPane(pane));
            setWrapText(wrapScroll);
        }

        /**
         * set properties from the given style; only the properties the style have,
         *  and {@link #applyTo(MutableAttributeSet)} to reflect the all propertis of the prefs to the target-pane
         * @param style the source style
         */
        public void setup(MutableAttributeSet style) {
            setFrom(style);
            applyTo(style);
        }

        /**
         * calls {@link #setup(MutableAttributeSet)} for the document of the pane ({@link #getTargetStyle(JComponent)});
         *  and {@link #applyToDocument(StyledDocument, MutableAttributeSet)} for overwritng the style to the entire text.
         *  Also update {@code wrapText} and {@link #applyToComponentWithoutDocument(JComponent)}
         * @param pane the target text-pane
         * @see #setup(MutableAttributeSet)
         */
        public void setup(JComponent pane) {
            var wrapScroll = isWrapLine(scrollPane(pane)); //apparently, the order matters; it seemed to overwrite the default style
            setWrapText(wrapScroll);
            applyToComponentWithoutDocument(pane);

            var style = getTargetStyle(pane);
            if (style != null) {
                setup(style);
                applyToDocument(getTargetDocument(pane), style);
            }
        }

        /**
         * update the target style by properties of the prefs, or if remove attributes by null-values
         * @param style the modified style
         */
        public void applyTo(MutableAttributeSet style) {
            var lineSpacing = getLineSpacing();
            var spaceAbove = getSpaceAbove();
            var fontFamily = getFontFamily();
            var fontSize = getFontSize();
            var bold = isBold();
            var italic = isItalic();
            var wrapText = isWrapText();
            var backgroundCustom = isBackgroundCustom();
            var foregroundCustom = isForegroundCustom();
            var backgroundColor = getBackgroundColor();
            var foregroundColor = getForegroundColor();

            if (lineSpacing == (DEFAULT_NUM)) {
                style.removeAttribute(StyleConstants.LineSpacing);
            } else {
                StyleConstants.setLineSpacing(style, lineSpacing);
            }
            if (spaceAbove == (DEFAULT_NUM)) {
                style.removeAttribute(StyleConstants.SpaceAbove);
            } else {
                StyleConstants.setSpaceAbove(style, spaceAbove);
            }
            if (fontFamily.equals(DEFAULT_FONT_FAMILY)) {
                style.removeAttribute(StyleConstants.FontFamily);
            } else {
                StyleConstants.setFontFamily(style, fontFamily);
            }
            if (fontSize == (DEFAULT_NUM)) {
                style.removeAttribute(StyleConstants.FontSize);
            } else {
                StyleConstants.setFontSize(style, fontSize);
            }
            StyleConstants.setBold(style, bold);
            StyleConstants.setItalic(style, italic);
            if (!backgroundCustom || backgroundColor == null) {
                backgroundColor = defaultBackground;
            }
            StyleConstants.setBackground(style, backgroundColor);
            if (!foregroundCustom || foregroundColor == null) {
                foregroundColor = defaultForeground;
            }
            StyleConstants.setForeground(style, foregroundColor);
        }

        /**
         * set style of the target-text-pane by {@link #getTargetStyle(JComponent)} and {@link #applyTo(MutableAttributeSet)}
         *  ,{@link #applyToDocument(StyledDocument, MutableAttributeSet)} and {@link #applyToComponentWithoutDocument(JComponent)}.
         * @param pane the target text-pane
         */
        public void applyTo(JComponent pane) {
            applyToComponentWithoutDocument(pane); //apparently, the order matters; it seemed to overwrite the default style
            var style = getTargetStyle(pane);
            if (style != null) {
                applyTo(style);
                applyToDocument(getTargetDocument(pane), style);
            }
        }

        /**
         * set the style of the entire-text of the document
         * @param doc the target-document or null
         * @param style the stytle to be set
         */
        public void applyToDocument(StyledDocument doc, MutableAttributeSet style) {
            if (doc != null) {
                doc.setParagraphAttributes(0, doc.getLength(), style, true);
            }
        }

        /**
         * set the background and foreground properties ({@link JComponent#setBackground(Color)} and {@link JComponent#setForeground(Color)})
         *  and wrapText by {@link #setWrapLine(JScrollPane, JComponent, boolean)}.
         *  Implementation note: it seemed to change the attributes of the default document-style. So it needs to call the method before updating the style
         * @param pane the target text-pane
         */
        public void applyToComponentWithoutDocument(JComponent pane) {
            var backfgroundColor = isBackgroundCustom() ? getBackgroundColor() : this.defaultBackground;
            var foregroundColor = isForegroundCustom() ? getForegroundColor() : this.defaultForeground;
            pane.setBackground(backfgroundColor);
            pane.setForeground(foregroundColor);
            if (pane instanceof JTextComponent textPane) {
                textPane.setCaretColor(foregroundColor);
            }
            var wrapLine = isWrapText();
            setWrapLine(scrollPane(pane), pane, wrapLine);
        }

        public StyledDocument getTargetDocument(JComponent pane) {
            if (pane instanceof JTextComponent textPane &&
                    textPane.getDocument() instanceof StyledDocument doc) {
                return doc;
            } else {
                return null;
            }
        }

        public Style getTargetStyle(JComponent pane) {
            var doc = getTargetDocument(pane);
            if (doc != null) {
                return doc.getStyle(StyleContext.DEFAULT_STYLE);
            } else {
                return null;
            }
        }

        /**
         * if parentScroll is non-null and {@link #isWrapLineNotMatch(JScrollPane, boolean)} with f,
         *  changes the wrapping-line by {@link TextPaneBuilder#setWrapLine(JScrollPane, boolean)}.
         *  Also if the field is a {@link WrapLineSupport}, call {@link WrapLineSupport#setWrapLine(boolean)}
         * @param parentScroll the parent pane of the field
         * @param field the target text-component
         * @param f the new flag value of {@code wrapText}
         */
        public void setWrapLine(JScrollPane parentScroll, JComponent field, boolean f) {
            if (parentScroll != null && isWrapLineNotMatch(parentScroll, f)) {
                TextPaneBuilder.setWrapLine(parentScroll, f);
                if (field instanceof WrapLineSupport editorPane) {
                    editorPane.setWrapLine(f);
                }
            }
        }
    }

    public static class StyleSetAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected Consumer<StyleSetAction> callback;
        @SuppressWarnings("this-escape")
        public StyleSetAction(String name, boolean initVal, Consumer<StyleSetAction> callback) {
            this.callback = callback;
            putValue(NAME, name);
            putValue(SELECTED_KEY, initVal);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            callback.accept(this);
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_PREFS;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_PREFS_CHANGE;
        }

        public void change(boolean v) {
            putValue(SELECTED_KEY, v);
            actionPerformed(null);
        }
    }

    public static class DocumentSettingAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected DocumentSettingPaneBase pane;
        protected JPanel contentPane;
        protected SettingsWindow.SettingsWindowClientBase editorPane;
        @SuppressWarnings("this-escape")
        public DocumentSettingAction(JComponent label, SettingsWindow.SettingsWindowClientBase editorPane, DocumentSettingPaneBase settingPane) {
            putValue(NAME, "Settings...");

            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_COMMA,
                            PopupExtension.getMenuShortcutKeyMask(), KeyEvent.SHIFT_DOWN_MASK));
            this.pane = settingPane;
            this.editorPane = editorPane;
            contentPane = new JPanel(new BorderLayout());
            contentPane.add(label, BorderLayout.NORTH);
            contentPane.add(pane, BorderLayout.CENTER);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JComponent sender = null;
            if (editorPane instanceof JComponent comp) {
                sender = comp;
            }
            editorPane.getSettingsWindow().show("Document Settings", sender, contentPane);
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_PREFS;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_PREFS_WINDOW;
        }

        public DocumentSettingPaneBase getPane() {
            return pane;
        }
    }

    public static <DocType extends Document> DocumentCharSequence<DocType> charSequence(DocType doc) {
        return new DocumentCharSequence<>(doc);
    }

    /**
     * the char-sequence definition wrapping a {@link Document}.
     * it also supports {@link Appendable} and some methods defined in {@link StringBuilder}.
     * <p>
     * it can be careated by {@link TextPaneBuilder#charSequence(Document)}
     *
     * @param <DocType> the concrete {@link Document} type
     * @since 1.9
     */
    public static class DocumentCharSequence<DocType extends Document> implements CharSequence, Appendable {
        protected DocType document;
        protected int cacheOffset = -1;
        protected String cache;

        public DocumentCharSequence(DocType doc) {
            this.document = doc;
            setCache(null, -1);
            doc.addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateAsInvalidAfter(e.getOffset());
                }
                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateAsInvalidAfter(e.getOffset());
                }
                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateAsInvalidAfter(e.getOffset());
                }
            });
        }

        public DocType document() {
            return document;
        }

        protected final void updateAsInvalidAfter(int off) {
            if (cacheOffset >= 0 && cacheOffset <= off) {
                setCache(null, -1);
            }
        }

        protected final void setCache(String cache, int offset) {
            this.cache = cache;
            cacheOffset = offset;
        }

        @Override
        public int length() {
            return document.getLength();
        }

        @Override
        public char charAt(int index) {
            if (!cacheRange(index)) {
                try {
                    int len = length();
                    setCache(document.getText(index, Math.min(len, index + cacheLength()) - index), index);
                } catch (BadLocationException e) {
                    throw new IndexOutOfBoundsException(e.offsetRequested());
                }
            }
            return cache.charAt(index - cacheOffset);
        }

        protected boolean cacheRange(int index) {
            return cacheOffset >= 0 && cacheOffset <= index && index < cacheOffset + cache.length();
        }

        protected int cacheLength() {
            return 10_000;
        }

        @Override
        public String subSequence(int start, int end) {
            if (start == end) {
                return "";
            } else {
                try {
                    return document.getText(start, end - start);
                } catch (BadLocationException e) {
                    throw new IndexOutOfBoundsException(e.offsetRequested());
                }
            }
        }

        @Override
        public String toString() {
            if (cacheOffset != 0 || cache.length() != length()) {
                setCache(subSequence(0, length()), 0);
            }
            return cache;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof CharSequence s) {
                return CharSequence.compare(this, s) == 0;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public DocumentCharSequence<DocType> append(CharSequence csq) {
            return insert(length(), csq.toString());
        }

        @Override
        public DocumentCharSequence<DocType> append(CharSequence csq, int start, int end)  {
            return append(csq.subSequence(start, end));
        }

        @Override
        public DocumentCharSequence<DocType> append(char c)  {
            return append(String.valueOf(c));
        }

        public DocumentCharSequence<DocType> delete(int start, int end) {
            try {
                document.remove(start, end - start);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public DocumentCharSequence<DocType> deleteCharAt(int index) {
            return delete(index, index + 1);
        }

        public int indexOf(String str) {
            return indexOf(str, 0);
        }

        public int indexOf(String str, int fromIndex) {
            return toString().indexOf(str, fromIndex);
        }

        public int lastIndexOf(String str) {
            return lastIndexOf(str, length());
        }

        public int lastIndexOf(String str, int fromIndex) {
            return toString().lastIndexOf(str, fromIndex);
        }

        public DocumentCharSequence<DocType> insert(int offset, String s) {
            try {
                document.insertString(offset, s, null);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public DocumentCharSequence<DocType> setCharAt(int index, char c) {
            deleteCharAt(index);
            insert(index, String.valueOf(c));
            return this;
        }

        public DocumentCharSequence<DocType> replace(int start, int end, String s) {
            delete(start, end);
            return insert(start, s);
        }

        public boolean contains(String str) {
            return indexOf(str) != -1;
        }

        public DocumentCharSequence<DocType> set(String str) {
            delete(0, length());
            return insert(0, str);
        }
    }

}
