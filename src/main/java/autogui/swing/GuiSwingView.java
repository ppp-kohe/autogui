package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;
import java.util.EventObject;
import java.util.function.Consumer;

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

        default void addSwingEditFinishHandler(Consumer<EventObject> eventHandler) {
        }

        PopupExtension.PopupMenuBuilder getSwingMenuBuilder();
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
    }
}
