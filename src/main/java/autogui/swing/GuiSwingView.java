package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.swing.table.TableTargetColumn;
import autogui.swing.table.TableTargetColumnAction;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface GuiSwingView extends GuiSwingElement {
    JComponent createView(GuiMappingContext context);

    default boolean isComponentResizable(GuiMappingContext context) {
        return false;
    }


    interface ValuePane<ValueType> {
        ValueType getSwingViewValue();
        /** update GUI display,
         *   and it does NOT update the target model value.
         * processed under the event thread */
        void setSwingViewValue(ValueType value);

        /** update the GUI display and the model. processed under the event thread */
        void setSwingViewValueWithUpdate(ValueType value);

        default void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) { }

        PopupExtension.PopupMenuBuilder getSwingMenuBuilder();

        default ValueScrollPane wrapScrollPane(boolean verticalAlways, boolean horizontalAlways) {
            return new ValueScrollPane(asComponent(),
                    verticalAlways ? ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    horizontalAlways ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }

        default ValueWrappingPane wrapPane() {
            return new ValueWrappingPane(asComponent());
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
            saveChildren(prefs, asComponent());
        }

        /**
         * @param prefs target prefs or ancestor of the target;
         *              actual target can be obtained by {@link GuiPreferences#getDescendant(GuiMappingContext)}
         */
        default void loadPreferences(GuiPreferences prefs) {
            loadChildren(prefs, asComponent());
        }

    }

    static void saveChildren(GuiPreferences prefs, JComponent comp) {
        for (Component c : comp.getComponents()) {
            if (c instanceof GuiSwingView.ValuePane) {
                GuiSwingView.ValuePane valuePane = (GuiSwingView.ValuePane) c;
                valuePane.savePreferences(prefs);
            } else if (c instanceof JComponent) {
                saveChildren(prefs, (JComponent) c);
            }
        }
    }

    static void loadChildren(GuiPreferences prefs, JComponent comp) {
        for (Component c : comp.getComponents()) {
            if (c instanceof GuiSwingView.ValuePane) {
                GuiSwingView.ValuePane valuePane = (GuiSwingView.ValuePane) c;
                valuePane.loadPreferences(prefs);
            } else if (c instanceof JComponent) {
                loadChildren(prefs, (JComponent) c);
            }
        }
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
                this.pane = (ValuePane) view;
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
    }


    class ValueWrappingPane<ValueType> extends JPanel implements ValuePane<ValueType> {
        protected ValuePane<ValueType> pane;

        public ValueWrappingPane(Component view) {
            super(new BorderLayout());
            add(view, BorderLayout.CENTER);
        }

        @SuppressWarnings("unchecked")
        private void setPane(Component view) {
            if (view instanceof ValuePane) {
                this.pane = (ValuePane) view;
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
    }

    class ContextRefreshAction extends AbstractAction {
        protected GuiMappingContext context;

        public ContextRefreshAction(GuiMappingContext context) {
            putValue(NAME, "Refresh");
            this.context = context;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
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
        protected ValuePane pane;
        protected GuiMappingContext context;

        public ToStringCopyAction(ValuePane pane, GuiMappingContext context) {
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
        public void actionPerformedOnTableColumn(ActionEvent e, TableTargetColumn target) {
            copy(target.getSelectedCellValues().values().stream()
                    .map(this::toString)
                    .collect(Collectors.joining("\n")));
        }
    }

    ///////////////////////////

    class HistoryMenu<ValueType, PaneType extends ValuePane<ValueType>> extends JMenu {
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
                    add(createAction(e));
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

    class HistorySetAction<ValueType> extends AbstractAction {
        protected ValueType value;
        protected ValuePane<ValueType> component;

        public HistorySetAction(String name, ValueType value, ValuePane<ValueType> component) {
            super(name);
            this.value = value;
            this.component = component;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            component.setSwingViewValueWithUpdate(value);
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
}
