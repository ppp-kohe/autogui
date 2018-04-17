package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewLabel;

/**
 * a column factory for any type of object.
 *
 * <p>
 *     the renderer is realized by {@link autogui.swing.GuiSwingViewLabel.PropertyLabel}.
 *     no editors.
 */
public class GuiSwingTableColumnLabel extends GUiSwingTableColumnStatic implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumnWithIndex(GuiMappingContext context, ObjectColumnIndex index) {
        GuiSwingViewLabel.PropertyLabel view = new GuiSwingViewLabel.PropertyLabel(context);
        view.setOpaque(true);
        return new ObjectTableColumnValue(context, view);
    }
}
