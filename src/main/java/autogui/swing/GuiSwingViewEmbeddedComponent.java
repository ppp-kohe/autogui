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
        ValuePane<Object> comp = new PropertyEmbeddedPane(context);
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
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Object> {
        protected GuiMappingContext context;
        protected PopupExtension popup;
        protected JComponent component;

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
            GuiReprEmbeddedComponent embeddedComponent = (GuiReprEmbeddedComponent) getContext().getRepresentation();

            JComponent comp = embeddedComponent.toUpdateValue(getContext(), value, this::setSwingViewValueComponent);
            setSwingViewValueComponent(comp);
        }

        public void setSwingViewValueComponent(JComponent comp) {
            if (comp != null && comp != component) {
//                if (getComponentCount() > 0) {
//                    remove(0);
//                }
//            } else {
                if (component != null && getComponentCount() > 0) {
                    remove(component);
                }
                add(comp, 0);
                setPreferredSize(comp.getPreferredSize());
                component = comp;
                revalidate();
            }
            repaint();
        }

        @Override
        public void setSwingViewValueWithUpdate(Object value) {
            setSwingViewValue(value);
            GuiReprEmbeddedComponent repr = (GuiReprEmbeddedComponent) getContext().getRepresentation();
            if (repr.isEditable(getContext())) {
                repr.updateFromGui(getContext(), value);
            }
        }
    }
}
