package autogui.swing;

import autogui.base.log.GuiLogManager;
import autogui.base.mapping.*;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * a swing view for {@link autogui.base.mapping.GuiReprObjectPane}
 *
 * <h3>swing-value</h3>
 * {@link ObjectPane#getSwingViewValue()}:
 *  latest set raw-object.
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
 *
 * <h3>preferences</h3>
 * <pre>
 *     "$split" : [ {
 *         "dividerLocation": Integer,
 *         "horizontal" : Boolean
 *     } ]
 * </pre>
 */
public class GuiSwingViewObjectPane implements GuiSwingView {
    protected GuiSwingMapperSet mapperSet;

    public GuiSwingViewObjectPane(GuiSwingMapperSet mapperSet) {
        this.mapperSet = mapperSet;
    }

    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        ObjectPane pane = createObjectPane(context, parentSpecifier);

        Map<GuiMappingContext,GuiSwingAction> actions = new LinkedHashMap<>();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiSwingElement e = mapperSet.view(subContext);
            if (e != null) {
                if (e instanceof GuiSwingView) {
                    createSubView(subContext, pane, (GuiSwingView) e);
                } else if (e instanceof GuiSwingAction) {
                    actions.put(subContext, (GuiSwingAction) e); //delay creation
                }
            }
        }

        //collect sub-tables for selection-change actions
        List<GuiSwingViewCollectionTable.CollectionTable> tables = collectTables(context, pane);

        actions.forEach((sc, a) ->
            createSubAction(sc, pane, a, tables));

        pane.fit();
        return pane;
    }

    public List<GuiSwingViewCollectionTable.CollectionTable> collectTables(GuiMappingContext context, ObjectPane pane) {
        List<GuiMappingContext> tableContexts = new ArrayList<>();
        findCollectionTables(context, tableContexts);
        return GuiSwingView.collectNonNullByFunction(pane, p -> {
            if (((ValuePane<?>) p) instanceof GuiSwingViewCollectionTable.CollectionTable &&
                    tableContexts.contains(p.getSwingViewContext())) {
                return (GuiSwingViewCollectionTable.CollectionTable) ((ValuePane<?>) p);
            } else {
                return null;
            }
        });
    }

    public void findCollectionTables(GuiMappingContext context, List<GuiMappingContext> tables) {
        for (GuiMappingContext sub : context.getChildren()) {
            if (sub.getRepresentation() instanceof GuiReprPropertyPane) {
                findCollectionTables(sub, tables);
            } else if (sub.isReprCollectionTable()) {
                tables.add(sub);
            }
        }
    }

    protected ObjectPane createObjectPane(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        return new ObjectPane(context, new SpecifierManagerDefault(parentSpecifier));
    }

    public void createSubView(GuiMappingContext subContext, ObjectPane pane, GuiSwingView view) {
        JComponent subComp = view.createView(subContext, pane::getSpecifier);
        if (subComp != null) {
            pane.addSubComponent(subComp, view.isComponentResizable(subContext));
        }
    }

    public void createSubAction(GuiMappingContext subContext, ObjectPane pane, GuiSwingAction action,
                                List<GuiSwingViewCollectionTable.CollectionTable> tables) {
        Action act = action.createAction(subContext, pane, tables);
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
        protected SpecifierManager specifierManager;
        protected JToolBar actionToolBar;
        protected JComponent contentPane;
        protected JComponent resizableSubComponents;
        protected PopupExtension popup;
        protected List<Action> actions = new ArrayList<>();
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;
        protected MenuBuilder.MenuLabel infoLabel;

        protected List<JSplitPane> splitPanes = new ArrayList<>();
        protected SplitPreferencesUpdater preferencesUpdater;
        protected SettingsWindow.LabelGroup labelGroup;

        protected GuiTaskClock viewClock = new GuiTaskClock(true);

        public ObjectPane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            init();
        }

        public void init() {
            initName();
            initLayout();
            initBorder();
            initContentPane();
            initLabelGroup();
            initPreferencesUpdater();
            initContextUpdate();
            initPopup();
            initFocus();
            initDragDrop();
        }

        public void initName() {
            setName(context.getName());
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
            GuiSwingView.setDescriptionToolTipText(context, this);
        }

        public void initLayout() {
            setLayout(new BorderLayout());
        }

        public void initBorder() {
            setBorder(new GuiSwingViewLabel.FocusBorder(this));
        }

        public void initContentPane() {
            contentPane = new JPanel();
            contentPane.setOpaque(false);
            ResizableFlowLayout layout = new ResizableFlowLayout(false, UIManagerUtil.getInstance().getScaledSizeInt(10));
            layout.setFitHeight(true);
            contentPane.setLayout(layout);
            setOpaque(false);
            add(contentPane, BorderLayout.CENTER);
        }

        public void initLabelGroup() {
            labelGroup = new SettingsWindow.LabelGroup();
        }

        public void initPreferencesUpdater() {
            preferencesUpdater = new SplitPreferencesUpdater(context, this::getSplitPanes);
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initPopup() {
            popup = new PopupExtension(this, new PopupCategorized(this::getSwingStaticMenuItems));
            setInheritsPopupMenu(true);
        }

        public void initFocus() {
            setFocusable(true);
        }

        public void initDragDrop() {
            GuiSwingView.setupTransferHandler(this, new ToStringTransferHandler(this));
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                menuItems = PopupCategorized.getMenuItems(
                        Arrays.asList(
                                infoLabel,
                                new ContextRefreshAction(context),
                                new ToStringCopyAction(this, context)),
                        GuiSwingJsonTransfer.getActions(this, context),
                        getActions());
            }
            return menuItems;
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
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock clock) {
            //nothing to do?
            viewClock.isOlderWithSet(clock);
        }


        public void addSubComponent(JComponent component, boolean resizable) {
            if (resizable) {
                addSubComponentResizable(component);
            } else {
                if (component instanceof NamedPane) {
                    JComponent label = ((NamedPane) component).getLabel();
                    if (label != null) {
                        labelGroup.addName(label);
                    }
                }
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

        public void fit() {
            labelGroup.fitWidth();
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
            return context.getSource().getValue();
        }

        /** special case: update the source */
        @Override
        public void setSwingViewValue(Object value) {
            GuiSwingView.updateFromGui(this, value, viewClock.increment());
            revalidate();
            repaint();
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            GuiSwingView.updateFromGui(this, value, viewClock.increment());
            revalidate();
            repaint();
        }

        @Override
        public void setSwingViewValue(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                //nothing to do
                revalidate();
                repaint();
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                //nothing to do
                revalidate();
                repaint();
            }
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs) {
            try {
                GuiSwingView.loadPreferencesDefault(this, prefs);
                preferencesUpdater.apply(prefs.getDescendant(getSwingViewContext()));
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        public void saveSwingPreferences(GuiPreferences prefs) {
            try {
                GuiSwingView.savePreferencesDefault(this, prefs);
                preferencesUpdater.getPrefs().saveTo(prefs.getDescendant(getSwingViewContext()));
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
            }
        }

        @Override
        public void setPreferencesUpdater(Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater) {
            preferencesUpdater.setUpdater(updater);
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }

        public PopupExtension getPopup() {
            return popup;
        }

        @Override
        public void setKeyStrokeString(String keyStrokeString) {
            infoLabel.setAdditionalInfo(keyStrokeString);
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
            String data = pane.getSwingViewContext().getRepresentation()
                            .toHumanReadableString(pane.getSwingViewContext(), pane.getSwingViewValue());
            return new StringSelection(data);
        }
    }

    public static class SplitPreferencesUpdater implements PropertyChangeListener {
        protected Consumer<GuiSwingPreferences.PreferencesUpdateEvent> updater;
        protected GuiMappingContext context;
        protected Supplier<List<JSplitPane>> panes;
        protected PreferencesForSplit prefs;
        protected boolean savingDisabled = false;

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
            if (!savingDisabled) {
                prefs.set(panes.get());
                if (updater != null) {
                    updater.accept(new GuiSwingPreferences.PreferencesUpdateEvent(context, prefs));
                }
            }
        }

        public void apply(GuiPreferences p) {
            savingDisabled = true;
            try {
                prefs.loadFrom(p);
                prefs.applyTo(panes.get());
            } finally {
                savingDisabled = false;
            }
        }

        public PreferencesForSplit getPrefs() {
            return prefs;
        }
    }

    public static class PreferencesForSplit implements GuiSwingPreferences.PreferencesByJsonEntry {
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
            if (json instanceof List<?>) {
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
