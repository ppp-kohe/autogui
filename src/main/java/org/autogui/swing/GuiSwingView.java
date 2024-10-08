package org.autogui.swing;

import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.*;
import org.autogui.swing.GuiSwingTaskRunner.ContextTaskResult;
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
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
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
    interface ValuePane<ValueType> {
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
         * notifies property update to the component.
         * getter will be called and GUI display will be updated.
         *  the method just calls {@link GuiMappingContext#updateSourceSubTree()}.
         * @since 1.1
         */
        default void updateSwingViewSource() {
            getSwingViewContext().updateSourceSubTree();
        }

        /**
         * notifies property update to the component and enclosing other components.
         * For single case, {@link #updateSwingViewSource()} is sufficient.
         * If the component is part of tree of value-panes, the method will be effective.
         *  the method just calls {@link GuiMappingContext#updateSourceFromRoot()}.
         * @since 1.1
         */
        default void updateSwingViewSourceFromRoot() {
            getSwingViewContext().updateSourceFromRoot();
        }

        /**
         * the method is used for table cell-editors, in order to observe the completion of editing.
         * the default impl. is empty.
         * @param eventHandler the handler called when the editing is finished
         */
        default void addSwingEditFinishHandler(Runnable eventHandler) { }

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

        GuiMappingContext getSwingViewContext();

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
            savePreferencesDefault(asSwingViewComponent(), prefs);
        }

        /**
         * {@link #loadSwingPreferences(GuiPreferences, GuiSwingPrefsApplyOptions)}
         *  with {@link GuiSwingPrefsApplyOptions#APPLY_OPTIONS_DEFAULT}.
         *  Do not override the method, instead, implements the same method with options version
         * @param prefs target prefs
         */
        default void loadSwingPreferences(GuiPreferences prefs) {
            loadSwingPreferences(prefs, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
        }

        /**
         * a sub-class which wraps another value-pane should overrides the method
         *   in order to omit needless value setting.
         *   it can use {@link #loadPreferencesDefault(JComponent, GuiPreferences)}
         * @param prefs target prefs or ancestor of the target;
         *              actual target can be obtained by {@link GuiPreferences#getDescendant(GuiMappingContext)}
         * @param options options for applying
         * @since 1.4
         */
        default void loadSwingPreferences(GuiPreferences prefs, GuiSwingPrefsApplyOptions options) {
            loadPreferencesDefault(asSwingViewComponent(), prefs, options);
        }

        /**
         * for each component, at the end of the process, this method will be called
         */
        default void shutdownSwingView() { }

        default boolean isSwingEditable() {
            GuiMappingContext context = getSwingViewContext();
            return !context.isReprValue() || context.getReprValue().isEditable(context);
        }

        /**
         * @return creates a key-stroke from it's context. for wrapping panes, returns null
         */
        default KeyStroke getSwingFocusKeyStroke() {
            return GuiSwingKeyBinding.getKeyStroke(getSwingViewContext().getAcceleratorKeyStroke());
        }

        /**
         * @param context the searched context, a descendant child of the context of the pane
         * @return a descendant value pane holding the context, or null.
         *     wrappers holding the same context are avoided.
         */
        default ValuePane<Object> getDescendantByContext(GuiMappingContext context) {
            return GuiSwingView.findChild(asSwingViewComponent(), p ->
                    Objects.equals(context, p.getSwingViewContext()));
        }

        /**
         * @param value the searched value
         * @return a descendant value pane holding the value, or null.
         *    wrappers holding the same context are avoided.
         */
        default ValuePane<Object> getDescendantByValue(Object value) {
            return GuiSwingView.findChild(asSwingViewComponent(),
                    p -> Objects.equals(p.getSwingViewValue(), value));
        }

        /**
         * @param valuePredicate the condition holds the searched value
         * @return a first descendant value pane holding a value matched by the predicate, or null.
         *    wrappers holding the same context are avoided.
         */
        default ValuePane<Object> getDescendantByValueIf(Predicate<Object> valuePredicate) {
            return GuiSwingView.findChild(asSwingViewComponent(),
                    p -> valuePredicate.test(p.getSwingViewValue()));
        }

        /**
         * @param name the searched context name
         * @return a child (or descendant for wrappers) value pane holding the named context, or null.
         *    wrappers holding the same context are avoided.
         */
        default ValuePane<Object> getChildByName(String name) {
            return GuiSwingView.getChild(asSwingViewComponent(),
                    p -> p.getSwingViewContext() != null &&
                            Objects.equals(p.getSwingViewContext().getName(), name));
        }

        /**
         * @param name the context name of the action
         * @return an action defined in the context of the pane. as the default implementation,
         *    it searches {@link #getSwingStaticMenuItems()}
         */
        default GuiSwingActionDefault.ExecutionAction getActionByName(String name) {
            return getActionDefault(this,
                    e ->  e.getContext() != null && e.getContext().getName().equals(name));
        }

        default GuiSwingActionDefault.ExecutionAction getActionByContext(GuiMappingContext context) {
            return getActionDefault(this, e ->  Objects.equals(e.getContext(), context));
        }

        default GuiSwingActionDefault.ExecutionAction getDescendantActionByContext(GuiMappingContext context) {
            return findNonNullByFunction(asSwingViewComponent(), p -> getActionByContext(context));
        }

        /**
         * @return the specifier for the context value, typically obtained from {@link GuiSwingView.SpecifierManager}
         */
        GuiReprValue.ObjectSpecifier getSpecifier();

        default void requestSwingViewFocus() {
            asSwingViewComponent().requestFocusInWindow();
        }

        /**
         * set the key-stroke info. to the pane as a guide
         * @param keyStrokeString the key stroke info to be set.
         */
        default void setKeyStrokeString(String keyStrokeString) { }

        /**
         * called from a value history menu. the default impl. just calls {@link #setSwingViewValueWithUpdate(Object)}.
         * @param value the new value. usually a ValueType.
         */
        @SuppressWarnings("unchecked")
        default void setSwingViewHistoryValue(Object value) {
            setSwingViewValueWithUpdate((ValueType) value);
        }

        /**
         * for each component, the method is called before refreshing in order to clear the view-clock.
         */
        void prepareForRefresh();

        default boolean isSwingCurrentValueSupported() {
            return getSwingViewContext().isHistoryValueSupported();
        }

        /**
         *
         * @param task the task
         * @param afterTask the task executed for deferred value
         * @param <RetType> the value type
         * @return {@link GuiSwingTaskRunner#executeContextTask(Supplier, Consumer)}
         */
        default <RetType> ContextTaskResult<RetType> executeContextTask(Supplier<RetType> task,
                                                                        Consumer<ContextTaskResult<RetType>> afterTask) {
            return new GuiSwingTaskRunner(getSwingViewContext()).executeContextTask(task, afterTask);
        }

        /**
         * prepare, clear and update via the context
         * @since 1.5
         */
        default void refreshByContext() {
            prepareForRefresh();
            GuiSwingActionDefault.ActionPreparation.prepareAction(asSwingViewComponent());
            GuiMappingContext context = getSwingViewContext();
            context.clearSourceSubTree();
            context.updateSourceSubTree();
        }

        /**
         * @return prefs JSON
         * @since 1.5
         */
        default Object getPrefsJsonSupported() {
            Object o = getSwingViewValue();
            if (o instanceof GuiPreferences.PreferencesJsonSupport) {
                return ((GuiPreferences.PreferencesJsonSupport) o).getPrefsJson();
            } else {
                return null;
            }
        }

        /**
         * @param json prefs JSON
         * @since 1.5
         */
        @SuppressWarnings("unchecked")
        default void setPrefsJsonSupported(Object json) {
            Object o = getSwingViewValue();
            if (o instanceof GuiPreferences.PreferencesJsonSupport && json instanceof Map<?,?>) {
                ((GuiPreferences.PreferencesJsonSupport) o).setPrefsJson(
                        (Map<String,Object>) json);
            }
        }
    }


    ///////////////////////////////

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
    interface SettingsWindowClient {
        /**
         * at initialization of the root-pane, the method will be called for existing sub-panes.
         *   a dynamically created view needs to be manually set by those panes.
         * @param settingWindow a settings-window from the root-pane, which will be disposed at closing of the main window
         */
        void setSettingsWindow(SettingsWindow settingWindow);
        SettingsWindow getSettingsWindow();
    }

    /**
     * recursively traverses components and if a component is an instance of the type, call f.
     * @param type type for a component
     * @param comp the searched component
     * @param f a process for each found component
     * @param <T> any type a component might belong to
     */
    static <T> void forEach(Class<T> type, Component comp, Consumer<T> f) {
        if (type.isInstance(comp)) {
            f.accept(type.cast(comp));
        }
        if (comp instanceof Container) {
            for (Component sub : ((Container) comp).getComponents()) {
                forEach(type, sub, f);
            }
        }
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
     * traverse descendant components and call {@link GuiSwingView.ValuePane#saveSwingPreferences(GuiPreferences)} for each component.
     * the root pane calls the method.
     * @param prefs the preferences for the comp
     * @param comp the top component
     */
    static void saveChildren(GuiPreferences prefs, JComponent comp) {
        forEach(ValuePane.class, comp, e -> {
            GuiSwingView.ValuePane<?> valuePane = (GuiSwingView.ValuePane<?>) e;
            if (valuePane != comp) { //skip top
                var subPrefs = prefs.getDescendant(valuePane.getSwingViewContext());
                try {
                    valuePane.saveSwingPreferences(
                            subPrefs);
                } catch (IllegalArgumentException iaex) {
                    if (iaex.getMessage().contains("Value too long")) {
                        GuiLogManager.get().logFormat("saveChildren(%s): Value too long", subPrefs.getName());
                    } else {
                        GuiLogManager.get().logError(iaex);
                    }
                } catch (Exception ex) {
                    GuiLogManager.get().logError(ex);
                }
            }
        });
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
     * {@link #loadChildren(GuiPreferences, JComponent, GuiSwingPrefsApplyOptions)}
     *  with {@link GuiSwingPrefsApplyOptions#APPLY_OPTIONS_DEFAULT}
     * @param prefs a top prefs
     * @param comp a top component
     */
    static void loadChildren(GuiPreferences prefs, JComponent comp) {
        loadChildren(prefs, comp, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
    }

    /**
     * traverses descendant components and call {@link GuiSwingView.ValuePane#loadSwingPreferences(GuiPreferences, GuiSwingPrefsApplyOptions)}
     * @param prefs a top prefs
     * @param comp  a top component
     * @param options  options for applying
     * @since 1.4
     */
    static void loadChildren(GuiPreferences prefs, JComponent comp, GuiSwingPrefsApplyOptions options) {
        forEach(ValuePane.class, comp, c -> {
            if (c != comp) { //skip top
                GuiSwingView.ValuePane<?> valuePane = (GuiSwingView.ValuePane<?>) c;
                try {
                    valuePane.loadSwingPreferences(
                            prefs.getDescendant(valuePane.getSwingViewContext()), options);
                } catch (Exception ex) {
                    GuiLogManager.get().logError(ex);
                }
            }
        });
    }

    static <PaneType extends ValuePane<?>> PaneType findChildByType(Component component, Class<PaneType> paneType) {
        return paneType.cast(findChild(component, paneType::isInstance));
    }

    static ValuePane<Object> findChild(Component component, Predicate<ValuePane<Object>> predicate) {
        return findNonNullByFunction(component, p -> predicate.test(p) ? p : null);
    }

    static List<ValuePane<Object>> collectChildren(Component component, Predicate<ValuePane<Object>> predicate) {
        return collectNonNullByFunction(component, p -> predicate.test(p) ? p : null);
    }

    static GuiSwingView.ValuePane<Object> getChild(Component component, Predicate<ValuePane<Object>> predicate) {
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                GuiSwingView.ValuePane<Object> r = checkNonNull(child, p -> predicate.test(p) ? p : null);
                if (r != null) {
                    return r;
                }
                //descendant for non value pane
                if (!(child instanceof ValuePane<?>)) {
                    r = getChild(child, predicate);
                    if (r != null) {
                        return r;
                    }
                }
            }
        }
        //descendant for wrapper
        if (component instanceof GuiSwingViewWrapper.ValuePaneWrapper<?> wrapper) {
            if (wrapper.isSwingViewWrappedPaneSameContext()) {
                return getChild(wrapper.getSwingViewWrappedPaneAsTypeOrNull(JComponent.class), predicate);
            }
        }
        return null;
    }

    static <T> T findNonNullByFunction(Component component, Function<ValuePane<Object>, T> f) {
        T r = checkNonNull(component, f);
        if (r != null) {
            return r;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                T t = findNonNullByFunction(child, f);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    static <T> List<T> collectNonNullByFunction(Component component, Function<ValuePane<Object>, T> f) {
        T r = checkNonNull(component, f);
        if (r != null) {
            return Collections.singletonList(r);
        }
        if (component instanceof Container) {
            return Arrays.stream(((Container) component).getComponents())
                    .flatMap(c -> collectNonNullByFunction(c, f).stream())
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T checkNonNull(Component component, Function<ValuePane<Object>,T> f) {
        if (component instanceof ValuePane<?>) {
            ValuePane<Object> vp = (ValuePane<Object>) component;
            T t = f.apply(vp);
            if (t != null) {
                if (component instanceof GuiSwingViewWrapper.ValuePaneWrapper<?> &&
                        ((GuiSwingViewWrapper.ValuePaneWrapper<Object>) component).isSwingViewWrappedPaneSameContext()) {
                    GuiSwingView.ValuePane<Object> wrapped = ((GuiSwingViewWrapper.ValuePaneWrapper<Object>) component).getSwingViewWrappedPane();
                    if (wrapped == null) {
                        return t;
                    } else {
                        T wrappedResult = checkNonNull(wrapped.asSwingViewComponent(), f);
                        //not matched with the wrapped pane, so return the wrapper's one
                        return Objects.requireNonNullElse(wrappedResult, t);
                    }
                } else {
                    return t;  //different context
                }
            }
        }
        return null;
    }

    static GuiSwingActionDefault.ExecutionAction getActionDefault(ValuePane<?> p, Predicate<GuiSwingActionDefault.ExecutionAction> predicate) {
        return p.getSwingStaticMenuItems().stream()
                .filter(GuiSwingActionDefault.ExecutionAction.class::isInstance)
                .map(GuiSwingActionDefault.ExecutionAction.class::cast)
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    /**
     * a refresh-action calls the method in order to clear view-clocks of sub-panes
     * @param pane a root component
     */
    @SuppressWarnings("rawtypes")
    static void prepareForRefresh(ValuePane<?> pane) {
        forEach(ValuePane.class,
                pane.asSwingViewComponent(),
                ValuePane::prepareForRefresh);
    }


    /////////////////////////

    static <T extends Component> T findComponent(Component component, Class<T> type, Predicate<T> predicate) {
        if (type.isInstance(component) && predicate.test(type.cast(component))) {
            return type.cast(component);
        }
        if (component instanceof Container) {
            for (Component sub : ((Container) component).getComponents()) {
                T t = findComponent(sub, type, predicate);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    static <T extends Component> List<T> collectComponents(Component component, Class<T> type, Predicate<T> predicate) {
        List<T> ts = new ArrayList<>();
        if (type.isInstance(component) && predicate.test(type.cast(component))) {
            ts.add(type.cast(component));
        }
        if (component instanceof Container) {
            for (Component sub : ((Container) component).getComponents()) {
                ts.addAll(collectComponents(sub, type, predicate));
            }
        }
        return ts;
    }


    /////////////////////////

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
     *  first it calls {@link GuiSwingView#prepareForRefresh(GuiSwingView.ValuePane)}
     *      which traverses the target pane and calls {@link GuiSwingView.ValuePane#prepareForRefresh(GuiSwingView.ValuePane)} for each component.
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


    ///////////////////////////

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

    /////////////////////////////////

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
