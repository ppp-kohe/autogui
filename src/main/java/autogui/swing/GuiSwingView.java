package autogui.swing;

import autogui.base.log.GuiLogManager;
import autogui.base.mapping.*;
import autogui.swing.table.TableTargetColumnAction;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface GuiSwingView extends GuiSwingElement {
    JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier);

    default boolean isComponentResizable(GuiMappingContext context) {
        return false;
    }

    interface ValuePane<ValueType> {
        /** @return basically, the source value of the context, held by the component without touching the context.  */
        ValueType getSwingViewValue();

        /** update GUI display,
         *   and it does NOT update the target model value.
         * processed under the event thread
         * @param value the new value
         * */
        void setSwingViewValue(ValueType value);

        /**
         *  update if the clock is newer than the current clock of the pane.
         *  Note: instead, {@link #setSwingViewValue(Object)} increments the current clock
         * @param value the new value
         * @param clock the clock of the value: if the clock is newer than the current clock of the pane, it can update
         */
        void setSwingViewValue(ValueType value, GuiTaskClock clock);

        /** update the GUI display and the model. processed under the event thread
         * @param value the new value
         * */
        void setSwingViewValueWithUpdate(ValueType value);

        void setSwingViewValueWithUpdate(ValueType value, GuiTaskClock clock);

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

        default GuiSwingViewWrapper.ValueScrollPane<ValueType> wrapSwingScrollPane(boolean verticalAlways, boolean horizontalAlways) {
            return new GuiSwingViewWrapper.ValueScrollPane<>(asSwingViewComponent(),
                    verticalAlways ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    horizontalAlways ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }

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
         * a sub-class which wraps another value-pane should overrides the method
         *   in order to omit needless value setting.
         *   it can use {@link #loadPreferencesDefault(JComponent, GuiPreferences)}
         * @param prefs target prefs or ancestor of the target;
         *              actual target can be obtained by {@link GuiPreferences#getDescendant(GuiMappingContext)}
         */
        default void loadSwingPreferences(GuiPreferences prefs) {
            loadPreferencesDefault(asSwingViewComponent(), prefs);
        }

        /**
         * for each component, at the end of the process, this method will be called
         */
        default void shutdownSwingView() { }

        default boolean isSwingEditable() {
            GuiMappingContext context = getSwingViewContext();
            return !context.isReprValue() || context.getReprValue().isEditable(context);
        }

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

        GuiReprValue.ObjectSpecifier getSpecifier();

        default void requestSwingViewFocus() {
            asSwingViewComponent().requestFocusInWindow();
        }

        default void setKeyStrokeString(String keyStrokeString) { }

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

        //TODO execute impl
        default <RetType> RetType execute(Supplier<RetType> task, RetType timeOutValue, RetType cancelValue, Consumer<RetType> afterTask) {
            RetType ret;
            try {
                ret = task.get();
//            } catch (InterruptedException ie) {
//                ret = timeoutValue;
            } catch (Throwable ex) {
                ret = cancelValue;
            }
            if (afterTask != null) {
                afterTask.accept(ret);
            }
            return ret;
        }
    }


    ///////////////////////////////

    static void updateFromGui(ValuePane<?> pane, Object value, GuiTaskClock viewClock) {
        GuiMappingContext context = pane.getSwingViewContext();
        GuiReprValue repr = context.getReprValue();
        if (repr.isEditable(context)) {
            GuiReprValue.ObjectSpecifier spec = pane.getSpecifier();
            GuiTaskClock clock = viewClock.copy();

            pane.execute(() -> {
                repr.updateFromGui(context, value, spec, clock); //callbacks are done by invokeLater
                return null;
            }, null, null, null);
        }
    }

    static void setDescriptionToolTipText(GuiMappingContext context, JComponent comp) {
        String d = context.getDescription();
        if (!d.isEmpty()) {
            comp.setToolTipText(d);
        }
    }

    interface SettingsWindowClient {
        /**
         * at initialization of the root-pane, the method will be called for existing sub-panes.
         *   a dynamically created view needs to be manually set by those panes.
         * @param settingWindow a settings-window from the root-pane, which will be disposed at closing of the main window
         */
        void setSettingsWindow(SettingsWindow settingWindow);
        SettingsWindow getSettingsWindow();
    }

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

    static void setLastHistoryValue(GuiPreferences prefs, ValuePane<Object> pane) {
        if (!pane.isSwingEditable() || !pane.isSwingCurrentValueSupported()) {
            return;
        }

        Object v = prefs.getCurrentValue();
        if (v != null) {
            pane.setSwingViewHistoryValue(v);
        } else if (pane.getSwingViewContext().isHistoryValueStored(null)) {
            List<GuiPreferences.HistoryValueEntry> es = new ArrayList<>(prefs.getHistoryValues());
            es.sort(Comparator.comparing(GuiPreferences.HistoryValueEntry::getTime));
            if (!es.isEmpty()) {
                Object value = es.get(es.size() - 1).getValue();
                pane.setSwingViewHistoryValue(value);
            }
        }
    }

    static void saveChildren(GuiPreferences prefs, JComponent comp) {
        forEach(ValuePane.class, comp, e -> {
            GuiSwingView.ValuePane<?> valuePane = (GuiSwingView.ValuePane<?>) e;
            if (valuePane != comp) { //skip top
                try {
                    valuePane.saveSwingPreferences(
                            prefs.getDescendant(valuePane.getSwingViewContext()));
                } catch (Exception ex) {
                    GuiLogManager.get().logError(ex);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    static void savePreferencesDefault(JComponent pane, GuiPreferences prefs) {
        if (pane instanceof ValuePane<?>) {
            GuiMappingContext context = ((ValuePane<?>) pane).getSwingViewContext();
            GuiPreferences targetPrefs = prefs.getDescendant(context);
            if (((ValuePane<?>) pane).isSwingCurrentValueSupported()) {
                targetPrefs.setCurrentValue(((ValuePane<?>) pane).getSwingViewValue());
            }
            if (context.isHistoryValueSupported()) {
                GuiPreferences ctxPrefs = context.getPreferences();
                if (!ctxPrefs.equals(targetPrefs)) {
                    context.getPreferences().getHistoryValues().stream()
                            .sorted(Comparator.comparing(GuiPreferences.HistoryValueEntry::getTime))
                            .forEachOrdered(e -> targetPrefs.addHistoryValue(e.getValue(), e.getTime()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    static void loadPreferencesDefault(JComponent pane, GuiPreferences prefs) {
        try {
            if (pane instanceof ValuePane<?>) {
                GuiMappingContext context = ((ValuePane) pane).getSwingViewContext();
                GuiPreferences targetPrefs = prefs.getDescendant(context);
                setLastHistoryValue(targetPrefs, (ValuePane<Object>) pane);

                if (context.isHistoryValueSupported()) {
                    GuiPreferences ctxPrefs = context.getPreferences();
                    if (!targetPrefs.equals(ctxPrefs)) {
                        targetPrefs.getHistoryValues().stream()
                                .sorted(Comparator.comparing(GuiPreferences.HistoryValueEntry::getTime))
                                .forEachOrdered(e -> ctxPrefs.addHistoryValue(e.getValue(), e.getTime()));
                    }
                }
            }
        } catch (Exception ex) {
            GuiLogManager.get().logError(ex);
        }
    }

    static void loadChildren(GuiPreferences prefs, JComponent comp) {
        forEach(ValuePane.class, comp, c -> {
            if (c != comp) { //skip top
                GuiSwingView.ValuePane<?> valuePane = (GuiSwingView.ValuePane<?>) c;
                try {
                    valuePane.loadSwingPreferences(
                            prefs.getDescendant(valuePane.getSwingViewContext()));
                } catch (Exception ex) {
                    GuiLogManager.get().logError(ex);
                }
            }
        });
    }

    static <PaneType extends ValuePane<?>> PaneType findChildByType(Component component, Class<PaneType> paneType) {
        return paneType.cast(findChild(component, paneType::isInstance));
    }

    @SuppressWarnings("unchecked")
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
        if (component instanceof GuiSwingViewWrapper.ValuePaneWrapper<?>) {
            GuiSwingViewWrapper.ValuePaneWrapper<?> wrapper = (GuiSwingViewWrapper.ValuePaneWrapper<?>) component;
            if (wrapper.isSwingViewWrappedPaneSameContext()) {
                return getChild(wrapper.getSwingViewWrappedPane().asSwingViewComponent(), predicate);
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
                    T wrappedResult = checkNonNull(wrapped.asSwingViewComponent(), f);
                    if (wrappedResult != null) {
                        return wrappedResult;
                    } else {
                        return t; //not matched with the wrapped pane, so return the wrapper's one
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
    static void prepareForRefresh(ValuePane<?> pane) {
        forEach(ValuePane.class, pane.asSwingViewComponent(), ValuePane::prepareForRefresh);
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
            if (ks != null && Arrays.stream(inputs.keys())
                    .anyMatch(s::equals)) { //actually defined in the inputs
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


    class ContextRefreshAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected GuiMappingContext context;
        protected ValuePane<?> pane;

        public ContextRefreshAction(GuiMappingContext context, ValuePane<?> pane) {
            putValue(NAME, "Refresh");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_DOWN_MASK));
            this.pane = pane;
            this.context = context;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (pane != null) {
                GuiSwingView.prepareForRefresh(pane);
            }
            context.clearSourceSubTree();
            context.updateSourceSubTree();
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

    class ToStringCopyAction extends ContextAction implements TableTargetColumnAction {
        protected ValuePane<?> pane;

        public ToStringCopyAction(ValuePane<?> pane, GuiMappingContext context) {
            super(context);
            putValue(NAME, "Copy as Text");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            this.pane = pane;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object v = pane.getSwingViewValue();
            execute(() -> toString(v), null, null,
                    this::copy);
        }

        public String toString(Object v) {
            return context.getRepresentation().toHumanReadableString(context, v);
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
            execute(() -> vs.stream()
                    .map(this::toString)
                    .collect(Collectors.joining("\n")), null, null,
                    this::copy);
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

    class ContextAction extends AbstractAction {
        protected GuiMappingContext context;

        public ContextAction(GuiMappingContext context) { //context may be null
            this.context = context;
        }

        public GuiMappingContext getContext() {
            return context;
        }
        //TODO execute impl
        public <RetType> RetType execute(Supplier<RetType> task, RetType timeOutValue, RetType cancelValue, Consumer<RetType> afterTask) {
            RetType ret = null;
            try {
                ret = task.get();
//            } catch (InterruptedException ie) {
//                ret = timeoutValue;
            } catch (Throwable ex) {
                ret = cancelValue;
            } finally {
                if (afterTask != null) {
                    afterTask.accept(ret);
                }
            }
            return ret;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }
    }

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
        component.setTransferHandler(handler);
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(component, DnDConstants.ACTION_COPY, e -> {
            component.getTransferHandler().exportAsDrag(component, e.getTriggerEvent(), TransferHandler.COPY);
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

        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), copy.getValue(Action.NAME));
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), paste.getValue(Action.NAME));
    }

    interface SpecifierManager {
        GuiReprValue.ObjectSpecifier getSpecifier();
    }

    class SpecifierManagerDefault implements SpecifierManager {
        protected Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier;
        protected GuiReprValue.ObjectSpecifier specifierCache;

        public SpecifierManagerDefault(Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
            this.parentSpecifier = parentSpecifier;
        }

        public GuiReprValue.ObjectSpecifier getSpecifier() {
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
