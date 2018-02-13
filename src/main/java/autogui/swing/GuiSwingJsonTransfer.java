package autogui.swing;

import autogui.base.JsonWriter;
import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.table.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
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
        public void actionPerformedOnTableColumn(ActionEvent e, TableTargetColumn target) {
            Map<Integer,Object> map = target.getSelectedCellValues();
            //suppose the map preserves order of values, and skip null element
            copy(map.values().stream()
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
        public void actionPerformedOnTableCell(ActionEvent e, TableTargetCell target) {
            if (target.getTable() instanceof GuiSwingViewCollectionTable.CollectionTable) {
                GuiMappingContext context = ((GuiSwingViewCollectionTable.CollectionTable) target.getTable()).getContext();

                copy(context.getRepresentation()
                        .toJson(context,target.getSelectedRowValues()));
            }
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
        public void actionPerformedOnTableCell(ActionEvent e, TableTargetCell target) {
            int prevLine = -1;
            List<Object> lines = new ArrayList<>();
            Map<String,Object> cols = new LinkedHashMap<>(activatedColumns.size());

            for (TableTargetCell.CellValue cell : target.getSelectedCells()) {
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

        public TableMenuCompositeJsonCopy getMenuCompositeForCell(TableTargetCell.CellValue cell) {
            return activatedColumns.stream()
                    .filter(cmp -> cmp.getIndex() == cell.getColumn())
                    .findFirst()
                    .orElse(null);
        }
    }
}
