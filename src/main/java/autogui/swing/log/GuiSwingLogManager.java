package autogui.swing.log;

import autogui.base.log.*;
import autogui.swing.icons.GuiSwingIcons;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.PrintStream;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class GuiSwingLogManager extends GuiLogManager {
    protected List<Consumer<GuiLogEntry>> views = new ArrayList<>();
    protected GuiLogManagerConsole console;

    /** view might accept same entries */
    public void addView(Consumer<GuiLogEntry> view) {
        views.add(view);
    }

    public void setConsole(GuiLogManagerConsole console) {
        this.console = console;
    }

    public GuiLogManagerConsole getConsole() {
        return console;
    }

    public GuiLogManager setupConsole(boolean replaceError, boolean replaceOutput, boolean uncaughtHandler) {
        setConsole(new GuiLogManagerConsole(getSystemErr()));
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
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return new Font("Menlo", Font.PLAIN, 14);
        } else {
            return new Font(Font.MONOSPACED, Font.PLAIN, 12);
        }
    }

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
            if (value != null && value instanceof GuiSwingLogEntry) {
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
            if (value != null && value instanceof GuiSwingLogEntry) {
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

    public static class GuiSwingLogWindow extends JFrame {
        protected GuiSwingLogList list;
        protected JToolBar toolbar;
        protected GuiSwingLogStatusBar statusBar;

        public GuiSwingLogWindow(GuiSwingLogManager manager) {
            setTitle("Log");
            JPanel pane = new JPanel(new BorderLayout());

            //list
            list = new GuiSwingLogList(manager);
            list.setEntryLimit(10000);
            pane.add(new JScrollPane(list,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                    BorderLayout.CENTER);

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
            setSize(400, 600);
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

        public JPanel getPaneWithStatusBar(JPanel centerPane) {
            JPanel pane = new JPanel(new BorderLayout());
            pane.add(centerPane, BorderLayout.CENTER);
            pane.add(statusBar, BorderLayout.SOUTH);
            return pane;
        }
    }

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
            putValue(LARGE_ICON_KEY, icons.getIcon("log-", "show", 32, 25));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, icons.getPressedIcon("log-", "show", 32, 25));
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
