package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;

import javax.swing.*;

public interface TableTargetMenu {
    JMenu convert(GuiReprCollectionTable.TableTargetColumn target);
}
