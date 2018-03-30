package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.util.MenuBuilder;

import javax.swing.*;

/**
 * a menu factory for selected column-rows
 */
public interface TableTargetMenu {
    /**
     * the category prefix
     */
    String MENU_COLUMN_ROWS = "Column Rows ";

    JMenu convert(GuiReprCollectionTable.TableTargetColumn target);
}
