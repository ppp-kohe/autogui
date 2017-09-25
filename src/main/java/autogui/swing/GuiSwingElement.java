package autogui.swing;

import autogui.base.mapping.*;
import autogui.swing.table.GuiSwingTableColumnBoolean;
import autogui.swing.table.GuiSwingTableColumnString;

public interface GuiSwingElement {

    static GuiSwingMapperSet getDefaultMapperSet() {
        GuiSwingMapperSet set = new GuiSwingMapperSet();

        set.addReprClassTableColumn(GuiReprValueBooleanCheckBox.class, new GuiSwingTableColumnBoolean())
            .addReprClassTableColumn(GuiReprValueStringField.class, new GuiSwingTableColumnString())
            .addReprClass(GuiReprCollectionElement.class, new GuiSwingViewCollectionTable.TableColumnSetDefault(set))
            .addReprClass(GuiReprActionList.class, null); //nothing: handled by a sibling GuiSwingViewCollectionTable


        set.addReprClass(GuiReprValueBooleanCheckBox.class, new GuiSwingViewBooleanCheckBox())
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
