package autogui.swing.log;

import autogui.base.log.*;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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

    public GuiLogManager setupConsole(boolean replaceError, boolean replaceOutput) {
        setConsole(new GuiLogManagerConsole(getSystemErr()));
        if (replaceError) {
            PrintStream exErr = System.err;
            if (exErr instanceof LogPrintStream) {
                System.setErr(new LogPrintStream(this, exErr));
            } else {
                //err -> logString -> original err
                System.setErr(new LogPrintStream(this));
            }
        }
        if (replaceOutput) {
            PrintStream exOut = System.err;
            //out -> {original out, logString -> original err }
            System.setOut(new LogPrintStream(this, exOut));
        }
        return this;
    }

    public PrintStream getSystemErr() {
        PrintStream err = System.err;
        if (err instanceof LogPrintStream) {
            GuiLogManager manager = ((LogPrintStream) err).getManager();
            if (manager instanceof GuiSwingLogManager) {
                GuiLogManagerConsole console = ((GuiSwingLogManager) manager).getConsole();
                if (console != null) {
                    err = console.getOut();
                }
            }
        }
        return err;
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

    public void show(GuiLogEntry e) {
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

    public static class LogPrintStream extends PrintStream {
        private GuiLogManager manager;
        public LogPrintStream(GuiLogManager manager) {
            super(new LogOutputStream(manager));
        }

        public LogPrintStream(GuiLogManager manager, OutputStream out) {
            super(new LogOutputStream(manager, out));
            this.manager = manager;
        }

        public GuiLogManager getManager() {
            return manager;
        }

        public OutputStream getOut() {
            return out;
        }
    }

    public static class LogOutputStream extends OutputStream {
        protected OutputStream out;
        protected ByteBuffer buffer;
        protected GuiLogManager manager;
        protected Charset defaultCharset;

        public LogOutputStream(GuiLogManager manager) {
            this(manager, null);
        }

        public LogOutputStream(GuiLogManager manager, OutputStream out) {
            this.manager = manager;
            this.out = out;
            buffer = ByteBuffer.allocateDirect(4096);
            defaultCharset = Charset.defaultCharset(); //PrintStream always encode by default encoding
        }

        @Override
        public void write(int b) throws IOException {
            if (out != null) {
                out.write(b);
            }
            synchronized (this) {
                expand(1000);
                buffer.put((byte) b);
                debug("write " + b + " : " + buffer);
                if (b == '\n') {
                    flushLog();
                }
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            if (out != null) {
                out.write(b);
            }
            synchronized (this) {
                expand(b.length);
                buffer.put(b);
                debug("write [" + b.length + "] : " + buffer);
                for (byte e : b) {
                    if (e == '\n') {
                        flushLog();
                        break;
                    }
                }
            }
        }

        public void expand(int len) {
            if (len >= buffer.remaining()) {
                ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.position() + (int) (len * 1.2));
                ((Buffer) buffer).flip();
                newBuffer.put(buffer);
                buffer = newBuffer;
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (out != null) {
                out.write(b, off, len);
            }
            synchronized (this) {
                expand(len);
                buffer.put(b, off, len);
                debug("write [" + off + "," + len + "] : " + buffer);
                for (int i = 0; i < len; ++i) {
                    if (b[off + i] == '\n') {
                        flushLog();
                        break;
                    }
                }
            }
        }

        @Override
        public void flush() throws IOException {
            synchronized (this) {
                debug("flush");
                flushLog();
            }
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (this) {
                flushLog();
            }
            if (out != null) {
                out.close();
            }
        }

        public void flushLog() {
            ((Buffer) buffer).flip();
            if (buffer.hasRemaining()) {
                try {
                    String ds = buffer.toString();
                    String data = defaultCharset.decode(buffer).toString();
                    debug("flush " + ds + " -> " + buffer.toString() + " <" + data + ">");
                    //cut the last line
                    if (data.endsWith("\n")) {
                        data = data.substring(0, data.length() - 1);
                    }
                    if (manager != null) {
                        debug(" flush log: <" + data + ">" + Thread.currentThread());
                        manager.logString(data);
                    }
                } catch (Exception ex) {
                    manager.logString("data...");
                }
            }
            ((Buffer) buffer).clear();
            debug("    flush cleared: " + buffer);
        }

        void debug(String str) {
            area.append(str + "\n");
        }
        JTextArea area;
        {
            JFrame frame = new JFrame();
            area = new JTextArea();
            frame.setContentPane(new JScrollPane(area));
            frame.setSize(1000, 800);
            frame.setVisible(true);
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
            JButton showButton = new JButton(new LogWindowShowAction(this));
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
