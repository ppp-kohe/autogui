package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.GuiSwingView;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.PopupExtensionText;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * classes for copying string description of selected cells and rows
 */
public class ToStringCopyCell {
    /**
     * a composite for to-string relying on
     * {@link autogui.base.mapping.GuiRepresentation#toHumanReadableString(GuiMappingContext, Object)}
     */
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

    /**
     * the key of {@link TableMenuCompositeToStringValue}
     */
    public static class TableMenuCompositeSharedToStringValue implements ObjectTableColumn.TableMenuCompositeShared {
        @Override
        public List<PopupCategorized.CategorizedMenuItem> composite(JTable table, List<ObjectTableColumn.TableMenuComposite> columns, boolean row) {
            List<TableMenuCompositeToStringValue> cs = columns.stream()
                    .map(TableMenuCompositeToStringValue.class::cast)
                    .filter(e -> e.getIndex() != -1)
                    .collect(Collectors.toList());
            return Arrays.asList(
                    new ToStringCopyForCellsAction(cs, !row),
                    new ToStringSaveForCellsAction(cs, !row, table));
        }
    }

    /**
     * the action defined by composition of selected columns; it joins cell strings by new-lines (for row) and tabs (for columns).
     */
    public static class ToStringCopyForCellsAction extends AbstractAction implements TableTargetCellAction {
        protected List<TableMenuCompositeToStringValue> activatedColumns;
        protected boolean onlyApplyingSelectedColumns;

        public ToStringCopyForCellsAction(List<TableMenuCompositeToStringValue> activatedColumns, boolean onlyApplyingSelectedColumns) {
            putValue(NAME, onlyApplyingSelectedColumns ? "Copy Cells As String" : "Copy Row Cells As String");
            this.activatedColumns = activatedColumns;
            this.onlyApplyingSelectedColumns = onlyApplyingSelectedColumns;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            copy(getString(target));
        }

        public String getString(GuiReprCollectionTable.TableTargetCell target) {
            //follow the visual ordering
            int prevLine = -1;
            List<String> cols = new ArrayList<>();
            List<String> lines = new ArrayList<>();
            List<GuiReprCollectionTable.CellValue> cells = (onlyApplyingSelectedColumns
                    ? target.getSelectedCells()
                    : target.getSelectedRowAllCells());
            for (GuiReprCollectionTable.CellValue cell : cells) {
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

        public TableMenuCompositeToStringValue getMenuCompositeForCell(GuiReprCollectionTable.CellValue cell) {
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

        @Override
        public String getCategory() {
            return onlyApplyingSelectedColumns ? MENU_CATEGORY_CELL : MENU_CATEGORY_ROW;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_COPY;
        }
    }

    public static void save(Supplier<String> data, JComponent component, String name) {
        SettingsWindow.FileDialogManager fd = SettingsWindow.getFileDialogManager();
        Path p = fd.showConfirmDialogIfOverwriting(component,
                fd.showSaveDialog(component, PopupExtensionText.getEncodingPane(), name + ".txt"));
        if (p != null) {
            try {
                Files.write(p, Collections.singletonList(data.get()), PopupExtensionText.selectedCharset);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static class ToStringSaveForCellsAction extends ToStringCopyForCellsAction {
        protected JComponent table;

        public ToStringSaveForCellsAction(List<TableMenuCompositeToStringValue> activatedColumns, boolean onlyApplyingSelectedColumns,
                                          JComponent table) {
            super(activatedColumns, onlyApplyingSelectedColumns);
            putValue(NAME, onlyApplyingSelectedColumns ? "Save Cells As String..." : "Save Row Cells As String...");
            this.table = table;
        }

        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            String name = "selection";
            if (table instanceof GuiSwingView.ValuePane<?>) {
                name = ((GuiSwingView.ValuePane) table).getSwingViewContext().getName();
            }
            save(() -> getString(target), table, name);
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_EXPORT;
        }
    }
}
