package org.autogui.swing.mapping;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiReprValue;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.GuiSwingMapperSet;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** a GUI representation for a property holding an {@link Image}.
 * the representation depends on some AWT classes (java.desktop module)
 * <pre>
 *      &#64;GuiIncluded public Image imageProp;
 * </pre>
 *
 * */
public class GuiReprValueImagePane extends GuiReprValue {
    protected Map<Image,Path> imageToReference = new WeakHashMap<>();

    /**
     * the global instance set by {@link #GuiReprValueImagePane(boolean)} with true
     * @since 1.1
     */
    protected static GuiReprValueImagePane instance;

    /**
     * return the default global instance of the class, useful for {@link #getImagePath(Image)}.
     * <pre>
     *     void setImage(Image img) {
     *        ...
     *        Path p = GuiReprValueImagePane.getInstance().getImagePath(img);
     *        if (p != null) {
     *          ... //if the user drag an image file to the associated pane, p will be the path of the file.
     *        }
     *     }
     * </pre>
     * @return the global instance.
     * @since 1.1
     */
    public static GuiReprValueImagePane getInstance() {
        synchronized (GuiReprValueImagePane.class) {
            GuiSwingMapperSet.getReprDefaultSet(); //the method will cause construction of the global instance
            return instance;
        }
    }

    /**
     * the created instance will not be the global instance
     */
    public GuiReprValueImagePane() {
        this(false);
    }

    /**
     * @param processGlobal if true, the instance will be held by the static field of the class,
     *                       which can be obtained by {@link #getInstance()}. This is useful for obtaining image paths
     * @since 1.1
     */
    public GuiReprValueImagePane(boolean processGlobal) {
        if (processGlobal) {
            synchronized (GuiReprValueImagePane.class) {
                instance = this;
            }
        }
    }

    @Override
    public boolean matchValueType(Class<?> cls) {
        return Image.class.isAssignableFrom(cls);
    }

    public Image updateValue(GuiMappingContext context, Object value) {
        if (value instanceof Image) {
            return (Image) value;
        } else if (value instanceof ImageHistoryEntry) {
            return ((ImageHistoryEntry) value).getImage();
        } else {
            return null;
        }
    }

    public Dimension getSize(GuiMappingContext context, Image image) {
        return ImageSizeGetter.getSizeNonNull(image);
    }

    /**
     * only images which have been {@link #setImagePath(Image, Path)}
     *  will be stored as its path through {@link ImageHistoryEntry}.
     *  Currently, {@link ImageHistoryEntry} will not appear to non-history related arguments (like update(...newValue...)
     * @param value {@link ImageHistoryEntry} or {@link Image}
     * @return true only if {@link ImageHistoryEntry} or null (loading)
     */
    @Override
    public boolean isHistoryValueStored(Object value) {
        return value == null || value instanceof ImageHistoryEntry;
    }

    /**
     * an {@link ImageObserver} for obtaining the size of an image
     */
    public static class ImageSizeGetter implements ImageObserver {
        protected AtomicInteger width = new AtomicInteger();
        protected AtomicInteger height = new AtomicInteger();
        protected AtomicBoolean finish = new AtomicBoolean();

        public ImageSizeGetter() {}

        /**
         * the current impl. of {@link GuiReprValueImagePane#getSize(GuiMappingContext, Image)}
         * @param image an image or null
         * @return the size or (1,1)
         * @since 1.6.3
         */
        public static Dimension getSizeNonNull(Image image) {
            if (image == null) {
                return new Dimension(1, 1);
            } else if (image instanceof BufferedImage bufferedImage) {
                return new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight());
            } else {
                return new ImageSizeGetter().getWithWait(image);
            }
        }

        public Dimension getWithWait(Image image) {
            finish.set(false);
            int width = image.getWidth(this);
            int height = image.getHeight(this);
            if (width > 0) {
                this.width.set(width);
            }
            if (height > 0) {
                this.height.set(height);
            }

            if (width < 0 || height < 0) {
                Instant start = Instant.now();
                Duration limit = Duration.ofSeconds(10);
                try {
                    while (!finish.get()) {
                        Thread.sleep(10);
                        if (Duration.between(start, Instant.now()).compareTo(limit) >= 0) {
                            System.err.printf("%s: getWithWait timeout width=%,d height=%,d%n",
                                    this, this.width.get(), this.height.get());
                            break;
                        }
                    }
                } catch (Exception ex) {
                    error(ex);
                }
            }
            return new Dimension(this.width.get(), this.height.get());
        }

