package org.autogui.swing;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GuiSwingViewWrapper {
    /**
     * indicating the pane wraps another {@link GuiSwingView.ValuePane}
     * @param <ValueType> the value type
     */
    public interface ValuePaneWrapper<ValueType> extends GuiSwingView.ValuePane<ValueType> {
        /**
         * @return a wrapped child pane or null.
         *   the returned pane might have the same context of this, or different one like "property(value)"
         */
        GuiSwingView.ValuePane<Object> getSwingViewWrappedPane();

        default boolean isSwingViewWrappedPaneSameContext() {
            GuiSwingView.ValuePane<Object> p = getSwingViewWrappedPane();
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
            return GuiSwingView.getActionDefault(this,
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
            return GuiSwingView.getActionDefault(this, e ->  Objects.equals(e.getContext(), context));
        }

        @Override
        default KeyStroke getSwingFocusKeyStroke() {
            return null;
        }

        /**
         * nothing happen in the wrapper pane. the wrapped pane do the task
         * @param prefs target prefs or ancestor of the target;
         */
        @Override
        default void saveSwingPreferences(GuiPreferences prefs) { }

        /**
         * nothing happen in the wrapper pane. the wrapped pane do the task
         * @param prefs target prefs or ancestor of the target;
         */
        @Override
        default void loadSwingPreferences(GuiPreferences prefs) { }

    }

    public static class ValueScrollPane<ValueType> extends JScrollPane implements ValuePaneWrapper<ValueType> {
        private static final long serialVersionUID = 1L;
        protected GuiSwingView.ValuePane<ValueType> pane;

        @SuppressWarnings("unchecked")
        public ValueScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
            super(view, vsbPolicy, hsbPolicy);
            if (view instanceof GuiSwingView.ValuePane) {
                this.pane = (GuiSwingView.ValuePane<ValueType>) view;
            }
        }

        @SuppressWarnings("unchecked")
        public ValueScrollPane(Component view) {
            super(view);
            if (view instanceof GuiSwingView.ValuePane) {
                this.pane = (GuiSwingView.ValuePane<ValueType>) view;
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
        public void setSwingViewValue(ValueType value, GuiTaskClock clock) {
            if (pane != null) {
                pane.setSwingViewValue(value, clock);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(ValueType value, GuiTaskClock clock) {
            if (pane != null) {
                pane.setSwingViewValueWithUpdate(value, clock);
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
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            if (pane != null) {
                pane.addSwingEditFinishHandler(eventHandler);
            }
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return pane == null ? null : pane.getSwingViewContext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public GuiSwingView.ValuePane<Object> getSwingViewWrappedPane() {
            return (GuiSwingView.ValuePane<Object>) pane;
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return pane == null ? GuiReprValue.NONE : pane.getSpecifier();
        }

        @Override
        public void prepareForRefresh() { }
    }

    public static class ValueWrappingPane<ValueType> extends JPanel implements ValuePaneWrapper<ValueType> {
        private static final long serialVersionUID = 1L;
        protected GuiSwingView.ValuePane<ValueType> pane;

        public ValueWrappingPane(Component view) {
            super(new BorderLayout());
            setOpaque(false);
            add(view, BorderLayout.CENTER);
        }

        @SuppressWarnings("unchecked")
        private void setPane(Component view) {
            if (view instanceof GuiSwingView.ValuePane) {
                this.pane = (GuiSwingView.ValuePane<ValueType>) view;
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
        public void setSwingViewValue(ValueType value, GuiTaskClock clock) {
            if (pane != null) {
                pane.setSwingViewValue(value, clock);
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(ValueType value, GuiTaskClock clock) {
            if (pane != null) {
                pane.setSwingViewValueWithUpdate(value, clock);
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
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            if (pane != null) {
                pane.addSwingEditFinishHandler(eventHandler);
            }
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return pane == null ? null : pane.getSwingViewContext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public GuiSwingView.ValuePane<Object> getSwingViewWrappedPane() {
            return (GuiSwingView.ValuePane<Object>) pane;
        }

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return pane == null ? GuiReprValue.NONE : pane.getSpecifier();
        }

        @Override
        public void prepareForRefresh() { }
    }
}
