package autogui.swing;

import autogui.base.JsonReader;
import autogui.base.JsonWriter;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.mapping.GuiReprValueDocumentEditor;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GuiSwingPreferences {
    protected JPanel mainPane;
    protected GuiMappingContext rootContext;
    protected JComponent rootComponent;
    protected JTable list;
    protected PreferencesListModel listModel;

    protected JEditorPane contentTextPane;

    public GuiSwingPreferences(GuiMappingContext rootContext, JComponent rootComponent) {
        this.rootContext = rootContext;
        this.rootComponent = rootComponent;
        init();
    }

    protected void init() {
        mainPane = new JPanel(new BorderLayout());
        {
            listModel = new PreferencesListModel(this::getRootContext);
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
                GuiReprValueDocumentEditor.setUpStyle((StyledDocument) contentTextPane.getDocument());
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
    }

    private JButton actionButton(Action action) {
        return new GuiSwingIcons.ActionButton(action);
    }

    public void show(JComponent sender) {
        SettingsWindow.get().show("Preferences", sender, mainPane);
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
        if (rootComponent instanceof GuiSwingView.ValuePane<?>) {
            ((GuiSwingView.ValuePane) rootComponent).loadPreferences(
                rootContext.getPreferences());
        }
    }

    public void savePreferences(GuiPreferences prefs) {
        if (rootComponent instanceof GuiSwingView.ValuePane<?>) {
            ((GuiSwingView.ValuePane) rootComponent).savePreferences(
                    prefs);
        }
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

    public static class NewPrefsAction extends AbstractAction {
        protected GuiSwingPreferences owner;
        public NewPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Save");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("add"));
            this.owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiPreferences rootPrefs = owner.getRootContext().getPreferences();
            GuiPreferences newStore = rootPrefs.addNewSavedStoreAsRoot();
            owner.savePreferences(newStore);
            Map<String,Object> map = rootPrefs.toJson();
            if (map.containsKey("$name")) {
                map.remove("$name");
            }
            newStore.fromJson(map);
            owner.reloadList();
        }
    }

    public static class DeletePrefsAction extends AbstractAction {
        protected GuiSwingPreferences owner;
        public DeletePrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Delete");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("delete"));
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
    }

    protected JFileChooser chooser;

    public JFileChooser getChooser() {
        if (chooser == null) {
            chooser = new JFileChooser();
        }
        return chooser;
    }

    public static class SavePrefsAction extends AbstractAction {
        protected GuiSwingPreferences owner;
        public SavePrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Write To File...");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("save"));
            this.owner = owner;
            owner.addSelectionListener(() -> setEnabled(!owner.isSelectionEmpty()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = owner.getChooser();
            GuiPreferences pref = owner.getSelectedSavedPreferences();
            String name = toSafeName(pref.getValueStore().getString("$name", "pref")) + ".json";
            chooser.setSelectedFile(new File(name));
            int ret = chooser.showSaveDialog(owner.getMainPane());
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try (Writer w = Files.newBufferedWriter(file.toPath())) {
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
    }

    public static class LoadPrefsAction extends AbstractAction {
        protected GuiSwingPreferences owner;
        public LoadPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Load From File...");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("load"));
            this.owner = owner;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = owner.getChooser();
            int ret = chooser.showOpenDialog(owner.getMainPane());
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                Object json = JsonReader.read(file);
                owner.getRootContext().getPreferences().addNewSavedStoreAsRoot()
                        .fromJson((Map<String,Object>) json);
                owner.reloadList();
            }
        }
    }

    public static class ApplyPrefsAction extends AbstractAction {
        protected GuiSwingPreferences owner;
        public ApplyPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Apply");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("apply"));
            this.owner = owner;
            owner.addSelectionListener(() -> setEnabled(!owner.isSelectionEmpty()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiPreferences preferences = owner.getSelectedSavedPreferences();
            if (preferences != null) {
                owner.getRootContext().getPreferences().fromJson(preferences.toJson());
                owner.applyPreferences();
            }
        }
    }

    public static class ResetPrefsAction extends AbstractAction {
        protected GuiSwingPreferences owner;

        public ResetPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Reset");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("reset"));
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
    }
}
