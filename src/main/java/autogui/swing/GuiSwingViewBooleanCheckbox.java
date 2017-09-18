package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueBooleanCheckbox;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GuiSwingViewBooleanCheckbox implements GuiSwingView {
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

            GuiReprValueBooleanCheckbox repr = (GuiReprValueBooleanCheckbox) context.getRepresentation();

            setEnabled(repr.isEditable(context));
            context.addSourceUpdateListener(this);
            setSelected(repr.toUpdateValue(context, context.getSource()));

            if (context.isTypeElementProperty()) {
                setText(context.getDisplayName());
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiReprValueBooleanCheckbox repr = (GuiReprValueBooleanCheckbox) context.getRepresentation();
            repr.updateFromGui(context, isSelected());
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            GuiReprValueBooleanCheckbox repr = (GuiReprValueBooleanCheckbox) context.getRepresentation();
            //setSelected seems not to cause ActionEvent
            setSelected(repr.toUpdateValue(context, newValue));
        }
    }
}
