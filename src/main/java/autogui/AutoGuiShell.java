package autogui;

import autogui.base.mapping.GuiReprSet;
import autogui.base.type.GuiTypeBuilder;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingWindow;

import javax.swing.*;

public class AutoGuiShell {
    protected GuiTypeBuilder typeBuilder = new GuiTypeBuilder();
    protected GuiReprSet representationSet = GuiSwingElement.getReprDefaultSet();
    protected GuiSwingMapperSet viewMapperSet = GuiSwingElement.getDefaultMapperSet();

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

            GuiSwingWindow window = GuiSwingWindow.createForObject(o);
            window.setVisible(true);
        });
    }
}
