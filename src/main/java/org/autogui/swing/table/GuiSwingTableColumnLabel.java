package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingViewLabel.PropertyLabel;
import org.autogui.swing.util.TextCellRenderer;

import javax.swing.*;
import java.util.Comparator;
import java.util.Objects;

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
        TextCellRenderer.setCellDefaultProperties(view);
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier, view)
                .withBorderType(ObjectTableColumnValue.CellBorderType.Regular)
                .withComparator(Comparator.comparing(Objects::toString));
    }
}
