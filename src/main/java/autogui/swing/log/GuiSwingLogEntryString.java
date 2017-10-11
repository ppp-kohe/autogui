package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryString;
import autogui.base.log.GuiLogManager;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiSwingLogEntryString extends GuiLogEntryString implements GuiSwingLogEntry {
    protected int selectionFrom;
    protected int selectionTo;

    public GuiSwingLogEntryString(String data) {
        super(data);
    }

    public GuiSwingLogEntryString(Instant time, String data) {
        super(time, data);
    }

    @Override
    public LogEntryRenderer getRenderer(GuiSwingLogManager manager, ContainerType type) {
        return new GuiSwingLogStringRenderer(manager);
    }

    public int getSelectionFrom() {
        return selectionFrom;
    }

    public void setSelectionFrom(int selectionFrom) {
        this.selectionFrom = selectionFrom;
    }

    public int getSelectionTo() {
        return selectionTo;
    }

    public void setSelectionTo(int selectionTo) {
        this.selectionTo = selectionTo;
    }

    public static void drawSelection(Dimension size, Graphics g) {
        RoundRectangle2D.Float r = new RoundRectangle2D.Float(2, 2, size.width - 4, size.height - 4, 3, 3);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UIManager.getColor("TextPane.selectionBackground"));
        g2.draw(r);
    }

    public static Style getTimeStyle(StyledDocument doc) {
        return getTimeStyle(doc, doc.getStyle(StyleContext.DEFAULT_STYLE));
    }

    public static Style getTimeStyle(StyledDocument doc, Style defaultStyle) {
        Style timeStyle = doc.addStyle("time", defaultStyle);
        StyleConstants.setForeground(timeStyle, new Color(48, 144, 20));
        return timeStyle;
    }

    /** returns the X position of headerEnd or &lt;0 value */
    public static float setHeaderStyle(JTextPane pane, String headerEnd, Style style) {
        pane.setSize(pane.getPreferredSize()); //this makes modelToView(i) return non-null rect

        StyledDocument doc = pane.getStyledDocument();

        int headerEndIndex = pane.getText().indexOf(headerEnd);
        if (headerEndIndex >= 0) {
            headerEndIndex += headerEnd.length() - 1;
            doc.setCharacterAttributes(0, headerEndIndex, style, true);
            try {
                Rectangle rect = pane.modelToView(headerEndIndex);
                return (float) rect.getMaxX();
            } catch (Exception ex) {
                //
            }
        }
        return -1;
    }

    public static class GuiSwingLogStringRenderer extends JTextPane
            implements TableCellRenderer, ListCellRenderer<GuiLogEntry>, LogEntryRenderer {
        protected GuiLogManager manager;
        protected boolean selected;

        protected TextPaneCellSupport support;

        protected Style defaultStyle;
        protected Style timeStyle;
        protected Style followingLinesStyle;

        public GuiSwingLogStringRenderer(GuiLogManager manager) {
            setBorder(BorderFactory.createEmptyBorder(7, 10, 3, 10));
            setFont(GuiSwingLogManager.getFont());
            this.manager = manager;
            //setEditable(false);

            setOpaque(false);

            support = new TextPaneCellSupport(this);


            StyledDocument doc = getStyledDocument();
            defaultStyle = doc.getStyle(StyleContext.DEFAULT_STYLE);

            timeStyle = getTimeStyle(doc, defaultStyle);

            followingLinesStyle = doc.addStyle("followingLines", defaultStyle);
            StyleConstants.setLeftIndent(followingLinesStyle, 200);
        }

        @Override
        public ListCellRenderer<GuiLogEntry> getTableCellRenderer() {
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (selected) {
                drawSelection(getSize(), g);
            }
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends GuiLogEntry> list, GuiLogEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            return getTableCellRendererComponent(null, value, isSelected, cellHasFocus, index, 0);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (row > -1) {
                selected = isSelected;
                support.setSelectionHighlightClear();
            }
            if (value instanceof GuiLogEntryString) {
                GuiLogEntryString str = (GuiLogEntryString) value;
                String text = formatString(str);
                setText(text);
                invalidate(); //important call for JTextPane: refresh layout

                updateStyle(text);

                if (value instanceof GuiSwingLogEntryString) {
                    GuiSwingLogEntryString sStr = (GuiSwingLogEntryString) value;
                    if (row > -1) { //row==-1: for mouse events
                        support.setSelectionHighlight(selected, sStr.getSelectionFrom(), sStr.getSelectionTo());
                        support.setFindHighlights();
                    }
                }
            }
            return this;
        }

        public void updateStyle(String text) {
            setSize(getPreferredSize()); //this makes modelToView(i) return non-null rect

            StyledDocument doc = getStyledDocument();

            float timeEnd = setHeaderStyle(this, "] ", timeStyle);
            if (timeEnd > 0) {
                StyleConstants.setLeftIndent(followingLinesStyle, timeEnd);
            }

            char prev = ' ';
            for (int i = 0, l = text.length(); i < l; ++i) {
                char c = text.charAt(i);
                if (prev == '\n') {
                    doc.setLogicalStyle(i, followingLinesStyle);
                }
                prev = c;
            }
        }

        public String formatString(GuiLogEntryString str) {
            return String.format("%s %s",
                    manager.formatTime(str.getTime()),
                    str.getData());
        }

        @Override
        public boolean isShowing() {
            return true;
        }

        @Override
        public void mousePressed(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            str.setSelectionTo(-1);
            str.setSelectionFrom(viewToModel(point));
        }

        @Override
        public void mouseDragged(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            str.setSelectionTo(viewToModel(point));
        }

        @Override
        public void mouseReleased(GuiSwingLogEntry entry, Point point) {

        }

        @Override
        public int findText(GuiSwingLogEntry entry, String findKeyword) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            String text = formatString(str);
            return support.findText(text, findKeyword).size();
        }

        @Override
        public Object focusNextFound(GuiSwingLogEntry entry, Object prevIndex, boolean forward) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            TextPaneCellMatch m = support.nextFindMatched(prevIndex, forward, entry);
            int[] range = support.getFindMatchedRange(m);
            if (range[1] > 0) {
                str.setSelectionFrom(range[0]);
                str.setSelectionTo(range[1]);
            }
            return m;
        }
    }

    public static class TextPaneCellSupport {
        protected JTextPane pane;
        protected Object selectionHighlight;
        protected List<Object> findHighlightKeys = new ArrayList<>();

        protected String findText;
        protected Pattern findPattern;
        protected List<Integer> findMatchedPositions = new ArrayList<>();

        protected Highlighter.HighlightPainter findPainter;

        public TextPaneCellSupport(JTextPane pane) {
            this.pane = pane;
            addSelectionHighlight();
            findPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.orange);
        }

        public void addSelectionHighlight() {
            try {
                Color c = UIManager.getColor("TextPane.selectionBackground");
                selectionHighlight = pane.getHighlighter().addHighlight(0, 0,
                        new DefaultHighlighter.DefaultHighlightPainter(c));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void setFindPainter(Highlighter.HighlightPainter findPainter) {
            this.findPainter = findPainter;
        }

        public Highlighter.HighlightPainter getFindPainter() {
            return findPainter;
        }

        public void setSelectionHighlightClear() {
            setSelectionHighlight(false, 0, 0);
        }

        public void setSelectionHighlight(boolean isSelected, int from, int to) {
            setHighlight(selectionHighlight, isSelected, from, to);
        }

        public void setHighlight(Object highlightKey, boolean isSelected, int from, int to) {
            if (highlightKey == null) {
                return;
            }
            try {
                if (from >= 0 && to >= 0 && isSelected) {
                    int len = pane.getDocument().getLength();
                    if (from > to) {
                        int tmp = to;
                        to = from;
                        from = tmp;
                    }
                    pane.getHighlighter().changeHighlight(highlightKey, Math.min(from, len), Math.min(to, len));
                } else {
                    pane.getHighlighter().changeHighlight(highlightKey, 0, 0);
                }
            } catch (Exception ex) {
                //nothing
            }
        }

        public void setFindHighlights(Pattern findPattern, Highlighter.HighlightPainter findPainter) {
            int hk = 0;
            if (findPattern != null) {
                Matcher m = findPattern.matcher(pane.getText());
                while (m.find()) {
                    int s = m.start();
                    int e = m.end();
                    if (hk < findHighlightKeys.size()) {
                        try {
                            pane.getHighlighter().changeHighlight(findHighlightKeys.get(hk), s, e);
                        } catch (Exception ex) {
                            //
                        }
                    } else {
                        try {
                            findHighlightKeys.add(pane.getHighlighter().addHighlight(s, e, findPainter));
                        } catch (Exception ex) {
                            //
                        }
                    }
                    ++hk;
                }
            }
            for (int size = findHighlightKeys.size(); hk < size; ++hk) {
                try {
                    pane.getHighlighter().changeHighlight(findHighlightKeys.get(hk), 0, 0);
                } catch (Exception ex) {
                    //
                }
            }
        }

        public static void click(Map<JTextComponent, int[]> selectionMap, boolean start, Point clickPoint, JComponent component) {
            if (component instanceof JTextComponent) {
                JTextComponent text = (JTextComponent) component;

                Rectangle bounds = text.getBounds();
                Point viewPoint = new Point(clickPoint);
                if (bounds.getMaxY() <= viewPoint.getY()) {
                    viewPoint.x = (int) bounds.getMaxX() + 1;
                } else if (viewPoint.getY() <= bounds.getY()){
                    viewPoint.x = (int) bounds.getX() - 1;
                }

                int pos = text.viewToModel(viewPoint);
                selectionMap.computeIfAbsent(text, k -> new int[] {-1, -1})[start ? 0 : 1] = pos;
            }
            for (Component c : component.getComponents()) {
                if (c instanceof JComponent) {
                    click(selectionMap, start, SwingUtilities.convertPoint(component, clickPoint, c), (JComponent) c);
                }
            }
        }

        public void setSelectionHighlight(Map<JTextComponent, int[]> selectionMap) {
            int[] sel = selectionMap.get(pane);
            if (sel == null) {
                setSelectionHighlightClear();
            } else {
                setSelectionHighlight(true, sel[0], sel[1]);
            }
        }

        ////////////

        public List<Integer> findText(String text, String findKeyword) {
            if (findKeyword == null || findKeyword.isEmpty()) {
                findText = null;
                findPattern = null;
                findMatchedPositions.clear();
            } else if (findText == null || !findText.equals(findKeyword)) {
                //update
                findText = findKeyword;
                findPattern = Pattern.compile(Pattern.quote(findKeyword));
                Matcher m = findPattern.matcher(text);
                findMatchedPositions.clear();
                while (m.find()) {
                    findMatchedPositions.add(m.start());
                }
            }
            return findMatchedPositions;
        }

        public List<Integer> getFindMatchedPositions() {
            return findMatchedPositions;
        }

        public String getFindText() {
            return findText;
        }

        public Pattern getFindPattern() {
            return findPattern;
        }

        public void setFindHighlights() {
            setFindHighlights(findPattern, findPainter);
        }

        public TextPaneCellMatch nextFindMatched(Object prevIndex, boolean forward, Object... keys) {
            if (findMatchedPositions.isEmpty()) {
                return null;
            }
            if (prevIndex == null ||
                    !(prevIndex instanceof TextPaneCellMatch) ||
                    !((TextPaneCellMatch) prevIndex).sameKeys(keys)) {
                if (forward) {
                    return new TextPaneCellMatch(0, keys);
                } else {
                    return new TextPaneCellMatch(findMatchedPositions.size() - 1, keys);
                }
            } else {
                int i = ((TextPaneCellMatch) prevIndex).index + (forward ? 1 : -1);
                if (0 <= i && i < findMatchedPositions.size()) {
                    return new TextPaneCellMatch(i, keys);
                } else {
                    return null;
                }
            }
        }

        public int[] getFindMatchedRange(TextPaneCellMatch m) {
            if (m == null || !(0 <= m.index && m.index < findMatchedPositions.size())) {
                return new int[] {0, 0};
            } else {
                int n = findMatchedPositions.get(m.index);
                return new int[] {n, n + findText.length()};
            }
        }
    }

    public static class TextPaneCellMatch {
        public Object[] keys;
        public int index;

        public TextPaneCellMatch(int index, Object... keys) {
            this.index = index;
            this.keys = keys;
        }

        public boolean sameKeys(Object... keys) {
            return Arrays.equals(this.keys, keys);
        }

        public boolean sameKeysHead(Object... fixedKeys) {
            if (keys.length == fixedKeys.length + 1) {
                for (int i = 0, l = fixedKeys.length; i < l; ++i) {
                    if (!Objects.equals(fixedKeys[i], keys[i])) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        public Object key(int i) {
            return 0 <= i && i < keys.length ? keys[i] : null;
        }

        @Override
        public String toString() {
            return Arrays.toString(keys) + ":" + index;
        }
    }

    public static TextPaneCellMatch nextFindMatchedList(List<TextPaneCellSupport> supports,
                                                        Object prevIndex, boolean forward, Object... fixedKeys) {
        //keys: ...fixedKeys, index
        if (prevIndex != null && prevIndex instanceof TextPaneCellMatch &&
                ((TextPaneCellMatch) prevIndex).sameKeysHead(fixedKeys)) {
            TextPaneCellMatch pm = (TextPaneCellMatch) prevIndex;
            int supportIndex = (Integer) pm.key(fixedKeys.length);
            TextPaneCellMatch m = supports.get(supportIndex)
                    .nextFindMatched(pm, forward, pm.keys);
            if (m != null) {
                return m;
            } else {
                supportIndex += (forward ? 1 : -1);
                while (0 <= supportIndex && supportIndex < supports.size()) {
                    m = supports.get(supportIndex)
                            .nextFindMatched(null, forward, fixedKeysAndSupportIndex(fixedKeys, supportIndex));
                    if (m != null) {
                        return m;
                    }
                    supportIndex += (forward ? 1 : -1);
                }
                return null;
            }
        } else {
            int supportIndex = forward ? 0 : supports.size() - 1;
            return supports.get(supportIndex)
                    .nextFindMatched(null, forward, fixedKeysAndSupportIndex(fixedKeys, supportIndex));
        }
    }

    private static Object[] fixedKeysAndSupportIndex(Object[] fixedKeys, int supportIndex) {
        Object[] nextKeys = Arrays.copyOf(fixedKeys, fixedKeys.length + 1);
        nextKeys[fixedKeys.length] = supportIndex;
        return nextKeys;
    }
}
