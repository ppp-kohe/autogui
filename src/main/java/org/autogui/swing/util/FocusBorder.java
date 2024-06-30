package org.autogui.swing.util;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;

/**
 * border for indicating focusing
 * @since 1.6.3
 */
public class FocusBorder implements Border {
    protected Color focusColor;
    protected BasicStroke[] strokes;

    public FocusBorder(JComponent target) {
        if (target != null) {
            installListener(target);
        }
    }

    /**
     * adding an {@link FocusListener} for repainting the pane when the focus changed
     * @param target the target comopnent
     */
    public void installListener(JComponent target) {
        target.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                target.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                target.repaint();
            }
        });
    }

    public void initFocusColor() {
        focusColor = SearchTextField.getFocusColor();
    }

    public void initStrokes() {
        strokes = new BasicStroke[3];
        Arrays.fill(strokes, new BasicStroke(strokes.length / 2.0f));
    }

    public float getStrokeSize() {
        return UIManagerUtil.getInstance().getScaledSizeFloat(3f);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (focusColor == null) {
            initFocusColor();
        }
        if (strokes == null){
            initStrokes();
        }

        if (c.hasFocus()) {
            paintStrokes(g, x, y, width, height);
        }
    }

    public void paintStrokes(Graphics g, int x, int y, int width, int height) {
        double ss = getStrokeSize();
        RoundRectangle2D rr = new RoundRectangle2D.Double(x + ss / 2f, y + ss / 2f, width - ss, height - ss, ss * 1.3f, ss * 1.3f);
        Graphics2D g2 = (Graphics2D) g;
        Color color2 = new Color(focusColor.getRed(), focusColor.getGreen(), focusColor.getBlue(), 150);
        g2.setColor(color2);
        for (BasicStroke s : strokes) {
            g2.setStroke(s);
            g2.draw(rr);
        }
    }

    @Override
    public Insets getBorderInsets(Component c) {
        int s = (int) getStrokeSize();
        return new Insets(s, s, s, s);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
