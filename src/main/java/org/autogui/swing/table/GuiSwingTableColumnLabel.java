package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingViewLabel.PropertyLabel;

/**
 * a column factory for any type of object.
 *
 * <p>
 *     the renderer is realized by {@link PropertyLabel}.
 *     no editors.
 */
public class GuiSwingTableColumnLabel implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          SpecifierManager parentSpecifier) {
        SpecifierManager valueSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);
        PropertyLabel view = new PropertyLabel(context, valueSpecifier);
        view.setOpaque(true);
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier, view);
    }
}
