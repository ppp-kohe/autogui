package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewLabel;

/**
 * a column factory for any type of object.
 *
 * <p>
 *     the renderer is realized by {@link autogui.swing.GuiSwingViewLabel.PropertyLabel}.
 *     no editors.
 */
public class GuiSwingTableColumnLabel implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          GuiSwingView.SpecifierManager parentSpecifier) {
        GuiSwingViewLabel.PropertyLabel view = new GuiSwingViewLabel.PropertyLabel(context, parentSpecifier);
        view.setOpaque(true);
        return new ObjectTableColumnValue(context, rowSpecifier, parentSpecifier, view);
    }
}
