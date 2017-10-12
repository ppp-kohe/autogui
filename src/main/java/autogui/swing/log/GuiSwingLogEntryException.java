package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryException;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.*;
import java.util.List;

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

        protected StackTraceStyleSet topSet;
        protected StackTraceStyleSet middleSet;
        protected StackTraceStyleSet lastSet;

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

            //styles

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
                String msg = errorValue.toString();
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


        public void formatStackTrace(Throwable ex, StyledDocument doc) throws Exception {
            doc.remove(0, doc.getLength());

            Set<Throwable> history = Collections.newSetFromMap(new IdentityHashMap<>());
            formatStackTrace(ex, doc, history, topSet, Collections.emptyList());
        }

        public void formatStackTraceEnclosing(Throwable ex, StyledDocument doc, Set<Throwable> history,
                                              List<StackTraceElement> stackTrace) {
            StackTraceStyleSet set;
            if (ex.getCause() == null ) {
                set = lastSet;
            } else {
                set = middleSet;
            }
            formatStackTrace(ex, doc, history, set, stackTrace);
        }

        public void formatStackTrace(Throwable ex, StyledDocument doc, Set<Throwable> history,
                                              StackTraceStyleSet set,
                                              List<StackTraceElement> stackTrace) {
            if (history.contains(ex)) {
                return;
            }
            history.add(ex);

            List<StackTraceElement> nextStackTrace = Arrays.asList(ex.getStackTrace());

            nextStackTrace.forEach(e -> formatStackTraceElement(e, doc, set));

            Throwable[] ss = ex.getSuppressed();
            if (ss != null) {
                Arrays.stream(ss)
                        .forEach(s -> formatStackTraceEnclosing(s, doc, history, stackTrace));
            }

            if (ex.getCause() != null) {
                formatStackTraceEnclosing(ex, doc, history, nextStackTrace);
            }
        }

        public void formatStackTraceElement(StackTraceElement e, StyledDocument doc, StackTraceStyleSet styleSet) {

            //TODO header
            try {
                //classLoaderName/moduleName@moduleVersion/declCls.methodName(fileName:lineNum)
                String str = e.toString();
                int methodEnd = str.indexOf('('); //it suppose a class loader name does not contain "("
                int methodStart = str.lastIndexOf('.', methodEnd - 1);

                String typePart = str.substring(0, methodStart);
                int moduleEnd = typePart.lastIndexOf('/');
                String classPart = typePart.substring(moduleEnd + 1);
                int packEnd = classPart.lastIndexOf('.');
                int enclosingStart = classPart.lastIndexOf('$');
                if (enclosingStart < 0) {
                    enclosingStart = classPart.length();
                }
                if (moduleEnd > 0) {
                    doc.insertString(doc.getLength(), typePart.substring(0, moduleEnd + 1), styleSet.moduleStyle);
                }
                if (packEnd > 0) {
                    doc.insertString(doc.getLength(), classPart.substring(0, packEnd + 1), styleSet.packageStyle);
                }
                doc.insertString(doc.getLength(), classPart.substring(packEnd + 1, enclosingStart + 1), styleSet.classNameStyle);
                if (enclosingStart < classPart.length()) {
                    doc.insertString(doc.getLength(), classPart.substring(enclosingStart + 1), styleSet.innerClassNameStyle);
                }
                doc.insertString(doc.getLength(), str.substring(methodStart, methodEnd), styleSet.methodStyle);
                doc.insertString(doc.getLength(), str.substring(methodEnd), styleSet.fileNameStyle);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static class StackTraceStyleSet {
        public Style timeStyle;
        public Style moduleStyle;
        public Style packageStyle;
        public Style classNameStyle;
        public Style innerClassNameStyle;
        public Style methodStyle;
        public Style fileNameStyle;
    }
}
