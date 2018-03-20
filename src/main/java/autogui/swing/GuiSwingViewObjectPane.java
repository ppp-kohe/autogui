package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprObjectPane;
import autogui.base.mapping.GuiReprPropertyPane;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.ResizableFlowLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <h3>representation</h3>
 * {@link autogui.base.mapping.GuiReprObjectPane}
 *
 * <h3>{@link ObjectPane#getSwingViewValue()}</h3>
 * latest set raw-object.
 *
 *  Currently, {@link ObjectPane#setSwingViewValue(Object)} also set context's value.
 *    TODO omit to update?
 *
 * <h3>history-value</h3>
 *  supported.
 *
 * <h3>string-transfer</h3>
 * no-transfer-handler.
 *  supported by {@link autogui.swing.GuiSwingView.ToStringCopyAction} and
 *     {@link autogui.base.mapping.GuiRepresentation#toHumanReadableString(GuiMappingContext, Object)}.
 */
public class GuiSwingViewObjectPane implements GuiSwingView {
    protected GuiSwingMapperSet mapperSet;

    public GuiSwingViewObjectPane(GuiSwingMapperSet mapperSet) {
        this.mapperSet = mapperSet;
    }

    @Override
    public JComponent createView(GuiMappingContext context) {
        ObjectPane pane = new ObjectPane(context);
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement e = mapperSet.view(subContext);
            if (e != null) {
                if (e instanceof GuiSwingView) {
                    createSubView(subContext, pane, (GuiSwingView) e);
                } else if (e instanceof GuiSwingAction) {
                    createSubAction(subContext, pane, (GuiSwingAction) e);
                }
            }
        }
        return pane;
    }

    public void createSubView(GuiMappingContext subContext, ObjectPane pane, GuiSwingView view) {
        JComponent subComp = view.createView(subContext);
        if (subComp != null) {
            pane.addSubComponent(subComp, view.isComponentResizable(subContext));
        }
    }

    public void createSubAction(GuiMappingContext subContext, ObjectPane pane, GuiSwingAction action) {
        Action act = action.createAction(subContext);
        if (act != null) {
            pane.addAction(act);
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    /**
     * <pre>
     * actionToolBar: [JToolBar: actionButtons...],
     * contentPane: [
     *   fixedSizeComponent1,
     *   ...
     *   resizableSubComponents [
     *       [splitPane: resizableComponent1 |
     *         [splitPane: resizableComponent2 | ... ]
     *       ]
     *   ],
     *   fixedSizeComponent2,
     *   fixedSizeComponent3,
     *   ... ]
     *  </pre>
     */
    public static class ObjectPane extends JPanel implements GuiMappingContext.SourceUpdateListener, ValuePane<Object>,
        GuiSwingPreferences.PreferencesUpdateSupport {
        protected GuiMappingContext context;
        protected JToolBar actionToolBar;
        protected JComponent contentPane;
        protected JComponent resizableSubComponents;
        protected PopupExtension popup;
        protected List<Action> actions = new ArrayList<>();

        protected List<JSplitPane> splitPanes = new ArrayList<>();
        protected SplitPreferencesUpdater preferencesUpdater;

        public ObjectPane(GuiMappingContext context) {
            this.context = context;
            setLayout(new BorderLayout());
            initContentPane();
            add(contentPane, BorderLayout.CENTER);

            preferencesUpdater = new SplitPreferencesUpdater(context, this::getSplitPanes);

            //context update
            context.addSourceUpdateListener(this);

            //popup
            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            ContextRefreshAction refreshAction = new ContextRefreshAction(context);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.accept(info);
                menu.accept(refreshAction);
                GuiSwingJsonTransfer.getActions(this, context)
                        .forEach(menu::accept);
                menu.accept(new ToStringCopyAction(this, context));
                menu.accept(new JPopupMenu.Separator());
                actions.forEach(menu::accept);
            });
            setInheritsPopupMenu(true);

            setBorder(new GuiSwingViewLabel.FocusBorder(this));
            setFocusable(true);
            GuiSwingView.setupTransferHandler(this, new ToStringTransferHandler(this));
        }

        public void initContentPane() {
            contentPane = new JPanel();
            contentPane.setOpaque(false);
            ResizableFlowLayout layout = new ResizableFlowLayout(false, 10);
            layout.setFitHeight(true);
            contentPane.setLayout(layout);
        }

        public List<Action> getActions() {
            return actions;
        }

        public JComponent getContentPane() {
            return contentPane;
        }

        public JToolBar getActionToolBar() {
            return actionToolBar;
        }

        public JComponent getResizableSubComponents() {
            return resizableSubComponents;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            //nothing to do?
        }

        public void addSubComponent(JComponent component, boolean resizable) {
            if (resizable) {
                addSubComponentResizable(component);
            } else {
                ResizableFlowLayout.add(contentPane, component, false);
            }
        }

        public void addSubComponentResizable(JComponent component) {
            if (resizableSubComponents == null) {
                resizableSubComponents = createResizableSubComponents();
                resizableSubComponents.add(component);
                ResizableFlowLayout.add(contentPane, resizableSubComponents, true);
            } else {
                Component prev = resizableSubComponents.getComponent(0);
                resizableSubComponents.removeAll();
                JComponent split = createResizableSplit(true, prev, component);
                resizableSubComponents.add(split);
            }
        }

        public JComponent createResizableSubComponents() {
            JComponent resizableSubComponents = new JPanel();
            resizableSubComponents.setLayout(new BorderLayout());
            resizableSubComponents.setBorder(BorderFactory.createEmptyBorder());
            resizableSubComponents.setOpaque(false);
            return resizableSubComponents;
        }

        public JComponent createResizableSplit(boolean horizontal, Component left, Component right) {
            double prevWidth = getSize(horizontal, left);
            double nextWidth = getSize(horizontal, right);

            JSplitPane pane = new JSplitPane(horizontal ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT, left, right);
            pane.setOpaque(false);
            pane.setBorder(BorderFactory.createEmptyBorder());
            pane.setDividerLocation(prevWidth / (prevWidth + nextWidth));

            this.splitPanes.add(pane);

            pane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, preferencesUpdater);
            pane.addPropertyChangeListener(JSplitPane.ORIENTATION_PROPERTY, preferencesUpdater);
            return pane;
        }

        public List<JSplitPane> getSplitPanes() {
            return splitPanes;
        }

        private double getSize(boolean horizontal, Component comp) {
            Dimension size = comp.getPreferredSize();
            return horizontal ? size.getWidth() : size.getHeight();
        }

        public void addAction(Action action) {
            Object name = action.getValue(Action.NAME);
            if (name != null) {
                getActionMap().put(name, action);
            }
            if (actionToolBar == null) {
                initActionToolBar();
            }
            actionToolBar.add(new GuiSwingIcons.ActionButton(action));
            actions.add(action);
        }

        public void initActionToolBar() {
            actionToolBar = new JToolBar();
            actionToolBar.setFloatable(false);
            actionToolBar.setOpaque(false);
            add(actionToolBar, BorderLayout.PAGE_START);
        }

        @Override
        public Object getSwingViewValue() {
            return context.getSource();
        }

        /** special case: update the source */
        @Override
        public void setSwingViewValue(Object value) {
            context.setSource(value);
            revalidate();
            repaint();
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            ((GuiReprObjectPane) getContext().getRepresentation())
                    .updateFromGui(getContext(), value);
            revalidate();
            repaint();
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }

        @Override
        public void loadPreferences(GuiPreferences prefs) {
            GuiSwingView.loadPreferencesDefault(this, prefs);
            preferencesUpdater.apply(prefs.getDescendant(getContext()));
        }

        @Override
        public void savePreferences(GuiPreferences prefs) {
            GuiSwingView.savePreferencesDefault(this, prefs);
            preferencesUpdater.getPrefs().saveTo(prefs.getDescendant(getContext()));
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            preferencesUpdater.setUpdater(updater);
        }
    }

    public static class ToStringTransferHandler extends TransferHandler {
        protected ValuePane<?> pane;

        public ToStringTransferHandler(ValuePane<?> pane) {
            super();
            this.pane = pane;
        }


        @Override
        public boolean canImport(TransferSupport support) {
            return false;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            String data = pane.getContext().getRepresentation()
                            .toHumanReadableString(pane.getContext(), pane.getSwingViewValue());
            return new StringSelection(data);
        }
    }

    public static class SplitPreferencesUpdater implements PropertyChangeListener {
        protected Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater;
        protected GuiMappingContext context;
        protected Supplier<List<JSplitPane>> panes;
        protected PreferencesForSplit prefs;

        public SplitPreferencesUpdater(GuiMappingContext context, Supplier<List<JSplitPane>> panes) {
            this.context = context;
            this.panes = panes;
            prefs = new PreferencesForSplit();
        }

        public void setUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            this.updater = updater;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            prefs.set(panes.get());
            updater.accept(new GuiSwingPreferences.PreferencesUpdateEvent(context, prefs));
        }

        public void apply(GuiPreferences p) {
            prefs.loadFrom(p);
            prefs.applyTo(panes.get());
        }

        public PreferencesForSplit getPrefs() {
            return prefs;
        }
    }

    public static class PreferencesForSplit implements GuiSwingPreferences.Preferences {
        protected List<PreferencesForSplitEntry> splits = new ArrayList<>();

        @Override
        public String getKey() {
            return "$split";
        }

        @Override
        public Object toJson() {
            return splits.stream()
                    .map(PreferencesForSplitEntry::toJson)
                    .collect(Collectors.toList());
        }

        public void applyTo(List<JSplitPane> panes) {
            for (int i = 0, l = panes.size(); i < l; ++i) {
                JSplitPane pane = panes.get(i);
                if (i < splits.size()) {
                    splits.get(i).applyTo(pane);
                }
            }
        }

        public void set(List<JSplitPane> panes) {
            for (int i = 0, l = panes.size(); i < l; ++i) {
                JSplitPane pane = panes.get(i);
                while (i >= splits.size()) {
                    splits.add(new PreferencesForSplitEntry());
                }
                splits.get(i).set(pane);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json != null && json instanceof List<?>) {
                splits.clear();
                for (Object item : (List<?>) json) {
                    PreferencesForSplitEntry e = new PreferencesForSplitEntry();
                    if (item instanceof Map<?,?>) {
                        Map<String,Object> map = (Map<String,Object>) item;
                        e.dividerLocation = (Integer) map.getOrDefault("dividerLocation", 0);
                        e.horizontal = (Boolean) map.getOrDefault("horizontal", true);
                    }
                    splits.add(e);
                }
            }
        }
    }

    public static class PreferencesForSplitEntry {
        public int dividerLocation;
        public boolean horizontal;

        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("dividerLocation", dividerLocation);
            map.put("horizontal", horizontal);
            return map;
        }

        public void applyTo(JSplitPane pane) {
            pane.setDividerLocation(dividerLocation);
        }

        public void set(JSplitPane pane) {
            dividerLocation = pane.getDividerLocation();
            horizontal = (pane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT);
        }
    }
}
