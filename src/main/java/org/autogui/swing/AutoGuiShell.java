package org.autogui.swing;

import org.autogui.swing.util.SwingDeferredRunner;
import org.autogui.GuiIncluded;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * a launcher of automatic GUI binding.
 * <pre>
 *     &#64;{@link GuiIncluded}
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
 *     jshell&gt; /env -class-path target/autogui-1.0-SNAPSHOT.jar
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

    /**
     * create a window for the object o as the application root and display it.
     *  before creating the window, it sets LookAndFeel as the value of {@link #lookAndFeelClass} by {@link #setLookAndFeel()}.
     * @param o the target object
     */
    public void showWindow(Object o) {
        showWindow(o, this::setLookAndFeel);
    }

    public AutoGuiShell withCrossPlatformLookAndFeel() {
        lookAndFeelClass = UIManager.getCrossPlatformLookAndFeelClassName();
        return this;
    }

    /**
     * set the look-and-feel of {@link UIManager} from {@link #lookAndFeelClass}.
     *   the field specifies the class name of LookAndFeel.
     *  if the value of the field is "#system" then, it uses system-look-and-feel.
     */
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
        showWindow(o, beforeActionInEvent, null);
    }

    /**
     * create a window for the object o as the application root and display it.
     * @param o the target object
     * @param beforeActionInEvent action executed before creating the window within the event dispatching thread. nullable
     * @param afterActionInEvent action executed after creating the window within the event dispatching thread. nullable
     * @since 1.1
     */
    public void showWindow(Object o, Runnable beforeActionInEvent, Consumer<GuiSwingWindow> afterActionInEvent) {
        SwingUtilities.invokeLater(() -> {
            beforeActionInEvent.run();
            createWindow(o, true, afterActionInEvent).setVisible(true);
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
        return createWindow(o, appRoot, null);
    }

    /**
     *
     * if the current thread is the event dispatching thread, it will immediately create a window for o and return it.
     * otherwise, it invoke the same task to the event dispatching thread and waits it.
     *  the afterActionInEvent will be executed within in the event dispatching thread after creating the window.
     * @param o the target object
     * @param appRoot if true, the returned window will clean-up windows and task-runners at closing of the window
     * @param afterActionInEvent null or an action with the created window, executed within the event dispatching thread
     * @return the created window for o, with default component-set
     * @since 1.1
     */
    public GuiSwingWindow createWindow(Object o, boolean appRoot, Consumer<GuiSwingWindow> afterActionInEvent) {
        return invokeAndWait(() -> {
            GuiSwingWindow w = GuiSwingWindow.createForObject(o);
            w.setApplicationRoot(appRoot);
            if (afterActionInEvent != null) {
                afterActionInEvent.accept(w);
            }
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
        return createWindowRelaxed(o, null);
    }

    /**
     *
     * @param o the target object
     * @param afterActionInEvent  null or an action with the created window, executed within the event dispatching thread
     * @return the created window for o, with relaxed component-set. Note that the created window does not become application root.
     * @since 1.1
     */
    public GuiSwingWindow createWindowRelaxed(Object o, Consumer<GuiSwingWindow> afterActionInEvent) {
        return invokeAndWait(() -> {
            GuiSwingWindow w = GuiSwingWindow.createForObjectRelaxed(o);
            w.setApplicationRoot(false);
            if (afterActionInEvent != null) {
                afterActionInEvent.accept(w);
            }
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
