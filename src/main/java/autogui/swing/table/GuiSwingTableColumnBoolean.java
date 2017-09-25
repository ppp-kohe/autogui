package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewBooleanCheckBox;
import autogui.swing.GuiSwingViewCollectionTable;

import javax.swing.*;
import java.util.Comparator;

public class GuiSwingTableColumnBoolean implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox view = new GuiSwingViewBooleanCheckBox.PropertyCheckBox(context);
        view.setHorizontalAlignment(SwingConstants.CENTER);
        view.setBorderPainted(true);

        return new GuiSwingViewCollectionTable.ObjectTableColumnValue(context, view)
                    .withComparator(Comparator.comparing(Boolean.class::cast));
    }

}
