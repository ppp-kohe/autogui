package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueImagePane;
import autogui.swing.util.MenuBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;

public class GuiSwingViewImagePane implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        ImagePropertyPane imagePane = new ImagePropertyPane(context);
        JComponent pane = new JScrollPane(imagePane);
        if (context.isTypeElementProperty()) {
            return new GuiSwingViewPropertyPane.PropertyPane(context, true, pane);
        } else {
            return pane;
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static class ImagePropertyPane extends JComponent implements GuiMappingContext.SourceUpdateListener {
        protected GuiMappingContext context;
        protected Image image;
        protected Dimension imageSize = new Dimension(1, 1);
        protected float maxImageScale = 1f;

        public ImagePropertyPane(GuiMappingContext context) {
            this.context = context;

            context.addSourceUpdateListener(this);
            update(context, context.getSource());
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            GuiReprValueImagePane img = (GuiReprValueImagePane) context.getRepresentation();
            setImage(img.updateValue(context, newValue));
        }

        public void setMaxImageScale(float maxImageScale) {
            this.maxImageScale = maxImageScale;
        }

        public float getMaxImageScale() {
            return maxImageScale;
        }

        public Image getImage() {
            return image;
        }

        public void setImage(Image image) {
            GuiReprValueImagePane img = (GuiReprValueImagePane) context.getRepresentation();
            this.image = image;
            imageSize = img.getSize(context, image);
            setPreferredSize(imageSize);
            revalidate();
            repaint();
        }

        public Dimension getImageSize() {
            return imageSize;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            if (image != null) {
                Dimension paneSize = getSize();
                Dimension size = getScaledImageSize(imageSize, paneSize);
                int left = (paneSize.width - size.width) / 2;
                int top = (paneSize.height - size.height) / 2;


                try {
                    while (!g.drawImage(image, left, top, size.width, size.height, this)) {
                        Thread.sleep(10);
                    }
                } catch (InterruptedException ex) {
                    //nothing
                }
            }
        }

        public Dimension getScaledImageSize(Dimension srcSize, Dimension dstSize) {
            float pw = (dstSize.width / (float) srcSize.width);
            float ph = (dstSize.height / (float) srcSize.height);
            float p = Math.min(pw < ph ? pw : ph, maxImageScale);
            return new Dimension((int) (srcSize.width * p), (int) (srcSize.height * p));
        }
    }

    public static class ImageActionPopupMenu {
        protected JPopupMenu menu;
        protected ImagePropertyPane propertyPane;

        public ImageActionPopupMenu(ImagePropertyPane propertyPane) {
            this.propertyPane = propertyPane;
            menu = new JPopupMenu();
        }

        public void show(Component comp, int x, int y) {
            setupMenu();
            menu.show(comp, x, y);
        }

        public void setupMenu() {
            menu.removeAll();
            menu.add(createSizeInfo(propertyPane.getSize()));
            menu.add(new ImageCopyAction(propertyPane.getImage()));
        }

        public JLabel createSizeInfo(Dimension size) {
            JLabel label = new JLabel();
            label.setText(String.format("Size: %,d x %,d", size.width, size.height));
            return label;
        }
    }

    public static class ImageCopyAction extends AbstractAction {
        protected Image image;

        public ImageCopyAction(Image image) {
            putValue(NAME, "Copy");
            this.image = image;
        }

        @Override
        public boolean isEnabled() {
            return image != null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (image != null) {
                Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
                ImageSelection selection = new ImageSelection(image);
                board.setContents(selection, selection);
            }
        }
    }

    public static class ImageSelection implements Transferable, ClipboardOwner {
        protected Image image;

        protected static DataFlavor[] flavors = {
                DataFlavor.imageFlavor,
        };

        public ImageSelection(Image image) {
            this.image = image;
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
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            if (DataFlavor.imageFlavor.equals(flavor)) {
                return image;
            }
            throw new UnsupportedFlavorException(flavor);
        }
        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            image = null;
        }

    }


}
