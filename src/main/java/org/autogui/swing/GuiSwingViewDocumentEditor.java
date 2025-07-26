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
        if (pane instanceof PropertyDocumentTextPane textPane) {
            settingActions = Collections.singletonList(textPane.getSettingAction());
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
        protected DocumentSettingAction settingAction;

        public PropertyDocumentTextPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            settingPane = new DocumentSettingPane(this);
            settingAction = new DocumentSettingAction(GuiSwingContextInfo.get().getInfoLabel(context),
                    this, settingPane);
            popup = new TextPaneInitializer(this, context).getPopup();
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
        }

        public DocumentSettingAction getSettingAction() {
            return settingAction;
        }

        @Override
        public void setDocument(Document doc) {
            if (lineNumberPane != null) {
                lineNumberPane.uninstall();
            }
            super.setDocument(doc);
            installLineNumberPane();
            if (settingPane != null) {
                settingPane.updateGuiAndTargetTextPaneFromPrefsObj();
            }
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

        public DocumentSettingPane getPane() {
            return pane;
        }
    }

    public static final double DOCUMENT_SETTING_PANE_SPACE_ABOBE_MIN = -1f;
    public static final double DOCUMENT_SETTING_PANE_SPACE_ABOBE_MAX = 1000f;
    public static final double DOCUMENT_SETTING_PANE_SPACE_LINE_MIN = -1f;
    public static final double DOCUMENT_SETTING_PANE_SPACE_LINE_MAX = 5f;
    public static final int DOCUMENT_SETTING_PANE_FONT_SIZE_MIN = -1;
    public static final int DOCUMENT_SETTING_PANE_FONT_SIZE_MAX = 400;

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
         * initialize {@link PreferencesForDocumentSetting} with default (non-null) values;
         *  also if it has a target text-pane, calls {@link PreferencesForDocumentSetting#setup(JComponent)}
         *    for reading from the pane settings and set those values to the prefsObj.
         *   The GUI of the setting-pane is updated by the prefsObj
         */
        public void initPrefsObj() {
            prefsObj = new GuiSwingViewDocumentEditor.PreferencesForDocumentSetting(pane);
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
        public void setFrom(GuiSwingViewDocumentEditor.PreferencesForDocumentSetting prefsObj) {
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
                System.err.println(ex);
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
        public boolean loadFromAndChanged(GuiPreferences prefs) {
            if (prefsObj.loadFromAndChanged(prefs)) {
                updateGuiAndTargetTextPaneFromPrefsObj();
                return true;
            } else {
                return false;
            }
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
        public PreferencesForDocumentSetting() {
            this(null);
        }

        /**
         * initialize with the target pane (or if null,  it sets default null values).
         * it has 2 modes,
         *    1. the property mode with no pane which allows null values for representing saved prefs values,
         *    2. the practical mode with target pane which (basically) does not allow null-values, instead, it merges actual pane's value if null-values arrived.
         * @param pane the target text-pane or null
         */
        public PreferencesForDocumentSetting(JEditorPane pane) {
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
                this.defaultBackground = pane.getBackground();
                this.defaultForeground = pane.getForeground();
                allowNullValues = false;
            } else {
                allowNullValues = true;
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
            setBackgroundCustom(backgroundColor != null);
            setForegroundCustom(foregroundColor != null);
            setBackgroundColor(styleValue(Color.class, backgroundColor, null));
            setForegroundColor(styleValue(Color.class, foregroundColor, null));
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
        public boolean loadFromAndChanged(GuiPreferences prefs) {
            GuiPreferences.GuiValueStore store = prefs.getValueStore();
            var lineSpacing = store.getString(KEY_LINE_SPACING, null);
            var spaceAbove = store.getString(KEY_SPACE_ABOVE, null);
            var fontFamily = store.getString(KEY_FONT_FAMILY, null);
            var fontSize = store.getString(KEY_FONT_SIZE, null);
            var bold = store.getString(KEY_BOLD, null);
            var italic = store.getString(KEY_ITALIC, null);
            var backgroundColor = fromJsonColorSource(store.getString(KEY_BACKGROUND_COLOR, null));
            var foregroundColor = fromJsonColorSource(store.getString(KEY_FOREGROUND_COLOR, null));
            var backgroundCustom = store.getString(KEY_BACKGROUND_CUSTOM, null);
            var foregroundCustom = store.getString(KEY_FOREGROUND_CUSTOM, null);
            var wrapText = store.getString(KEY_WRAP_TEXT, null);
            boolean updateLineSpacing = updateProp(this::setLineSpacing, getLineSpacing(), (lineSpacing != null ?  Float.parseFloat(lineSpacing) :  DEFAULT_NUM));
            boolean updateSpaceAbove = updateProp(this::setSpaceAbove, getSpaceAbove(), (spaceAbove != null ?  Float.parseFloat(spaceAbove) : DEFAULT_NUM));
            boolean updateFontFamily = updateProp(this::setFontFamily, getFontFamily(), (fontFamily != null ?  fontFamily : DEFAULT_FONT_FAMILY));
            boolean updateFontSize = updateProp(this::setFontSize, getFontSize(), (fontSize != null ?  Integer.parseInt(fontSize) : DEFAULT_NUM));
            boolean updateBold = updateProp(this::setBold, isBold(), (bold != null && bold.equals("true")));
            boolean updateItalic = updateProp(this::setItalic, isItalic(), (italic != null && italic.equals("true")));
            boolean updateWrapText = updateProp(this::setWrapText, isWrapText(), (wrapText != null && wrapText.equals("true")));
            boolean updateBackgroundCustom = updateProp(this::setBackgroundCustom, isBackgroundCustom(), (backgroundCustom != null && backgroundCustom.equals("true")));
            boolean updateForegroundCustom = updateProp(this::setForegroundCustom, isForegroundCustom(), (foregroundCustom != null && foregroundCustom.equals("true")));
            boolean updateBackgroundColor = updateProp(this::setBackgroundColor, getBackgroundColor(), (backgroundColor));
            boolean updateForegroundColor = updateProp(this::setForegroundColor, getForegroundColor(), (foregroundColor));
            return updateLineSpacing || updateSpaceAbove || updateFontFamily || updateFontSize || updateBold || updateItalic ||
                    updateWrapText || updateBackgroundCustom || updateForegroundCustom || updateBackgroundColor || updateForegroundColor;
        }

        private <T> boolean updateProp(Consumer<T> setter, T current, T next) {
            if (!Objects.equals(current, next)) {
                setter.accept(next);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void saveTo(GuiPreferences prefs) {
            GuiPreferences.GuiValueStore store = prefs.getValueStore();
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
            if (lineSpacing != DEFAULT_NUM) { store.putString(KEY_LINE_SPACING, Float.toString(lineSpacing)); } else { store.remove(KEY_LINE_SPACING); }
            if (spaceAbove != DEFAULT_NUM) { store.putString(KEY_SPACE_ABOVE, Float.toString(spaceAbove)); } else { store.remove(KEY_SPACE_ABOVE); }
            if (fontFamily != null && !Objects.equals(fontFamily, DEFAULT_FONT_FAMILY)) { store.putString(KEY_FONT_FAMILY, fontFamily); } else { store.remove(KEY_FONT_FAMILY); }
            if (fontSize != DEFAULT_NUM) { store.putString(KEY_FONT_SIZE, Integer.toString(fontSize)); } else { store.remove(KEY_FONT_SIZE); }
            store.putString(KEY_BOLD, Boolean.toString(bold));
            store.putString(KEY_ITALIC, Boolean.toString(italic));
            store.putString(KEY_BACKGROUND_CUSTOM, Boolean.toString(backgroundCustom));
            store.putString(KEY_FOREGROUND_CUSTOM, Boolean.toString(foregroundCustom));
            store.putString(KEY_WRAP_TEXT, Boolean.toString(wrapText));
            if (backgroundColor != null) { store.putString(KEY_BACKGROUND_COLOR, JsonWriter.create().withNewLines(false).write(toJsonColor(backgroundColor)).toSource()); } else { store.remove(KEY_BACKGROUND_COLOR); }
            if (foregroundColor != null) { store.putString(KEY_FOREGROUND_COLOR, JsonWriter.create().withNewLines(false).write(toJsonColor(foregroundColor)).toSource()); } else { store.remove(KEY_FOREGROUND_COLOR); }
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
