package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;

import javax.swing.*;

/**
 * a menu factory for selected column-rows
 */
public interface TableTargetMenu {
    JMenu convert(GuiReprCollectionTable.TableTargetColumn target);
}
