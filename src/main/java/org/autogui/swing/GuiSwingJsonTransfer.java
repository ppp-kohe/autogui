package org.autogui.swing;

import org.autogui.base.JsonReader;
import org.autogui.base.JsonWriter;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprCollectionTable;
import org.autogui.base.mapping.GuiReprCollectionTable.CellValue;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.swing.GuiSwingViewCollectionTable.CollectionTable;
import org.autogui.swing.table.ObjectTableColumn;
import org.autogui.swing.table.TableTargetCellAction;
import org.autogui.swing.table.TableTargetColumnAction;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.util.SettingsWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * a class cluster for JSON reading/writing
 */
public class GuiSwingJsonTransfer {

    public static List<JMenuItem> getActionMenuItems(GuiSwingView.ValuePane<?> component, GuiMappingContext context) {
        return getActions(component, context).stream()
                .map(JMenuItem::new)
                .collect(Collectors.toList());

    }

    public static List<Action> getActions(GuiSwingView.ValuePane<?> component, GuiMappingContext context) {
        return Arrays.asList(
                new JsonCopyAction(component, context),
                new JsonPasteAction(component, context),
                new JsonSaveAction(component, context),
                new JsonLoadAction(component, context));
    }

    public static void copy(Object map) {
        if (map != null) {
            String src = JsonWriter.create().write(map).toSource();
            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();

            JsonTransferable data = new JsonTransferable(src);
            board.setContents(data, data);
        }
    }

    public static void save(GuiSwingTaskRunner.ContextAction runner, Supplier<Object> json, JComponent owner, String name) {
        Path path = SettingsWindow.getFileDialogManager().showSaveDialog(
                owner, null,
                name + ".json");
        if (path != null) {
            runner.executeContextTask(json,
                    r -> r.executeIfPresent(
                            j -> SwingUtilities.invokeLater(() -> JsonWriter.write(j, path.toFile()))));
        }
    }


    public static Object readJson() {
        String json = readJsonSource();
        if (json != null) {
            return JsonReader.create(json).parseValue();
        } else {
            return null;
        }
    }

    public static String readJsonSource() {
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

    public static Object load(JComponent component) {
        Path path = SettingsWindow.getFileDialogManager().showOpenDialog(component, null);
        if (path != null) {
            return JsonReader.read(path.toFile());
        } else {
            return null;
        }
    }

    /** an action for copying a JSON value of the target component */
    public static class JsonCopyAction extends GuiSwingTaskRunner.ContextAction implements TableTargetColumnAction,
            PopupCategorized.CategorizedMenuItemAction  {
        private static final long serialVersionUID = 1L;
        protected GuiSwingView.ValuePane<?> component;

        public JsonCopyAction(GuiSwingView.ValuePane<?> component, GuiMappingContext context) {
            super(context);
            this.component = component;
            putValue(NAME, "Copy as JSON");
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_C,
                            PopupExtension.getMenuShortcutKeyMask(), KeyEvent.ALT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object value = component.getSwingViewValue();
            executeContextTask(
                    () -> toCopiedJson(value),
                    r -> r.executeIfPresent(
                            v -> SwingUtilities.invokeLater(() -> copy(v))));
        }


        public Object toCopiedJson(Object value) {
            return getContext().getRepresentation().toJsonWithNamed(getContext(), value);
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            //suppose the map preserves order of values, and skip null element
            List<Object> values = target.getSelectedCellValues();
            executeContextTask(
                    () -> values.stream()
                            .map(this::toCopiedJson)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()),
                    r -> r.executeIfPresent(
                            json -> SwingUtilities.invokeLater(() -> copy(json.size() == 1 ? json.get(0) : json)))); //if singleton, unwrap the list);
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_COPY;
        }
    }

