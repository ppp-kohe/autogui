package autogui.demo;

import javax.swing.*;
import javax.swing.plaf.metal.MetalTextFieldUI;
import java.awt.event.ActionEvent;

public class TestKey {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new TestKey()::run);
    }

    public void run() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            System.out.println(UIManager.getLookAndFeel().getDefaults().getUIClass("TextFieldUI"));
            System.out.println(new MetalTextFieldUI().getEditorKit(null));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        JFrame f = new JFrame();
        {
            JPanel pane = new JPanel();
            JTextField fld = new JTextField("hello");
            pane.add(fld);
            /*
            */
            Action a = new AbstractAction() {
                {
                    putValue(NAME, "Hello");
                }
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (KeyStroke s : fld.getInputMap().allKeys()) {
                        System.err.println(s);
                    }
                }
            };
            JButton b = new JButton(a);
            pane.add(b);
            f.setContentPane(pane);
        }
        f.pack();
        f.setVisible(true);
    }
}
