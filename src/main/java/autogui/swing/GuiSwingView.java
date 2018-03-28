package autogui.swing;

import autogui.base.log.GuiLogManager;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.table.TableTargetColumnAction;
import autogui.swing.table.TableTargetMenu;
import autogui.swing.util.MenuBuilder;
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
import java.util.stream.Collectors;

public interface GuiSwingView extends GuiSwingElement {
    JComponent createView(GuiMappingContext context);

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

        PopupExtension.PopupMenuBuilder getSwingMenuBuilder();

        default ValueScrollPane<ValueType> wrapScrollPane(boolean verticalAlways, boolean horizontalAlways) {
            return new ValueScrollPane<>(asComponent(),
                    verticalAlways ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    horizontalAlways ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }

        default ValueWrappingPane<ValueType> wrapPane() {
            return new ValueWrappingPane<>(asComponent());
        }

        GuiMappingContext getContext();

        default GuiSwingViewPropertyPane.PropertyPane wrapProperty() {
            return new GuiSwingViewPropertyPane.PropertyPane(getContext(), true, asComponent());
        }

        default GuiSwingViewPropertyPane.NamedPropertyPane wrapNamed() {
            return new GuiSwingViewPropertyPane.NamedPropertyPane(getContext().getDisplayName(), getContext().getName(),
                    asComponent(), getSwingMenuBuilder());
        }

        default JComponent asComponent() {
            return (JComponent) this;
        }

        /**
         * @param prefs target prefs or ancestor of the target;
         *              actual target can be obtained by {@link GuiPreferences#getDescendant(GuiMappingContext)}
         */
        default void savePreferences(GuiPreferences prefs) {
            savePreferencesDefault(asComponent(), prefs);
        }

        /**
         * a sub-class which wraps another value-pane should overrides the method
         *   in order to omit needless value setting
         * @param prefs target prefs or ancestor of the target;
         *              actual target can be obtained by {@link GuiPreferences#getDescendant(GuiMappingContext)}
         */
        default void loadPreferences(GuiPreferences prefs) {
            loadPreferencesDefault(asComponent(), prefs);
        }

        default void shutdown() { }

        default boolean isEditable() {
            GuiMappingContext context = getContext();
            return !context.isReprValue() || context.getReprValue().isEditable(context);
        }
    }

    ///////////////////////////////


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
        if (!pane.isEditable()) {
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
                    valuePane.savePreferences(
                            prefs.getDescendant(valuePane.getContext()));
                } catch (Exception ex) {
                    GuiLogManager.get().logError(ex);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    static void savePreferencesDefault(JComponent pane, GuiPreferences prefs) {
        if (pane instanceof ValuePane<?>) {
            prefs.getDescendant(((ValuePane<?>) pane).getContext());
        }
    }

    @SuppressWarnings("unchecked")
    static void loadPreferencesDefault(JComponent pane, GuiPreferences prefs) {
        if (pane instanceof ValuePane<?>) {
            GuiMappingContext context = ((ValuePane) pane).getContext();
            GuiPreferences targetPrefs = prefs.getDescendant(context);
            if (context.isHistoryValueSupported()) {
                setLastHistoryValue(targetPrefs, (ValuePane<Object>) pane);
            }
        }
    }

    static void loadChildren(GuiPreferences prefs, JComponent comp) {
        forEach(ValuePane.class, comp, c -> {
            if (c != comp) { //skip top
                GuiSwingView.ValuePane<?> valuePane = (GuiSwingView.ValuePane<?>) c;
                try {
                    valuePane.loadPreferences(
                            prefs.getDescendant(valuePane.getContext()));
                } catch (Exception ex) {
                    GuiLogManager.get().logError(ex);
                }
            }
        });
    }

    class ValueScrollPane<ValueType> extends JScrollPane implements ValuePane<ValueType> {
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
        public GuiMappingContext getContext() {
            return pane == null ? null : pane.getContext();
        }

        @Override
        public void loadPreferences(GuiPreferences prefs) {
        }
    }


    class ValueWrappingPane<ValueType> extends JPanel implements ValuePane<ValueType> {
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
        public GuiMappingContext getContext() {
            return pane == null ? null : pane.getContext();
        }

        @Override
        public void loadPreferences(GuiPreferences prefs) {
        }
    }

    class ContextRefreshAction extends AbstractAction {
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
    }

    class ContextRefreshRootAction extends AbstractAction {
        protected GuiMappingContext context;

        public ContextRefreshRootAction(GuiMappingContext context) {
            putValue(NAME, "Refresh All");
            this.context = context;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            context.updateSourceFromRoot();
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
    }

    ///////////////////////////

    class HistoryMenu<ValueType, PaneType extends ValuePane<ValueType>> extends JMenu implements TableTargetMenu {
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
    }

    class HistorySetAction<ValueType> extends AbstractAction {
        protected ValueType value;
        protected ValuePane<ValueType> component;

        public HistorySetAction(String name, ValueType value, ValuePane<ValueType> component) {
            super(name);
            this.value = value;
            this.component = component;
        }

        @Override
        public boolean isEnabled() {
            return component.isEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            component.setSwingViewValueWithUpdate(value);
        }
    }

    class HistorySetForColumnAction<ValueType> extends AbstractAction {
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

    class HistoryClearAction extends AbstractAction {
        protected HistoryMenu menu;

        public HistoryClearAction(HistoryMenu menu) {
            putValue(NAME, "Clear");
            this.menu = menu;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            menu.clearHistory();
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
}
