package autogui;

import autogui.base.mapping.GuiReprSet;
import autogui.base.type.GuiTypeBuilder;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingWindow;

import javax.swing.*;

public class AutoGuiShell {
    public static AutoGuiShell get() {
        return new AutoGuiShell();
    }

    public void showWindow(Object o) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            createWindow(o).setVisible(true);
        });
    }

    /**
     * if the current thread is the event dispatching thread, it will immediately create a window for o and return it.
     * otherwise, it invoke the same task to the event dispatching thread and waits it
     * @param o the target object
     * @return the created window for o, with default component-set
     */
    public GuiSwingWindow createWindow(Object o) {
        if (SwingUtilities.isEventDispatchThread()) {
            return GuiSwingWindow.createForObject(o);
        } else {
            GuiSwingWindow[] w = new GuiSwingWindow[1];
            try {
                SwingUtilities.invokeAndWait(() -> {
                    w[0] = GuiSwingWindow.createForObject(o);
                });
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return w[0];
        }
    }
}
