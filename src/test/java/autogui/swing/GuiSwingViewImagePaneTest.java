package autogui.swing;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprValueImagePane;
import autogui.base.type.GuiTypeMemberProperty;
import autogui.base.type.GuiTypeValue;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class GuiSwingViewImagePaneTest extends GuiSwingTestCase {
    public static void main(String[] args) throws Exception {
        new GuiSwingViewImagePaneTest().test();
    }

    @Test
    public void test() throws Exception {
        GuiSwingViewImagePane img = new GuiSwingViewImagePane();

        ImageProp prop = new ImageProp();
        GuiMappingContext context = new GuiMappingContext(prop, new GuiReprValueImagePane());

        JComponent component = runGet(() -> {
            JComponent comp = img.createView(context);
            testFrame(comp).setSize(1000, 1000);
            return comp;
        });

        context.updateSourceFromRoot();

        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = image.createGraphics();
        {
            g.setColor(new Color(200, 100, 100));
            g.fillRect(0, 0, 300, 400);

            g.setColor(new Color(100, 170, 120));
            RoundRectangle2D.Float r = new RoundRectangle2D.Float(
                    20, 20, 60, 60, 10, 10
            );
            g.fill(r);
        }
        g.dispose();
        Thread.sleep(1000);
        prop.img = image;
        context.updateSourceFromRoot();

        BufferedImage image2 = new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR);
        g = image2.createGraphics();
        {
            g.setColor(new Color(100, 200, 100));
            g.fillRect(0, 0, 300, 400);

            g.setColor(new Color(170, 110, 120));
            RoundRectangle2D.Float r = new RoundRectangle2D.Float(
                    20, 20, 60, 60, 10, 10
            );
            g.fill(r);
        }
        g.dispose();
        Thread.sleep(1000);
        GuiSwingViewImagePane.ImagePropertyPane imgPane = runQuery(component,
                query(JScrollPane.class, 0)
                        .cat(JViewport.class, 0)
                        .cat(GuiSwingViewImagePane.ImagePropertyPane.class, 0));
        run(() -> imgPane.setImage(image2));
        System.err.println(imgPane);

        Thread.sleep(1000);
        Assert.assertEquals(image2, prop.img);
    }

    public static class ImageProp extends GuiTypeMemberProperty {
        public BufferedImage img;
        public ImageProp() {
            super("hello");
            setType(new GuiTypeValue(BufferedImage.class));

            img = new BufferedImage(300, 400, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = img.createGraphics();
            {
                g.setColor(new Color(20, 0, 0, 100));
                g.fillRect(0, 0, 300, 400);

                g.setColor(new Color(100, 100, 170));
                RoundRectangle2D.Float r = new RoundRectangle2D.Float(
                        20, 20, 270, 370, 10, 10
                );
                g.fill(r);
            }
            g.dispose();
        }

        @Override
        public Object executeGet(Object target, Object prevValue) throws Exception {
            return compareGet(prevValue, img);
        }

        @Override
        public Object executeSet(Object target, Object value) throws Exception {
            this.img = (BufferedImage) value;
            System.err.println("update " + img);
            return null;
        }

        @Override
        public boolean isWritable() {
            return true;
        }
    }
}
