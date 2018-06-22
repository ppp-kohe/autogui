package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingView;

/**
 * the dynamic version of {@link GuiSwingTableColumn}, creating a factory instead of a column
 */
public interface GuiSwingTableColumnDynamic extends GuiSwingElement {
    ObjectTableModelColumns.DynamicColumnFactory createColumnDynamic(GuiMappingContext context,
                                                                     GuiSwingTableColumnSet.TableColumnHost model,
                                                                     GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
                                                                     GuiSwingView.SpecifierManager parentSpecifier, boolean tableTop);

}
