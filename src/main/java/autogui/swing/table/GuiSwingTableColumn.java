package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingElement;

import java.util.function.Supplier;

/**
 * a column factory
 */
public interface GuiSwingTableColumn extends GuiSwingElement {
    ObjectTableColumn createColumn(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> rowSpecifier);
    default ObjectTableColumnDynamicFactory createColumnDynamic(GuiMappingContext context,
                                                                Supplier<GuiReprValue.ObjectSpecifier> rowSpecifier) {
        return null;
    }
}
