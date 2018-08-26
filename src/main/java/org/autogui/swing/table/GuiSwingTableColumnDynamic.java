package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.swing.GuiSwingElement;
import org.autogui.swing.GuiSwingView;

/**
 * the dynamic version of {@link GuiSwingTableColumn}, creating a factory instead of a column
 */
public interface GuiSwingTableColumnDynamic extends GuiSwingElement {
    void createColumnDynamic(GuiMappingContext context,
                         GuiSwingTableColumnSet.TableColumnHost model,
                         GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
                         GuiSwingView.SpecifierManager parentSpecifier);

}
