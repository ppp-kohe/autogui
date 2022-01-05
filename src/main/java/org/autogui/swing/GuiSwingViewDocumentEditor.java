package org.autogui.swing;

import org.autogui.base.JsonReader;
import org.autogui.base.JsonWriter;
import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.mapping.GuiReprValueDocumentEditor;
import org.autogui.swing.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * a swing view for {@link GuiReprValueDocumentEditor}
 *
 * <h3>swing-value </h3>
 * {@link PropertyDocumentEditorPane#getSwingViewValue()} or
 *      {@link PropertyDocumentTextPane#getSwingViewValue()}:
 * {@link Document},  {@link AbstractDocument.Content}, {@link StringBuilder},
 *    or {@link SwingDeferredRunner.TaskResultFuture} handled by
 *        {@link GuiReprValueDocumentEditor#toUpdateValue(GuiMappingContext, Object, Consumer)}
 *      <ul>
 *       <li>setting to the value is processed by
 *          {@link #setSwingViewValue(JEditorPane, GuiSwingView.SpecifierManager, GuiMappingContext, Object, boolean, GuiTaskClock)}.
 *          it calls {@link GuiReprValueDocumentEditor#toUpdateValue(GuiMappingContext, Object, Consumer)}.
 *              if the given value is a {@link SwingDeferredRunner.TaskResultFuture},.
 *            it immediately obtains the result value as a Document by re-run toUpdateValue with the value of the future, or
 *               returns null with runs a delayed task.</li>
 *       <li>For another non-Document value, toUpdateValue returns a Document wrapping the value</li>
 *       <li>As the delayed task,
 *       {@link #setSwingViewValueDocument(JEditorPane, GuiSwingView.SpecifierManager, GuiMappingContext, Object, Document, boolean, GuiTaskClock)}.
 *          is used. the method updates Document of the pane by {@link JEditorPane#setDocument(Document)}
 *             and calls {@link GuiSwingView.ValuePane#updateFromGui(GuiSwingView.ValuePane, Object, GuiTaskClock)} (if contextUpdate=true) </li>
 *      </ul>
 *
 * <h3>history-value</h3>
 * TODO unsupported yet
 *
 * <h3>string-transfer</h3>
 * TODO unsupported yet
 *
 * <h3>preferences</h3>
 * <pre>
 *     {
 *         "lineSpacing": Number,
 *         "fontFamily": String,
 *         "fontSize": Number,
 *         "bold": Boolean,
 *         "italic": Boolean,
 *         "backgroundColor": "[intR, intG, intB, intA]",
 *         "foregroundColor": "[intR, intG, intB, intA]",
 *         "wrapText": Boolean
 *     }
 * </pre>
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
            l.update(context, context.getSource().getValue(), context.getContextClock().copy());
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
            if (pane instanceof ValuePane<?>) {
                GuiSwingView.setupKeyBindingsForStaticMenuItems((ValuePane<?>) pane);
            }
        }

        public void initKeyBindings() {
            PopupExtensionText.putInputEditActions(pane);
            PopupExtensionText.putUnregisteredEditActions(pane);
        }

        public void initHighlight() {
            pane.setCaret(new DefaultCaret() {
                private static final long serialVersionUID = 1L;
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

    public static List<PopupCategorized.CategorizedMenuItem> getTextMenuItems(ValuePane<?> pane,
                                                                              MenuBuilder.MenuLabel infoLabel) {
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
                        infoLabel,
                        new ContextRefreshAction(context, pane)),
                PopupExtensionText.getEditActions((JTextComponent) pane),
                GuiSwingJsonTransfer.getActions(pane, context),
                settingActions);
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
                g2.fill(selRect);
                return selRect;
            } catch (Exception ex) {
                ex.printStackTrace();
                return bounds;
            }

        }
    }

    public static void setSwingViewValue(JEditorPane pane, SpecifierManager specifierManager, GuiMappingContext context,
                                         Object newValue, boolean contextUpdate, GuiTaskClock viewClock) {
        GuiReprValueDocumentEditor docEditor = (GuiReprValueDocumentEditor) context.getRepresentation();
        Document doc = docEditor.toUpdateValue(context, newValue, delayedDoc -> {
            setSwingViewValueDocument(pane, specifierManager, context, delayedDoc, delayedDoc, contextUpdate, viewClock);
        });
        if (doc != null) {
            setSwingViewValueDocument(pane, specifierManager, context, newValue, doc, contextUpdate, viewClock);
        }
    }

    public static void setSwingViewValueDocument(JEditorPane pane, SpecifierManager specifierManager, GuiMappingContext context,
                                                 Object newValue, Document doc, boolean contextUpdate, GuiTaskClock viewClock) {
        GuiReprValueDocumentEditor docEditor = (GuiReprValueDocumentEditor) context.getRepresentation();
        if (doc != null && !Objects.equals(pane.getDocument(), doc)) {
            pane.setDocument(doc);
            pane.repaint();
        }
        if (contextUpdate && docEditor.isEditable(context)) {
            //newValue will be a Document
            if (newValue instanceof Document) {
                newValue = docEditor.toSourceValue(context, (Document) newValue);
            }
            docEditor.updateFromGui(context, newValue, specifierManager.getSpecifier(), viewClock.copy());
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
        private static final long serialVersionUID = 1L;
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected PopupExtension popup;
        protected boolean wrapLine = true;
        protected SettingsWindow settingsWindow;
        protected MenuBuilder.MenuLabel infoLabel;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);
        protected LineNumberPane lineNumberPane;

        public PropertyDocumentEditorPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            popup = new TextPaneInitializer(this, context).getPopup();
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
        }

        @Override
        public void setDocument(Document doc) {
            if (lineNumberPane != null) {
                lineNumberPane.uninstall();
            }
            super.setDocument(doc);
            installLineNumberPane();
        }

        protected void installLineNumberPane() {
            if (getDocument() != null) {
                lineNumberPane = new LineNumberPane(this);
                lineNumberPane.install();
                revalidate();
            }
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            return getTextMenuItems(this, infoLabel);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            if (viewClock.isOlderWithSet(contextClock)) { //the source from target precedes other GUI generated values
                SwingUtilities.invokeLater(() ->
                        GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, newValue, false, viewClock));
            }
        }

        @Override
        public Object getSwingViewValue() {
            return GuiSwingViewDocumentEditor.getSwingViewValue(this, context);
        }

        @Override
        public void setSwingViewValue(Object value) {
            viewClock.increment();
            GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, false, viewClock);
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            viewClock.increment();
            GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, true, viewClock);
        }

        @Override
        public void setSwingViewValue(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, false, viewClock);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, true, viewClock);
            }
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

        @Override
        public void setKeyStrokeString(String keyStrokeString) {
            infoLabel.setAdditionalInfo(keyStrokeString);
        }

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
        }
    }

    public static class PropertyDocumentTextPane extends JTextPane
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object>,
            SettingsWindowClient, GuiSwingPreferences.PreferencesUpdateSupport   { //ValuePane<StringBuilder|Content|Document>
        private static final long serialVersionUID = 1L;
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected PopupExtension popup;
        protected boolean wrapLine = true;
        protected DocumentSettingPane settingPane;
        protected SettingsWindow settingsWindow;
        protected MenuBuilder.MenuLabel infoLabel;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);
        protected LineNumberPane lineNumberPane;

        public PropertyDocumentTextPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            settingPane = new DocumentSettingPane(this);
            popup = new TextPaneInitializer(this, context).getPopup();
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
        }

        @Override
        public void setDocument(Document doc) {
            if (lineNumberPane != null) {
                lineNumberPane.uninstall();
            }
            super.setDocument(doc);
            installLineNumberPane();
        }

        protected void installLineNumberPane() {
            if (getDocument() != null) {
                lineNumberPane = new LineNumberPane(this);
                lineNumberPane.install();
                revalidate();
            }
        }


        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            return getTextMenuItems(this, infoLabel);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            if (viewClock.isOlderWithSet(contextClock)) {  //the source from target precedes other GUI generated values
                SwingUtilities.invokeLater(() ->
                        GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, newValue, false, viewClock));
            }
        }

        @Override
        public Object getSwingViewValue() {
            return GuiSwingViewDocumentEditor.getSwingViewValue(this, context);
        }

        @Override
        public void setSwingViewValue(Object value) {
            viewClock.increment();
            GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, false, viewClock);
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            viewClock.increment();
            GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, true, viewClock);
        }

        @Override
        public void setSwingViewValue(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, false, viewClock);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                GuiSwingViewDocumentEditor.setSwingViewValue(this, specifierManager, context, value, true, viewClock);
            }
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
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPreferences.PrefsApplyOptions options) {
            try {
                GuiSwingView.loadPreferencesDefault(this, prefs, options);
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

        @Override
        public void setKeyStrokeString(String keyStrokeString) {
            infoLabel.setAdditionalInfo(keyStrokeString);
        }

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
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
        private static final long serialVersionUID = 1L;
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
        private static final long serialVersionUID = 1L;
        protected DocumentSettingPane pane;
        protected JPanel contentPane;
        protected SettingsWindowClient editorPane;
        public DocumentSettingAction(JComponent label, SettingsWindowClient editorPane, DocumentSettingPane settingPane) {
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

        public DocumentSettingPane getPane() {
            return pane;
        }
    }

    public static class DocumentSettingPane extends JPanel implements ItemListener, ChangeListener,
            GuiSwingPreferences.Preferences {
        private static final long serialVersionUID = 1L;
        protected JEditorPane pane;
        protected Map<String, Font> nameFonts = new HashMap<>();

        protected JComboBox<String> fontFamily;
        protected JSpinner fontSize;
        protected JPopupMenu fontStyleMenu;
        protected JSpinner lineSpacing;

        protected StyleSetAction styleItalic;
        protected StyleSetAction styleBold;
        protected TextWrapTextAction wrapText;
        protected boolean updateDisabled;

        /** @since 1.2 */
        protected JCheckBox backgroundCustom;
        /** @since 1.2 */
        protected JCheckBox foregroundCustom;
        protected SettingsWindow.ColorButton backgroundColor;
        protected SettingsWindow.ColorButton foregroundColor;

        protected EditingRunner updater;
        protected Consumer<GuiSwingPreferences.PreferencesUpdateEvent> preferencesUpdater;

        protected int defaultFontSize = 14;

        public DocumentSettingPane(JEditorPane pane) {
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int top = ui.getScaledSizeInt(3);
            int size = ui.getScaledSizeInt(10);
            setBorder(BorderFactory.createEmptyBorder(top, size, size, size));
            this.pane = pane;

            Style s = getTargetStyle();
            boolean defaultBold = false;
            boolean defaultItalic = false;
            double defaultLineSpace = 1.0;
            if (s != null) {
                defaultFontSize = StyleConstants.getFontSize(s);
                defaultBold = StyleConstants.isBold(s);
                defaultItalic = StyleConstants.isItalic(s);
                defaultLineSpace = (double) StyleConstants.getLineSpacing(s);
            } else {
                Font font = ui.getEditorPaneFont();
                defaultFontSize = font.getSize();
            }
            //font name
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            fontFamily = new JComboBox<>(env.getAvailableFontFamilyNames());
            fontFamily.setRenderer(new DefaultListCellRenderer() {
                private static final long serialVersionUID = 1L;
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
            styleBold = new StyleSetAction("Bold", defaultBold, a -> updateStyle());
            styleItalic = new StyleSetAction("Italic", defaultItalic, a -> updateStyle());
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
            lineSpacing = new JSpinner(new SpinnerNumberModel(defaultLineSpace,
                    Short.MIN_VALUE, Short.MAX_VALUE, 0.1));
            lineSpacing.addChangeListener(this);

            updater = new EditingRunner(500, l -> updateStyle());

            //color
            JPanel backgroundColorPane = new JPanel();
            JPanel foregroundColorPane = new JPanel();
            backgroundCustom = new JCheckBox();
            foregroundCustom = new JCheckBox();
            backgroundCustom.addActionListener(e -> updateStyle());
            foregroundCustom.addActionListener(e -> updateStyle());
            backgroundColor = new SettingsWindow.ColorButton(getDefaultBackground(), updater::schedule);
            foregroundColor = new SettingsWindow.ColorButton(getDefaultForeground(), updater::schedule);
            backgroundColor.setEnabled(false);
            foregroundColor.setEnabled(false);

            backgroundColorPane.add(backgroundCustom);
            backgroundColorPane.add(backgroundColor);
            foregroundColorPane.add(foregroundCustom);
            foregroundColorPane.add(foregroundColor);

            new SettingsWindow.LabelGroup(this)
                .addRow("Font:", fontFamily)
                .addRow("Font Size:", fontSize)
                .addRow("Style:", styleButton)
                .addRow("Line Spacing:", lineSpacing)
                .addRowFixed("Font Color:", foregroundColorPane)
                .addRowFixed("Background:", backgroundColorPane)
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
                if (backgroundCustom.isSelected()) {
                    pane.setBackground(backgroundColor.getColor());
                } else {
                    pane.setBackground(getDefaultBackground());
                }
                backgroundColor.setEnabled(backgroundCustom.isSelected());
                if (foregroundCustom.isSelected()) {
                    StyleConstants.setForeground(style, foregroundColor.getColor());
                } else {
                    Color c = getDefaultForeground();
                    StyleConstants.setForeground(style, c);
                    pane.setForeground(c);
                }
                foregroundColor.setEnabled(foregroundCustom.isSelected());

                doc.setParagraphAttributes(0, doc.getLength(), style, true);
                sendPreferences();
            }
            pane.repaint();
        }

        /**
         * @return the default color for background
         * @since 1.2
         */
        protected Color getDefaultBackground() {
            return UIManagerUtil.getInstance().getTextPaneBackground();
        }

        /**
         * @return the default color for foreground
         * @since 1.2
         */
        protected Color getDefaultForeground() {
            return UIManagerUtil.getInstance().getTextPaneForeground();
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

                    Color foreground = StyleConstants.getForeground(style);
                    boolean foregroundFlag = !(foreground == null || Objects.equals(foreground, getDefaultForeground()));
                    foregroundCustom.setSelected(foregroundFlag);

                    Color background = StyleConstants.getForeground(style);
                    boolean backgroundFlag = !(background == null || Objects.equals(background, getDefaultBackground()));
                    backgroundCustom.setSelected(backgroundFlag);

                    foregroundColor.setEnabled(foregroundFlag);
                    backgroundColor.setEnabled(backgroundFlag);

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
                jsonSet(map, "backgroundCustom", Boolean.class, backgroundCustom::setSelected);
                jsonSet(map, "foregroundCustom", Boolean.class, foregroundCustom::setSelected);
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
            json.put("backgroundCustom", (Boolean) backgroundCustom.isSelected());
            json.put("foregroundCustom", (Boolean) foregroundCustom.isSelected());
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
                int g = list.size() >= 2 ? ((Number) list.get(1)).intValue() : 0;
                int b = list.size() >= 3 ? ((Number) list.get(2)).intValue() : 0;
                int a = list.size() >= 4 ? ((Number) list.get(3)).intValue() : 0;
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

        public JComboBox<String> getFontFamily() {
            return fontFamily;
        }

        public JSpinner getFontSize() {
            return fontSize;
        }

        public JPopupMenu getFontStyleMenu() {
            return fontStyleMenu;
        }

        public JSpinner getLineSpacing() {
            return lineSpacing;
        }

        public SettingsWindow.ColorButton getBackgroundColor() {
            return backgroundColor;
        }

        /**
         * @return the backgroundCustom checkbox
         * @since 1.2
         */
        public JCheckBox getBackgroundCustom() {
            return backgroundCustom;
        }

        /**
         * @return the foregroundCustom checkbox
         * @since 1.2
         */
        public JCheckBox getForegroundCustom() {
            return foregroundCustom;
        }

        public SettingsWindow.ColorButton getForegroundColor() {
            return foregroundColor;
        }

        public StyleSetAction getStyleItalic() {
            return styleItalic;
        }

        public StyleSetAction getStyleBold() {
            return styleBold;
        }

        public TextWrapTextAction getWrapText() {
            return wrapText;
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
        private static final long serialVersionUID = 1L;
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

        public void change(boolean v) {
            putValue(SELECTED_KEY, v);
            actionPerformed(null);
        }
    }


    ///////////////////

}
