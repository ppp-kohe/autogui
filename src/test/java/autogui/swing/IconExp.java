package autogui.swing;

import autogui.swing.icons.GuiSwingIcons;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IconExp {
    public static void main(String[] args) {
        IconExp exp = new IconExp();
        SwingUtilities.invokeLater(exp.grasp(exp::run));
    }

    public interface ErrorRunnable {
        void run() throws Throwable;
    }

    public Runnable grasp(ErrorRunnable r) {
        return () -> {
            try {
                r.run();
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    public void run() throws Exception {
        JFrame frame = new JFrame("test");
        {


//            JButton btn2 = button("/Users/kohe/work/autogui-icon@4x.png", "Run 128");
//            JButton btn3 = button("/Users/kohe/work/autogui-icon.png", "Run 800");

            List<JButton> btns = new ArrayList<>();

            JButton btn1 = button("src/main/resources/autogui/swing/icons/autogui-icon@2x.png", "Run 64");
            btns.add(btn1);

            GuiSwingIcons.getInstance().getIconWords().forEach(n -> btns.add(buttonRes(n)));
            btns.add(buttonRes("NOTHING"));
            GuiSwingIcons.getInstance().getSynonyms().keySet().stream()
                    .sorted()
                    .forEach(k -> btns.add(buttonRes(k)));

            //JToolBar bar = new JToolBar();
            //bar.setFloatable(false);
            //btns.forEach(bar::add);
            JPanel pane = new JPanel();//new JPanel(new BorderLayout());
            btns.forEach(pane::add);
            frame.setContentPane(pane);
        }
        frame.setSize(500, 500);
        frame.setVisible(true);
    }

    private JButton buttonRes(String name) {
        Icon icon = GuiSwingIcons.getInstance().getIcon(name);
        Action act = new Action();
        act.putValue(Action.NAME, name);
        act.putValue(Action.SMALL_ICON, icon);
        JButton btn  =new JButton(act);
        btn.setBorderPainted(false);
        btn.setHideActionText(false);

        btn.setDisabledSelectedIcon(GuiSwingIcons.getInstance().getDefaultIcon(name));
        btn.setDisabledIcon(GuiSwingIcons.getInstance().getDefaultIcon(name));

        System.err.printf("GuiSwingIcon: icon=%s, dis=%s, press=%s, roll=%s, sel=%s, disSel=%s, rollSel=%s\n",
                btn.getIcon(), btn.getDisabledIcon(), btn.getPressedIcon(), btn.getRolloverIcon(), btn.getSelectedIcon(),
                btn.getDisabledSelectedIcon(), btn.getRolloverSelectedIcon());
        return btn;
    }

    private JButton button(String path, String name) throws Exception {
        BufferedImage img = ImageIO.read(Paths.get(path).toFile());
        Image scaledImg = img.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(scaledImg);
        //MyIcon icon = new MyIcon(img, 32, 32);

        Action act = new Action();
        act.putValue(Action.NAME, name);
        act.putValue(Action.SMALL_ICON, icon);
        JButton btn  =new JButton(act);
        btn.setBorderPainted(false);
        btn.setHideActionText(false);

        System.err.printf("ImageIcon: icon=%s, dis=%s, press=%s, roll=%s, sel=%s, disSel=%s, rollSel=%s\n",
                btn.getIcon(), btn.getDisabledIcon(), btn.getPressedIcon(), btn.getRolloverIcon(), btn.getSelectedIcon(),
                btn.getDisabledSelectedIcon(), btn.getRolloverSelectedIcon());
        return btn;
    }

    class Action extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    }

    class MyIcon implements Icon {
        protected BufferedImage img;
        protected int width;
        protected int height;
        protected AffineTransformOp op;

        public MyIcon(BufferedImage img, int width, int height) {
            this.img = img;
            this.width = width;
            this.height = height;
            float iw = img.getWidth();
            float ih = img.getHeight();
            RenderingHints hints = new RenderingHints(new HashMap<>());
            hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            op = new AffineTransformOp(
                    new AffineTransform(width / iw, 0, 0, height / ih, 0, 0),
                    hints);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g;
            ///g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(img, op, 0, 0);
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }
}
