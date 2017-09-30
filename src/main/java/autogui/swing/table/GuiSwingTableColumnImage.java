package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewImagePane;

import java.awt.*;

public class GuiSwingTableColumnImage implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {
        ColumnEditImagePane img = new ColumnEditImagePane(context);
        ColumnEditImagePane edit = new ColumnEditImagePane(context);
        return new ObjectTableColumnValue(context, img, edit).withRowHeight(64);
    }

    public static class ColumnEditImagePane extends GuiSwingViewImagePane.PropertyImagePane {
        public ColumnEditImagePane(GuiMappingContext context) {
            super(context);
            setOpaque(true);
            setPreferredSize(new Dimension(64, 64));
        }

        @Override
        public void setPreferredSizeFromImageSize() {
            setPreferredSize(new Dimension(64, 64));
        }
    }
}
