package org.autogui.swing;

import org.autogui.base.JsonReader;
import org.autogui.base.JsonWriter;
import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.ScheduledTaskRunner;
import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.mapping.GuiReprValueDocumentEditor;
import org.autogui.swing.table.ObjectTableColumnValue;
import org.autogui.swing.util.*;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
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
 * <h3>GUI properties as preferences</h3>
 *
 *  <ol>
 *      <li>define a custom {@link PreferencesByJsonEntry} (or {@link Preferences}).
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
 *           Consumer&lt;{@link PreferencesUpdateEvent}&gt; updater;
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
 *        and the GUI component sets up the listener with implementing {@link PreferencesUpdateSupport}
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
 *           {@link GuiSwingView.ValuePane#loadSwingPreferences(GuiPreferences)} for bulk loading/saving.
 *       <pre>
 *            public void loadSwingPreferences(GuiPreferences p) {
 *                GuiSwingView.loadPreferencesDefault(this, p);
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

    protected JEditorPane contentTextPane;
    protected WindowPreferencesUpdater prefsWindowUpdater;

    protected SettingsWindow settingsWindow;

    protected int[] lastSelection = new int[0];

    public interface RootView {
        GuiMappingContext getContext();
        JComponent getViewComponent();
        void loadPreferences(GuiPreferences prefs);
        void savePreferences(GuiPreferences prefs);

    }

    public GuiSwingPreferences(RootView rootPane) {
        setRootView(rootPane);
        init();
        initRunner();
    }

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
        mainPane = new JPanel(new BorderLayout());
        {
            listModel = new PreferencesListModel(this::getRootContext);
            listModel.addTableModelListener(e -> {
                if (e.getColumn() == PreferencesListModel.COLUMN_LAUNCH) {
                    lastSelection = new int[] {e.getLastRow()};
                }
                SwingUtilities.invokeLater(this::restoreSelection);
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

            JPanel viewPane = new JPanel(new BorderLayout());
            {
                contentTextPane = new JTextPane();
                contentTextPane.setEditable(false);
                GuiReprValueDocumentEditor.setupStyle((StyledDocument) contentTextPane.getDocument());
                viewPane.add(new JScrollPane(contentTextPane));
            }

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, viewPane);
            mainPane.add(splitPane, BorderLayout.CENTER);
        }
        {
            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);
            toolBar.setOpaque(false);
            toolBar.add(actionButton(new NewPrefsAction(this)));
            toolBar.add(actionButton(new DeletePrefsAction(this)));
            toolBar.add(actionButton(new ApplyPrefsAction(this)));
            toolBar.add(actionButton(new SavePrefsAction(this)));
            toolBar.add(actionButton(new LoadPrefsAction(this)));
            toolBar.add(actionButton(new ResetPrefsAction(this)));

            mainPane.add(toolBar, BorderLayout.PAGE_START);
        }
        prefsWindowUpdater = new WindowPreferencesUpdater(null, rootContext, "$preferencesWindow");
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
        if (rootPane != null) {
            rootPane.loadPreferences(rootContext.getPreferences());
        } else {
            if (rootComponent instanceof GuiSwingView.ValuePane<?>) {
                ((GuiSwingView.ValuePane) rootComponent).loadSwingPreferences(
                        rootContext.getPreferences());
            }
            GuiSwingView.loadChildren(rootContext.getPreferences(), rootComponent);
        }
    }

    public void savePreferences(GuiPreferences prefs) {
        if (rootPane != null) {
            rootPane.savePreferences(prefs);
        } else {
            if (rootComponent instanceof GuiSwingView.ValuePane<?>) {
                ((GuiSwingView.ValuePane) rootComponent).saveSwingPreferences(
                        prefs);
            }
            GuiSwingView.saveChildren(rootContext.getPreferences(), rootComponent);
        }
    }

    public WindowPreferencesUpdater getPrefsWindowUpdater() {
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
        List<Object> list = new ArrayList<>();
        for (GuiPreferences prefs : getSelectedSavedPreferencesList()) {
            list.add(prefs.toJson());
        }

        JsonWriter w = JsonWriter.create().withNewLines(true);
        if (list.size() == 1) {
            w.write(list.get(0));
        } else {
            w.write(list);
        }
        Document doc = contentTextPane.getDocument();
        try {
            doc.remove(0, doc.getLength());
            doc.insertString(0, w.toSource(), null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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
        private static final long serialVersionUID = 1L;
        protected Supplier<GuiMappingContext> rootContext;
        protected List<GuiPreferences> list;
        protected List<GuiPreferences> savedPrefsList;
        protected GuiPreferences launchPrefs;
        protected GuiPreferences lastDefault;
        protected GuiPreferences targetDefault; //prefs is "empty" and thus using default values of context objects

        public static int COLUMN_SIZE = 2;
        public static int COLUMN_NAME = 0;
        public static int COLUMN_LAUNCH = 1;

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
            } else if (columnIndex == COLUMN_LAUNCH) {
                return true;
            } else {
                return false;
            }
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
                return "Defaults";
            } else {
                return prefs.getValueStore().getString("$name", "Preferences " + list.indexOf(prefs));
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
                String v = prefs.getValueStore().getString("$uuid", "");
                if (v.isEmpty()) {
                    v = UUID.randomUUID().toString();
                    prefs.getValueStore().putString("$uuid", v);
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
            rootContext.get().getPreferences().setLaunchPrefsAsRoot(getUUID(launchPrefs));
        }

        public void setName(GuiPreferences prefs, String name) {
            prefs.getValueStore().putString("$name", name);
            update(false);
        }

        public void update(boolean loadSavedList) {
            GuiMappingContext context = this.rootContext.get();
            lastDefault = context.getPreferences();
            targetDefault = new GuiPreferences(new GuiPreferences.GuiValueStoreOnMemory(), context);
            targetDefault.getValueStore().putString("$name", "Target Code Values");
            targetDefault.getValueStore().putString("$uuid", "empty");

            list = new ArrayList<>();
            list.add(lastDefault); //0
            list.add(targetDefault); //1
            if (savedPrefsList == null || loadSavedList) {
                savedPrefsList = lastDefault.getSavedStoreListAsRoot();
            }
            savedPrefsList.sort(Comparator.comparing(this::getName)); //sort by updated names
            list.addAll(savedPrefsList);

            String launchPrefsUUID = lastDefault.getLaunchPrefsAsRoot();
            if (launchPrefsUUID.isEmpty()) { //default
                launchPrefs = lastDefault;
            } else if (launchPrefsUUID.equals("empty")) {
                launchPrefs = targetDefault;
            } else {
                launchPrefs = savedPrefsList.stream()
                        .filter(p -> p.getValueStore().getString("$uuid", "").equals(launchPrefsUUID))
                        .findFirst()
                        .orElse(targetDefault);
            }
            fireTableDataChanged();
        }
    }

    public static class PreferencesNameRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        protected PreferencesListModel listModel;

        public PreferencesNameRenderer(PreferencesListModel listModel) {
            this.listModel = listModel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof GuiPreferences) {
                GuiPreferences prefs = (GuiPreferences) value;
                value = listModel.getName(prefs);
            }
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return this;
        }
    }

    public static class PreferencesNameEditor extends DefaultCellEditor {
        private static final long serialVersionUID = 1L;
        protected PreferencesListModel listModel;

        public PreferencesNameEditor(PreferencesListModel listModel) {
            super(new JTextField());
            this.listModel = listModel;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof GuiPreferences) {
                GuiPreferences prefs = (GuiPreferences) value;
                value = listModel.getName(prefs);
            }
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

    public static class PreferencesLaunchApplyRenderer extends JCheckBox implements TableCellRenderer {
        private static final long serialVersionUID = 1L;
        protected PreferencesListModel listModel;

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
            ObjectTableColumnValue.setTableColor(table, this, isSelected, row, column);
            return this;
        }
    }

    public static void setupCheckBox(JCheckBox box) {
        box.setHorizontalAlignment(SwingConstants.CENTER);
        box.setOpaque(true);
        box.setText("");
    }

    public static class PreferencesLaunchApplyEditor extends DefaultCellEditor {
        private static final long serialVersionUID = 1L;
        protected PreferencesListModel listModel;

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

    public static class NewPrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;
        public NewPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Save");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("add"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("add"));
            this.owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiPreferences rootPrefs = owner.getRootContext().getPreferences();
            GuiPreferences newStore = rootPrefs.addNewSavedStoreAsRoot();
            owner.savePreferences(newStore);

//            Map<String,Object> map = rootPrefs.toJson();
//            map.remove("$name");
//            map.remove("$uuid");
//            newStore.fromJson(map);

            newStore.getValueStore().flush();
            owner.reloadList();
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

    public static class DeletePrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;
        public DeletePrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Delete");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("delete"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("delete"));
            this.owner = owner;
            owner.addSelectionListener(() -> setEnabled(!owner.isSelectionEmpty()));

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (GuiPreferences p : owner.getSelectedSavedPreferencesList()) {
                p.clearAll();
            }
            owner.reloadList();
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

    public static class SavePrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;

        public SavePrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Write To File...");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("save"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("save"));
            this.owner = owner;
            owner.addSelectionListener(() -> setEnabled(!owner.isSelectionEmpty()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiPreferences pref = owner.getSelectedSavedPreferences();
            if (pref == null) {
                return;
            }

            String name = toSafeName(pref.getValueStore().getString("$name", "pref")) + ".json";
            SettingsWindow.FileDialogManager fd = SettingsWindow.getFileDialogManager();
            Path file = fd.showConfirmDialogIfOverwriting(owner.getMainPane(),
                    fd.showSaveDialog(owner.getMainPane(), null, name));
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
        private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;

        public LoadPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Load From File...");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("load"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("load"));
            this.owner = owner;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed(ActionEvent e) {
            Path file = SettingsWindow.getFileDialogManager().showOpenDialog(owner.getMainPane(), null);
            if (file != null) {
                Object json = JsonReader.read(file.toFile());
                owner.getRootContext().getPreferences().addNewSavedStoreAsRoot()
                        .fromJson((Map<String,Object>) json);
                owner.reloadList();
            }
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
        private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;

        public ApplyPrefsAction(GuiSwingPreferences owner) {
            this.owner = owner;
            init();
        }

        protected void init() {
            putValue(NAME, "Apply");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("apply"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("apply"));
            owner.addSelectionListener(() -> setEnabled(!owner.isSelectionEmpty()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiPreferences preferences = owner.getSelectedSavedPreferences();
            apply(preferences);
        }

        public void apply(GuiPreferences preferences) {
            if (preferences != null) {
                owner.getRootContext().getPreferences().clearAll();
                owner.getRootContext().getPreferences().fromJson(preferences.toJson());
                owner.applyPreferences();
            }
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

    public static class ResetPrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences owner;

        public ResetPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Reset");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("reset"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("reset"));
            this.owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int r = JOptionPane.showConfirmDialog(owner.getMainPane(),
                    "Reset Entire Preferences ?");
            if (r == JOptionPane.OK_OPTION) {
                owner.getRootContext().getPreferences().resetAsRoot();
                owner.applyPreferences();
                owner.reloadList();
            }
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

    //////////

    protected ScheduledTaskRunner<PreferencesUpdateEvent> updater;

    public void initRunner() {
        updater = new ScheduledTaskRunner<>(1000, this::runPreferencesUpdate);
        initRunnerToSupports(rootComponent);
        prefsWindowUpdater.setUpdater(getUpdateRunner());
    }

    protected void initRunnerToSupports(Component component) {
        GuiSwingView.forEach(PreferencesUpdateSupport.class, component,
                c -> c.setPreferencesUpdater(getUpdateRunner()));
    }

    public ScheduledTaskRunner<PreferencesUpdateEvent> getUpdater() {
        return updater;
    }

    public Consumer<PreferencesUpdateEvent> getUpdateRunner() {
        return updater::schedule;
    }

    public void runPreferencesUpdate(List<PreferencesUpdateEvent> list) {
        list.stream()
                .distinct()
                .forEach(PreferencesUpdateEvent::save);
        rootContext.getPreferences().getValueStore().flush();
    }

    public static class PreferencesUpdateEvent {
        protected GuiMappingContext context;
        protected Preferences prefs;

        public PreferencesUpdateEvent(GuiMappingContext context, Preferences prefs) {
            this.context = context;
            this.prefs = prefs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PreferencesUpdateEvent that = (PreferencesUpdateEvent) o;
            return Objects.equals(context, that.context) &&
                    Objects.equals(prefs, that.prefs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(context, prefs);
        }

        public void save() {
            prefs.saveTo(context.getPreferences());
        }
    }

    /** partial updater */
    public interface PreferencesUpdateSupport {
        void setPreferencesUpdater(Consumer<PreferencesUpdateEvent> updater);
    }

    public interface Preferences {
        void loadFrom(GuiPreferences prefs);

        void saveTo(GuiPreferences prefs);
    }

    public interface PreferencesByJsonEntry extends Preferences {
        String getKey();
        Object toJson();
        void setJson(Object json);

        default void loadFrom(GuiPreferences prefs) {
            setJson(JsonReader.create(prefs.getValueStore().getString(getKey(), "null"))
                    .parseValue());
        }

        default void saveTo(GuiPreferences prefs) {
            prefs.getValueStore().putString(getKey(),
                    JsonWriter.create().withNewLines(false).write(toJson()).toSource());
        }
    }

    public static class PreferencesForWindow implements PreferencesByJsonEntry {
        protected int x;
        protected int y;
        protected int width;
        protected int height;
        protected String key = "$window";

        public PreferencesForWindow(String key) {
            this.key = key;
        }

        public PreferencesForWindow() {}

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public void applyTo(Window window) {
            if (width > 0 && height > 0) {
                Dimension size = new Dimension(width, height);
                window.setSize(size);
            } else {
                window.pack();
            }
            if (x > 0 && y > 0) {
                Point p = new Point(x, y);
                window.setLocation(p);
            }
        }

        public void setSizeFrom(Window window) {
            Dimension size = window.getSize();
            width = size.width;
            height = size.height;
        }

        public void setLocationFrom(Window window) {
            Point p = window.getLocation();
            x = p.x;
            y = p.y;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("x", x);
            map.put("y", y);
            map.put("width", width);
            map.put("height", height);
            return map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?,?>) {
                Map<String,Object> map = (Map<String,Object>) json;
                this.x = (Integer) map.getOrDefault("x", 0);
                this.y = (Integer) map.getOrDefault("y", 0);
                this.width = (Integer) map.getOrDefault("width", 0);
                this.height = (Integer) map.getOrDefault("height", 0);
            }
        }
    }

    public static class WindowPreferencesUpdater implements ComponentListener, SettingsWindow.SettingSupport {
        protected Window window;
        protected GuiMappingContext context;
        protected PreferencesForWindow prefs;
        protected Consumer<PreferencesUpdateEvent> updater;
        protected boolean savingDisabled = false;

        public WindowPreferencesUpdater(Window window, GuiMappingContext context) {
            this.window = window;
            this.context = context;
            prefs = new PreferencesForWindow();
        }

        public WindowPreferencesUpdater(Window window, GuiMappingContext context, String name) {
            this.window = window;
            this.context = context;
            prefs = new PreferencesForWindow(name);
        }

        public void setUpdater(Consumer<PreferencesUpdateEvent> updater) {
            this.updater = updater;
        }

        public PreferencesForWindow getPrefs() {
            return prefs;
        }

        @Override
        public void resized(JFrame window) {
            if (!savingDisabled) {
                prefs.setSizeFrom(window);
                sendToUpdater();
            }
        }

        @Override
        public void moved(JFrame window) {
            if (!savingDisabled) {
                prefs.setLocationFrom(window);
                sendToUpdater();
            }
        }

        @Override
        public void setup(JFrame window) {
            savingDisabled = true;
            try {
                prefs.applyTo(window);
            } finally {
                savingDisabled = false;
            }
        }

        public void apply(GuiPreferences p) {
            savingDisabled = true;
            try {
                prefs.loadFrom(p);
                if (window != null) {
                    prefs.applyTo(window);
                }
            } finally {
                savingDisabled = false;
            }
        }

        public void sendToUpdater() {
            if (updater != null) {
                updater.accept(new PreferencesUpdateEvent(context, prefs));
            }
        }

        @Override
        public void componentResized(ComponentEvent e) {
            if (window != null && !savingDisabled) {
                prefs.setSizeFrom(window);
                sendToUpdater();
            }
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            if (window != null && !savingDisabled) {
                prefs.setLocationFrom(window);
                sendToUpdater();
            }
        }

        @Override
        public void componentShown(ComponentEvent e) { }

        @Override
        public void componentHidden(ComponentEvent e) { }
    }

    public static class PreferencesForFileDialog implements PreferencesByJsonEntry {
        protected List<String> fileList = new ArrayList<>();
        protected String backPath;
        protected String currentDirectory;
        protected String key = "$fileDialog";

        public void setFileList(List<Path> fileList) {
            this.fileList = fileList.stream()
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }

        public void setBackPath(Path path) {
            this.backPath = (path == null ? null : path.toString());
        }

        public void setCurrentDirectory(Path path) {
            this.currentDirectory = (path == null ? null : path.toString());
        }

        public void applyTo(SettingsWindow.FileDialogManager dialogManager) {
            dialogManager.setFieList(fileList.stream()
                    .map(Paths::get)
                    .collect(Collectors.toList()));
            if (currentDirectory != null) {
                dialogManager.setCurrentPath(Paths.get(currentDirectory));
            }
            if (backPath != null) {
                dialogManager.setBackButtonPath(Paths.get(backPath));
            }
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("fileList", fileList);
            map.put("currentDirectory", currentDirectory);
            map.put("backPath", backPath);
            return map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?,?>) {
                Map<String,Object> map =  (Map<String,Object>) json;
                fileList = (List<String>) map.getOrDefault("fileList", new ArrayList<>());
                currentDirectory = (String) map.getOrDefault("currentDirectory", null);
                backPath = (String) map.getOrDefault("backPath", null);
            }
        }
    }

    public static class FileDialogPreferencesUpdater implements SettingsWindow.FileDialogManagerListener {
        protected SettingsWindow.FileDialogManager dialogManager;
        protected GuiMappingContext context;
        protected Consumer<PreferencesUpdateEvent> updater;
        protected PreferencesForFileDialog prefs;

        protected boolean savingDisabled;

        public FileDialogPreferencesUpdater(SettingsWindow.FileDialogManager dialogManager, GuiMappingContext context) {
            this.dialogManager = dialogManager;
            this.context = context;
            prefs = new PreferencesForFileDialog();
        }

        public void setUpdater(Consumer<PreferencesUpdateEvent> updater) {
            this.updater = updater;
        }

        public void addToDialogManager() {
            int len = GuiPreferences.getStoreValueMaxLength(); //8192
            int maxPathLength = 255 + 4;
            int jsonSymbols = 47;
            //suppose the max length of a path is 255 (actually it would be longer),
            //   then a path string becomes 255 + (2 of \" : 4), with a comma separator
            // (note: Windows path separators will be escaped)
            // JSON keys and symbols: {"fileList":[],"currentDirectory":,"backPath":} :47
            // 8192 = (n + 2) * 259 + (n - 1) + 47;  thus n = (8192 - 259 * 2 + 1 - 47) / 260 =~ 29
            int maxList = (len - maxPathLength * 2 + 1 - jsonSymbols) / (maxPathLength + 1);
            dialogManager.setMaxHistoryList(maxList);
            dialogManager.addListener(this);
        }

        public void removeFromDialogManager() {
            dialogManager.removeListener(this);
        }

        @Override
        public void updateFileList(SettingsWindow.FileListModel listModel) {
            if (!savingDisabled) {
                prefs.setFileList(listModel.getPaths());
                sendToUpdater();
            }
        }

        @Override
        public void updateCurrentDirectory(Path path) {
            if (!savingDisabled) {
                prefs.setCurrentDirectory(path);
                sendToUpdater();
            }
        }

        @Override
        public void updateBackButton(Path path) {
            if (!savingDisabled) {
                prefs.setBackPath(path);
                sendToUpdater();
            }
        }

        public void sendToUpdater() {
            if (updater != null) {
                updater.accept(new PreferencesUpdateEvent(context, prefs));
            }
        }

        public void apply(GuiPreferences p) {
            savingDisabled = true;
            try {
                prefs.loadFrom(p);
                prefs.applyTo(dialogManager);
            } finally {
                savingDisabled = false;
            }
        }

        public PreferencesForFileDialog getPrefs() {
            return prefs;
        }
    }

    public static class PrefsApplyMenu extends JMenu {
        private static final long serialVersionUID = 1L;
        protected GuiSwingPreferences preferences;

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
        private static final long serialVersionUID = 1L;
        protected GuiPreferences targetPrefs;

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
            apply(targetPrefs);
        }
    }
}
