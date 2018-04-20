package autogui.swing;

import autogui.base.JsonReader;
import autogui.base.JsonWriter;
import autogui.base.log.GuiLogManager;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.mapping.GuiReprValueDocumentEditor;
import autogui.swing.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <h3>representation</h3>
 * {@link GuiReprValueDocumentEditor}
 *
 * <h3>{@link PropertyDocumentEditorPane#getSwingViewValue()} or
 *      {@link PropertyDocumentTextPane#getSwingViewValue()}</h3>
 * {@link Document},  {@link AbstractDocument.Content}, or {@link StringBuilder}
 *
 * <h3>history-value</h3>
 * TODO unsupported yet
 *
 * <h3>string-transfer</h3>
 * TODO unsupported yet
 */
public class GuiSwingViewDocumentEditor implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        GuiReprValueDocumentEditor doc = (GuiReprValueDocumentEditor) context.getRepresentation();
        SpecifierManagerDefault specifierManager = new SpecifierManagerDefault(parentSpecifier);
        ValuePane<Object> text = doc.isStyledDocument(context) ?
                new PropertyDocumentTextPane(context, specifierManager) :
                new PropertyDocumentEditorPane(context, specifierManager);
        ValuePane<Object> pane = text.wrapSwingScrollPane(true, false);
        if (context.isTypeElementProperty()) {
            return pane.wrapSwingProperty();
        } else {
            return pane.asSwingViewComponent();
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static class TextPaneInitializer {
        protected JEditorPane pane;
        protected GuiMappingContext context;
        protected PopupExtension popup;

        public TextPaneInitializer(JEditorPane pane, GuiMappingContext context) {
            this.pane = pane;
            this.context = context;
            init();
        }

        public PopupExtension getPopup() {
            return popup;
        }

        public void init() {
            initName();
            initContextUpdate();
            initValue();
            initSize();
            initPopup();
            initKeyBindings();
            initHighlight();
            initUndo();
        }

        public void initName() {
            pane.setName(context.getName());
            GuiSwingView.setDescriptionToolTipText(context, pane);
        }

        public void initContextUpdate() {
            GuiMappingContext.SourceUpdateListener l = (GuiMappingContext.SourceUpdateListener) pane;
            context.addSourceUpdateListener(l);
        }

        public void initValue() {
            GuiMappingContext.SourceUpdateListener l = (GuiMappingContext.SourceUpdateListener) pane;
            l.update(context, context.getSource());
        }

        public void initSize() {
            pane.setMinimumSize(new Dimension(1, 1));
        }

        public void initPopup() {
            PopupExtension.PopupMenuBuilder builder;
            if (pane instanceof ValuePane<?>) {
                builder = new PopupCategorized(((ValuePane<?>) pane)::getSwingStaticMenuItems);
            } else {
                builder = new PopupExtension.PopupMenuBuilderEmpty();
            }
            popup = new PopupExtensionText(pane, PopupExtension.getDefaultKeyMatcher(), builder);
            pane.setInheritsPopupMenu(true);
        }

        public void initKeyBindings() {
            PopupExtensionText.putInputEditActions(pane);
            PopupExtensionText.putUnregisteredEditActions(pane);
        }

        public void initHighlight() {
            pane.setCaret(new DefaultCaret() {
                SelectionHighlightPainter painter = new SelectionHighlightPainter();
                @Override
                protected Highlighter.HighlightPainter getSelectionPainter() {
                    return painter;
                }
            });
        }

        public void initUndo() {
            new KeyUndoManager().putListenersAndActionsTo(pane);
        }
    }

    public static List<PopupCategorized.CategorizedMenuItem> getTextMenuItems(ValuePane<?> pane) {
        GuiMappingContext context = pane.getSwingViewContext();
        List<Action> settingActions = Collections.emptyList();
        if (pane instanceof PropertyDocumentTextPane) {
            settingActions = Collections.singletonList(
                    new DocumentSettingAction(
                            GuiSwingContextInfo.get().getInfoLabel(context),
                            (SettingsWindowClient) pane,
                            ((PropertyDocumentTextPane) pane).getSettingPane()));
        }
        return PopupCategorized.getMenuItems(
                Arrays.asList(
                        GuiSwingContextInfo.get().getInfoLabel(context),
                        new ContextRefreshAction(context)),
                PopupExtensionText.getEditActions((JTextComponent) pane),
                GuiSwingJsonTransfer.getActions(pane, context),
                settingActions);
    }

    public static class SelectionHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        public SelectionHighlightPainter() {
            super(UIManager.getColor("TextPane.selectionBackground"));
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
                Rectangle2D.Float selRect = new Rectangle2D.Float(startLeft, startTop, endRight - startLeft, startBottom - startTop);
                g2.fill(selRect);
                return selRect;
            } catch (Exception ex) {
                ex.printStackTrace();
                return bounds;
            }

        }
    }

    public static void setSwingViewValue(JEditorPane pane, SpecifierManager specifierManager, GuiMappingContext context, Object newValue, boolean contextUpdate) {
        GuiReprValueDocumentEditor docEditor = (GuiReprValueDocumentEditor) context.getRepresentation();
        Document doc = docEditor.toUpdateValue(context, newValue, delayedDoc -> {
            setSwingViewValueDocument(pane, specifierManager, context, delayedDoc, delayedDoc, contextUpdate);
        });
        setSwingViewValueDocument(pane, specifierManager, context, newValue, doc, contextUpdate);
    }

    public static void setSwingViewValueDocument(JEditorPane pane, SpecifierManager specifierManager, GuiMappingContext context, Object newValue, Document doc, boolean contextUpdate) {
        GuiReprValueDocumentEditor docEditor = (GuiReprValueDocumentEditor) context.getRepresentation();
        if (pane.getDocument() != doc && doc != null) {
            pane.setDocument(doc);

            //line numbering
            new LineNumberPane(pane).install();
            pane.repaint();
        }
        if (contextUpdate && docEditor.isEditable(context)) {
            //newValue will be a Document
            if (newValue instanceof Document) {
                newValue = docEditor.toSourceValue(context, (Document) newValue);
            }
            docEditor.updateFromGui(context, newValue, specifierManager.getSpecifier());
        }
    }

    /**
     *
     * @param pane the pane of the document
     * @param context the context of the view.
     * @return always return a {@link Document}
     */
    public static Object getSwingViewValue(JEditorPane pane, GuiMappingContext context) {
        GuiReprValueDocumentEditor doc = (GuiReprValueDocumentEditor) context.getRepresentation();
        return pane.getDocument();
    }

    public static Dimension getPreferredSize(JTextComponent field, boolean wrapLine) {
        Dimension dim = field.getUI().getPreferredSize(field);
        Component parent = SwingUtilities.getUnwrappedParent(field);
        if (!wrapLine && parent != null) {
            dim = new Dimension(Math.max(dim.width, parent.getSize().width), dim.height);
        }
        return dim;
    }

    public static class PropertyDocumentEditorPane extends JEditorPane
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object>,
                SettingsWindowClient { //ValuePane<StringBuilder|Content|Document>
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected PopupExtension popup;
        protected boolean wrapLine = true;
        protected SettingsWindow settingsWindow;

        public PropertyDocumentEditorPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            popup = new TextPaneInitializer(this, context).getPopup();
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            return getTextMenuItems(this);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return GuiSwingViewDocumentEditor.getSwingViewValue(this, context);
        }

        @Override
        public void setSwingViewValue(Object value) {
            GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, false);
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, true);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return wrapLine;
        }

        public void setWrapLine(boolean wrapLine) {
            this.wrapLine = wrapLine;
        }

        @Override
        public Dimension getPreferredSize() {
            try {
                return GuiSwingViewDocumentEditor.getPreferredSize(this, wrapLine);
            } catch (Exception ex) {
                return getSize();
            }
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public void setSettingsWindow(SettingsWindow settingWindow) {
            this.settingsWindow = settingWindow;
        }

        @Override
        public SettingsWindow getSettingsWindow() {
            return settingsWindow == null ? SettingsWindow.get() : settingsWindow;
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }
    }

    public static class PropertyDocumentTextPane extends JTextPane
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object>,
            SettingsWindowClient, GuiSwingPreferences.PreferencesUpdateSupport   { //ValuePane<StringBuilder|Content|Document>
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected PopupExtension popup;
        protected boolean wrapLine = true;
        protected DocumentSettingPane settingPane;
        protected SettingsWindow settingsWindow;

        public PropertyDocumentTextPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            settingPane = new DocumentSettingPane(this);
            popup = new TextPaneInitializer(this, context).getPopup();
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            return getTextMenuItems(this);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return GuiSwingViewDocumentEditor.getSwingViewValue(this, context);
        }

        @Override
        public void setSwingViewValue(Object value) {
            GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, false);
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, true);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return wrapLine;
        }

        public void setWrapLine(boolean wrapLine) {
            this.wrapLine = wrapLine;
        }

        @Override
        public Dimension getPreferredSize() {
            try {
                return GuiSwingViewDocumentEditor.getPreferredSize(this, wrapLine);
            } catch (Exception ex) {
                return getSize();
            }
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        public DocumentSettingPane getSettingPane() {
            return settingPane;
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            try {
                GuiSwingView.savePreferencesDefault(this, prefs);
                GuiPreferences targetPrefs = prefs.getDescendant(context);
                settingPane.saveTo(targetPrefs);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs) {
            try {
                GuiSwingView.loadPreferencesDefault(this, prefs);
                GuiPreferences targetPrefs = prefs.getDescendant(context);
                settingPane.loadFrom(targetPrefs);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        public void setSettingsWindow(SettingsWindow settingWindow) {
            this.settingsWindow = settingWindow;
        }

        @Override
        public SettingsWindow getSettingsWindow() {
            return settingsWindow == null ? SettingsWindow.get() : settingsWindow;
        }

        @Override
        public void shutdownSwingView() {
            settingPane.shutDown();
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            settingPane.setPreferencesUpdater(updater);
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }
    }

    ///////////


    public static JScrollPane scroll(Container c) {
        if (c == null) {
            return null;
        } else if (c instanceof JViewport) {
            return scroll(c.getParent());
        } else if (c instanceof JScrollPane) {
            return (JScrollPane) c;
        } else {
            return null;
        }
    }

    public static class TextWrapTextAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected JTextComponent field;

        public TextWrapTextAction(JTextComponent field) {
            putValue(NAME, "Wrap Line");
            this.field = field;
            putValue(SELECTED_KEY, isWrapLine(scroll(field.getParent())));
        }

        public void updateSelected() {
            putValue(SELECTED_KEY, isWrapLine(scroll(field.getParent())));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JScrollPane pane = scroll(field.getParent());
            boolean f = isWrapLine(pane);
            setWrapLine(pane, !f);
        }

        public boolean isWrapLine(JScrollPane pane) {
            if (pane == null) {
                return true;
            } else {
                return pane.getHorizontalScrollBarPolicy() == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
            }
        }

        public void setWrapLine(JScrollPane pane, boolean f) {
            if (pane != null) {
                pane.setHorizontalScrollBarPolicy(f ?
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER :
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                if (field instanceof PropertyDocumentEditorPane) {
                    ((PropertyDocumentEditorPane) field).setWrapLine(f);
                } else if (field instanceof PropertyDocumentTextPane) {
                    ((PropertyDocumentTextPane) field).setWrapLine(f);
                }
                pane.revalidate();
            }
        }

        public void change(boolean f) {
            putValue(SELECTED_KEY, f);
            JScrollPane pane = scroll(field.getParent());
            setWrapLine(pane, f);
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_PREFS;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_PREFS_CHANGE;
        }
    }


    public static class DocumentSettingAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected DocumentSettingPane pane;
        protected JPanel contentPane;
        protected SettingsWindowClient editorPane;
        public DocumentSettingAction(JComponent label, SettingsWindowClient editorPane, DocumentSettingPane settingPane) {
            putValue(NAME, "Settings...");
            this.pane = settingPane;
            this.editorPane = editorPane;
            contentPane = new JPanel(new BorderLayout());
            contentPane.add(label, BorderLayout.NORTH);
            contentPane.add(pane, BorderLayout.CENTER);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            editorPane.getSettingsWindow().show("Document Settings", pane, contentPane);
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_PREFS;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_PREFS_WINDOW;
        }
    }

    public static class DocumentSettingPane extends JPanel implements ItemListener, ChangeListener,
            GuiSwingPreferences.Preferences {
        protected JEditorPane pane;
        protected Map<String, Font> nameFonts = new HashMap<>();

        protected JComboBox<String> fontFamily;
        protected JSpinner fontSize;
        protected JPopupMenu fontStyleMenu;
        protected JSpinner lineSpacing;

        protected Action styleItalic;
        protected Action styleBold;
        protected TextWrapTextAction wrapText;
        protected boolean updateDisabled;

        protected SettingsWindow.ColorButton backgroundColor;
        protected SettingsWindow.ColorButton foregroundColor;

        protected ScheduledTaskRunner.EditingRunner updater;
        protected Consumer<GuiSwingPreferences.PreferencesUpdateEvent> preferencesUpdater;

        protected int defaultFontSize = 14;

        public DocumentSettingPane(JEditorPane pane) {
            setBorder(BorderFactory.createEmptyBorder(3, 10, 10, 10));
            this.pane = pane;

            Style s = getTargetStyle();
            if (s != null) {
                defaultFontSize = StyleConstants.getFontSize(s);
            }
            //font name
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            fontFamily = new JComboBox<>(env.getAvailableFontFamilyNames());
            fontFamily.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (isSelected) {
                        setFont(getListFont((String) value));
                    } else {
                        setFont(null);
                    }
                    return this;
                }
            });
            fontFamily.addItemListener(this);


            //font size
            fontSize = new JSpinner(new SpinnerNumberModel(defaultFontSize, 0, 400, 1));
            fontSize.addChangeListener(this);

            //font style
            styleBold = new StyleSetAction("Bold", StyleConstants.isBold(getTargetStyle()), a -> updateStyle());
            styleItalic = new StyleSetAction("Italic", StyleConstants.isBold(getTargetStyle()), a -> updateStyle());
            fontStyleMenu = new JPopupMenu();
            fontStyleMenu.add(new JCheckBoxMenuItem(styleBold));
            fontStyleMenu.add(new JCheckBoxMenuItem(styleItalic));

            wrapText = new TextWrapTextAction(pane);
            fontStyleMenu.add(new JCheckBoxMenuItem(wrapText));
            //delay checking after component setting-up
            Timer timer = new Timer(200, e -> wrapText.updateSelected());
            timer.setRepeats(false);
            timer.start();

            JButton styleButton = new JButton("Style");
            PopupButtonListener buttonListener = new PopupButtonListener(styleButton, fontStyleMenu);
            styleButton.addActionListener(buttonListener);
            fontStyleMenu.addPopupMenuListener(buttonListener);

            //line spacing: the model support double instead of float
            lineSpacing = new JSpinner(new SpinnerNumberModel((double) StyleConstants.getLineSpacing(getTargetStyle()),
                    Short.MIN_VALUE, Short.MAX_VALUE, 0.1));
            lineSpacing.addChangeListener(this);

            updater = new ScheduledTaskRunner.EditingRunner(500, l -> updateStyle());

            //color
            backgroundColor = new SettingsWindow.ColorButton(Color.white, updater::schedule);
            foregroundColor = new SettingsWindow.ColorButton(Color.black, updater::schedule);

            new SettingsWindow.LabelGroup(this)
                .addRow("Font:", fontFamily)
                .addRow("Font Size:", fontSize)
                .addRow("Style:", styleButton)
                .addRow("Line Spacing:", lineSpacing)
                .addRowFixed("Font Color:", foregroundColor)
                .addRowFixed("Background:", backgroundColor)
                .fitWidth();

            updateFromStyle();
        }

        public Font getListFont(String family) {
            return nameFonts.computeIfAbsent(family,
                    n -> new Font(family, Font.PLAIN, defaultFontSize));
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateStyle();
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            updateStyle();
        }

        public void updateStyle() {
            if (updateDisabled) {
                return;
            }
            StyledDocument doc = getTargetDocument();
            Style style = getTargetStyle();
            if (style != null) {
                StyleConstants.setLineSpacing(style, ((Number) lineSpacing.getValue()).floatValue());
                StyleConstants.setFontFamily(style, (String) fontFamily.getSelectedItem());
                StyleConstants.setFontSize(style, ((Number) fontSize.getValue()).intValue());
                StyleConstants.setBold(style, (Boolean) styleBold.getValue(Action.SELECTED_KEY));
                StyleConstants.setItalic(style, (Boolean) styleItalic.getValue(Action.SELECTED_KEY));
                pane.setBackground(backgroundColor.getColor());
                pane.setCaretColor(foregroundColor.getColor());
                StyleConstants.setForeground(style, foregroundColor.getColor());
                doc.setParagraphAttributes(0, doc.getLength(), style, true);
                sendPreferences();
            }
            pane.repaint();
        }

        public void updateFromStyle() {
            if (updateDisabled) {
                return;
            }
            updateDisabled = true;
            try {
                Style style = getTargetStyle();
                if (style != null) {
                    lineSpacing.setValue((double) StyleConstants.getLineSpacing(style));
                    fontFamily.setSelectedItem(StyleConstants.getFontFamily(style));
                    fontSize.setValue(StyleConstants.getFontSize(style));
                    styleBold.putValue(Action.SELECTED_KEY, StyleConstants.isBold(style));
                    styleItalic.putValue(Action.SELECTED_KEY, StyleConstants.isItalic(style));
                    backgroundColor.setColorWithoutUpdate(pane.getBackground());
                    foregroundColor.setColorWithoutUpdate(StyleConstants.getForeground(style));
                }
            } finally {
                updateDisabled = false;
            }
        }

        public StyledDocument getTargetDocument() {
            if (pane.getDocument() instanceof StyledDocument) {
                return (StyledDocument) pane.getDocument();
            } else {
                return null;
            }
        }

        public Style getTargetStyle() {
            StyledDocument doc = getTargetDocument();
            if (doc != null) {
                return doc.getStyle(StyleContext.DEFAULT_STYLE);
            } else {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        public void setJson(Object obj) {
            if (obj instanceof Map<?,?>) {
                Map<String, ?> map = (Map<String, ?>) obj;
                jsonSet(map, "lineSpacing", Number.class, v -> lineSpacing.setValue(v.doubleValue()));
                jsonSet(map, "fontFamily", String.class, v -> fontFamily.setSelectedItem(v));
                jsonSet(map, "fontSize", Number.class, v -> fontSize.setValue(v));
                jsonSet(map, "bold", Boolean.class, v -> styleBold.putValue(Action.SELECTED_KEY, v));
                jsonSet(map, "italic", Boolean.class, v -> styleItalic.putValue(Action.SELECTED_KEY,v));
                Color c = fromJsonColor(map.get("backgroundColor"));
                if (c != null) {
                    backgroundColor.setColor(c);
                }
                c = fromJsonColor(map.get("foregroundColor"));
                if (c != null) {
                    foregroundColor.setColor(c);
                }
                jsonSet(map, "wrapText", Boolean.class, wrapText::change);
                updateStyle();
            }
        }

        public Map<String,Object> getJson() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("lineSpacing", ((Number) lineSpacing.getValue()).floatValue());
            json.put("fontFamily", (String) fontFamily.getSelectedItem());
            json.put("fontSize", ((Number) fontSize.getValue()).intValue());
            json.put("bold", (Boolean) styleBold.getValue(Action.SELECTED_KEY));
            json.put("italic", (Boolean) styleItalic.getValue(Action.SELECTED_KEY));
            json.put("backgroundColor", toJsonColor(backgroundColor.getColor()));
            json.put("foregroundColor", toJsonColor(foregroundColor.getColor()));
            json.put("wrapText", (Boolean) wrapText.getValue(Action.SELECTED_KEY));
            return json;
        }

        public <T> void jsonSet(Map<String,?> json, String key, Class<T> type, Consumer<T> setter) {
            Object v = json.get(key);
            if (v != null && type.isInstance(v)) {
                setter.accept(type.cast(v));
            }
        }

        public Color fromJsonColor(Object o) {
            if (o != null) {
                List<?> list = (List<?>) o;
                int r = list.size() >= 1 ? ((Number) list.get(0)).intValue() : 0;
                int g = list.size() >= 2 ? ((Number) list.get(1)).intValue() : 1;
                int b = list.size() >= 3 ? ((Number) list.get(2)).intValue() : 2;
                int a = list.size() >= 4 ? ((Number) list.get(3)).intValue() : 3;
                return new Color(r, g, b, a);
            } else {
                return null;
            }
        }

        public Object toJsonColor(Color c) {
            return new ArrayList<>(Arrays.asList(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()));
        }

        public void shutDown() {
            updater.shutdown();
            backgroundColor.dispose();
            foregroundColor.dispose();
        }

        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> preferencesUpdater) {
            this.preferencesUpdater = preferencesUpdater;
        }

        public void sendPreferences() {
            if (pane instanceof ValuePane<?> && preferencesUpdater != null) {
                preferencesUpdater.accept(
                        new GuiSwingPreferences.PreferencesUpdateEvent(((ValuePane) pane).getSwingViewContext(), this));
            }
        }

        @Override
        public void loadFrom(GuiPreferences prefs) {
            GuiPreferences.GuiValueStore store = prefs.getValueStore();
            Map<String,Object> json = new HashMap<>();
            for (Map.Entry<String,Object> e : getJson().entrySet()) { //JSON has all keys and values
                String k = e.getKey();
                String storeVal = store.getString(k, "");
                Object exVal = e.getValue();
                if (!storeVal.isEmpty()) {
                    if (exVal instanceof String) {
                        json.put(k, storeVal);
                    } else if (exVal instanceof Boolean) {
                        json.put(k, storeVal.endsWith("true"));
                    } else if (exVal instanceof Number) {
                        try {
                            json.put(k, Integer.valueOf(storeVal));
                        } catch (Exception ex) {
                            json.put(k, Float.valueOf(storeVal));
                        }
                    } else {
                        json.put(k, JsonReader.create(storeVal).parseValue());
                    }
                }
            }
            setJson(json);
            updateStyle();
        }

        @Override
        public void saveTo(GuiPreferences prefs) {
            GuiPreferences.GuiValueStore store = prefs.getValueStore();
            for (Map.Entry<String,Object> e : getJson().entrySet()) {
                String k = e.getKey();
                Object v = e.getValue();
                if (v instanceof String) {
                    store.putString(k, (String) v);
                } else if (v instanceof Number) {
                    store.putString(k, v.toString());
                } else if (v instanceof Boolean) {
                    store.putString(k, v.equals(Boolean.TRUE) ? "true" : "false");
                } else {
                    store.putString(k, JsonWriter.create()
                            .withNewLines(false).write(v).toSource());
                }
            }
        }
    }

    public static class PopupButtonListener implements ActionListener, PopupMenuListener {
        protected Instant cancelTime = Instant.EPOCH;
        protected JComponent button;
        protected JPopupMenu menu;

        public PopupButtonListener(JComponent button, JPopupMenu menu) {
            this.button = button;
            this.menu = menu;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Duration d = Duration.between(cancelTime, Instant.now());
            if (d.compareTo(Duration.ofMillis(100)) > 0) {
                menu.show(button, 0, button.getHeight());
            }
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            cancelTime = Instant.EPOCH;
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            cancelTime = Instant.now();
        }
    }

    public static class StyleSetAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected Consumer<StyleSetAction> callback;
        public StyleSetAction(String name, boolean initVal, Consumer<StyleSetAction> callback) {
            putValue(NAME, name);
            putValue(SELECTED_KEY, initVal);
            this.callback = callback;
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
    }


    ///////////////////

}
