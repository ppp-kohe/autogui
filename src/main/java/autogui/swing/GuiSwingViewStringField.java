package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueStringField;
import autogui.swing.util.CategorizedPopup;
import autogui.swing.util.NamedPane;
import autogui.swing.util.SearchTextField;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class GuiSwingViewStringField implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyTextPane field = new PropertyTextPane(context);
        if (context.isTypeElementProperty()) {
            return new NamedPane(context.getDisplayName(), field);
        } else {
            return field;
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return false;
    }

    public static class PropertyTextPane extends SearchTextField implements GuiMappingContext.SourceUpdateListener {
        protected GuiMappingContext context;

        public PropertyTextPane(GuiMappingContext context) {
            this.context = context;
            getIcon().setVisible(false);

            GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
            getField().setEditable(str.isEditable(context));
            context.addSourceUpdateListener(this);

            update(context, context.getSource());
        }

        @Override
        public void updateFieldInEvent(boolean modified) {
            super.updateFieldInEvent(modified);
            if (modified) {
                GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
                str.updateFromGui(context, getField().getText());
            }
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            GuiReprValueStringField str = (GuiReprValueStringField) context.getRepresentation();
            SwingUtilities.invokeLater(() -> setTextWithoutUpdateField(str.toUpdateValue(context, newValue)));
        }
    }
}
