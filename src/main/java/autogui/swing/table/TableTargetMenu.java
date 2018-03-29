package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;

import javax.swing.*;

/**
 * a menu factory for selected column-rows
 */
public interface TableTargetMenu {
    String MENU_COLUMN_ROWS = MenuBuilder.getImplicitCategory("Column Rows");

    JMenu convert(GuiReprCollectionTable.TableTargetColumn target);
}
