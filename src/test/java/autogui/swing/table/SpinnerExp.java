package autogui.swing.table;

import javax.swing.*;

public class SpinnerExp {
    public static void main(String[] args) {
        JFrame frame = new JFrame();

        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        JPanel panel = new JPanel();
        panel.add(spinner);
        frame.getContentPane().add(panel);

        frame.setVisible(true);
    }
}
