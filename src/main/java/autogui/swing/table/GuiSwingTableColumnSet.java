package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingView;

import javax.swing.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * an interface of a set of {@link GuiSwingTableColumn}.
 */
public interface GuiSwingTableColumnSet extends GuiSwingElement {
    void createColumns(GuiMappingContext context, ObjectTableModel model,
                       GuiSwingTableColumn.SpecifierManagerIndex rowSpecifier,
                       GuiSwingView.SpecifierManager specifierManager);

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

        /**
         * the finisher for the action: the table implements the method in order to refresh
         *  selected rows.
         *  However, naive executions of the action by list selection changes will cause infinite loop.
         *  To avoid this, an impl. of the method needs to check the flag and
         *     avoid handling list selection events caused by the method.
         *  @param autoSelection true if the action is automatically executed by selection changes
         */
        void selectionActionFinished(boolean autoSelection);
    }
}
