package org.autogui.swing.prefs;

import org.autogui.base.mapping.GuiPreferences;
import org.autogui.swing.*;
import org.autogui.swing.table.GuiSwingTableColumnString;
import org.autogui.swing.table.GuiSwingTableModelCollection;
import org.autogui.swing.table.ObjectTableColumn;
import org.autogui.swing.table.ObjectTableModelColumns;

public interface GuiSwingPrefsApplyOptions {

    /**
     *  default options at prefs loading: non-init, no skipping
     * @since 1.4
     */
    PrefsApplyOptionsDefault APPLY_OPTIONS_DEFAULT = new PrefsApplyOptionsDefault(false, false);

    /**
     * @return true if the loading processing is the initial time; then the window location might be set
     */
    boolean isInit();

    /**
     * @return if true, skipping setting object properties
     */
    boolean isSkippingValue();

    default boolean hasHistoryValues(GuiPreferences targetPrefs, GuiPreferences ctxPrefs) {
        return !targetPrefs.equals(ctxPrefs);
    }

    default void begin(Object loadingTarget, GuiPreferences prefs, PrefsApplyOptionsLoadingTargetType targetType) {}
    default void apply(GuiSwingPrefsSupports.WindowPreferencesUpdater windowPrefs, GuiPreferences prefs) {
        windowPrefs.apply(prefs, this);
    }
    default void loadFromPrefs(GuiSwingPrefsSupports.WindowPreferencesUpdater windowPrefs, GuiPreferences prefs) {
        windowPrefs.getPrefs().loadFrom(prefs);
    }
    default void apply(GuiSwingPrefsSupports.FileDialogPreferencesUpdater fileDialogPrefs, GuiPreferences prefs) {
        fileDialogPrefs.apply(prefs);
    }
    default void apply(GuiSwingViewObjectPane.SplitPreferencesUpdater splitPrtefs, GuiPreferences prefs) {
        splitPrtefs.apply(prefs);
    }
    default void apply(GuiSwingViewTabbedPane.TabPreferencesUpdater tabPrefs, GuiPreferences prefs) {
        tabPrefs.apply(prefs);
    }
    default void addHistoryValue(GuiPreferences.HistoryValueEntry entry, GuiPreferences prefs) {
        prefs.addHistoryValue(entry.getValue(), entry.getTime());
    }
    default void loadFrom(GuiSwingViewDocumentEditor.DocumentSettingPane pane, GuiPreferences prefs) {
        pane.loadFrom(prefs);
    }
    default void loadFrom(GuiSwingViewNumberSpinner.TypedSpinnerNumberModel numModel, GuiPreferences prefs) {
        numModel.loadFrom(prefs);
    }
    default void setLastHistoryValueBySwingViewHistoryValue(GuiSwingView.ValuePane<Object> pane, GuiPreferences prefs, Object value) {
        pane.setSwingViewHistoryValue(value);
    }
    default void setLastHistoryValueByPrefsJsonSupported(GuiSwingView.ValuePane<Object> pane, GuiPreferences prefs, Object value) {
        pane.setPrefsJsonSupported(value);
    }
    default void setLastHistoryValueBySwingViewHistoryValue(GuiSwingView.ValuePane<Object> pane, GuiPreferences prefs, GuiPreferences.HistoryValueEntry entry) {
        Object value = entry.getValue();
        pane.setSwingViewHistoryValue(value);
    }

    default void apply(GuiSwingViewCollectionTable.TablePreferencesUpdater tablePrefs, GuiPreferences prefs) {
        tablePrefs.apply(prefs);
    }

    default boolean isSavingAsCurrentPreferencesInColumns() {
        return true;
    }

    default void loadFrom(GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic widthPrefs, GuiPreferences prefs) {
        widthPrefs.loadFrom(prefs);
    }

    default void loadFrom(GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic orderPrefs, GuiPreferences prefs) {
        orderPrefs.loadFrom(prefs);
    }

    default void applyTo(GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic widthPrefs, ObjectTableColumn column) {
        widthPrefs.applyTo(column);
    }

    default boolean applyTo(GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic orderPrefs, GuiSwingTableModelCollection.GuiSwingTableModelColumns columns, int modelIndex) {
        return orderPrefs.applyTo(columns, modelIndex);
    }

    default void loadFromAndApplyTo(GuiSwingTableModelCollection.PreferencesForTableColumnWidth columnWidthPrefs, ObjectTableColumn column, GuiPreferences prefs) {
        columnWidthPrefs.loadFrom(prefs);
        columnWidthPrefs.applyTo(column);
    }

    default boolean loadFromAndApplyTo(GuiSwingTableModelCollection.PreferencesForTableColumnOrder orderPrefs, ObjectTableModelColumns columns, GuiPreferences prefs) {
        orderPrefs.loadFrom(prefs);
        return orderPrefs.applyTo(columns);
    }

    /**
     * @param columnEditor the actual pane  
     * @param prefs the source prefs
     * @since 1.8
     */
    default void loadFromAndApplyTo(GuiSwingTableColumnString.MultilineColumnTextPane columnEditor, GuiPreferences prefs) {
        columnEditor.loadFromAndApplyTo(prefs);
    }

    default void end(Object loadingTarget, GuiPreferences prefs, PrefsApplyOptionsLoadingTargetType targetType) {}

    enum PrefsApplyOptionsLoadingTargetType {
        View,
        HistoryValues,
        CurrentValue
    }


    /**
     * the default impl. of options at prefs loading
     * @since 1.4
     */
    class PrefsApplyOptionsDefault implements GuiSwingPrefsApplyOptions {
        protected boolean init;
        protected boolean skippingValue;

        public PrefsApplyOptionsDefault(boolean init, boolean skippingValue) {
            this.init = init;
            this.skippingValue = skippingValue;
        }

        @Override
        public boolean isInit() {
            return init;
        }

        @Override
        public boolean isSkippingValue() {
            return skippingValue;
        }
    }
}
