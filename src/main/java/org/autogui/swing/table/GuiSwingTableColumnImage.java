package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.GuiSwingView.SpecifierManager;
import org.autogui.swing.GuiSwingView.SpecifierManagerDefault;
import org.autogui.swing.GuiSwingViewImagePane;
import org.autogui.swing.GuiSwingViewImagePane.PropertyImagePane;
import org.autogui.swing.mapping.GuiReprValueImagePane;
import org.autogui.swing.table.ObjectTableColumnValue.ObjectTableCellRenderer;
import org.autogui.swing.util.MenuBuilder;
import org.autogui.swing.util.PopupCategorized;
import org.autogui.swing.util.PopupCategorized.CategorizedMenuItem;
import org.autogui.swing.util.TextCellRenderer;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.Serial;
import java.util.List;
import java.util.*;

/**
 * a column factory for {@link Image}.
 *
 * <p>
 *     both editor and renderer are realized by a subclass of
 *     {@link PropertyImagePane}.
 */
public class GuiSwingTableColumnImage implements GuiSwingTableColumn {
    public GuiSwingTableColumnImage() {}
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          SpecifierManager parentSpecifier) {
        SpecifierManager valueSpecifier = new SpecifierManagerDefault(parentSpecifier::getSpecifier);
        ColumnEditImagePane img = new ColumnEditImagePane(context, valueSpecifier, false);
        ColumnEditImagePane edit = new ColumnEditImagePane(context, valueSpecifier, true);
        edit.setScaleTarget(img);
        img.setScaleTarget(edit);
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier, img, edit)
                .withBorderType(ObjectTableColumnValue.CellBorderType.Regular)
                .withEditorForColumnAlwaysApplying(true)
                .withRowHeight(img.getPreferredSize().height)
                .withComparator(Comparator.comparing(Objects::hash));
    }

    /**
     * the default value of each row-height of image-column
     * @since 1.1
     */
    public static float defaultCellSize = 64;

    /**
     * a component for column editor
     */
    public static class ColumnEditImagePane extends PropertyImagePane
            implements ObjectTableColumnValue.ColumnViewUpdateSource, ObjectTableColumnValue.ColumnViewUpdateTarget {
        @Serial private static final long serialVersionUID = 1L;

        protected ColumnEditImagePane scaleTarget;
        protected int updating;
        protected boolean editor;
        protected Runnable viewUpdater;

        /** @since 1.6 */
        protected List<Runnable> finishRunners = new ArrayList<>(1);

        @SuppressWarnings("this-escape")
        public ColumnEditImagePane(GuiMappingContext context, SpecifierManager specifierManager, boolean editor) {
            super(context, specifierManager);
            this.editor = editor;
            TextCellRenderer.setCellDefaultProperties(this);
            setPreferredSizeFromImageSize(); //fixed size
            setCurrentValueSupported(false);
            if (editor) {
                ObjectTableColumnValue.KeyHandlerFinishEditing.installFinishEditingKeyHandler(this, finishRunners);
            }
        }

        @Override
        public void setImageWithoutContextUpdate(Image image) {
            GuiReprValueImagePane img = (GuiReprValueImagePane) context.getRepresentation();
            this.image = image;
            imageSize = img.getSize(context, image);
            setPreferredSizeFromImageSize();
            revalidate();
            repaint();
        }

        @Override
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            finishRunners.add(eventHandler);
            super.addSwingEditFinishHandler(eventHandler);
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
            if (renderer instanceof ObjectTableCellRenderer) {
                JComponent comp = ((ObjectTableCellRenderer) renderer).getComponent();
                if (comp instanceof PropertyImagePane pane) {
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
            int size = (int) UIManagerUtil.getInstance().getScaledSizeFloat(defaultCellSize);
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        public Dimension getMinimumSize() {
            Insets insets = getInsets();
            Dimension size = getImageScaledSize();
            return new Dimension(size.width + insets.left + insets.right, size.height + insets.top + insets.bottom);
        }

        public List<CategorizedMenuItem> getDynamicMenuItems() {
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

        @Override
        public void updateFromGui(Object v, GuiTaskClock viewClock) {
            //nothing
        }
    }
}
