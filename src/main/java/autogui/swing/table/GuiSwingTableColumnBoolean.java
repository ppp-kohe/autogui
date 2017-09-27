package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewBooleanCheckBox;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

public class GuiSwingTableColumnBoolean implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        GuiSwingViewBooleanCheckBox.PropertyCheckBox view = new GuiSwingViewBooleanCheckBox.PropertyCheckBox(context);
        view.setHorizontalAlignment(SwingConstants.CENTER);
        view.setBorderPainted(true);
        view.setOpaque(true);

        GuiSwingViewBooleanCheckBox.PropertyCheckBox editor = new GuiSwingViewBooleanCheckBox.PropertyCheckBox(context);
        editor.setHorizontalAlignment(SwingConstants.CENTER);
        editor.setBorderPainted(false);
        editor.setBorder(BorderFactory.createEmptyBorder());
        editor.setOpaque(true);


        ObjectTableColumnValue column = new ObjectTableColumnValue(context, view, editor);
        column.withComparator(Comparator.comparing(Boolean.class::cast));

        ((ObjectTableColumnValue.ObjectTableCellEditor) column.getTableColumn().getCellEditor()).setClickCount(0);
        return column;
    }


}
