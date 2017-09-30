package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewEnumComboBox;
import autogui.swing.GuiSwingViewLabel;

import javax.swing.*;
import java.util.Comparator;

public class GuiSwingTableColumnEnum implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        GuiSwingViewLabel.PropertyLabel label = new GuiSwingViewLabel.PropertyLabel(context);
        label.setOpaque(true);

        GuiSwingViewEnumComboBox.PropertyEnumComboBox comboBox = new GuiSwingViewEnumComboBox.PropertyEnumComboBox(context);
        comboBox.setBorder(BorderFactory.createEmptyBorder());
        comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        ObjectTableColumnValue.ObjectTableCellEditor editor = new ObjectTableColumnValue.ObjectTableCellEditor(
                comboBox);
        editor.setClickCount(2);

        return new ObjectTableColumnValue(context,
                new ObjectTableColumnValue.ObjectTableCellRenderer(label),
                editor)
                .withComparator(Comparator.naturalOrder());
    }
}
