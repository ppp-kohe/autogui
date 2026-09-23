package org.autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Objects;
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
    protected boolean orientation;
    /** @since 1.9 */ protected Alignment alignment = Alignment.Mininum;
    protected Set<Component> resizable = new HashSet<>();

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

    public ResizableFlowLayout(boolean horizontalOrientation, int margin) {
        this.margin = margin;
        this.orientation = horizontalOrientation;
    }
    public ResizableFlowLayout(boolean horizontalOrientation) {
        this.orientation = horizontalOrientation;
    }

    /**
     * Here, the word "height" means opposite orientation of lining orientation.
     * it mens {@link #setAlignment(Alignment)} with {@link Alignment#Filling}.
     * @param fitHeight the flag
     * @return this
     */
    public ResizableFlowLayout setFitHeight(boolean fitHeight) {
        return setAlignment(fitHeight ? Alignment.Filling : Alignment.Center);
    }
    public boolean isFitHeight() {
        return alignment.equals(Alignment.Filling);
    }

    /**
     * @return the current alignment. the default value is {@link Alignment#Mininum}
     * @since 1.9
     */
    public Alignment getAlignment() {
        return alignment;
    }

    /**
     * @param alignment the setting of the alignment of all components
     * @return this
     * @since 1.9
     */
    public ResizableFlowLayout setAlignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    /**
     * @since 1.9
     */
    public enum Alignment {
        /** centering */
        Center,
        /** top or left */
        Mininum,
        /** bottom or right */
        Maximum,
        /** change height or width to the max size */
        Filling,
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

        boolean isHorizontal = orientation;

        float resizedResSum = (isHorizontal ? parent.getWidth() : parent.getHeight()) - rw.fixedSum;
        int x = isHorizontal ? insets.left : insets.top;
        boolean first = true;
        for (Component c : parent.getComponents()) {
            Dimension size = c.getPreferredSize();
            int w = isHorizontal ? size.width : size.height;
            if (first) {
                first = false;
            } else {
                x += margin;
            }
            int y;
            int h;
            if (alignment.equals(Alignment.Filling)) {
                y = isHorizontal ? insets.top : insets.left;
                h = (isHorizontal ? parent.getHeight() : parent.getWidth())
                        - (isHorizontal ? insetsHeight(insets) : insetsWidth(insets));
            } else {
                if (alignment.equals(Alignment.Mininum)) {
                    y = isHorizontal ? insets.top : insets.left;
                } else if (alignment.equals(Alignment.Maximum)) {
                    y = rw.height - (isHorizontal ? insets.bottom : insets.right) - (isHorizontal ? size.height : size.width);
                } else {
                    int topInset = (isHorizontal ? insets.top : insets.left);
                    int hNoInsets = rw.height - topInset - (isHorizontal ? insets.bottom : insets.right);
                    y = topInset + (hNoInsets / 2) - ((isHorizontal ? size.height : size.width) / 2);
                }
                h = isHorizontal ? size.height : size.width;
            }
            c.setLocation(isHorizontal ? x : y, isHorizontal ? y : x);
            if (isResizable(c)) {
                w = ((int) ((resizedResSum / rw.resSum) * (isHorizontal ? size.getWidth() : size.getHeight())));
            }
            c.setSize(Math.max(c.getMinimumSize().width, (isHorizontal ? w : h)),
                    Math.max(c.getMinimumSize().height, (isHorizontal ? h : w)));
            x += isHorizontal ? c.getWidth() : c.getHeight();
        }
    }

    /**
     * internal state for calculating layout.
     */
    public class ResizedWidth {
        /** summation of base size of lining orientation; resizable component's minimum or preferred size */
        public float resSum;
        /** summation of fixed size parts of lining orientation; insets, margin and non-resizable component's size  */
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
                    Dimension minSize = c.getMinimumSize(); //minimumSize might be larger than preferredSize
                    Dimension size = (minimum ? c.getMinimumSize() : c.getPreferredSize());
                    int w = orientation ? Math.max(minSize.width, size.width) : Math.max(minSize.height, size.height);
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
                    height = Math.max(orientation ? minSize.height : minSize.width,
                            Math.max(orientation ? size.height : size.width, height));
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

    public static final String RESIZABLE = "resizable";
    public static final String FIXED = "";

    public void addLayoutComponent(String name, Component comp) {
        setResizable(comp, Objects.equals(RESIZABLE, name));
    }

    public void removeLayoutComponent(Component comp) {
        resizable.remove(comp);
    }

    public static void add(Container parent, Component comp, boolean resizable) {
        LayoutManager m = parent.getLayout();
        ResizableFlowLayout rfl;
        if (!(m instanceof ResizableFlowLayout)) {
            rfl = new ResizableFlowLayout(true);
            parent.setLayout(rfl);
        }
        parent.add(comp, resizable ? RESIZABLE : FIXED);
    }

    public static void remove(Container parent, Component comp) {
        parent.remove(comp);
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
            parent.add(comp, resizable ? RESIZABLE : FIXED);
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
