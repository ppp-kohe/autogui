package autogui.swing.table;

import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.GuiSwingView;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionSender;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/** indicating an action can be converted for selected table columns (with entire rows or selected cells) processing,
 *   handled by {@link autogui.swing.table.ObjectTableColumnValue.CollectionRowsActionBuilder}.
 *   The key-point is that the action can be statically constructed for (selected or entire) rows of the column and
 *      focused on the single target column.
 *
 *  <p>
 *  a collection table whose model is {@link ObjectTableModel} builds menu items including actions of the type.
 *   It obtains an selected column {@link ObjectTableColumn} as the target
 *   and calls {@link ObjectTableColumn#getMenuBuilderSource()} to the target.
 *   The current impl. of the method returns the cell renderer implementing {@link autogui.swing.table.ObjectTableColumn.PopupMenuBuilderSource}.
 *      For instance, those renderers are {@link ObjectTableColumnValue.ObjectTableCellRenderer} and
 *       {@link ObjectTableColumn.NumberRenderer} (for indexes).
 *   <p>
 *       In  the case of {@link ObjectTableColumnValue.ObjectTableCellRenderer},
 *         it creates {@link ObjectTableColumnValue.ObjectTableColumnActionBuilder} with
 *           original builder of the column by {@link GuiSwingView.ValuePane#getSwingMenuBuilder()}.
 *          The original builder creates menu items for a non-table view.
 *          Those items are converted to {@link TableTargetColumnAction}s
 *           by {@link autogui.swing.table.ObjectTableColumnValue.CollectionRowsActionBuilder}
 *            in the {@link autogui.swing.table.ObjectTableColumnValue.ObjectTableColumnActionBuilder#build(PopupExtensionSender, Consumer)} .
 *   */
public interface TableTargetColumnAction extends Action {
    void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target);
}
