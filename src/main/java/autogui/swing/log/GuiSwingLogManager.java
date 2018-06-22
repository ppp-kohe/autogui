package autogui.swing.log;

import autogui.base.log.*;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.util.UIManagerUtil;

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
 */
public class GuiSwingLogManager extends GuiLogManager {
    protected List<Consumer<GuiLogEntry>> views = new ArrayList<>();
    protected GuiLogManagerConsole console;

    public static boolean replaceErr = true;
    public static boolean replaceOut = true;
    public static boolean replaceExceptionHandler = true;
    public static boolean redirectToConsole = true;

    public static void setDefaultReplace(boolean flag) {
        replaceErr = flag;
        replaceOut = flag;
        replaceExceptionHandler = flag;
    }

    public static void setRedirectToConsole(boolean flag) {
        redirectToConsole = flag;
    }

    /**
     * @param view might accept same entries
     * @return key object for removing, currently view itself
     */
    public Object addView(Consumer<GuiLogEntry> view) {
        views.add(view);
        return view;
    }

    @SuppressWarnings("unchecked")
    public void removeView(Object v) {
        if (v instanceof Consumer<?>) {
            views.remove((Consumer<GuiLogEntry>) v);
        }
    }

    public void setConsole(GuiLogManagerConsole console) {
        this.console = console;
    }

    public GuiLogManagerConsole getConsole() {
        return console;
    }

    public GuiLogManager setupConsole(boolean replaceError, boolean replaceOutput, boolean uncaughtHandler,
                                      boolean redirectToConsole) {
        if (redirectToConsole) {
            setConsole(new GuiLogManagerConsole(getSystemErr()));
        }
        replaceConsole(replaceError, replaceOutput);
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
    public GuiLogEntryString logString(String str) {
        GuiLogEntryString e = new GuiSwingLogEntryString(str);
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
        views.forEach(v -> v.accept(e));
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
    }

    public GuiSwingLogWindow createWindow() {
        return new GuiSwingLogWindow(this);
    }

    /**
     * a window for displaying log-list
     */
    public static class GuiSwingLogWindow extends JFrame {
        protected GuiSwingLogList list;
        protected JToolBar toolbar;
        protected GuiSwingLogStatusBar statusBar;

        public GuiSwingLogWindow(GuiSwingLogManager manager) {
            setTitle("Log");
            setType(Type.UTILITY);
            JPanel pane = new JPanel(new BorderLayout());

            //list
            list = new GuiSwingLogList(manager);
            list.setEntryLimit(10000);
            JScrollPane scrollPane = new JScrollPane(list,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            UIManagerUtil ui = UIManagerUtil.getInstance();
            scrollPane.getVerticalScrollBar().setUnitIncrement(ui.getScaledSizeInt(16));
            pane.add(scrollPane, BorderLayout.CENTER);

            //tool bar
            toolbar = list.createToolBar();
            pane.add(toolbar, BorderLayout.NORTH);

            //status bar
            statusBar = new GuiSwingLogStatusBar(manager);
            JButton showButton = new GuiSwingIcons.ActionButton(new LogWindowShowAction(this));
            showButton.setHideActionText(true);
            showButton.setBorderPainted(false);
            statusBar.add(showButton, BorderLayout.WEST);
            setContentPane(pane);
            pack();
            setSize(ui.getScaledSizeInt(400), ui.getScaledSizeInt(600));
        }

        @Override
        public void dispose() {
            list.removeFromManager();
            statusBar.removeFromManager();
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
        protected JFrame frame;
        protected boolean first = true;
        public LogWindowShowAction(JFrame frame) {
            putValue(NAME, "Show");
            putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_L,
                            KeyEvent.SHIFT_DOWN_MASK |
                                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
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
