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

    /**
     * @param target the target accessor used by the returned menu-element
     * @return a menu-element including JMenu, JMenuItem, or JComponent
     */
    Object convert(GuiReprCollectionTable.TableTargetColumn target);
}
