package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiPreferences;
import autogui.base.mapping.GuiReprCollectionTable;
import autogui.base.mapping.GuiReprValue;
import autogui.swing.mapping.GuiReprValueImagePane;
import autogui.swing.table.TableTargetColumnAction;
import autogui.swing.util.MenuBuilder;
import autogui.swing.util.PopupCategorized;
import autogui.swing.util.PopupExtension;
import autogui.swing.util.SettingsWindow;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * a swing view for {@link GuiReprValueImagePane}
 *
 * <h3>swing-value</h3>
 * {@link PropertyImagePane#getSwingViewValue()}:
 * latest set image as {@link Image}
 *
 * <p>
 *   updating is caused by {@link PropertyImagePane#setImage(Image)}
 *
 * <h3>history-value</h3>
 * supported.
 *
 * <h3>string-transfer</h3>
 * unsupported. but it can directly handle an image-data, or a file in a file-list.
 */
public class GuiSwingViewImagePane implements GuiSwingView {
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        PropertyImagePane imagePane = new PropertyImagePane(context, new SpecifierManagerDefault(parentSpecifier));
        ValuePane<Image> pane = new GuiSwingView.ValueScrollPane<>(imagePane);
        if (context.isTypeElementProperty()) {
            return pane.wrapSwingProperty();
        } else {
            return pane.asSwingViewComponent();
        }
    }

    @Override
    public boolean isComponentResizable(GuiMappingContext context) {
        return true;
    }

    public static class PropertyImagePane extends JComponent
            implements GuiMappingContext.SourceUpdateListener, GuiSwingView.ValuePane<Image> {
        protected GuiMappingContext context;
        protected SpecifierManager specifierManager;
        protected Image image;
        protected Dimension imageSize = new Dimension(1, 1);
        protected boolean editable;

        protected ImageScale imageScale;
        protected ImageScaleFit imageScaleDefault;
        protected ImageScaleFit imageScaleFit;
        protected ImageScaleMouseWheel imageScaleMouseWheel;

        protected boolean imageScaleAutoSwitchByMouseWheel = true;

        protected PopupExtension popup;
        protected List<PopupCategorized.CategorizedMenuItem> menuItems;

        protected ImageScaleAutoSwitchByMouseWheel autoSwitchByMouseWheel;
        protected ImageScaleSwitchFitAction switchFitAction;

        public PropertyImagePane(GuiMappingContext context, SpecifierManager specifierManager) {
            this.context = context;
            this.specifierManager = specifierManager;
            init();
        }

        public void init() {
            initName();
            initScale();
            initEditable();
            initContextUpdate();
            initValue();
            initPopup();
            initDragDrop();
            initFocus();
        }

        public void initName() {
            setName(context.getName());
            GuiSwingView.setDescriptionToolTipText(context, this);
        }

        public void initScale() {
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
            if (imageScale instanceof MouseWheelListener) {
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

        public void initEditable() {
            setEditable(((GuiReprValueImagePane) context.getRepresentation())
                    .isEditable(context));
        }

        public void initContextUpdate() {
            context.addSourceUpdateListener(this);
        }

        public void initValue() {
            update(context, context.getSource().getValue());
        }

        public void initPopup() {
            popup = new PopupExtension(this, new PopupCategorized(
                    PopupCategorized.getMenuItemsSupplier(this::getSwingStaticMenuItems, this::getDynamicMenuItems)));
            setInheritsPopupMenu(true);
        }

        public void initDragDrop() {
            GuiSwingView.setupTransferHandler(this, new ImageTransferHandler(this));
        }


        public void initFocus() {
            setFocusable(true);
        }

        @Override
        public GuiMappingContext getSwingViewContext() {
            return context;
        }

        public JComponent createSizeInfo(Dimension size) {
            return MenuBuilder.get().createLabel(String.format("Size: %,d x %,d", size.width, size.height),
                    PopupCategorized.SUB_CATEGORY_LABEL_VALUE);
        }

        public JComponent createScaleInfo(Dimension size) {
            return MenuBuilder.get().createLabel(String.format("Scale: %s",
                    imageScale == null ? "null" : imageScale.getInfo(size, getViewSize())),
                    PopupCategorized.SUB_CATEGORY_LABEL_VALUE);
        }

        @Override
        public List<PopupCategorized.CategorizedMenuItem> getSwingStaticMenuItems() {
            if (menuItems == null) {
                JMenu scaleMenu = new JMenu("Scale");
                scaleMenu.add(new ImageScaleSizeAction(this, 0.5f));
                scaleMenu.add(new ImageScaleSizeAction(this, 1.5f));
                scaleMenu.add(new ImageScaleSizeAction(this, 2f));
                scaleMenu.add(new ImageScaleSizeAction(this, 4f));
                scaleMenu.add(new ImageScaleSizeAction(this, 8f));

                autoSwitchByMouseWheel = new ImageScaleAutoSwitchByMouseWheel(this);
                switchFitAction = new ImageScaleSwitchFitAction(this);

                menuItems = PopupCategorized.getMenuItems(
                        Arrays.asList(
                                GuiSwingContextInfo.get().getInfoLabel(context),
                                new ContextRefreshAction(context),
                                switchFitAction,
                                new ImageScaleOriginalSizeAction(this),
                                autoSwitchByMouseWheel,
                                new PopupCategorized.CategorizedMenuItemComponentDefault(scaleMenu,
                                        PopupExtension.MENU_CATEGORY_VIEW, ""),
                                new ImageCopyAction(this::getImage),
                                new ImagePasteAction(this),
                                new ImageSaveAction(this),
                                new ImageLoadAction(this),
                                new HistoryMenuImage(this, context)),
                        GuiSwingJsonTransfer.getActions(this, context)
                );
            }
            return menuItems;
        }

        public List<PopupCategorized.CategorizedMenuItem> getDynamicMenuItems() {
            return PopupCategorized.getMenuItems(
                    Arrays.asList(createSizeInfo(getImageSize()), createScaleInfo(getImageSize())));
        }

        @Override
        public PopupExtension.PopupMenuBuilder getSwingMenuBuilder() {
            return popup.getMenuBuilder();
        }

        public boolean isSwingEditable() {
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
                img.updateFromGui(context, image, getSpecifier());
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
                if (this.imageScale instanceof MouseWheelListener) {
                    removeMouseWheelListener((MouseWheelListener) this.imageScale);
                }
                this.imageScale = imageScale;

                //inherits fitting scale to mouse-wheel zoom
                if (oldScale instanceof ImageScaleFit && imageScale instanceof ImageScaleMouseWheel) {
                    float p = ((ImageScaleFit) oldScale).getScale(getImageSize(), getViewSize());
                    ((ImageScaleMouseWheel) imageScale).setCurrentZoom(p);
                }

                if (imageScale instanceof MouseWheelListener &&
                        isImageScaleAutoSwitchByMouseWheel()) {
                    addMouseWheelListener((MouseWheelListener) imageScale);
                }
                updateScale();
            }
        }

        public void updateScale() {
            Dimension paneSize = getViewSize();
            Dimension size = (imageScale == null ? imageSize : imageScale.getScaledImageSize(imageSize, paneSize));

            if (autoSwitchByMouseWheel != null) {
                autoSwitchByMouseWheel.updateSelected();
            }
            if (switchFitAction != null) {
                switchFitAction.updateSelected();
            }

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

        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
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

    public static class ImageScaleSwitchFitAction extends AbstractAction
            implements PopupCategorized.CategorizedMenuItemActionCheck, TableTargetColumnAction {
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

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            actionPerformed(null);
        }

        public boolean isSelected() {
            return pane.getImageScale() == pane.getImageScaleFit();
        }

        public void updateSelected() {
            putValue(SELECTED_KEY, isSelected());
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_VIEW;
        }
    }

    public static class ImageScaleOriginalSizeAction extends AbstractAction
            implements PopupCategorized.CategorizedMenuItemAction, TableTargetColumnAction {
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

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            actionPerformed(null);
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_VIEW;
        }
    }

    public static class ImageScaleSizeAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
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

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_VIEW;
        }
    }

    public static class ImageScaleAutoSwitchByMouseWheel extends AbstractAction implements PopupCategorized.CategorizedMenuItemActionCheck {
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

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_VIEW;
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
        protected Supplier<Image> image;

        public ImageCopyAction(Supplier<Image> image) {
            putValue(NAME, "Copy");
            this.image = image;
        }

        @Override
        public boolean isEnabled() {
            return image.get() != null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            copy(this.image.get());
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

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_COPY;
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
            return pane.isSwingEditable();
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


        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_EDIT;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_PASTE;
        }
    }

    public static class ImageSaveAction extends ImageCopyAction {
        protected PropertyImagePane pane;
        public ImageSaveAction(PropertyImagePane pane) {
            super(pane::getImage);
            putValue(NAME, "Export...");
            this.pane = pane;
        }

        @Override
        public void copy(Image image) {
            SettingsWindow.FileDialogManager fd = SettingsWindow.getFileDialogManager();
            Path path = fd.showConfirmDialogIfOverwriting(pane.asSwingViewComponent(),
                    fd.showSaveDialog(pane.asSwingViewComponent(), null, pane.getName() + ".png"));
            if (path != null) {
                try {
                    String name = path.getFileName().toString();
                    int suffIdx = name.lastIndexOf('.');
                    String format = "png";
                    if (suffIdx > 0) {
                        format = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
                    }

                    GuiReprValueImagePane imagePane = (GuiReprValueImagePane) pane.getSwingViewContext().getRepresentation();
                    ImageIO.write(imagePane.getRenderedImage(pane.getSwingViewContext(), image), format, path.toFile());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_TRANSFER;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_EXPORT;
        }
    }

    public static class ImageLoadAction extends ImagePasteAction {

        public ImageLoadAction(PropertyImagePane pane) {
            super(pane);
            putValue(NAME, "Import...");
        }

        @Override
        public void paste(Consumer<Image> c) {
            Path path = SettingsWindow.getFileDialogManager().showOpenDialog(pane, null);
            if (path != null) {
                try {
                    c.accept(ImageIO.read(path.toFile()));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_TRANSFER;
        }

        @Override
        public String getSubCategory() {
            return PopupExtension.MENU_SUB_CATEGORY_IMPORT;
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
            return imagePane.isSwingEditable() &&
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
