package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueBooleanCheckBox;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GuiSwingViewBooleanCheckBox implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        return new PropertyCheckBox(context);
    }

    public static class PropertyCheckBox extends JCheckBox implements ActionListener, GuiMappingContext.SourceUpdateListener {
        protected GuiMappingContext context;

        public PropertyCheckBox(GuiMappingContext context) {
            addActionListener(this);
            this.context = context;
            setName(context.getName());

            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();

            setEnabled(repr.isEditable(context));
            context.addSourceUpdateListener(this);

            if (context.isTypeElementProperty()) {
                setText(context.getDisplayName());
            }

            update(context, context.getSource());

            new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> { }); //TODO
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            repr.updateFromGui(context, isSelected());
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            GuiReprValueBooleanCheckBox repr = (GuiReprValueBooleanCheckBox) context.getRepresentation();
            //setSelected seems not to cause ActionEvent
            SwingUtilities.invokeLater(() -> setSelected(repr.toUpdateValue(context, newValue)));
        }
    }
}
