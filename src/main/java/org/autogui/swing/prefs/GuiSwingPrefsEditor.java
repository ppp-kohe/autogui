package org.autogui.swing.prefs;

import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValueNumberSpinner;
import org.autogui.swing.*;
import org.autogui.swing.table.GuiSwingTableModelCollection;
import org.autogui.swing.table.ObjectTableColumn;
import org.autogui.swing.table.ObjectTableModelColumns;
import org.autogui.swing.util.*;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GuiSwingPrefsEditor implements GuiSwingPrefsApplyOptions {
    protected JComponent rootPane;
    protected JComponent contentPane;
    protected ValueListPaneHistoryValueEntry historyValuesPane;
    protected GuiSwingPrefsHistoryValues valuePaneFactory = new GuiSwingPrefsHistoryValues();
    protected List<Consumer<Boolean>> updatedListeners = new ArrayList<>();

    protected boolean construction = false;
    protected boolean updated;

    protected List<JTextComponent> labelFields = new ArrayList<>();
    protected AbstractAction revertBackupAction;
    protected GuiPreferences preferences;
    protected GuiPreferences backupPrefs;
    protected List<Runnable> validationCheckers = new ArrayList<>();

    protected Map<String, JComponent> namePathToPanes = new HashMap<>();

    public GuiSwingPrefsEditor() {}

    public GuiSwingPrefsEditor addUpdatedListener(Consumer<Boolean> listener) {
        updatedListeners.add(listener);
        return this;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        boolean changed = !this.updated == updated;
        this.updated = updated;
        if (changed) {
            updatedListeners.forEach(l -> l.accept(updated));
        }
    }

    public synchronized void setConstruction(boolean construction) {
        this.construction = construction;
    }

    public synchronized boolean isNotConstruction() {
        return !construction;
    }

    public JComponent getRootPaneAfterInit() {
        setConstruction(false);
        if (rootPane == null) {
            rootPane = createRootPane();
        }
        return getRootPane();
    }

    public JComponent getContentPane() {
        return contentPane;
    }

    public JComponent getRootPane() {
        return rootPane;
    }

    public AbstractAction getRevertBackupAction() {
        if (revertBackupAction == null) {
            revertBackupAction = new RevertBackupAction(this);
            revertBackupAction.setEnabled(updated);
            addUpdatedListener(v -> revertBackupAction.setEnabled(v));
        }
        return revertBackupAction;
    }

    public JComponent create(GuiSwingPreferences.RootView view, GuiPreferences prefs) {
        return create(view::loadPreferences, prefs);
    }

    public JComponent create(GuiSwingView.ValuePane<?> view, GuiPreferences prefs) {
        return create(view::loadSwingPreferences, prefs);
    }

    protected JComponent create(BiConsumer<GuiPreferences, GuiSwingPrefsEditor> loader, GuiPreferences prefs) {
        this.preferences = prefs;
        backupPrefs = prefs.copyOnMemoryAsRoot();
        loader.accept(prefs, this);
        return getRootPaneAfterInit();
    }

    public GuiPreferences getBackupPrefs() {
        return backupPrefs;
    }

    public void revertBackupPrefs() {
        try (var lock = preferences.lock()) {
            lock.use();
            preferences.fromJson(backupPrefs.toJson());
        }
        preferences.getValueStore().flush();
        revalidate();
        setUpdated(false);
    }

    public void revalidate() {
        validationCheckers.forEach(Runnable::run);
    }

    @Override
    public boolean isInit() {
        return false;
    }

    @Override
    public boolean isSkippingValue() {
        return false;
    }

    @Override
    public boolean hasHistoryValues(GuiPreferences targetPrefs, GuiPreferences ctxPrefs) {
        return true;
    }

    @Override
    public void begin(Object loadingTarget, GuiPreferences prefs, PrefsApplyOptionsLoadingTargetType targetType) {
        if (contentPane == null) {
            setConstruction(true);
            contentPane = createPane(false);
            UIManagerUtil u = UIManagerUtil.getInstance();
            contentPane.setBorder(BorderFactory.createEmptyBorder(u.getScaledSizeInt(15), u.getScaledSizeInt(15), u.getScaledSizeInt(15), u.getScaledSizeInt(15)));

            addToContentPane(createPrefsInfo(prefs));
        }
        if (targetType == PrefsApplyOptionsLoadingTargetType.HistoryValues) {
            var created = addToContentPaneIfFirst(prefs, GuiPreferences.KEY_HISTORY, this::createHistoryValuesPane);
            if (created != null) {
                historyValuesPane = created;
            }
        }
    }

    @Override
    public void end(Object loadingTarget, GuiPreferences prefs, PrefsApplyOptionsLoadingTargetType targetType) {
        if (targetType == PrefsApplyOptionsLoadingTargetType.HistoryValues) {
            if (historyValuesPane != null) {
                addHistoryValuesPane(prefs, historyValuesPane);
                historyValuesPane = null;
            }
        }
    }

    @Override
    public void apply(GuiSwingPrefsSupports.WindowPreferencesUpdater windowPrefs, GuiPreferences prefs) {
        addToContentPaneIfFirst(prefs, windowPrefs.getPrefs(), p -> createWindowPrefs(windowPrefs.getPrefs().getKey(), p));
    }

    @Override
    public void loadFromPrefs(GuiSwingPrefsSupports.WindowPreferencesUpdater windowPrefs, GuiPreferences prefs) {
        apply(windowPrefs, prefs);
    }

    @Override
    public void apply(GuiSwingViewTabbedPane.TabPreferencesUpdater tabPrefs, GuiPreferences prefs) {
        addToContentPaneIfFirst(prefs, tabPrefs.getPrefs(), this::createTabPrefs);
    }

    @Override
    public void apply(GuiSwingViewObjectPane.SplitPreferencesUpdater splitPrefs, GuiPreferences prefs) {
        addToContentPaneIfFirst(prefs, splitPrefs.getPrefs().getKey(), this::createSplitPrefs);
    }

    @Override
    public void apply(GuiSwingPrefsSupports.FileDialogPreferencesUpdater fileDialogPrefs, GuiPreferences prefs) {
        addToContentPaneIfFirst(prefs, fileDialogPrefs.getPrefs(), this::createFileDialogPrefs);
    }

    @Override
    public void loadFrom(GuiSwingViewDocumentEditor.DocumentSettingPane pane, GuiPreferences prefs) {
        addToContentPaneIfFirst(prefs, DOCUMENT_PREFS_KEY, this::createDocumentSettingPrefs);
    }

    @Override
    public void loadFrom(GuiSwingViewNumberSpinner.TypedSpinnerNumberModel numModel, GuiPreferences prefs) {
        addToContentPaneIfFirst(prefs, NUMBER_PREFS_KEY, p -> createNumberPrefs(numModel.getNumberType(), p));
    }

    @Override
    public void addHistoryValue(GuiPreferences.HistoryValueEntry entry, GuiPreferences prefs) {
        //historyValuesPane.editSource(es -> es.add(entry)); //it does not need to manually add
    }

    @Override
    public void setLastHistoryValueBySwingViewHistoryValue(GuiSwingView.ValuePane<Object> pane, GuiPreferences prefs, Object value) {
        //prefs current value
        addToContentPaneIfFirst(prefs, TABLE_PREFS_CURRENT_VALUE, p -> createLastHistoryValueBySwingViewHistoryValue(p, value));
    }

    @Override
    public void setLastHistoryValueByPrefsJsonSupported(GuiSwingView.ValuePane<Object> pane, GuiPreferences prefs, Object value) {
        addToContentPaneIfFirst(prefs, TABLE_PREFS_CURRENT_VALUE, p -> createLastHistoryValueBySwingViewHistoryValue(p, value));
    }

    @Override
    public void setLastHistoryValueBySwingViewHistoryValue(GuiSwingView.ValuePane<Object> pane, GuiPreferences prefs, GuiPreferences.HistoryValueEntry entry) {
        //the value is loaded from¥ a top history-entry; thus the entry will be loaded as a history-entry
        //addToRootPane(createLastHistoryValueBySwingViewHistoryValue(prefs, entry));
    }

    @Override
    public void apply(GuiSwingViewCollectionTable.TablePreferencesUpdater tablePrefs, GuiPreferences prefs) {
        addToContentPaneIfFirst(prefs, new GuiSwingViewCollectionTable.PreferencesForTable().getKey(), this::createTablePrefs);
    }


    @Override
    public boolean isSavingAsCurrentPreferencesInColumns() {
        return false;
    }

    @Override
    public void loadFrom(GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic widthPrefs, GuiPreferences prefs) {
        addToContentPaneIfFirst(prefs, widthPrefs, this::createTableColumnWidthStatic);
    }

    @Override
    public void loadFrom(GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic orderPrefs, GuiPreferences prefs) {
        addToContentPaneIfFirst(prefs, orderPrefs, this::createTableColumnOrderStatic);
    }

    @Override
    public void applyTo(GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic widthPrefs, ObjectTableColumn column) {
    }

    @Override
    public boolean applyTo(GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic orderPrefs, GuiSwingTableModelCollection.GuiSwingTableModelColumns columns, int modelIndex) {
        return false;
    }

    @Override
    public void loadFromAndApplyTo(GuiSwingTableModelCollection.PreferencesForTableColumnWidth columnWidthPrefs, ObjectTableColumn column, GuiPreferences prefs) {
        addToContentPaneIfFirst(prefs, columnWidthPrefs, this::createTableColumnWidth);
    }

    @Override
    public boolean loadFromAndApplyTo(GuiSwingTableModelCollection.PreferencesForTableColumnOrder orderPrefs, ObjectTableModelColumns columns, GuiPreferences prefs) {
        addToContentPaneIfFirst(prefs, orderPrefs, this::createTableColumnOrder);
        return false;
    }

    public JComponent createRootPane() {
        UIManagerUtil u = UIManagerUtil.getInstance();
        var rootPane = new JPanel(new BorderLayout());
        {
            rootPane.setBorder(BorderFactory.createEmptyBorder(u.getScaledSizeInt(10), u.getScaledSizeInt(10), u.getScaledSizeInt(10), u.getScaledSizeInt(10)));

            var toolBar = new JPanel(new ResizableFlowLayout(true));
            {
                //toolBar.setBorderPainted(false);
                //toolBar.setFloatable(false);
                toolBar.setBorder(BorderFactory.createEmptyBorder(u.getScaledSizeInt(5), u.getScaledSizeInt(15), u.getScaledSizeInt(5), u.getScaledSizeInt(15)));
                ResizableFlowLayout.add(toolBar, createSearchTextField(), true);

                ResizableFlowLayout.add(toolBar, Box.createHorizontalStrut(u.getScaledSizeInt(15)), false);

                JButton button = new JButton(getRevertBackupAction());
                ResizableFlowLayout.add(toolBar, button, false);
            }
            rootPane.add(toolBar, BorderLayout.NORTH);

            ResizableFlowLayout.add(contentPane, Box.createVerticalStrut(u.getScaledSizeInt(30)), false);
            JScrollPane scrollPane = new JScrollPane(contentPane);
            {
                scrollPane.getVerticalScrollBar().setUnitIncrement(u.getScaledSizeInt(16));
                scrollPane.setBorder(BorderFactory.createEmptyBorder());
            }
            rootPane.add(scrollPane, BorderLayout.CENTER);
        }
        return rootPane;
    }

    public ValueListPaneHistoryValueEntry createHistoryValuesPane(GuiPreferences prefs) {
        return valueList(new ValueListPaneHistoryValueEntry(prefs::getHistoryValues, prefs, saveRunnerForHistory()));
    }


    public <PaneType extends JComponent> PaneType addToContentPaneIfFirst(GuiPreferences prefs, GuiSwingPrefsSupports.PreferencesByJsonEntry prefsObj, Function<GuiPreferences, PaneType> paneFactory) {
        return addToContentPaneIfFirst(prefs, prefsObj.getKey(), paneFactory);
    }

    public <PaneType extends JComponent> PaneType addToContentPaneIfFirst(GuiPreferences prefs, String key, Function<GuiPreferences, PaneType> paneFactory) {
        return addToContentPaneIfFirst(getName(prefs, key), () -> paneFactory.apply(prefs));
    }

    public <PaneType extends JComponent> PaneType addToContentPaneIfFirst(String name, Supplier<PaneType> paneFactory) {
        if (!namePathToPanes.containsKey(name)) {
            var pane = paneFactory.get();
            if (pane != null) {
                namePathToPanes.put(name, pane);
                addToContentPane(pane);
            }
            return pane;
        } else {
            return null;
        }
    }

    public void addHistoryValuesPane(GuiPreferences prefs, JComponent historyValuesPane) {
        addToContentPane(createNamed(getName(prefs, GuiPreferences.KEY_HISTORY), historyValuesPane));
    }

    public void addToContentPane(JComponent component) {
        ResizableFlowLayout.add(contentPane, component, false);
    }

    public static final String WINDOW_PREFS_PROP_X = "x";
    public static final String WINDOW_PREFS_PROP_Y = "y";
    public static final String WINDOW_PREFS_PROP_WIDTH = "width";
    public static final String WINDOW_PREFS_PROP_HEIGHT = "height";
    public static final String TAB_PREFS_PROP_SELECTED = "selected";
    public static final String FILE_DIALOG_PREFS_PROP_BACK_PATH = "backPath";
    public static final String FILE_DIALOG_PREFS_PROP_CURRENT_DIRECTORY = "currentDirectory";
    public static final String FILE_DIALOG_PREFS_PROP_FILE_LIST = "fileList";
    public static final String SPLIT_PREFS_PROP_DIVIDER = "divider";
    public static final String SPLIT_PREFS_PROP_HORIZONTAL = "horizontal";
    public static final String TABLE_PREFS_COLUMN_ORDER = "columnOrder";
    public static final String TABLE_PREFS_COLUMN_WIDTH = "columnWidth";
    public static final String TABLE_PREFS_ROW_HEIGHT = "rowHeight";
    public static final String TABLE_PREFS_ROW_CUSTOM = "rowCustom";
    public static final String TABLE_PREFS_ROW_FIT_TO_CONTENT = "rowFitToContent";
    public static final String TABLE_PREFS_ROW_SORT = "rowSort";
    public static final String TABLE_PREFS_COLUMN_INDEX = "columnIndex";
    public static final String TABLE_PREFS_COLUMN_ORDER_MODEL_INDEX = "modelIndex";
    public static final String TABLE_PREFS_COLUMN_ORDER_VIEW_INDEX = "viewIndex";
    public static final String TABLE_PREFS_CURRENT_VALUE = GuiPreferences.KEY_CURRENT_VALUE;
    public static final String DOCUMENT_PREFS_KEY = "<DocumentSetting>";
    public static final String NUMBER_PREFS_KEY = "<NumberSetting>";

    public JComponent createPrefsInfo(GuiPreferences prefs) {
        var pane = createPane(false);
        {
            var nameField = SearchFilterTextField.createTextFieldAsLabel("");
            SettingsWindow.LabelGroup g = new SettingsWindow.LabelGroup();
            ResizableFlowLayout.add(pane, createNamedHorizontal(g, GuiPreferences.KEY_NAME, nameField), false);

            var uuidField = SearchFilterTextField.createTextFieldAsLabel("");
            ResizableFlowLayout.add(pane, createNamedHorizontal(g, GuiPreferences.KEY_UUID, uuidField), false);

            Runnable updater = () -> {
                var name = prefs.getValueStore().getString(GuiPreferences.KEY_NAME, "");
                nameField.setText(name);

                var uuid = prefs.getValueStore().getString(GuiPreferences.KEY_UUID, "");
                uuidField.setText(uuid);
            };
            validationCheckers.add(updater);
            updater.run();

            ResizableFlowLayout.add(pane, new JSeparator(JSeparator.HORIZONTAL), false);
        }
        return pane;
    }

    public JComponent createWindowPrefs(String key, GuiPreferences prefs) {
        var prefsObj = new GuiSwingPrefsSupports.PreferencesForWindow(key);
        prefsObj.loadFrom(prefs);

        var pane = createPane(true);
        {
            var names = new SettingsWindow.LabelGroup();
            ResizableFlowLayout.add(pane, createNumberInt(names, WINDOW_PREFS_PROP_X, prefsObj::getX, setterPrefs(prefsObj::setX, prefsObj, prefs), validationCheckers::add), false);
            ResizableFlowLayout.add(pane, createNumberInt(names, WINDOW_PREFS_PROP_Y, prefsObj::getY, setterPrefs(prefsObj::setY, prefsObj, prefs), validationCheckers::add), false);
            ResizableFlowLayout.add(pane, createNumberInt(names, WINDOW_PREFS_PROP_WIDTH, prefsObj::getWidth, setterPrefs(prefsObj::setWidth, prefsObj, prefs), validationCheckers::add), false);
            ResizableFlowLayout.add(pane, createNumberInt(names, WINDOW_PREFS_PROP_HEIGHT, prefsObj::getHeight, setterPrefs(prefsObj::setHeight, prefsObj, prefs), validationCheckers::add), false);
            names.fitWidth();
        }
        return createNamed(getName(prefs, prefsObj), pane);
    }

    public JComponent createTabPrefs(GuiPreferences prefs) {
        var prefsObj = new GuiSwingViewTabbedPane.PreferencesForTab();
        prefsObj.loadFrom(prefs);

        var pane = createPane(true);
        {
            ResizableFlowLayout.add(pane, createNumberInt(null, TAB_PREFS_PROP_SELECTED, prefsObj::getSelectedIndex, setterPrefs(prefsObj::setSelectedIndex, prefsObj, prefs), validationCheckers::add), false);
        }
        return createNamed(getName(prefs, prefsObj), pane);
    }

    public JComponent createSplitPrefs(GuiPreferences prefs) {
        var prefsObj = new GuiSwingViewObjectPane.PreferencesForSplit();
        prefsObj.loadFrom(prefs);
        return createNamed(getName(prefs, prefsObj), valueList(new ValueListPaneSplitEntry(prefsObj, saveRunner(prefsObj, prefs))));
    }

    public JComponent createFileDialogPrefs(GuiPreferences prefs) {
        var prefsObj = new GuiSwingPrefsSupports.PreferencesForFileDialog();
        prefsObj.loadFrom(prefs);

        var pane = createPane(false);
        {
            var names = new SettingsWindow.LabelGroup();
            ResizableFlowLayout.add(pane, createFile(names, FILE_DIALOG_PREFS_PROP_BACK_PATH, prefsObj::getBackPath, setterPrefs(prefsObj::setBackPath, prefsObj, prefs), validationCheckers::add), false);
            ResizableFlowLayout.add(pane, createFile(names, FILE_DIALOG_PREFS_PROP_CURRENT_DIRECTORY, prefsObj::getCurrentDirectory, setterPrefs(prefsObj::setCurrentDirectory, prefsObj, prefs), validationCheckers::add), false);
            names.fitWidth();

            ResizableFlowLayout.add(pane, createNamed(getName(prefs, prefsObj.getKey() + "." + FILE_DIALOG_PREFS_PROP_FILE_LIST), valueList(new ValueListPaneFileDialogList(prefsObj, saveRunner(prefsObj, prefs)))), false);
        }

        return createNamed(getName(prefs, prefsObj), pane);
    }

    public JComponent createDocumentSettingPrefs(GuiPreferences prefs) {
        var docPane = new GuiSwingViewDocumentEditor.DocumentSettingPane(null);
        docPane.loadFrom(prefs);
        Runnable saveRunner = saveRunner(docPane.getPrefsObj(), prefs);
        docPane.setPreferencesUpdater(p -> saveRunner.run());
        validationCheckers.add(docPane::updateGuiAndTargetTextPaneFromPrefsObj);
        return createNamed(getName(prefs, DOCUMENT_PREFS_KEY), docPane);
    }

    public JComponent createNumberPrefs(GuiReprValueNumberSpinner.NumberType numType, GuiPreferences prefs) {
        var model = new GuiSwingViewNumberSpinner.TypedSpinnerNumberModel(numType);
        var saveRunner = saveRunner(model, prefs);
        model.loadFrom(prefs);
        model.addChangeListener(e -> saveRunner.run());
        var pane = new GuiSwingViewNumberSpinner.NumberSettingPane(model);

        var settingsPane = ResizableFlowLayout.create(false)
                .add(pane)
                .add(createWindowPrefs(GuiSwingViewNumberSpinner.NUMBER_SETTING_WINDOW_PREFS_KEY, prefs))
                .getContainer();
        validationCheckers.add(pane::updateFromModel);

        return createNamed(getName(prefs, NUMBER_PREFS_KEY), settingsPane);
    }

    public JComponent createLastHistoryValueBySwingViewHistoryValue(GuiPreferences prefs, Object value) {
        var jsonPane = valuePaneFactory.createHistoryObjectPrefs(value, prefs, true);
        validationCheckers.add(jsonPane::updateLastEntrySource);
        return createNamed(getName(prefs, TABLE_PREFS_CURRENT_VALUE), jsonPane);
    }

    public JComponent createLastHistoryValueBySwingViewHistoryValue(GuiPreferences prefs, GuiPreferences.HistoryValueEntry entry) {
        var jsonPane = valuePaneFactory.createHistory(entry, prefs);
        validationCheckers.add(jsonPane::updateLastEntrySource);
        return createNamed(getName(prefs, TABLE_PREFS_CURRENT_VALUE), jsonPane);
    }

    public JComponent createTablePrefs(GuiPreferences prefs) {
        GuiSwingViewCollectionTable.PreferencesForTable prefsObj = new GuiSwingViewCollectionTable.PreferencesForTable();
        prefsObj.loadFrom(prefs);

        var pane = createPane(false);
        {
            ResizableFlowLayout.add(pane, createString(null, TABLE_PREFS_COLUMN_ORDER,
                            () -> getIntListString(prefsObj.getColumnOrder()),
                            setterPrefs(s -> setIntListString(prefsObj.getColumnOrder(), s), prefsObj, prefs), validationCheckers::add), false);
            ResizableFlowLayout.add(pane, createString(null, TABLE_PREFS_COLUMN_WIDTH,
                            () -> getIntListString(prefsObj.getColumnWidth()),
                            setterPrefs(s -> setIntListString(prefsObj.getColumnWidth(), s), prefsObj,prefs), validationCheckers::add), false);

            var paneNum = createPane(true);
            {
                var names = new SettingsWindow.LabelGroup();
                ResizableFlowLayout.add(paneNum, createNumberInt(names, TABLE_PREFS_ROW_HEIGHT, prefsObj::getRowHeight,
                        setterPrefs(prefsObj::setRowHeight, prefsObj, prefs), validationCheckers::add), false);
                ResizableFlowLayout.add(paneNum, createBoolean(names, TABLE_PREFS_ROW_CUSTOM, prefsObj::isRowCustom,
                                setterPrefs(prefsObj::setRowCustom, prefsObj, prefs), validationCheckers::add), false);
                ResizableFlowLayout.add(paneNum, createBoolean(names, TABLE_PREFS_ROW_FIT_TO_CONTENT, prefsObj::isRowFitToContent,
                                setterPrefs(prefsObj::setRowFitToContent, prefsObj, prefs), validationCheckers::add), false);
                names.fitWidth();
            }
            ResizableFlowLayout.add(pane, paneNum, false);

            ResizableFlowLayout.add(pane, createNamed(TABLE_PREFS_ROW_SORT, valueList(new ValueListTableRowSort(prefsObj::getRowSort, saveRunner(prefsObj, prefs)))), false);
        }
        return createNamed(getName(prefs, prefsObj), pane);
    }

    public JComponent createTableColumnWidthStatic(GuiPreferences prefs) {
        GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic prefsObj = new GuiSwingTableModelCollection.PreferencesForTableColumnWidthStatic();
        prefsObj.loadFrom(prefs);
        return createNamed(getName(prefs, prefsObj), valueList(new ValueListMapPaneForTableColumnWidthStatic(prefsObj::getModelIndexToWidthDirect, saveRunner(prefsObj, prefs))));
    }

    public JComponent createTableColumnOrderStatic(GuiPreferences prefs) {
        GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic prefsObj = new GuiSwingTableModelCollection.PreferencesForTableColumnOrderStatic();
        prefsObj.loadFrom(prefs);
        return createNamed(getName(prefs, prefsObj), valueList(new ValueListMapPaneForTableColumnOrder(prefsObj::getModelIndexToOrderDirect, saveRunner(prefsObj, prefs))));
    }

    public JComponent createTableColumnWidth(GuiPreferences prefs) {
        GuiSwingTableModelCollection.PreferencesForTableColumnWidth prefsObj = new GuiSwingTableModelCollection.PreferencesForTableColumnWidth();
        prefsObj.loadFrom(prefs);

        var pane = createPane(true);
        {
            ResizableFlowLayout.add(pane, createNumberInt(null, TABLE_PREFS_COLUMN_WIDTH, prefsObj::getWidth, setterPrefs(prefsObj::setWidth, prefsObj, prefs), validationCheckers::add), false);
        }
        return createNamed(getName(prefs, prefsObj), pane);
    }

    public JComponent createTableColumnOrder(GuiPreferences prefs) {
        GuiSwingTableModelCollection.PreferencesForTableColumnOrder prefsObj = new GuiSwingTableModelCollection.PreferencesForTableColumnOrder();
        prefsObj.loadFrom(prefs);
        var pane = createPane(true);
        {
            ResizableFlowLayout.add(pane, createNumberInt(null, TABLE_PREFS_COLUMN_ORDER_MODEL_INDEX, prefsObj::getModelIndex, setterPrefs(prefsObj::setModelIndex, prefsObj, prefs), validationCheckers::add), false);
            ResizableFlowLayout.add(pane, createNumberInt(null, TABLE_PREFS_COLUMN_ORDER_VIEW_INDEX, prefsObj::getViewIndex, setterPrefs(prefsObj::setViewIndex, prefsObj, prefs), validationCheckers::add), false);
        }
        return createNamed(getName(prefs, prefsObj.getKey()), pane);
    }

    public JComponent createSearchTextField() {
        return SearchFilterTextField.createTextHighlightCollection(this::getLabelFields)
                .setPlaceHolderText("Search settings...");
    }

    public List<JTextComponent> getLabelFields() {
        return labelFields;
    }

    public static String getIntListString(List<Integer> target) {
        return target.stream()
                .map(Object::toString)
                .collect(Collectors.joining(" "));
    }

    public static void setIntListString(List<Integer> target, String s) {
        var pat = Pattern.compile("(-|)[0-9]+");
        target.clear();
        Arrays.stream(s.split("[\\s,]+"))
                .filter(v -> pat.matcher(v).matches())
                .map(Integer::parseInt)
                .forEach(target::add);
    }

    public <ValueType> Consumer<ValueType> setterPrefs(Consumer<ValueType> setter, GuiSwingPrefsSupports.Preferences prefsObj, GuiPreferences prefs) {
        return v -> {
            setter.accept(v);
            if (isNotConstruction()) {
                prefsObj.saveTo(prefs);
                setUpdated(true);
            }
        };
    }

    public Runnable saveRunner(GuiSwingPrefsSupports.Preferences prefsObj, GuiPreferences prefs) {
        return () -> {
            if (isNotConstruction()) {
                prefsObj.saveTo(prefs);
                setUpdated(true);
            }
        };
    }

    public Runnable saveRunnerForHistory() {
        return () -> {
            if (isNotConstruction()) {
                setUpdated(true);
            }
        };
    }

    public static JPanel createPane(boolean horizontal) {
        var pane = new JPanel();
        ResizableFlowLayout layout = new ResizableFlowLayout(horizontal, UIManagerUtil.getInstance().getScaledSizeInt(10));
        layout.setFitHeight(true);
        pane.setLayout(layout);
        return pane;
    }

    public JPanel createNamed(String name, JComponent contentPane) {
        var pane = new JPanel(new BorderLayout());
        var label = SearchFilterTextField.createTextFieldAsLabel(name + ":");
        UIManagerUtil u = UIManagerUtil.getInstance();
        label.setFont(u.getConsoleFont().deriveFont(Font.BOLD));
        pane.add(label, BorderLayout.NORTH);
        pane.add(contentPane, BorderLayout.CENTER);
        labelFields.add(label);
        return pane;
    }

    public static JPanel createNamedHorizontal(SettingsWindow.LabelGroup names, String name, JComponent contentPane) {
        UIManagerUtil u = UIManagerUtil.getInstance();
        var w = u.getScaledSizeInt(5);
        var h = u.getScaledSizeInt(5);
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout(w, h));
        var nameLabel = new JLabel(name + ":");
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nameLabel.setFont(u.getConsoleFont());
        if (names != null) {
            names.addName(nameLabel);
        }
        pane.add(nameLabel, BorderLayout.WEST);
        pane.add(contentPane, BorderLayout.CENTER);
        pane.setBorder(BorderFactory.createEmptyBorder(h, w, h, w));
        return pane;
    }

    public static JComponent createNumberInt(SettingsWindow.LabelGroup names, String name, Supplier<Integer> getter, Consumer<Integer> setter, Consumer<Runnable> validationCheckers) {
        return createNumber(names, name, Integer.class, getter, setter, validationCheckers);
    }

    public static <NumType extends Number> JComponent createNumber(SettingsWindow.LabelGroup names, String name, Class<NumType> type, Supplier<NumType> getter, Consumer<NumType> setter, Consumer<Runnable> validationCheckers) {
        var num = new LambdaProperty.LambdaNumberSpinner(name, type, getter, setter);
        num.setPreferredSize(createNumberPreferredSize());
        return createNamedHorizontal(names, name, valuePane(num, validationCheckers));
    }

    public static JComponent createString(SettingsWindow.LabelGroup names, String name, Supplier<String> getter, Consumer<String> setter, Consumer<Runnable> validationCheckers) {
        return createNamedHorizontal(names, name, valuePane(new LambdaProperty.LambdaStringPane(name, getter, setter), validationCheckers));
    }

    public static JComponent createBoolean(SettingsWindow.LabelGroup names, String name, Supplier<Boolean> getter, Consumer<Boolean> setter, Consumer<Runnable> validationCheckers) {
        return createNamedHorizontal(names, name, valuePane(new LambdaProperty.LambdaBooleanCheckBox(name, "", getter, setter), validationCheckers));
    }

    public static Dimension createNumberPreferredSize() {
        UIManagerUtil u = UIManagerUtil.getInstance();
        return new Dimension(u.getScaledSizeInt(96), u.getScaledSizeInt(24));
    }

    public static JComponent createFile(SettingsWindow.LabelGroup names, String name, Supplier<Path> getter, Consumer<Path> setter, Consumer<Runnable> validationCheckers) {
        return createNamedHorizontal(names, name, valuePane(new LambdaProperty.LambdaFilePathPane(name, getter, setter), validationCheckers));
    }

    public static <Pane extends GuiSwingView.ValuePane<?>> Pane valuePane(Pane p, Consumer<Runnable> validationCheckers) {
        if (validationCheckers != null) {
            validationCheckers.accept(p::updateSwingViewSource);
        }
        return p;
    }

    protected <Pane extends ValueListPane<?, ?>> Pane valueList(Pane list) {
        this.validationCheckers.add(list::syncElements);
        return list;
    }

    public static String getName(GuiPreferences prefs, GuiSwingPrefsSupports.PreferencesByJsonEntry prefsObj) {
        return getName(prefs, prefsObj.getKey());
    }

    public static String getName(GuiPreferences prefs, String nameOpt) {
        List<String> names = new ArrayList<>();
        if (nameOpt != null) {
            names.add(nameOpt);
        }
        for (GuiPreferences current = prefs; current != null; current = current.getParent()) {
            names.add(current.getName());
        }
        return String.join(".", names.reversed());
    }

    public static class ValueListPaneSplitEntry extends ValueListPane<GuiSwingViewObjectPane.PreferencesForSplitEntry, ValueListSplitEntryItemPane> {
        protected GuiSwingViewObjectPane.PreferencesForSplit prefsObj;
        protected Runnable saveRunner;

        @SuppressWarnings("this-escape")
        public ValueListPaneSplitEntry(GuiSwingViewObjectPane.PreferencesForSplit prefsObj, Runnable saveRunner) {
            super(false);
            this.prefsObj = prefsObj;
            this.saveRunner = saveRunner;
            init();
            syncElements();
        }

        @Override
        public List<GuiSwingViewObjectPane.PreferencesForSplitEntry> takeSource() {
            return prefsObj == null ? null : nonNullList(prefsObj.getSplits());
        }

        @Override
        public ValueListSplitEntryItemPane newElementPane(int i, ValueListElementPane<GuiSwingViewObjectPane.PreferencesForSplitEntry, ValueListSplitEntryItemPane> elementPane) {
            return new ValueListSplitEntryItemPane(saveRunner);
        }

        @Override
        public boolean updateSourceValueToElementPane(int i, GuiSwingViewObjectPane.PreferencesForSplitEntry value, ValueListElementPane<GuiSwingViewObjectPane.PreferencesForSplitEntry, ValueListSplitEntryItemPane> pane) {
            boolean update = !Objects.equals(value, pane.contentPane().getEntry());
            if (update) {
                pane.contentPane().setEntry(value);
            }
            return update;
        }

        @Override
        public GuiSwingViewObjectPane.PreferencesForSplitEntry newSourceValue(int i) {
            return new GuiSwingViewObjectPane.PreferencesForSplitEntry();
        }

        @Override
        public void sourceAdded(int newIdx, GuiSwingViewObjectPane.PreferencesForSplitEntry v) {
            saveRunner.run();
        }

        @Override
        public void sourceRemoved(int[] removedIndices, List<GuiSwingViewObjectPane.PreferencesForSplitEntry> removed) {
            saveRunner.run();
        }
    }

    public static class ValueListSplitEntryItemPane extends JPanel {
        protected GuiSwingViewObjectPane.PreferencesForSplitEntry entry;
        protected LambdaProperty.LambdaNumberSpinner dividerLocationPane;
        protected LambdaProperty.LambdaBooleanCheckBox horizontalPane;
        protected Runnable saveRunner;

        @SuppressWarnings("this-escape")
        public ValueListSplitEntryItemPane(Runnable saveRunner) {
            this.saveRunner = saveRunner;
            entry = new GuiSwingViewObjectPane.PreferencesForSplitEntry();
            setLayout(new ResizableFlowLayout(true));
            dividerLocationPane = new LambdaProperty.LambdaNumberSpinner(Integer.class, this::getDividerLocation, this::setDividerLocationPane);
            horizontalPane = new LambdaProperty.LambdaBooleanCheckBox(SPLIT_PREFS_PROP_HORIZONTAL, this::isHorizontal, this::setHorizontal);
            ResizableFlowLayout.add(this, createNamedHorizontal(null, SPLIT_PREFS_PROP_DIVIDER, dividerLocationPane), true);
            ResizableFlowLayout.add(this, horizontalPane, false);
        }

        public GuiSwingViewObjectPane.PreferencesForSplitEntry getEntry() {
            return entry;
        }

        public int getDividerLocation() {
            return entry.dividerLocation;
        }

        public void setDividerLocationPane(int n) {
            boolean change = entry.dividerLocation != n;
            entry.dividerLocation = n;
            if (change) {
                saveRunner.run();
            }
        }

        public boolean isHorizontal() {
            return entry.horizontal;
        }

        public void setHorizontal(boolean b) {
            boolean change = entry.horizontal != b;
            entry.horizontal = b;
            if (change) {
                saveRunner.run();
            }
        }

        public void setEntry(GuiSwingViewObjectPane.PreferencesForSplitEntry entry) {
            if (entry == null) {
                entry = new GuiSwingViewObjectPane.PreferencesForSplitEntry();
            }
            this.entry = entry;
            dividerLocationPane.updateSwingViewSource();
            horizontalPane.updateSwingViewSource();
        }
    }

     public static class ValueListPaneFileDialogList extends ValueListPane<String, LambdaProperty.LambdaFilePathPane> {
        protected GuiSwingPrefsSupports.PreferencesForFileDialog prefsObj;
        protected Runnable saveRunner;

         @SuppressWarnings("this-escape")
         public ValueListPaneFileDialogList(GuiSwingPrefsSupports.PreferencesForFileDialog prefsObj, Runnable saveRunner) {
             super(false);
             this.prefsObj = prefsObj;
             this.saveRunner = saveRunner;
             setName(FILE_DIALOG_PREFS_PROP_FILE_LIST);
             init();
             syncElements();
         }

         @Override
         public List<String> takeSource() {
            return prefsObj == null ? null : nonNullList(prefsObj.getFileListDirect());
         }

         @Override
         public LambdaProperty.LambdaFilePathPane newElementPane(int i, ValueListElementPane<String, LambdaProperty.LambdaFilePathPane> elementPane) {
             return new LambdaProperty.LambdaFilePathPane(
                     () -> Paths.get(takeSource().get(elementPane.index())),
                     v -> setValue(elementPane, v.toString()));
         }

         public void setValue(ValueListElementPane<String, LambdaProperty.LambdaFilePathPane> elementPane, String v) {
             var src = takeSource();
             var oldVal = src.get(elementPane.index());
             boolean change = !Objects.equals(oldVal, v);
             if (change) {
                 src.set(elementPane.index(), v);
                 saveRunner.run();
             }
         }

         @Override
         public String newSourceValue(int i) {
             return "";
         }

         @Override
         public boolean removeSourceValue(int i, String value) {
             return true;
         }

         @Override
         public boolean updateSourceValueToElementPane(int i, String value, ValueListElementPane<String, LambdaProperty.LambdaFilePathPane> pane) {
             pane.contentPane().updateSwingViewSource();
             return true;
         }

         @Override
         public void sourceAdded(int newIndex, String v) {
             saveRunner.run();
         }

         @Override
         public void sourceRemoved(int[] removedIndices, List<String> removed) {
             saveRunner.run();
         }
     }

     public static class ValueListTableRowSort extends ValueListPane<GuiSwingViewCollectionTable.PreferencesForTableRowSort, ValueListTableRowSortItem> {
         protected Supplier<List<GuiSwingViewCollectionTable.PreferencesForTableRowSort>> source;
         protected Runnable saveRunner;

         @SuppressWarnings("this-escape")
         public ValueListTableRowSort(Supplier<List<GuiSwingViewCollectionTable.PreferencesForTableRowSort>> source, Runnable saveRunner) {
             super(false);
             this.source = source;
             this.saveRunner = saveRunner;
             setName(TABLE_PREFS_ROW_SORT);
             init();
             syncElements();
         }

         @Override
         public List<GuiSwingViewCollectionTable.PreferencesForTableRowSort> takeSource() {
             return nonNullList(source.get());
         }

         @Override
         public GuiSwingViewCollectionTable.PreferencesForTableRowSort newSourceValue(int i) {
             return new GuiSwingViewCollectionTable.PreferencesForTableRowSort(i, SortOrder.UNSORTED);
         }

         @Override
         public boolean updateSourceValueToElementPane(int i, GuiSwingViewCollectionTable.PreferencesForTableRowSort preferencesForTableRowSort, ValueListElementPane<GuiSwingViewCollectionTable.PreferencesForTableRowSort, ValueListTableRowSortItem> valueListElementPane) {
             return valueListElementPane.contentPane().setSort(preferencesForTableRowSort);
         }

         @Override
         public ValueListTableRowSortItem newElementPane(int i, ValueListElementPane<GuiSwingViewCollectionTable.PreferencesForTableRowSort, ValueListTableRowSortItem> valueListElementPane) {
             return new ValueListTableRowSortItem(saveRunner);
         }
         @Override
         public void sourceAdded(int newIndex, GuiSwingViewCollectionTable.PreferencesForTableRowSort v) {
             saveRunner.run();
         }

         @Override
         public void sourceRemoved(int[] removedIndices, List<GuiSwingViewCollectionTable.PreferencesForTableRowSort> removed) {
             saveRunner.run();
         }
     }

     public static class ValueListTableRowSortItem extends JComponent {
        protected GuiSwingViewCollectionTable.PreferencesForTableRowSort sort;
        protected GuiSwingViewNumberSpinner.PropertyNumberSpinner columnNumber;
        protected GuiSwingViewEnumComboBox.PropertyEnumComboBox orderEnum;
        protected Runnable saveRunner;

         @SuppressWarnings("this-escape")
         public ValueListTableRowSortItem(Runnable saveRunner) {
             this.saveRunner = saveRunner;
             setLayout(new ResizableFlowLayout(true));
             columnNumber = new LambdaProperty.LambdaNumberSpinner("column", Integer.class, this::getColumn, this::setColumn);
             orderEnum = new LambdaProperty.LambdaEnumComboBox("order", SortOrder.class, this::getOrder, this::setOrder);
             ResizableFlowLayout.add(this, new JLabel("column:"), false);
             ResizableFlowLayout.add(this, columnNumber, false);
             ResizableFlowLayout.add(this, new JLabel("order:"), false);
             ResizableFlowLayout.add(this, orderEnum, false);
         }

         public GuiSwingViewNumberSpinner.PropertyNumberSpinner getColumnNumber() {
             return columnNumber;
         }

         public GuiSwingViewEnumComboBox.PropertyEnumComboBox getOrderEnum() {
             return orderEnum;
         }

         public boolean setSort(GuiSwingViewCollectionTable.PreferencesForTableRowSort sort) {
             boolean change = !Objects.equals(this.sort, sort);
             this.sort = sort;
             if (change) {
                 columnNumber.updateSwingViewSource();
                 orderEnum.updateSwingViewSource();
             }
             return change;
         }

         public GuiSwingViewCollectionTable.PreferencesForTableRowSort getSort() {
             return sort;
         }

         public int getColumn() {
             return sort == null ? -1 : sort.getColumn();
         }

         public SortOrder getOrder() {
             return sort == null ? SortOrder.UNSORTED : SortOrder.valueOf(sort.getOrder());
         }

         public void setOrder(SortOrder o) {
             if (sort != null) {
                 boolean changed = !Objects.equals(sort.getOrder(), o.name());
                 if (changed) {
                     sort.setOrder(o.name());
                     saveRunner.run();
                 }
             }
         }

         public void setColumn(int index) {
             if (sort != null) {
                 boolean changed = sort.getColumn() != index;
                 if (changed) {
                     sort.setColumn(index);
                     saveRunner.run();
                 }
             }
         }
     }

    public static abstract class ValueListMapPane<Key,Value> extends ValueListPane<Map.Entry<Key,Value>, ValueListMapItemPane<Key, Value>> {
        protected Supplier<Map<Key, Value>> source;
        protected MapList<Key, Value> sourceWrapper;
        protected Runnable saveRunner;

        @SuppressWarnings("this-escape")
        public ValueListMapPane(Supplier<Map<Key, Value>> source, Runnable saveRunner) {
            super(false);
            this.source = source;
            this.saveRunner = saveRunner;
            takeSource();
            init();
            syncElements();
        }

        @Override
        public List<Map.Entry<Key, Value>> takeSource() {
            if (sourceWrapper == null || sourceWrapper.getSource() != source.get()) {
                var map = source.get();
                if (map == null) {
                    return new ArrayList<>();
                }
                sourceWrapper = new MapList<>(source.get());
            }
            return sourceWrapper;
        }

        public void updateSource() {
            sourceWrapper = null;
            takeSource();
        }

        public Map<Key, Value> takeSourceMap() {
            var map =  source.get();
            return map == null ? new HashMap<>() : map;
        }

        @Override
        public boolean updateSourceValueToElementPane(int i, Map.Entry<Key, Value> value, ValueListElementPane<Map.Entry<Key, Value>, ValueListMapItemPane<Key, Value>> pane) {
            return pane.contentPane().updateValue(value.getKey(), value.getValue());
        }

        @Override
        public void sourceAdded(int newIndex, Map.Entry<Key, Value> v) {
            if (saveRunner != null) {
                saveRunner.run();
            }
        }

        @Override
        public void sourceRemoved(int[] removedIndices, List<Map.Entry<Key, Value>> removed) {
            if (saveRunner != null) {
                saveRunner.run();
            }
        }

        public void sourceUpdated(int index, Map.Entry<Key, Value> v) {
            var old = takeSource().set(index, v);
            if (!Objects.equals(old.getKey(), v.getKey())) { //key change
                syncElements();
            }
            if (saveRunner != null) {
                saveRunner.run();
            }
        }
    }

    /**
     * a list wrapping a map; it supposes that the map never updated the outside of the list
     * @param <K> the map key
     * @param <V>  the map value
     */
    public static class MapList<K, V> extends AbstractList<Map.Entry<K, V>> {
        protected Map<K, V> source;
        protected List<K> keys;

        @SuppressWarnings("this-escape")
        public MapList(Map<K, V> source) {
            this.source = source;
            setKeysFromSource();
        }

        public void setKeysFromSource() {
            if (source != null) {
                keys = new ArrayList<>(source.keySet());
            } else {
                keys = new ArrayList<>();
            }
        }

        public Map<K, V> getSource() {
            return source;
        }

        public List<K> getKeys() {
            return keys;
        }

        public boolean inconsistentSource() {
            return source == null ? !keys.isEmpty() :
                    !new HashSet<>(keys).equals(new HashSet<>(source.keySet()));
        }

        @Override
        public Map.Entry<K, V> get(int index) {
            var k = keys.get(index);
            return Map.entry(k, source.get(k));
        }

        @Override
        public int size() {
            return keys.size(); //==source.size()
        }

        @Override
        public void add(int index, Map.Entry<K, V> element) {
            if (source == null) {
                source = new LinkedHashMap<>();
            }
            if (!source.containsKey(element.getKey())) { //new key
                keys.add(index, element.getKey());
            }
            source.put(element.getKey(), element.getValue());
        }

        @Override
        public Map.Entry<K, V> remove(int index) {
            var k = keys.remove(index);
            var v = source.remove(k);
            return Map.entry(k, v);
        }

        @Override
        public Map.Entry<K, V> set(int index, Map.Entry<K, V> element) {
            if (index < 0 || index >= size()) {
                throw new IndexOutOfBoundsException(index);
            } else {
                var ek = keys.get(index);
                V ev;
                boolean diffKey = !ek.equals(element.getKey());
                if (source.containsKey(ek) && diffKey) { //different key: remove the existing entry
                    ev = source.remove(ek);
                    keys.remove(index);
                } else {
                    ev = source.get(ek);
                }
                source.put(element.getKey(), element.getValue());
                if (diffKey) {
                    if (!keys.contains(element.getKey())) { //new key
                        keys.add(index, element.getKey());
                    }
                }
                return Map.entry(ek, ev);
            }
        }
    }

    public static class ValueListMapItemPane<Key, Value> extends JComponent {
        protected Key key;
        protected Value value;
        protected JComponent keyComponent;
        protected JComponent valueComponent;
        protected ValueListMapPane<Key, Value> owner;

        @SuppressWarnings("this-escape")
        public ValueListMapItemPane(ValueListMapPane<Key, Value> owner) {
            init(owner);
        }

        protected void init(ValueListMapPane<Key, Value> owner) {
            this.owner = owner;
            UIManagerUtil u = UIManagerUtil.getInstance();
            var w = u.getScaledSizeInt(5);
            var h = u.getScaledSizeInt(10);
            setLayout(new BorderLayout(w, h));
            keyComponent = initKeyComponent(owner);
            add(keyComponent, BorderLayout.WEST);
            valueComponent = initValueComponent(owner);
            add(valueComponent, BorderLayout.CENTER);
        }

        protected JComponent initKeyComponent(ValueListMapPane<Key, Value> owner) {
            return createLabel("");
        }

        protected JComponent initValueComponent(ValueListMapPane<Key, Value> owner) {
            JLabel label = new JLabel();
            label.setFont(UIManagerUtil.getInstance().getConsoleFont());
            return label;
        }

        public static JLabel createLabel(String text) {
            var nameLabel = new JLabel(text);
            nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            nameLabel.setFont(UIManagerUtil.getInstance().getConsoleFont());
            return nameLabel;
        }

        public boolean updateValue(Key key, Value value) {
            boolean updated = !Objects.equals(this.key, key) || !Objects.equals(this.value, value);
            if (updated) {
                this.key = key;
                this.value = value;
                updateValueForce(key, value);
            }
            return updated;
        }

        public void updateValueForce(Key key, Value value) {
            if (keyComponent instanceof JLabel keyLabel) {
                keyLabel.setText(Objects.toString(key));
            }
            if (valueComponent instanceof JLabel label) {
                label.setText(Objects.toString(value));
            }
        }

        public Key getKey() {
            return key;
        }

        public void setKey(Key key) {
            boolean diff = !Objects.equals(this.key, key);
            this.key = key;
            if (diff) {
                ownerUpdate();
            }
        }

        public void ownerUpdate() {
            if (owner != null) {
                var elemPane = owner.upperElement(this);
                if (elemPane != null) {
                    owner.sourceUpdated(elemPane.index(), Map.entry(key, value));
                }
            }
        }

        public Value getValue() {
            return value;
        }

        public void setValue(Value value) {
            boolean diff = !Objects.equals(this.value, value);
            this.value = value;
            if (diff) {
                ownerUpdate();
            }
        }
    }

    public static class ValueListMapPaneForTableColumnWidthStatic extends ValueListMapPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth> {
        public ValueListMapPaneForTableColumnWidthStatic(Supplier<Map<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth>> source, Runnable saveRunner) {
            super(source, saveRunner);
        }

        @Override
        public Map.Entry<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth> newSourceValue(int i) {
            var ks = takeSourceMap().keySet();
            var n = ks.stream().max(Comparator.naturalOrder())
                    .orElse(-1) + 1;
            return Map.entry(n, new GuiSwingTableModelCollection.PreferencesForTableColumnWidth());
        }

        @Override
        public ValueListMapItemPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth> newElementPane(int i, ValueListElementPane<Map.Entry<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth>, ValueListMapItemPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth>> elementPane) {
            return new ValueListMapItemPaneForTableColumnWidthStatic(this);
        }
    }

    public static class ValueListMapItemPaneForTableColumnWidthStatic extends ValueListMapItemPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth> {
        protected ValueListMapPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth> owner;
        protected Runnable updater;
        protected LambdaProperty.LambdaNumberSpinner keyPane;
        protected LambdaProperty.LambdaNumberSpinner valuePane;

        public ValueListMapItemPaneForTableColumnWidthStatic(GuiSwingTableModelCollection.PreferencesForTableColumnWidth prefsObj, Runnable updater) {
            super(null);
            this.value = prefsObj;
            this.updater = updater;
        }
        public ValueListMapItemPaneForTableColumnWidthStatic(ValueListMapPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth> owner) {
            super(owner);
            this.owner = owner;
        }

        @Override
        protected JComponent initKeyComponent(ValueListMapPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth> owner) {
            key = -1;
            keyPane = new LambdaProperty.LambdaNumberSpinner(TABLE_PREFS_COLUMN_INDEX, Integer.class, this::getKey, this::setKey);
            return createNamedHorizontal(null, TABLE_PREFS_COLUMN_INDEX, keyPane);
        }

        @Override
        protected JComponent initValueComponent(ValueListMapPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth> owner) {
            value = new GuiSwingTableModelCollection.PreferencesForTableColumnWidth();
            valuePane = new LambdaProperty.LambdaNumberSpinner(TABLE_PREFS_COLUMN_WIDTH, Integer.class, this::getValueWidth, this::setValueWidth);
            return createNamedHorizontal(null, TABLE_PREFS_COLUMN_WIDTH, valuePane);
        }

        public int getValueWidth() {
            return value.getWidth();
        }

        public void setValueWidth(int w) {
            boolean diff = this.value.getWidth() != w;
            this.value.setWidth(w);
            if (diff) {
                ownerUpdate();
            }
        }

        @Override
        public void ownerUpdate() {
            super.ownerUpdate();
            if (updater != null) {
                updater.run();
            }
        }

        @Override
        public void updateValueForce(Integer integer, GuiSwingTableModelCollection.PreferencesForTableColumnWidth prefs) {
            keyPane.updateSwingViewSource();
            valuePane.updateSwingViewSource();
        }
    }

    public static class ValueListMapPaneForTableColumnOrder extends ValueListMapPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder> {
        public ValueListMapPaneForTableColumnOrder(Supplier<Map<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder>> source, Runnable saveRunner) {
            super(source, saveRunner);
        }

        @Override
        public Map.Entry<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder> newSourceValue(int i) {
            var ks = takeSourceMap().keySet();
            var n = ks.stream().max(Comparator.naturalOrder())
                    .orElse(-1) + 1;
            return Map.entry(n, new GuiSwingTableModelCollection.PreferencesForTableColumnOrder());
        }

        @Override
        public ValueListMapItemPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder> newElementPane(int i, ValueListElementPane<Map.Entry<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder>, ValueListMapItemPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder>> elementPane) {
            return new ValueListMapItemPaneForTableColumnOrder(this);
        }
    }

    public static class ValueListMapItemPaneForTableColumnOrder extends ValueListMapItemPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder> {
        protected ValueListMapPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder> owner;
        protected Runnable updater;
        protected LambdaProperty.LambdaNumberSpinner keyPane;

        public ValueListMapItemPaneForTableColumnOrder(GuiSwingTableModelCollection.PreferencesForTableColumnOrder value, Runnable updater) {
            super(null);
            this.value = value;
            this.updater = updater;
        }
        public ValueListMapItemPaneForTableColumnOrder(ValueListMapPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder> owner) {
            super(owner);
            this.owner = owner;
        }

        @Override
        protected JComponent initKeyComponent(ValueListMapPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder> owner) {
            key = -1;
            keyPane = new LambdaProperty.LambdaNumberSpinner(TABLE_PREFS_COLUMN_INDEX, Integer.class, this::getKey, this::setKey);
            return createNamedHorizontal(null, TABLE_PREFS_COLUMN_INDEX, keyPane);
        }

        @Override
        protected JComponent initValueComponent(ValueListMapPane<Integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder> owner) {
            value = new GuiSwingTableModelCollection.PreferencesForTableColumnOrder();
            var pane = createPane(true);
            {
                ResizableFlowLayout.add(pane, createNumberInt(null, TABLE_PREFS_COLUMN_ORDER_MODEL_INDEX, this::getValueModelIndex, this::setValueModelIndex, null), false);
                ResizableFlowLayout.add(pane, createNumberInt(null, TABLE_PREFS_COLUMN_ORDER_VIEW_INDEX, this::getValueViewIndex, this::setValueViewIndex, null), false);
            }
            return pane;
        }

        public int getValueModelIndex() {
            return value.getModelIndex();
        }

        @Override
        public void ownerUpdate() {
            super.ownerUpdate();
            if (updater != null) {
                updater.run();
            }
        }

        public void setValueModelIndex(int modelIndex) {
            boolean diff = value.getModelIndex() != modelIndex;
            value.setModelIndex(modelIndex);
            if (diff) {
                ownerUpdate();
            }
        }

        public int getValueViewIndex() {
            return value.getViewIndex();
        }

        public void setValueViewIndex(int viewIndex) {
            boolean diff = value.getViewIndex() != viewIndex;
            value.setViewIndex(viewIndex);
            if (diff) {
                ownerUpdate();
            }
        }

        @Override
        public void updateValueForce(Integer integer, GuiSwingTableModelCollection.PreferencesForTableColumnOrder preferencesForTableColumnOrder) {
            keyPane.updateSwingViewSource();
            GuiSwingView.collectNonNullByFunction(this.valueComponent, Function.identity())
                    .forEach(GuiSwingView.ValuePane::updateSwingViewSource);
        }
    }

    public static <T> List<T> nonNullList(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }

    public static class ValueListPaneHistoryValueEntry extends ValueListPane<GuiPreferences.HistoryValueEntry, GuiSwingPrefsHistoryValues.HistoryPaneResult> {
        protected Supplier<List<GuiPreferences.HistoryValueEntry>> source;
        protected GuiPreferences prefs;
        protected GuiSwingPrefsHistoryValues factory;
        protected Runnable saveRunner;

        @SuppressWarnings("this-escape")
        public ValueListPaneHistoryValueEntry(Supplier<List<GuiPreferences.HistoryValueEntry>> source, GuiPreferences prefs, Runnable saveRunner) {
            super(false);
            this.source = source;
            this.prefs = prefs;
            factory = new GuiSwingPrefsHistoryValues();
            this.saveRunner = saveRunner;
            init();
            syncElements();
        }

        @Override
        public List<GuiPreferences.HistoryValueEntry> takeSource() {
            return source == null ? new ArrayList<>() : nonNullList(source.get());
        }

        @Override
        public void afterUpdateElements() {
            boolean hasFree = (prefs.getHistoryValues().size() < prefs.getHistoryValueLimit());
            addAction.setEnabled(hasFree);
        }

        @Override
        public GuiPreferences.HistoryValueEntry newSourceValue(int i) {
            boolean hasFree = (prefs.getHistoryValues().size() < prefs.getHistoryValueLimit());
            return hasFree ? prefs.getHistoryValueFree() : null;
        }

        @Override
        public GuiSwingPrefsHistoryValues.HistoryPaneResult newElementPane(int i, ValueListElementPane<GuiPreferences.HistoryValueEntry, GuiSwingPrefsHistoryValues.HistoryPaneResult> elementPane) {
            return factory.createHistoryObjectPrefs(null, prefs, false)
                    .withGuiToSourceUpdater(v -> updateGuiToSource(elementPane, v));
        }

        public void updateGuiToSource(ValueListElementPane<GuiPreferences.HistoryValueEntry, GuiSwingPrefsHistoryValues.HistoryPaneResult> elementPane, Object v) {
            var e = elementPane.value();
            if (e != null) {
                boolean changed = !Objects.equals(e.getValue(), v);
                e.setValue(v);
                if (changed) {
                    saveRunner.run();
                }
            }
        }

        @Override
        public boolean updateSourceValueToElementPane(int i, GuiPreferences.HistoryValueEntry value, ValueListElementPane<GuiPreferences.HistoryValueEntry, GuiSwingPrefsHistoryValues.HistoryPaneResult> pane) {
            return pane.contentPane().withEntry(value).lastEntryUpdated();
        }

        @Override
        public void sourceMoved(int insertedTargetIndex, List<GuiPreferences.HistoryValueEntry> movedValues) {
            prefs.syncHistoryValues(true);
            saveRunner.run();
        }

        @Override
        public void sourceRemoved(int[] removedIndices, List<GuiPreferences.HistoryValueEntry> removed) {
            prefs.removeHistories(removed);
            saveRunner.run();
        }

        @Override
        public void sourceAdded(int newIndex, GuiPreferences.HistoryValueEntry v) {
            prefs.syncHistoryValues(true);
            saveRunner.run();
        }
    }

    public static class RevertBackupAction extends AbstractAction {
        protected GuiSwingPrefsEditor editor;
        @SuppressWarnings("this-escape")
        public RevertBackupAction(GuiSwingPrefsEditor editor) {
            this.editor = editor;
            putValue(NAME, "Revert");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            editor.revertBackupPrefs();
        }
    }
}
