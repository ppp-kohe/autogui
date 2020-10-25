package org.autogui.swing;

import org.autogui.swing.util.SwingDeferredRunner;
import org.autogui.GuiIncluded;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
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
 *     jshell&gt; import org.autogui.swing.*
 *     jshell&gt; AutoGuiShell.showLive(new Hello())
 * </pre>
 */
public class AutoGuiShell {
    public String lookAndFeelClass = "#system";

    /**
     * @since 1.2
     */
    protected Function<Object, GuiSwingWindow> windowCreator = GuiSwingWindow.creator();
    /**
     * @since 1.2
     */
    protected Function<Object, GuiSwingWindow> windowCreatorRelaxed = GuiSwingWindow.creator().withTypeBuilderRelaxed();

    /**
     * @return a new instance
     */
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
        UIManagerUtil.getInstance().setLookAndFeel(lookAndFeelClass);
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
            if (beforeActionInEvent != null) {
                beforeActionInEvent.run();
            }
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
     * otherwise, it invokes the same task to the event dispatching thread and waits it.
     *  the afterActionInEvent will be executed within the event dispatching thread after creating the window.
     * @param o the target object
     * @param appRoot if true, the returned window will clean-up windows and task-runners at closing of the window
     * @param afterActionInEvent null or an action with the created window, executed within the event dispatching thread
     * @return the created window for o, with default component-set
     * @since 1.1
     */
    public GuiSwingWindow createWindow(Object o, boolean appRoot, Consumer<GuiSwingWindow> afterActionInEvent) {
        return invokeAndWait(() -> {
            GuiSwingWindow w = createWindowInEvent(o);
            w.setApplicationRoot(appRoot);
            if (afterActionInEvent != null) {
                afterActionInEvent.accept(w);
            }
            return w;
        });
    }

    /**
     * creates a window for o with relaxed type-binding,
     * i.e. binding public and package-private members without {@link GuiIncluded} annotations.
     * The created window is NOT app-root ({@link GuiSwingWindow#isApplicationRoot()}==false), thus
     *    closing the window will not call {@link AutoCloseable#close()} to o.
     *    In order to cause the close method, you can directly call {@link GuiSwingWindow#cleanUp()}.
     * @param o the target object
     * @return the created window for o
     */
    public static GuiSwingWindow showLive(Object o) {
        GuiSwingWindow w = get().createWindowRelaxed(o);
        SwingUtilities.invokeLater(() -> displayLiveWindow(w));
        return w;
    }

    public static void displayLiveWindow(JFrame w) {
        w.setVisible(true);
        w.toFront();
        Desktop.getDesktop().requestForeground(false);
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
            GuiSwingWindow w = createWindowRelaxedInEvent(o);
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

    /**
     * calling the creator, within the event-dispatching thread
     * @param o the binding object
     * @return a created window
     * @since 1.2
     */
    protected GuiSwingWindow createWindowInEvent(Object o) {
        return windowCreator.apply(o);
    }

    /**
     * calling the relaxed-creator, within the event-dispatching thread
     * @param o the binding object
     * @return a created window
     * @since 1.2
     */
    protected GuiSwingWindow createWindowRelaxedInEvent(Object o) {
        return windowCreatorRelaxed.apply(o);
    }

    /**
     * customizing the creator for {@link #createWindowInEvent(Object)}
     * @param windowCreator the creator. the default creator is {@link GuiSwingWindow#creator()}
     * @return this
     * @since 1.2
     */
    public AutoGuiShell withWindowCreator(Function<Object, GuiSwingWindow> windowCreator) {
        this.windowCreator = windowCreator;
        return this;
    }

    /**
     * customizing the creator for {@link #createWindowRelaxedInEvent(Object)}
     * @param windowCreatorRelaxed the creator.
     *        the default creator is {@link GuiSwingWindow#creator()}
     *              with {@link GuiSwingWindow.GuiSwingWindowCreator#withTypeBuilderRelaxed()}
     * @return this
     * @since 1.2
     */
    public AutoGuiShell withWindowCreatorRelaxed(Function<Object, GuiSwingWindow> windowCreatorRelaxed) {
        this.windowCreatorRelaxed = windowCreatorRelaxed;
        return this;
    }

    /**
     * clear the {@link #lookAndFeelClass} and call the init
     * @param init the user init process for installing LAF by custom mechanism with receiving this
     * @return this
     * @since 1.2
     */
    public AutoGuiShell withClearLookAndFeelClass(Consumer<AutoGuiShell> init) {
        this.lookAndFeelClass = null;
        if (init != null) {
            init.accept(this);
        }
        return this;
    }
}
