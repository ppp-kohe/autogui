package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.swing.table.TableTargetColumn;
import autogui.swing.table.TableTargetColumnAction;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.EventObject;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface GuiSwingView extends GuiSwingElement {
    JComponent createView(GuiMappingContext context);

    default boolean isComponentResizable(GuiMappingContext context) {
        return false;
    }


    interface ValuePane {
        Object getSwingViewValue();
        /** updates GUI display,
         *   and it does NOT update the target model value.
         * processed under the event thread */
        void setSwingViewValue(Object value);

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

        default void loadPreferences() {
            loadChildren(asComponent());
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

    static void loadChildren(JComponent comp) {
        for (Component c : comp.getComponents()) {
            if (c instanceof GuiSwingView.ValuePane) {
                GuiSwingView.ValuePane valuePane = (GuiSwingView.ValuePane) c;
                valuePane.loadPreferences();
            } else if (c instanceof JComponent) {
                loadChildren((JComponent) c);
            }
        }
    }

    class ValueScrollPane extends JScrollPane implements ValuePane {
        protected ValuePane pane;

        public ValueScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
            super(view, vsbPolicy, hsbPolicy);
            if (view instanceof ValuePane) {
                this.pane = (ValuePane) view;
            }
        }

        public ValueScrollPane(Component view) {
            super(view);
            if (view instanceof ValuePane) {
                this.pane = (ValuePane) view;
            }
        }

        @Override
        public Object getSwingViewValue() {
            return pane == null ? null : pane.getSwingViewValue();
        }

        @Override
        public void setSwingViewValue(Object value) {
            if (pane != null) {
                pane.setSwingViewValue(value);
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


    class ValueWrappingPane extends JPanel implements ValuePane {
        protected ValuePane pane;

        public ValueWrappingPane(Component view) {
            super(new BorderLayout());
            add(view, BorderLayout.CENTER);
        }

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
        public Object getSwingViewValue() {
            return pane == null ? null : pane.getSwingViewValue();
        }

        @Override
        public void setSwingViewValue(Object value) {
            if (pane != null) {
                pane.setSwingViewValue(value);
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
}
