package org.autogui.swing.table;

import org.autogui.base.mapping.GuiReprCollectionTable;

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
