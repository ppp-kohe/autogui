package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingElement;

import javax.swing.*;
import java.util.List;

/**
 * an interface of a set of {@link GuiSwingTableColumn}.
 */
public interface GuiSwingTableColumnSet extends GuiSwingElement {
    void createColumns(GuiMappingContext context, ObjectTableModel model);

    /**
     * create actions for sub-contexts associated to {@link autogui.base.mapping.GuiReprAction}.
     * @param context the context usually associated with {@link autogui.base.mapping.GuiReprCollectionElement}
     * @param source the table source for actions.
     *               the selected items will become targets of returned actions.
     * @return the created actions
     */
    List<Action> createColumnActions(GuiMappingContext context, TableSelectionSource source);

    /** an action target */
    interface TableSelectionSource {
        boolean isSelectionEmpty();
        List<?> getSelectedItems();
        void selectionActionFinished();
    }
}
