package autogui.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class JSpinnerTest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new JSpinnerTest()::run);
    }

    public void run() {
        JFrame f = new JFrame("");
        {
            JPanel panel = new JPanel(new BorderLayout());
            {
                JSpinner s = new JSpinner();
                System.out.println(s.getEditor().getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)));
                System.out.println(((JSpinner.DefaultEditor)s.getEditor()).getTextField().getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)));
                panel.add(s);
            }
            f.setContentPane(panel);
        }
        f.setSize(400, 300);
        f.setVisible(true);
    }
}
