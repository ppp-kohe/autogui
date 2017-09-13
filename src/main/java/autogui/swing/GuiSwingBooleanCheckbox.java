package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprBooleanCheckbox;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GuiSwingBooleanCheckbox {

    public JComponent create(GuiMappingContext context) {
        return new PropertyCheckBox(context);
    }

    public static class PropertyCheckBox extends JCheckBox implements ActionListener, GuiMappingContext.SourceUpdateListener {
        protected GuiMappingContext context;
        public PropertyCheckBox(GuiMappingContext context) {
            addActionListener(this);
            this.context = context;
            context.addSourceUpdateListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GuiReprBooleanCheckbox repr = (GuiReprBooleanCheckbox) context.getRepresentation();
            repr.updateFromGui(context, isSelected());
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            GuiReprBooleanCheckbox repr = (GuiReprBooleanCheckbox) context.getRepresentation();
            setSelected(repr.toUpdateValue(context, newValue));
        }
    }
}
