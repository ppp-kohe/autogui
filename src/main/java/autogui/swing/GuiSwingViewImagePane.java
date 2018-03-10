package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.swing.mapping.GuiReprValueImagePane;
import autogui.swing.table.TableTargetColumn;
import autogui.swing.table.TableTargetColumnAction;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupExtension;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class GuiSwingViewImagePane implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyImagePane imagePane = new PropertyImagePane(context);
        ValuePane pane = new GuiSwingView.ValueScrollPane(imagePane);
        if (context.isTypeElementProperty()) {
            return pane.wrapProperty();
        } else {
            return pane.asComponent();
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static class PropertyImagePane extends JComponent
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Image> {
        protected GuiMappingContext context;
        protected Image image;
        protected Dimension imageSize = new Dimension(1, 1);
        protected float maxImageScale = 1f;
        protected boolean editable;

        protected PopupExtension popup;

        public PropertyImagePane(GuiMappingContext context) {
            this.context = context;

            //editable
            setEditable(((GuiReprValueImagePane) context.getRepresentation())
                    .isEditable(context));

            //context update
            context.addSourceUpdateListener(this);
            //initial update
            update(context, context.getSource());

            //popup
            JComponent label = GuiSwingContextInfo.get().getInfoLabel(context);
            ContextRefreshAction refreshAction = new ContextRefreshAction(context);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.accept(label);
                menu.accept(refreshAction);
                menu.accept(createSizeInfo(getImageSize()));
                menu.accept(new ImageCopyAction(getImage()));
                menu.accept(new ImagePasteAction(this));
                GuiSwingJsonTransfer.getActions(this, context)
                        .forEach(menu::accept);
                menu.accept(new HistoryMenu<>(this, context));
            });
            setInheritsPopupMenu(true);

            //drag drop
            setTransferHandler(new ImageTransferHandler(this));
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, e -> {
                getTransferHandler().exportAsDrag(this, e.getTriggerEvent(), TransferHandler.COPY);
            });
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }

        public JComponent createSizeInfo(Dimension size) {
            return MenuBuilder.get().createLabel(String.format("Size: %,d x %,d", size.width, size.height));
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        public boolean isEditable() {
            return editable;
        }

        public void setEditable(boolean editable) {
            this.editable = editable;
        }

        @Override
        public void update(GuiMappingContext cause, Object newValue) {
            SwingUtilities.invokeLater(() -> setSwingViewValue((Image) newValue));
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
            setPreferredSizeFromImageSize();
            revalidate();
            repaint();
        }

        public void setPreferredSizeFromImageSize() {
            setPreferredSize(imageSize);
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

        @Override
        public Image getSwingViewValue() {
            return getImage();
        }

        @Override
        public void setSwingViewValue(Image value) {
            GuiReprValueImagePane img = (GuiReprValueImagePane) context.getRepresentation();
            setImageWithoutContextUpdate(img.updateValue(context, value));
        }

        @Override
        public void setSwingViewValueWithUpdate(Image value) {
            setImage(value);
        }
    }

    public static class ImageCopyAction extends AbstractAction implements TableTargetColumnAction {
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
            copy(this.image);
        }

        public void copy(Image image) {
            if (image != null) {
                Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
                ImageSelection selection = new ImageSelection(image);
                board.setContents(selection, selection);
            }
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, TableTargetColumn target) {
            Object o = target.getSelectedCellValue();
            if (o instanceof Image) {
                copy((Image) o);
            }
        }
    }

    public static class ImagePasteAction extends AbstractAction implements TableTargetColumnAction {
        protected PropertyImagePane pane;

        public ImagePasteAction(PropertyImagePane pane) {
            putValue(NAME, "Paste");
            this.pane = pane;
        }

        @Override
        public boolean isEnabled() {
            return pane.isEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            paste(pane::setImage);
        }

        public void paste(Consumer<Image> c) {
            Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
            try {
                Image img = (Image) clip.getData(DataFlavor.imageFlavor);
                c.accept(img);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, TableTargetColumn target) {
            paste(img -> target.setSelectedCellValues(r -> img));
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

        public Image getImage() {
            return image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            System.err.println("isDataFlavorSupported: " + flavor);
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
        protected PropertyImagePane imagePane;

        public ImageTransferHandler(PropertyImagePane imagePane) {
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
            Image img = imagePane.getImage();
            Dimension size = imagePane.getScaledImageSize(imagePane.getImageSize(), new Dimension(100, 100));
            setDragImage(img.getScaledInstance(size.width , size.height, Image.SCALE_DEFAULT));
            return new ImageSelection(img);
        }

    }
}
