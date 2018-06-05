package autogui.swing;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class AutoGuiShell {
    public String lookAndFeelClass = "#system";

    public static AutoGuiShell get() {
        return new AutoGuiShell();
    }

    public void showWindow(Object o) {
        showWindow(o, this::setLookAndFeel);
    }

    public AutoGuiShell withCrossPlatformLookAndFeel() {
        lookAndFeelClass = UIManager.getCrossPlatformLookAndFeelClassName();
        return this;
    }

    public void setLookAndFeel() {
        try {
//            boolean sysLafIsGtk = false;
//            if (UIManager.getSystemLookAndFeelClassName().contains("GTKLookAndFeel")) {
//                sysLafIsGtk = true;
//            }
            String laf = lookAndFeelClass;
            if (laf != null && laf.equals("#system")) {
                laf = UIManager.getSystemLookAndFeelClassName();
            }
            if (laf != null) {
                UIManager.setLookAndFeel(laf);
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
