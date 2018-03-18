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
    protected JList<GuiPreferences> list;
    protected PreferencesListModel listModel;

    public GuiSwingPreferences(GuiMappingContext rootContext) {
        this.rootContext = rootContext;
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
            toolBar.add(actionButton(new SavePrefsAction()));
            toolBar.add(actionButton(new LoadPrefsAction()));
            toolBar.add(actionButton(new ApplyPrefsAction(this::getRootContext, this::getSelectedSavedPreferences,
                    () -> getRootContext().updateSourceFromRoot())));
            toolBar.add(actionButton(new NewPrefsAction(this::getRootContext, this::reloadList)));
            toolBar.add(actionButton(new DeletePrefsAction(this::getSelectedSavedPreferencesList, this::reloadList)));

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
        protected Supplier<GuiMappingContext> context;
        protected Runnable updater;
        public NewPrefsAction(Supplier<GuiMappingContext> context, Runnable updater) {
            putValue(NAME, "New");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("add"));
            this.context = context;
            this.updater = updater;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiPreferences rootPrefs = context.get().getPreferences();
            GuiPreferences newStore = rootPrefs.addNewSavedStoreAsRoot();
            newStore.fromJson(rootPrefs.toJson());
            updater.run();
        }
    }

    public static class DeletePrefsAction extends AbstractAction {
        protected Supplier<List<GuiPreferences>> prefs;
        protected Runnable updater;
        public DeletePrefsAction(Supplier<List<GuiPreferences>> prefs, Runnable updater) {
            putValue(NAME, "Delete");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("delete"));
            this.prefs = prefs;
            this.updater = updater;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (GuiPreferences p : prefs.get()) {
                p.removeAsSavedStore();
            }
            updater.run();
        }
    }

    public static class SavePrefsAction extends AbstractAction {
        public SavePrefsAction() {
            putValue(NAME, "Save...");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("save"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {

        }
    }

    public static class LoadPrefsAction extends AbstractAction {
        public LoadPrefsAction() {
            putValue(NAME, "Load...");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("load"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {

        }
    }

    public static class ApplyPrefsAction extends AbstractAction {
        protected Supplier<GuiMappingContext> context;
        protected Supplier<GuiPreferences> prefs;
        protected Runnable updater;
        public ApplyPrefsAction(Supplier<GuiMappingContext> context, Supplier<GuiPreferences> prefs, Runnable updater) {
            putValue(NAME, "Apply");
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("apply"));
            this.context = context;
            this.prefs = prefs;
            this.updater = updater;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiPreferences preferences = prefs.get();
            if (preferences != null) {
                context.get().getPreferences().fromJson(preferences.toJson());
                  //TODO apply the last history value to context value, if isHistoryValueSupported() && isHistoryValueStored()
                updater.run();
            }
        }
    }
}
