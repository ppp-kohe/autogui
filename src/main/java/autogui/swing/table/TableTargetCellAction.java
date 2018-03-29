package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * action for processing selected cell values or selected rows with all cells in those rows processed
 *    by  {@link autogui.swing.table.ObjectTableModel.TableTargetCellExecutionAction}.
 *
 *  <p>
 *    a collection table whose model is {@link ObjectTableModel} builds menu items including actions of the type.
 *    For selected cells values, it obtains selected columns (a list of {@link ObjectTableColumn}s) and
 *     calls {@link ObjectTableModel#getBuilderForRowsOrCells(JTable, List, boolean)}
 *     with the columns and row=false.
 *    For selected rows, it obtains all columns and calls the same method with the columns and row=true.
 *
 *  <p>
 *   In the building method, it calls {@link ObjectTableColumn#getCompositesForCells()}
 *   or {@link ObjectTableColumn#getCompositesForRows()},
 *     and obtains a list of {@link autogui.swing.table.ObjectTableColumn.TableMenuComposite}.
 *   The list means a list of all supported menu-items for each column.
 *    Each {@link ObjectTableColumn.TableMenuComposite#getShared()} return a shared "key" for those menus,
 *     indicating the menu identity, as {@link autogui.swing.table.ObjectTableColumn.TableMenuCompositeShared}.
 *  <p>
 *   For each {@link autogui.swing.table.ObjectTableColumn.TableMenuCompositeShared}
 *      with columns returned the same shared key,
 *      it calls {@link autogui.swing.table.ObjectTableColumn.TableMenuCompositeShared#composite(JTable, List, boolean)}
 *        and obtains {@link autogui.swing.table.ObjectTableModel.PopupMenuBuilderForRowsOrCells}
 *        for building an action summarizing those columns as {@link TableTargetCellAction}.
 *  <p>
 *   The {@link ObjectTableModel#getBuilderForRowsOrCells(JTable, List, boolean)}
 *    calls build method for each returned builder
 *      with {@link autogui.swing.table.ObjectTableModel.CollectionRowsAndCellsActionBuilder} as the menu appender.
 *    It converts {@link TableTargetCellAction} to
 *     {@link autogui.swing.table.ObjectTableModel.TableTargetCellExecutionAction} and add it to the menu.
 *  <p>
 *   Also, the returned builder will receive custom sender which becomes
 *      a subtype of {@link autogui.swing.util.PopupExtension}  for table processing,
 *     and the action can take custom table info including GuiMappingContext:
 *      for instance, GuiSwingCollectionTable provides PopupExtensionCollection.
 */
public interface TableTargetCellAction extends PopupCategorized.CategorizedMenuItemAction {

    String MENU_CATEGORY_ROW = "Rows";
    String MENU_CATEGORY_CELL = "Cells";

    void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target);

    @Override
    default String getCategory() {
        return MENU_CATEGORY_ROW;
    }
}
