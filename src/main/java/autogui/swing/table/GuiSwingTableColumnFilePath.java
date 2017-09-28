package autogui.swing.table;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.GuiSwingViewFilePathField;
import autogui.swing.util.PopupCategorized;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Comparator;

public class GuiSwingTableColumnFilePath implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context) {

        return new ObjectTableColumnValue(context,
                new ColumnEditFilePath(context, false),
                new ColumnEditFilePath(context, true))
                .withComparator(Comparator.comparing(Path.class::cast));
    }

    public static class ColumnEditFilePath extends GuiSwingViewFilePathField.PropertyFilePathPane {
        protected boolean editor;
        public ColumnEditFilePath(GuiMappingContext context, boolean editor) {
            super(context);
            this.editor = editor;
        }

        //TODO
        @Override
        public void setIconFromSearchedItem(PopupCategorized.CategorizedPopupItem item) {
            BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = icon.createGraphics();
            g.setColor(new Color(0, 0,0 ,0));
            g.fillRect(0, 0, 16, 16);
            RoundRectangle2D.Float rr = new RoundRectangle2D.Float(3, 3, 10, 10, 4, 4);
            g.setColor(Color.blue);
            g.fill(rr);
            g.dispose();
            this.icon.setIcon(new ImageIcon(icon));
        }

        @Override
        public void initLayout() {
            initBackgroundPainter();
            setLayout(new BorderLayout());
            add(icon, BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
            setOpaque(true);
            getField().setOpaque(true);
            getIcon().setOpaque(true);
            if (!editor) {
                getField().setBorder(BorderFactory.createEmptyBorder());
                setBorder(BorderFactory.createEmptyBorder());
            }
        }

        @Override
        public void initBackgroundPainter() {
            backgroundPainter = new SearchBackgroundPainter();
        }

        @Override
        public void setBackground(Color bg) {
            super.setBackground(bg);
            icon.setBackground(bg);
            field.setBackground(bg);
        }

        @Override
        public void setForeground(Color fg) {
            super.setForeground(fg);
            icon.setForeground(fg);
            field.setForeground(fg);
        }
    }
}
