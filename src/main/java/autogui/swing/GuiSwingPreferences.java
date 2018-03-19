package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Supplier;

public class GuiSwingPreferences {
    protected JPanel mainPane;
    protected GuiMappingContext rootContext;
    protected JComponent rootComponent;
    protected JList<GuiPreferences> list;
    protected PreferencesListModel listModel;

    public GuiSwingPreferences(GuiMappingContext rootContext, JComponent rootComponent) {
        this.rootContext = rootContext;
        this.rootComponent = rootComponent;
        init();
    }

    protected void init() {
        mainPane = new JPanel(new BorderLayout());
        {
            listModel = new PreferencesListModel(this::getRootContext);
            list = new JList<>(listModel);
            list.setCellRenderer(new PreferencesRenderer());

            JScrollPane listScroll = new JScrollPane(list);
            listScroll.setPreferredSize(new Dimension(300, 300));

            JPanel viewPane = new JPanel();

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
        return list.getSelectedValuesList();
    }

    public GuiPreferences getSelectedSavedPreferences() {
        return list.getSelectedValue();
    }

    public JList<GuiPreferences> getList() {
        return list;
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

    public static class PreferencesListModel extends AbstractListModel<GuiPreferences> {
        protected Supplier<GuiMappingContext> context;
        protected List<GuiPreferences> list;
        public PreferencesListModel(Supplier<GuiMappingContext> context) {
            this.context = context;
            reload();
        }

        @Override
        public int getSize() {
            return list.size();
        }

        @Override
        public GuiPreferences getElementAt(int index) {
            return list.get(index);
        }

        public void reload() {
            int oldSize = 0;
            if (list != null) {
                oldSize = list.size();
            }
            list = context.get().getPreferences().getSavedStoreListAsRoot();
            int diff = oldSize - list.size();
            fireContentsChanged(this, 0, Math.min(oldSize - 1, list.size() - 1));
            if (diff > 0) { //removed
                fireIntervalRemoved(this, list.size(), oldSize - 1);
            }
            if (diff < 0) { //added
                fireIntervalAdded(this, oldSize, list.size() - 1);
            }
        }

        public List<GuiPreferences> getList() {
            return list;
        }
    }

    public static class PreferencesRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof GuiPreferences) {
                GuiPreferences prefs = (GuiPreferences) value;
                value = prefs.getValueStore().getString("$name", "Preferences " + index);
            }
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            return this;
        }
    }

    public static class NewPrefsAction extends AbstractAction {
        protected GuiSwingPreferences owner;
        public NewPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "New");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("add"));
            this.owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiPreferences rootPrefs = owner.getRootContext().getPreferences();
            GuiPreferences newStore = rootPrefs.addNewSavedStoreAsRoot();
            owner.savePreferences(newStore);
            newStore.fromJson(rootPrefs.toJson());
            owner.reloadList();
        }
    }

    public static class DeletePrefsAction extends AbstractAction {
        protected GuiSwingPreferences owner;
        public DeletePrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Delete");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("delete"));
            this.owner = owner;
            owner.getList().addListSelectionListener(e -> {
                setEnabled(!owner.getList().isSelectionEmpty());
            });
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (GuiPreferences p : owner.getSelectedSavedPreferencesList()) {
                p.clearAll();
            }
            owner.reloadList();
        }
    }

    public static class SavePrefsAction extends AbstractAction {
        protected GuiSwingPreferences owner;
        public SavePrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Save...");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("save"));
            this.owner = owner;
            owner.getList().addListSelectionListener(e -> {
                setEnabled(!owner.getList().isSelectionEmpty());
            });
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO
        }
    }

    public static class LoadPrefsAction extends AbstractAction {
        protected GuiSwingPreferences owner;
        public LoadPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Load...");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("load"));
            owner.getList().addListSelectionListener(e -> {
                setEnabled(!owner.getList().isSelectionEmpty());
            });
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO
        }
    }

    public static class ApplyPrefsAction extends AbstractAction {
        protected GuiSwingPreferences owner;
        public ApplyPrefsAction(GuiSwingPreferences owner) {
            putValue(NAME, "Apply");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("apply"));
            this.owner = owner;
            owner.getList().addListSelectionListener(e -> {
                setEnabled(!owner.getList().isSelectionEmpty());
            });
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
            putValue(NAME, "Clear All");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("reset"));
            this.owner = owner;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int r = JOptionPane.showConfirmDialog(owner.getMainPane(),
                    "Reset Entire Preferences ?");
            if (r == JOptionPane.OK_OPTION) {
                owner.getRootContext().getPreferences().clearAll();
            }
        }
    }
}
