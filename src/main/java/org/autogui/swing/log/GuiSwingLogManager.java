package org.autogui.swing.log;

import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.util.PopupExtension;
import org.autogui.swing.util.UIManagerUtil;
import org.autogui.base.log.*;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.PrintStream;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * a log-manager supporting Swing GUI
 * <pre>
 *     GuiSwingLogManager m = new GuiSwingLogManager();
 *     m.setupConsole(
 *               GuiSwingLogManager.replaceErr,
 *               GuiSwingLogManager.replaceOut,
 *               GuiSwingLogManager.replaceExceptionHandler,
 *               GuiSwingLogManager.redirectToConsole,
 *               GuiSwingLogManager.suppressOutputRedirection);
 *     GuiLogManager.setManager(m);
 *     ...
 *     frame.setContentPane(m.createWindow().getPaneWithStatusBar(frame.getContentPane()));
 *     ...
 *     GuiLogManager.log(...);
 * </pre>
 */
public class GuiSwingLogManager extends GuiLogManager {
    protected List<GuiSwingLogView> views = new ArrayList<>();
    protected GuiLogManagerConsole console;

    public static boolean replaceErr = true;
    public static boolean replaceOut = true;
    public static boolean replaceExceptionHandler = true;
    public static boolean redirectToConsole = true;
    /** @since 1.1 */
    public static boolean suppressOutputRedirection = true;

    /**
     * methods of the interface might happen outside of the event thread
     * @since 1.2
     */
    public interface GuiSwingLogView {
        /**
         * @param entry added entry for displaying in the view
         */
        void addLogEntry(GuiLogEntry entry);

        /**
         * clear added views
         */
        void clearLogEntries();
    }

    public static void setDefaultReplace(boolean flag) {
        replaceErr = flag;
        replaceOut = flag;
        replaceExceptionHandler = flag;
    }

    public static void setRedirectToConsole(boolean flag) {
        redirectToConsole = flag;
    }

    /**
     * @param flag flag for {@link #suppressOutputRedirection}
     * @since 1.1
     */
    public static void setSuppressOutputRedirection(boolean flag) {
        suppressOutputRedirection = flag;
    }

    /**
     * @param view might accept same entries
     * @return key object for removing, currently view itself
     */
    public Object addView(Consumer<GuiLogEntry> view) {
        views.add(new GuiSwingLogView() {
            @Override
            public void addLogEntry(GuiLogEntry entry) {
                view.accept(entry);
            }

            @Override
            public void clearLogEntries() { }
        });
        return view;
    }

    /**
     * @param view a view for log entries, might accept same entries
     * @return key object for removing, currently view itself
     * @since 1.2
     */
    public Object addView(GuiSwingLogView view) {
        views.add(view);
        return view;
    }

    public void removeView(Object v) {
        if (v instanceof GuiSwingLogView) {
            views.remove((GuiSwingLogView) v);
        }
    }

    public void setConsole(GuiLogManagerConsole console) {
        this.console = console;
    }

    public GuiLogManagerConsole getConsole() {
        return console;
    }

    /**
     * @return existing manager from {@link #get()} or a new manager
     * @since 1.1
     */
    public static GuiSwingLogManager getOrSetSwingLogManager() {
        GuiSwingLogManager logManager;
        synchronized (GuiLogManager.class) {
            GuiLogManager m = GuiLogManager.get();
            if (m instanceof GuiSwingLogManager) {
                logManager = (GuiSwingLogManager) m;
            } else {
                logManager = new GuiSwingLogManager();
                logManager.setupConsoleWithDefaultFlags();
                GuiLogManager.setManager(logManager);
            }
        }
        return logManager;
    }

