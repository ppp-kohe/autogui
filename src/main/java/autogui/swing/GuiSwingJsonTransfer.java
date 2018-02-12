package autogui.swing;

import autogui.base.JsonWriter;
import autogui.base.mapping.GuiMappingContext;
import autogui.swing.table.TableTargetColumn;
import autogui.swing.table.TableTargetColumnAction;

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

        public void copy(Object map) {
            if (map != null) {
                String src = JsonWriter.create().write(map).toSource();
                Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();

                JsonTransferable data = new JsonTransferable(src);
                board.setContents(data, data);
            }
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
}
