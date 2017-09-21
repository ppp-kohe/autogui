package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueFilePathField;
import autogui.swing.util.NamedPane;
import autogui.swing.util.SearchTextFieldFilePath;

import javax.swing.*;

public class GuiSwingViewFilePathField implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyFilePathPane field = new PropertyFilePathPane(context);
        if (context.isTypeElementProperty()) {
            return new NamedPane(context.getDisplayName(), field);
        } else {
            return field;
        }
    }

    public static class PropertyFilePathPane extends SearchTextFieldFilePath
            implements GuiMappingContext.SourceUpdateListener {
        protected GuiMappingContext context;

        public PropertyFilePathPane(GuiMappingContext context) {
            this.context = context;


            getField().setEditable(((GuiReprValueFilePathField) context.getRepresentation())
                    .isEditable(context));

            context.addSourceUpdateListener(this);
            update(context, context.getSource());
        }

        @Override
        public void updateFieldInEvent(boolean modified) {
            super.updateFieldInEvent(modified);
            if (modified) {
                GuiReprValueFilePathField path = (GuiReprValueFilePathField) context.getRepresentation();
                path.updateFromGui(context, getFile());
            }
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setFile(((GuiReprValueFilePathField) context.getRepresentation())
                .toUpdateValue(context, newValue)));
        }
    }
}
