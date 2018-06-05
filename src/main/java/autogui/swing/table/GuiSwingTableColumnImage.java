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
        GuiSwingView.SpecifierManager valueSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);
        ColumnEditImagePane img = new ColumnEditImagePane(context, valueSpecifier);
        ColumnEditImagePane edit = new ColumnEditImagePane(context, valueSpecifier);
        edit.setScaleTarget(img); //TODO bidirectional
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier, img, edit).withRowHeight(64);
    }

    /**
     * a component for column editor
     */
    public static class ColumnEditImagePane extends GuiSwingViewImagePane.PropertyImagePane {
        protected GuiSwingViewImagePane.PropertyImagePane scaleTarget;
        public ColumnEditImagePane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager) {
            super(context, specifierManager);
            setOpaque(true);
            setPreferredSize(new Dimension(64, 64));
        }

        public void setScaleTarget(GuiSwingViewImagePane.PropertyImagePane scaleTarget) {
            this.scaleTarget = scaleTarget;
        }

        @Override
        public void setImageScale(GuiSwingViewImagePane.ImageScale imageScale) {
            super.setImageScale(imageScale);
            if (scaleTarget != null) {
                scaleTarget.setImageScale(imageScale); //TODO after the scale is changed, it needs to redisplay entire row
            }
        }

        @Override
        public void setPreferredSizeFromImageSize() {
            setPreferredSize(new Dimension(64, 64));
        }
    }
}
