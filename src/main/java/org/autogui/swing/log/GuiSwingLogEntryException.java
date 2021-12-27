package org.autogui.swing.log;

import org.autogui.base.log.GuiLogEntry;
import org.autogui.base.log.GuiLogEntryException;
import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.util.TextCellRenderer;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.RoundRectangle2D;
import java.text.AttributedCharacterIterator.Attribute;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * a log-entry for an exception with supporting GUI rendering
 */
public class GuiSwingLogEntryException extends GuiLogEntryException implements GuiSwingLogEntry {
    protected Map<TextCellRenderer<?>, int[]> selectionMap = new HashMap<>(2);
    protected boolean expanded;
    protected boolean selected;

    protected String lines;
    protected Map<Integer, List<StackTraceAttributesForLine>> lineToAttrs;

    protected Map<Object, float[]> rendererToSizeCache;

    public GuiSwingLogEntryException(Throwable exception) {
        super(exception);
    }

    public GuiSwingLogEntryException(Instant time, Throwable exception) {
        super(time, exception);
    }

    public float[] sizeCache(Object r, Supplier<float[]> src) {
        if (rendererToSizeCache == null) {
            rendererToSizeCache = new HashMap<>(3);
        }
        return rendererToSizeCache.computeIfAbsent(r, _r -> src.get());
    }

    @Override
    public LogEntryRenderer getRenderer(GuiSwingLogManager manager, ContainerType type) {
        return new GuiSwingLogExceptionRenderer(manager, type);
    }

