package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewEnumComboBox;
import autogui.swing.GuiSwingViewLabel;

import javax.swing.*;
import java.util.Comparator;

/**
 * a column factory for {@link Enum}.
 *
 * <p>
 *    The renderer is realized by {@link autogui.swing.GuiSwingViewLabel.PropertyLabel}.
 *    The editor is realized by {@link autogui.swing.GuiSwingViewEnumComboBox.PropertyEnumComboBox}.
 */
public class GuiSwingTableColumnEnum implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        GuiSwingViewLabel.PropertyLabel label = new GuiSwingViewLabel.PropertyLabel(context);
        label.setOpaque(true);

        GuiSwingViewEnumComboBox.PropertyEnumComboBox comboBox = new GuiSwingViewEnumComboBox.PropertyEnumComboBox(context);
        comboBox.setBorder(BorderFactory.createEmptyBorder());
        comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        ObjectTableColumnValue.ObjectTableCellEditor editor = new ObjectTableColumnValue.ObjectTableCellEditor(
                comboBox, false);
        editor.setClickCount(2);

        return new ObjectTableColumnValue(context,
                new ObjectTableColumnValue.ObjectTableCellRenderer(label),
                editor)
                .withComparator(Comparator.naturalOrder());
    }
}
