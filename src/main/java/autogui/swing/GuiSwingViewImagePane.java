package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueImagePane;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
        protected boolean editable;
        protected ImageActionPopupMenu popupMenu;

        public ImagePropertyPane(GuiMappingContext context) {
            this.context = context;

            setEditable(((GuiReprValueImagePane) context.getRepresentation())
                    .isEditable(context));

            context.addSourceUpdateListener(this);
            update(context, context.getSource());

            popupMenu = new ImageActionPopupMenu(this);
            addMouseListener(popupMenu.createMouseHandler());

            setTransferHandler(new ImageTransferHandler(this));
        }

        public boolean isEditable() {
            return editable;
        }

        public void setEditable(boolean editable) {
            this.editable = editable;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            GuiReprValueImagePane img = (GuiReprValueImagePane) context.getRepresentation();
            SwingUtilities.invokeLater(() -> setImageWithoutContextUpdate(img.updateValue(context, newValue)));
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

        public void setImageWithoutContextUpdate(Image image) {
            GuiReprValueImagePane img = (GuiReprValueImagePane) context.getRepresentation();
            this.image = image;
            imageSize = img.getSize(context, image);
            setPreferredSize(imageSize);
            revalidate();
            repaint();
        }

        public void setImage(Image image) {
            setImageWithoutContextUpdate(image);
            GuiReprValueImagePane img = (GuiReprValueImagePane) context.getRepresentation();
            img.updateFromGui(context, image);
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
            menu.add(createSizeInfo(propertyPane.getImageSize()));
            menu.add(new ImageCopyAction(propertyPane.getImage()));
            menu.add(new ImagePasteAction(propertyPane));
        }

        public JLabel createSizeInfo(Dimension size) {
            JLabel label = new JLabel();
            label.setText(String.format("Size: %,d x %,d", size.width, size.height));
            return label;
        }

        public MouseAdapter createMouseHandler() {
            return new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handle(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handle(e);
                }

                public void handle(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        show(propertyPane, e.getX(), e.getY());
                    }
                }
            };
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

    public static class ImagePasteAction extends AbstractAction {
        protected ImagePropertyPane pane;

        public ImagePasteAction(ImagePropertyPane pane) {
            putValue(NAME, "Paste");
            this.pane = pane;
        }

        @Override
        public boolean isEnabled() {
            return pane.isEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
            try {
                Image img = (Image) clip.getData(DataFlavor.imageFlavor);
                pane.setImage(img);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
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


    public static class ImageTransferHandler extends TransferHandler {
        protected ImagePropertyPane imagePane;

        public ImageTransferHandler(ImagePropertyPane imagePane) {
            this.imagePane = imagePane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return imagePane.isEditable() &&
                    (support.isDataFlavorSupported(DataFlavor.imageFlavor) ||
                        support.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                return select(getTransferableAsImage(support, DataFlavor.imageFlavor));
            } else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return select(loadTransferableFilesAsImage(support, DataFlavor.javaFileListFlavor));
            } else {
                return false;
            }
        }

        public Image getTransferableAsImage(TransferSupport support, DataFlavor flavor) {
            try {
                return (Image) support.getTransferable().getTransferData(flavor);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @SuppressWarnings("unchecked")
        public Image loadTransferableFilesAsImage(TransferSupport support, DataFlavor flavor) {
            try {
                List<File> fs = (List<File>) support.getTransferable().getTransferData(flavor);
                if (fs != null && !fs.isEmpty()) {
                    try {
                        return ImageIO.read(fs.get(0));
                    } catch (Exception ex) {
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public boolean select(Image image) {
            if (image != null) {
                imagePane.setImage(image);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public int getSourceActions(JComponent c) {
            if (c.equals(imagePane)) {
                return COPY;
            }
            return super.getSourceActions(c);
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new ImageSelection(imagePane.getImage());
        }


    }
}
