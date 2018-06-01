package autogui.swing;

import autogui.base.JsonReader;
import autogui.base.JsonWriter;
import autogui.base.log.GuiLogManager;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.mapping.GuiReprValueDocumentEditor;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.base.mapping.ScheduledTaskRunner;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
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
 *          {@link autogui.swing.GuiSwingView.ValuePane#saveSwingPreferences(GuiPreferences)} and
 *           {@link autogui.swing.GuiSwingView.ValuePane#loadSwingPreferences(GuiPreferences)} for bulk loading/saving.
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
                this.showSelectedPrefs();
            });
            list = new JTable(listModel);
            list.setDefaultRenderer(Object.class, new PreferencesRenderer());
            TableColumn column = list.getColumnModel().getColumn(0);
            {
                column.setHeaderValue("Saved Prefs");
                column.setCellEditor(new PreferencesNameEditor());
            }
            addSelectionListener(this::showSelectedPrefs);

            JScrollPane listScroll = new JScrollPane(list);
            listScroll.setPreferredSize(new Dimension(300, 300));

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
        list.getSelectionModel().addListSelectionListener(e -> r.run());
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

    public void showSelectedPrefs() {
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

    public static class PreferencesListModel extends AbstractTableModel {
        protected Supplier<GuiMappingContext> context;
        protected List<GuiPreferences> list;
        public PreferencesListModel(Supplier<GuiMappingContext> context) {
            this.context = context;
            reload();
        }

        @Override
        public int getRowCount() {
            return list.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return list.get(rowIndex);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        public void reload() {
            int oldSize = 0;
            if (list != null) {
                oldSize = list.size();
            }
            list = context.get().getPreferences().getSavedStoreListAsRoot();
            fireTableDataChanged();
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
    }

    public static class PreferencesRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof GuiPreferences) {
                GuiPreferences prefs = (GuiPreferences) value;
                value = prefs.getValueStore().getString("$name", "Preferences " + row);
            }
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return this;
        }
    }

    public static class PreferencesNameEditor extends DefaultCellEditor {
        protected GuiPreferences currentPrefs;
        public PreferencesNameEditor() {
            super(new JTextField());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentPrefs = null;
            if (value instanceof GuiPreferences) {
                currentPrefs = (GuiPreferences) value;
                value = ((GuiPreferences) value).getValueStore().getString("$name", "");
            }
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        @Override
        public Object getCellEditorValue() {
            Object name = super.getCellEditorValue();
            if (currentPrefs != null) {
                currentPrefs.getValueStore().putString("$name", name.toString());

            }
            return currentPrefs;
        }
    }

    public static class NewPrefsAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
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
            Map<String,Object> map = rootPrefs.toJson();
            map.remove("$name");
            newStore.fromJson(map);
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
        protected GuiSwingPreferences owner;
        public ApplyPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Apply");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("apply"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("apply"));
            this.owner = owner;
            owner.addSelectionListener(() -> setEnabled(!owner.isSelectionEmpty()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiPreferences preferences = owner.getSelectedSavedPreferences();
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
                owner.getRootContext().getPreferences().clearAll();
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
}
