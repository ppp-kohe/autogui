package autogui.swing.mapping;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** the representation depends on some AWT classes (java.desktop module) */
public class GuiReprValueImagePane extends GuiReprValue {
    @Override
    public boolean matchValueType(Class<?> cls) {
        return Image.class.isAssignableFrom(cls);
    }

    public Image updateValue(GuiMappingContext context, Object value) {
        if (value instanceof Image) {
            return (Image) value;
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
}
