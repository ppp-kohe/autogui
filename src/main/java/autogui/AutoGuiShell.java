package autogui;

import autogui.base.mapping.GuiMappingContext;
import autogui.base.mapping.GuiReprSet;
import autogui.base.type.GuiTypeBuilder;
import autogui.base.type.GuiTypeElement;
import autogui.swing.GuiSwingElement;
import autogui.swing.GuiSwingMapperSet;
import autogui.swing.GuiSwingView;

import javax.swing.*;

public class AutoGuiShell {
    protected GuiTypeBuilder typeBuilder = new GuiTypeBuilder();
    protected GuiReprSet representationSet = GuiSwingElement.getReprDefaultSet();
    protected GuiSwingMapperSet viewMapperSet = GuiSwingElement.getDefaultMapperSet();

    public static AutoGuiShell get() {
        return new AutoGuiShell();
    }

    public void showWindow(Object o) {

        GuiTypeElement typeElement = typeBuilder.get(o.getClass());

        GuiMappingContext context = new GuiMappingContext(typeElement, o);
        representationSet.match(context);

        GuiSwingElement view = viewMapperSet.view(context);
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (view instanceof GuiSwingView) {
                JComponent component = ((GuiSwingView) view).createView(context);
                JFrame frame = new JFrame(context.getDisplayName());
                frame.setContentPane(component);
                frame.pack();
                frame.setVisible(true);

                context.updateSourceFromRoot();
            }
        });
    }
}
