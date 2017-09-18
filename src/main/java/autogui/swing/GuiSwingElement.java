package autogui.swing;

import autogui.base.mapping.GuiReprAction;
import autogui.base.mapping.GuiReprPropertyPane;
import autogui.base.mapping.GuiReprValueBooleanCheckbox;
import autogui.base.mapping.GuiReprValueStringField;

public interface GuiSwingElement {

    static GuiSwingMapperSet getDefaultMapperSet() {
        GuiSwingMapperSet set = new GuiSwingMapperSet();
        set.addReprClass(GuiReprValueBooleanCheckbox.class, new GuiSwingViewBooleanCheckbox())
            .addReprClass(GuiReprValueStringField.class, new GuiSwingViewStringField())
            .addReprClass(GuiReprPropertyPane.class, new GuiSwingViewPropertyPane(set))
            .addReprClass(GuiReprAction.class, new GuiSwingActionDefault());
        return set;
    }
}
