package autogui.base;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprBooleanCheckbox;
import autogui.base.mapping.GuiReprObjectPane;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeObject;
import autogui.base.type.GuiTypeValue;
import autogui.swing.GuiSwingBooleanCheckbox;

import javax.swing.*;

public class Exp {
    public static void main(String[] args) {
        JFrame frame = new JFrame();

        GuiMappingContext ctx = new GuiMappingContext(
                new GuiTypeObject(Exp.class.getName())
                        .addProperties(new GuiTypeMemberProperty("value", null, "getValue", null, new GuiTypeValue(boolean.class))),
                new GuiReprObjectPane(), new Exp());

        ctx.createChildCandidates().forEach(GuiMappingContext::addToParent);

        GuiMappingContext valueCtx = ctx.getChildren().get(0);
        valueCtx.setRepresentation(new GuiReprBooleanCheckbox());

        frame.add(new GuiSwingBooleanCheckbox.PropertyCheckBox(valueCtx));
        frame.pack();
        frame.setVisible(true);
    }

    public boolean getValue() {
        return true;
    }
}
