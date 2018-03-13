package autogui.swing;

import autogui.base.JsonReader;
import autogui.base.JsonWriter;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.table.ObjectTableColumn;
import autogui.swing.table.ObjectTableModel;
import autogui.swing.table.TableTargetCellAction;
import autogui.swing.table.TableTargetColumnAction;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GuiSwingJsonTransfer {

    public static List<JMenuItem> getActionMenuItems(GuiSwingView.ValuePane component, GuiMappingContext context) {
        return getActions(component, context).stream()
                .map(JMenuItem::new)
                .collect(Collectors.toList());

    }

    public static List<Action> getActions(GuiSwingView.ValuePane component, GuiMappingContext context) {
        return Collections.singletonList(new JsonCopyAction(component, context));
    }

    public static void copy(Object map) {
        if (map != null) {
            String src = JsonWriter.create().write(map).toSource();
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();

            JsonTransferable data = new JsonTransferable(src);
            board.setContents(data, data);
        }
    }

    public static class JsonCopyAction extends AbstractAction implements TableTargetColumnAction {
        protected GuiSwingView.ValuePane component;
        protected GuiMappingContext context;

        public JsonCopyAction(GuiSwingView.ValuePane component, GuiMappingContext context) {
            this.component = component;
            this.context = context;
            putValue(NAME, "Copy As JSON");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object value = component.getSwingViewValue();
            Object map = toCopiedJson(value);
            copy(map);
        }


        public Object toCopiedJson(Object value) {
            return context.getRepresentation().toJsonWithNamed(context, value);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            //suppose the map preserves order of values, and skip null element
            copy(target.getSelectedCellValues().stream()
                    .map(this::toCopiedJson)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
    }

    /** default representation: InputStream */
    public static DataFlavor jsonFlavor = new DataFlavor("application/json", "JSON text");

    public static class JsonTransferable implements Transferable, ClipboardOwner {

        public static DataFlavor[] flavors = {
                jsonFlavor,
                DataFlavor.stringFlavor
        };

        protected String data;

        public JsonTransferable(String data) {
            this.data = data;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return Arrays.stream(flavors)
                    .anyMatch(flavor::equals);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (flavor.equals(DataFlavor.stringFlavor)) {
                return data;
            } else if (flavor.equals(jsonFlavor)) {
                return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            }
            throw new UnsupportedFlavorException(flavor);
        }

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) { }
    }

    ////////////////////////////

    public static class TableMenuCompositeJsonCopy implements ObjectTableColumn.TableMenuComposite {
        protected GuiMappingContext context;
        protected int index;

        public TableMenuCompositeJsonCopy(int index) {
            this.index = index;
        }

        public TableMenuCompositeJsonCopy(GuiMappingContext context, int index) {
            this.context = context;
            this.index = index;
        }

        @Override
        public ObjectTableColumn.TableMenuCompositeShared getShared() {
            return shared;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            if (context == null) {
                return "#";
            } else {
                return context.getName();
            }
        }

        public Object toJsonWithName(Object value) {
            if (context == null && value instanceof Number) { //for indexes
                return new GuiReprValue.NamedValue(getName(), value);
            } else if (context != null) {
                return context.getRepresentation().toJsonWithNamed(context, value);
            } else {
                return null;
            }
        }
    }

    public static TableMenuCompositeSharedJsonCopy shared = new TableMenuCompositeSharedJsonCopy();

    public static class TableMenuCompositeSharedJsonCopy implements ObjectTableColumn.TableMenuCompositeShared {
        @Override
        public ObjectTableModel.PopupMenuBuilderForRowsOrCells composite(JTable table,
                                                                         List<ObjectTableColumn.TableMenuComposite> columns, boolean row) {
            if (row) {
                return (sender, menu) -> {
                    if (sender instanceof GuiSwingViewCollectionTable.PopupExtensionCollection) {
                        menu.accept(new JsonCopyRowAction(
                                ((GuiSwingViewCollectionTable.PopupExtensionCollection) sender).getTable().getContext()));
                    } else {
                        menu.accept(new JsonCopyCellsAction(toJsonCopies(columns)));
                    }
                };
            } else {
                return (sender, menu) -> {
                    menu.accept(new JsonCopyCellsAction(toJsonCopies(columns)));
                };
            }
        }

        private List<TableMenuCompositeJsonCopy> toJsonCopies(List<ObjectTableColumn.TableMenuComposite> cols) {
            return cols.stream()
                    .filter(TableMenuCompositeJsonCopy.class::isInstance)
                    .map(TableMenuCompositeJsonCopy.class::cast)
                    .collect(Collectors.toList());
        }
    }

    public static class JsonCopyRowAction extends AbstractAction implements TableTargetCellAction {
        protected GuiMappingContext context;

        public JsonCopyRowAction(GuiMappingContext context) {
            putValue(NAME, "Copy Selected Rows As JSON");
            this.context = context;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            copy(context.getRepresentation()
                    .toJson(context, target.getSelectedRowValues()));
        }

    }

    public static class JsonCopyCellsAction extends AbstractAction implements TableTargetCellAction {
        protected List<TableMenuCompositeJsonCopy> activatedColumns;

        public JsonCopyCellsAction(List<TableMenuCompositeJsonCopy> activatedColumns) {
            putValue(NAME, "Copy Selected Cells As JSON");
            this.activatedColumns = activatedColumns;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            int prevLine = -1;
            List<Object> lines = new ArrayList<>();
            Map<String,Object> cols = new LinkedHashMap<>(activatedColumns.size());

            for (GuiReprCollectionTable.CellValue cell : target.getSelectedCells()) {
                TableMenuCompositeJsonCopy col = getMenuCompositeForCell(cell);
                if (col != null) {
                    if (prevLine != cell.getRow() && prevLine != -1) {
                        lines.add(cols);
                        cols = new LinkedHashMap<>(activatedColumns.size());
                    }
                    Object colVal = col.toJsonWithName(cell.getValue());
                    if (colVal != null) {
                        if (colVal instanceof GuiReprValue.NamedValue) {
                            ((GuiReprValue.NamedValue) colVal).putTo(cols);
                        } else {
                            cols.put(col.getName(), colVal);
                        }
                    }
                    prevLine = cell.getRow();
                }
            }
            if (!cols.isEmpty()) {
                lines.add(cols);
            }
            copy(lines);
        }

        public TableMenuCompositeJsonCopy getMenuCompositeForCell(GuiReprCollectionTable.CellValue cell) {
            return activatedColumns.stream()
                    .filter(cmp -> cmp.getIndex() == cell.getColumn())
                    .findFirst()
                    .orElse(null);
        }
    }

    ////////////////////////////

    public static class TableMenuCompositeJsonPaste implements ObjectTableColumn.TableMenuComposite {
        protected GuiMappingContext context;
        protected int index;

        public TableMenuCompositeJsonPaste(int index) {
            this.index = index;
        }

        public TableMenuCompositeJsonPaste(GuiMappingContext context, int index) {
            this.context = context;
            this.index = index;
        }

        public boolean isIndexColumn() {
            return context == null;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public ObjectTableColumn.TableMenuCompositeShared getShared() {
            return jsonPasteShared;
        }

        public Object fromJson(Object columnValue) {
            if (isIndexColumn()) {
                if (columnValue instanceof Integer) {
                    return columnValue;
                } else {
                    return Integer.valueOf((String) columnValue);
                }
            } else {
                return context.getRepresentation().fromJson(context, null, columnValue);
            }
        }

        public boolean matchJsonKey(String key) {
            if (isIndexColumn()) {
                return key.equals("#");
            } else {
                return context.getName().equals(key);
            }
        }
    }

    public static TableMenuCompositeSharedJsonPaste jsonPasteShared = new TableMenuCompositeSharedJsonPaste();

    public static class TableMenuCompositeSharedJsonPaste implements ObjectTableColumn.TableMenuCompositeShared {
        @Override
        public ObjectTableModel.PopupMenuBuilderForRowsOrCells composite(JTable table,
                                                                         List<ObjectTableColumn.TableMenuComposite> columns, boolean row) {

            return (sender, menu) -> {
                menu.accept(new JsonPasteCellAction(toJsonPaste(columns)));
            };
        }

        private List<TableMenuCompositeJsonPaste> toJsonPaste(List<ObjectTableColumn.TableMenuComposite> columns) {
            return columns.stream()
                    .filter(TableMenuCompositeJsonPaste.class::isInstance)
                    .map(TableMenuCompositeJsonPaste.class::cast)
                    .collect(Collectors.toList());
        }
    }

    /**
     * a cell value indicating that it's row index is specified like { "#":123, ... }
     */
    public static class CellValueRowSpecified extends GuiReprCollectionTable.CellValue {
        public CellValueRowSpecified(int row, int column, Object value) {
            super(row, column, value);
        }
    }

    public static class JsonPasteCellAction extends AbstractAction implements TableTargetCellAction {
        protected List<TableMenuCompositeJsonPaste> activeComposite;

        public JsonPasteCellAction(List<TableMenuCompositeJsonPaste> activeComposite) {
            putValue(NAME, "Paste JSON To Selected Cells");
            this.activeComposite = activeComposite;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            Object json = readJson();

            /* separate entries to 2 types: free-rows and specified-rows
             *
             *   [{ "#":0, "value":"a" },  { "#":1, "value":"b" }, { "#":3, "value":"c"} ] : specifiedRows
             *   [{ "value":"d" }, { "value":"e"}, {"value":"f"} ] : freeRows
             *
             * selected cells:
             *    row0 <- specifiedRows[0]
             *    //omit row1
             *    row2 <- freeRows[0]
             *    row3 <- specifiedRows[2]
             *    row4 <- freeRows[1]
             *    row5 <- freeRows[2]
             *    row6 <- freeRows[0]
             *    ...
             */
            if (json != null) {
                JsonFillLoop fillLoop = new JsonFillLoop();
                if (json instanceof List<?>) { //[ ... ]
                    int rowIndex = 0;
                    for (Object o : (List<?>) json) {
                        List<GuiReprCollectionTable.CellValue> updatedRow = getCellsForRow(rowIndex, o);
                        if (fillLoop.addRow(updatedRow)) {
                            ++rowIndex;
                        }
                    }
                } else {
                    List<GuiReprCollectionTable.CellValue> updatedRow = getCellsForRow(0, json); //single row
                    fillLoop.addRow(updatedRow);
                }
                target.setCellValues(target.getSelectedCellIndexesStream(), fillLoop);
            }
        }

        private boolean isRowSpecified(List<GuiReprCollectionTable.CellValue> updatedRow) {
            return updatedRow.stream()
                    .anyMatch(CellValueRowSpecified.class::isInstance); //all cells in the returned list has a same row
        }

        public List<GuiReprCollectionTable.CellValue> getCellsForRow(int rowIndex, Object rowJson) {
            int targetRow = rowIndex;
            boolean rowSpecified = false;
            List<GuiReprCollectionTable.CellValue> updatedRow = new ArrayList<>();

            if (rowJson instanceof List<?>) { // [ [...], [...], ...]
                List<?> row = (List<?>) rowJson;
                for (int i = 0, l = row.size(); i < l; ++i) {
                    TableMenuCompositeJsonPaste column = activeComposite.get(i % activeComposite.size());
                    if (column.isIndexColumn()) {
                        //specify the row index
                        targetRow = (Integer) column.fromJson(row.get(i));
                        rowSpecified = true;
                    } else {
                        updatedRow.add(new GuiReprCollectionTable.CellValue(targetRow, column.getIndex(),
                                column.fromJson(row.get(i))));
                    }
                }
            } else if (rowJson instanceof Map<?,?>) { //[ {...}, {...}, ...]
                for (Map.Entry<?,?> entry : ((Map<?,?>) rowJson).entrySet()) {
                    TableMenuCompositeJsonPaste column = find(entry.getKey());
                    if (column != null) {
                        if (column.isIndexColumn()) {
                            targetRow = (Integer) column.fromJson(entry.getValue());
                            rowSpecified = true;
                        } else {
                            updatedRow.add(new GuiReprCollectionTable.CellValue(targetRow, column.getIndex(),
                                    column.fromJson(entry.getValue())));
                        }
                    }
                }
            } else { // [ a, b, ... ]
                TableMenuCompositeJsonPaste uniqueColumn = activeComposite.stream()
                        .filter(e -> !e.isIndexColumn())
                        .findFirst()
                        .orElse(null);
                if (uniqueColumn != null) {
                    updatedRow.add(new GuiReprCollectionTable.CellValue(targetRow, uniqueColumn.getIndex(),
                            uniqueColumn.fromJson(rowJson)));
                }
            }

            if (targetRow != rowIndex) {
                for (GuiReprCollectionTable.CellValue c : updatedRow) {
                    c.row = targetRow;
                }
            }
            if (rowSpecified) {
                return updatedRow.stream()
                        .map(e -> new CellValueRowSpecified(e.row, e.column, e.value))
                        .collect(Collectors.toList());
            } else {
                return updatedRow;
            }
        }

        public TableMenuCompositeJsonPaste find(Object key) {
            String str = key.toString();
            return activeComposite.stream()
                    .filter(c -> c.matchJsonKey(str))
                    .findFirst()
                    .orElse(null);
        }

        public Object readJson() {
            String json = readJsonSource();
            if (json != null) {
                return JsonReader.create(json).parseValue();
            } else {
                return null;
            }
        }

        public String readJsonSource() {
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            try {
                if (board.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                    return (String) board.getData(DataFlavor.stringFlavor);
                } else if (board.isDataFlavorAvailable(jsonFlavor)) {
                    try (InputStream in = (InputStream) board.getData(jsonFlavor)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                        List<String> lines = new ArrayList<>();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            lines.add(line);
                        }
                        return String.join("\n", lines);
                    }
                }
                return null;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static class JsonFillLoop implements Function<int[], Object> {
        protected Map<Integer,List<GuiReprCollectionTable.CellValue>> specifiedRows = new HashMap<>();
        protected List<List<GuiReprCollectionTable.CellValue>> freeRows = new ArrayList<>();

        protected Map<Integer,Integer> rowToFreeIndex = new HashMap<>();

        public boolean addRow(List<GuiReprCollectionTable.CellValue> updatedRow) {
            if (isRowSpecified(updatedRow)) {
                specifiedRows.put(updatedRow.get(0).row, updatedRow);
                return false;
            } else {
                freeRows.add(updatedRow);
                return true;
            }
        }

        private boolean isRowSpecified(List<GuiReprCollectionTable.CellValue> updatedRow) {
            return updatedRow.stream()
                    .anyMatch(CellValueRowSpecified.class::isInstance); //all cells in the returned list has a same row
        }

        @Override
        public Object apply(int[] pos) {
            List<GuiReprCollectionTable.CellValue> row = specifiedRows.get(pos[0]);
            if (row == null) {
                row = freeRows.get(rowToFreeIndex.computeIfAbsent(pos[0],
                        key -> rowToFreeIndex.size() % freeRows.size()));
            }
            return row.stream()
                    .filter(c -> c.column == pos[1])
                    .findFirst()
                    .map(c -> c.value)
                    .orElse(null);
        }
    }

}
