package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryException;
import autogui.swing.icons.GuiSwingIcons;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * a log-entry for an exception with supporting GUI rendering
 */
public class GuiSwingLogEntryException extends GuiLogEntryException implements GuiSwingLogEntry {
    protected Map<JTextComponent, int[]> selectionMap = new HashMap<>(2);
    protected boolean expanded;
    protected boolean selected;

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

    public Map<JTextComponent, int[]> getSelections() {
        return selectionMap;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void clearSelection() {
        selectionMap.clear();
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /** a renderer for a log-entry */
    public static class GuiSwingLogExceptionRenderer extends JComponent
            implements TableCellRenderer, ListCellRenderer<GuiLogEntry>, LogEntryRenderer {
        protected ContainerType containerType;
        protected JPanel messageLinePane;
        protected JTextPane message;
        protected JTextPane stackTrace;
        protected JViewport stackViewport;
        protected TextPaneCellSupport.TextPaneCellSupportList supports;

        protected ExceptionExpandAction expandAction;
        protected JButton expandButton;

        protected GuiSwingLogManager manager;

        protected Style messageTimeStyle;
        protected Style messageStyle;

        protected StackTraceStyleSet topSet;
        protected StackTraceStyleSet middleSet;
        protected StackTraceStyleSet lastSet;

        protected boolean selected;

        protected GuiLogEntryException lastValue;
        protected GuiSwingLogEntryException expandPressed;

        protected Map<StyledDocument, List<Integer>> lineIndexes = new HashMap<>(2);

        protected JList lastList;

        public GuiSwingLogExceptionRenderer(GuiSwingLogManager manager, ContainerType type) {
            this.containerType = type;
            setBorder(BorderFactory.createEmptyBorder(7, 10, 3, 10));
            this.manager = manager;
            setLayout(new BorderLayout(0, 0));
            message = new JTextPane();
            message.setOpaque(false);
            message.setFont(GuiSwingLogManager.getFont());

            stackTrace = new JTextPane();
            stackTrace.setFont(GuiSwingLogManager.getFont());

            stackViewport = new JViewport();
            stackViewport.setView(stackTrace);

            expandAction = new ExceptionExpandAction(this);
            expandButton = new GuiSwingIcons.ActionButton(expandAction);
            expandButton.setHideActionText(true);
            expandButton.setPreferredSize(new Dimension(32, 28));

            messageLinePane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            messageLinePane.getInsets().set(0, 0, 0, 0);
            messageLinePane.setBorder(BorderFactory.createEmptyBorder());
            messageLinePane.setOpaque(false);
            messageLinePane.add(message);
            if (type.equals(ContainerType.List)) {
                messageLinePane.add(expandButton);
            }

            add(messageLinePane, BorderLayout.NORTH);
            add(stackViewport, BorderLayout.CENTER);

            if (type.equals(ContainerType.List)) {
                supports = new TextPaneCellSupport.TextPaneCellSupportList(message, stackTrace);
            } else {
                stackViewport.setVisible(false);
            }

            //styles
            messageTimeStyle = GuiSwingLogEntryString.getTimeStyle(message.getStyledDocument());
            messageStyle = message.getStyledDocument().addStyle("message", message.getStyle(StyleContext.DEFAULT_STYLE));
            StyleConstants.setForeground(messageStyle, new Color(142, 73, 60));

            Style defaultStyle = stackTrace.getStyle(StyleContext.DEFAULT_STYLE);
            StyleConstants.setLineSpacing(defaultStyle, 0.1f);

            topSet = new StackTraceStyleSet();
            topSet.set(stackTrace.getStyledDocument(), defaultStyle,
                    new Color(48, 144, 20),
                    new Color(142,73,60),
                    new Color(133, 120, 197),
                    new Color(89, 184, 196),
                    new Color(2, 129, 18),
                    new Color(147, 159, 43),
                    new Color(57, 104, 173),
                    new Color(159, 65, 141));

            middleSet = new StackTraceStyleSet();
            middleSet.set(stackTrace.getStyledDocument(), defaultStyle,
                    new Color(48, 144, 20),
                    new Color(154,128,126),
                    new Color(168, 156, 213),
                    new Color(111, 127, 146),
                    new Color(135, 174, 129),
                    new Color(127, 145, 0),
                    new Color(96, 119, 146),
                    new Color(153, 108, 143));

            lastSet = new StackTraceStyleSet();
            lastSet.set(stackTrace.getStyledDocument(), defaultStyle,
                    new Color(48, 144, 20),
                    new Color(53,129,142),
                    new Color(133, 120, 197),
                    new Color(89, 184, 196),
                    new Color(2, 129, 18),
                    new Color(147, 159, 43),
                    new Color(57, 104, 173),
                    new Color(159, 65, 141));
        }

        @Override
        public ListCellRenderer<GuiLogEntry> getTableCellRenderer() {
            return this;
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends GuiLogEntry> list, GuiLogEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            if (list != null) {
                lastList = list;
            }
            return getTableCellRendererComponent(null, value, isSelected, cellHasFocus, index, 0);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.selected = isSelected;
            if (value instanceof GuiLogEntryException) {
                GuiLogEntryException ex = (GuiLogEntryException) value;
                lastValue = ex;
                setDocument(ex);

                if (supports != null) {
                    supports.setFindHighlights();
                }
                setSelectionHighlights(ex);
            }
            return this;
        }

        public void setDocument(GuiLogEntryException ex) {
            formatMessage(ex.getTime(), ex.getException());
            formatStackTrace(ex.getException());
            stackViewport.setViewSize(stackTrace.getPreferredSize()); //it seems import call
            expansionChanged();
        }

        public void expansionChanged() {
            if (lastValue != null && lastValue instanceof GuiSwingLogEntryException) {
                boolean expanded = ((GuiSwingLogEntryException) lastValue).isExpanded();

                expandButton.setIcon(expandAction.getIcon(expanded));
                expandButton.setPressedIcon(expandAction.getPressedIcon(expanded));

                int i = getExpansionTextStartIndex();
                if (!expanded && i >= 0) {
                    try {
                        int sizeY = stackTrace.modelToView(i).y;
                        stackViewport.setPreferredSize(new Dimension(stackTrace.getPreferredSize().width, sizeY));
                        stackViewport.setViewPosition(new Point(0, 0));
                    } catch (Exception dex) {
                        throw new RuntimeException(dex);
                    }
                } else {
                    stackViewport.setPreferredSize(stackTrace.getPreferredSize());
                }
            }
        }

        /** a valid result will be returned only after {@link #setDocument(GuiLogEntryException)}
         * @return an index
         */
        public int getExpansionTextStartIndex() {
            if (lastValue != null && lastValue instanceof GuiSwingLogEntryException) {
                List<Integer> lines = getLinesForDocument(stackTrace.getStyledDocument());
                if (lines.size() >= 2) {
                    int n = lines.get(1);
                    if (n <= stackTrace.getStyledDocument().getLength()) {
                        return n;
                    }
                }
            }
            return -1;
        }

        public void setSelectionHighlights(GuiLogEntryException ex) {
            if (ex != null && ex instanceof GuiSwingLogEntryException && supports != null) {
                GuiSwingLogEntryException swingEx = (GuiSwingLogEntryException) ex;
                selected = swingEx.isSelected();
                if (selected) {
                    supports.setSelectionHighlights(swingEx.getSelections());
                } else {
                    supports.setSelectionHighlightsClear();
                }
            }
        }

        @Override
        public boolean isShowing() {
            return true;
        }

        //// formatting

        public void formatMessage(Instant time, Throwable ex) {
            String msg = ex.toString();
            StyledDocument doc = message.getStyledDocument();

            //clear
            clearLinesForDocument(doc);
            try {
                doc.remove(0, doc.getLength());
            } catch (Exception dex) {
                throw new RuntimeException(dex);
            }

            // [time] !!! message
            append(doc, manager.formatTime(time), messageTimeStyle);
            append(doc, " !!! " + (msg == null ? "" : msg), messageStyle);
            message.invalidate();
        }

        public void formatStackTrace(Throwable ex)  {
            StyledDocument doc = stackTrace.getStyledDocument();
            clearLinesForDocument(doc);
            try {
                doc.remove(0, doc.getLength());

                Set<Throwable> history = Collections.newSetFromMap(new IdentityHashMap<>());
                formatStackTrace(ex, doc, history, "", topSet, Collections.emptyList());

                int len = doc.getLength();
                if (len > 0 && doc.getText(len - 1, 1).equals("\n")) { //remove last newline
                    doc.remove(len - 1, 1);
                }
            } catch (Exception dex) {
                throw new RuntimeException(dex);
            }
            stackTrace.invalidate();
            stackTrace.setSize(stackTrace.getPreferredSize());
        }

        public List<Integer> getLinesForDocument(StyledDocument doc) {
            return lineIndexes.computeIfAbsent(doc, k -> {
                List<Integer> is = new ArrayList<>();
                is.add(0);
                return is;
            });
        }

        public void clearLinesForDocument(StyledDocument doc) {
            List<Integer> lines = lineIndexes.computeIfAbsent(doc, k -> new ArrayList<>());
            lines.clear();
            lines.add(0);
        }

        public void formatStackTrace(Throwable ex, StyledDocument doc, Set<Throwable> history,
                                     String lineHead,
                                     StackTraceStyleSet set,
                                      List<StackTraceElement> prevStackTrace) {
            if (history.contains(ex)) {
                append(doc, "[CIRCULAR REFERENCE: " + ex + "]\n", set.messageStyle);
                return;
            }
            history.add(ex);

            List<StackTraceElement> nextStackTrace = Arrays.asList(ex.getStackTrace());

            int commons = 0;
            int nextIdx = nextStackTrace.size() - 1,
                    prevIdx = prevStackTrace.size() - 1;
            for (; nextIdx >= 0 && prevIdx >= 0 &&
                         nextStackTrace.get(nextIdx).equals(prevStackTrace.get(prevIdx));
                 --nextIdx, --prevIdx) {
                commons++;
            }

            nextStackTrace.subList(0, nextIdx + 1)
                    .forEach(e -> formatStackTraceElement(e, lineHead, doc, set));
            if (commons > 0) {
                append(doc, lineHead + "\t... " + commons + " more\n", null);
            }

            Throwable[] ss = ex.getSuppressed();
            if (ss != null) {
                Arrays.stream(ss)
                        .forEach(s -> formatStackTraceEnclosing(s, doc, history,
                                "Suppressed: ", lineHead + "\t", prevStackTrace));
            }

            if (ex.getCause() != null) {
                formatStackTraceEnclosing(ex.getCause(), doc, history, "Caused by: ", lineHead, nextStackTrace);
            }
        }

        public void formatStackTraceEnclosing(Throwable ex, StyledDocument doc, Set<Throwable> history,
                                              String head,
                                              String lineHead,
                                              List<StackTraceElement> stackTrace) {
            StackTraceStyleSet set;
            if (ex.getCause() == null ) {
                set = lastSet;
            } else {
                set = middleSet;
            }
            append(doc, lineHead + head, null);
            append(doc, ex.toString() + "\n", set.messageStyle);

            formatStackTrace(ex, doc, history, lineHead, set, stackTrace);
        }

        public void formatStackTraceElement(StackTraceElement e, String lineHead, StyledDocument doc, StackTraceStyleSet styleSet) {
            append(doc, lineHead + "\tat ", null);
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
                enclosingStart = classPart.length() - 1;
            }
            if (moduleEnd > 0) {
                append(doc, typePart.substring(0, moduleEnd + 1), styleSet.moduleStyle);
            }
            if (packEnd > 0) {
                append(doc, classPart.substring(0, packEnd + 1), styleSet.packageStyle);
            }
            append(doc, classPart.substring(packEnd + 1, enclosingStart + 1), styleSet.classNameStyle);
            if (enclosingStart < classPart.length()) {
                append(doc, classPart.substring(enclosingStart + 1), styleSet.innerClassNameStyle);
            }
            append(doc, str.substring(methodStart, methodEnd), styleSet.methodStyle);
            append(doc, str.substring(methodEnd), styleSet.fileNameStyle);

            append(doc, "\n", null);
        }

        public void append(StyledDocument doc, String str, AttributeSet attributeSet) {
            try {

                List<Integer> indexes = getLinesForDocument(doc);
                int len = doc.getLength();
                for (int i = 0, l = str.length(); i < l; ++i) {
                    char c = str.charAt(i);
                    if (c == '\n') {
                        indexes.add(len + i + 1);
                    }
                }

                doc.insertString(len, str, attributeSet);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            setSelectionHighlights(lastValue);
            super.paintComponent(g);
            if (selected && containerType.equals(ContainerType.List)) {
                GuiSwingLogEntryString.drawSelection(getSize(), g);
            }
        }

        @Override
        public void mousePressed(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryException ex = (GuiSwingLogEntryException) entry;
            ex.getSelections().clear();

            TextPaneCellSupport.click(ex.getSelections(), true, point, this);

            Point expandPoint = SwingUtilities.convertPoint(this, point, expandButton);
            expandPressed = expandButton.contains(expandPoint) ? ex : null;
        }

        @Override
        public void mouseDragged(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryException ex = (GuiSwingLogEntryException) entry;
            TextPaneCellSupport.click(ex.getSelections(), false, point, this);
        }

        @Override
        public void mouseReleased(GuiSwingLogEntry entry, Point point) {
            if (expandPressed != null) {
                expandButton.doClick();
            }
        }

        @Override
        public boolean updateFindPattern(String findKeyword) {
            return supports != null &&
                    supports.updateFindPattern(findKeyword);
        }

        @Override
        public int findText(GuiSwingLogEntry entry, String findKeyword) {
            GuiSwingLogEntryException ex = (GuiSwingLogEntryException) entry;
            if (supports != null) {
                if (lastValue != ex) {
                    setDocument(ex);
                }
                List<List<Integer>> is = supports.findTexts(findKeyword,
                        message.getText(), stackTrace.getText());
                return is.stream()
                        .mapToInt(List::size)
                        .sum();
            }
            return 0;
        }

        @Override
        public Object focusNextFound(GuiSwingLogEntry entry, Object prevIndex, boolean forward) {
            GuiSwingLogEntryException ex = (GuiSwingLogEntryException) entry;
            if (supports != null) {
                TextPaneCellSupport.TextPaneCellMatchList m = supports.nextFindMatchedList(prevIndex, forward, entry);
                if (m != null) {
                    TextPaneCellSupport support = supports.getSupport(m.getSupportIndex());
                    int[] range = support.updateSelectionMap(ex.getSelections(), m);
                    int exIndex = getExpansionTextStartIndex();

                    if (range != null && m.getSupportIndex() == 1 && //stackTrace
                            (exIndex <= range[0] || exIndex <= range[1])
                            && !ex.isExpanded()) { //lastValue == ex
                        flipExpansion();
                    }
                }
                return m;
            }
            return null;
        }

        public void flipExpansion() {
            if (lastValue != null && lastValue instanceof GuiSwingLogEntryException) {
                GuiSwingLogEntryException ex = (GuiSwingLogEntryException) lastValue;
                ex.setExpanded(!ex.isExpanded());
                expansionChanged();
                if (lastList != null && lastList.getModel() instanceof GuiSwingLogList.GuiSwingLogListModel) {
                    GuiSwingLogList.GuiSwingLogListModel model = (GuiSwingLogList.GuiSwingLogListModel) lastList.getModel();
                    model.fireRowChanged(lastValue);
                }
            }
        }

        @Override
        public String getSelectedText(GuiSwingLogEntry entry, boolean entireText) {
            GuiSwingLogEntryException ex = (GuiSwingLogEntryException) entry;
            if (lastValue != ex) {
                setDocument(ex);
            }
            if (supports != null) {
                return supports.getSelectedTexts(entireText ? null : ex.getSelections()).stream()
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining("\n"));
            } else {
                return message.getText();
            }
        }
    }

    /**
     * a set of {@link Style}s for describing stack-trace info.
     */
    public static class StackTraceStyleSet {
        public Style timeStyle;
        public Style messageStyle;
        public Style moduleStyle;
        public Style packageStyle;
        public Style classNameStyle;
        public Style innerClassNameStyle;
        public Style methodStyle;
        public Style fileNameStyle;


        public void set(StyledDocument doc, Style parent,
                        Color time, Color message, Color module, Color pack,
                        Color className, Color innerClass, Color method, Color fileName) {
            timeStyle = doc.addStyle("timeStyle", parent);
            StyleConstants.setForeground(timeStyle, time);

            messageStyle = doc.addStyle("messageStyle", parent);
            StyleConstants.setForeground(messageStyle, message);

            moduleStyle = doc.addStyle("moduleStyle", parent);
            StyleConstants.setForeground(moduleStyle, module);

            packageStyle = doc.addStyle("packageStyle", parent);
            StyleConstants.setForeground(packageStyle, pack);

            classNameStyle = doc.addStyle("classNameStyle", parent);
            StyleConstants.setForeground(classNameStyle, className);

            innerClassNameStyle = doc.addStyle("innerClassNameStyle", parent);
            StyleConstants.setForeground(innerClassNameStyle, innerClass);

            methodStyle = doc.addStyle("methodStyle", parent);
            StyleConstants.setForeground(methodStyle, method);

            fileNameStyle = doc.addStyle("fileNameStyle", parent);
            StyleConstants.setForeground(fileNameStyle, fileName);
        }
    }

    /**
     * an action for expanding stack-trace of an exception entry
     */
    public static class ExceptionExpandAction extends AbstractAction {
        protected GuiSwingLogExceptionRenderer renderer;
        public ExceptionExpandAction(GuiSwingLogExceptionRenderer renderer) {
            super("Expand");
            this.renderer = renderer;
            putValue(LARGE_ICON_KEY, getIcon(true));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, getPressedIcon(true));
        }

        public Icon getIcon(boolean expand) {
            return GuiSwingIcons.getInstance().getIcon("log-", expand ? "expand" : "collapse", 16, 14);
        }

        public Icon getPressedIcon(boolean expand) {
            return GuiSwingIcons.getInstance().getPressedIcon("log-", expand ? "expand" : "collapse", 16, 14);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            renderer.flipExpansion();
        }
    }
}
