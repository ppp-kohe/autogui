package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.GuiSwingJsonTransfer;
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
import java.awt.event.KeyEvent;
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
    public static class TableMenuCompositeToStringCopy implements ObjectTableColumn.TableMenuComposite {
        protected GuiMappingContext context;
        protected int index;

        public TableMenuCompositeToStringCopy(int index) {
            this.index = index;
        }

        public TableMenuCompositeToStringCopy(GuiMappingContext context, int index) {
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

    public static TableMenuCompositeSharedToStringCopy shared = new TableMenuCompositeSharedToStringCopy();

    /**
     * the key of {@link TableMenuCompositeToStringCopy}
     */
    public static class TableMenuCompositeSharedToStringCopy implements ObjectTableColumn.TableMenuCompositeShared {
        @Override
        public List<PopupCategorized.CategorizedMenuItem> composite(JTable table, List<ObjectTableColumn.TableMenuComposite> columns, boolean row) {
            List<TableMenuCompositeToStringCopy> cs = columns.stream()
                    .map(TableMenuCompositeToStringCopy.class::cast)
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
        protected List<TableMenuCompositeToStringCopy> activatedColumns;
        protected boolean onlyApplyingSelectedColumns;

        public ToStringCopyForCellsAction(List<TableMenuCompositeToStringCopy> activatedColumns, boolean onlyApplyingSelectedColumns) {
            putValue(NAME, onlyApplyingSelectedColumns ? "Copy Cells as Text" : "Copy Row Cells as Text");
            if (onlyApplyingSelectedColumns) {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            }
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
                TableMenuCompositeToStringCopy col = getMenuCompositeForCell(cell);
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

        public TableMenuCompositeToStringCopy getMenuCompositeForCell(GuiReprCollectionTable.CellValue cell) {
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

        public ToStringSaveForCellsAction(List<TableMenuCompositeToStringCopy> activatedColumns, boolean onlyApplyingSelectedColumns,
                                          JComponent table) {
            super(activatedColumns, onlyApplyingSelectedColumns);
            putValue(NAME, onlyApplyingSelectedColumns ? "Save Cells as Text..." : "Save Row Cells as Text...");
            if (onlyApplyingSelectedColumns) {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            }
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

    ///////////////

    public static TableMenuCompositeSharedToStringPaste pasteShared = new TableMenuCompositeSharedToStringPaste();

    public static class TableMenuCompositeToStringPaste extends TableMenuCompositeToStringCopy {
        public TableMenuCompositeToStringPaste(int index) {
            super(index);
        }

        public TableMenuCompositeToStringPaste(GuiMappingContext context, int index) {
            super(context, index);
        }

        public boolean isIndexColumn() {
            return context == null;
        }

        public Object toValueFromString(String s) {
            return context.getRepresentation().fromHumanReadableString(context, s);
        }

        @Override
        public ObjectTableColumn.TableMenuCompositeShared getShared() {
            return pasteShared;
        }
    }

    public static class TableMenuCompositeSharedToStringPaste implements ObjectTableColumn.TableMenuCompositeShared  { //TODO

        @Override
        public List<PopupCategorized.CategorizedMenuItem> composite(JTable table, List<ObjectTableColumn.TableMenuComposite> columns, boolean row) {
            List<TableMenuCompositeToStringPaste> cs = columns.stream()
                    .map(TableMenuCompositeToStringPaste.class::cast)
                    .filter(e -> e.getIndex() != -1)
                    .collect(Collectors.toList());
            return Arrays.asList(
                    new ToStringPasteForCellsAction(cs, !row),
                    new ToStringLoadForCellsAction(cs, !row, table));
        }
    }

    public static class ToStringPasteForCellsAction extends PopupExtensionText.TextPasteAllAction implements TableTargetCellAction {
        protected List<TableMenuCompositeToStringPaste> activeComposites;
        protected String lineSeparator = "\\n"; //regex
        protected String columnSeparator = "\\t"; //regex
        protected boolean onlyApplyingSelectedColumns;

        public ToStringPasteForCellsAction(List<TableMenuCompositeToStringPaste> activeComposites, boolean onlyApplyingSelectedColumns) {
            super(null);
            putValue(NAME, onlyApplyingSelectedColumns ? "Paste Text to Cells" : "Paste Text to Row Cells");
            if (onlyApplyingSelectedColumns) {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            } else {
                putValue(ACCELERATOR_KEY, null);
            }
            this.activeComposites = activeComposites;
            this.onlyApplyingSelectedColumns = onlyApplyingSelectedColumns;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }


        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            paste(s -> run(s, target));
        }

        public void run(String str, GuiReprCollectionTable.TableTargetCell target) {
            int rowIndex = 0;
            GuiSwingJsonTransfer.JsonFillLoop fillLoop = new GuiSwingJsonTransfer.JsonFillLoop();
            for (String line :str.split(lineSeparator)) {
                if (fillLoop.addRow(runLine(line, rowIndex))) {
                    ++rowIndex;
                }
            }
            target.setCellValues(target.getSelectedRowAllCellIndexesStream(), fillLoop);
        }

        public List<GuiReprCollectionTable.CellValue> runLine(String line, int targetRow) {
            List<GuiReprCollectionTable.CellValue> updatedRow = new ArrayList<>();
            boolean rowSpecified = false;
            int rowIndex = targetRow;
            int c = 0;
            for (String col : line.split(columnSeparator)) {
                TableMenuCompositeToStringPaste composite = activeComposites.get(c % activeComposites.size());
                if (composite.isIndexColumn()) { //specify the row index
                    targetRow = Integer.valueOf(col);
                    rowSpecified = true;
                } else {
                    updatedRow.add(new GuiReprCollectionTable.CellValue(targetRow, composite.getIndex(),
                            composite.toValueFromString(col)));
                }

                ++c;
            }
            if (targetRow != rowIndex) {
                for (GuiReprCollectionTable.CellValue cell : updatedRow) {
                    cell.row = targetRow;
                }
            }
            if (rowSpecified) {
                return updatedRow.stream()
                        .map(e -> new GuiSwingJsonTransfer.CellValueRowSpecified(e.row, e.column, e.value))
                        .collect(Collectors.toList());
            } else {
                return updatedRow;
            }
        }

        @Override
        public String getCategory() {
            return onlyApplyingSelectedColumns ? MENU_CATEGORY_CELL : MENU_CATEGORY_ROW;
        }
    }

    public static class ToStringLoadForCellsAction extends ToStringPasteForCellsAction {
        protected JComponent table;
        protected PopupExtensionText.TextLoadAction loader;

        public ToStringLoadForCellsAction(List<TableMenuCompositeToStringPaste> activeComposites, boolean onlyApplyingSelectedColumns, JComponent table) {
            super(activeComposites, onlyApplyingSelectedColumns);
            putValue(NAME, onlyApplyingSelectedColumns ? "Load Text to Cells..." : "Load Text to Row Cells...");
            if (onlyApplyingSelectedColumns) {
                putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            } else {
                putValue(ACCELERATOR_KEY, null);
            }
            this.table = table;
            loader = new PopupExtensionText.TextLoadAction(null) {
                @Override
                protected JComponent getComponent() {
                    return table;
                }
            };
        }

        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            String line = loader.load();
            if (line != null) {
                run(line, target);
            }
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_IMPORT;
        }
    }
}
