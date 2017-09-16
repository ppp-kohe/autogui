package autogui.swing;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PopupExp {
    public static void main(String[] args) {
        JFrame frame = new JFrame();

        JPanel pane = new JPanel();
        {

            JPopupMenu menu = new JPopupMenu();
            menu.add("hello");
            menu.add("world");

            JButton btn = new JButton("hello");
            btn.addActionListener((e) -> {
                menu.show(null, 0, 0);
            });
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        menu.show(btn, 20, 20);
                    }
                }
            });
            pane.add(btn);

            JTextField fld = new JTextField(20);
            fld.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        menu.show(fld, e.getX(), e.getY());
                    }
                }
            });
            pane.add(fld);
        }
        frame.add(pane);
        frame.pack();

        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
