package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryException;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class GuiSwingLogEntryException extends GuiLogEntryException implements GuiSwingLogEntry {
    protected Map<JTextComponent, int[]> selectionMap = new HashMap<>(2);

    public GuiSwingLogEntryException(Throwable exception) {
        super(exception);
    }

    public GuiSwingLogEntryException(Instant time, Throwable exception) {
        super(time, exception);
    }

    @Override
    public LogEntryRenderer getRenderer(GuiSwingLogManager manager, ContainerType type) {
        return new GuiSwingLogExceptionRenderer(manager, type);
    }

    public static class GuiSwingLogExceptionRenderer extends JComponent
            implements TableCellRenderer, ListCellRenderer<GuiLogEntry>, LogEntryRenderer {
        protected JTextPane message;
        protected JTextPane stackTrace;
        protected JViewport stackViewport;
        protected TextPaneCellSupport.TextPaneCellSupportList supports;

        protected GuiSwingLogManager manager;

        public GuiSwingLogExceptionRenderer(GuiSwingLogManager manager, ContainerType type) {
            setBorder(BorderFactory.createEmptyBorder(7, 10, 3, 10));
            this.manager = manager;
            setLayout(new BorderLayout());
            message = new JTextPane();
            message.setFont(GuiSwingLogManager.getFont());
            stackTrace = new JTextPane();
            stackTrace.setFont(GuiSwingLogManager.getFont());

            stackViewport = new JViewport();
            stackViewport.setView(stackTrace);

            add(message, BorderLayout.NORTH);
            add(stackViewport, BorderLayout.CENTER);

            if (type.equals(ContainerType.List)) {
                supports = new TextPaneCellSupport.TextPaneCellSupportList(message, stackTrace);
            }
        }

        @Override
        public ListCellRenderer<GuiLogEntry> getTableCellRenderer() {
            return this;
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends GuiLogEntry> list, GuiLogEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            return getTableCellRendererComponent(null, value, isSelected, cellHasFocus, index, 0);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof GuiLogEntryException) {
                GuiLogEntryException ex = (GuiLogEntryException) value;
                Throwable errorValue = ex.getException();
                String msg = errorValue.getMessage();
                String messageLine = String.format("%s !!! %s",
                        manager.formatTime(ex.getTime()),
                        (msg == null ? "" : msg));
                message.setText(messageLine);
                message.invalidate();

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                errorValue.printStackTrace(pw);
                pw.close();
                String traceStr = sw.toString();
                stackTrace.setText(traceStr);
                stackTrace.invalidate();
                stackViewport.setPreferredSize(stackTrace.getPreferredSize());
            }
            return this;
        }

        @Override
        public boolean isShowing() {
            return true;
        }
    }
}
