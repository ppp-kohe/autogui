package org.autogui.swing;

import org.autogui.base.mapping.GuiPreferencesLoader;
import org.autogui.swing.util.SwingDeferredRunner;
import org.autogui.GuiIncluded;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
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
 *
 * <h2>Setting Look and Feel</h2>
 * <p>
 *  configured by the field {@link #lookAndFeelClass} which can be set by the following withLookAndFeel... methods.
 *    The default value is <code>#prop:autogui.laf</code>.
 *   <ul>
 *       <li>{@link #withLookAndFeelClass(String)} : directly set the argument</li>
 *       <li>{@link #withLookAndFeelProperty(String)} : <code>#prop:</code><i>p</i>.
 *         The value of the property <i>p</i> will be passed to {@link UIManagerUtil#selectLookAndFeelFromSpecialName(String)}.
 *         The absence of the property is equivalent to the special name <code>"default"</code>
 *       </li>
 *       <li>{@link #withLookAndFeelSpecial(String)} : <code>#special:</code><i>v</i>.
 *         The value <i>v</i> will passed to {@link UIManagerUtil#selectLookAndFeelFromSpecialName(String)}</li>
 *       <li>{@link #withCrossPlatformLookAndFeel()} : a cross platform class</li>
 *       <li>{@link #withLookAndFeelNone()} : <code>#none</code> </li>
 *       <li>{@link #withLookAndFeelClassFromFunction(Function)} : the function will be invoked immediately.
 *          It can be used for custom LAF installation.
 *          The function can return a value for {@link #lookAndFeelClass} (nullable) </li>
 *   </ul>
 *   In {@link #setLookAndFeel()}, the {@link #lookAndFeelClass} will be passed to {@link UIManagerUtil#setLookAndFeel(String)}.
 *   <p>
 *  For example, the JVM option <code>-Dautogui.laf=metal</code> with the default configuration
 *    resets the property by the concrete MetalLookAndFeel class-name and sets to metal-laf thanks to {@link UIManagerUtil#setLookAndFeel(String)}.
 *
 */
public class AutoGuiShell {
    public String lookAndFeelClass = UIManagerUtil.getLookAndFeelProp(UIManagerUtil.LOOK_AND_FEEL_PROP_DEFAULT);

    /**
     * @since 1.2
     */
    protected Function<Object, GuiSwingWindow> windowCreator = GuiSwingWindow.creator();
    /**
     * @since 1.2
     */
    protected Function<Object, GuiSwingWindow> windowCreatorRelaxed = GuiSwingWindow.creator().withTypeBuilderRelaxed();

    public AutoGuiShell() {}

    /**
     * @return a new instance
     */
    public static AutoGuiShell get() {
        return new AutoGuiShell();
    }

    /**
     * create a window for the object o as the application root and display it.
     *  before creating the window, it sets LookAndFeel as the value of {@link #lookAndFeelClass} by {@link #setLookAndFeel()}.
     *  <p>
     *   The method is equivalent to the following code:
     *  <pre>
     *  SwingUtilities.invokeLater(() -&gt; {
     *      setLookAndFeel();
     *      createWindow(o, true).setVisible(true);
     *  });
     *  </pre>
     * @param o the target object
     */
    public void showWindow(Object o) {
        showWindow(o, this::setLookAndFeel);
    }

    public AutoGuiShell withCrossPlatformLookAndFeel() {
        return withLookAndFeelClass(UIManager.getCrossPlatformLookAndFeelClassName());
    }

    /**
     * @param lookAndFeelClass the LAF class name or a special name
     *                         which can be acceptable by {@link UIManagerUtil#setLookAndFeel(String)}
     * @return this
     * @since 1.2
     */
    public AutoGuiShell withLookAndFeelClass(String lookAndFeelClass) {
        this.lookAndFeelClass = lookAndFeelClass;
        return this;
    }

    /**
     * @param init the user init process for installing LAF by custom mechanism with receiving this.
     *             the returned string will be passed to {@link #withLookAndFeelClass(String)}
     * @return this
     * @since 1.2
     */
    public AutoGuiShell withLookAndFeelClassFromFunction(Function<AutoGuiShell,String> init) {
        return withLookAndFeelClass(init == null ? null : init.apply(this));
    }

    /**
     * @param p a system property, like "autogui.laf".
     * @return this with setting "#prop:p"
     * @since 1.3
     */
    public AutoGuiShell withLookAndFeelProperty(String p) {
        return withLookAndFeelClass(UIManagerUtil.getLookAndFeelProp(p));
    }

    /**
     * @param v a special name can be passed to {@link UIManagerUtil#selectLookAndFeelFromSpecialName(String)}
     * @return this with setting "#special:v"
     * @since 1.4
     */
    public AutoGuiShell withLookAndFeelSpecial(String v) {
        return withLookAndFeelClass(UIManagerUtil.getLookAndFeelSpecial(v));
    }

    /**
     * turning off the feature of the setting look-and-feel from some property
     * @return this with setting "#none"
     * @since 1.3
     */
    public AutoGuiShell withLookAndFeelNone() {
        return withLookAndFeelClass(UIManagerUtil.LOOK_AND_FEEL_NONE);
    }

    /**
     * set the look-and-feel of {@link UIManager} from {@link #lookAndFeelClass}.
     *   the field specifies the class name of LookAndFeel.
     *  if the value of the field is "#system" then, it uses system-look-and-feel.
     */
    public void setLookAndFeel() {
        UIManagerUtil.getInstance().setLookAndFeel(lookAndFeelClass);
    }

    /**
     * skip loading default properties of object from saved default prefs.
     * The method is useful when the target object's properties are already set:
     * e.g.
     * <pre>
     *     GuiPreferencesLoader.get().parseArgs(obj, args); //control loading prefs by args
     *     AutoGuiShell.get()
     *         .withPrefsValuesLoadSkip() //skip loading prefs
     *         .showWindow(obj);
     * </pre>
     * @return this
     * @since 1.4
     */
    public AutoGuiShell withPrefsValuesLoadSkip() {
        GuiSwingWindow.GuiSwingWindowCreator c = (windowCreator instanceof GuiSwingWindow.GuiSwingWindowCreator) ?
                ((GuiSwingWindow.GuiSwingWindowCreator) windowCreator) : GuiSwingWindow.creator();
        return withWindowCreator(c.withPrefsApplyOptions(new GuiSwingPreferences.PrefsApplyOptionsDefault(true, true)));
    }

    /**
     * apply {@link GuiPreferencesLoader#parseArgs(Object, List)} for the target object.
     * @param args the command args
     * @return this
     * @since 1.4
     */
    public AutoGuiShell withPrefsValuesLoadArgs(String... args) {
        GuiSwingWindow.GuiSwingWindowCreator c = (windowCreator instanceof GuiSwingWindow.GuiSwingWindowCreator) ?
                ((GuiSwingWindow.GuiSwingWindowCreator) windowCreator) : GuiSwingWindow.creator();
        c.withPrefsApplyOptions(new GuiSwingPreferences.PrefsApplyOptionsDefault(true, true));
        return withWindowCreator(obj -> {
            GuiPreferencesLoader.get().parseArgs(obj, Arrays.asList(args));
            return c.createWindow(obj);
        });
    }

    /**
     * shorthand for <code>showWindow(o, beforeActionInEvent, null)</code>
     * @param o the target object
     * @param beforeActionInEvent action executed before creating the window
     * @see #showWindow(Object, Runnable, Consumer)
     */
    public void showWindow(Object o, Runnable beforeActionInEvent) {
        showWindow(o, beforeActionInEvent, null);
    }

    /**
     * create a window for the object o as the application root and display it.
     *  The method runs 1) <code>beforeActionInEvent</code>,
     *    and 2) {@link #createWindow(Object, boolean, Consumer)} with <code>(o,true,afterActionInEvent)</code>
     *       and showing the returned window.
     *    The steps are executed under {@link SwingUtilities#invokeLater(Runnable)}.
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
     * otherwise, it invoke the same task to the event dispatching thread and waits it.
     *  It is shorthand for <code>createWindow(o, appRoot, null)</code>
     * @param o the target object
     * @param appRoot if true, the returned window will clean-up windows and task-runners at closing of the window.
     * @return the created window for o, with default component-set
     * @see #createWindow(Object, boolean, Consumer)
     */
    public GuiSwingWindow createWindow(Object o, boolean appRoot) {
        return createWindow(o, appRoot, null);
    }

    /**
     *
     * if the current thread is the event dispatching thread, it will immediately create a window for o and return it.
     * otherwise, it invokes the same task to the event dispatching thread and waits it.
     *  the afterActionInEvent will be executed within the event dispatching thread after creating the window.
     *  The method 1) runs {@link #createWindowInEvent(Object)}
     *     with setting the application root of the window as <code>appRoot</code>,
     *     and 2) <code>afterActionInEvent</code>.
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
     *  The method do {@link #get()} and {@link #showWindowLive(Object)}.
     * @param o the target object
     * @return the created window for o
     */
    public static GuiSwingWindow showLive(Object o) {
        return get().showWindowLive(o);
    }

    /**
     * called from {@link #showLive(Object)},
     *  creating a new window by {@link #createWindowRelaxed(Object)} and
     *    displaying it by {@link #displayLiveWindow(JFrame)}.
     * @param o the target object
     * @return the created window for o
     * @since 1.3
     */
    public GuiSwingWindow showWindowLive(Object o) {
        GuiSwingWindow w = createWindowRelaxed(o);
        SwingUtilities.invokeLater(() -> displayLiveWindow(w));
        return w;
    }
    /**
     * display the given window and moving it to front
     * @param w the showing window
     */
    public static void displayLiveWindow(JFrame w) {
        w.setVisible(true);
        w.toFront();
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
            Desktop.getDesktop().requestForeground(false);
        }
    }

    /**
     * shorthand for <code>createWindowRelaxed(o, this::setLookAndFeel, null)</code>
     * @param o the target object
     * @return the created window for o
     * @see #createWindowRelaxed(Object, Runnable, Consumer)
     */
    public GuiSwingWindow createWindowRelaxed(Object o) {
        return createWindowRelaxed(o, null);
    }

    /**
     * shorthand for <code>createWindowRelaxed(o, this::setLookAndFeel, afterActionInEvent)</code>
     * @param o the target object
     * @param afterActionInEvent  null or an action with the created window, executed within the event dispatching thread
     * @return the created window for o, with relaxed component-set. Note that the created window does not become application root.
     * @since 1.1
     */
    public GuiSwingWindow createWindowRelaxed(Object o, Consumer<GuiSwingWindow> afterActionInEvent) {
        return createWindowRelaxed(o, this::setLookAndFeel, afterActionInEvent);
    }

    /**
     * The method runs 1) the <code>beforeActionInEvent</code>,
     *   2) {@link #createWindowRelaxedInEvent(Object)} for o with setting the returned window to non application root,
     *   and 3) runs <code>afterActionInEvent</code>.
     *   Those steps are executed under the event dispatching thread ({@link SwingUtilities#invokeAndWait(Runnable)})
     * @param o the target object
     * @param beforeActionInEvent null or an action for pre-process before the creating window
     * @param afterActionInEvent  null or an action with the created window, executed within the event dispatching thread
     * @return the created window for o, with relaxed component-set. Note that the created window does not become application root.
     * @since 1.3
     */
    public GuiSwingWindow createWindowRelaxed(Object o, Runnable beforeActionInEvent, Consumer<GuiSwingWindow> afterActionInEvent) {
        return invokeAndWait(() -> {
            if (beforeActionInEvent != null) {
                beforeActionInEvent.run();
            }
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
     * calling the creator, within the event-dispatching thread.
     *  The creator is set by {@link #withWindowCreator(Function)} or the default creator by {@link GuiSwingWindow#creator()}.
     * @param o the binding object
     * @return a created window
     * @since 1.2
     */
    protected GuiSwingWindow createWindowInEvent(Object o) {
        return windowCreator.apply(o);
    }

    /**
     * calling the relaxed-creator, within the event-dispatching thread
     *  The creator is set by {@link #withWindowCreatorRelaxed(Function)}
     *  or the default creator by {@link GuiSwingWindow#creator()}
     *     with {@link GuiSwingWindow.GuiSwingWindowCreator#withTypeBuilderRelaxed()}
     * @param o the binding object
     * @return a created window
     * @since 1.2
     */
    protected GuiSwingWindow createWindowRelaxedInEvent(Object o) {
        return windowCreatorRelaxed.apply(o);
    }

    /**
     * customizing the creator for {@link #createWindowInEvent(Object)}.
     *  e.g.
     *  <pre>
     *      AutoGuiShell.get()
     *          .withWindowCreator(
     *              GuiSwingWindow.creator()
     *                    .withLogStatus(false)) //customizing the creator
     *          .showWindow(o);
     *  </pre>
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

}
