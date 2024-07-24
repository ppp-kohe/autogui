package org.autogui.swing;

import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiReprObjectTabbedPane;
import org.autogui.swing.prefs.GuiSwingPrefsApplyOptions;
import org.autogui.swing.prefs.GuiSwingPrefsSupports;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * a swing view for {@link GuiReprObjectTabbedPane}
 *
 * <h2>swing-value</h2>
 *  {@link ObjectTabbedPane#getSwingViewValue()}: Object.
 *
 *
 * <h2>preferences</h2>
 * <pre>
 *     "$tab" {
 *         "selectedIndex" : Integer
 *     }
 * </pre>
 */
public class GuiSwingViewTabbedPane extends GuiSwingViewObjectPane {

    public GuiSwingViewTabbedPane(GuiSwingMapperSet mapperSet) {
        super(mapperSet);
    }

    @Override
    protected ObjectPane createObjectPane(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        return new ObjectTabbedPane(context, new SpecifierManagerDefault(parentSpecifier));
    }

    @Override
    public void createSubView(GuiMappingContext subContext, ObjectPane pane, GuiSwingView view) {
        JComponent comp = view.createView(subContext, pane::getSpecifier);
        if (comp != null) {
            ((ObjectTabbedPane) pane).addSubComponent(subContext, comp);
        }
    }

    public static class ObjectTabbedPane extends GuiSwingViewObjectPane.ObjectPane {
        @Serial private static final long serialVersionUID = 1L;
        protected JTabbedPane tabbedPane;
        protected TabPreferencesUpdater tabPreferencesUpdater;

        public ObjectTabbedPane(GuiMappingContext context, SpecifierManager specifierManager) {
            super(context, specifierManager);
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
            childContextToComponents.put(subContext.getName(), component);
            String desc = subContext.getDescription();
            if (!component.isOpaque() && !UIManagerUtil.getInstance().isTabbedPaneAllowOpaqueComponent()) {
                JComponent content = new JPanel(new BorderLayout());
                content.setOpaque(true);
                content.add(component);
                component = content;
            }
            if (desc.isEmpty()) {
                tabbedPane.addTab(subContext.getDisplayName(), component);
            } else {
                tabbedPane.addTab(subContext.getDisplayName(), null, component, desc);
            }
        }

        @Override
        public void addSubComponent(JComponent component, boolean resizable, GuiMappingContext subContext) {
            addSubComponent(subContext, component);
        }

        @Override
        public void addSubComponent(JComponent component, boolean resizable) {
            if (component instanceof ValuePane<?>) {
                addSubComponent(((ValuePane<?>) component).getSwingViewContext(), component);
            }
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            try {
                options.begin(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
                super.loadSwingPreferences(prefs, options);
                options.apply(tabPreferencesUpdater, prefs.getDescendant(getSwingViewContext()));
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
                options.end(this, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
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
        public void setPreferencesUpdater(Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> updater) {
            super.setPreferencesUpdater(updater);
            tabPreferencesUpdater.setUpdater(updater);
        }
    }

    public static class TabPreferencesUpdater implements ChangeListener {
        protected Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> updater;
        protected GuiMappingContext context;
        protected Supplier<JTabbedPane> pane;
        protected PreferencesForTab prefs;
        protected boolean savingDisabled = false;

        public TabPreferencesUpdater(GuiMappingContext context, Supplier<JTabbedPane> pane) {
            this.context = context;
            this.pane = pane;
            prefs = new PreferencesForTab();
        }

        public void setUpdater(Consumer<GuiSwingPrefsSupports.PreferencesUpdateEvent> updater) {
            this.updater = updater;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (!savingDisabled) {
                prefs.set(pane.get());
                if (updater != null) {
                    updater.accept(new GuiSwingPrefsSupports.PreferencesUpdateEvent(context, prefs));
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

    public static class PreferencesForTab implements GuiSwingPrefsSupports.PreferencesByJsonEntry {
        protected int selectedIndex;

        public PreferencesForTab() {}

        public void applyTo(JTabbedPane tabbedPane) {
            tabbedPane.getModel().setSelectedIndex(selectedIndex);
        }

        public void set(JTabbedPane tabbedPane) {
            selectedIndex = tabbedPane.getModel().getSelectedIndex();
        }

        /**
         * @return the selected tab index
         * @since 1.7
         */
        public int getSelectedIndex() {
            return selectedIndex;
        }

        /**
         * @param selectedIndex updated index
         * @since 1.7
         */
        public void setSelectedIndex(int selectedIndex) {
            this.selectedIndex = selectedIndex;
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
            if (json instanceof Map<?,?> map) {
                selectedIndex = GuiSwingPrefsSupports.getAs(map, Integer.class, "selectedIndex", 0);
            }
        }
    }
}