    /**
     * the method calls {@link #setupConsole(boolean, boolean, boolean, boolean, boolean)}
     *  with reading following properties with conjunction of static fields.
     *  those properties take "true" (default) or "false".
     * <ul>
     *  <li>autogui.log.replaceErr </li>
     *  <li>autogui.log.replaceOut </li>
     *  <li>autogui.log.replaceExceptionHandler </li>
     *  <li>autogui.log.redirectToConsole </li>
     *  <li>autogui.log.suppressOutputRedirection </li>
     * </ul>
     * @return this
     * @since 1.1
     *
     * @see #setupConsole(boolean, boolean, boolean, boolean, boolean)
     */
    public GuiLogManager setupConsoleWithDefaultFlags() {
        boolean replaceErr = getPropertyFlag("autogui.log.replaceErr");
        boolean replaceOut = getPropertyFlag("autogui.log.replaceOut");
        boolean replaceExceptionHandler = getPropertyFlag("autogui.log.replaceExceptionHandler");
        boolean redirectToConsole = getPropertyFlag("autogui.log.redirectToConsole");
        boolean suppressOutputRedirection = getPropertyFlag("autogui.log.suppressOutputRedirection");
        return setupConsole(
                replaceErr && GuiSwingLogManager.replaceErr,
                replaceOut && GuiSwingLogManager.replaceOut,
                replaceExceptionHandler && GuiSwingLogManager.replaceExceptionHandler,
                redirectToConsole && GuiSwingLogManager.redirectToConsole,
                suppressOutputRedirection && GuiSwingLogManager.suppressOutputRedirection);
    }

    private static boolean getPropertyFlag(String name) {
        return System.getProperty(name, "true").equals("true");
    }

    public GuiLogManager setupConsole(boolean replaceError, boolean replaceOutput, boolean uncaughtHandler,
                                      boolean redirectToConsole) {
        return setupConsole(replaceError, replaceOutput, uncaughtHandler, redirectToConsole, true);
    }

    /**
     * controls logging redirection.
     *
     * <pre>
     *     setupConsole(true, true, true, true, true);
     *     //default: redirect to console and GUI-list
     *      System.err.print("msg"); // =&gt; stderr: "[Time...] msg",  GUI-list: "[Time...] msg"
     *      System.out.print("msg"); // =&gt; stdout: "msg",            GUI-list: "[Time...] msg"
     *      logString("msg");        // =&gt; stderr: "[Time...] msg",  GUI-list: "[Time...] msg"
     *
     *     setupConsole(false, false, true, true, true);
     *     //no stdout/stderr replacement.
     *      System.err.print("msg"); // =&gt; stderr: "msg"
     *      System.out.print("msg"); // =&gt; stdout: "msg"
     *      logString("msg");        // =&gt; stderr: "[Time...] msg",  GUI-list: "[Time...] msg"
     *
     *     setupConsole(true, true, true, false, true);
     *     //no console redirection: only stdout directly write message to the console as outputs
     *      System.err.print("msg"); // =&gt;                           GUI-list: "[Time...] msg"
     *      System.out.print("msg"); // =&gt; stdout: "msg",            GUI-list: "[Time...] msg"
     *      logString("msg");        // =&gt;                           GUI-list: "[Time...] msg"
     *
     *     setupConsole(false, false, true, false, true);
     *     //no stdout/stderr replacement, and no console redirection: stdout/stderr just work as original
     *      System.err.print("msg"); // =&gt;  stderr: "msg"
     *      System.out.print("msg"); // =&gt;  stdout: "msg"
     *      logString("msg");        // =&gt;                           GUI-list: "[Time...] msg"
     *
     *     setupConsole(true, true, true, true, false);
     *     //full redirection: stdout also writes to stderr
     *      System.err.print("msg"); // =&gt;  stderr: "[Time...] msg",                GUI-list: "[Time...] msg"
     *      System.out.print("msg"); // =&gt;  stderr: "[Time...] msg", stdout: "msg", GUI-list: "[Time...] msg"
     *      logString("msg");        // =&gt;  stderr: "[Time...] msg",                GUI-list: "[Time...] msg"
     *
     * </pre>
     * @param replaceError replace System.err for redirection to the GUI list
     * @param replaceOutput replace System.out for redirection to the GUI list
     * @param uncaughtHandler replace the uncaught-handler for redirection to the GUI list
     * @param redirectToConsole adding {@link GuiLogManagerConsole} to the manager: this means that the GUI list redirects to the console (original System.err)
     * @param suppressOutputRedirection avoiding redirection of redirectToConsole
     *                                  from replaced System.out. So work only if replaceOutput=true and redirectToConsole=true
     * @return this
     * @since 1.1
     */
    public GuiLogManager setupConsole(boolean replaceError, boolean replaceOutput, boolean uncaughtHandler,
                                      boolean redirectToConsole, boolean suppressOutputRedirection) {
        if (redirectToConsole) {
            GuiLogManagerConsole console = new GuiLogManagerConsole(getSystemErr());
            console.setShowStandard(!suppressOutputRedirection);
            setConsole(console);
        } else {
            setConsole(null);
        }

        resetOut();
        resetErr();
        replaceConsole(replaceError, replaceOutput);

        resetUncaughtHandler();
        if (uncaughtHandler) {
            replaceUncaughtHandler();
        }
        return this;
    }


