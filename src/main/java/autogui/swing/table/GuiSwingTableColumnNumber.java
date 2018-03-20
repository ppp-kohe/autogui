package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueNumberSpinner;
import autogui.swing.GuiSwingViewLabel;
import autogui.swing.GuiSwingViewNumberSpinner;

import javax.swing.*;
import java.util.Comparator;

/**
 * a column factory for a {@link Number}.
 *
 * <p>
 *     the renderer is realized by {@link autogui.swing.GuiSwingViewLabel.PropertyLabel}.
 *     the editor is realized by {@link autogui.swing.GuiSwingViewNumberSpinner.PropertyNumberSpinner}.
 */
public class GuiSwingTableColumnNumber implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        GuiSwingViewLabel.PropertyLabel label = new GuiSwingViewLabel.PropertyLabel(context);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setOpaque(true);
        GuiSwingViewNumberSpinner.PropertyNumberSpinner spinner = new GuiSwingViewNumberSpinner.PropertyNumberSpinner(context);
        spinner.getEditorField().setBorder(BorderFactory.createEmptyBorder());
        return new ObjectTableColumnValue(context,
                label,
                spinner)
                .withRowHeight(spinner.getPreferredSize().height)
                .withComparator(new NumberComparator());
    }

    /**
     * a comparator for comparing numbers */
    public static class NumberComparator implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            if (o1 instanceof Number && o2 instanceof Number) {
                return GuiReprValueNumberSpinner.compare((Number) o1, (Number) o2);
            }
            return 0;
        }
    }
}
