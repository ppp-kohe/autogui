package org.autogui.swing;

import org.autogui.swing.mapping.GuiReprValueImagePane;
import org.autogui.swing.table.TableTargetColumnAction;
import org.autogui.base.mapping.*;
import org.autogui.swing.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * a swing view for {@link GuiReprValueImagePane}
 *
 * <h2>swing-value</h2>
 * {@link PropertyImagePane#getSwingViewValue()}:
 * latest set image as {@link Image}
 *
 * <p>
 *   updating is caused by {@link PropertyImagePane#setImage(Image)}
 *
 * <h2>history-value</h2>
 * supported.
 *
 * <h2>string-transfer</h2>
 * unsupported. but it can directly handle an image-data, or a file in a file-list.
 */
@SuppressWarnings("this-escape")
public class GuiSwingViewImagePane implements GuiSwingView {
    public GuiSwingViewImagePane() {}
    @Override
    public JComponent createView(GuiMappingContext context, Supplier<GuiReprValue.ObjectSpecifier> parentSpecifier) {
        PropertyImagePane imagePane = new PropertyImagePane(context, new SpecifierManagerDefault(parentSpecifier));
        ValuePane<Image> pane = new GuiSwingViewWrapper.ValueScrollPane<>(imagePane);
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
        @Serial private static final long serialVersionUID = 1L;

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
        protected MenuBuilder.MenuLabel infoLabel;

        protected ImageScaleAutoSwitchByMouseWheel autoSwitchByMouseWheel;
        protected ImageScaleSwitchFitAction switchFitAction;

        protected GuiTaskClock viewClock = new GuiTaskClock(true);
        protected boolean currentValueSupported = true;

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
            initDragScroll();
            initFocus();
        }

        public void initName() {
            setName(context.getName());
            infoLabel = GuiSwingContextInfo.get().getInfoLabel(context);
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
            update(context, context.getSource().getValue(), context.getContextClock().copy());
        }

        public void initPopup() {
            popup = new PopupExtension(this, new PopupCategorized(
                    PopupCategorized.getMenuItemsSupplier(this::getSwingStaticMenuItems, this::getDynamicMenuItems)));
            GuiSwingView.setupKeyBindingsForStaticMenuItems(this);
            setInheritsPopupMenu(true);
        }

        public void initDragDrop() {
            GuiSwingView.setupTransferHandler(this, new ImageTransferHandler(this), KeyEvent.VK_SHIFT);
        }

        /**
         * adding listeners for supporting drag scrolling
         * @since 1.5
         */
        public void initDragScroll() {
            MouseAdapter dragHandler = new MouseAdapter() {
                Point dragStart;
                @Override
                public void mousePressed(MouseEvent e) {
                    dragStart = e.getLocationOnScreen();
                }
                @Override
                public void mouseDragged(MouseEvent e) {
                    Point current = e.getLocationOnScreen();
                    if (dragStart != null && isParentViewport()) {
                        int dx = current.x - dragStart.x;
                        int dy = current.y - dragStart.y;
                        JViewport viewport = (JViewport) getParent();
                        Dimension extentSize = viewport.getExtentSize();
                        Dimension size = viewport.getViewSize();
                        Point newPos = viewport.getViewPosition();
                        if (size.width > extentSize.width) {
                            newPos.x = Math.min(size.width, Math.max(0, newPos.x - dx));
                        }
                        if (size.height > extentSize.height) {
                            newPos.y = Math.min(size.height, Math.max(0, newPos.y - dy));

                        }
                        viewport.setViewPosition(newPos);
                    }
                    dragStart = current;
                }
            };
            addMouseListener(dragHandler);
            addMouseMotionListener(dragHandler);
        }

        public void initFocus() {
            setFocusable(true);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    requestFocusInWindow();
                }
            });
            setBorder(new GuiSwingViewLabel.FocusBorder(this));
        }

        @Override
        public boolean isSwingCurrentValueSupported() {
            return currentValueSupported && getSwingViewContext().isHistoryValueSupported();
        }

        public void setCurrentValueSupported(boolean currentValueSupported) {
            this.currentValueSupported = currentValueSupported;
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
                                infoLabel,
                                new ContextRefreshAction(context, this),
                                switchFitAction,
                                new ImageScaleOriginalSizeAction(this),
                                autoSwitchByMouseWheel,
                                new ImageScaleIncreaseAction(this, 0.1f),
                                new ImageScaleIncreaseAction(this, -0.1f),
                                new PopupCategorized.CategorizedMenuItemComponentDefault(scaleMenu,
                                        PopupExtension.MENU_CATEGORY_VIEW, ""),
                                new ImageCopyAction(this::getImage, context),
                                new ImagePasteAction(this),
                                new ImageClearAction(this),
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
        public void update(GuiMappingContext cause, Object newValue, GuiTaskClock contextClock) {
            SwingUtilities.invokeLater(() -> setSwingViewValue((Image) newValue, contextClock));
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
            GuiReprValueImagePane img = (GuiReprValueImagePane) context.getRepresentation();
            Image v = (Image) img.toUpdateValue(context, image);
            setImageWithoutContextUpdate(v);
            updateFromGui(v, viewClock.increment());
        }

        public void updateFromGui(Object v, GuiTaskClock viewClock) {
            GuiSwingView.updateFromGui(this, v, viewClock);
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

                Dimension size = getImageScaledSize();

                //if the image's size exceeds the view size, its starting position becomes 0, otherwise a half of diff.
                Insets insets = getInsets();
                int left;
                if (size.width > paneSize.width) {
                    left = insets.left;
                } else {
                    left = (paneSize.width - size.width) / 2 + insets.left;
                }
                int top;
                if (size.height > paneSize.height) {
                    top = insets.top;
                } else {
                    top = (paneSize.height - size.height) / 2 + insets.top;
                }

                try {
                    while (!g.drawImage(image, left, top, size.width, size.height, this)) {
                        Thread.sleep(10);
                    }
                } catch (InterruptedException ex) {
                    //nothing
                }
            }
        }

        public Dimension getImageScaledSize() {
            return imageScale == null ? imageSize : imageScale.getScaledImageSize(imageSize, getViewSize());
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

        public Dimension getViewSize() { //it means "extentSize"
            Insets insets = getInsets();
            if (isParentViewport()) {
                return subtract(getParent().getSize(), insets);
            } else {
                return subtract(getSize(), insets);
            }
        }

        private Dimension subtract(Dimension dim, Insets insets) {
            if (insets == null) {
                return dim;
            } else {
                return new Dimension(dim.width - insets.left - insets.right ,
                        dim.height - insets.top - insets.bottom);
            }
        }

        private boolean isParentViewport() {
            return getParent() != null && getParent() instanceof JViewport;
        }

        /**
         * @return the parent viewport's viewPosition or (0,0)
         * @since 1.5
         */
        public Point getViewPosition() {
            if (isParentViewport()) {
                Point p = ((JViewport) getParent()).getViewPosition();
                if (p == null) {
                    p = new Point();
                }
                return p;
            } else {
                return new Point();
            }
        }

        @Override
        public Image getSwingViewValue() {
            return getImage();
        }

        @Override
        public void setSwingViewValue(Image value) {
            viewClock.increment();
            GuiReprValueImagePane img = (GuiReprValueImagePane) context.getRepresentation();
            setImageWithoutContextUpdate(img.updateValue(context, value));
        }

        @Override
        public void setSwingViewValueWithUpdate(Image value) {
            GuiSwingView.updateViewClockSync(viewClock, context);
            setImage(value);
        }

        @Override
        public void setSwingViewValue(Image value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                GuiReprValueImagePane img = (GuiReprValueImagePane) context.getRepresentation();
                setImageWithoutContextUpdate(img.updateValue(context, value));
            }
        }

        @Override
        public void setSwingViewValueWithUpdate(Image value, GuiTaskClock clock) {
            if (viewClock.isOlderWithSet(clock)) {
                GuiReprValueImagePane img = (GuiReprValueImagePane) context.getRepresentation();
                Image v = img.updateValue(context, value);
                setImageWithoutContextUpdate(v);
                updateFromGui(v, viewClock);
            }
        }



        @Override
        public GuiReprValue.ObjectSpecifier getSpecifier() {
            return specifierManager.getSpecifier();
        }

        @Override
        public void setKeyStrokeString(String keyStrokeString) {
            infoLabel.setAdditionalInfo(keyStrokeString);
        }

        /**
         * an imported image from a file always be called this method with the file.
         *  then the value of {@link GuiPreferences.HistoryValueEntry} becomes
         *  {@link GuiReprValueImagePane.ImageHistoryEntry}.
         * @param image a loaded image. if null, ignored
         * @param path source path of the image
         */
        public void setImagePath(Image image, Path path) {
            ((GuiReprValueImagePane) getSwingViewContext().getRepresentation()).setImagePath(image, path);
        }

        public Path getImagePath(Image image) {
            return ((GuiReprValueImagePane) getSwingViewContext().getRepresentation()).getImagePath(image);
        }

        @Override
        public void setSwingViewHistoryValue(Object value) {
            if (value instanceof GuiReprValueImagePane.ImageHistoryEntry) {
                setSwingViewValueWithUpdate(((GuiReprValueImagePane.ImageHistoryEntry) value).getImage());
            } else {
                setSwingViewValueWithUpdate((Image) value);
            }
        }

        @Override
        public void prepareForRefresh() {
            viewClock.clear();
        }
    }

    public interface ImageScale {
        Dimension getScaledImageSize(Dimension srcSize, Dimension dstSize);
        String getInfo();
        String getInfo(Dimension srcSize, Dimension dstSize);
        ImageScale copyFor(PropertyImagePane pane);
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
            return Math.min(Math.min(pw, ph), maxImageScale);
        }

        @Override
        public String getInfo() {
            return "Fit";
        }

        @Override
        public String getInfo(Dimension srcSize, Dimension dstSize) {
            Dimension size = getScaledImageSize(srcSize, dstSize);
            return String.format("Fit: %d%%, %,d x %,d", (int) (getScale(srcSize, dstSize) * 100f), size.width, size.height);
        }

        @Override
        public ImageScale copyFor(PropertyImagePane pane) {
            return new ImageScaleFit(maxImageScale);
        }
    }

    /** the max size of image-scaling
     * @since 1.2 */
    public static final int SCALE_MAX_WIDTH = 30_000;
    /** the max size of image-scaling
     * @since 1.2 */
    public static final int SCALE_MAX_HEIGHT = 30_000;

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
            Dimension imageSize = pane.getImageSize();
            if (imageSize != null) {
                double toMaxW = SCALE_MAX_WIDTH / imageSize.getWidth();
                double toMaxH = SCALE_MAX_HEIGHT / imageSize.getHeight();
                currentZoom = (float) Math.min(Math.min(toMaxW, toMaxH), currentZoom);
            }
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
        public String getInfo() {
            return String.format("Zoom: %d%%", (int) (getCurrentZoom() * 100f));
        }

        @Override
        public String getInfo(Dimension srcSize, Dimension dstSize) {
            Dimension size = getScaledImageSize(srcSize, dstSize);
            return String.format("Zoom: %d%%, %,d x %,d", (int) (getCurrentZoom() * 100f), size.width, size.height);
        }

        @Override
        public ImageScale copyFor(PropertyImagePane pane) {
            ImageScaleMouseWheel w = new ImageScaleMouseWheel(pane);
            w.currentZoom = currentZoom;
            return w;
        }
    }

    public static class ImageScaleSwitchFitAction extends AbstractAction
            implements PopupCategorized.CategorizedMenuItemActionCheck, TableTargetColumnAction {
        @Serial private static final long serialVersionUID = 1L;

        protected PropertyImagePane pane;

        public ImageScaleSwitchFitAction(PropertyImagePane pane) {
            this.pane = pane;
            putValue(NAME, "Fit to View Size");

            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_T,
                            PopupExtension.getMenuShortcutKeyMask(), KeyEvent.SHIFT_DOWN_MASK));
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
        @Serial private static final long serialVersionUID = 1L;

        protected PropertyImagePane pane;

        public ImageScaleOriginalSizeAction(PropertyImagePane pane) {
            this.pane = pane;
            putValue(NAME, "Zoom to Original Size");

            putValue(Action.ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_O,
                            PopupExtension.getMenuShortcutKeyMask(), KeyEvent.SHIFT_DOWN_MASK));
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
        @Serial private static final long serialVersionUID = 1L;

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
        @Serial private static final long serialVersionUID = 1L;

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

    public static class ImageScaleIncreaseAction extends AbstractAction implements PopupCategorized.CategorizedMenuItemAction {
        @Serial private static final long serialVersionUID = 1L;

        protected PropertyImagePane pane;
        protected float n;

        public ImageScaleIncreaseAction(PropertyImagePane pane, float n) {
            this.pane = pane;
            putValue(NAME, n > 0 ? "Increase Scale" : "Decrease Scale");
            putValue(ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(n > 0 ? KeyEvent.VK_I : KeyEvent.VK_D,
                            PopupExtension.getMenuShortcutKeyMask(), KeyEvent.SHIFT_DOWN_MASK));
            this.n = n;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pane.setImageScale(pane.getImageScaleMouseWheel());
            ImageScaleMouseWheel w = pane.getImageScaleMouseWheel();

            if (n > 0) {
                if (w.getCurrentZoom() < 8.0f) {
                    w.setCurrentZoom(Math.min(8.0f, w.getCurrentZoom() + n));
                }
            } else {
                if (w.getCurrentZoom() > 0.1f) {
                    w.setCurrentZoom(Math.max(0.1f, w.getCurrentZoom() + n));
                }
            }
        }

        @Override
        public String getCategory() {
            return PopupExtension.MENU_CATEGORY_VIEW;
        }
    }

    ///////////////////////

    public static class HistoryMenuImage extends GuiSwingHistoryMenu<Image, PropertyImagePane> {
        @Serial private static final long serialVersionUID = 1L;

        public HistoryMenuImage(PropertyImagePane component, GuiMappingContext context) {
            super(component, context);
        }

        @Override
        public Action createAction(GuiPreferences.HistoryValueEntry e) {
            Object value = e.getValue();
            String name;
            Image img;
            if (value instanceof GuiReprValueImagePane.ImageHistoryEntry he) {
                img = he.getImage();
                name = getActionNameFromString(he.getPath().toString());
            } else {
                img = (Image) value;
                name = getActionName(e);
            }
            Action a = createActionBase(name, img);

            int size = UIManagerUtil.getInstance().getScaledSizeInt(16);
            if (img != null) {
                Image icon = img.getScaledInstance(size, size, Image.SCALE_DEFAULT);
                a.putValue(Action.SMALL_ICON, new ImageIcon(icon));
            }
            return a;
        }

        @Override
        public String getActionName(GuiPreferences.HistoryValueEntry e) {
            Object v = e.getValue();
            StringBuilder buf = new StringBuilder();
            if (v == null) {
                buf.append("null");
            } else {
                buf.append(v.getClass().getSimpleName());
                if (v instanceof BufferedImage img) {
                    buf.append(": ").append(img.getWidth()).append(" x ").append(img.getHeight());
                }
            }
            return buf.toString();
        }

        public Action createActionBase(String name, Image value) {
            return new HistorySetAction<>(name, value, component);
        }

        @Override
        public JMenu convert(GuiReprCollectionTable.TableTargetColumn target) {
            return new HistoryMenuItemForTableColumn(component, context, target);
        }
    }

    public static class HistoryMenuItemForTableColumn extends HistoryMenuImage {
        @Serial private static final long serialVersionUID = 1L;

        protected GuiReprCollectionTable.TableTargetColumn target;
        public HistoryMenuItemForTableColumn(PropertyImagePane component, GuiMappingContext context, GuiReprCollectionTable.TableTargetColumn target) {
            super(component, context);
            this.target = target;
        }

        @Override
        public Action createActionBase(String name, Image value) {
            return new HistorySetForColumnAction<>(name, value, target);
        }
    }

    public static class ImageCopyAction extends AbstractAction implements TableTargetColumnAction {
        @Serial private static final long serialVersionUID = 1L;

        protected Supplier<Image> image;
        protected GuiMappingContext context;

        public ImageCopyAction(Supplier<Image> image, GuiMappingContext contextOpt) {
            putValue(NAME, "Copy");
            putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_C,
                    PopupExtension.getMenuShortcutKeyMask()));
            this.image = image;
            this.context = contextOpt;
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
                ImageSelection selection = (context == null ?
                        new ImageSelection(image) :
                        new ImageSelection(image, context));
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
        @Serial private static final long serialVersionUID = 1L;

        protected PropertyImagePane pane;

        public ImagePasteAction(PropertyImagePane pane) {
            putValue(NAME, "Paste");
            putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_V,
                    PopupExtension.getMenuShortcutKeyMask()));
            this.pane = pane;
        }

        @Override
        public boolean isEnabled() {
            return pane != null && pane.isSwingEditable();
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

                setFile(img, clip);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @SuppressWarnings("unchecked")
        private void setFile(Image img, Clipboard clip) throws Exception {
            if (clip.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                List<File> fs = (List<File>) clip.getData(DataFlavor.javaFileListFlavor);
                if (fs != null && !fs.isEmpty()) {
                    pane.setImagePath(img, fs.getFirst().toPath());
                }
            }
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            paste(img -> target.setCellValues(target.getSelectedCellIndices(), r -> img));
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

    /**
     * action for clearing the image
     * @since 1.5
     */
    //TODO set null ?
    public static class ImageClearAction extends AbstractAction implements TableTargetColumnAction {
        @Serial private static final long serialVersionUID = 1L;

        protected PropertyImagePane pane;

        public ImageClearAction(PropertyImagePane pane) {
            putValue(NAME, "Clear");
            this.pane = pane;
        }

        BufferedImage image;

        public BufferedImage getClearImage() {
            if (image == null) {
                image = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
                image.setRGB(0, 0, 0xFFFF_FFFF);
            }
            return image;
        }

        @Override
        public boolean isEnabled() {
            return pane != null && pane.isSwingEditable();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            clear(pane::setImage);
        }

        public void clear(Consumer<Image> c) {
            c.accept(getClearImage());
        }

        @Override
        public void actionPerformedOnTableColumn(ActionEvent e, GuiReprCollectionTable.TableTargetColumn target) {
            clear(img -> target.setCellValues(target.getSelectedCellIndices(), r -> img));
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
        @Serial private static final long serialVersionUID = 1L;

        protected PropertyImagePane pane;
        public ImageSaveAction(PropertyImagePane pane) {
            super(pane::getImage, pane.getSwingViewContext());
            putValue(NAME, "Export...");
            putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_S,
                    PopupExtension.getMenuShortcutKeyMask()));
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
        @Serial private static final long serialVersionUID = 1L;

        public ImageLoadAction(PropertyImagePane pane) {
            super(pane);
            putValue(NAME, "Import...");
            putValue(ACCELERATOR_KEY, PopupExtension.getKeyStroke(KeyEvent.VK_O,
                    PopupExtension.getMenuShortcutKeyMask()));
        }

        @Override
        public void paste(Consumer<Image> c) {
            Path path = SettingsWindow.getFileDialogManager().showOpenDialog(pane, null);
            if (path != null) {
                try {
                    Image img = ImageIO.read(path.toFile());
                    pane.setImagePath(img, path);
                    c.accept(img);
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
        protected GuiMappingContext context;

        protected List<DataFlavor> flavors;

        public ImageSelection(Image image) {
            this.image = image;
            flavors = Collections.singletonList(DataFlavor.imageFlavor);
        }

        public ImageSelection(Image image, GuiMappingContext context) {
            this.image = image;
            this.context = context;
            flavors = new ArrayList<>(3);
            flavors.add(DataFlavor.imageFlavor);

            Path p = ((GuiReprValueImagePane) context.getReprValue()).getImagePath(image);
            if (p != null) {
                flavors.add(DataFlavor.javaFileListFlavor);
            }

            flavors.add(DataFlavor.stringFlavor);
        }

        public Image getImage() {
            return image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors.toArray(new DataFlavor[0]);
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavors.stream()
                    .anyMatch(flavor::equals);
        }

        @Override
        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
            if (DataFlavor.imageFlavor.equals(flavor)) {
                return image;
            } else if (context != null && DataFlavor.javaFileListFlavor.equals(flavor)) {
                Path p = ((GuiReprValueImagePane) context.getReprValue()).getImagePath(image);
                if (p != null) {
                    return Collections.singletonList(p.toFile());
                }
            } else if (context != null && DataFlavor.stringFlavor.equals(flavor)) {
                return context.getReprValue().toHumanReadableString(context, image);
            }
            throw new UnsupportedFlavorException(flavor);
        }
        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            image = null;
        }
    }


    public static class ImageTransferHandler extends TransferHandler {
        @Serial private static final long serialVersionUID = 1L;

        protected PropertyImagePane imagePane;

        public ImageTransferHandler(PropertyImagePane imagePane) {
            this.imagePane = imagePane;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return imagePane.isSwingEditable() &&
                    (support.isDataFlavorSupported(DataFlavor.imageFlavor) ||
                        support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                        support.isDataFlavorSupported(DataFlavor.stringFlavor));
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                return select(getTransferableAsImage(support, DataFlavor.imageFlavor));
            } else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return select(loadTransferableFilesAsImage(support, DataFlavor.javaFileListFlavor));
            } else if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return select(getTransferableStringAsImage(support, DataFlavor.stringFlavor));
            } else {
                return false;
            }
        }

        public Image getTransferableAsImage(TransferSupport support, DataFlavor flavor) {
            try {
                Image image = (Image) support.getTransferable().getTransferData(flavor);
                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    File file = getTransferableFile(support, DataFlavor.javaFileListFlavor);
                    if (file != null) {
                        imagePane.setImagePath(image, file.toPath());
                    }
                }
                return image;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @SuppressWarnings("unchecked")
        private File getTransferableFile(TransferSupport support, DataFlavor flavor) throws Exception {
            List<File> fs = (List<File>) support.getTransferable().getTransferData(flavor);
            if (!fs.isEmpty()) {
                return fs.getFirst();
            } else {
                return null;
            }
        }

        public Image loadTransferableFilesAsImage(TransferSupport support, DataFlavor flavor) {
            try {
                try {
                    File file = getTransferableFile(support, flavor);
                    if (file == null) {
                        return null;
                    }
                    Image img = ImageIO.read(file);
                    imagePane.setImagePath(img, file.toPath());
                    return img;
                } catch (Exception ex) {
                    return null;
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public Image getTransferableStringAsImage(TransferSupport support, DataFlavor flavor) {
            try {
                String data = (String) support.getTransferable().getTransferData(flavor);
                GuiMappingContext context = imagePane.getSwingViewContext();
                return (Image) context.getReprValue().fromHumanReadableString(context, data);
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
            if (img == null) {
                return null;
            } else {
                int s = UIManagerUtil.getInstance().getScaledSizeInt(100);
                Dimension size = imagePane.getImageScaleDefault().getScaledImageSize(imagePane.getImageSize(), new Dimension(s, s));
                setDragImage(img.getScaledInstance(size.width, size.height, Image.SCALE_DEFAULT));
                return new ImageSelection(img, imagePane.getSwingViewContext());
            }
        }

    }
}