    public Map<TextCellRenderer<?>, int[]> getSelections() {
        return selectionMap;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setLines(String lines) {
        this.lines = lines;
    }

    public String getLines() {
        return lines;
    }

    public Map<Integer, List<StackTraceAttributesForLine>> getLineToAttrs() {
        return lineToAttrs;
    }

    public void setLineToAttrs(Map<Integer, List<StackTraceAttributesForLine>> lineToAttrs) {
        this.lineToAttrs = lineToAttrs;
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

    /** a renderer for exception stack-traces */
    public static class GuiSwingLogExceptionRenderer extends JComponent
            implements TableCellRenderer, ListCellRenderer<GuiLogEntry>, LogEntryRenderer {
        private static final long serialVersionUID = 1L;
        protected ContainerType containerType;
        protected GuiSwingLogManager manager;
        protected TextCellRenderer<GuiLogEntryException> message;
        protected ExceptionStackTraceRenderer stackTrace;

        protected ExceptionExpandAction expandAction;
        protected JButton expandButton;
        protected JPanel messageLinePane;
        @SuppressWarnings("rawtypes")
        protected JList lastList;

        protected boolean selected;
        protected GuiLogEntryException lastValue;
        protected GuiSwingLogEntryException expandPressedValue = null;

        public GuiSwingLogExceptionRenderer(GuiSwingLogManager manager, ContainerType type) {
            this.containerType = type;
            this.manager = manager;
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int h = ui.getScaledSizeInt(5);
            int w = ui.getScaledSizeInt(10);
            setBorder(BorderFactory.createEmptyBorder(h, w, h, w));
            setLayout(new BorderLayout(0, 0));
            setOpaque(false);

            message = new ExceptionMessageRenderer(manager, containerType);

            stackTrace = new ExceptionStackTraceRenderer();
            if (type.equals(ContainerType.List)) {
                add(stackTrace, BorderLayout.CENTER);
            }

            expandAction = new ExceptionExpandAction(this);
            {
                expandButton = new GuiSwingIcons.ActionButton(expandAction);
                expandButton.setHideActionText(true);
                expandButton.setPreferredSize(new Dimension(ui.getScaledSizeInt(32), ui.getScaledSizeInt(28)));
            }

            messageLinePane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            {
                messageLinePane.getInsets().set(0, 0, 0, 0);
                messageLinePane.setBorder(BorderFactory.createEmptyBorder());
                messageLinePane.setOpaque(false);
                messageLinePane.add(message);
                if (type.equals(ContainerType.List)) {
                    messageLinePane.add(expandButton);
                }
                add(messageLinePane, BorderLayout.NORTH);
            }
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
            message.setProperty(list);
            stackTrace.setProperty(list);
            return getTableCellRendererComponent(null, value, isSelected, cellHasFocus, index, 0);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            message.setProperty(table);
            stackTrace.setProperty(table);
            this.selected = isSelected;
            if (value instanceof GuiLogEntryException) {
                setValue((GuiLogEntryException) value, row <= -1);
            }
            return this;
        }

        public void setValue(GuiLogEntryException value, boolean forMouseEvents) {
            if (lastValue == null || !lastValue.equals(value)) {
                lastValue = value;
                message.setValue(value, forMouseEvents);
                stackTrace.setValue(value, forMouseEvents);
                expansionChanged();
            } else {
                if (lastValue instanceof GuiSwingLogEntryException) {
                    if (stackTrace.isExpanded() != ((GuiSwingLogEntryException) lastValue).isExpanded()) {
                        expansionChanged();
                    }
                }
            }
            setSelection();
        }



        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (selected && !containerType.equals(ContainerType.StatusBar)) {
                Dimension size = getSize();

                UIManagerUtil ui = UIManagerUtil.getInstance();
                int xy = ui.getScaledSizeInt(2);
                int hw = ui.getScaledSizeInt(5);
                int arc = ui.getScaledSizeInt(3);
                RoundRectangle2D.Float r = new RoundRectangle2D.Float(xy, xy, size.width - hw, size.height - hw, arc, arc);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(message.getSelectionBackground());
                g2.draw(r);
            }
        }

        //////////////////

        public void expansionChanged() {
            if (lastValue instanceof GuiSwingLogEntryException) {
                boolean expanded = ((GuiSwingLogEntryException) lastValue).isExpanded();

                expandButton.setIcon(expandAction.getIcon(expanded));
                expandButton.setPressedIcon(expandAction.getPressedIcon(expanded));

                stackTrace.setExpanded(expanded);
            }
            if (lastList != null && lastList.getModel() instanceof GuiSwingLogList.GuiSwingLogListModel) {
                GuiSwingLogList.GuiSwingLogListModel model = (GuiSwingLogList.GuiSwingLogListModel) lastList.getModel();
                model.fireRowChanged(lastValue);
            }
        }

        public void flipExpansion() {
            if (expandPressedValue != null) {
                GuiSwingLogEntryException ex = expandPressedValue;
                ex.setExpanded(!ex.isExpanded());
                expansionChanged();

            }
        }

        //////////////////

        public void setSelection() {
            if (lastValue instanceof GuiSwingLogEntryException) {
                GuiSwingLogEntryException ex = (GuiSwingLogEntryException) lastValue;
                selected = ex.isSelected();
                message.clearSelectionRange();
                stackTrace.clearSelectionRange();
                if (selected) {
                    ex.getSelections().forEach((view,range) ->
                        view.setSelectionRange(range[0], range[1]));
                }
            }
        }

        @Override
        public void mousePressed(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryException ex = (GuiSwingLogEntryException) entry;

            TextCellRenderer.mouseUpdateForComposition((GuiLogEntryException) entry, ex.getSelections(),
                    true, this, point, message, stackTrace);

            Point expandPoint = SwingUtilities.convertPoint(this, point, expandButton);
            expandPressedValue = expandButton.contains(expandPoint) ? ex : null;
        }

        @Override
        public void mouseDragged(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryException ex = (GuiSwingLogEntryException) entry;
            TextCellRenderer.mouseUpdateForComposition((GuiLogEntryException) entry, ex.getSelections(),
                    false, this, point, message, stackTrace);
        }

        @Override
        public void mouseReleased(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryException ex = (GuiSwingLogEntryException) entry;
            TextCellRenderer.mouseUpdateForComposition((GuiLogEntryException) entry, ex.getSelections(),
                    false, this, point, message, stackTrace);
            if (expandPressedValue != null) {
                expandButton.doClick();
            }
        }

        ///////////////////


        @Override
        public boolean updateFindPattern(String findKeyword) {
            return TextCellRenderer.updateFindPatternForComposition(findKeyword, message, stackTrace);
        }

        @Override
        public int findText(GuiSwingLogEntry entry, String findKeyword) {
            GuiSwingLogEntryException ex = (GuiSwingLogEntryException) entry;
            return TextCellRenderer.findTextForComposition(ex, findKeyword, message, stackTrace);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object focusNextFound(GuiSwingLogEntry entry, Object prevIndex, boolean forward) {
            GuiSwingLogEntryException ex = (GuiSwingLogEntryException) entry;
            boolean expanded = ex.isExpanded();
            try {
                return TextCellRenderer.getFocusNextFoundForComposition(ex, prevIndex, forward, message, stackTrace);
            } finally {
                if (ex.isExpanded() != expanded) {
                    expansionChanged();
                }
            }
        }

        @Override
        public String getSelectedText(GuiSwingLogEntry entry, boolean entireText) {
            GuiSwingLogEntryException ex = (GuiSwingLogEntryException) entry;
            return TextCellRenderer.getSelectedTextForComposition(ex, entireText, ex.getSelections(), message, stackTrace);
        }
    }

    /** a renderer for an exception message */
    public static class ExceptionMessageRenderer extends TextCellRenderer<GuiLogEntryException> {
        private static final long serialVersionUID = 1L;
        protected GuiSwingLogManager manager;
        protected ContainerType containerType;
        protected Map<Attribute, Object> messageTimeAttributes;
        protected Map<Attribute, Object> messageAttributes;

        protected int timeEnd;

        public ExceptionMessageRenderer(GuiSwingLogManager manager, ContainerType containerType) {
            this.manager = manager;
            this.containerType = containerType;

            messageTimeAttributes = GuiSwingLogEntryString.getTimeStyle();

            messageAttributes = GuiSwingLogEntryString.getBodyStyle();
            {
                messageAttributes.put(TextAttribute.FOREGROUND, new Color(132, 63, 50));
            }
            setFont(GuiSwingLogManager.getFont());
        }

        @Override
        protected void initBorder() {
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

        @Override
        public String format(GuiLogEntryException value) {
            if (value != null) {
                String msg = formatPreProcess(value.getException().toString());

                String time = formatPreProcess(manager.formatTime(value.getTime()));
                timeEnd = time.length();
                return time + " !!! " + (msg == null ? "" : msg);
            } else {
                timeEnd = 0;
                return super.format(null);
            }
        }

        @Override
        public float[] buildSize() {
            if (value instanceof GuiSwingLogEntry) {
                return ((GuiSwingLogEntry) value).sizeCache(this, super::buildSize);
            } else {
                return super.buildSize();
            }
        }

        @Override
        public LineInfo createLine(LineInfo prevLine, int lineIndex, int start, String line) {
            if (lineIndex == 0) {
                int pos = 0;
                if (start <= timeEnd && timeEnd < start + line.length()) {
                    pos = timeEnd - start;
                }
                return GuiSwingLogEntryString.createLineHead(start, line, pos, messageTimeAttributes, messageAttributes);
            } else {
                return GuiSwingLogEntryString.createLineFollowing(prevLine, lineIndex, start, line, messageAttributes);
            }
        }


        @Override
        public void paintLineSelection(Graphics2D g2, LineInfo line, TextLayout l, Color selectionColor, float lineX) {
            if (containerType.equals(ContainerType.StatusBar)) {
                return;
            }
            super.paintLineSelection(g2, line, l, selectionColor, lineX);
        }

        @Override
        public void paintCellSelection(Graphics g, Color selectionColor) {
            if (containerType.equals(ContainerType.StatusBar)) {
                return;
            }
            super.paintCellSelection(g, selectionColor);
        }
    }

    /** attribute info. for stack-trace line */
    public static class StackTraceAttributesForLine {
        public int line;
        public int start;
        public int end;
        public Map<Attribute,Object> attributes;

        public StackTraceAttributesForLine(int line, int start, int end, Map<Attribute, Object> attributes) {
            this.line = line;
            this.start = start;
            this.end = end;
            this.attributes = attributes;
        }
    }

    /** a renderer for a stack-trace lines */
    public static class ExceptionStackTraceRenderer extends TextCellRenderer<GuiLogEntryException> {
        private static final long serialVersionUID = 1L;
        protected StackTraceAttributeSet topSet;
        protected StackTraceAttributeSet middleSet;
        protected StackTraceAttributeSet lastSet;

        protected float[] expandedStackSize = new float[] {0,0};
        protected float[] collapsedStackSize = new float[] {0,0};
        protected boolean expanded;

        public ExceptionStackTraceRenderer() {
            initAttributes();
        }

        @Override
        protected void initBorder() {
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

        protected void initAttributes() {
            Map<Attribute,Object> defaultSet = new HashMap<>();
            defaultSet.put(TextAttribute.FONT, GuiSwingLogManager.getFont());
            setFont(GuiSwingLogManager.getFont());

            topSet = new StackTraceAttributeSet();
            topSet.set(defaultSet,
                    new Color(38, 115, 16),
                    new Color(103,53,44),
                    new Color(97, 88, 144),
                    new Color(35, 151, 165),
                    new Color(2, 142, 20),
                    new Color(105, 113, 31),
                    new Color(50, 91, 152),
                    new Color(122, 50, 108));

            middleSet = new StackTraceAttributeSet();
            middleSet.set(defaultSet,
                    new Color(48, 144, 20),
                    new Color(154,128,126),
                    new Color(168, 156, 213),
                    new Color(111, 127, 146),
                    new Color(135, 174, 129),
                    new Color(127, 145, 0),
                    new Color(96, 119, 146),
                    new Color(153, 108, 143));

            lastSet = new StackTraceAttributeSet();
            lastSet.set(defaultSet,
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
        public boolean setValue(GuiLogEntryException value, boolean forMouseEvents) {
            if (value instanceof GuiSwingLogEntryException) {
                GuiSwingLogEntryException ex = (GuiSwingLogEntryException) value;
                if (isValueSame(value, forMouseEvents)) {
                    return false;
                } else {
                    if (ex.getLines() != null && ex.getLineToAttrs() != null) {
                        //cached
                        this.value = value;
                        this.text = ex.getLines();
                        this.lineAttrs = ex.getLineToAttrs();
                        this.forMouseEvents = forMouseEvents;
                        buildFromValue();
                        return true;
                    } else {
                        boolean r = super.setValue(value, forMouseEvents);
                        ex.setLines(text);
                        ex.setLineToAttrs(lineAttrs);
                        return r;
                    }
                }
            } else {
                return super.setValue(value, forMouseEvents);
            }
        }

        @Override
        public void buildFromValue() {
            super.buildFromValue();
            expandedStackSize = buildSize();
            collapsedStackSize = getCollapsedSize();
        }

        @Override
        public LineInfo createLine(LineInfo prevLine, int lineIndex, int start, String line) {
            LineInfo info = super.createLine(prevLine, lineIndex, start, line);
            info.setIndent(4);
            if (lineAttrs != null && lineAttrs.containsKey(lineIndex)) {
                for (StackTraceAttributesForLine a : lineAttrs.get(lineIndex)) {
                    if (a.attributes != null && a.start < a.end) {
                        int e = Math.min(line.length(), a.end);
                        int s = Math.min(Math.max(a.start, 0), e);
                        if (s < e) {
                            info.attributedString.addAttributes(a.attributes,
                                    s, e);
                        }
                    }
                }
            }
            return info;
        }

        @Override
        public String format(GuiLogEntryException value) {
            Throwable ex = value.getException();
            lines.clear();
            lineAttrs.clear();
            line = new StringBuilder();

            Set<Throwable> history = Collections.newSetFromMap(new IdentityHashMap<>());
            formatStackTrace(ex, history, "", topSet, Collections.emptyList());

            if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) { //cut empty tail
                lines.remove(lines.size() - 1);
            }
            return String.join("\n", lines);
        }

        protected List<String> lines = new ArrayList<>();
        protected Map<Integer, List<StackTraceAttributesForLine>> lineAttrs = new HashMap<>();
        protected StringBuilder line = new StringBuilder();

        public void append(String str, Map<Attribute,Object> attrs) {
            str = formatPreProcess(str);
            int start = line.length();
            for (char c : str.toCharArray()) {
                if (c == '\n') {
                    if (line.length() > 0) {
                        int lineNum = lines.size();
                        lineAttrs.computeIfAbsent(lineNum, l -> new ArrayList<>())
                            .add(new StackTraceAttributesForLine(lineNum, start, line.length(), attrs));
                    }
                    lines.add(line.toString());
                    line = new StringBuilder();
                    start = 0;
                } else {
                    line.append(c);
                }
            }
            if (line.length() > 0) {
                int lineNum = lines.size();
                lineAttrs.computeIfAbsent(lineNum, l -> new ArrayList<>())
                        .add(new StackTraceAttributesForLine(lineNum, start, line.length(), attrs));
            }
        }

        public void formatStackTrace(Throwable ex, Set<Throwable> history,
                           String lineHead,
                           StackTraceAttributeSet attrSet,
                           List<StackTraceElement> prevStackTrace) {
            if (history.contains(ex)) {
                append("[CIRCULAR REFERENCE: " + ex + "]\n", attrSet.messageStyle);
                return;
            }
            history.add(ex);
            List<StackTraceElement> nextStackTrace = Arrays.asList(ex.getStackTrace());

            int commons = getCommons(nextStackTrace, prevStackTrace);
            nextStackTrace.subList(0, nextStackTrace.size() - commons)
                .forEach(e -> formatStackTraceElement(e, lineHead, attrSet));

            if (commons > 0) {
                append(lineHead + "\t... " + commons + " more\n", attrSet.defaultStyle);
            }

            if (ex.getCause() != null) {
                formatStackTraceEnclosing(ex.getCause(), history, "Caused by: ", lineHead, nextStackTrace);
            }

            Throwable[] ss = ex.getSuppressed();
            if (ss != null) {
                Arrays.stream(ss)
                        .forEach(s -> formatStackTraceEnclosing(s, history,
                                "Suppressed: ", lineHead + "\t", prevStackTrace));
            }

            if (ex.getCause() != null) {
                formatStackTraceEnclosing(ex.getCause(), history, "Caused by: ", lineHead, nextStackTrace);
            }
        }

        public void formatStackTraceEnclosing(Throwable ex, Set<Throwable> history,
                                              String head,
                                              String lineHead,
                                              List<StackTraceElement> stackTrace) {
            StackTraceAttributeSet attrSet;
            if (ex.getCause() == null) {
                attrSet = lastSet;
            } else {
                attrSet = middleSet;
            }
            append(lineHead + head, attrSet.defaultStyle);
            append(ex.toString() + "\n", attrSet.messageStyle);

            formatStackTrace(ex, history, lineHead, attrSet, stackTrace);
        }

        public int getCommons(List<StackTraceElement> nextStackTrace, List<StackTraceElement> prevStackTrace) {
            int commons = 0;
            int nextIdx = nextStackTrace.size() - 1;
            int prevIdx = prevStackTrace.size() - 1;
            for (; nextIdx >= 0 && prevIdx >= 0 &&
                    nextStackTrace.get(nextIdx).equals(prevStackTrace.get(prevIdx));
                 --nextIdx, --prevIdx) {  //from bottom to top, check commons
                ++commons;
            }
            return commons;
        }

        public void formatStackTraceElement(StackTraceElement e, String lineHead,
                                            StackTraceAttributeSet attrSet) {
            append(lineHead + "\t  at ", attrSet.defaultStyle);
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
                append(typePart.substring(0, moduleEnd + 1), attrSet.moduleStyle);
            }

            if (packEnd > 0) {
                append(classPart.substring(0, packEnd + 1), attrSet.packageStyle);
            }

            append(classPart.substring(packEnd + 1, enclosingStart + 1), attrSet.classNameStyle);

            if (enclosingStart < classPart.length()) {
                append(classPart.substring(enclosingStart + 1), attrSet.innerClassNameStyle);
            }

            append(str.substring(methodStart, methodEnd), attrSet.moduleStyle);
            append(str.substring(methodEnd), attrSet.fileNameStyle);
            append("\n", attrSet.defaultStyle);
        }

        //////////////

        public float[] getCollapsedSize() {
            int[] bs = getBorderSize();
            float[] baseSize = getBaseSize();
            return new float[] {
                    Math.max(1, maxWidth) * baseSize[0] + bs[0],
                    baseSize[1] + bs[1] + bs[1] };
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
            if (expanded) {
                setPreferredSize(new Dimension((int) expandedStackSize[0], (int) expandedStackSize[1]));
            } else {
                setPreferredSize(new Dimension((int) collapsedStackSize[0], (int) collapsedStackSize[1]));
            }
        }

        public boolean isExpanded() {
            return expanded;
        }

        @Override
        public Object getFocusNextFound(GuiLogEntryException value, Object prevIndex, boolean forward) {
            Object v = super.getFocusNextFound(value, prevIndex, forward);
            if (v instanceof LineInfoMatch && ((LineInfoMatch) v).line >= 2 && !expanded) {
                //found
                if (value instanceof GuiSwingLogEntryException) {
                    ((GuiSwingLogEntryException) value).setExpanded(true);
                }
                setExpanded(true);
            }
            return v;
        }
    }

    /**
     * an action for expanding stack-trace of an exception entry
     */
    public static class ExceptionExpandAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        protected GuiSwingLogExceptionRenderer renderer;

        public ExceptionExpandAction(GuiSwingLogExceptionRenderer renderer) {
            super("Expand");
            this.renderer = renderer;
            putValue(LARGE_ICON_KEY, getIcon(true));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, getPressedIcon(true));
        }

        public Icon getIcon(boolean expand) {
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int w = ui.getScaledSizeInt(16);
            int h = ui.getScaledSizeInt(14);
            return GuiSwingIcons.getInstance().getIcon("log-", expand ? "expand" : "collapse", w, h);
        }

        public Icon getPressedIcon(boolean expand) {
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int w = ui.getScaledSizeInt(16);
            int h = ui.getScaledSizeInt(14);
            return GuiSwingIcons.getInstance().getPressedIcon("log-", expand ? "expand" : "collapse", w, h);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            renderer.flipExpansion();
        }
    }

    /**
     * a set of attributes for stack traces
     */
    public static class StackTraceAttributeSet {
        public Map<Attribute, Object> defaultStyle;
        public Map<Attribute, Object> timeStyle;
        public Map<Attribute, Object> messageStyle;
        public Map<Attribute, Object> moduleStyle;
        public Map<Attribute, Object> packageStyle;
        public Map<Attribute, Object> classNameStyle;
        public Map<Attribute, Object> innerClassNameStyle;
        public Map<Attribute, Object> methodStyle;
        public Map<Attribute, Object> fileNameStyle;

        public void set(Map<Attribute, Object> base,
                        Color time, Color message, Color module, Color pack,
                        Color className, Color innerClass, Color method, Color fileName) {
            defaultStyle = new HashMap<>(base);
            timeStyle = new HashMap<>(base);
            timeStyle.put(TextAttribute.FOREGROUND, time);
            messageStyle = new HashMap<>(base);
            messageStyle.put(TextAttribute.FOREGROUND, message);
            moduleStyle = new HashMap<>(base);
            moduleStyle.put(TextAttribute.FOREGROUND, module);
            packageStyle = new HashMap<>(base);
            packageStyle.put(TextAttribute.FOREGROUND, pack);
            classNameStyle = new HashMap<>(base);
            classNameStyle.put(TextAttribute.FOREGROUND, className);
            innerClassNameStyle = new HashMap<>(base);
            innerClassNameStyle.put(TextAttribute.FOREGROUND, innerClass);
            methodStyle = new HashMap<>(base);
            methodStyle.put(TextAttribute.FOREGROUND, method);
            fileNameStyle = new HashMap<>(base);
            fileNameStyle.put(TextAttribute.FOREGROUND, fileName);
        }
    }
}
