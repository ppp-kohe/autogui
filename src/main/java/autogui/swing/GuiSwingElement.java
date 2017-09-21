package autogui.swing;

import autogui.base.mapping.*;

public interface GuiSwingElement {

    static GuiSwingMapperSet getDefaultMapperSet() {
        GuiSwingMapperSet set = new GuiSwingMapperSet();
        set.addReprClass(GuiReprValueBooleanCheckbox.class, new GuiSwingViewBooleanCheckbox())
            .addReprClass(GuiReprValueStringField.class, new GuiSwingViewStringField())
            .addReprClass(GuiReprValueNumberSpinner.class, new GuiSwingViewNumberSpinner())
            //TODO .addReprClass(GuiReprValueFilePathField.class, new GuiSwingViewFilePathField())
            .addReprClass(GuiReprValueEnumComboBox.class, new GuiSwingViewEnumComboBox())
            .addReprClass(GuiReprPropertyPane.class, new GuiSwingViewPropertyPane(set))
            .addReprClass(GuiReprAction.class, new GuiSwingActionDefault())
            .addReprClass(GuiRepresentation.class, new GUiSwingViewLabel());
        return set;
    }
}
