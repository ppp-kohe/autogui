package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.mapping.GuiReprValueLabel;
import autogui.swing.util.NamedPane;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.*;

public class GuiSwingViewLabel implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyLabel label = new PropertyLabel(context);
        if (context.isTypeElementProperty()) {
            return new NamedPane(context.getDisplayName(), label);
        } else {
            return label;
        }
    }

    public static class PropertyLabel extends JLabel
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane {
        protected GuiMappingContext context;
        protected Object value;
        protected PopupExtension popup;

        public PropertyLabel(GuiMappingContext context) {
            this.context = context;
            putClientProperty("html.disable", Boolean.TRUE);
            setMinimumSize(new Dimension(100, 20));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

            context.addSourceUpdateListener(this);

            update(context, context.getSource());

            JComponent info = GuiSwingContextInfo.get().getInfoLabel(context);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.accept(info);
            });
            setInheritsPopupMenu(true);
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue(newValue));
        }

        @Override
        public Object getSwingViewValue() {
            return value;
        }

        @Override
        public void setSwingViewValue(Object value) {
            GuiReprValue label = (GuiReprValue) context.getRepresentation();
            this.value = value;
            setText("" + label.toUpdateValue(context, value));
        }
    }
}

