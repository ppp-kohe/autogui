package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;

public class GuiSwingViewEmbeddedComponent implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        ValuePane comp = new PropertyEmbeddedPane(context);
        if (context.isTypeElementProperty()) {
            return comp.wrapProperty();
        } else {
            return comp.asComponent();
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static class PropertyEmbeddedPane extends JComponent
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;
        protected PopupExtension popup;

        public PropertyEmbeddedPane(GuiMappingContext context) {
            setLayout(new BorderLayout());
            this.context = context;
            update(context, context.getSource());
            setPreferredSize(new Dimension(300, 200));
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return (sender, menu) -> {};
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            if (getComponentCount() == 0) {
                return null;
            } else {
                return getComponent(0);
            }
        }

        @Override
        public void setSwingViewValue(Object value) {
            if (value == null) {
                if (getComponentCount() > 0) {
                    remove(0);
                }
            } else {
                JComponent comp = (JComponent) value;
                add(comp, 0);
                setPreferredSize(comp.getPreferredSize());
            }
            revalidate();
        }
    }
}
