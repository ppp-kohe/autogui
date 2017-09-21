package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueLabel;
import autogui.swing.util.NamedPane;

import javax.swing.*;
import java.awt.*;

public class GUiSwingViewLabel implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyLabel label = new PropertyLabel(context);
        if (context.isTypeElementProperty()) {
            return new NamedPane(context.getDisplayName(), label);
        } else {
            return label;
        }
    }

    public static class PropertyLabel extends JLabel implements GuiMappingContext.SourceUpdateListener {
        protected GuiMappingContext context;

        public PropertyLabel(GuiMappingContext context) {
            this.context = context;
            setMinimumSize(new Dimension(100, 20));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

            context.addSourceUpdateListener(this);

            update(context, context.getSource());
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            GuiReprValueLabel label = (GuiReprValueLabel) context.getRepresentation();

            SwingUtilities.invokeLater(() -> setText(label.toUpdateValue(context, newValue)));
        }
    }
}

