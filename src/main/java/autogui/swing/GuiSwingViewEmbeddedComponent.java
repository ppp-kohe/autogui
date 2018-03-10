package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.mapping.GuiReprEmbeddedComponent;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;

/**
 * <h3>representation</h3>
 * {@link autogui.swing.mapping.GuiReprEmbeddedComponent}
 *
 * <h3>{@link PropertyEmbeddedPane#getSwingViewValue()}</h3>
 * {@link JComponent}
 *
 * <h3>history-value</h3>
 * unsupported
 *
 * <h3>string-transfer</h3>
 * unsupported.
 */
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
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<JComponent> {
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
            return (sender, menu) -> {
            };
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue((JComponent) newValue));
        }

        @Override
        public JComponent getSwingViewValue() {
            if (getComponentCount() == 0) {
                return null;
            } else {
                return (JComponent) getComponent(0);
            }
        }

        @Override
        public void setSwingViewValue(JComponent value) {
            if (value == null) {
                if (getComponentCount() > 0) {
                    remove(0);
                }
            } else {
                add(value, 0);
                setPreferredSize(value.getPreferredSize());
            }
            revalidate();
        }

        @Override
        public void setSwingViewValueWithUpdate(JComponent value) {
            setSwingViewValue(value);
            ((GuiReprEmbeddedComponent) getContext().getRepresentation())
                    .updateFromGui(getContext(), value);
        }
    }
}
