package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingView;
import autogui.swing.GuiSwingViewImagePane;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Collections;
import java.util.List;

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
        ColumnEditImagePane img = new ColumnEditImagePane(context, valueSpecifier, false);
        ColumnEditImagePane edit = new ColumnEditImagePane(context, valueSpecifier, true);
        edit.setScaleTarget(img);
        img.setScaleTarget(edit);
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier, img, edit) {
            @Override
            protected TableCellEditor editorForColumn() {
                return editor; //always apply the editor
            }
        }.withRowHeight(UIManagerUtil.getInstance().getScaledSizeInt(64));
    }

    /**
     * a component for column editor
     */
    public static class ColumnEditImagePane extends GuiSwingViewImagePane.PropertyImagePane
            implements ObjectTableColumnValue.ColumnViewUpdateSource, ObjectTableColumnValue.ColumnViewUpdateTarget {
        protected ColumnEditImagePane scaleTarget;
        protected int updating;
        protected boolean editor;
        protected Runnable viewUpdater;

        public ColumnEditImagePane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager, boolean editor) {
            super(context, specifierManager);
            this.editor = editor;
            setOpaque(true);
            int size = UIManagerUtil.getInstance().getScaledSizeInt(64);
            setPreferredSize(new Dimension(size, size));
            setCurrentValueSupported(false);
        }

        public void setScaleTarget(ColumnEditImagePane scaleTarget) {
            this.scaleTarget = scaleTarget;
        }

        @Override
        public void setColumnViewUpdater(Runnable updater) {
            this.viewUpdater = updater;
        }

        @Override
        public void columnViewUpdateAsDynamic(ObjectTableColumn source) {
            TableCellRenderer renderer = source.getTableColumn().getCellRenderer();
            if (renderer instanceof ObjectTableColumnValue.ObjectTableCellRenderer) {
                JComponent comp = ((ObjectTableColumnValue.ObjectTableCellRenderer) renderer).getComponent();
                if (comp instanceof GuiSwingViewImagePane.PropertyImagePane) {
                    GuiSwingViewImagePane.PropertyImagePane pane = (GuiSwingViewImagePane.PropertyImagePane) comp;
                    setImageScale(pane.getImageScale());
                }
            }
        }

        @Override
        public void updateScale() {
            super.updateScale();
            if (viewUpdater != null) {
                viewUpdater.run();
            }
            if (updating <= 0) {
                if (scaleTarget != null) {
                    scaleTarget.setImageScaleFromOpponent(imageScale.copyFor(scaleTarget));
                }
            }
        }

        public void setImageScaleFromOpponent(GuiSwingViewImagePane.ImageScale imageScale) {
            try {
                ++updating;
                setImageScale(imageScale);
            } finally {
                --updating;
            }
        }

        @Override
        public void setPreferredSizeFromImageSize() {
            int size = UIManagerUtil.getInstance().getScaledSizeInt(64);
            setPreferredSize(new Dimension(size, size));
        }


        public List<PopupCategorized.CategorizedMenuItem> getDynamicMenuItems() {
            if (editor) {
                return super.getDynamicMenuItems();
            } else {
                return PopupCategorized.getMenuItems(
                        Collections.singletonList(createScaleInfo()));
            }
        }


        public JComponent createScaleInfo() {
            return MenuBuilder.get().createLabel(String.format("Scale: %s",
                    imageScale == null ? "null" : imageScale.getInfo()),
                    PopupCategorized.SUB_CATEGORY_LABEL_VALUE);
        }
    }
}
