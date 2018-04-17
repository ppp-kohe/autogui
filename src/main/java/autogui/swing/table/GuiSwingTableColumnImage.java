package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewImagePane;

import java.awt.*;

/**
 * a column factory for {@link Image}.
 *
 * <p>
 *     both editor and renderer are realized by a sub-class of
 *     {@link autogui.swing.GuiSwingViewImagePane.PropertyImagePane}.
 */
public class GuiSwingTableColumnImage extends GUiSwingTableColumnStatic implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumnWithIndex(GuiMappingContext context, ObjectColumnIndex index) {
        ColumnEditImagePane img = new ColumnEditImagePane(context);
        ColumnEditImagePane edit = new ColumnEditImagePane(context);
        return new ObjectTableColumnValue(context, img, edit).withRowHeight(64);
    }

    /**
     * a component for column editor
     */
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
