package autogui.swing;

import autogui.base.log.GuiLogManager;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.table.TableTargetColumnAction;
import autogui.swing.table.TableTargetMenu;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

        /** update the GUI display and the model. processed under the event thread
         * @param value the new value
         * */
        void setSwingViewValueWithUpdate(ValueType value);

        default void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) { }

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

        default ValueScrollPane<ValueType> wrapSwingScrollPane(boolean verticalAlways, boolean horizontalAlways) {
            return new ValueScrollPane<>(asSwingViewComponent(),
                    verticalAlways ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    horizontalAlways ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }

        default ValueWrappingPane<ValueType> wrapSwingPane() {
            return new ValueWrappingPane<>(asSwingViewComponent());
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
         * @param prefs target prefs or ancestor of the target;
         *              actual target can be obtained by {@link GuiPreferences#getDescendant(GuiMappingContext)}
         */
        default void saveSwingPreferences(GuiPreferences prefs) {
            savePreferencesDefault(asSwingViewComponent(), prefs);
        }

        /**
         * a sub-class which wraps another value-pane should overrides the method
         *   in order to omit needless value setting
         * @param prefs target prefs or ancestor of the target;
         *              actual target can be obtained by {@link GuiPreferences#getDescendant(GuiMappingContext)}
         */
        default void loadSwingPreferences(GuiPreferences prefs) {
            loadPreferencesDefault(asSwingViewComponent(), prefs);
        }

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
    }

    /**
     * indicating the pane wraps another {@link ValuePane}
     * @param <ValueType> the value type
     */
    interface ValuePaneWrapper<ValueType> extends ValuePane<ValueType> {
        /**
         * @return a wrapped child pane or null.
         *   the returned pane might have the same context of this, or different one like "property(value)"
         */
        ValuePane<Object> getSwingViewWrappedPane();

        default boolean isSwingViewWrappedPaneSameContext() {
            ValuePane<Object> p = getSwingViewWrappedPane();
            return p != null && Objects.equals(p.getSwingViewContext(), getSwingViewContext());
        }

        @Override
        default GuiSwingActionDefault.ExecutionAction getActionByName(String name) {
            if (isSwingViewWrappedPaneSameContext()) {
                GuiSwingActionDefault.ExecutionAction a = getSwingViewWrappedPane().getActionByName(name);
                if (a != null) {
                    return a;
                }
            }
            return getActionDefault(this,
                    e ->  e.getContext() != null && e.getContext().getName().equals(name));
        }

        @Override
        default GuiSwingActionDefault.ExecutionAction getActionByContext(GuiMappingContext context) {
            if (isSwingViewWrappedPaneSameContext()) {
                GuiSwingActionDefault.ExecutionAction a = getSwingViewWrappedPane().getActionByContext(context);
                if (a != null) {
                    return a;
                }
            }
            return getActionDefault(this, e ->  Objects.equals(e.getContext(), context));
        }
    }


    ///////////////////////////////

    static void setDescriptionToolTipText(GuiMappingContext context, JComponent comp) {
        String d = context.getDescription();
        if (!d.isEmpty()) {
            comp.setToolTipText(d);
        }
    }

    interface SettingsWindowClient {
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
        if (!pane.isSwingEditable()) {
            return;
        }
        List<GuiPreferences.HistoryValueEntry> es = new ArrayList<>(prefs.getHistoryValues());
        es.sort(Comparator.comparing(GuiPreferences.HistoryValueEntry::getTime));
        if (!es.isEmpty()) {
            Object value = es.get(es.size() - 1).getValue();
            pane.setSwingViewValueWithUpdate(value);
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
            prefs.getDescendant(((ValuePane<?>) pane).getSwingViewContext());
        }
    }

    @SuppressWarnings("unchecked")
    static void loadPreferencesDefault(JComponent pane, GuiPreferences prefs) {
        try {
            if (pane instanceof ValuePane<?>) {
                GuiMappingContext context = ((ValuePane) pane).getSwingViewContext();
                GuiPreferences targetPrefs = prefs.getDescendant(context);
                if (context.isHistoryValueSupported()) {
                    setLastHistoryValue(targetPrefs, (ValuePane<Object>) pane);
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
        if (component instanceof ValuePaneWrapper<?>) {
            ValuePaneWrapper<?> wrapper = (ValuePaneWrapper<?>) component;
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

    @SuppressWarnings("unchecked")
    static <T> T checkNonNull(Component component, Function<ValuePane<Object>,T> f) {
        if (component instanceof ValuePane<?>) {
            ValuePane<Object> vp = (ValuePane<Object>) component;
            T t = f.apply(vp);
            if (t != null) {
                if (component instanceof GuiSwingView.ValuePaneWrapper<?> &&
                        ((GuiSwingView.ValuePaneWrapper<Object>) component).isSwingViewWrappedPaneSameContext()) {
                    GuiSwingView.ValuePane<Object> wrapped = ((GuiSwingView.ValuePaneWrapper<Object>) component).getSwingViewWrappedPane();
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

    class ValueScrollPane<ValueType> extends JScrollPane implements ValuePaneWrapper<ValueType> {
        protected ValuePane<ValueType> pane;

        @SuppressWarnings("unchecked")
        public ValueScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
            super(view, vsbPolicy, hsbPolicy);
            if (view instanceof ValuePane) {
                this.pane = (ValuePane<ValueType>) view;
            }
        }

        @SuppressWarnings("unchecked")
        public ValueScrollPane(Component view) {
            super(view);
            if (view instanceof ValuePane) {
                this.pane = (ValuePane<ValueType>) view;
            }
        }

        @Override
        public ValueType getSwingViewValue() {
            return pane == null ? null : pane.getSwingViewValue();
        }

        @Override
        public void setSwingViewValue(ValueType value) {
            if (pane != null) {
                pane.setSwingViewValue(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(ValueType value) {
            if (pane != null) {
                pane.setSwingViewValueWithUpdate(value);
            }
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            return Collections.emptyList();
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return pane == null ? null : pane.getSwingMenuBuilder();
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            if (pane != null) {
                pane.addSwingEditFinishHandler(eventHandler);
            }
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return pane == null ? null : pane.getSwingViewContext();
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs) {
        }

        @SuppressWarnings("unchecked")
        @Override
        public ValuePane<Object> getSwingViewWrappedPane() {
            return (ValuePane<Object>) pane;
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return pane == null ? GuiReprValue.NONE : pane.getSpecifier();
        }
    }


    class ValueWrappingPane<ValueType> extends JPanel implements ValuePaneWrapper<ValueType> {
        protected ValuePane<ValueType> pane;

        public ValueWrappingPane(Component view) {
            super(new BorderLayout());
            setOpaque(false);
            add(view, BorderLayout.CENTER);
        }

        @SuppressWarnings("unchecked")
        private void setPane(Component view) {
            if (view instanceof ValuePane) {
                this.pane = (ValuePane<ValueType>) view;
            }
        }

        public ValueWrappingPane(LayoutManager m) {
            super(m);
        }

        @Override
        protected void addImpl(Component comp, Object constraints, int index) {
            setPane(comp);
            super.addImpl(comp, constraints, index);
        }

        @Override
        public ValueType getSwingViewValue() {
            return pane == null ? null : pane.getSwingViewValue();
        }

        @Override
        public void setSwingViewValue(ValueType value) {
            if (pane != null) {
                pane.setSwingViewValue(value);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(ValueType value) {
            if (pane != null) {
                pane.setSwingViewValueWithUpdate(value);
            }
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            return Collections.emptyList();
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return pane == null ? null : pane.getSwingMenuBuilder();
        }

        @Override
        public void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
            if (pane != null) {
                pane.addSwingEditFinishHandler(eventHandler);
            }
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return pane == null ? null : pane.getSwingViewContext();
        }

        @Override
        public void loadSwingPreferences(GuiPreferences prefs) {
        }

        @SuppressWarnings("unchecked")
        @Override
        public ValuePane<Object> getSwingViewWrappedPane() {
            return (ValuePane<Object>) pane;
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return pane == null ? GuiReprValue.NONE : pane.getSpecifier();
        }
    }

    class ContextRefreshAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected GuiMappingContext context;

        public ContextRefreshAction(GuiMappingContext context) {
            putValue(NAME, "Refresh");
            this.context = context;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
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

    class ToStringCopyAction extends AbstractAction implements TableTargetColumnAction {
        protected ValuePane<?> pane;
        protected GuiMappingContext context;

        public ToStringCopyAction(ValuePane<?> pane, GuiMappingContext context) {
            putValue(NAME, "Copy As String");
            this.pane = pane;
            this.context = context;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            copy(toString(pane.getSwingViewValue()));
        }

        public String toString(Object v) {
            return context.getRepresentation().toHumanReadableString(context, v);
        }


        public void copy(String data) {
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection sel = new StringSelection(data);
            board.setContents(sel, sel);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            copy(target.getSelectedCellValues().stream()
                    .map(this::toString)
                    .collect(Collectors.joining("\n")));
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

    class HistoryMenu<ValueType, PaneType extends ValuePane<ValueType>> extends JMenu implements TableTargetMenu,
            PopupCategorized.CategorizedMenuItemComponent {
        protected PaneType component;
        protected GuiMappingContext context;

        public HistoryMenu(PaneType component, GuiMappingContext context) {
            super("History");
            this.component = component;
            this.context = context;
            buildMenu();
        }

        protected void buildMenu() {
            addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {
                    loadItems();
                }
                @Override
                public void menuDeselected(MenuEvent e) { }
                @Override
                public void menuCanceled(MenuEvent e) { }
            });
        }

        @Override
        public JComponent getMenuItem() {
            return this;
        }

        @Override
        public JMenu convert(GuiReprCollectionTable.TableTargetColumn target) {
            return new HistoryMenuForTableColumn<>(component, context, target);
        }

        public void clearHistory() {
            context.getPreferences().clearHistories();
        }

        public void loadItems() {
            removeAll();
            boolean added = false;
            List<GuiPreferences.HistoryValueEntry> prefEs = context.getPreferences().getHistoryValues();
            List<GuiPreferences.HistoryValueEntry> es = new ArrayList<>(prefEs);
            Collections.reverse(es);
            for (GuiPreferences.HistoryValueEntry e : es) {
                if (e.getIndex() != -1 && e.getValue() != null) {
                    addAction(e);
                    added = true;
                }
            }
            if (!added) {
                JMenuItem nothing = new JMenuItem("Nothing");
                nothing.setEnabled(false);
                add(nothing);
            }
            HistoryClearAction clearAction = new HistoryClearAction(this);
            if (!added) {
                clearAction.setEnabled(false);
            }
            addSeparator();
            add(clearAction);
        }

        static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        public void addAction(GuiPreferences.HistoryValueEntry e) {
            JMenuItem item = new JMenuItem(createAction(e));
            item.setText(item.getText() + " : " + formatter.format(LocalDateTime.ofInstant(e.getTime(), ZoneId.systemDefault())));
            add(item);
        }

        @SuppressWarnings("unchecked")
        public Action createAction(GuiPreferences.HistoryValueEntry e) {
            ValueType v = (ValueType) e.getValue();
            return new HistorySetAction<>(getActionName(e), v, component);
        }

        public String getActionName(GuiPreferences.HistoryValueEntry e) {
            String name = context.getRepresentation().toHumanReadableString(context, e.getValue());
            if (name.length() > 30) {
                name = name.substring(0, 30) + "...";
            }
            return name;
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_SET;
        }

    }

    class HistoryMenuForTableColumn<ValueType, PaneType extends ValuePane<ValueType>> extends HistoryMenu<ValueType, PaneType> {
        protected GuiReprCollectionTable.TableTargetColumn target;
        public HistoryMenuForTableColumn(PaneType component, GuiMappingContext context, GuiReprCollectionTable.TableTargetColumn target) {
            super(component, context);
            this.target = target;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Action createAction(GuiPreferences.HistoryValueEntry e) {
            return new HistorySetForColumnAction<>(getActionName(e), (ValueType) e.getValue(), target);
        }

        @Override
        public String getCategory() {
            return MenuBuilder.getCategoryWithPrefix(MENU_COLUMN_ROWS, PopupExtension.MENU_CATEGORY_SET);
        }
    }

    class HistorySetAction<ValueType> extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected ValueType value;
        protected ValuePane<ValueType> component;

        public HistorySetAction(String name, ValueType value, ValuePane<ValueType> component) {
            super(name);
            this.value = value;
            this.component = component;
        }

        @Override
        public boolean isEnabled() {
            return component.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            component.setSwingViewValueWithUpdate(value);
        }
    }

    class HistorySetForColumnAction<ValueType> extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected ValueType value;
        protected GuiReprCollectionTable.TableTargetColumn target;

        public HistorySetForColumnAction(String name, ValueType value, GuiReprCollectionTable.TableTargetColumn target) {
            super(name);
            this.value = value;
            this.target = target;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            target.setCellValues(target.getSelectedCellIndexesStream(), i -> value);
        }
    }

    class HistoryClearAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        protected HistoryMenu menu;

        public HistoryClearAction(HistoryMenu menu) {
            putValue(NAME, "Clear");
            this.menu = menu;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            menu.clearHistory();
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_DELETE;
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
    }


}
