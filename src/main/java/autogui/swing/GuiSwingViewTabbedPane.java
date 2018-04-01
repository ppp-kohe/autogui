package autogui.swing;

import autogui.base.log.GuiLogManager;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GuiSwingViewTabbedPane extends GuiSwingViewObjectPane {

    public GuiSwingViewTabbedPane(GuiSwingMapperSet mapperSet) {
        super(mapperSet);
    }

    @Override
    protected ObjectPane createObjectPane(GuiMappingContext context) {
        return new ObjectTabbedPane(context);
    }

    @Override
    public void createSubView(GuiMappingContext subContext, ObjectPane pane, GuiSwingView view) {
        JComponent comp = view.createView(subContext);
        if (comp != null) {
            ((ObjectTabbedPane) pane).addSubComponent(subContext, comp);
        }
    }

    public static class ObjectTabbedPane extends GuiSwingViewObjectPane.ObjectPane {
        protected JTabbedPane tabbedPane;
        protected TabPreferencesUpdater tabPreferencesUpdater;

        public ObjectTabbedPane(GuiMappingContext context) {
            super(context);
        }

        public JTabbedPane getTabbedPane() {
            return tabbedPane;
        }

        @Override
        public void initContentPane() {
            tabbedPane = new JTabbedPane();
            this.contentPane = tabbedPane;
            tabPreferencesUpdater = new TabPreferencesUpdater(context, this::getTabbedPane);
            tabbedPane.addChangeListener(tabPreferencesUpdater);
            add(tabbedPane);
        }

        public void addSubComponent(GuiMappingContext subContext, JComponent component) {
            String desc = subContext.getDescription();
            if (desc.isEmpty()) {
                tabbedPane.addTab(subContext.getDisplayName(), component);
            } else {
                tabbedPane.addTab(subContext.getDisplayName(), null, component, desc);
            }
        }

        @Override
        public void addSubComponent(JComponent component, boolean resizable) {
            if (component instanceof ValuePane<?>) {
                addSubComponent(((ValuePane) component).getSwingViewContext(), component);
            }
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs) {
            try {
                super.loadSwingPreferences(prefs);
                tabPreferencesUpdater.apply(prefs.getDescendant(getSwingViewContext()));
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            try {
                super.saveSwingPreferences(prefs);
                tabPreferencesUpdater.getPrefs().saveTo(prefs.getDescendant(getSwingViewContext()));
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            super.setPreferencesUpdater(updater);
            tabPreferencesUpdater.setUpdater(updater);
        }
    }

    public static class TabPreferencesUpdater implements ChangeListener {
        protected Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater;
        protected GuiMappingContext context;
        protected Supplier<JTabbedPane> pane;
        protected PreferencesForTab prefs;
        protected boolean savingDisabled = false;

        public TabPreferencesUpdater(GuiMappingContext context, Supplier<JTabbedPane> pane) {
            this.context = context;
            this.pane = pane;
            prefs = new PreferencesForTab();
        }

        public void setUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            this.updater = updater;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (!savingDisabled) {
                prefs.set(pane.get());
                if (updater != null) {
                    updater.accept(new GuiSwingPreferences.PreferencesUpdateEvent(context, prefs));
                }
            }
        }

        public void apply(GuiPreferences p) {
            savingDisabled = true;
            try {
                prefs.loadFrom(p);
                prefs.applyTo(pane.get());
            } finally {
                savingDisabled = false;
            }
        }

        public PreferencesForTab getPrefs() {
            return prefs;
        }
    }

    public static class PreferencesForTab implements GuiSwingPreferences.PreferencesByJsonEntry {
        protected int selectedIndex;

        public void applyTo(JTabbedPane tabbedPane) {
            tabbedPane.getModel().setSelectedIndex(selectedIndex);
        }

        public void set(JTabbedPane tabbedPane) {
            selectedIndex = tabbedPane.getModel().getSelectedIndex();
        }

        @Override
        public String getKey() {
            return "$tab";
        }

        @Override
        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("selectedIndex", selectedIndex);
            return map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json != null && json instanceof Map<?,?>) {
                Map<String,Object> map = (Map<String,Object>) json;
                selectedIndex = (Integer) map.getOrDefault("selectedIndex", selectedIndex);
            }
        }
    }
}
