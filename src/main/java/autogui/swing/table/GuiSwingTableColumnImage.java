package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewImagePane;

import java.awt.*;

/**
 * a column factory for {@link Image}.
 *
 * <p>
 *     both editor and renderer are realized by a sub-class of
 *     {@link autogui.swing.GuiSwingViewImagePane.PropertyImagePane}.
 */
public class GuiSwingTableColumnImage implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          GuiSwingView.SpecifierManager parentSpecifier) {
        ColumnEditImagePane img = new ColumnEditImagePane(context, parentSpecifier);
        ColumnEditImagePane edit = new ColumnEditImagePane(context, parentSpecifier);
        return new ObjectTableColumnValue(context, rowSpecifier, parentSpecifier, img, edit).withRowHeight(64);
    }

    /**
     * a component for column editor
     */
    public static class ColumnEditImagePane extends GuiSwingViewImagePane.PropertyImagePane {
        public ColumnEditImagePane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager) {
            super(context, specifierManager);
            setOpaque(true);
            setPreferredSize(new Dimension(64, 64));
        }

        @Override
        public void setPreferredSizeFromImageSize() {
            setPreferredSize(new Dimension(64, 64));
        }
    }
}
