package autogui;

import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.util.SettingsWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class FileLoadExp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new FileLoadExp()::run);
    }

    public void run() {
        JFrame frame = new JFrame("");
        frame.setSize(500, 500);

        JComboBox<?> box = new JComboBox<>(new String[] {"Hello", "world"});

        JPanel pane = new JPanel();
        pane.add(new JButton(new AbstractAction() {
            {
                putValue(NAME, "OPEN");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                System.err.println(SettingsWindow.getFileDialogManager().showOpenDialog(pane, box));
            }
        }));
        pane.add(new JButton(new AbstractAction() {
            {
                putValue(NAME, "SAVE");
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                System.err.println(SettingsWindow.getFileDialogManager().showSaveDialog(pane, null, null));
            }
        }));

        frame.setContentPane(pane);
        frame.setVisible(true);
    }
}
