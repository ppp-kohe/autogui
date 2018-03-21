package autogui.swing.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** a shared window manager for the setting panel */
public class SettingsWindow {
    protected static SettingsWindow instance;

    public SettingsFrame window;
    protected SettingSupport settingSupport;

    public static SettingsWindow get() {
        if (instance == null) {
            instance = new SettingsWindow();
        }
        return instance;
    }

    public SettingsWindow() {
        window = new SettingsFrame();
        window.setType(Window.Type.UTILITY);
        window.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (settingSupport != null) {
                    settingSupport.resized(window);
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (settingSupport != null) {
                    settingSupport.moved(window);
                }
            }

            @Override
            public void componentShown(ComponentEvent e) { }

            @Override
            public void componentHidden(ComponentEvent e) { }
        });
    }

    public class SettingsFrame extends JFrame {
        protected boolean shown;

        public void setShown(boolean shown) {
            this.shown = shown;
        }

        public boolean isShown() {
            return shown;
        }
    }

    public JFrame getWindow() {
        return window;
    }

    public void show() {
        window.setShown(true);
        window.setVisible(true);
    }

    public void show(JComponent sender) {
        if (sender != null) {
            window.setLocationRelativeTo(sender);
        }
        if (settingSupport != null) {
            settingSupport.setup(window);
        }
        show();
    }

    public void show(String title, JComponent sender, JComponent contentPane) {
        show(title, sender, contentPane, null);
    }

    public void show(String title, JComponent sender, JComponent contentPane, SettingSupport settingSupport) {
        window.setTitle(title);
        window.setContentPane(contentPane);
        window.pack();
        this.settingSupport = settingSupport;
        show(sender);
    }

    public interface SettingSupport {
        void resized(JFrame window);
        void moved(JFrame window);
        void setup(JFrame window);
    }



    ///////// header text:

    /** a group of labels */
    public static class LabelGroup {
        protected List<JComponent> names = new ArrayList<>();
        protected int align;
        protected JComponent pane;

        public LabelGroup(JComponent pane) {
            this(pane, FlowLayout.RIGHT);
        }

        public LabelGroup(JComponent pane, int align) {
            this.align = align;
            this.pane = pane;
            if (pane != null) {
                pane.setLayout(new ResizableFlowLayout(false).setFitHeight(true));
            }
        }

        public LabelGroup addRow(String label, JComponent content) {
            JPanel pane = new JPanel(new FlowLayout(align));
            pane.add(new JLabel(label));
            return addRow(pane, content);
        }

        public LabelGroup addRowFixed(String label, JComponent content) {
            JPanel namePane = new JPanel(new FlowLayout(align));
            namePane.add(new JLabel(label));

            ResizableFlowLayout.add(pane,
                    ResizableFlowLayout.create(true)
                            .add(addName(namePane))
                            .add(content).getContainer(), false);
            return this;
        }

        public LabelGroup addRow(JComponent label, JComponent content) {
            ResizableFlowLayout.add(pane,
                    ResizableFlowLayout.create(true)
                            .add(addName(label))
                            .add(content, true).getContainer(), false);
            return this;
        }

        public JComponent addName(JComponent component) {
            names.add(component);
            return component;
        }

        public void fitWidth() {
            Dimension dim = new Dimension();
            for (JComponent item : names){
                Dimension p = item.getPreferredSize();
                dim.width = Math.max(p.width, dim.width);
            }

            for (JComponent item : names){
                Dimension p = item.getPreferredSize();
                p.width = dim.width;
                item.setPreferredSize(p);
            }
        }
    }

    ////////////// color chooser

    protected static ColorWindow colorWindow;

    public static ColorWindow getColorWindow() {
        if (colorWindow == null) {
            colorWindow = new ColorWindow();
        }
         return colorWindow;
    }

    /** a window holder for the color chooser */
    public static class ColorWindow {
        protected JFrame window;

        protected Consumer<Color> callback;
        protected JColorChooser colorChooser;
        protected boolean updateDisabled;

        public ColorWindow() {
            window = new JFrame();
            window.setType(Window.Type.UTILITY);
            colorChooser = new JColorChooser();
            colorChooser.getSelectionModel().addChangeListener(e ->
                    update(colorChooser.getColor()));
            window.setContentPane(colorChooser);
            window.pack();
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    callback = null;
                }
            });
        }

        public void show(Component sender, Color color, Consumer<Color> callback) {
            this.callback = callback;
            if (sender != null) {
                window.setLocationRelativeTo(sender);
            }
            if (color != null) {
                updateDisabled = true;
                colorChooser.setColor(color);
                updateDisabled = false;
            }
            window.setVisible(true);
        }

        public JFrame getWindow() {
            return window;
        }

        public void update(Color c) {
            if (!updateDisabled && callback != null) {
                callback.accept(c);
            }
        }
    }

    /** a color well can be changed by a shared color-panel */
    public static class ColorButton extends JButton implements ActionListener {
        protected Color color;
        protected Consumer<Color> callback;

        public ColorButton(Color color, Consumer<Color> callback) {
            this.color = color;
            this.callback = callback;
            setPreferredSize(new Dimension(32, 18));
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ColorWindow w = getColorWindow();
            w.show(this, color, this::setColor);
        }

        public void setColor(Color color) {
            setColorWithoutUpdate(color);
            callback.accept(color);
        }

        public void setColorWithoutUpdate(Color color) {
            this.color = color;
            repaint();
        }

        public Color getColor() {
            return color;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(color);
            Dimension size = new Dimension(Math.min(32, getWidth() - 8), Math.min(14, getHeight() - 8));
            Rectangle2D.Float rect = new Rectangle2D.Float(getWidth() / 2 - size.width / 2, getHeight() / 2 - size.height / 2,
                    size.width, size.height);

            g2.fill(rect);
            g2.setColor(Color.gray);
            g2.draw(rect);
        }
    }
}
