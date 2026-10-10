package org.autogui.swing;

import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.*;
import org.autogui.swing.prefs.GuiSwingPrefsApplyOptions;
import org.autogui.swing.table.TableTargetColumnAction;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.util.SettingsWindow;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * the interface for regular pane factory, creating a {@link GuiSwingView.ValuePane}.
 */
public interface GuiSwingView extends GuiSwingElement {
    JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier);

    default boolean isComponentResizable(GuiMappingContext context) {
        return false;
    }

    /**
     * the base mixin interface to a {@link JComponent} bound to an object or a property via a context ({@link #getSwingViewContext()}).
     * <p>
     *     a pane will need to manage a {@link GuiTaskClock} as the view-clock.
     * @param <ValueType> the type of the bound value
     */
    interface ValuePane<ValueType> extends GuiSwingFeature {
        @Override
        default List<?> getSwingViewSubNodes() {
            return getSwingViewSubNodesSameDisplay();
        }

        @Override
        default List<?> getSwingViewSubNodesSameDisplay() {
            return Arrays.asList(asSwingViewComponent().getComponents());
        }

        /** @return basically, the source value of the context, held by the component without touching the context.  */
        ValueType getSwingViewValue();

        /** update GUI display,
         *   and it does NOT update the target model value.
         * processed under the event thread.
         *  also increment the view-clock.
         * @param value the new value
         * */
        void setSwingViewValue(ValueType value);

        /**
         * update GUI display as a table-cell.
         * the default impl. is just calling {@link #setSwingViewValue(Object)}
         * @param table the table
         * @param value the table row value as Object
         * @param row the view row index
         * @param column the view column index
         * @since 1.6
         */
        @SuppressWarnings("unchecked")
        default void setSwingViewValueForTable(JTable table, Object value, int row, int column) {
            setSwingViewValue((ValueType) value);
        }

        /**
         *  update if the clock is newer than the current view-clock of the pane.
         *  the view-clock becomes the given clock.
         *  Note: instead, {@link #setSwingViewValue(Object)} increments the current clock
         * @param value the new value
         * @param clock the clock of the value: if the clock is newer than the current view-clock of the pane, it can update
         */
        void setSwingViewValue(ValueType value, GuiTaskClock clock);

        /** update the GUI display and the model. processed under the event thread.
         * also increment the view-clock
         * @param value the new value
         * */
        void setSwingViewValueWithUpdate(ValueType value);

        /**
         * update the GUI display and the model if the clock is newer than the current view-clock of the pane.
         * the view-clock becomes the given clock.
         * @param value the new value
         * @param clock the clock of the value
         */
        void setSwingViewValueWithUpdate(ValueType value, GuiTaskClock clock);

        /**
         * a typical implementation becomes like the following code:
         *  <pre>
         *      if (menuItems == null) { menuItems = {@link PopupCategorized}.getMenuItems(actions, menus); }
         *      return menuItems;
         *  </pre>
         *  <p>
         *      the method returns original menu items for the pane. so,
         *       if a wrapper pane has a wrapped sub-pane added as a child component,
         *         it should not include items of the wrapped sub-pane.
         * @return a list of actions (or menu-components) statically determined,
         *     that should be always same instances
         */
        List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems();

        /**
         * a typical implementation if there are no dynamic items becomes like the following code:
         * <pre>
         *     if (popup == null) { popup = new {@link PopupExtension}(this,
         *                                    new {@link PopupCategorized}(this::getSwingStaticMenuItems)); }
         *     return popup.getMenuBuilder();
         * </pre>
         * @return a menu builder
         */
        PopupExtension.PopupMenuBuilder getSwingMenuBuilder();

        /**
         *
         * @param verticalAlways the flag of showing the vertical scroll-bar
         * @param horizontalAlways the flag of showing the horizontal scroll-bar
         * @return a {@link GuiSwingViewWrapper.ValueScrollPane} which wraps the pane
         */
        default GuiSwingViewWrapper.ValueScrollPane<ValueType> wrapSwingScrollPane(boolean verticalAlways, boolean horizontalAlways) {
            return new GuiSwingViewWrapper.ValueScrollPane<>(asSwingViewComponent(),
                    verticalAlways ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    horizontalAlways ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }

        /**
         * @return a {@link GuiSwingViewWrapper.ValueWrappingPane} which wraps the pane
         */
        default GuiSwingViewWrapper.ValueWrappingPane<ValueType> wrapSwingPane() {
            return new GuiSwingViewWrapper.ValueWrappingPane<>(asSwingViewComponent());
        }

        default GuiSwingViewPropertyPane.PropertyPane wrapSwingProperty() {
            return new GuiSwingViewPropertyPane.PropertyWrapperPane(getSwingViewContext(), true, this);
        }

        default GuiSwingViewPropertyPane.NamedPropertyPane wrapSwingNamed() {
            return new GuiSwingViewPropertyPane.NamedPropertyPane(getSwingViewContext().getDisplayName(), getSwingViewContext().getName(),
                    asSwingViewComponent(), getSwingMenuBuilder());
        }

        /**
         * @return this
         */
        default JComponent asSwingViewComponent() {
            return (JComponent) this;
        }

        /**
         * only save the component itself, not including children.
         *  it can use {@link #savePreferencesDefault(JComponent, GuiPreferences)}
         * @param prefs target prefs or ancestor of the target;
         *              actual target can be obtained by {@link GuiPreferences#getDescendant(GuiMappingContext)}
         */
        default void saveSwingPreferences(GuiPreferences prefs) {
            GuiSwingView.savePreferencesDefault(asSwingViewComponent(), prefs);
        }

        /**
         * a sub-class which wraps another value-pane should overrides the method
         *   in order to omit needless value setting.
         *   it can use {@link GuiSwingView#loadPreferencesDefault(JComponent, GuiPreferences)}
         * @param prefs target prefs or ancestor of the target;
         *              actual target can be obtained by {@link GuiPreferences#getDescendant(GuiMappingContext)}
         * @param options options for applying
         * @since 1.4
         */
        default void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            GuiSwingView.loadPreferencesDefault(asSwingViewComponent(), prefs, options);
        }

        default void requestSwingViewFocus() {
            asSwingViewComponent().requestFocusInWindow();
        }

        /**
         * @return creates a key-stroke from it's context. for wrapping panes, returns null
         */
        default KeyStroke getSwingFocusKeyStroke() {
            return GuiSwingKeyBinding.getKeyStroke(getSwingViewContext().getAcceleratorKeyStroke());
        }

        /**
         * called from a value history menu. the default impl. just calls {@link #setSwingViewValueWithUpdate(Object)}.
         * @param value the new value. usually a ValueType.
         */
        @SuppressWarnings("unchecked")
        default void setSwingViewHistoryValue(Object value) {
            setSwingViewValueWithUpdate((ValueType) value);
        }

        default boolean isSwingCurrentValueSupported() {
            return getSwingViewContext().isHistoryValueSupported();
        }
    }

    /**
     * @param target the target component
     * @param value the searched value
     * @return a descendant value pane holding the value, or null.
     *    wrappers holding the same context are avoided.
     */
    @SuppressWarnings("unchecked")
    static ValuePane<Object> getDescendantByValue(Object target, Object value) {
        return GuiSwingFeature.traverseFindFirstIf(target, ValuePane.class,
                p -> Objects.equals(p.getSwingViewValue(), value), GuiSwingFeature.TraversalOption.NonTop);
    }

    /**
     * @param target the target component
     * @param valuePredicate the condition holds the searched value
     * @return a first descendant value pane holding a value matched by the predicate, or null.
     *    wrappers holding the same context are avoided.
     */
    @SuppressWarnings("unchecked")
    static ValuePane<Object> getDescendantByValueIf(Object target, Predicate<Object> valuePredicate) {
        return GuiSwingFeature.traverseFindFirstIf(target, ValuePane.class,
                p -> valuePredicate.test(p.getSwingViewValue()), GuiSwingFeature.TraversalOption.NonTop);
    }

    /**
     *
     * @param target the root node
     * @param name the context-name
     * @return the first descendant value-pane matched by the context-name
     * @since 1.9
     */
    @SuppressWarnings("unchecked")
    static ValuePane<Object> getDescendantByName(Object target, String name) {
        return GuiSwingFeature.traverseFindFirstIf(target, ValuePane.class,
                p -> p.getSwingViewContext().getName().equals(name), GuiSwingFeature.TraversalOption.NonTop);
    }

    /**
     * @param target the target component
     * @param name the searched context name
     * @return a child (or descendant for wrappers) value pane holding the named context, or null.
     *    wrappers holding the same context are avoided.
     */
    @SuppressWarnings("unchecked")
    static ValuePane<Object> getChildByName(Object target, String name) {
        return GuiSwingFeature.getChildByName(target, ValuePane.class, name);
    }

    /**
     * @param name the context name of the action
     * @return an action defined in the context of the pane. as the default implementation,
     *    it searches {@link ValuePane#getSwingStaticMenuItems()}
     */
    static GuiSwingActionDefault.ExecutionAction getActionByName(Object target, String name) {
        return getActionDefault(target, e ->  e.getContext() != null && Objects.equals(e.getContext().getName(), name));
    }

    /**
     *
     * @param target the parent pane
     * @param context the searched context
     * @return  the first child action of the context
     * @since 1.9
     */
    static GuiSwingActionDefault.ExecutionAction getActionByContext(Object target, GuiMappingContext context) {
        return getActionDefault(target, e ->  Objects.equals(e.getContext(), context));
    }

    /**
     *
     * @param target the parent pane
     * @param context the searched context
     * @return the first descendant action of the context
     * @since 1.9
     */
    static GuiSwingActionDefault.ExecutionAction getDescendantActionByContext(Object target, GuiMappingContext context) {
        return GuiSwingFeature.traverseFindFirst(target, ValuePane.class, getActionMapper(e -> Objects.equals(e.getContext(), context)));
    }

    /**
     *
     * @param target the parent pane
     * @param predicate the tested predicate
     * @return the first child action matched by the predicate
     * @since 1.9
     */
    static GuiSwingActionDefault.ExecutionAction getActionDefault(Object target, Predicate<GuiSwingActionDefault.ExecutionAction> predicate) {
        return GuiSwingFeature.findFirstChild(target, ValuePane.class, getActionMapper(predicate), GuiSwingFeature.TraversalOption.OncePertContext);
    }

    /**
     * @param predicate the test
     * @return action testing and getter with listing menu-items
     * @since 1.9
     */
    @SuppressWarnings("rawtypes")
    private static Function<ValuePane, GuiSwingActionDefault.ExecutionAction> getActionMapper(Predicate<GuiSwingActionDefault.ExecutionAction> predicate) {
        return pane -> {
            for (var item : pane.getSwingStaticMenuItems()) {
                if (item instanceof GuiSwingActionDefault.ExecutionAction a && predicate.test(a)) {
                    return a;
                }
            }
            return null;
        };
    };

    //--------------------------------------

    /**
     *
     * @param obj the parent component
     * @param type the target type
     * @return a first descendant  of the type (including obj)
     * @param <RetType> the retruned component type
     * @since 1.9
     */
    static <RetType extends Component> RetType findDescendantByType(Object obj, Class<RetType> type) {
        return findDescendantByType(obj, type, p -> true);
    }

    /**
     *
     * @param obj the parent component
     * @param type the target type
     * @param condition the tested condition
     * @return a first descendant of the type with hoding the condition (including obj)
     * @param <RetType> the returned compoent type
     * @since 1.9
     */
    static <RetType extends Component> RetType findDescendantByType(Object obj, Class<RetType> type, Predicate<RetType> condition) {
        return GuiSwingFeature.traverseFindFirstIf(obj, type, condition);
    }

    /**
     *
     * @param obj the parent component
     * @param type the target type
     * @return a first descendant  of the type (including obj)
     * @param <RetType> the returned value-pane type
     * @since 1.9
     */
    static <RetType extends ValuePane<?>> RetType findDescendantValuePaneByType(Object obj, Class<RetType> type) {
        return findDescendantValuePaneByType(obj, type, p -> true);
    }

    /**
     *
     * @param obj the parent component
     * @param type the target type
     * @param condition the tested condition
     * @return a first descendant of the type with hoding the condition (including obj)
     * @param <RetType> the returned value-pane type
     * @since 1.9
     */
    static <RetType extends ValuePane<?>> RetType findDescendantValuePaneByType(Object obj, Class<RetType> type, Predicate<RetType> condition) {
        return GuiSwingFeature.traverseFindFirstIf(obj, type, condition);
    }

    //--------------------------------------

    /**
     * obtains context clock and set to the viewClock if the viewClock is older:
     *  typically used by {@link ValuePane#setSwingViewValueWithUpdate(Object)}
     * @param viewClock the changed view-clock
     * @param context this source context
     */
    static void updateViewClockSync(GuiTaskClock viewClock, GuiMappingContext context) {
        viewClock.isOlderWithSet(context.getContextClock());
    }

    /**
     * send the value to the context of the pane by {@link GuiReprValue#updateFromGui(GuiMappingContext, Object, GuiReprValue.ObjectSpecifier, GuiTaskClock)},
     *    with copying the clock and the specifier of the pane in the caller's thread.
     * @param pane the source pane
     * @param value the new value to be sent
     * @param viewClock the view-clock of the value
     */
    static void updateFromGui(ValuePane<?> pane, Object value, GuiTaskClock viewClock) {
        GuiMappingContext context = pane.getSwingViewContext();
        GuiReprValue repr = context.getReprValue();
        if (repr.isEditable(context)) {
            GuiReprValue.ObjectSpecifier spec = pane.getSpecifier();
            GuiTaskClock clock = viewClock.copy();

            pane.executeContextTask(() -> {
                repr.updateFromGui(context, value, spec, clock); //callbacks are done by invokeLater
                return null;
            }, null);
        }
    }

    static void setDescriptionToolTipText(GuiMappingContext context, JComponent comp) {
        String d = context.getDescription();
        if (!d.isEmpty()) {
            comp.setToolTipText(d);
        }
    }

    /**
     * the interface for a client of {@link SettingsWindow} implemented by a {@link GuiSwingView.ValuePane}.
     */
    interface SettingsWindowClient extends SettingsWindow.SettingsWindowClientBase {
    }


    /**
     * {@link #setLastHistoryValue(GuiPreferences, ValuePane, GuiSwingPrefsApplyOptions)}
     *   with {@link GuiSwingPrefsApplyOptions#APPLY_OPTIONS_DEFAULT}
     * @param prefs the source preferences
     * @param pane the target pane
     */
    static void setLastHistoryValue(GuiPreferences prefs, ValuePane<Object> pane) {
        setLastHistoryValue(prefs, pane, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
    }

    /**
     * obtain {@link GuiPreferences#getCurrentValue()}
     *   or a last value of {@link GuiPreferences#getHistoryValues()}
     *      (only when the context {@link GuiMappingContext#isHistoryValueStored(Object)} with null),
     *   and set the value by {@link GuiSwingView.ValuePane#setSwingViewHistoryValue(Object)}.
     *   <p>
     *  {@link GuiSwingView#loadPreferencesDefault(JComponent, GuiPreferences)} automatically calls the method.
     * @param prefs the source preferences
     * @param pane the target pane
     * @param options the options
     * @since 1.4
     */
    static void setLastHistoryValue(GuiPreferences prefs, ValuePane<Object> pane, GuiSwingPrefsApplyOptions options) {
        if (options.isSkippingValue()) {
            return;
        }

        boolean loadAsJson = prefs.isContextTypeIsJsonSupport();
        if (!pane.isSwingEditable() && !loadAsJson) {
            return;
        }

        try {
            options.begin(pane, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.CurrentValue);
            Object v = prefs.getCurrentValue();
            if (v != null || loadAsJson) {
                if (v != null && pane.isSwingCurrentValueSupported()) {
                    options.setLastHistoryValueBySwingViewHistoryValue(pane, prefs, v);
                } else if (loadAsJson) {
                    options.setLastHistoryValueByPrefsJsonSupported(pane, prefs, prefs.getCurrentValueAsJsonSupported());
                }
            } else if (pane.getSwingViewContext().isHistoryValueStored(null)) {
                List<GuiPreferences.HistoryValueEntry> es = new ArrayList<>(prefs.getHistoryValues());
                es.sort(Comparator.comparing(GuiPreferences.HistoryValueEntry::getTime));
                if (!es.isEmpty()) {
                    options.setLastHistoryValueBySwingViewHistoryValue(pane, prefs, es.getLast());
                }
            }
        } finally {
            options.end(pane, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.CurrentValue);
        }
    }

    /**
     * save the current value and history values of the pane to given preferences
     * @param pane a {@link GuiSwingView.ValuePane}
     * @param prefs a target preferences
     */
    static void savePreferencesDefault(JComponent pane, GuiPreferences prefs) {
        if (pane instanceof ValuePane<?> vPane) {
            GuiMappingContext context = vPane.getSwingViewContext();
            GuiPreferences targetPrefs = prefs.getDescendant(context);
            if (vPane.isSwingCurrentValueSupported()) {
                targetPrefs.setCurrentValue(vPane.getSwingViewValue());
            } else if (targetPrefs.isContextTypeIsJsonSupport()) {
                targetPrefs.setCurrentValueAsJsonSupported(vPane.getPrefsJsonSupported());
            }
            if (context.isHistoryValueSupported()) {
                GuiPreferences ctxPrefs = context.getPreferences();
                if (!ctxPrefs.equals(targetPrefs)) {
                    try (var ctxLock = ctxPrefs.lock();
                        var lock = targetPrefs.lock()) {
                        ctxLock.use();
                        lock.use();
                        targetPrefs.setHistoryValues(ctxPrefs.getHistoryValues());
                    }
                }
            }
        }
    }

    /**
     * {@link #loadPreferencesDefault(JComponent, GuiPreferences, GuiSwingPrefsApplyOptions)}
     *  with {@link GuiSwingPrefsApplyOptions#APPLY_OPTIONS_DEFAULT}
     * @param pane a target {@link GuiSwingView.ValuePane}
     * @param prefs a source preferences
     */
    static void loadPreferencesDefault(JComponent pane, GuiPreferences prefs) {
        loadPreferencesDefault(pane, prefs, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
    }

    /**
     * load the current value and history values of the prefs to the pane
     * @param pane a target {@link GuiSwingView.ValuePane}
     * @param prefs a source preferences
     * @param options options for applying
     * @since 1.4
     */
    @SuppressWarnings("unchecked")
    static void loadPreferencesDefault(JComponent pane, GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
        try {
            options.begin(pane, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
            if (pane instanceof ValuePane<?> valuePane) {
                GuiMappingContext context = valuePane.getSwingViewContext();
                GuiPreferences targetPrefs = prefs.getDescendant(context);
                setLastHistoryValue(targetPrefs, (ValuePane<Object>) valuePane, options);

                if (context.isHistoryValueSupported()) {
                    GuiPreferences ctxPrefs = context.getPreferences();
                    if (options.hasHistoryValues(targetPrefs, ctxPrefs)) {
                        options.begin(targetPrefs.getHistoryValues(), targetPrefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.HistoryValues);
                        try (var lock = targetPrefs.lock();
                            var ctxLock = ctxPrefs.lock()) {
                            lock.use();
                            ctxLock.use();
                            targetPrefs.getHistoryValues().stream()
                                    .sorted(Comparator.comparing(GuiPreferences.HistoryValueEntry::getTime))
                                    .forEachOrdered(e -> options.addHistoryValue(e, ctxPrefs));
                        } finally {
                            options.end(targetPrefs.getHistoryValues(), targetPrefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.HistoryValues);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            GuiLogManager.get().logError(ex);
            options.end(pane, prefs, GuiSwingPrefsApplyOptions.PrefsApplyOptionsLoadingTargetType.View);
        }
    }

    /**
     * a refresh-action calls the method in order to clear view-clocks of sub-panes
     * @param pane a root component
     */
    @SuppressWarnings("rawtypes")
    static void prepareForRefresh(GuiSwingFeature pane) {
        GuiSwingFeature.traverse(pane, GuiSwingFeature.class, GuiSwingFeature::prepareForRefresh);
    }

    //--------------------------------------

    /**
     * @param pane the method obtains static-methods from the pane.
     *              so it needs to be called after building valid menu items.
     */
    static void setupKeyBindingsForStaticMenuItems(ValuePane<?> pane) {
        setupKeyBindingsForStaticMenuItems(pane, pane.asSwingViewComponent(), a -> {
            a.putValue(Action.ACCELERATOR_KEY, null);
            return false;
        });
    }

    static void setupKeyBindingsForStaticMenuItems(ValuePane<?> pane, Predicate<Action> overwrite) {
        setupKeyBindingsForStaticMenuItems(pane, pane.asSwingViewComponent(), overwrite);
    }

    static void setupKeyBindingsForStaticMenuItems(ValuePane<?> pane, JComponent targetPane, Predicate<Action> overwrite) {
        pane.getSwingStaticMenuItems().stream()
                .map(PopupCategorized::getMenuItemAction)
                .filter(Objects::nonNull)
                .forEach(a -> setupKeyBindingsForStaticMenuItemAction(targetPane, a, overwrite));

        pane.getSwingStaticMenuItems().stream()
                .map(PopupCategorized::getJMenuItem)
                .filter(Objects::nonNull)
                .forEach(i -> setupKeyBindingsForStaticJMenuSubItems(targetPane, i, overwrite));
    }

    static boolean setupKeyBindingsForStaticMenuItemAction(JComponent pane, Action a, Predicate<Action> overwrite) {
        KeyStroke s = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
        if (s != null) {
            InputMap inputs = pane.getInputMap();
            ActionMap actions = pane.getActionMap();
            Object ks = inputs.get(s);

            boolean put;
            if (ks != null && Arrays.asList(inputs.keys()).contains(s)) { //actually defined in the inputs
                if (!Objects.equals(actions.get(inputs.get(s)), a)) {
                    put = overwrite.test(a);
                } else {
                    put = true;
                }
            } else {
                put = true;
            }

            if (put) {
                String name = (String) a.getValue(Action.NAME);
                inputs.put(s, name);
                actions.put(name, a);
            }
            return put;
        } else {
            return false;
        }
    }

    static void setupKeyBindingsForStaticJMenuSubItems(JComponent pane, JMenuItem i, Predicate<Action> overwrite) {
        Arrays.stream(i.getComponents())
                .filter(AbstractButton.class::isInstance)
                .map(AbstractButton.class::cast)
                .map(AbstractButton::getAction)
                .filter(Objects::nonNull)
                .forEach(a -> setupKeyBindingsForStaticMenuItemAction(pane, a, overwrite));
        Arrays.stream(i.getComponents())
                .filter(JMenuItem.class::isInstance)
                .map(JMenuItem.class::cast)
                .forEach(si -> setupKeyBindingsForStaticJMenuSubItems(pane, si, overwrite));
    }

    /** an action for refreshing the value of the context of the pane.
     * <p>
     *  first it calls {@link GuiSwingView#prepareForRefresh(GuiSwingFeature)}
     *      which traverses the target pane and calls {@link GuiSwingFeature#prepareForRefresh()}  for each component.
     * <p>
     *  next clears values by {@link GuiMappingContext#clearSourceSubTree()} and calls {@link GuiMappingContext#updateSourceSubTree()}
     *     */
    class ContextRefreshAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;

        protected GuiMappingContext context;
        protected ValuePane<?> pane;

        @SuppressWarnings("this-escape")
        public ContextRefreshAction(GuiMappingContext context, ValuePane<?> pane) {
            putValue(NAME, "Refresh");
            putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_R,
                    PopupExtension.getMenuShortcutKeyMask(), KeyEvent.SHIFT_DOWN_MASK));
            this.pane = pane;
            this.context = context;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (pane != null) {
                pane.refreshByContext();
            } else {
                GuiSwingActionDefault.ActionPreparation.prepareAction(e);
                context.clearSourceSubTree();
                context.updateSourceSubTree();
            }
        }

        @Override
        public String getCategory() {
            return PopupCategorized.CATEGORY_LABEL;
        }

        @Override
        public String getSubCategory() {
            return PopupCategorized.SUB_CATEGORY_LABEL_ACTION;
        }
    }

    /**
     * a general to-string copy action by using {@link GuiRepresentation#toHumanReadableString(GuiMappingContext, Object)}
     */
    class ToStringCopyAction extends GuiSwingTaskRunner.ContextAction implements TableTargetColumnAction {
        @Serial private static final long serialVersionUID = 1L;

        protected ValuePane<?> pane;

        @SuppressWarnings("this-escape")
        public ToStringCopyAction(ValuePane<?> pane, GuiMappingContext context) {
            super(context);
            putValue(NAME, "Copy as Text");
            putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_C,
                    PopupExtension.getMenuShortcutKeyMask()));
            this.pane = pane;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object v = pane.getSwingViewValue();
            executeContextTask(
                    () -> toString(v),
                    r -> r.executeIfPresent(this::copy));
        }

        public String toString(Object v) {
            return getContext().getRepresentation().toHumanReadableString(getContext(), v);
        }


        public void copy(String data) {
            if (data != null) {
                Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection sel = new StringSelection(data);
                board.setContents(sel, sel);
            }
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            List<Object> vs = target.getSelectedCellValues();
            executeContextTask(() -> vs.stream()
                    .map(this::toString)
                    .collect(Collectors.joining("\n")),
                    r -> r.executeIfPresent(this::copy));
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_COPY;
        }
    }


    //--------------------------------------

    /**
     * set up the transfer handler for a component.
     * also set up drag-gesture for the COPY operation.
     * if the component is not a {@link JTextComponent},
     *  then it sets up copy and paste actions to the action-map,
     *  and input key-strokes (Cmd+C and Cmd+V) to the input-map (which will be activated when the component is focusable).
     * @param component the target component
     * @param handler the transfer-handler to be set
     */
    static void setupTransferHandler(JComponent component, TransferHandler handler) {
        setupTransferHandler(component, handler, new Integer[0]);
    }

    /**
     *
     * @param component the target component
     * @param handler the transfer-handler to be set
     * @param modifierKeys {@link KeyEvent#VK_SHIFT}, {@link KeyEvent#VK_META}, {@link KeyEvent#VK_ALT} or {@link KeyEvent#VK_ALT_GRAPH}
     * @since 1.5
     */
    static void setupTransferHandler(JComponent component, TransferHandler handler, Integer... modifierKeys) {
        component.setTransferHandler(handler);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(component, DnDConstants.ACTION_COPY, e -> {
            List<Integer> ks = Arrays.asList(modifierKeys);
            InputEvent ie = e.getTriggerEvent();
            if (ks.isEmpty()
                    || (ks.contains(KeyEvent.VK_SHIFT) && ie.isShiftDown())
                    || (ks.contains(KeyEvent.VK_META) && ie.isMetaDown())
                    || (ks.contains(KeyEvent.VK_ALT) && ie.isAltDown())
                    || (ks.contains(KeyEvent.VK_ALT_GRAPH) && ie.isAltGraphDown())) {
                component.getTransferHandler().exportAsDrag(component, ie, TransferHandler.COPY);
            }
        });
        if (!(component instanceof JTextComponent)) {
            setupCopyAndPasteActions(component);
        }
    }

    static void setupCopyAndPasteActions(JComponent component) {
        Action copy = TransferHandler.getCopyAction();
        component.getActionMap().put(copy.getValue(Action.NAME), copy);
        Action paste = TransferHandler.getPasteAction();
        component.getActionMap().put(paste.getValue(Action.NAME), paste);

        component.getInputMap().put(PopupExtension.getKeyStroke(KeyEvent.VK_C,
                PopupExtension.getMenuShortcutKeyMask()), copy.getValue(Action.NAME));
        component.getInputMap().put(PopupExtension.getKeyStroke(KeyEvent.VK_V,
                PopupExtension.getMenuShortcutKeyMask()), paste.getValue(Action.NAME));
    }

    //--------------------------------------

    /** a factory interface for {@link GuiReprValue.ObjectSpecifier}.
     *   With regular(non-list) objects, it can be created by {@link #specifierManager(Supplier)} for a parent specifier,
     *   or {@link #specifierManagerRoot()} for the root.
     * */
    interface SpecifierManager extends Supplier<GuiReprValue.ObjectSpecifier>  {
        default GuiReprValue.ObjectSpecifier getSpecifier() {
            return get();
        }
    }

    /**
     * @return the none
     * @since 1.7
     */
    static SpecifierManager specifierManagerRoot() {
        return () -> GuiReprValue.NONE;
    }

    /**
     * the default factory
     * @param parentSpecifier the parent specifieler
     * @return a new instance
     * @since 1.7
     */
    static SpecifierManagerDefault specifierManager(Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        return new SpecifierManagerDefault(parentSpecifier);
    }

    /** a default implementation of specifier-manager:
     *   it creates a {@link GuiReprValue.ObjectSpecifier} with the parent specifier and caches it.
     *    if the parent factory returns a same specifier to the parent of the cache, it reuses the cache */
    class SpecifierManagerDefault implements SpecifierManager {
        protected Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier;
        protected GuiReprValue.ObjectSpecifier specifierCache;

        public SpecifierManagerDefault(Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
            this.parentSpecifier = parentSpecifier;
        }

        public GuiReprValue.ObjectSpecifier get() {
            GuiReprValue.ObjectSpecifier parentSpec = null;
            if (parentSpecifier != null) {
                parentSpec = parentSpecifier.get();
                if (specifierCache != null && specifierCache.getParent().equals(parentSpec)) {
                    return specifierCache;
                }
            }
            specifierCache = new GuiReprValue.ObjectSpecifier(parentSpec, false);
            return specifierCache;
        }

        @Override
        public String toString() {
            return String.format("<%x>", System.identityHashCode(this));
        }
    }
}
