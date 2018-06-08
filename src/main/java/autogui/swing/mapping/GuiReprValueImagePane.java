package autogui.swing.mapping;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;
import autogui.base.mapping.GuiTaskClock;

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
 * the representation depends on some AWT classes (java.desktop module) */
public class GuiReprValueImagePane extends GuiReprValue {
    protected Map<Image,Path> imageToReference = new WeakHashMap<>();

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
        if (image == null) {
            return new Dimension(1, 1);
        } else if (image instanceof BufferedImage) {
            BufferedImage bufferedImage = (BufferedImage) image;
            return new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight());
        } else {
            return new ImageSizeGetter().getWithWait(image);
        }
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

            if (width <= 1 || height <= 1) {
                try {
                    while (!finish.get()) {
                        Thread.sleep(10);
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
        public boolean imageUpdate(Image img, int infoflags,
                                   int x, int y, int width, int height) {
            if ((infoflags & ImageObserver.ERROR) != 0) {
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
            boolean f = ((infoflags & ImageObserver.ALLBITS) != 0)
                    || ((infoflags & (ImageObserver.WIDTH | ImageObserver.HEIGHT)) != 0);
            finish.set(f);
            return !f;
        }
    }

    /**
     *
     * @param context a context holds the representation
     * @param source  the converted object {@link Image} or {@link ImageHistoryEntry}
     * @return image bytes encoded as PNG base64 String.
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

    @Override
    public Object fromJson(GuiMappingContext context, Object target, Object json) {
        if (json instanceof String) {
            String jsonStr = (String) json;

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

    public RenderedImage getRenderedImage(GuiMappingContext context, Object source) {
        if (source instanceof RenderedImage) { //including BufferedImage
            return (RenderedImage) source;
        } else if (source instanceof Image) {
            return getBufferedImage(context, source);
        } else {
            return null;
        }
    }

    public BufferedImage getBufferedImage(GuiMappingContext context, Object source) {
        if (source instanceof BufferedImage) {
            return (BufferedImage) source;
        } else if (source instanceof Image) {
            Image image = (Image) source;
            Dimension size = getSize(context, image);
            BufferedImage tmp = new BufferedImage(size.width, size.height, BufferedImage.TYPE_4BYTE_ABGR);

            Path exPath = imageToReference.get(image);
            if (exPath != null) {
                imageToReference.put(tmp, exPath);
            }

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
            if (image == null && Files.exists(path)) {
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
