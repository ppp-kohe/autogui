package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.util.MenuBuilder;

import javax.swing.*;

/**
 * a menu factory for selected column-rows
 */
public interface TableTargetMenu {
    String MENU_COLUMN_ROWS = MenuBuilder.getCategoryImplicit("Column Rows");

    JMenu convert(GuiReprCollectionTable.TableTargetColumn target);
}