    @Override
    public PrintStream getErr() {
        GuiLogManagerConsole console = getConsole();
        if (console != null) {
            return console.getErr();
        }
        return null;
    }

    @Override
    public GuiLogEntryString logString(String str, boolean fromStandard) {
        GuiLogEntryString e = new GuiSwingLogEntryString(str, fromStandard);
        if (console != null) {
            console.showString(e);
        }
        show(e);
        return e;
    }

    @Override
    public GuiLogEntryException logError(Throwable ex) {
        GuiLogEntryException e = new GuiSwingLogEntryException(ex);
        if (console != null) {
            console.showError(e);
        }
        show(e);
        return e;
    }

    @Override
    public GuiLogEntryProgress logProgress() {
        GuiSwingLogEntryProgress p = new GuiSwingLogEntryProgress();
        p.addListener(this::updateProgress);
        if (console != null) {
            console.showProgress(p);
        }
        show(p);
        return p;
    }

    @Override
    public void updateProgress(GuiLogEntryProgress p) {
        if (console != null) {
            console.updateProgress(p);
        }
        show(p);
    }

    @Override
    public synchronized void show(GuiLogEntry e) {
        views.forEach(v -> v.addLogEntry(e));
    }

    /**
     * call {@link GuiSwingLogView#clearLogEntries()} to views
     * @since 1.2
     */
    public synchronized void clear() {
        views.forEach(GuiSwingLogView::clearLogEntries);
    }

    public static Font getFont() {
        return UIManagerUtil.getInstance().getConsoleFont();
    }

    /**
     * a log-entry renderer for a list and a status-bar
     */
    public static class GuiSwingLogRenderer implements TableCellRenderer, ListCellRenderer<GuiLogEntry> {
        protected GuiSwingLogManager manager;
        protected JLabel nullLabel;
        protected Map<Object, GuiSwingLogEntry.LogEntryRenderer> rendererMap = new HashMap<>();
        protected GuiSwingLogEntry.ContainerType containerType;

        public GuiSwingLogRenderer(GuiSwingLogManager manager, GuiSwingLogEntry.ContainerType type) {
            this.manager = manager;
            nullLabel = new JLabel("null");
            this.containerType = type;
        }

        public GuiSwingLogEntry.LogEntryRenderer getEntryRenderer(GuiSwingLogEntry e) {
            Object rendererKey = e.getRendererKey();
            if (rendererKey == null) {
                return e.getRenderer(manager, containerType);
            } else {
                return rendererMap.computeIfAbsent(rendererKey, k ->
                        e.getRenderer(manager, containerType));
            }
        }

        public List<GuiSwingLogEntry.LogEntryRenderer> getRendererList() {
            return new ArrayList<>(rendererMap.values());
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends GuiLogEntry> list, GuiLogEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof GuiSwingLogEntry) {
                ListCellRenderer<GuiLogEntry> renderer = getEntryRenderer((GuiSwingLogEntry) value).getTableCellRenderer();
                if (renderer != null) {
                    return renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            }
            nullLabel.setText(Objects.toString(value));
            return nullLabel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            if (value instanceof GuiSwingLogEntry) {
                GuiSwingLogEntry e = (GuiSwingLogEntry) value ;
                TableCellRenderer renderer = (TableCellRenderer) getEntryRenderer(e).getTableCellRenderer();
                if (renderer != null) {
                    return renderer.getTableCellRendererComponent(table, e, isSelected, hasFocus, row, column);
                }
            }
            nullLabel.setText(Objects.toString(value));
            return nullLabel;
        }

