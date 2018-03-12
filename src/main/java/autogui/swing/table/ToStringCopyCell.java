package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ToStringCopyCell {
    public static class TableMenuCompositeToStringValue implements ObjectTableColumn.TableMenuComposite {
        protected GuiMappingContext context;
        protected int index;

        public TableMenuCompositeToStringValue(int index) {
            this.index = index;
        }

        public TableMenuCompositeToStringValue(GuiMappingContext context, int index) {
            this.context = context;
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public ObjectTableColumn.TableMenuCompositeShared getShared() {
            return shared;
        }

        public String toHumanReadableString(Object value) {
            if (context == null) {
                return "" + value;
            } else {
                return context.getRepresentation().toHumanReadableString(context, value);
            }
        }
    }

    public static TableMenuCompositeSharedToStringValue shared = new TableMenuCompositeSharedToStringValue();

    public static class TableMenuCompositeSharedToStringValue implements ObjectTableColumn.TableMenuCompositeShared {
        @Override
        public ObjectTableModel.PopupMenuBuilderForRowsOrCells composite(JTable table, List<ObjectTableColumn.TableMenuComposite> columns, boolean row) {
            return (sender, menu) -> {
                menu.accept(new ToStringCopyForCellsAction(columns.stream()
                        .map(TableMenuCompositeToStringValue.class::cast)
                        .collect(Collectors.toList()), !row));
            };
        }
    }

    public static class ToStringCopyForCellsAction extends AbstractAction implements TableTargetCellAction {
        protected List<TableMenuCompositeToStringValue> activatedColumns;
        protected boolean onlyApplyingSelectedColumns;

        public ToStringCopyForCellsAction(List<TableMenuCompositeToStringValue> activatedColumns, boolean onlyApplyingSelectedColumns) {
            putValue(NAME, onlyApplyingSelectedColumns ? "Copy Selected Cells As String" : "Copy Selected Row Cells As String");
            this.activatedColumns = activatedColumns;
            this.onlyApplyingSelectedColumns = onlyApplyingSelectedColumns;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void actionPerformedOnTableCell(ActionEvent e, TableTargetCell target) {
            copy(getString(target));
        }

        public String getString(TableTargetCell target) {
            //follow the visual ordering
            int prevLine = -1;
            List<String> cols = new ArrayList<>();
            List<String> lines = new ArrayList<>();
            List<TableTargetCell.CellValue> cells = (onlyApplyingSelectedColumns ? target.getSelectedCells()
                    : target.getSelectedRowCells());
            for (TableTargetCell.CellValue cell : cells) {
                TableMenuCompositeToStringValue col = getMenuCompositeForCell(cell);
                if (col != null) {
                    if (prevLine != cell.getRow() && prevLine != -1) {
                        lines.add(String.join("\t", cols));
                        cols.clear();
                    }
                    String colStr = col.toHumanReadableString(cell.getValue());
                    if (colStr != null) {
                        cols.add(colStr);
                    }
                    prevLine = cell.getRow();
                }
            }
            if (!cols.isEmpty()) {
                lines.add(String.join("\t", cols));
            }

            return String.join("\n", lines);
        }

        public TableMenuCompositeToStringValue getMenuCompositeForCell(TableTargetCell.CellValue cell) {
            return activatedColumns.stream()
                    .filter(cmp -> cmp.getIndex() == cell.getColumn())
                    .findFirst()
                    .orElse(null);
        }

        public void copy(String data) {
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection sel = new StringSelection(data);
            board.setContents(sel, sel);
        }
    }
}
