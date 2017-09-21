package autogui.swing;

import autogui.base.mapping.*;

public interface GuiSwingElement {

    static GuiSwingMapperSet getDefaultMapperSet() {
        GuiSwingMapperSet set = new GuiSwingMapperSet();
        set.addReprClass(GuiReprValueBooleanCheckbox.class, new GuiSwingViewBooleanCheckbox())
            .addReprClass(GuiReprValueDocumentEditor.class, new GuiSwingViewDocumentEditor())
            .addReprClass(GuiReprValueEnumComboBox.class, new GuiSwingViewEnumComboBox())
            .addReprClass(GuiReprValueFilePathField.class, new GuiSwingViewFilePathField())
            .addReprClass(GuiReprValueImagePane.class, new GuiSwingViewImagePane())
            .addReprClass(GuiReprValueNumberSpinner.class, new GuiSwingViewNumberSpinner())
            .addReprClass(GuiReprValueStringField.class, new GuiSwingViewStringField())
            .addReprClass(GuiReprPropertyPane.class, new GuiSwingViewPropertyPane(set))
            .addReprClass(GuiReprObjectPane.class, new GuiSwingViewObjectPane(set))
            .addReprClass(GuiReprAction.class, new GuiSwingActionDefault())
            .addReprClass(GuiRepresentation.class, new GUiSwingViewLabel());
        return set;
    }
}
