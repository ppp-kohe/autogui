package org.autogui.test.swing.icons;

import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.util.UIManagerUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GuiSwingIconsTest {
    int iconSize;
    BufferedImage image;
    Graphics2D graphics;
    GuiSwingIcons icons;

    @BeforeEach
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
        assertEquals(iconSize, w, "icon width");
        assertEquals(iconSize, h, "icon height");

        icon.paintIcon(null, graphics, 0, 0);

        assertNotEquals(icons.getIcon("No-such-icon!!!!"), icon, "not the fallback icon");

        graphics.dispose();
        Map<Integer, Integer> colorCount = new HashMap<>();
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                colorCount.compute(image.getRGB(i, j), (k, ev) -> ev == null ? 1 : ev + 1);
            }
        }
        assertTrue(colorCount.size() > 1, "not same color");
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
                assertEquals(iconSize, w, "icon width: " + m.getName());
                assertEquals(iconSize, h, "icon height: " + m.getName());

                assertNotEquals(noIcon, icon, "not the fallback icon: " + m.getName());
                ++count;
            }
        }
        assertEquals(103, count, "icons");
    }
}
