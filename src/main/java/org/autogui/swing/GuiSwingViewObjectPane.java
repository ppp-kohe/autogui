package org.autogui.swing;

import org.autogui.base.log.GuiLogManager;
import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.base.mapping.*;
import org.autogui.swing.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serial;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * a swing view for {@link GuiReprObjectPane}
 *
 * <h2>swing-value</h2>
 * {@link ObjectPane#getSwingViewValue()}:
 *  latest set raw-object.
 *  Currently, {@link ObjectPane#setSwingViewValue(Object)} also set context's value.
 *    TODO omit to update?
 *
 * <h2>history-value</h2>
 *  supported.
 *
 * <h2>string-transfer</h2>
 * no-transfer-handler.
 *  supported by {@link GuiSwingView.ToStringCopyAction} and
 *     {@link GuiRepresentation#toHumanReadableString(GuiMappingContext, Object)}.
 *
 * <h2>preferences</h2>
 * <pre>
 *     "$split" : [ {
 *         "dividerLocation": Integer,
 *         "horizontal" : Boolean
 *     } ]
 * </pre>
 */
@SuppressWarnings("this-escape")
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

        pane.afterActionsAdded();
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
            pane.addSubComponent(subComp, view.isComponentResizable(subContext), subContext);
        }
    }

    public void createSubAction(GuiMappingContext subContext, ObjectPane pane, GuiSwingAction action,
                                List<GuiSwingViewCollectionTable.CollectionTable> tables) {
        Action act = action.createAction(subContext, pane, tables);
        if (act != null) {
            pane.addAction(act, subContext);
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
        @Serial private static final long serialVersionUID = 1L;
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

        /** recording mapping between prop (context) name to subcomponent
         * @since 1.5 */
        protected Map<String, JComponent> childContextToComponents = new HashMap<>();

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
            setBorder(new FocusBorder(this));
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
                                new ContextRefreshAction(context, this),
                                new ToStringCopyAction(this, context),
                                new FlipSplitOrientationAction(this::getSplitPanes)),
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

        /**
         * adding a child component
         * @param component the component for the subContext
         * @param resizable true if the component is resizable
         * @param subContext a target child of the context of the pane
         * @since 1.5
         */
        public void addSubComponent(JComponent component, boolean resizable, GuiMappingContext subContext) {
            childContextToComponents.put(subContext.getName(), component);
            addSubComponent(component, resizable);
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
            pane.setOneTouchExpandable(true);
            pane.setOpaque(false);
            pane.setBorder(BorderFactory.createEmptyBorder());
            pane.setDividerLocation(prevWidth / (prevWidth + nextWidth));

            this.splitPanes.add(pane);

            pane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, preferencesUpdater);
            pane.addPropertyChangeListener(JSplitPane.ORIENTATION_PROPERTY, preferencesUpdater);
            return pane;
        }

        public void afterActionsAdded() {
            fit();
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this);
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

        /**
         * adding the action created from subContext
         * @param action the action for subContext
         * @param subContext a child context of the context of the pane
         * @since 1.5
         */
        public void addAction(Action action, GuiMappingContext subContext) {
            SearchTextField field = getTargetForTextAction(subContext);
            if (field != null) {
                addActionAsTextAction(field, action);
            } else {
                addAction(action);
            }
        }

        /**
         * obtains action target for a sub-context.
         *  if the action is "xyzAction" then, searches the "xyz" property, and it is a {@link SearchTextField},
         *   then it can combine the action and the field as like [ field [action] ]
         * @param subContext the context of the action
         * @return non-null target-field if exists, or null
         */
        protected SearchTextField getTargetForTextAction(GuiMappingContext subContext) {
            String name = subContext.getName();
            String suffix = "Action";
            if (name.length() > suffix.length() && name.endsWith(suffix)) { //xyzAction -> for property xyz
                String targetName = name.substring(0, name.length() - suffix.length());
                JComponent childComponent = childContextToComponents.get(targetName);
                if (childComponent instanceof NamedPane) {
                    childComponent = ((NamedPane) childComponent).getContentPane();
                }
                if (childComponent instanceof SearchTextField) {
                    return (SearchTextField) childComponent;
                }
            }
            return null;
        }

        private void addActionAsTextAction(SearchTextField targetText, Action action) {
            putActionMap(action);
            targetText.getButtonsPane().add(new SearchTextActionButton(action), 0);
        }

        private void putActionMap(Action action) {
            Object name = action.getValue(Action.NAME);
            if (name != null) {
                getActionMap().put(name, action);
            }
            actions.add(action);
        }

        public void addAction(Action action) {
            putActionMap(action);
            if (actionToolBar == null) {
                initActionToolBar();
            }
            actionToolBar.add(new GuiSwingIcons.ActionButton(action));
        }

        public void initActionToolBar() {
            actionToolBar = new JToolBar();
            actionToolBar.setFloatable(false);
            actionToolBar.setOpaque(false);
            actionToolBar.setInheritsPopupMenu(true);
            new ToolBarHiddenMenu().addTo(actionToolBar);
            add(actionToolBar, BorderLayout.PAGE_START);
            if (popup != null) {
                popup.addListenersTo(actionToolBar);
            }
        }

        @Override
        public Object getSwingViewValue() {
            return context.getSource().getValue();
        }

        /** special case: update the source */
        @Override
        public void setSwingViewValue(Object value) {
            updateFromGui(value, viewClock.increment());
            revalidate();
            repaint();
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            GuiSwingView.updateViewClockSync(viewClock, context);
            updateFromGui(value, viewClock.increment());
            revalidate();
            repaint();
        }

        public void updateFromGui(Object v, GuiTaskClock viewClock) {
            GuiSwingView.updateFromGui(this, v, viewClock);
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
        public void loadSwingPreferences(GuiPreferences prefs, GuiSwingPreferences.PrefsApplyOptions options) {
            try {
                options.begin(this, prefs, GuiSwingPreferences.PrefsApplyOptionsLoadingTargetType.View);
                GuiSwingView.loadPreferencesDefault(this, prefs, options);
                options.apply(preferencesUpdater, prefs.getDescendant(getSwingViewContext()));
            } catch (Exception ex) {
                GuiLogManager.get().logError(ex);
                options.end(this, prefs, GuiSwingPreferences.PrefsApplyOptionsLoadingTargetType.View);
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

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
        }
    }

    /**
     * action for flipping orientation of all split-panes
     * @since 1.5
     */
    public static class FlipSplitOrientationAction extends AbstractAction {
        protected Supplier<List<JSplitPane>> splits;

        public FlipSplitOrientationAction(Supplier<List<JSplitPane>> splits) {
            this.splits = splits;
            putValue(NAME, "Flip Split Orientation");
        }

        @Override
        public boolean isEnabled() {
            return !splits.get().isEmpty();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            splits.get()
                    .forEach(split -> split.setOrientation(
                            split.getOrientation() == JSplitPane.HORIZONTAL_SPLIT ?
                                JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT));
        }
    }

    /**
     * button used for a buttonPane member of {@link SearchTextField}
     * @since 1.5
     */
    public static class SearchTextActionButton extends GuiSwingIcons.ActionButton {
        @Serial private static final long serialVersionUID = -1;
        public SearchTextActionButton(Action a) {
            super(a);
            setBorder(BorderFactory.createEmptyBorder());
            setMargin(new Insets(0, 0, 0, 0));
            setHideActionText(true);
            updateIcon(this::getIcon, this::setIcon);
            updateIcon(this::getPressedIcon, this::setPressedIcon);
        }

        private void updateIcon(Supplier<Icon> getter, Consumer<Icon> setter) {
            Icon icon = getter.get();
            if (icon != null) {
                setter.accept(new SearchTextFieldFilePath.IconWrapper(icon, 2 * icon.getIconWidth() / 3)); // 2/3 size
            }
        }
    }

    public static class ToStringTransferHandler extends TransferHandler {
        @Serial private static final long serialVersionUID = 1L;
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
        public PreferencesForSplit() {}
        @Override
        public String getKey() {
            return "$split";
        }

        /**
         * @return the entry list; modification will be affected to the prefs
         * @since 1.6.3
         */
        public List<PreferencesForSplitEntry> getSplits() {
            return splits;
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
            GuiSwingPreferences.setAsList(splits, json, Map.class, PreferencesForSplitEntry::new);
        }
    }

    public static class PreferencesForSplitEntry {
        public int dividerLocation;
        public boolean horizontal;

        public PreferencesForSplitEntry() {}

        /**
         * @param map the JSON map for the entry containing "divierLocation" and "horizontal"
         */
        public PreferencesForSplitEntry(Map<?, ?> map) {
            dividerLocation = GuiSwingPreferences.getAs(map, Integer.class, "dividerLocation", 0);
            horizontal = GuiSwingPreferences.getAs(map, Boolean.class,"horizontal", true);
        }

        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("dividerLocation", dividerLocation);
            map.put("horizontal", horizontal);
            return map;
        }

        public void applyTo(JSplitPane pane) {
            pane.setDividerLocation(dividerLocation);
            pane.setOrientation(horizontal ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT);
        }

        public void set(JSplitPane pane) {
            dividerLocation = pane.getDividerLocation();
            horizontal = (pane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT);
        }
    }
}
