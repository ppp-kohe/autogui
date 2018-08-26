package org.autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * a layout manager for lining up components horizontally or vertically.
 *
 *
 *  <pre>
 *      orientation = true; //horizontal
 *      [comp1] [comp2] ...
 *  </pre>
 *
 *  <pre>
 *      orientation = false; //vertical
 *      [comp1]
 *      [comp2]
 *      ...
 *  </pre>
 *
 *  <p>
 *  Width or height of each component can be resizable or fixed.
 *   If the size is fixed, the manager refers preferred size and minimum size of a component.
 *
 *  <p>
 * To manage components, it needs to manually add them to the manager.
 * {@link #add(Container, Component, boolean)} is the convenient method for the task.
 *   <pre>
 *       JPanel pane = new JPanel();
 *       ResizableFlowLayout layout = new ResizableFlowLayout(true); //horizontal
 *       pane.setLayout(layout);
 *
 *       ResizableFlowLayout.add(pane, new JLabel("Message:"), false); //fixed
 *       ResizableFlowLayout.add(pane, new JTextField(""), true);  //resizable
 *   </pre>
 *
 *  <p>
 *    {@link LayoutAppender} is a factory for creating a pane with the layout.
 *      It can be created by {@link #create(boolean)}.
 *
 *    <pre>
 *        JPanel pane = ResizableFlowLayout.create(false) //vertical
 *          .add(ResizableFlowLayout.create(true)
 *                  .add(new JLabel("Message:")).add(new JTextField(""), true).getContainer())
 *          .add(ResizableFlowLayout.create(true)
 *                  .add(new JLabel("Name:")).add(new JTextField(""), true).getContainer())
 *          .getContainer();
 *    </pre>
 */
public class ResizableFlowLayout implements LayoutManager {
    protected int margin = 1;
    protected boolean orientation = true;
    protected boolean fitHeight = false;
    protected Set<Component> resizable = new HashSet<Component>();

    public static ResizableFlowLayout.LayoutAppender<JPanel> create(boolean horizontalOrientation) {
        ResizableFlowLayout r = new ResizableFlowLayout(horizontalOrientation);
        if (!horizontalOrientation) {
            r.setFitHeight(true);
        }
        JPanel pane = new JPanel();
        pane.setOpaque(false);
        return r.withContainer(pane);
    }

    public static ResizableFlowLayout.LayoutAppender<JPanel> create(boolean horizontalOrientation, boolean fitOpposite) {
        ResizableFlowLayout r = new ResizableFlowLayout(horizontalOrientation);
        r.setFitHeight(fitOpposite);
        JPanel pane = new JPanel();
        pane.setOpaque(false);
        return r.withContainer(pane);
    }

    public ResizableFlowLayout(boolean orientation, int margin) {
        this.margin = margin;
        this.orientation = orientation;
    }
    public ResizableFlowLayout(boolean orientation) {
        this.orientation = orientation;
    }

    /**
     * Here, the word "height" means opposite orientation of lining orientation.
     * @param fitHeight the flag
     * @return this
     */
    public ResizableFlowLayout setFitHeight(boolean fitHeight) {
        this.fitHeight = fitHeight;
        return this;
    }
    public boolean isFitHeight() {
        return fitHeight;
    }

    public boolean isVertical() {
        return !orientation;
    }
    public boolean isHorizontal() {
        return orientation;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }
    public int getMargin() {
        return margin;
    }

    public void setResizable(Component comp, boolean r) {
        if (r) {
            resizable.add(comp);
        } else {
            resizable.remove(comp);
        }
    }
    public boolean isResizable(Component comp) {
        return resizable.contains(comp);
    }

    public static int insetsWidth(Insets insets) {
        return insets.left + insets.right;
    }
    public static int insetsHeight(Insets insets) {
        return insets.top + insets.bottom;
    }


    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        ResizedWidth rw = new ResizedWidth(parent, false);

        float resizedResSum = (orientation ? parent.getWidth() : parent.getHeight()) - rw.fixedSum;
        int x = orientation ? insets.left : insets.top;
        boolean first = true;
        for (Component c : parent.getComponents()) {
            Dimension size = c.getPreferredSize();
            int w = orientation ? size.width : size.height;
            if (first) {
                first = false;
            } else {
                x += margin;
            }
            int y;
            int h;
            if (fitHeight) {
                y = orientation ? insets.top : insets.left;
                h = (orientation ? parent.getHeight() : parent.getWidth())
                        - (orientation ? insetsHeight(insets) : insetsWidth(insets));
            } else {
                y = (rw.height / 2) - ((orientation ? size.height : size.width) / 2);
                h = orientation ? size.height : size.width;
            }
            c.setLocation(orientation ? x : y, orientation ? y : x);
            if (isResizable(c)) {
                w = ((int) ((resizedResSum / rw.resSum) * (orientation ? size.getWidth() : size.getHeight())));
            }
            c.setSize(Math.max(c.getMinimumSize().width, (orientation ? w : h)),
                    Math.max(c.getMinimumSize().height, (orientation ? h : w)));
            x += orientation ? c.getWidth() : c.getHeight();
        }
    }

    /**
     * internal state for calculating layout.
     */
    public class ResizedWidth {
        /** summation of base size of lining orientation; resizable component's minimum or preferred size */
        public float resSum;
        /** summation of fixed size parts of lining orientation; insets, margin and non resizable component's size  */
        public float fixedSum;
        /** max size of opposite orientation of lining orientation and insets*/
        public int height;

        public ResizedWidth(Container parent, boolean minimum) {
            Insets insets = parent.getInsets();
            resSum = 0;
            fixedSum = orientation ? insetsWidth(insets) : insetsHeight(insets);
            boolean first = true;
            for (Component c : parent.getComponents()) {
                if (c.isVisible()) {
                    Dimension size = (minimum ? c.getMinimumSize() : c.getPreferredSize());
                    int w = orientation ? size.width : size.height;
                    if (first) {
                        first = false;
                    } else {
                        fixedSum += margin;
                    }
                    if (isResizable(c)) {
                        resSum += w;
                    } else {
                        fixedSum += w;
                    }
                    height = Math.max(orientation ? size.height : size.width, height);
                }
            }
            height += orientation ? insetsHeight(insets): insetsWidth(insets);
        }
        public Dimension getSize() {
            return orientation ? new Dimension((int) (fixedSum + resSum), height) :
                    new Dimension(height, (int) (fixedSum + resSum));
        }
    }

    public Dimension minimumLayoutSize(Container parent) {
        return new ResizedWidth(parent, true).getSize();
    }

    public Dimension preferredLayoutSize(Container parent) {
        return new ResizedWidth(parent, false).getSize();
    }

    public void addLayoutComponent(String name, Component comp) {
    }

    public void removeLayoutComponent(Component comp) {
    }

    public static void add(Container parent, Component comp, boolean resizable) {
        parent.add(comp);
        LayoutManager m = parent.getLayout();
        ResizableFlowLayout rfl = null;
        if (!(m instanceof ResizableFlowLayout)) {
            rfl = new ResizableFlowLayout(true);
            parent.setLayout(rfl);
        } else {
            rfl = (ResizableFlowLayout) m;
        }
        rfl.setResizable(comp, resizable);
    }

    public static void remove(Container parent, Component comp) {
        LayoutManager m = parent.getLayout();
        if (m instanceof ResizableFlowLayout) {
            ((ResizableFlowLayout) m).remove(comp);
        }
    }

    public void remove(Component comp) {
        resizable.remove(comp);
    }

    public <CT extends Container> LayoutAppender<CT> withContainer(CT parent) {
        return new LayoutAppender<>(parent, this);
    }

    /** a class for providing fluent interface for constructing {@link ResizableFlowLayout} */
    public static class LayoutAppender<ContainerType extends Container> {
        ContainerType parent;
        ResizableFlowLayout layout;
        public LayoutAppender(ContainerType parent, ResizableFlowLayout layout) {
            this.parent = parent;
            this.layout = layout;
            parent.setLayout(layout);
        }
        public LayoutAppender<ContainerType> withMargin(int m) {
            layout.setMargin(m);
            return this;
        }
        public LayoutAppender<ContainerType> add(Component... comp) {
            for (Component c : comp) {
                parent.add(c);
            }
            return this;
        }
        public LayoutAppender<ContainerType> add(Component comp, boolean resizable) {
            parent.add(comp);
            layout.setResizable(comp, resizable);
            return this;
        }
        public ContainerType getContainer() {
            return parent;
        }

        public LayoutAppender<ContainerType> withInsets(int x, int y) {
            parent.getInsets().set(y, x, y, x);
            return this;
        }
        /**
         * Assume ContainerType is {@link JComponent}.
         * @param x the border size w
         * @param y the border size y
         * @return this
         */
        public LayoutAppender<ContainerType> withBorder(int x, int y) {
            JComponent p = (JComponent) parent;
            p.setBorder(BorderFactory.createEmptyBorder(y, x, y, x));
            return this;
        }
    }
}