        public void error(Exception ex) {
            throw new RuntimeException(ex);
        }

        @Override
        public boolean imageUpdate(Image img, int infoFlags,
                                   int x, int y, int width, int height) {
            if ((infoFlags & ImageObserver.ERROR) != 0) {
                finish.set(true);
                this.width.set(0);
                this.height.set(0);
                return true;
            }
            if (width > 0) {
                this.width.set(width);
            }
            if (height > 0) {
                this.height.set(height);
            }
            boolean f = ((infoFlags & ImageObserver.ALLBITS) != 0)
                    || ((infoFlags & (ImageObserver.WIDTH | ImageObserver.HEIGHT)) != 0);
            finish.set(f);
            return !f;
        }
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object {@link Image} or {@link ImageHistoryEntry}
     * @return image bytes encoded as PNG base64 String(<code>data:image/png;base64,...</code>), or
     *     source file path (<code>file://...</code>) if the source is a {@link ImageHistoryEntry}.
     *     The method does not check registered file paths by {@link #setImagePath(Image, Path)}.
     *   For non-RenderedImage, it temporally creates a BufferedImage and renders the source to the image.
     */
    @Override
    public Object toJson(GuiMappingContext context, Object source) {
        if (source instanceof ImageHistoryEntry) { //preferences convert an entry to a stored value
            return ((ImageHistoryEntry) source).getPath().toUri().toString();
        } else {
            RenderedImage img = getRenderedImage(context, source);
            if (img != null) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try {
                    ImageIO.write(img, "png", bytes);
                    return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
                } catch (Exception ex) {
                    return null;
                }
            }
            return null;
        }
    }

    static Pattern dataPattern = Pattern.compile("data:(image/.+?)?(;.+?)?,(.*+)");

    /***
     * @param context the target context, ignored
     * @param target the target object, ignored
     * @param json a string with format of <code>data:...</code> or <code>file://...</code>
     * @return an {@link Image} or an {@link ImageHistoryEntry} with lazy loading
     *  (at the loading, {@link #setImagePath(Image, Path)} will be called and the path is stored).
     */
    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        if (json instanceof String jsonStr) {
            if (jsonStr.startsWith("file://")) {
                return new ImageHistoryEntry(Paths.get(URI.create(jsonStr)), this::setImagePath);
            }

            Matcher m = dataPattern.matcher(jsonStr);
            if (m.find()) {
                jsonStr = m.group(3);
            }

            byte[] bs = Base64.getDecoder().decode(jsonStr);
            ByteArrayInputStream bin = new ByteArrayInputStream(bs);
            try {
                return ImageIO.read(bin);
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    @Override
    public String toHumanReadableString(GuiMappingContext context, Object source) {
        return (String) toJson(context, source);
    }

    @Override
    public Object fromHumanReadableString(GuiMappingContext context, String str) {
        return fromJson(context, null, str);
    }

    @Override
    public boolean isJsonSetter() {
        return false;
    }

    public void setImagePath(Image image, Path path) {
        if (image != null) {
            imageToReference.put(image, path);
        }
    }

    public Path getImagePath(Image image) {
        return imageToReference.get(image);
    }

    /**
     * support object for {@link #getRenderedImage(GuiMappingContext, Object)}
     * @since 1.6.3
     */
    public interface RenderedImageGetterSupport {
        Dimension size(Image image);
        void setSourceImageToTemporaryImage(Image source, BufferedImage temporaryImage);
    }

    /**
     * impl of the support interface
     * @since 1.6.3
     */
    public static class RenderedImageGetterSupportForContext implements RenderedImageGetterSupport {
        protected GuiReprValueImagePane img;
        protected GuiMappingContext context;

        public RenderedImageGetterSupportForContext(GuiReprValueImagePane img, GuiMappingContext context) {
            this.img = img;
            this.context = context;
        }

        @Override
        public Dimension size(Image image) {
            return img.getSize(context, image);
        }

        @Override
        public void setSourceImageToTemporaryImage(Image source, BufferedImage temporaryImage) {
            img.setImagePath(temporaryImage, img.getImagePath(source));
        }
    }

    /**
     * impl. of {@link #getRenderedImage(GuiMappingContext, Object)}
     * @param support the non-null support
     * @param source the source image object
     * @return source or if it is not an {@link RenderedImage}, it renders to a temporary buffered-image.
     * @since 1.6.3
     */
    public static RenderedImage getRenderedImageWithSupport(RenderedImageGetterSupport support, Object source) {
        if (source instanceof RenderedImage) { //including BufferedImage
            return (RenderedImage) source;
        } else if (source instanceof Image) {
            return getBufferedImageWithSupport(support, source);
        } else {
            return null;
        }
    }

    /**
     * impl. of {@link #getBufferedImage(GuiMappingContext, Object)}
     * @param support the non-null support
     * @param source the source image object
     * @return source or if it is not an {@link RenderedImage}, it renders to a temporary buffered-image.
     * @since 1.6.3
     */
    public static BufferedImage getBufferedImageWithSupport(RenderedImageGetterSupport support, Object source) {
        if (source instanceof BufferedImage) {
            return (BufferedImage) source;
        } else if (source instanceof Image image) {
            Dimension size = support.size(image);
            BufferedImage tmp = new BufferedImage(size.width, size.height, BufferedImage.TYPE_4BYTE_ABGR);
            support.setSourceImageToTemporaryImage(image, tmp);
            Graphics2D g = tmp.createGraphics();
            {
                int count = 0;
                while (!g.drawImage(image, 0, 0, null) &&
                        count < 100_000) {
                    try {
                        Thread.sleep(10);
                    } catch (Exception ex) {
                        break;
                    }
                    ++count;
                }
            }
            g.dispose();
            return tmp;
        } else {
            return null;
        }
    }

    public RenderedImage getRenderedImage(GuiMappingContext context, Object source) {
        return getRenderedImageWithSupport(new RenderedImageGetterSupportForContext(this, context), source);
    }

    public BufferedImage getBufferedImage(GuiMappingContext context, Object source) {
        return getBufferedImageWithSupport(new RenderedImageGetterSupportForContext(this, context), source);
    }

    @Override
    public void updateFromGui(GuiMappingContext context, Object newValue, ObjectSpecifier specifier, GuiTaskClock clock) {
        Class<?> valueType = getValueType(context);
        //image conversion
        if (valueType != null) {
            if (BufferedImage.class.isAssignableFrom(valueType)) {
                newValue = getBufferedImage(context, newValue);
            } else if (RenderedImage.class.isAssignableFrom(valueType)) {
                newValue = getBufferedImage(context, newValue);
            }
        }
        super.updateFromGui(context, newValue, specifier, clock);
    }

    @Override
    public void addHistoryValue(GuiMappingContext context, Object value) {
        if (value instanceof Image) {
            Path p = imageToReference.get(value);
            if (p != null) {
                super.addHistoryValue(context, new ImageHistoryEntry(p, (Image) value));
            } else {
                super.addHistoryValue(context, value);
            }
        }
    }


    /** a special value of history entry which has an image, and it's source file path */
    public static class ImageHistoryEntry {
        protected Path path;
        protected Image image;
        protected BiConsumer<Image,Path> imagePathSetter;

        public ImageHistoryEntry(Path path, Image image) {
            this.path = path;
            this.image = image;
        }

        public ImageHistoryEntry(Path path, BiConsumer<Image,Path> imagePathSetter) {
            this.path = path;
            this.imagePathSetter = imagePathSetter;
        }

        /**
         * @return non-null
         */
        public Path getPath() {
            return path;
        }

        public Image getImage() {
            if (image == null && path != null && Files.exists(path)) {
                try {
                    image = ImageIO.read(path.toFile());
                    if (imagePathSetter != null) {
                        imagePathSetter.accept(image, path);
                    }
                } catch (Exception ex) {
                    //
                }
            }
            return image;
        }

        @Override
        public String toString() {
            return path.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImageHistoryEntry that = (ImageHistoryEntry) o;
            return Objects.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }
}
