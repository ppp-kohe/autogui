package autogui.swing;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class AutoGuiShell {
    public boolean forceSystemLAF = false;

    public static AutoGuiShell get() {
        return new AutoGuiShell();
    }

    public void showWindow(Object o) {
        showWindow(o, this::setLookAndFeel);
    }

    public void setLookAndFeel() {
        try {
            LookAndFeel laf = UIManager.getLookAndFeel();
            boolean sysLaf = false;
            boolean sysLafIsGtk = false;
            if (laf != null && laf.getClass().getName().endsWith(UIManager.getSystemLookAndFeelClassName())) {
                sysLaf = true;
            }
            if (UIManager.getSystemLookAndFeelClassName().contains("GTKLookAndFeel")) {
                sysLafIsGtk = true;
            }
            if (!sysLaf && (!sysLafIsGtk || forceSystemLAF)) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void showWindow(Object o, Runnable beforeActionInEvent) {
        SwingUtilities.invokeLater(() -> {
            beforeActionInEvent.run();
            createWindow(o, true).setVisible(true);
        });
    }

    /**
     * if the current thread is the event dispatching thread, it will immediately create a window for o and return it.
     * otherwise, it invoke the same task to the event dispatching thread and waits it
     * @param o the target object
     * @param appRoot if true, the returned window will clean-up windows and task-runners at closing of the window.
     * @return the created window for o, with default component-set
     */
    public GuiSwingWindow createWindow(Object o, boolean appRoot) {
        return invokeAndWait(() -> {
            GuiSwingWindow w = GuiSwingWindow.createForObject(o);
            w.setApplicationRoot(appRoot);
            return w;
        });
    }

    public static GuiSwingWindow liveShow(Object o) {
        GuiSwingWindow w = get().createWindowRelaxed(o);
        SwingUtilities.invokeLater(() -> w.setVisible(true));
        return w;
    }

    public GuiSwingWindow createWindowRelaxed(Object o) {
        return invokeAndWait(() -> {
            GuiSwingWindow w = GuiSwingWindow.createForObjectRelaxed(o);
            w.setApplicationRoot(false);
            return w;
        });
    }

    public <T> T invokeAndWait(Supplier<T> factory) {
        if (SwingUtilities.isEventDispatchThread()) {
            return factory.get();
        } else {
            AtomicReference<T> res = new AtomicReference<>();
            try {
                SwingUtilities.invokeAndWait(() -> res.set(factory.get()));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return res.get();
        }
    }
}
