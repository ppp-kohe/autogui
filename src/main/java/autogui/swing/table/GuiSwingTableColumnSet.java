package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingElement;

import javax.swing.*;
import java.util.List;

public interface GuiSwingTableColumnSet extends GuiSwingElement {
    void createColumns(GuiMappingContext context, ObjectTableModel model);

    List<Action> createColumnActions(GuiMappingContext context, TableSelectionSource source);

    interface TableSelectionSource {
        boolean isSelectionEmpty();
        List<?> getSelectedItems();
        void selectionActionFinished();
    }
}
