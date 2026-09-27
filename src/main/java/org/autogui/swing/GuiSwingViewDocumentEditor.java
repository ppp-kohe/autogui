package org.autogui.swing;

import org.autogui.GuiIncluded;
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

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.Serial;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
 *             and calls {@link GuiSwingView#updateFromGui(ValuePane, Object, GuiTaskClock)} (if contextUpdate=true) </li>
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

    public static class TextPaneInitializer extends TextPaneBuilder {
        protected GuiMappingContext context;

        public TextPaneInitializer(JEditorPane pane, GuiMappingContext context) {
            super(pane);
            this.context = context;
        }

        @Override
        public TextPaneInitializer init() {
            initName();
            initContextUpdate();
            initValue();
            super.init();
            return this;
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

        @GuiIncluded
        public void initPopup() {
            super.initPopup();
            if (pane instanceof ValuePane<?>) {
                GuiSwingView.setupKeyBindingsForStaticMenuItems((ValuePane<?>) pane);
            }
        }

        @Override
        protected DocumentSettingPane createSettingPane() {
            return new DocumentSettingPane(pane);
        }

        @Override
        public DocumentSettingPane getSettingPane() {
            return (DocumentSettingPane) super.getSettingPane();
        }

        @Override
        protected PopupExtension.PopupMenuBuilder createPopupMenuBuilder() {
            if (pane instanceof ValuePane<?> vPane) {
                return new PopupCategorized(vPane::getSwingStaticMenuItems);
            } else {
                return super.createPopupMenuBuilder();
            }
        }

        @Override
        protected SettingsWindow.SettingsWindowClientBase getSettingsWindowClient() {
            if (pane instanceof SettingsWindow.SettingsWindowClientBase client) {
                return client;
            } else {
                return super.getSettingsWindowClient();
            }
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
     *  checks the horizontal policy of parent {@link JScrollPane} by {@link TextPaneBuilder#isWrapLine(JScrollPane)} and
     *   calls {@link TextPaneBuilder#getPreferredSize(JTextComponent, boolean)}.
     *   A text-component can support wrapping-line by overriding {@link JTextComponent#getPreferredSize()} and
     *    {@link JTextComponent#getScrollableTracksViewportWidth()} with this method and {@link #getScrollableTracksViewportWidth(JTextComponent)}.
     * @param field the target text-component
     * @return the preferred-size suitable for the current wrapping-line setting
     * @see #getScrollableTracksViewportWidth(JTextComponent)
     * @since 1.7
     */
    public static Dimension getPreferredSizeWithParentScrollWrapLine(JTextComponent field) {
        return TextPaneBuilder.getPreferredSize(field, TextPaneBuilder.isWrapLine(TextPaneBuilder.scrollPane(field)));
    }

    /**
     * an implementation for {@link JTextComponent#getScrollableTracksViewportWidth()}
     * @param field the target text-component
     * @return true if {@link TextPaneBuilder#isWrapLine(JScrollPane)} of the parent of the field
     * @since 1.7
     */
    public static boolean getScrollableTracksViewportWidth(JTextComponent field) {
        return TextPaneBuilder.isWrapLine(TextPaneBuilder.scrollPane(field));
    }

    public static class PropertyDocumentEditorPane extends JEditorPane
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object>,
                SettingsWindowClient, TextPaneBuilder.WrapLineSupport { //ValuePane<StringBuilder|Content|Document>
        @Serial private static final long serialVersionUID = 1L;
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected PopupExtension popup;
        protected boolean wrapLine = true;
        protected SettingsWindow settingsWindow;
        protected MenuBuilder.MenuLabel infoLabel;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);

        public PropertyDocumentEditorPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            popup = new TextPaneInitializer(this, context) {
                @Override
                protected LineNumberPane createLineNumberPane(JEditorPane pane) {
                    return PropertyDocumentEditorPane.this.createLineNumberPane();
                }
            }.init().getPopup();
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
        }

        protected LineNumberPane createLineNumberPane() {
            var p = new LineNumberPane(this);
            p.install();
            return p;
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
                return TextPaneBuilder.getPreferredSize(this, wrapLine);
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
            SettingsWindowClient, GuiSwingPrefsSupports.PreferencesUpdateSupport, TextPaneBuilder.WrapLineSupport { //ValuePane<StringBuilder|Content|Document>
        @Serial private static final long serialVersionUID = 1L;
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected PopupExtension popup;
        protected boolean wrapLine = true;
        protected DocumentSettingPane settingPane;
        protected SettingsWindow settingsWindow;
        protected MenuBuilder.MenuLabel infoLabel;
        protected GuiTaskClock viewClock = new GuiTaskClock(true);
        protected TextPaneBuilder.DocumentSettingAction settingAction;

        public PropertyDocumentTextPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            var init = new TextPaneInitializer(this, context).init();
            settingPane = init.getSettingPane();
            settingAction = init.getSettingAction();
            popup = init.getPopup();
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
        }

        public TextPaneBuilder.DocumentSettingAction getSettingAction() {
            return settingAction;
        }

        protected LineNumberPane createLineNumberPane() {
            var p = new LineNumberPane(this);
            p.install();
            return p;
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
                return TextPaneBuilder.getPreferredSize(this, wrapLine);
            } catch (Exception ex) {
                return getSize();
            }
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        public TextPaneBuilder.DocumentSettingPaneBase getSettingPane() {
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

    /**
     * setting pane
     */
    public static class DocumentSettingPane extends TextPaneBuilder.DocumentSettingPaneBase implements GuiSwingPrefsSupports.Preferences {
        public DocumentSettingPane(JEditorPane pane) {
            super(pane);
        }

        @Override
        protected PreferencesForDocumentSetting createPfersObj() {
            return new PreferencesForDocumentSetting(pane);
        }

        @Override
        public PreferencesForDocumentSetting getPrefsObj() {
            return (PreferencesForDocumentSetting) super.getPrefsObj();
        }

        public void setPreferencesUpdaterEventListener(Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> eventListener) {
            if (eventListener == null) {
                setPreferencesUpdater(null);
            } else {
                setPreferencesUpdater(preferencesUpdaterForEventListener(pane, eventListener));
            }
        }

        public static Consumer<TextPaneBuilder.PreferencesForDocumentSettingBase> preferencesUpdaterForEventListener(JComponent pane, Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> eventListener) {
            return (prefsObj) -> {
                if (pane instanceof GuiSwingView.ValuePane<?> valuePane && eventListener != null
                    && prefsObj instanceof PreferencesForDocumentSetting prefsObjActual) {
                    eventListener.accept(
                            new GuiSwingPrefsSupports.PreferencesUpdateEvent(valuePane.getSwingViewContext(), prefsObjActual));
                }
            };
        }

        @Override
        public boolean loadFromAndChanged(GuiPreferences prefs) {
            if (getPrefsObj().loadFromAndChanged(prefs)) {
                updateGuiAndTargetTextPaneFromPrefsObj();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void saveTo(GuiPreferences prefs) {
            getPrefsObj().saveTo(prefs);
        }
    }

    /**
     * actually implements {@link org.autogui.swing.prefs.GuiSwingPrefsSupports.Preferences}
     * @since 1.7
     */
    public static class PreferencesForDocumentSetting extends TextPaneBuilder.PreferencesForDocumentSettingBase implements GuiSwingPrefsSupports.Preferences {
        public PreferencesForDocumentSetting() {
        }

        public PreferencesForDocumentSetting(JEditorPane pane) {
            super(pane);
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
}
