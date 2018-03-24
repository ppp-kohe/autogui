package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.swing.mapping.GuiReprValueImagePane;
import autogui.swing.table.TableTargetColumnAction;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupExtension;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * <h3>representation</h3>
 * {@link GuiReprValueImagePane}
 *
 * <h3>{@link PropertyImagePane#getSwingViewValue()}</h3>
 * latest set image: {@link Image}
 *
 * <p>
 *   updating is caused by {@link PropertyImagePane#setImage(Image)}
 *
 * <h3>history-value</h3>
 * supported.
 *
 * <h3>string-transfer</h3>
 * unsupported.
 */
public class GuiSwingViewImagePane implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context) {
        PropertyImagePane imagePane = new PropertyImagePane(context);
        ValuePane<Image> pane = new GuiSwingView.ValueScrollPane<>(imagePane);
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
        protected boolean editable;

        protected ImageScale imageScale;
        protected ImageScaleFit imageScaleDefault;
        protected ImageScaleFit imageScaleFit;
        protected ImageScaleMouseWheel imageScaleMouseWheel;

        protected boolean imageScaleAutoSwitchByMouseWheel = true;

        protected PopupExtension popup;

        public PropertyImagePane(GuiMappingContext context) {
            this.context = context;
            init();
        }

        protected void init() {
            initScale();
            initEditable();
            initPopup();
            initDragAndDrop();
            initFocus();
            initContext();
        }

        protected void initScale() {
            imageScaleDefault = new ImageScaleFit(1f);
            setImageScale(imageScaleDefault);

            imageScaleFit = new ImageScaleFit(100f);

            imageScaleMouseWheel = new ImageScaleMouseWheel(this);
            addMouseWheelListener(e -> {
                boolean activate = imageScaleMouseWheelShouldBeActivated(e);
                if (activate) {
                    setImageScale(imageScaleMouseWheel);
                    imageScaleMouseWheel.mouseWheelMoved(e);
                }
            });
        }

        protected boolean imageScaleMouseWheelShouldBeActivated(MouseWheelEvent e) {
            return isImageScaleAutoSwitchByMouseWheel();// && (this.imageScale == imageScaleDefault);
        }

        public void setImageScaleAutoSwitchByMouseWheel(boolean imageScaleAutoSwitchByMouseWheel) {
            this.imageScaleAutoSwitchByMouseWheel = imageScaleAutoSwitchByMouseWheel;
            if (imageScale != null && imageScale instanceof MouseWheelListener) {
                if (imageScaleAutoSwitchByMouseWheel) {
                    addMouseWheelListener((MouseWheelListener) imageScale);
                } else {
                    removeMouseWheelListener((MouseWheelListener) imageScale);
                }
            }
        }

        public boolean isImageScaleAutoSwitchByMouseWheel() {
            return imageScaleAutoSwitchByMouseWheel;
        }

        protected void initEditable() {
            setEditable(((GuiReprValueImagePane) context.getRepresentation())
                    .isEditable(context));
        }

        protected void initPopup() {
            JComponent label = GuiSwingContextInfo.get().getInfoLabel(context);
            ContextRefreshAction refreshAction = new ContextRefreshAction(context);
            popup = new PopupExtension(this, PopupExtension.getDefaultKeyMatcher(), (sender, menu) -> {
                menu.accept(label);
                menu.accept(refreshAction);
                menu.accept(createSizeInfo(getImageSize()));
                menu.accept(createScaleInfo(getImageSize()));
                menu.accept(new JCheckBoxMenuItem(new ImageScaleSwitchFitAction(this)));
                menu.accept(new ImageScaleOriginalSizeAction(this));
                menu.accept(new JCheckBoxMenuItem(new ImageScaleAutoSwitchByMouseWheel(this)));

                JMenu scaleMenu = new JMenu("Scale");
                scaleMenu.add(new ImageScaleSizeAction(this, 0.5f));
                scaleMenu.add(new ImageScaleSizeAction(this, 1.5f));
                scaleMenu.add(new ImageScaleSizeAction(this, 2f));
                scaleMenu.add(new ImageScaleSizeAction(this, 4f));
                scaleMenu.add(new ImageScaleSizeAction(this, 8f));
                menu.accept(scaleMenu);


                menu.accept(new ImageCopyAction(getImage()));
                menu.accept(new ImagePasteAction(this));
                GuiSwingJsonTransfer.getActions(this, context)
                        .forEach(menu::accept);
                menu.accept(new HistoryMenuImage(this, context));
            });
            setInheritsPopupMenu(true);
        }

        protected void initDragAndDrop() {
            GuiSwingView.setupTransferHandler(this, new ImageTransferHandler(this));
        }

        protected void initContext() {
            //context update
            context.addSourceUpdateListener(this);
            //initial update
            update(context, context.getSource());
        }

        protected void initFocus() {
            setFocusable(true);
        }

        @Override
        public GuiMappingContext getContext() {
            return context;
        }

        public JComponent createSizeInfo(Dimension size) {
            return MenuBuilder.get().createLabel(String.format("Size: %,d x %,d", size.width, size.height));
        }

        public JComponent createScaleInfo(Dimension size) {
            return MenuBuilder.get().createLabel(String.format("Scale: %s",
                    imageScale == null ? "null" : imageScale.getInfo(size, getViewSize())));
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
            if (img.isEditable(context)) {
                img.updateFromGui(context, image);
            }
        }

        public Dimension getImageSize() {
            return imageSize;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            if (image != null) {
                Dimension paneSize = getViewSize();

                Dimension size = (imageScale == null ? imageSize : imageScale.getScaledImageSize(imageSize, paneSize));

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

        public ImageScale getImageScale() {
            return imageScale;
        }

        public ImageScaleFit getImageScaleDefault() {
            return imageScaleDefault;
        }

        public ImageScaleMouseWheel getImageScaleMouseWheel() {
            return imageScaleMouseWheel;
        }

        public ImageScaleFit getImageScaleFit() {
            return imageScaleFit;
        }

        public void setImageScale(ImageScale imageScale) {
            if (imageScale != this.imageScale) {
                ImageScale oldScale = this.imageScale;
                if (this.imageScale != null && this.imageScale instanceof MouseWheelListener) {
                    removeMouseWheelListener((MouseWheelListener) this.imageScale);
                }
                this.imageScale = imageScale;

                //inherits fitting scale to mouse-wheel zoom
                if (oldScale instanceof ImageScaleFit && imageScale instanceof ImageScaleMouseWheel) {
                    float p = ((ImageScaleFit) oldScale).getScale(getImageSize(), getViewSize());
                    ((ImageScaleMouseWheel) imageScale).setCurrentZoom(p);
                }

                if (imageScale != null && imageScale instanceof MouseWheelListener &&
                        isImageScaleAutoSwitchByMouseWheel()) {
                    addMouseWheelListener((MouseWheelListener) imageScale);
                }
                updateScale();
            }
        }

        public void updateScale() {
            Dimension paneSize = getViewSize();
            Dimension size = (imageScale == null ? imageSize : imageScale.getScaledImageSize(imageSize, paneSize));

            setPreferredSize(size);
            revalidate();
            repaint();
        }

        public Dimension getViewSize() {
            if (getParent() != null && getParent() instanceof JViewport) {
                return getParent().getSize();
            } else {
                return getSize();
            }
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

    public interface ImageScale {
        Dimension getScaledImageSize(Dimension srcSize, Dimension dstSize);
        String getInfo(Dimension srcSize, Dimension dstSize);
    }

    public static class ImageScaleFit implements ImageScale {
        protected float maxImageScale;

        public ImageScaleFit(float maxImageScale) {
            this.maxImageScale = maxImageScale;
        }

        @Override
        public Dimension getScaledImageSize(Dimension srcSize, Dimension dstSize) {
            float p = getScale(srcSize, dstSize);
            return new Dimension((int) (srcSize.width * p), (int) (srcSize.height * p));
        }

        public float getScale(Dimension srcSize, Dimension dstSize) {
            float pw = (dstSize.width / (float) srcSize.width);
            float ph = (dstSize.height / (float) srcSize.height);
            return Math.min(pw < ph ? pw : ph, maxImageScale);
        }

        @Override
        public String getInfo(Dimension srcSize, Dimension dstSize) {
            Dimension size = getScaledImageSize(srcSize, dstSize);
            return String.format("Fit: %d%%, %,d x %,d", (int) (getScale(srcSize, dstSize) * 100f), size.width, size.height);
        }
    }

    public static class ImageScaleMouseWheel implements MouseWheelListener, ImageScale {
        protected PropertyImagePane pane;
        protected float currentZoom = 1.0f;

        public ImageScaleMouseWheel(PropertyImagePane pane) {
            this.pane = pane;
        }

        public float getCurrentZoom() {
            return currentZoom;
        }

        public void setCurrentZoom(float currentZoom) {
            this.currentZoom = currentZoom;
            pane.updateScale();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent event) {
            if (event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                float amount = (event.getUnitsToScroll() / 100.0f) * 4.0f;
                setCurrentZoom(Math.min(Math.max(0.1f, amount + currentZoom), 20f));
            }
        }

        @Override
        public Dimension getScaledImageSize(Dimension srcSize, Dimension dstSize) {
            return new Dimension((int)  (srcSize.width * currentZoom), (int) (srcSize.height * currentZoom));
        }

        @Override
        public String getInfo(Dimension srcSize, Dimension dstSize) {
            Dimension size = getScaledImageSize(srcSize, dstSize);
            return String.format("Zoom: %d%%, %,d x %,d", (int) (getCurrentZoom() * 100f), size.width, size.height);
        }
    }

    public static class ImageScaleSwitchFitAction extends AbstractAction {
        protected PropertyImagePane pane;

        public ImageScaleSwitchFitAction(PropertyImagePane pane) {
            this.pane = pane;
            putValue(NAME, "Fit to View Size");
            updateSelected();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isSelected()) {
                pane.setImageScale(pane.getImageScaleDefault());
            } else {
                pane.setImageScale(pane.getImageScaleFit());
            }
            updateSelected();
        }

        public boolean isSelected() {
            return pane.getImageScale() == pane.getImageScaleFit();
        }

        public void updateSelected() {
            putValue(SELECTED_KEY, isSelected());
        }
    }

    public static class ImageScaleOriginalSizeAction extends AbstractAction {
        protected PropertyImagePane pane;

        public ImageScaleOriginalSizeAction(PropertyImagePane pane) {
            this.pane = pane;
            putValue(NAME, "Zoom to Original Size");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pane.setImageScale(pane.getImageScaleMouseWheel());
            pane.getImageScaleMouseWheel().setCurrentZoom(1.0f);
            pane.updateScale();
        }
    }

    public static class ImageScaleSizeAction extends AbstractAction {
        protected PropertyImagePane pane;
        protected float n;

        public ImageScaleSizeAction(PropertyImagePane pane, float n) {
            this.pane = pane;
            putValue(NAME, String.format("%d%%", (int) (n * 100)));
            this.n = n;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pane.setImageScale(pane.getImageScaleMouseWheel());
            pane.getImageScaleMouseWheel().setCurrentZoom(n);
            pane.updateScale();
        }
    }

    public static class ImageScaleAutoSwitchByMouseWheel extends AbstractAction {
        protected PropertyImagePane pane;

        public ImageScaleAutoSwitchByMouseWheel(PropertyImagePane pane) {
            putValue(NAME, "Zoom by Mouse Wheel");
            this.pane = pane;
            updateSelected();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pane.setImageScaleAutoSwitchByMouseWheel(!isSelected());
            updateSelected();
        }

        public boolean isSelected() {
            return pane.isImageScaleAutoSwitchByMouseWheel();
        }

        public void updateSelected() {
            putValue(SELECTED_KEY, isSelected());
        }
    }

    ///////////////////////

    public static class HistoryMenuImage extends HistoryMenu<Image, PropertyImagePane> {
        public HistoryMenuImage(PropertyImagePane component, GuiMappingContext context) {
            super(component, context);
        }

        @Override
        public Action createAction(GuiPreferences.HistoryValueEntry e) {
            Action a = super.createAction(e);
            Image img = (Image) e.getValue();
            Image icon = img.getScaledInstance(16, 16, Image.SCALE_DEFAULT);
            a.putValue(Action.SMALL_ICON, new ImageIcon(icon));
            return a;
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
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
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
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            paste(img -> target.setCellValues(target.getSelectedCellIndexesStream(), r -> img));
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
            Dimension size = imagePane.getImageScaleDefault().getScaledImageSize(imagePane.getImageSize(), new Dimension(100, 100));
            setDragImage(img.getScaledInstance(size.width , size.height, Image.SCALE_DEFAULT));
            return new ImageSelection(img);
        }

    }
}