    /** an action for pasting a JSON value of the target component */
    public static class JsonPasteAction extends GuiSwingTaskRunner.ContextAction
        implements PopupCategorized.CategorizedMenuItemAction  {
        private static final long serialVersionUID = 1L;
        protected GuiSwingView.ValuePane<?> component;

        public JsonPasteAction(GuiSwingView.ValuePane<?> component, GuiMappingContext context) {
            super(context);
            this.component = component;
            putValue(NAME, "Paste JSON");
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_V,
                            PopupExtension.getMenuShortcutKeyMask(), KeyEvent.ALT_DOWN_MASK));
        }

        @Override
        public boolean isEnabled() {
            GuiMappingContext context = getContext();
            return !context.isReprValue() || context.getReprValue().isEditable(context);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            set(readJson());
        }

        @SuppressWarnings("unchecked")
        public void set(Object json) {
            executeContextTask(() -> toValueFromJson(json),
                    r -> r.executeIfPresent(
                        ret -> SwingUtilities.invokeLater(() ->
                            ((GuiSwingView.ValuePane<Object>) component).setSwingViewValueWithUpdate(ret))));
        }

        /**
         * check whether the json is a map containing the context-key and selectively calls a fromJson method.
         * @param json json for pasting
         * @return the value updated by the json
         * @since 1.1
         */
        public Object toValueFromJson(Object json) {
            Object v = component.getSwingViewValue();
            GuiMappingContext context = getContext();
            if (context.isTypeElementProperty() &&
                    json instanceof Map<?,?> && ((Map<?,?>) json).containsKey(context.getName())) {
                return context.getRepresentation().fromJsonWithNamed(context, v, json);
            } else {
                return context.getRepresentation().fromJson(context, v, json);
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_PASTE;
        }
    }

    /** an action for saving a JSON value to a file of the target component */
    public static class JsonSaveAction extends JsonCopyAction implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        public JsonSaveAction(GuiSwingView.ValuePane<?> component, GuiMappingContext context) {
            super(component, context);
            putValue(NAME, "Export JSON...");
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_S,
                            PopupExtension.getMenuShortcutKeyMask(), KeyEvent.ALT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object v = component.getSwingViewValue();
            save(this, () -> toCopiedJson(v), component.asSwingViewComponent(),
                    getContext().getName());
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            List<Object> cells = target.getSelectedCellValues();
            save(this, () -> cells.stream()
                    .map(this::toCopiedJson)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()), component.asSwingViewComponent(), getContext().getName());
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_TRANSFER;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_EXPORT;
        }
    }

    /** an action for loading a JSON value to a file of the target component */
    public static class JsonLoadAction extends JsonPasteAction implements PopupCategorized.CategorizedMenuItemAction {
        private static final long serialVersionUID = 1L;
        public JsonLoadAction(GuiSwingView.ValuePane<?> component, GuiMappingContext context) {
            super(component, context);
            putValue(NAME, "Import JSON...");
            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_O,
                            PopupExtension.getMenuShortcutKeyMask(), KeyEvent.ALT_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object json = load(component.asSwingViewComponent());
            if (json != null) {
                set(json);
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_TRANSFER;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_IMPORT;
        }
    }

    /** default representation: InputStream */
    public static DataFlavor jsonFlavor = new DataFlavor("application/json", "JSON text");

    /** JSON transferable supporting string and "application/json" */
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

    /** a table menu composite for copying JSON values of target table-cells */
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
            if (context == null && value instanceof Number) { //for indices
                return new GuiReprValue.NamedValue(getName(), value);
            } else if (context != null) {
                return context.getRepresentation().toJsonWithNamed(context, value);
            } else {
                return null;
            }
        }
    }

    public static TableMenuCompositeSharedJsonCopy shared = new TableMenuCompositeSharedJsonCopy();

    /** a shared key of table menu composite for copying JSON values of target table-cells */
    public static class TableMenuCompositeSharedJsonCopy implements ObjectTableColumn.TableMenuCompositeShared {
        @Override
        public List<PopupCategorized.CategorizedMenuItem> composite(JTable table,
                                                                   List<ObjectTableColumn.TableMenuComposite> columns, boolean row) {
            List<ObjectTableColumn.TableMenuComposite> cs = columns.stream()
                    .filter(this::isNonRowComposite)
                    .collect(Collectors.toList());
            List<PopupCategorized.CategorizedMenuItem> actions = new ArrayList<>();
            if (row) {
                if (table instanceof CollectionTable) {
                    CollectionTable colTable = (CollectionTable) table;
                    actions.add(new JsonCopyRowsAction(colTable.getSwingViewContext()));
                    actions.add(new JsonSaveRowsAction(colTable));
                } else {
                    actions.add(new JsonCopyCellsAction(null, toJsonCopies(cs)));
                    actions.add(new JsonSaveCellsAction(null, toJsonCopies(cs), table));
                }
            } else {
                GuiMappingContext context = null;
                if (table instanceof CollectionTable) {
                    context = ((CollectionTable) table).getSwingViewContext();
                }
                actions.add(new JsonCopyCellsAction(context, toJsonCopies(cs)));
                actions.add(new JsonSaveCellsAction(context, toJsonCopies(cs), table));
            }
            return actions;
        }

        private boolean isNonRowComposite(ObjectTableColumn.TableMenuComposite c) {
            if (c instanceof TableMenuCompositeJsonCopy) {
                return ((TableMenuCompositeJsonCopy) c).getIndex() != -1;
            } else {
                return false;
            }
        }

        private List<TableMenuCompositeJsonCopy> toJsonCopies(List<ObjectTableColumn.TableMenuComposite> cols) {
            return cols.stream()
                    .filter(TableMenuCompositeJsonCopy.class::isInstance)
                    .map(TableMenuCompositeJsonCopy.class::cast)
                    .collect(Collectors.toList());
        }
    }

    /** an action for copying JSON values of target table-rows:
     *    each JSON values are converted from selected-rows by the context of the table */
    public static class JsonCopyRowsAction extends GuiSwingTaskRunner.ContextAction implements TableTargetCellAction {
        private static final long serialVersionUID = 1L;

        public JsonCopyRowsAction(GuiMappingContext context) {
            super(context);
            putValue(NAME, "Copy Rows as JSON");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            List<Object> values = target.getSelectedRowValues();
            executeContextTask(
                    () -> getJson(values),
                    r -> r.executeIfPresent(
                            json -> SwingUtilities.invokeLater(() -> copy(json))));
        }

        public Object getJson(List<Object> values) {
            return getContext().getRepresentation()
                    .toJson(getContext(), values);
        }

        @Override
        public String getCategory() {
            return MENU_CATEGORY_ROW;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_COPY;
        }
    }

    /** an action for saving JSON values of target table-rows to a file  */
    public static class JsonSaveRowsAction extends JsonCopyRowsAction {
        private static final long serialVersionUID = 1L;
        protected GuiSwingView.ValuePane<?> table;
        public JsonSaveRowsAction(GuiSwingView.ValuePane<?> table) {
            super(table.getSwingViewContext());
            this.table = table;
            putValue(NAME, "Export Rows as JSON...");
        }

        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            List<Object> values = target.getSelectedRowValues();
            save(this, () -> getJson(values), table.asSwingViewComponent(), table.getSwingViewContext().getName());
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_EXPORT;
        }
    }

    /** an action for copying JSON values of target table-cells:
     *    each JSON values are converted from each table-column contained as a list of {@link TableMenuCompositeJsonCopy}
     *       and selected based on the column-index  */
    public static class JsonCopyCellsAction extends GuiSwingTaskRunner.ContextAction implements TableTargetCellAction {
        private static final long serialVersionUID = 1L;
        protected List<TableMenuCompositeJsonCopy> activatedColumns;

        public JsonCopyCellsAction(GuiMappingContext contextOptional, List<TableMenuCompositeJsonCopy> activatedColumns) {
            super(contextOptional);
            putValue(NAME, "Copy Cells as JSON");
            putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_C,
                    PopupExtension.getMenuShortcutKeyMask(), KeyEvent.ALT_DOWN_MASK));
            this.activatedColumns = activatedColumns;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            List<CellValue> cells = target.getSelectedCells();
            executeContextTask(
                    () -> getJson(cells),
                    r -> r.executeIfPresent(
                            json -> SwingUtilities.invokeLater(() -> copy(json))));
        }

        /**
         * @param cells the selected cells : the method suppose the cells are sorted by row-index
         * @return a JSON list
         */
        public List<Object> getJson(List<CellValue> cells) {
            int prevLine = -1;
            List<Object> lines = new ArrayList<>();
            Map<String,Object> cols = new LinkedHashMap<>(activatedColumns.size());

            for (CellValue cell : cells) {
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
            return lines;
        }

        public TableMenuCompositeJsonCopy getMenuCompositeForCell(CellValue cell) {
            return activatedColumns.stream()
                    .filter(cmp -> cmp.getIndex() == cell.getColumn())
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String getCategory() {
            return MENU_CATEGORY_CELL;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_COPY;
        }
    }

    /** an action for saving JSON values of target table-cells */
    public static class JsonSaveCellsAction extends JsonCopyCellsAction {
        private static final long serialVersionUID = 1L;
        JComponent table;
        public JsonSaveCellsAction(GuiMappingContext contextOptional, List<TableMenuCompositeJsonCopy> activatedColumns, JComponent table) {
            super(contextOptional, activatedColumns);
            putValue(NAME, "Export Cells as JSON...");
            putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_S,
                    PopupExtension.getMenuShortcutKeyMask(), KeyEvent.ALT_DOWN_MASK));
            this.table = table;
        }

        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            String name = "selection";
            if (table instanceof GuiSwingView.ValuePane<?>) {
                name = ((GuiSwingView.ValuePane) table).getSwingViewContext().getName();
            }
            List<CellValue> cells = target.getSelectedCells();
            save(this, () -> getJson(cells), table, name);
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_EXPORT;
        }
    }

    ////////////////////////////

    /** a table menu composite for pasting JSON values of target table-cells */
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

    /** a shared key of table menu composite for pasting JSON values of target table-cells */
    public static class TableMenuCompositeSharedJsonPaste implements ObjectTableColumn.TableMenuCompositeShared {
        @Override
        public List<PopupCategorized.CategorizedMenuItem> composite(JTable table,
                                                                   List<ObjectTableColumn.TableMenuComposite> columns, boolean row) {
            GuiMappingContext context = null;
            if (table instanceof CollectionTable) {
                context = ((CollectionTable) table).getSwingViewContext();
            }
            List<ObjectTableColumn.TableMenuComposite> cs = columns.stream()
                    .filter(this::isNonRowComposite)
                    .collect(Collectors.toList());
            return Arrays.asList(
                    new JsonPasteCellsAction(context, toJsonPaste(cs), row),
                    new JsonLoadCellsAction(context, toJsonPaste(cs), row, table));
        }

        private boolean isNonRowComposite(ObjectTableColumn.TableMenuComposite c) {
            if (c instanceof TableMenuCompositeJsonPaste) {
                return ((TableMenuCompositeJsonPaste) c).getIndex() != -1;
            } else {
                return false;
            }
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
    public static class CellValueRowSpecified extends CellValue {
        public CellValueRowSpecified(int row, int column, Object value) {
            super(row, column, value);
        }

        @Override
        public CellValueRowSpecified withValue(Object v) {
            return new CellValueRowSpecified(row, column, value);
        }
    }

    /** an action for pasting JSON values of target table-rows or cells */
    public static class JsonPasteCellsAction extends GuiSwingTaskRunner.ContextAction implements TableTargetCellAction {
        private static final long serialVersionUID = 1L;
        protected List<TableMenuCompositeJsonPaste> activeComposite;
        protected boolean rows;

        public JsonPasteCellsAction(GuiMappingContext contextOptional, List<TableMenuCompositeJsonPaste> activeComposite, boolean allCells) {
            super(contextOptional);
            putValue(NAME, allCells
                    ? "Paste JSON to Rows"
                    : "Paste JSON to Cells");
            if (!allCells) {
                putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_V,
                        PopupExtension.getMenuShortcutKeyMask(), KeyEvent.ALT_DOWN_MASK));
            }
            rows = allCells;
            this.activeComposite = activeComposite;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }

        public Object getJson() {
            return readJson();
        }

        /**
         * it separates entries to 2 types: free-rows and specified-rows
         * <pre>
         *   [{ "#":0, "value":"a" },  { "#":1, "value":"b" }, { "#":3, "value":"c"} ] // specifiedRows
         *   [{ "value":"d" }, { "value":"e"}, {"value":"f"} ] // freeRows
         *
         * selected cells: //suppose the user selects row0 and row2-6
         *    row0 &lt;- specifiedRows[0]
         *    //omit row1: specifiedRows[1] is discarded
         *    row2 &lt;- freeRows[0]
         *    row3 &lt;- specifiedRows[2]
         *    row4 &lt;- freeRows[1]
         *    row5 &lt;- freeRows[2]
         *    row6 &lt;- freeRows[0] //loop
         *    ...
         * </pre>
         * @param e an action event
         * @param target the selection targert
         */
        @Override
        public void actionPerformedOnTableCell(ActionEvent e, GuiReprCollectionTable.TableTargetCell target) {
            Object json = getJson();
            if (json != null) {
                Iterable<int[]> is = target.getSelectedCellIndices();
                executeContextTask(() -> {
                            JsonFillLoop fillLoop = new JsonFillLoop();
                            if (json instanceof List<?>) { //[ ... ]
                                int rowIndex = 0;
                                for (Object o : (List<?>) json) {
                                    List<CellValue> updatedRow = getCellsForRow(rowIndex, o);
                                    if (fillLoop.addRow(updatedRow)) {
                                        ++rowIndex;
                                    }
                                }
                            } else {
                                List<CellValue> updatedRow = getCellsForRow(0, json); //single row
                                fillLoop.addRow(updatedRow);
                            }
                            return fillLoop;
                        },
                        r -> r.executeIfPresent(
                                fl -> SwingUtilities.invokeLater(() -> target.setCellValues(is, fl))));
            }
        }

        private boolean isRowSpecified(List<CellValue> updatedRow) {
            return updatedRow.stream()
                    .anyMatch(CellValueRowSpecified.class::isInstance); //all cells in the returned list has a same row
        }

        public List<CellValue> getCellsForRow(int rowIndex, Object rowJson) {
            int targetRow = rowIndex;
            boolean rowSpecified = false;
            List<CellValue> updatedRow = new ArrayList<>();

            if (rowJson instanceof List<?>) { // [ [...], [...], ...]
                List<?> row = (List<?>) rowJson;
                for (int i = 0, l = row.size(); i < l; ++i) {
                    TableMenuCompositeJsonPaste column = activeComposite.get(i % activeComposite.size());
                    if (column.isIndexColumn()) {
                        //specify the row index
                        targetRow = (Integer) column.fromJson(row.get(i));
                        rowSpecified = true;
                    } else {
                        updatedRow.add(new CellValue(targetRow, column.getIndex(),
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
                            updatedRow.add(new CellValue(targetRow, column.getIndex(),
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
                    updatedRow.add(new CellValue(targetRow, uniqueColumn.getIndex(),
                            uniqueColumn.fromJson(rowJson)));
                }
            }

            if (targetRow != rowIndex) {
                for (CellValue c : updatedRow) {
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

        @Override
        public String getCategory() {
            return rows ? MENU_CATEGORY_ROW : MENU_CATEGORY_CELL;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_PASTE;
        }
    }

    /** a table value selector for specified row-and-column indices.
     *    values are selected from  specifiedRows and freeRows */
    public static class JsonFillLoop implements Function<int[], Object> {
        protected Map<Integer,List<CellValue>> specifiedRows = new HashMap<>();
        protected List<List<CellValue>> freeRows = new ArrayList<>();

        protected Map<Integer,Integer> rowToFreeIndex = new HashMap<>();

        public boolean addRow(List<CellValue> updatedRow) {
            if (isRowSpecified(updatedRow)) {
                specifiedRows.put(updatedRow.get(0).row, updatedRow);
                return false;
            } else {
                freeRows.add(updatedRow);
                return true;
            }
        }

        private boolean isRowSpecified(List<CellValue> updatedRow) {
            return updatedRow.stream()
                    .anyMatch(CellValueRowSpecified.class::isInstance); //all cells in the returned list has a same row
        }

        @Override
        public Object apply(int[] pos) {
            List<CellValue> row = specifiedRows.get(pos[0]);

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

    /** an action for loading JSON values from a file for target table-rows or cells */
    public static class JsonLoadCellsAction extends JsonPasteCellsAction {
        private static final long serialVersionUID = 1L;
        protected JComponent table;
        public JsonLoadCellsAction(GuiMappingContext contextOptional, List<TableMenuCompositeJsonPaste> activeComposite, boolean allCells, JComponent table) {
            super(contextOptional, activeComposite, allCells);
            this.table = table;
            putValue(NAME, allCells
                    ? "Import JSON to Rows..."
                    : "Import JSON to Cells...");
            if (!allCells) {
                putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_O,
                        PopupExtension.getMenuShortcutKeyMask(), KeyEvent.ALT_DOWN_MASK));
            }
        }

        @Override
        public Object getJson() {
            return load(table);
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_IMPORT;
        }
    }
}
