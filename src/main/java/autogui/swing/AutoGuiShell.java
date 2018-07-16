package autogui.swing;

import autogui.swing.util.SwingDeferredRunner;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * a launcher of automatic GUI binding.
 * <pre>
 *     &#64;{@link autogui.GuiIncluded}
 *     public class MyApp {
 *         public static void main(String[] args) {
 *             AutoGuiShell.get().showWindow(new MyApp());
 *         }
 *
 *         &#64;GuiIncluded public String input;
 *         &#64;GuiIncluded public void action() {
 *             System.out.println("hello " + input);
 *         }
 *     }
 * </pre>
 *
 * <p>
 * for jshell: the static method {@link #showLive(Object)} relaxes accessibility and annotations
 * <pre>
 *     jshell&gt; /env --add-class-path target/autogui-1.0-SNAPSHOT.jar
 *     jshell&gt; class Hello {
 *        ...&gt;   String value;
 *        ...&gt;   void action() {
 *        ...&gt;     System.out.println(value);
 *        ...&gt;   }
 *        ...&gt; }
 *     jshell&gt; import autogui.swing.*
 *     jshell&gt; AutoGuiShell.showLive(new Hello())
 * </pre>
 */
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

    public static GuiSwingWindow showLive(Object o) {
        GuiSwingWindow w = get().createWindowRelaxed(o);
        SwingUtilities.invokeLater(() -> displayLiveWindow(w));
        return w;
    }

    public static void displayLiveWindow(JFrame w) {
        w.setVisible(true);
        w.toFront();
        try { //jdk9
            Method requestForeground = Desktop.class.getMethod("requestForeground", boolean.class);
            requestForeground.invoke(Desktop.getDesktop(), false);
        } catch (Exception ex) {
            //
        }
    }

    public GuiSwingWindow createWindowRelaxed(Object o) {
        return invokeAndWait(() -> {
            GuiSwingWindow w = GuiSwingWindow.createForObjectRelaxed(o);
            w.setApplicationRoot(false);
            return w;
        });
    }

    public <T> T invokeAndWait(Supplier<T> factory) {
        if (SwingDeferredRunner.isEventThreadOrDispatchedFromEventThread()) {
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
