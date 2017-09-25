package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewCollectionTable;
import autogui.swing.GuiSwingViewStringField;

import java.util.Comparator;

public class GuiSwingTableColumnString implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        return new GuiSwingViewCollectionTable.ObjectTableColumnValue(context,
                new GuiSwingViewStringField.PropertyTextPane(context))
                    .withComparator(Comparator.comparing(String.class::cast));
    }
}
