package autogui.swing;

import autogui.swing.util.PopupExtensionText;

import javax.swing.*;
import javax.swing.text.Keymap;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public class TextExp {
    public static void main(String[] args) {
        JFrame frame = new JFrame("hello");
        {
            JTextPane pane = new JTextPane();
            PopupExtensionText.installDefault(pane);
            pane.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {

                    if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_L) {
                        key(pane.getKeymap());

                        action(pane.getActionMap());
                        Arrays.stream(pane.getEditorKit().getActions())
                            .forEach(a -> System.out.println(" editor action: " + a.getValue(Action.NAME)));
                        input(pane.getInputMap());
                        Arrays.stream(pane.getRegisteredKeyStrokes())
                            .forEach(k -> System.out.println(" reg key : " + k));

                    }
                }
            });
            frame.setContentPane(new JScrollPane(pane));
        }
        frame.setSize(1000, 600);
        frame.setVisible(true);
    }

    public static void key(Keymap keymap) {
        System.out.println("------------- " + keymap.getName());
        Action[] as = keymap.getBoundActions();
        if (as != null) {
            Arrays.stream(as)
                    .forEach(a -> System.out.println(" action: " + a.getValue(Action.NAME)));
        }
        KeyStroke[] ks = keymap.getBoundKeyStrokes();
        if (ks != null) {
            Arrays.stream(ks)
                    .forEach(k -> System.out.println(" key : " + k));
        }
        if (keymap.getResolveParent() != null) {
            key(keymap.getResolveParent());
        }
    }

    public static void action(ActionMap actionMap) {
        System.out.println("actionMap -------------- " + actionMap.getClass());
        Object[] ks = actionMap.allKeys();
        if (ks != null) {
            Arrays.stream(ks)
                    .forEach(k -> System.out.println("actionMap " + k + " -> " + actionMap.get(k)));
        }
    }

    public static void input(InputMap inputMap) {
        System.out.println("inputMap -------------- " + inputMap.getClass());
        KeyStroke[] ks = inputMap.allKeys();
        if (ks != null) {
            Arrays.stream(ks)
                    .forEach(k -> System.out.println("inputMap " + k + " -> " + inputMap.get(k)));
        }
    }
}
