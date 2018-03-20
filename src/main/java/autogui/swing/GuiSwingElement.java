package autogui.swing;

import autogui.base.mapping.*;
import autogui.swing.mapping.GuiReprEmbeddedComponent;
import autogui.swing.mapping.GuiReprValueDocumentEditor;
import autogui.swing.mapping.GuiReprValueImagePane;
import autogui.swing.table.*;

public interface GuiSwingElement {
    /**
     * @return the default set of representation including swing-based items.
     *    {@link GuiRepresentation#getDefaultSet()} + reprs in autogui.swing.mapping.*
     */
    static GuiReprSet getReprDefaultSet() {
        GuiReprSet set = new GuiReprSet();
        set.add(new GuiReprCollectionElement(set));

        set.add(new GuiReprValueBooleanCheckBox(),
                new GuiReprValueEnumComboBox(),
                new GuiReprValueFilePathField(),
                new GuiReprValueNumberSpinner(),
                new GuiReprValueStringField());

        set.add(new GuiReprEmbeddedComponent(),
                new GuiReprValueDocumentEditor(),
                new GuiReprValueImagePane());

        set.add(new GuiReprCollectionTable(set),
                new GuiReprObjectPane(set),
                new GuiReprPropertyPane(set),
                new GuiReprAction(),
                new GuiReprActionList());

        set.add(new GuiReprValueLabel());

        return set;
    }

    static GuiSwingMapperSet getDefaultMapperSet() {
        GuiSwingMapperSet set = new GuiSwingMapperSet();

        set.addReprClassTableColumn(GuiReprValueBooleanCheckBox.class, new GuiSwingTableColumnBoolean())
            .addReprClassTableColumn(GuiReprValueStringField.class, new GuiSwingTableColumnString())
            .addReprClassTableColumn(GuiReprValueEnumComboBox.class, new GuiSwingTableColumnEnum())
            .addReprClassTableColumn(GuiReprValueFilePathField.class, new GuiSwingTableColumnFilePath())
            .addReprClassTableColumn(GuiReprValueNumberSpinner.class, new GuiSwingTableColumnNumber())
            .addReprClassTableColumn(GuiReprValueImagePane.class, new GuiSwingTableColumnImage())
            .addReprClassTableColumn(GuiRepresentation.class, new GuiSwingTableColumnLabel())
            .addReprClass(GuiReprCollectionElement.class, new GuiSwingTableColumnSetDefault(set))
            .addReprClass(GuiReprActionList.class, null); //nothing: handled by a sibling GuiSwingViewCollectionTable


        set.addReprClass(GuiReprValueBooleanCheckBox.class, new GuiSwingViewBooleanCheckBox())
            .addReprClass(GuiReprValueDocumentEditor.class, new GuiSwingViewDocumentEditor())
            .addReprClass(GuiReprValueEnumComboBox.class, new GuiSwingViewEnumComboBox())
            .addReprClass(GuiReprValueFilePathField.class, new GuiSwingViewFilePathField())
            .addReprClass(GuiReprValueImagePane.class, new GuiSwingViewImagePane())
            .addReprClass(GuiReprValueNumberSpinner.class, new GuiSwingViewNumberSpinner())
            .addReprClass(GuiReprValueStringField.class, new GuiSwingViewStringField())
            .addReprClass(GuiReprCollectionTable.class, new GuiSwingViewCollectionTable(set))
            .addReprClass(GuiReprPropertyPane.class, new GuiSwingViewPropertyPane(set))
            .addReprClass(GuiReprObjectPane.class, new GuiSwingViewObjectPane(set))
            .addReprClass(GuiReprAction.class, new GuiSwingActionDefault())
            .addReprClass(GuiRepresentation.class, new GuiSwingViewLabel());
        return set;
    }
}
