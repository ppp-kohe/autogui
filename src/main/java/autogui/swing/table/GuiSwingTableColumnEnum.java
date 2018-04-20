package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewEnumComboBox;
import autogui.swing.GuiSwingViewLabel;

import javax.swing.*;
import java.util.Comparator;
import java.util.function.Supplier;

/**
 * a column factory for {@link Enum}.
 *
 * <p>
 *    The renderer is realized by {@link autogui.swing.GuiSwingViewLabel.PropertyLabel}.
 *    The editor is realized by {@link autogui.swing.GuiSwingViewEnumComboBox.PropertyEnumComboBox}.
 */
public class GuiSwingTableColumnEnum implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> rowSpecifier) {
        GuiSwingView.SpecifierManagerDefault specifierManager = new GuiSwingView.SpecifierManagerDefault(rowSpecifier);
        GuiSwingViewLabel.PropertyLabel label = new GuiSwingViewLabel.PropertyLabel(context, specifierManager);
        label.setOpaque(true);

        GuiSwingViewEnumComboBox.PropertyEnumComboBox comboBox = new GuiSwingViewEnumComboBox.PropertyEnumComboBox(context, specifierManager);
        comboBox.setBorder(BorderFactory.createEmptyBorder());
        comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        ObjectTableColumnValue.ObjectTableCellEditor editor = new ObjectTableColumnValue.ObjectTableCellEditor(
                comboBox, false);
        editor.setClickCount(2);

        return new ObjectTableColumnValue(context, specifierManager,
                new ObjectTableColumnValue.ObjectTableCellRenderer(label),
                editor)
                .withComparator(Comparator.naturalOrder());
    }
}
