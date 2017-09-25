package autogui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.Method;

public class PopoverExp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PopoverExp().run());
        }

    public void run() {
        RoundRectangle2D rr = new RoundRectangle2D.Double(10, 10, 700 - 20, 600 - 20, 20, 20);
        JFrame frm = new JFrame("test");

        //JPanel pane = new UndecoratedPane();
        JPanel pane = new JPanel() {
            /*
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);



                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setColor(new Color(100, 100, 100, 100));
                g2.fill(rr);
            }*/
        };
        pane.add(new JButton("Hello"));
        //pane.setOpaque(true);
        frm.getContentPane().add(pane);

        JPopupMenu menu = new JPopupMenu();
        {
            menu.add("Hello");

            JPanel mPane = new JPanel();
            {
                mPane.add(new JLabel("Value"));
                mPane.add(new JSpinner(new SpinnerNumberModel()));
                mPane.setPreferredSize(new Dimension(300, 100));
            }
            mPane.setOpaque(false);

            menu.add(mPane);


            menu.add("World");

            JLabel label = new JLabel("Hello, world");
            JPanel lPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
            lPane.add(label);
            label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            menu.add(lPane);

        }

        pane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });


        DraggingWindowTracker t = new DraggingWindowTracker(frm);
        frm.getContentPane().addMouseMotionListener(t);
        frm.getContentPane().addMouseListener(t);

        //frm.setUndecorated(true);
//        try {
//            Class<?> awt = Class.forName("com.sun.awt.AWTUtilities");
//            Method setWindowOpacity = awt.getMethod("setWindowOpaque", Window.class, boolean.class);
//            setWindowOpacity.invoke(null, frm, false);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
        //frm.setShape(rr);
        //frm.setOpacity(0);

        JPanel gPane = new JPanel();
        gPane.setOpaque(false);
        frm.setGlassPane(gPane);
        gPane.add(new UndecoratedPane());
        gPane.setVisible(true);

        //frm.setBackground(new Color(0, 0, 0, 0));

        frm.setSize(700, 600);
        frm.setVisible(true);
    }


    public static class UndecoratedPane extends JPanel {
        private static final long serialVersionUID = 1L;
        protected Color color;
        public UndecoratedPane() {
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            color = new Color(238, 238, 238, 200);
            //setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setVisible(true);
        }
        @Override
        protected void paintComponent(Graphics g) {
            RoundRectangle2D rr = new RoundRectangle2D.Float(10, 10, getWidth() - 20, getHeight() - 20, 20, 20);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(color);
            g2.fill(rr);
        }
    }

    public static class DraggingWindowTracker extends MouseAdapter {
        protected Window frame;
        protected int sx;
        protected int sy;

        public DraggingWindowTracker(Window frame) {
            this.frame = frame;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            sx = e.getX();
            sy = e.getY();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            int dx = e.getX() - sx;
            int dy = e.getY() - sy;
            Point loc = frame.getLocation();
            frame.setLocation(loc.x + dx, loc.y + dy);
        }
    }
}
