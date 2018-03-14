package autogui.swing;

import autogui.base.JsonReader;
import autogui.base.JsonWriter;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
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
    public JComponent createView(GuiMappingContext context) {
        GuiReprValueDocumentEditor doc = (GuiReprValueDocumentEditor) context.getRepresentation();
        ValuePane<Object> text = doc.isStyledDocument(context) ?
                new PropertyDocumentTextPane(context) : new PropertyDocumentEditorPane(context);
        ValuePane<Object> pane = text.wrapScrollPane(true, false);
        if (context.isTypeElementProperty()) {
            return pane.wrapProperty();
        } else {
            return pane.asComponent();
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static PopupExtension initText(JEditorPane pane, GuiMappingContext context) {
        GuiMappingContext.SourceUpdateListener l = (GuiMappingContext.SourceUpdateListener) pane;
        //context update
        context.addSourceUpdateListener(l);
        //initial update
        l.update(context, context.getSource());

        pane.setMinimumSize(new Dimension(1, 1));

        //popup
        JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
        JComponent infoForSetting = GuiSwingContextInfo.get().getInfoLabel(context);
        List<Action> actions = PopupExtensionText.getEditActions(pane);
        ContextRefreshAction refreshAction = new ContextRefreshAction(context);
        DocumentSettingAction settingAction = (pane instanceof PropertyDocumentTextPane ?
                new DocumentSettingAction(infoForSetting, pane, ((PropertyDocumentTextPane) pane).getSettingPane()) : null);
        PopupExtensionText ext = new PopupExtensionText(pane, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
            menu.accept(info);
            menu.accept(refreshAction);
            actions.forEach(menu::accept);

            menu.accept(new JPopupMenu.Separator());
            if (pane instanceof ValuePane) {
                GuiSwingJsonTransfer.getActions((ValuePane) pane, context)
                    .forEach(menu::accept);
            }

            if (settingAction != null) {
                menu.accept(new JPopupMenu.Separator());
                menu.accept(settingAction);
            }
        });
        pane.setInheritsPopupMenu(true);

        //key-binding
        PopupExtensionText.putInputEditActions(pane);
        PopupExtensionText.putUnregisteredEditActions(pane);

        //selection highlight
        pane.setCaret(new DefaultCaret() {
            SelectionHighlightPainter painter = new SelectionHighlightPainter();
            @Override
            protected Highlighter.HighlightPainter getSelectionPainter() {
                return painter;
            }
        });

        return ext;
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

                Rectangle startRect = c.modelToView(offs0);
                Rectangle endRect = c.modelToView(offs1);

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

    public static void setSwingViewValue(JEditorPane pane, GuiMappingContext context, Object newValue, boolean contextUpdate) {
        GuiReprValueDocumentEditor docEditor = (GuiReprValueDocumentEditor) context.getRepresentation();
        Document doc = docEditor.toUpdateValue(context, newValue);
        if (pane.getDocument() != doc && doc != null) {
            pane.setDocument(doc);

            //line numbering
            new LineNumberPane(pane).install();
            pane.repaint();
        }
        if (contextUpdate) {
            //newValue will be a Document
            if (newValue instanceof Document) {
                newValue = docEditor.toSourceValue(context, (Document) newValue);
            }
            docEditor.updateFromGui(context, newValue);
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
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object> { //ValuePane<StringBuilder|Content|Document>
        protected GuiMappingContext context;
        protected PopupExtension popup;
        protected boolean wrapLine = true;

        public PropertyDocumentEditorPane(GuiMappingContext context) {
            this.context = context;
            popup = initText(this, context);
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
            GuiSwingViewDocumentEditor.setSwingViewValue(this, context, value, false);
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            GuiSwingViewDocumentEditor.setSwingViewValue(this, context, value, true);
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
        public GuiMappingContext getContext() {
            return context;
        }
    }

    public static class PropertyDocumentTextPane extends JTextPane
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object> { //ValuePane<StringBuilder|Content|Document>
        protected GuiMappingContext context;
        protected PopupExtension popup;
        protected boolean wrapLine = true;
        protected DocumentSettingPane settingPane;

        public PropertyDocumentTextPane(GuiMappingContext context) {
            this.context = context;
            settingPane = new DocumentSettingPane(this);
            popup = initText(this, context);
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
            GuiSwingViewDocumentEditor.setSwingViewValue(this, context, value, false);
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            GuiSwingViewDocumentEditor.setSwingViewValue(this, context, value, true);
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
        public GuiMappingContext getContext() {
            return context;
        }

        public DocumentSettingPane getSettingPane() {
            return settingPane;
        }

        @Override
        public void savePreferences(GuiPreferences prefs) {
            GuiPreferences targetPrefs = prefs.getDescendant(context);
            Map<String, Object> jsonObj = settingPane.getJson();
            GuiPreferences.GuiValueStore store = targetPrefs.getValueStore();
            store.putString("style", JsonWriter.create().write(jsonObj).toSource());
        }

        @Override
        public void loadPreferences(GuiPreferences prefs) {
            GuiPreferences targetPrefs = prefs.getDescendant(context);
            GuiPreferences.GuiValueStore store = targetPrefs.getValueStore();
            Object jsonObj = JsonReader.create(store.getString("style", "null")).parseValue();
            settingPane.setJson(jsonObj);

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

    public static class TextWrapTextAction extends AbstractAction {
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
    }


    public static class DocumentSettingAction extends AbstractAction {
        protected DocumentSettingPane pane;
        protected JPanel contentPane;
        public DocumentSettingAction(JComponent label, JEditorPane editorPane, DocumentSettingPane settingPane) {
            putValue(NAME, "Settings...");
            this.pane = settingPane;
            contentPane = new JPanel(new BorderLayout());
            contentPane.add(label, BorderLayout.NORTH);
            contentPane.add(pane, BorderLayout.CENTER);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SettingsWindow.get().show(pane, contentPane);
        }
    }

    public static class DocumentSettingPane extends JPanel implements ItemListener, ChangeListener {
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

        public DocumentSettingPane(JEditorPane pane) {
            setBorder(BorderFactory.createEmptyBorder(3, 10, 10, 10));
            this.pane = pane;

            //font name
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            fontFamily = new JComboBox<>(env.getAvailableFontFamilyNames());
            fontFamily.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setFont(getListFont((String) value));
                    return this;
                }
            });
            fontFamily.addItemListener(this);

            //font size
            fontSize = new JSpinner(new SpinnerNumberModel(14, 0, 400, 1));
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
            new Timer(200, e -> wrapText.updateSelected())
                    .start();

            JButton styleButton = new JButton("Style");
            PopupButtonListener buttonListener = new PopupButtonListener(styleButton, fontStyleMenu);
            styleButton.addActionListener(buttonListener);
            fontStyleMenu.addPopupMenuListener(buttonListener);

            //line spacing
            lineSpacing = new JSpinner(new SpinnerNumberModel(StyleConstants.getLineSpacing(getTargetStyle()),
                    Short.MIN_VALUE, Short.MAX_VALUE, 0.1f));
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
                    n -> new Font(family, Font.PLAIN, 14));
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
            }
            pane.repaint();
        }

        public void updateFromStyle() {
            updateDisabled = true;
            try {
                Style style = getTargetStyle();
                if (style != null) {
                    lineSpacing.setValue(StyleConstants.getLineSpacing(style));
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
                jsonSet(map, "lineSpacing", Number.class, v -> lineSpacing.setValue(v));
                jsonSet(map, "fontFamily", String.class, v -> fontFamily.setSelectedItem(v));
                jsonSet(map, "fontSize", Number.class, v -> fontSize.setValue(v));
                jsonSet(map, "bold", Boolean.class, v -> styleBold.putValue(Action.SELECTED_KEY, v));
                jsonSet(map, "italic", Boolean.class, v -> styleItalic.putValue(Action.SELECTED_KEY,v));
                Color c = fromJsonColor(map.get("backgroundColor"));
                if (c != null) {
                    backgroundColor.setColor(c);
                }
                c = fromJsonColor("foregroundColor");
                if (c != null) {
                    foregroundColor.setColor(c);
                }
                jsonSet(map, "wrapText", Boolean.class, wrapText::change);
                updateStyle();
            }
        }

        public Map<String,Object> getJson() {
            Map<String, Object> json = new HashMap<>();
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

    public static class StyleSetAction extends AbstractAction {
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
    }


    ///////////////////

}
