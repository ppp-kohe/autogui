package org.autogui.swing;

import org.autogui.base.JsonReader;
import org.autogui.base.JsonWriter;
import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.ScheduledTaskRunner;
import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.prefs.GuiSwingPrefsApplyOptions;
import org.autogui.swing.prefs.GuiSwingPrefsEditor;
import org.autogui.swing.prefs.GuiSwingPrefsSupports;
import org.autogui.swing.prefs.GuiSwingPrefsTrees;
import org.autogui.swing.table.ObjectTableColumnValue;
import org.autogui.swing.util.*;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * a preferences manager.
 *
 *
 * <h2>GUI properties as preferences</h2>
 *
 *  <ol>
 *      <li>define a custom {@link GuiSwingPrefsSupports.PreferencesByJsonEntry} (or {@link GuiSwingPrefsSupports.Preferences}).
 *         <pre>
 *       class PreferencesForX implements PreferencesByJsonEntry {
 *           protected int prop; //a GUI property
 *
 *           public void applyTo(P guiComp) { //the method for setting the property of the component
 *               guiComp.setProp(prop);
 *           }
 *
 *           public void set(P guiComp) { //from the component to the prefs
 *               prop = guiComp.getProp();
 *           }
 *
 *           public String getKey() { return "$x"; } //the entry key
 *           public Object toJson() { Map m = new HashMap(); m.put("p", prop); return m; } //to a JSON entry
 *           public void setJson(Object j) {
 *               if (j != null &amp;&amp; j instanceof Map) { prop = (Integer) ((Map) j).get("p"); }
 *           }
 *       }
 *         </pre>
 *
 *      </li>
 *      <li> a GUI component defines a listener for incremental updating of properties
 *        <pre>
 *       class PropUpdater implements XListener {
 *           GuiMappingContext context;
 *           boolean enabled = true;
 *           Consumer&lt;{@link GuiSwingPrefsSupports.PreferencesUpdateEvent}&gt; updater;
 *           PreferencesForX prefs = new PreferencesForX();
 *
 *           void changed(Event e) { //suppose an event handler
 *              if (enabled) {
 *               prefs.set(e.getComponent());
 *               updater.accept(new PreferencesUpdateEvent(context, prefs);
 *              }
 *           }
 *       }
 *        </pre>
 *        and the GUI component sets up the listener with implementing {@link GuiSwingPrefsSupports.PreferencesUpdateSupport}
 *         <pre>
 *       class P implements ValuePane, PreferencesUpdateSupport {
 *            PropUpdater updater;
 *            P(GuiMappingContext c) {
 *                ...
 *                 updater = new PropUpdater(c);
 *                 addXLister(updater);
 *            }
 *            public void setPreferencesUpdater(Consumer&lt;PreferencesUpdateEvent&gt; u) {
 *                 updater.updater = u;
 *            }
 *        }
 *         </pre>
 *      </li>
 *      <li> the GUI component overrides
 *          {@link GuiSwingView.ValuePane#saveSwingPreferences(GuiPreferences)} and
 *           {@link GuiSwingView.ValuePane#loadSwingPreferences(GuiPreferences, GuiSwingPrefsApplyOptions)} for bulk loading/saving.
 *
 *       <pre>
 *            public void loadSwingPreferences(GuiPreferences p, GuiSwingPrefsApplyOptions options) {
 *                GuiSwingView.loadPreferencesDefault(this, p, options);
 *                updater.prefs.loadFrom(p.getDescendant(context));
 *                updater.enabled = false;
 *                updater.prefs.applyTo(this);
 *                updater.enabled = true;
 *            }
 *            public void saveSwingPreferences(GuiPreferences p) {
 *                GuiSwingView.savePreferencesDefault(this, p);
 *                updater.prefs.saveTo(p.getDescendant(context));
 *            }
 *       </pre>
 *
 *      </li>
 *  </ol>
 */
public class GuiSwingPreferences {
    protected JPanel mainPane;
    protected GuiMappingContext rootContext;
    protected RootView rootPane;
    protected JComponent rootComponent;
    protected JTable list;
    protected PreferencesListModel listModel;
    /* @since 1.3 */
    protected JTree contentTree;
    protected JComponent settingsPane;
    protected GuiSwingPrefsSupports.WindowPreferencesUpdater prefsWindowUpdater;

    protected SettingsWindow settingsWindow;

    protected int[] lastSelection = new int[0];

    protected Map<GuiPreferences, GuiSwingPrefsEditor> settingsEditors = new HashMap<>();

    public interface RootView {
        GuiMappingContext getContext();
        JComponent getViewComponent();
        default void loadPreferences(GuiPreferences prefs) {
            loadPreferences(prefs, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
        }

        /**
         * @param prefs the source prefs
         * @param options options for applying
         *                ({@link GuiSwingPrefsApplyOptions#APPLY_OPTIONS_DEFAULT} for {@link #loadPreferences(GuiPreferences)} that are all false)
         * @since 1.4
         */
        void loadPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options);
        void savePreferences(GuiPreferences prefs);

    }

    @SuppressWarnings("this-escape")
    public GuiSwingPreferences(RootView rootPane) {
        setRootView(rootPane);
        init();
        initRunner();
    }

    @SuppressWarnings("this-escape")

    public GuiSwingPreferences(GuiMappingContext rootContext, JComponent rootComponent) {
        this.rootContext = rootContext;
        this.rootComponent = rootComponent;
        init();
        initRunner();
    }

    public void setRootView(RootView rootPane) {
        this.rootContext = rootPane.getContext();
        this.rootComponent = rootPane.getViewComponent();
        this.rootPane = rootPane;
    }

    protected void init() {
        initMainPane();
        initToolBar();
        initSettingPrefs();
    }

    protected void initMainPane() {
        mainPane = new JPanel(new BorderLayout());
        {
            var listScroll = initList();
            var viewPane = initContentTree();
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, viewPane);
            mainPane.add(splitPane, BorderLayout.CENTER);
        }
    }

    protected JComponent initList() {
        listModel = new PreferencesListModel(this::getRootContext);
        listModel.addTableModelListener(e -> {
            if (e.getColumn() == PreferencesListModel.COLUMN_LAUNCH) {
                lastSelection = new int[] {e.getLastRow()};
            }
            SwingDeferredRunner.invokeLater(this::restoreSelection);
        });
        list = new JTable(listModel);
        TableColumn column = list.getColumnModel().getColumn(0);
        {
            column.setHeaderValue("Saved Preferences");
            column.setCellRenderer(new PreferencesNameRenderer(listModel));
            column.setCellEditor(new PreferencesNameEditor(listModel));
        }

        TableColumn columnSelect = list.getColumnModel().getColumn(1);
        {
            columnSelect.setHeaderValue("Apply at Launch");
            columnSelect.setCellRenderer(new PreferencesLaunchApplyRenderer(listModel));
            columnSelect.setCellEditor(new PreferencesLaunchApplyEditor(listModel));
        }
        addSelectionListener(this::showSelectedPrefs);
        list.setAutoCreateColumnsFromModel(false); //avoid to reset above columns

        UIManagerUtil ui = UIManagerUtil.getInstance();

        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setPreferredSize(new Dimension(ui.getScaledSizeInt(300), ui.getScaledSizeInt(300)));
        list.setRowHeight(ui.getScaledSizeInt(20));

        return listScroll;
    }

    protected JComponent initContentTree() {
        JTabbedPane viewPane = new JTabbedPane(JTabbedPane.BOTTOM);
        {
            settingsPane = new JPanel(new BorderLayout());
            {
                settingsPane.setOpaque(false);
                var setttingsMainPane = new JPanel();
                setttingsMainPane.setOpaque(false);
                settingsPane.add(setttingsMainPane, BorderLayout.CENTER);
            }
            viewPane.addTab("Settings", settingsPane);

            contentTree = GuiSwingPrefsTrees.getInstance().createTree();
            {
                viewPane.addChangeListener(e -> updateContentTree());
            }
            viewPane.addTab("Tree", new JScrollPane(contentTree));

        }
        return viewPane;
    }

    protected void initToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOpaque(false);
        toolBar.add(actionButton(new NewPrefsAction(this)));
        toolBar.add(actionButton(new UpdatePrefsAction(this)));
        toolBar.add(actionButton(new DeletePrefsAction(this)));
        toolBar.add(actionButton(new DuplicatePrefsAction(this)));
        toolBar.add(actionButton(new ApplyPrefsAction(this)));
        toolBar.add(actionButton(new SavePrefsAction(this)));
        toolBar.add(actionButton(new LoadPrefsAction(this)));
        //toolBar.add(actionButton(new ResetPrefsAction(this)));
        new ToolBarHiddenMenu().addTo(toolBar);
        mainPane.add(toolBar, BorderLayout.PAGE_START);
    }

    protected void initSettingPrefs() {
        prefsWindowUpdater = new GuiSwingPrefsSupports.WindowPreferencesUpdater(null, rootContext, "$preferencesWindow");
    }

    private JButton actionButton(Action action) {
        return new GuiSwingIcons.ActionButton(action);
    }

    public void show(JComponent sender) {
        (settingsWindow == null ? SettingsWindow.get() : settingsWindow)
                .show("Preferences", sender, mainPane, prefsWindowUpdater);
    }

    public void setSettingsWindow(SettingsWindow settingsWindow) {
        this.settingsWindow = settingsWindow;
    }

    public GuiMappingContext getRootContext() {
        return rootContext;
    }

    public boolean isEmpty(GuiPreferences prefs) {
        return listModel.isEmpty(prefs);
    }

    public void reloadList() {
        listModel.reload();
    }

    public List<GuiPreferences> getSelectedSavedPreferencesList() {
        return IntStream.of(list.getSelectedRows())
                .map(list::convertRowIndexToModel)
                .mapToObj(listModel.getList()::get)
                .collect(Collectors.toList());
    }

    public GuiPreferences getSelectedSavedPreferences() {
        int i = list.convertRowIndexToView(list.getSelectedRow());
        if (i >= 0) {
            return listModel.getList().get(i);
        } else {
            return null;
        }
    }

    public JTable getList() {
        return list;
    }

    public void addSelectionListener(Runnable r) {
        list.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                r.run();
            }
        });
    }

    public boolean isSelectionEmpty() {
        return list.getSelectionModel().isSelectionEmpty();
    }

    public JPanel getMainPane() {
        return mainPane;
    }

    public void applyPreferences() {
        applyPreferences(GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
    }

    /**
     * @param options options for applying
     * @since 1.4
     */
    public void applyPreferences(GuiSwingPrefsApplyOptions options) {
        GuiPreferences prefs = rootContext.getPreferences();
        try (var lock = prefs.lock()) {
            lock.use();
            if (rootPane != null) {
                rootPane.loadPreferences(prefs, options);
            } else {
                if (rootComponent instanceof GuiSwingView.ValuePane<?> valuePane) {
                    if (!options.isSkippingValue()) {
                        valuePane.loadSwingPreferences(prefs, options);
                    }
                }
                GuiSwingView.loadChildren(prefs, rootComponent, options);
            }
        }
    }

    public void savePreferences(GuiPreferences prefs) {
        if (rootPane != null) {
            rootPane.savePreferences(prefs);
        } else {
            if (rootComponent instanceof GuiSwingView.ValuePane<?> valuePane) {
                valuePane.saveSwingPreferences(prefs);
            }
            GuiPreferences rootPrefs = rootContext.getPreferences();
            try (var lock = rootPrefs.lock()) {
                lock.use();
                GuiSwingView.saveChildren(rootPrefs, rootComponent);
            }
        }
    }

    public GuiSwingPrefsSupports.WindowPreferencesUpdater getPrefsWindowUpdater() {
        return prefsWindowUpdater;
    }

    public void restoreSelection() {
        ListSelectionModel m = list.getSelectionModel();
        int size = list.getRowCount();
        m.setValueIsAdjusting(true);
        IntStream.of(lastSelection)
                .filter(i -> 0 <= i && i < size)
                .forEach(i -> m.addSelectionInterval(i, i));
        m.setValueIsAdjusting(false);
    }

    public void showSelectedPrefs() {
        int[] sels = list.getSelectedRows();
        if (sels.length > 0) {
            lastSelection = sels;
        }
        var t = GuiSwingPrefsTrees.getInstance();
        var prefs = getSelectedSavedPreferences();
        if (prefs != null) {
            prefs.load();
            var editor = settingsEditors.computeIfAbsent(prefs, this::createSettingsPane);
            settingsPane.removeAll();
            settingsPane.add(editor.getRootPane(), BorderLayout.CENTER);
            settingsPane.revalidate();
            settingsPane.repaint();

            updateContentTree();
        }
    }

    public GuiSwingPrefsEditor createSettingsPane(GuiPreferences prefs) {
        var e = new GuiSwingPrefsEditor();
        e.create(this.rootPane, prefs);
        return e;
    }

    public void updateContentTree() {
        var t = GuiSwingPrefsTrees.getInstance();
        var prefs = getSelectedSavedPreferencesList();
        t.setTreeModel(contentTree, t.createTreeNode(prefs));
    }

    public void shutdown() {
        getUpdater().shutdown();
        ScheduledExecutorService s = getUpdater().getExecutor();
        try {
            s.awaitTermination(2, TimeUnit.SECONDS);
        } catch (Exception ex) {
            GuiLogManager.get().logError(ex);
        }
    }

    /////////////

    public GuiPreferences getLaunchPreferences() {
        return listModel.getLaunchPrefs();
    }

    public List<GuiPreferences> getSavedPreferencesList() {
        return listModel.getSavedPrefsList();
    }

    public String getName(GuiPreferences prefs) {
        return listModel.getName(prefs);
    }

    /////////////

    public static class PreferencesListModel extends AbstractTableModel {
        @Serial private static final long serialVersionUID = 1L;
        protected Supplier<GuiMappingContext> rootContext;
        protected List<GuiPreferences> list;
        protected List<GuiPreferences> savedPrefsList;
        protected GuiPreferences launchPrefs;
        protected GuiPreferences lastDefault;
        protected GuiPreferences targetDefault; //prefs is "empty" and thus using default values of context objects

        public static int COLUMN_SIZE = 2;
        public static int COLUMN_NAME = 0;
        public static int COLUMN_LAUNCH = 1;

        @SuppressWarnings("this-escape")
        public PreferencesListModel(Supplier<GuiMappingContext> rootContext) {
            this.rootContext = rootContext;
            reload();
        }

        @Override
        public int getRowCount() {
            return list.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_SIZE;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            GuiPreferences prefs = list.get(rowIndex);
            if (columnIndex == COLUMN_NAME) {
                return getName(prefs);
            } else if (columnIndex == COLUMN_LAUNCH) {
                return isLaunchPrefs(prefs);
            } else {
                return "?";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            GuiPreferences prefs = list.get(rowIndex);
            if (columnIndex == COLUMN_NAME) {
                setName(prefs, (String) aValue);
                reload();
            } else if (columnIndex == COLUMN_LAUNCH) {
                if (aValue.equals(Boolean.TRUE)) {
                    setLaunchPrefs(prefs);
                }
                reload();
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex == COLUMN_NAME) {
                return isNameEditable(list.get(rowIndex));
            } else return columnIndex == COLUMN_LAUNCH;
        }

        public GuiPreferences getLaunchPrefs() {
            if (launchPrefs == null) {
                return rootContext.get().getPreferences();
            }
            return launchPrefs;
        }

        public boolean isLaunchPrefs(GuiPreferences prefs) {
            return launchPrefs == prefs ||
                    (launchPrefs == null && isDefault(prefs)) ||
                    (isDefault(launchPrefs) && isDefault(prefs)) ||
                    (isDefault(launchPrefs) && prefs == null);
        }

        public void reload() {
            update(true);
            /*
            int diff = oldSize - list.size();
            fireContentsChanged(this, 0, Math.min(oldSize - 1, list.size() - 1));
            if (diff > 0) { //removed
                fireIntervalRemoved(this, list.size(), oldSize - 1);
            }
            if (diff < 0) { //added
                fireIntervalAdded(this, oldSize, list.size() - 1);
            }*/
        }

        public List<GuiPreferences> getList() {
            return list;
        }

        public List<GuiPreferences> getSavedPrefsList() {
            return savedPrefsList;
        }

        public boolean isNameEditable(GuiPreferences prefs) {
            return !isEmpty(prefs) && !isDefault(prefs);
        }

        public String getName(GuiPreferences prefs) {
            if (isDefault(prefs)) {
                return "Current";
            } else {
                return prefs.getValueStore().getString(GuiPreferences.KEY_NAME, "Preferences " + list.indexOf(prefs));
            }
        }

        public boolean isDefault(GuiPreferences prefs) {
            return prefs == lastDefault ||
                    prefs == rootContext.get().getPreferences();
        }

        public boolean isEmpty(GuiPreferences prefs) {
            return prefs == targetDefault;
        }

        public String getUUID(GuiPreferences prefs) {
            if (prefs == null || isDefault(prefs)) {
                return "";
            } else if (isEmpty(prefs)) {
                return "empty";
            } else {
                String v = prefs.getValueStore().getString(GuiPreferences.KEY_UUID, "");
                if (v.isEmpty()) {
                    v = UUID.randomUUID().toString();
                    prefs.getValueStore().putString(GuiPreferences.KEY_UUID, v);
                }
                return v;
            }
        }

        public void setLaunchPrefs(GuiPreferences launchPrefs) {
            int current = list.indexOf(getLaunchPrefs());
            this.launchPrefs = launchPrefs;
            int newValue = list.indexOf(launchPrefs);
            saveLaunchPrefsUUID();
            fireTableCellUpdated(current, 1);
            fireTableCellUpdated(newValue, 1);
        }

        protected void saveLaunchPrefsUUID() {
            GuiPreferences prefs = rootContext.get().getPreferences();
            try (var lock = prefs.lock()) {
                lock.use();
                prefs.setLaunchPrefsAsRoot(getUUID(launchPrefs));
            }
        }

        public void setName(GuiPreferences prefs, String name) {
            prefs.getValueStore().putString(GuiPreferences.KEY_NAME, name);
            update(false);
        }

        public void update(boolean loadSavedList) {
            GuiMappingContext context = this.rootContext.get();
            lastDefault = context.getPreferences();
            try (var lock = lastDefault.lock()) {
                lock.use();
                lastDefault.load();
                initEmptyPrefs(context);
                initList(loadSavedList);
                initLaunchPrefs();
            }
            fireTableDataChanged();
        }

        protected void initEmptyPrefs(GuiMappingContext context) {
            targetDefault = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
            targetDefault.getValueStore().putString(GuiPreferences.KEY_NAME, "Empty");
            targetDefault.getValueStore().putString(GuiPreferences.KEY_UUID, "empty");
        }

        protected void initList(boolean loadSavedList) {
            list = new ArrayList<>();
            list.add(lastDefault); //0
            list.add(targetDefault); //1
            if (savedPrefsList == null || loadSavedList) {
                savedPrefsList = lastDefault.getSavedStoreListAsRoot();
            }
            savedPrefsList.sort(Comparator.comparing(this::getName)); //sort by updated names
            list.addAll(savedPrefsList);
        }

        protected void initLaunchPrefs() {
            String launchPrefsUUID = lastDefault.getLaunchPrefsAsRoot();
            if (launchPrefsUUID.isEmpty()) { //default
                launchPrefs = lastDefault;
            } else if (launchPrefsUUID.equals("empty")) {
                launchPrefs = targetDefault;
            } else {
                launchPrefs = savedPrefsList.stream()
                        .filter(p -> p.getValueStore().getString(GuiPreferences.KEY_UUID, "").equals(launchPrefsUUID))
                        .findFirst()
                        .orElse(targetDefault);
            }
        }
    }

    public static class PreferencesNameRenderer extends DefaultTableCellRenderer {
        @Serial private static final long serialVersionUID = 1L;
        protected PreferencesListModel listModel;

        public PreferencesNameRenderer(PreferencesListModel listModel) {
            this.listModel = listModel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof GuiPreferences prefs) {
                value = listModel.getName(prefs);
            }
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return this;
        }
    }

    public static class PreferencesNameEditor extends DefaultCellEditor {
        @Serial private static final long serialVersionUID = 1L;
        protected PreferencesListModel listModel;

        public PreferencesNameEditor(PreferencesListModel listModel) {
            super(new JTextField());
            this.listModel = listModel;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof GuiPreferences prefs) {
                value = listModel.getName(prefs);
            }
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

    public static class PreferencesLaunchApplyRenderer extends JCheckBox implements TableCellRenderer {
        @Serial private static final long serialVersionUID = 1L;
        protected PreferencesListModel listModel;

        @SuppressWarnings("this-escape")
        public PreferencesLaunchApplyRenderer(PreferencesListModel listModel) {
            this.listModel = listModel;
            setupCheckBox(this);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Boolean) {
                setSelected((Boolean) value);
            } else if (value instanceof GuiPreferences) {
                setSelected(listModel.isLaunchPrefs((GuiPreferences) value));
            }
            ObjectTableColumnValue.setTableColor(table, this, isSelected, hasFocus, row, column);
            return this;
        }
    }

    public static void setupCheckBox(JCheckBox box) {
        box.setHorizontalAlignment(SwingConstants.CENTER);
        box.setOpaque(true);
        box.setText("");
    }

    public static class PreferencesLaunchApplyEditor extends DefaultCellEditor {
        @Serial private static final long serialVersionUID = 1L;
        protected PreferencesListModel listModel;

        @SuppressWarnings("this-escape")
        public PreferencesLaunchApplyEditor(PreferencesListModel listModel) {
            super(new JCheckBox());
            this.listModel = listModel;
            setupCheckBox((JCheckBox) getComponent());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof GuiPreferences) {
                value = listModel.isLaunchPrefs((GuiPreferences) value);
            }
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

    ///////////////////

    public void doNewPrefs() {
        GuiPreferences rootPrefs = getRootContext().getPreferences();
        try (var lock = rootPrefs.lock()) {
            lock.use();
            GuiPreferences newStore = rootPrefs.addNewSavedStoreAsRoot();
            savePreferences(newStore);
            newStore.getValueStore().flush();
        }
        reloadList();
    }

    public void doUpdatePrefs() {
        if (!isSelectedPreferencesEditable()) {
            return;
        }
        GuiPreferences pref = getSelectedSavedPreferences();
        if (pref == null) {
            return;
        }
        try (var lock = pref.lock()) {
            lock.use();
            savePreferences(pref);
            pref.getValueStore().flush();
        }
        reloadList();
    }

    public void doDeletePrefs() {
        int r = JOptionPane.showConfirmDialog(getMainPane(),
                "Delete Selected Preferences ?", "Delete Preferences", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            for (GuiPreferences p : getSelectedSavedPreferencesList()) {
                p.clearAll();
                settingsEditors.remove(p);
            }
            reloadList();
        }
    }

    public boolean isSelectedPreferencesEditable() {
        return getSelectedSavedPreferences() != null && !isEmpty(getSelectedSavedPreferences());
    }

    public boolean isSelectedPreferencesEditableAll() {
        return !getSelectedSavedPreferencesList().isEmpty() &&
                getSelectedSavedPreferencesList().stream()
                        .noneMatch(this::isEmpty);
    }

    public void doSavePrefs() {
        GuiPreferences pref = getSelectedSavedPreferences();
        if (pref == null) {
            return;
        }

        String name = toSafeName(pref.getValueStore().getString(GuiPreferences.KEY_NAME, "pref")) + ".json";
        SettingsWindow.FileDialogManager fd = SettingsWindow.getFileDialogManager();
        Path file = fd.showConfirmDialogIfOverwriting(getMainPane(),
                fd.showSaveDialog(getMainPane(), null, name));
        if (file != null) {
            try (Writer w = Files.newBufferedWriter(file)) {
                new JsonWriter(w).withNewLines(true)
                        .write(pref.toJson());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            System.err.println("saved: " + file);
        }
    }

    public String toSafeName(String name) {
        return name.replaceAll("[\\-:/]", "_");
    }

    @SuppressWarnings("unchecked")
    public void doLoadPrefs() {
        Path file = SettingsWindow.getFileDialogManager().showOpenDialog(getMainPane(), null);
        if (file != null) {
            Object json = JsonReader.read(file.toFile());
            GuiPreferences prefs = getRootContext().getPreferences();
            try (var lock = prefs.lock()) {
                lock.use();
                prefs.addNewSavedStoreAsRoot()
                        .fromJson((Map<String, Object>) json);
            }
            reloadList();
        }
    }

    public void doApplyPrefs() {
        GuiPreferences preferences = getSelectedSavedPreferences();
        apply(preferences);
    }

    public void apply(GuiPreferences preferences) {
        if (preferences != null) {
            GuiPreferences prefs = getRootContext().getPreferences();
            try (var lock = prefs.lock()) {
                lock.use();
                prefs.overwriteByAnotherPrefs(preferences);
            }
            applyPreferences(new GuiSwingPrefsApplyOptions.PrefsApplyOptionsDefault(false, false));
        }
    }

//    public void doResetPrefs() {
//        int r = JOptionPane.showConfirmDialog(getMainPane(),
//                "Reset Entire Preferences ?", "Reset Entire Preferences", JOptionPane.OK_CANCEL_OPTION);
//        if (r == JOptionPane.OK_OPTION) {
//            GuiPreferences prefs = getRootContext().getPreferences();
//            try (var lock = prefs.lock()) {
//                lock.use();
//                prefs.overwriteToEmpty();
//            }
//            applyPreferences(new GuiSwingPrefsApplyOptions.PrefsApplyOptionsDefault(true, false));
//            reloadList();
//        }
//    }

    public void doDuplicatePrefs() {
        GuiPreferences pref = getSelectedSavedPreferences();
        if (pref == null) {
            return;
        }
        GuiPreferences rootPrefs = getRootContext().getPreferences();
        try (var lock = rootPrefs.lock()) {
            lock.use();
            GuiPreferences newStore = rootPrefs.addNewSavedStoreAsRoot();
            var name = newStore.getValueStore().getString(GuiPreferences.KEY_NAME, "");
            var uuid = newStore.getValueStore().getString(GuiPreferences.KEY_UUID, "");
            newStore.overwriteByAnotherPrefs(pref);
            if (!name.isEmpty()) {
                newStore.getValueStore().putString(GuiPreferences.KEY_NAME, name);
            }
            if (!uuid.isEmpty()) {
                newStore.getValueStore().putString(GuiPreferences.KEY_UUID, uuid);
            }
            newStore.getValueStore().flush();
        }
        reloadList();
    }

    public static class NewPrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;

        @SuppressWarnings("this-escape")
        public NewPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Save");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("add"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("add"));
            this.owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            owner.doNewPrefs();
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_PREFS;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_NEW;
        }
    }

    /**
     * the action for overwriting an existing prefs
     * @since 1.7
     */
    public static class UpdatePrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;
        @SuppressWarnings("this-escape")
        public UpdatePrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Update");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("update"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getIcon("update"));
            this.owner = owner;
            owner.addSelectionListener(this::updateEnabled);
        }

        public void updateEnabled() {
            setEnabled(owner.isSelectedPreferencesEditable());
        }

        @Override
        public boolean isEnabled() {
            updateEnabled();
            return enabled;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            owner.doUpdatePrefs();
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_PREFS;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_EXPORT;
        }
    }

    public static class DeletePrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;

        @SuppressWarnings("this-escape")
        public DeletePrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Delete");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("delete"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("delete"));
            this.owner = owner;
            owner.addSelectionListener(this::updateEnabled);
        }

        public void updateEnabled() {
            setEnabled(owner.isSelectedPreferencesEditableAll());
        }

        @Override
        public boolean isEnabled() {
            updateEnabled();
            return enabled;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            owner.doDeletePrefs();
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_PREFS;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_DELETE;
        }
    }

    public static class DuplicatePrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;

        @SuppressWarnings("this-escape")
        public DuplicatePrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Duplicate");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("duplicate"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("duplicate"));
            this.owner = owner;
            owner.addSelectionListener(this::updateEnabled);
        }

        public void updateEnabled() {
            setEnabled(owner.isSelectedPreferencesEditable());
        }

        @Override
        public boolean isEnabled() {
            updateEnabled();
            return enabled;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            owner.doDuplicatePrefs();
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_PREFS;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_COPY;
        }

    }

    public static class SavePrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;

        @SuppressWarnings("this-escape")
        public SavePrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Write To File...");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("save"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("save"));
            this.owner = owner;
            owner.addSelectionListener(this::updateEnabled);
        }

        public void updateEnabled() {
            setEnabled(owner.getSelectedSavedPreferences() != null);
        }

        @Override
        public boolean isEnabled() {
            updateEnabled();
            return enabled;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            owner.doSavePrefs();
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_PREFS;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_EXPORT;
        }
    }

    public static class LoadPrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;

        @SuppressWarnings("this-escape")
        public LoadPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Load From File...");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("load"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("load"));
            this.owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            owner.doLoadPrefs();
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_PREFS;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_IMPORT;
        }
    }

    public static class ApplyPrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction  {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;

        @SuppressWarnings("this-escape")
        public ApplyPrefsAction(GuiSwingPreferences owner) {
            this.owner = owner;
            init();
        }

        protected void init() {
            putValue(NAME, "Apply");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("apply"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("apply"));
            owner.addSelectionListener(this::updateEnabled);
        }

        public void updateEnabled() {
            setEnabled(owner.getSelectedSavedPreferences() != null);
        }

        @Override
        public boolean isEnabled() {
            updateEnabled();
            return enabled;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            owner.doApplyPrefs();
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
//
//    public static class ResetPrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
//        @Serial private static final long serialVersionUID = 1L;
//        protected GuiSwingPreferences owner;
//
//        @SuppressWarnings("this-escape")
//        public ResetPrefsAction(GuiSwingPreferences owner) {
//            putValue(NAME, "Reset");
//            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("reset"));
//            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("reset"));
//            this.owner = owner;
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            owner.doResetPrefs();
//        }
//
//        @Override
//        public String getCategory() {
//            return PopupExtension.MENU_CATEGORY_PREFS;
//        }
//
//        @Override
//        public String getSubCategory() {
//            return PopupExtension.MENU_SUB_CATEGORY_PREFS_CHANGE;
//        }
//    }

    //////////

    protected ScheduledTaskRunner<GuiSwingPrefsSupports.PreferencesUpdateEvent> updater;

    public void initRunner() {
        updater = new ScheduledTaskRunner<>(1000, this::runPreferencesUpdate);
        initRunnerToSupports(rootComponent);
        prefsWindowUpdater.setUpdater(getUpdateRunner());
    }

    protected void initRunnerToSupports(Component component) {
        GuiSwingView.forEach(GuiSwingPrefsSupports.PreferencesUpdateSupport.class, component,
                c -> c.setPreferencesUpdater(getUpdateRunner()));
    }

    public ScheduledTaskRunner<GuiSwingPrefsSupports.PreferencesUpdateEvent> getUpdater() {
        return updater;
    }

    public Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> getUpdateRunner() {
        return updater::schedule;
    }

    public void runPreferencesUpdate(List<GuiSwingPrefsSupports.PreferencesUpdateEvent> list) {
        list.stream()
                .distinct()
                .forEach(GuiSwingPrefsSupports.PreferencesUpdateEvent::save);
        GuiPreferences prefs = rootContext.getPreferences();
        try (var lock = prefs.lock()) {
            lock.use();
            prefs.getValueStore().flush();
        }
    }


    public static class PrefsApplyMenu extends JMenu {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences preferences;

        @SuppressWarnings("this-escape")
        public PrefsApplyMenu(GuiSwingPreferences preferences) {
            super("Apply Preferences");
            this.preferences = preferences;
            buildMenus();
        }

        public void buildMenus() {
            addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) { loadItems(); }

                @Override
                public void menuDeselected(MenuEvent e) { }

                @Override
                public void menuCanceled(MenuEvent e) { }
            });
        }

        public void loadItems() {
            removeAll();
            MenuBuilder builder = MenuBuilder.get();
            builder.addMenuItems(builder.createAppender(this),
                preferences.getSavedPreferencesList().stream()
                    .map(p -> new ApplySpecifiedPrefsAction(preferences, p))
                    .map(JMenuItem::new)
                    .collect(Collectors.toList()));
            if (getItemCount() == 0) {
                add(builder.createLabel("Nothing"));
            }
        }
    }

    public static class ApplySpecifiedPrefsAction extends ApplyPrefsAction {
        @Serial private static final long serialVersionUID = 1L;
        protected GuiPreferences targetPrefs;

        @SuppressWarnings("this-escape")
        public ApplySpecifiedPrefsAction(GuiSwingPreferences owner, GuiPreferences targetPrefs) {
            super(owner);
            this.targetPrefs = targetPrefs;
            initLazy();
        }

        @Override
        protected void init() { }

        protected void initLazy() {
            putValue(NAME, owner.getName(targetPrefs));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            owner.apply(targetPrefs);
        }
    }
}
