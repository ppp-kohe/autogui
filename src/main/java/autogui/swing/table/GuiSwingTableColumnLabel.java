package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewLabel;

import java.util.function.Supplier;

/**
 * a column factory for any type of object.
 *
 * <p>
 *     the renderer is realized by {@link autogui.swing.GuiSwingViewLabel.PropertyLabel}.
 *     no editors.
 */
public class GuiSwingTableColumnLabel implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> rowSpecifier) {
        GuiSwingView.SpecifierManagerDefault specifierManager = new GuiSwingView.SpecifierManagerDefault(rowSpecifier);
        GuiSwingViewLabel.PropertyLabel view = new GuiSwingViewLabel.PropertyLabel(context, specifierManager);
        view.setOpaque(true);
        return new ObjectTableColumnValue(context, specifierManager, view);
    }
}
