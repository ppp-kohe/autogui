package autogui.swing;

import javax.swing.*;
import java.awt.*;

public class BackgroundExp {
    public static void main(String[] args) {
        BackgroundExp exp = new BackgroundExp();
        SwingUtilities.invokeLater(exp::run);
    }

    public void run() {
        JFrame frame = new JFrame("hello");

        JPanel pane = new JPanel();
        pane.setPreferredSize(new Dimension(1000, 1000));

        pane.setBackground(Color.gray);
        {
            JTextField fld = new JTextField(20);
            fld.setBackground(Color.red);
            fld.setOpaque(false);
            fld.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(10, 10, 10, 10),
                        BorderFactory.createCompoundBorder(
                                UIManager.getBorder("Table.focusCellHighlightBorder"),
                                BorderFactory.createEmptyBorder(10, 10, 10, 10))));
            pane.add(fld);

            pane.add(new JButton("hello"));
        }

        frame.setContentPane(pane);

        frame.pack();
        frame.setVisible(true);
    }
}
