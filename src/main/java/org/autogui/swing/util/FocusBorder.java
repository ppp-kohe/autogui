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
 * @since 1.7
 */
public class FocusBorder implements Border {
    protected BasicStroke[] strokes;
    /** @since 1.9 */ protected JComponent component;

    @SuppressWarnings("this-escape")
    public FocusBorder(JComponent target) {
        this.component = target;
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
                repainTarget();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repainTarget();
            }
        });
    }

    /**
     * @since 1.9
     */
    protected void repainTarget() {
        if (component != null) {
            var p = SwingUtilities.getUnwrappedParent(component);
            if (p != null) {
                p.repaint();
            } else {
                component.repaint();
            }
        }
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
        if (strokes == null){
            initStrokes();
        }
        if (hasFocus(c)) {
            paintStrokesFocusColor(g, x, y, width, height, SearchTextField.getFocusColor());
        }
    }

    /**
     * @param c the target component
     * @return true if the c has focus
     * @since 1.9
     */
    protected boolean hasFocus(Component c) {
        return c.isFocusOwner();
    }

    /**
     *
     * @param g the graphics
     * @param x stroke origin x
     * @param y stroke origin y
     * @param width stroke size width
     * @param height stroke size height
     * @param focusColor the color
     * @since 1.9
     */
    public void paintStrokesFocusColor(Graphics g, int x, int y, int width, int height, Color focusColor) {
        Color color2 = new Color(focusColor.getRed(), focusColor.getGreen(), focusColor.getBlue(), 150);
        paintStrokes(g, x, y, width, height, color2);
    }

    /**
     * draws a rect with all strokes
     * @param g the graphics context
     * @param x  the left of the rect
     * @param y  the top of the rect
     * @param width the width of the rect
     * @param height the height of the rect
     * @param color painitng color
     * @since 1.9
     */
    public void paintStrokes(Graphics g, int x, int y, int width, int height, Color color) {
        Graphics2D g2 = (Graphics2D) g;
        double ss = getStrokeSize();
        g2.setColor(color);
        RoundRectangle2D rr = new RoundRectangle2D.Double(x + ss / 2f, y + ss / 2f, width - ss, height - ss, ss * 1.3f, ss * 1.3f);
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
