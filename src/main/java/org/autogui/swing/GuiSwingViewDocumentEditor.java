package org.autogui.swing;

import org.autogui.base.JsonReader;
import org.autogui.base.JsonWriter;
import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.mapping.GuiReprValueDocumentEditor;
import org.autogui.swing.prefs.GuiSwingPrefsApplyOptions;
import org.autogui.swing.prefs.GuiSwingPrefsSupports;
import org.autogui.swing.util.*;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * a swing view for {@link GuiReprValueDocumentEditor}
 *
 * <h2>swing-value </h2>
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
 *             and calls {@link GuiSwingView.ValuePane#updateFromGui(ValuePane, Object, GuiTaskClock)} (if contextUpdate=true) </li>
 *      </ul>
 *
 * <h2>history-value</h2>
 * TODO unsupported yet
 *
 * <h2>string-transfer</h2>
 * TODO unsupported yet
 *
 * <h2>preferences</h2>
 * <pre>
 *     {
 *         "lineSpacing": Number,
 *         "spaceAbove": Number,
 *         "fontFamily": String,
 *         "fontSize": Number,
 *         "bold": Boolean,
 *         "italic": Boolean,
 *         "backgroundColor": "[intR, intG, intB, intA]",
 *         "foregroundColor": "[intR, intG, intB, intA]",
 *         "backgroundCustom": Boolean,
 *         "foregroundCustom": Boolean,
 *         "wrapText": Boolean
 *     }
 * </pre>
 */
@SuppressWarnings("this-escape")
public class GuiSwingViewDocumentEditor implements GuiSwingView {
    public GuiSwingViewDocumentEditor() {}
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        GuiReprValueDocumentEditor doc = (GuiReprValueDocumentEditor) context.getRepresentation();
        SpecifierManagerDefault specifierManager = GuiSwingView.specifierManager(parentSpecifier);
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
                @Serial private static final long serialVersionUID = 1L;
                final SelectionHighlightPainter painter = new SelectionHighlightPainter();
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
        Document doc = docEditor.toUpdateValue(context, newValue, delayedDoc ->
            setSwingViewValueDocument(pane, specifierManager, context, delayedDoc, delayedDoc, contextUpdate, viewClock));
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

    /**
     * an implementation for {@link JTextComponent#getPreferredSize()} supporting wrapping-line;
     *  checks the horizontal policy of parent {@link JScrollPane} by {@link #isWrapLine(JScrollPane)} and
     *   calls {@link #getPreferredSize(JTextComponent, boolean)}.
     *   A text-component can support wrapping-line by overriding {@link JTextComponent#getPreferredSize()} and
     *    {@link JTextComponent#getScrollableTracksViewportWidth()} with this method and {@link #getScrollableTracksViewportWidth(JTextComponent)}.
     * @param field the target text-component
     * @return the preferred-size suitable for the current wrapping-line setting
     * @see #getScrollableTracksViewportWidth(JTextComponent)
     * @since 1.7
     */
    public static Dimension getPreferredSizeWithParentScrollWrapLine(JTextComponent field) {
        return getPreferredSize(field, isWrapLine(scrollPane(field)));
    }

    /**
     * an implementation for {@link JTextComponent#getScrollableTracksViewportWidth()}
     * @param field the target text-component
     * @return true if {@link #isWrapLine(JScrollPane)} of the parent of the field
     * @since 1.7
     */
    public static boolean getScrollableTracksViewportWidth(JTextComponent field) {
        return isWrapLine(scrollPane(field));
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

    /**
     * the interface for the target pane of {@link DocumentSettingPane};
     *  if the pane implements the interface, it will be called at updating the wrapping-line property from the setting pane.
     * @since 1.7
     */
    public interface WrapLineSupport {
        void setWrapLine(boolean wrapLine);
    }

    public static class PropertyDocumentEditorPane extends JEditorPane
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object>,
                SettingsWindowClient, WrapLineSupport { //ValuePane<StringBuilder|Content|Document>
        @Serial private static final long serialVersionUID = 1L;
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
                SwingDeferredRunner.invokeLater(() ->
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
            GuiSwingView.updateViewClockSync(viewClock, context);
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

        @Override
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
            SettingsWindowClient, GuiSwingPrefsSupports.PreferencesUpdateSupport, WrapLineSupport { //ValuePane<StringBuilder|Content|Document>
        @Serial private static final long serialVersionUID = 1L;
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
                SwingDeferredRunner.invokeLater(() ->
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
            GuiSwingView.updateViewClockSync(viewClock, context);
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

        @Override
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
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            try {
                options.begin(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
                GuiSwingView.loadPreferencesDefault(this, prefs, options);
                GuiPreferences targetPrefs = prefs.getDescendant(context);
                options.loadFrom(settingPane, targetPrefs);
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
                options.end(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
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
        public void setPreferencesUpdater(Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> updater) {
            settingPane.setPreferencesUpdaterEventListener(updater);
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

    public static class DocumentSettingAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
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

    /**
     * a setting pane for document default stype; a GUI for {@link PreferencesForDocumentSetting}
     */
    public static class DocumentSettingPane extends JPanel implements GuiSwingPrefsSupports.Preferences {
        @Serial private static final long serialVersionUID = 1L;
        protected JEditorPane pane;
        protected JComboBox<String> fontFamily;
        protected JToggleButton styleBold;
        protected JToggleButton styleItalic;
        protected JToggleButton styleWrapLine;
        protected JSpinner fontSize;
        protected JSpinner spaceAbove;
        protected JSpinner spaceLine;

        /** @since 1.2 */
        protected JCheckBox foregroundCustom;
        protected SettingsWindow.ColorButton foregroundColor;
        /** @since 1.2 */
        protected JCheckBox backgroundCustom;
        protected SettingsWindow.ColorButton backgroundColor;

        protected boolean updateDisabled;
        protected Consumer<PreferencesForDocumentSetting> preferencesUpdater;
        protected GuiSwingViewDocumentEditor.PreferencesForDocumentSetting prefsObj;

        public DocumentSettingPane(JEditorPane pane) {
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
            var styleSize = new Dimension(ui.getScaledSizeInt(28), ui.getScaledSizeInt(28));

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
            fontSize = new JSpinner(new SpinnerNumberModel(12, -1, 400, 1));
        }

        public void initSpace() {
            UIManagerUtil ui = UIManagerUtil.getInstance();
            var size = new Dimension(ui.getScaledSizeInt(72), ui.getScaledSizeInt(22));

            spaceLine = new JSpinner(new SpinnerNumberModel(0.5f, -1f, 5f, 0.05f));
            spaceLine.setToolTipText("Space factor between lines");
            spaceLine.setPreferredSize(size);

            spaceAbove = new JSpinner(new SpinnerNumberModel(0.0f, -1f, 1000f, 1f));
            spaceAbove.setToolTipText("Space points above paragraphs");
            spaceAbove.setPreferredSize(size);
        }

        public void initColor() {
            backgroundCustom = new JCheckBox();
            backgroundCustom.setToolTipText("Enable the custom background color");
            backgroundColor = new SettingsWindow.ColorButton(Color.white, c -> updateTargetTextPaneAndPrefsObjFromGui());
            backgroundColor.setToolTipText("Background color");

            foregroundCustom = new JCheckBox();
            foregroundCustom.setToolTipText("Enable the custom foreground color");
            foregroundColor = new SettingsWindow.ColorButton(Color.black, c -> updateTargetTextPaneAndPrefsObjFromGui());
            foregroundColor.setToolTipText("Foreground color");
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
         * initialize {@link PreferencesForDocumentSetting} with default (non-null) values;
         *  also if it has a target text-pane, calls {@link PreferencesForDocumentSetting#setup(JComponent)}
         *    for reading from the pane settings and set those values to the prefsObj.
         *   The GUI of the setting-pane is updated by the prefsObj
         */
        public void initPrefsObj() {
            prefsObj = new GuiSwingViewDocumentEditor.PreferencesForDocumentSetting();
            prefsObj.setUiDefault(); //set default (non-null) props
            if (pane != null) {
                prefsObj.setup(pane); //set props only set by the pane
            }
            setFrom(prefsObj); //update settings UI
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
                prefsObj.applyTo(pane);
                pane.repaint();
            }
        }

        /**
         * update the prefsObj by current GUI-settings props
         * @param prefsObj the updated prefsObj
         */
        public void setTo(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting prefsObj) {
            var data = prefsObj.getData();
            data.put(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_LINE_SPACING, ((Number) spaceLine.getValue()).floatValue());
            data.put(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_SPACE_ABOVE, ((Number) spaceAbove.getValue()).floatValue());
            data.put(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_FONT_FAMILY, fontFamily.getSelectedItem());
            data.put(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_FONT_SIZE, ((Number) fontSize.getValue()).intValue());
            data.put(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_BOLD, styleBold.isSelected());
            data.put(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_ITALIC, styleItalic.isSelected());
            data.put(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_BACKGROUND_COLOR, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.toJsonColor(backgroundColor.getColor()));
            data.put(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_FOREGROUND_COLOR, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.toJsonColor(foregroundColor.getColor()));
            data.put(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_BACKGROUND_CUSTOM, backgroundCustom.isSelected());
            data.put(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_FOREGROUND_CUSTOM, foregroundCustom.isSelected());
            data.put(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_WRAP_TEXT, styleWrapLine.isSelected());
        }

        /**
         * update the GUI-settings props by reading the prefsObj
         * @param prefsObj the source prefsObj
         */
        public void setFrom(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting prefsObj) {
            var data = prefsObj.getData();
            var lineSpacingVal = GuiSwingPrefsSupports.getAs(data, Number.class, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_LINE_SPACING, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.DEFAULT_NUM);
            var spaceAboveVal = GuiSwingPrefsSupports.getAs(data, Number.class, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_SPACE_ABOVE, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.DEFAULT_NUM);
            var fontFamilyVal = GuiSwingPrefsSupports.getAs(data, String.class, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_FONT_FAMILY, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.DEFAULT_FONT_FAMILY);
            var fontSizeVal = GuiSwingPrefsSupports.getAs(data, Number.class, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_FONT_SIZE, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.DEFAULT_NUM);
            var boldVal = GuiSwingPrefsSupports.getAs(data, Boolean.class, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_BOLD, false);
            var italicVal = GuiSwingPrefsSupports.getAs(data, Boolean.class, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_ITALIC, false);
            var wrapTextVal = GuiSwingPrefsSupports.getAs(data, Boolean.class, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_WRAP_TEXT, true);
            var backgroundCustomVal = GuiSwingPrefsSupports.getAs(data, Boolean.class, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_BACKGROUND_CUSTOM, false);
            var foregroundCustomVal = GuiSwingPrefsSupports.getAs(data, Boolean.class, GuiSwingViewDocumentEditor.PreferencesForDocumentSetting.KEY_FOREGROUND_CUSTOM, false);
            var backgroundColorVal = prefsObj.backgroundColor();
            var foregroundColorVal = prefsObj.foregroundColor();
            spaceLine.setValue(lineSpacingVal.floatValue());
            spaceAbove.setValue(spaceAboveVal.floatValue());
            fontFamily.setSelectedItem(fontFamilyVal);
            fontSize.setValue(fontSizeVal.intValue());
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
        }

        public void setPreferencesUpdaterEventListener(Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> eventListener) {
            if (eventListener == null) {
                this.preferencesUpdater = null;
            } else {
                this.preferencesUpdater = preferencesUpdaterForEventListener(pane, eventListener);
            }
        }

        public void setPreferencesUpdater(Consumer<PreferencesForDocumentSetting> updater) {
            this.preferencesUpdater = updater;
        }

        public static Consumer<PreferencesForDocumentSetting> preferencesUpdaterForEventListener(JComponent pane, Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> eventListener) {
            return (prefsObj) -> {
                if (pane instanceof GuiSwingView.ValuePane<?> valuePane && eventListener != null) {
                    eventListener.accept(
                            new GuiSwingPrefsSupports.PreferencesUpdateEvent(valuePane.getSwingViewContext(), prefsObj));
                }
            };
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


        @Override
        public void loadFrom(GuiPreferences prefs) {
            prefsObj.loadFrom(prefs);
            updateGuiAndTargetTextPaneFromPrefsObj();
        }

        @Override
        public void saveTo(GuiPreferences prefs) {
            prefsObj.saveTo(prefs);
        }

        public JEditorPane getPane() {
            return pane;
        }

        public PreferencesForDocumentSetting getPrefsObj() {
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
     * @since 1.7
     */
    public static class PreferencesForDocumentSetting implements GuiSwingPrefsSupports.Preferences {
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
        protected Map<String, Object> data = new LinkedHashMap<>();

        public static final String DEFAULT_FONT_FAMILY = "";
        public static final Number DEFAULT_NUM = -1;

        /**
         * initialized by null-values and black and white colors.
         */
        public PreferencesForDocumentSetting() {
            data.put(KEY_LINE_SPACING, DEFAULT_NUM);
            data.put(KEY_SPACE_ABOVE, DEFAULT_NUM);
            data.put(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY);
            data.put(KEY_FONT_SIZE, DEFAULT_NUM);
            data.put(KEY_BOLD, false);
            data.put(KEY_ITALIC, false);
            data.put(KEY_BACKGROUND_COLOR, toJsonColor(255, 255, 255, 255));
            data.put(KEY_FOREGROUND_COLOR, toJsonColor(0, 0, 0, 255));
            data.put(KEY_BACKGROUND_CUSTOM, false);
            data.put(KEY_FOREGROUND_CUSTOM, false);
            data.put(KEY_WRAP_TEXT, true);
        }

        /**
         * overwrites properties by values from {@link UIManagerUtil};
         *  also {@code lineSpacing} is 0.2, {@code spaceAbove} is 2.0, no background and foregorund custom,
         *    and {@code wrapText} is true.
         */
        public void setUiDefault() {
            var ui = UIManagerUtil.getInstance();
            var font = ui.getEditorPaneFont();
            data.put(KEY_LINE_SPACING, ui.getScaledSizeFloat(0.2f));
            data.put(KEY_SPACE_ABOVE, ui.getScaledSizeFloat(2.0f));
            data.put(KEY_FONT_FAMILY, font.getFamily());
            data.put(KEY_FONT_SIZE, font.getSize());
            data.put(KEY_BOLD, font.isBold());
            data.put(KEY_ITALIC, font.isItalic());
            data.put(KEY_BACKGROUND_COLOR, toJsonColor(ui.getTextPaneBackground()));
            data.put(KEY_FOREGROUND_COLOR, toJsonColor(ui.getTextPaneForeground()));
            data.put(KEY_BACKGROUND_CUSTOM, false);
            data.put(KEY_FOREGROUND_CUSTOM, false);
            data.put(KEY_WRAP_TEXT, true);
        }

        public static Object toJsonColor(Color c) {
            return toJsonColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        }

        public static Object toJsonColor(int r, int g, int b, int a) {
            return new ArrayList<>(Arrays.asList(r, g, b, a));
        }

        public static Color fromJsonColor(Object o) {
            if (o != null) {
                List<?> list = (List<?>) o;
                int r = !list.isEmpty() ? ((Number) list.get(0)).intValue() : 0;
                int g = list.size() >= 2 ? ((Number) list.get(1)).intValue() : 0;
                int b = list.size() >= 3 ? ((Number) list.get(2)).intValue() : 0;
                int a = list.size() >= 4 ? ((Number) list.get(3)).intValue() : 0;
                return new Color(r, g, b, a);
            } else {
                return null;
            }
        }

        /**
         * @return the direct reference to the properties
         */
        public Map<String, Object> getData() {
            return data;
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
            var backgroundColor = (Color) style.getAttribute(StyleConstants.Background);
            var foregroundColor = (Color) style.getAttribute(StyleConstants.Foreground);
            data.put(KEY_LINE_SPACING, lineSpacing == null ? DEFAULT_NUM : lineSpacing);
            data.put(KEY_SPACE_ABOVE, spaceAbove == null ? DEFAULT_NUM : spaceAbove);
            data.put(KEY_FONT_FAMILY, fontFamily == null ? DEFAULT_FONT_FAMILY : fontFamily);
            data.put(KEY_FONT_SIZE, fontSize == null ? DEFAULT_NUM : fontSize);
            data.put(KEY_BOLD, bold == null ? false : bold);
            data.put(KEY_ITALIC, italic == null ? false : italic);
            data.put(KEY_BACKGROUND_CUSTOM, backgroundColor != null);
            data.put(KEY_FOREGROUND_CUSTOM, foregroundColor != null);
            data.put(KEY_BACKGROUND_COLOR, backgroundColor == null ? DEFAULT_NUM : toJsonColor(backgroundColor));
            data.put(KEY_FOREGROUND_COLOR, foregroundColor == null ? DEFAULT_NUM : toJsonColor(foregroundColor));
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
            data.put(KEY_WRAP_TEXT, wrapScroll);
        }

        /**
         * set properties from the given style; only the properties the style have,
         *  and {@link #applyTo(MutableAttributeSet)} to reflect the all propertis of the prefs to the target-pane
         * @param style the source style
         */
        public void setup(MutableAttributeSet style) {
            var lineSpacing = style.getAttribute(StyleConstants.LineSpacing);
            var spaceAbove = style.getAttribute(StyleConstants.SpaceAbove);
            var fontFamily = style.getAttribute(StyleConstants.FontFamily);
            var fontSize = style.getAttribute(StyleConstants.FontSize);
            var bold = style.getAttribute(StyleConstants.Bold);
            var italic = style.getAttribute(StyleConstants.Italic);
            var backgroundColor = (Color) style.getAttribute(StyleConstants.Background);
            var foregroundColor = (Color) style.getAttribute(StyleConstants.Foreground);
            if (lineSpacing != null) { data.put(KEY_LINE_SPACING, lineSpacing); }
            if (spaceAbove != null) { data.put(KEY_SPACE_ABOVE, spaceAbove); }
            if (fontFamily != null) { data.put(KEY_FONT_FAMILY, fontFamily); }
            if (fontSize != null) { data.put(KEY_FONT_SIZE, fontSize); }
            if (bold != null) { data.put(KEY_BOLD, bold); }
            if (italic != null) { data.put(KEY_ITALIC, italic); }
            data.put(KEY_BACKGROUND_CUSTOM, (backgroundColor != null));
            data.put(KEY_FOREGROUND_CUSTOM, (foregroundColor != null));
            if (backgroundColor != null) { data.put(KEY_BACKGROUND_COLOR, toJsonColor(backgroundColor)); }
            if (foregroundColor != null) { data.put(KEY_FOREGROUND_COLOR, toJsonColor(foregroundColor)); }
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
            var style = getTargetStyle(pane);
            if (style != null) {
                setup(style);
                applyToDocument(getTargetDocument(pane), style);
            }
            var wrapScroll = isWrapLine(scrollPane(pane));
            data.put(KEY_WRAP_TEXT, wrapScroll);
            applyToComponentWithoutDocument(pane);
        }

        /**
         * update the target style by properties of the prefs, or if remove attributes by null-values
         * @param style the modified style
         */
        public void applyTo(MutableAttributeSet style) {
            var lineSpacing = GuiSwingPrefsSupports.getAs(data, Number.class, KEY_LINE_SPACING, DEFAULT_NUM);
            var spaceAbove = GuiSwingPrefsSupports.getAs(data, Number.class, KEY_SPACE_ABOVE, DEFAULT_NUM);
            var fontFamily = GuiSwingPrefsSupports.getAs(data, String.class, KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY);
            var fontSize = GuiSwingPrefsSupports.getAs(data, Number.class, KEY_FONT_SIZE, DEFAULT_NUM);
            var bold = GuiSwingPrefsSupports.getAs(data, Boolean.class, KEY_BOLD, false);
            var italic = GuiSwingPrefsSupports.getAs(data, Boolean.class, KEY_ITALIC, false);
            var wrapText = GuiSwingPrefsSupports.getAs(data, Boolean.class, KEY_WRAP_TEXT, true);
            var backgroundCustom = GuiSwingPrefsSupports.getAs(data, Boolean.class, KEY_BACKGROUND_CUSTOM, false);
            var foregroundCustom = GuiSwingPrefsSupports.getAs(data, Boolean.class, KEY_FOREGROUND_CUSTOM, false);
            var backgroundColor = backgroundColor();
            var foregroundColor = foregroundColor();

            if (lineSpacing.equals(DEFAULT_NUM)) {
                style.removeAttribute(StyleConstants.LineSpacing);
            } else {
                StyleConstants.setLineSpacing(style, lineSpacing.floatValue());
            }
            if (spaceAbove.equals(DEFAULT_NUM)) {
                style.removeAttribute(StyleConstants.SpaceAbove);
            } else {
                StyleConstants.setSpaceAbove(style, spaceAbove.floatValue());
            }
            if (fontFamily.equals(DEFAULT_FONT_FAMILY)) {
                style.removeAttribute(StyleConstants.FontFamily);
            } else {
                StyleConstants.setFontFamily(style, fontFamily);
            }
            if (fontSize.equals(DEFAULT_NUM)) {
                style.removeAttribute(StyleConstants.FontSize);
            } else {
                StyleConstants.setFontSize(style, fontSize.intValue());
            }
            StyleConstants.setBold(style, bold);
            StyleConstants.setItalic(style, italic);
            if (backgroundColor == null) {
                style.removeAttribute(StyleConstants.Background);
            } else {
                StyleConstants.setBackground(style, backgroundColor);
            }
            if (foregroundColor == null) {
                style.removeAttribute(StyleConstants.Foreground);
            } else {
                StyleConstants.setForeground(style, foregroundColor);
            }
        }

        /**
         * @return the {@code backgroundColor}-property if the prop is set and {@code backgroundCustom} is true, or null
         */
        public Color backgroundColor() {
            var backgroundCustom = GuiSwingPrefsSupports.getAs(data, Boolean.class, KEY_BACKGROUND_CUSTOM, false);
            var backfgroundColor = fromJsonColor(GuiSwingPrefsSupports.getAsListNonNull(data, Number.class, KEY_BACKGROUND_COLOR));
            if (backgroundCustom && backfgroundColor != null) {
                return backfgroundColor;
            } else {
                return null;
            }
        }

        /**
         * @return the {@code foregroundColor}-property if the prop is set and {@code foregroundCustom} is true, or null
         */
        public Color foregroundColor() {
            var foregroundCustom = GuiSwingPrefsSupports.getAs(data, Boolean.class, KEY_FOREGROUND_CUSTOM, false);
            var foregroundColor = fromJsonColor(GuiSwingPrefsSupports.getAsListNonNull(data, Number.class, KEY_FOREGROUND_COLOR));
            if (foregroundCustom && foregroundColor != null) {
                return foregroundColor;
            } else {
                return null;
            }
        }

        /**
         * set style of the target-text-pane by {@link #getTargetStyle(JComponent)} and {@link #applyTo(MutableAttributeSet)}
         *  ,{@link #applyToDocument(StyledDocument, MutableAttributeSet)} and {@link #applyToComponentWithoutDocument(JComponent)}.
         * @param pane the target text-pane
         */
        public void applyTo(JComponent pane) {
            var style = getTargetStyle(pane);
            if (style != null) {
                applyTo(style);
                applyToDocument(getTargetDocument(pane), style);
            }
            applyToComponentWithoutDocument(pane);
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
         *  and wrapText by {@link #setWrapLine(JScrollPane, JComponent, boolean)}
         * @param pane the target text-pane
         */
        public void applyToComponentWithoutDocument(JComponent pane) {
            var backfgroundColor = backgroundColor();
            var foregroundColor = foregroundColor();
            pane.setBackground(backfgroundColor);
            pane.setForeground(foregroundColor);
            var wrapLine = GuiSwingPrefsSupports.getAs(data, Boolean.class, KEY_WRAP_TEXT, true);
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

        public boolean isWrapLineNotMatch(JScrollPane pane, boolean tested) {
            return GuiSwingViewDocumentEditor.isWrapLineNotMatch(pane, tested);
        }

        /**
         * if parentScroll is non-null and {@link #isWrapLineNotMatch(JScrollPane, boolean)} with f,
         *  changes the wrapping-line by {@link GuiSwingViewDocumentEditor#setWrapLine(JScrollPane, boolean)}.
         *  Also if the field is a {@link WrapLineSupport}, call {@link WrapLineSupport#setWrapLine(boolean)}
         * @param parentScroll the parent pane of the field
         * @param field the target text-component
         * @param f the new flag value of {@code wrapText}
         */
        public void setWrapLine(JScrollPane parentScroll, JComponent field, boolean f) {
            if (parentScroll != null && isWrapLineNotMatch(parentScroll, f)) {
                GuiSwingViewDocumentEditor.setWrapLine(parentScroll, f);
                if (field instanceof WrapLineSupport editorPane) {
                    editorPane.setWrapLine(f);
                }
            }
        }

        @Override
        public void loadFrom(GuiPreferences prefs) {
            GuiPreferences.GuiValueStore store = prefs.getValueStore();
            var json = getData();
            for (Map.Entry<String,Object> e : json.entrySet()) { //JSON has all keys and values
                String k = e.getKey();
                String storeVal = store.getString(k, "");
                Object exVal = e.getValue();
                if (!storeVal.isEmpty()) {
                    switch (exVal) {
                        case String s -> json.put(k, storeVal);
                        case Boolean b -> json.put(k, storeVal.endsWith("true"));
                        case Number number -> {
                            try {
                                json.put(k, Integer.valueOf(storeVal));
                            } catch (Exception ex) {
                                json.put(k, Float.valueOf(storeVal));
                            }
                        }
                        case null, default -> json.put(k, JsonReader.create(storeVal).parseValue());
                    }
                }
            }
        }

        @Override
        public void saveTo(GuiPreferences prefs) {
            GuiPreferences.GuiValueStore store = prefs.getValueStore();
            for (Map.Entry<String,Object> e : getData().entrySet()) {
                String k = e.getKey();
                Object v = e.getValue();
                switch (v) {
                    case String s -> store.putString(k, s);
                    case Number number -> store.putString(k, v.toString());
                    case Boolean b -> store.putString(k, v.equals(Boolean.TRUE) ? "true" : "false");
                    case null, default -> store.putString(k, JsonWriter.create()
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
        @Serial private static final long serialVersionUID = 1L;
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

}