        public void clearRendererList() {
            rendererMap.forEach((k,v) -> v.close());
            rendererMap.clear();
        }
    }

    public GuiSwingLogWindow createWindow() {
        return new GuiSwingLogWindow(this);
    }

    /**
     * a window for displaying log-list
     */
    public static class GuiSwingLogWindow extends JFrame {
        private static final long serialVersionUID = 1L;
        protected GuiSwingLogManager manager;
        protected GuiSwingLogList list;
        protected JToolBar toolbar;
        protected GuiSwingLogStatusBar statusBar;

        public GuiSwingLogWindow(GuiSwingLogManager manager) {
            this.manager = manager;
            setTitle("Log");
            setType(Type.UTILITY);
            JPanel pane = new JPanel(new BorderLayout());

            UIManagerUtil ui = UIManagerUtil.getInstance();

            initList(pane);
            initStatusBar();

            setContentPane(pane);
            pack();
            setSize(ui.getScaledSizeInt(400), ui.getScaledSizeInt(600));
        }

        /**
         * @param pane the content pane of the window
         * @since 1.1
         */
        protected void initList(JPanel pane) {
            UIManagerUtil ui = UIManagerUtil.getInstance();
            //list
            list = new GuiSwingLogList(manager);
            list.setEntryLimit(10000);
            JScrollPane scrollPane = new JScrollPane(list,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getVerticalScrollBar().setUnitIncrement(ui.getScaledSizeInt(16));
            pane.add(scrollPane, BorderLayout.CENTER);

            //tool bar
            toolbar = list.createToolBar();
            pane.add(toolbar, BorderLayout.NORTH);
        }

        /**
         * @since 1.1
         */
        protected void initStatusBar() {
            //status bar
            statusBar = new GuiSwingLogStatusBar(manager);
            JButton showButton = new GuiSwingIcons.ActionButton(new LogWindowShowAction(this));
            showButton.setHideActionText(true);
            showButton.setBorderPainted(false);
            statusBar.add(showButton, BorderLayout.WEST);
        }

        /**
         * @return the log-manager
         * @since  1.1
         */
        public GuiSwingLogManager getManager() {
            return manager;
        }

        @Override
        public void dispose() {
            if (list != null) {
                list.removeFromManager();
            }
            if (statusBar != null) {
                statusBar.removeFromManager();
            }
            super.dispose();
        }

        public GuiSwingLogList getList() {
            return list;
        }

        public GuiSwingLogStatusBar getStatusBar() {
            return statusBar;
        }

        public JToolBar getToolbar() {
            return toolbar;
        }

        public JPanel getPaneWithStatusBar(JComponent centerPane) {
            JPanel pane = new JPanel(new BorderLayout());
            pane.add(centerPane, BorderLayout.CENTER);
            pane.add(statusBar, BorderLayout.SOUTH);
            return pane;
        }
    }

    /**
     * an action for showing the log-list, displayed on a status-bar
     */
    public static class LogWindowShowAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        protected JFrame frame;
        protected boolean first = true;
        public LogWindowShowAction(JFrame frame) {
            putValue(NAME, "Show");
            putValue(ACCELERATOR_KEY,
                    PopupExtension.getKeyStroke(KeyEvent.VK_L,
                            KeyEvent.SHIFT_DOWN_MASK,
                                    PopupExtension.getMenuShortcutKeyMask()));
            GuiSwingIcons icons = GuiSwingIcons.getInstance();

            int size = UIManagerUtil.getInstance().getScaledSizeInt(25);
            putValue(LARGE_ICON_KEY, icons.getIcon("log-", "show", size, size));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, icons.getPressedIcon("log-", "show", size, size));
            this.frame = frame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (first && e.getSource() instanceof JComponent) {
                frame.setLocationRelativeTo((JComponent) e.getSource());
            }
            first = false;
            frame.setVisible(true);
        }
    }
}
