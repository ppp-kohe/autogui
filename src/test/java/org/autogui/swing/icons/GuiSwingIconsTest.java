package org.autogui.swing.icons;

import org.autogui.swing.util.UIManagerUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class GuiSwingIconsTest {
    int iconSize;
    BufferedImage image;
    Graphics2D graphics;
    GuiSwingIcons icons;
    @Before
    public void setUp() {
        icons = new GuiSwingIcons();
        iconSize = UIManagerUtil.getInstance().getIconSize();
        image = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_RGB);
        graphics = image.createGraphics();
    }

    @Test
    public void testGetIcons() {

        var icon = icons.getIcon("add");
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        Assert.assertEquals("icon width", iconSize, w);
        Assert.assertEquals("icon height", iconSize, h);

        icon.paintIcon(null, graphics, 0, 0);

        Assert.assertNotEquals("not the fallback icon", icons.getIcon("No-such-icon!!!!"), icon);

        graphics.dispose();
        Map<Integer, Integer> colorCount = new HashMap<>();
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                colorCount.compute(image.getRGB(i, j), (k, ev) -> ev == null ? 1 : ev + 1);
            }
        }
        Assert.assertTrue("not same color", colorCount.size() > 1);
    }

    @Test
    public void testGetIconNamed() throws Exception {
        var noIcon = icons.getIcon("No-such-icon!!!!");
        int count = 0;
        for (Method m : icons.getClass().getMethods()) {
            if (m.getName().startsWith("get") && m.getName().endsWith("Icon") &&
                    m.getReturnType().equals(Icon.class) && m.getParameterCount() == 0) {
                var icon = (Icon) m.invoke(icons);
                int w = icon.getIconWidth();
                int h = icon.getIconHeight();
                Assert.assertEquals("icon width: " + m.getName(), iconSize, w);
                Assert.assertEquals("icon height: " + m.getName(), iconSize, h);

                Assert.assertNotEquals("not the fallback icon: " + m.getName(), noIcon, icon);
                ++count;
            }
        }
        Assert.assertEquals("icons", 103, count);
    }
}
