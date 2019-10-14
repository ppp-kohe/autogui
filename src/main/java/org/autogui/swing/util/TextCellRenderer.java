package org.autogui.swing.util;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * multi-line text cell renderer for JList and JTable
 * @param <ValueType> the type of cell values
 */
public class TextCellRenderer<ValueType> extends JPanel
    implements TableCellRenderer, ListCellRenderer<ValueType> {
    private static final long serialVersionUID = 1L;
    protected boolean selected;   //cell selection
    protected int selectionStart; //text selection range
    protected int selectionEnd;

    protected List<LineInfo> lines;

    protected BufferedImage testImage;
    protected float baseWidth;
    protected float baseHeight;

    protected ValueType value;
    protected boolean forMouseEvents;
    protected String text = "";
    protected String findText;
    protected Pattern findPattern;
    protected int maxWidth;
    protected LineInfoMatch lastMatch;

    protected Color selectionForeground;
    protected Color selectionBackground;

    public TextCellRenderer() {
        lines = new ArrayList<>();
        setOpaque(false);
        initColor();
        initBorder();
    }

    protected void initColor() {
        UIManagerUtil ui = UIManagerUtil.getInstance();
        Color b = ui.getLabelBackground();
        setBackground(new Color(b.getRed(), b.getGreen(), b.getBlue(), 0));
        setForeground(ui.getLabelForeground());
        setSelectionForeground(ui.getTextPaneSelectionBackground());
        setSelectionBackground(ui.getTextPaneSelectionForeground());
    }

    protected void initBorder() {
        UIManagerUtil ui = UIManagerUtil.getInstance();
        setBorder(BorderFactory.createEmptyBorder(
                ui.getScaledSizeInt(7), ui.getScaledSizeInt(10),
                ui.getScaledSizeInt(7), ui.getScaledSizeInt(10)));
    }

    public String getText() {
        return text;
    }

    public String getTextSelection() {
        int s = selectionStart;
        int e = selectionEnd;
        if (s > e) {
            int t = e;
            e = s;
            s = t;
        }

        if (e == -1 && s == -1) {
            return "";
        } else {
            if (e == -1) {
                e = text.length();
            }
            e = Math.max(0, Math.min(e, text.length()));
            s = Math.max(s, Math.min(s, e));
            return text.substring(s, e);
        }
    }

    public ValueType getValue() {
        return value;
    }

    public List<LineInfo> getLines() {
        return lines;
    }

    public Pattern getFindPattern() {
        return findPattern;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ValueType> list, ValueType value, int index, boolean isSelected, boolean cellHasFocus) {
        setProperty(list);
        return getTableCellRendererComponent(null, value, isSelected, cellHasFocus, index, 0);
    }

    public void setProperty(JList<?> list) {
        if (list != null) {
            setSelectionForeground(list.getSelectionForeground());
            setSelectionBackground(list.getSelectionBackground());
            setForeground(list.getForeground());
            setBackground(list.getBackground());
        }
    }

    public void setProperty(JTable list) {
        if (list != null) {
            setSelectionForeground(list.getSelectionForeground());
            setSelectionBackground(list.getSelectionBackground());
            setForeground(list.getForeground());
            setBackground(list.getBackground());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setProperty(table);
        boolean forMouseEvents = row <= -1;
        if (!forMouseEvents) {
            this.selected = isSelected;
            clearSelectionRange();
        }

        setValue((ValueType) value, forMouseEvents);
        return this;
    }

    /**
     * called from {@link #getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)}
     *    (also {@link #getListCellRendererComponent(JList, Object, int, boolean, boolean)})
     * <ol>
     *     <li>check the value by {@link #isValueSame(Object, boolean)}. if the value is same, returns false </li>
     *     <li>updates value and text with {@link #format(Object)}</li>
     *     <li>{@link #buildFromValue()}: constructs {@link LineInfo}s
     *       ({@link #buildLines(String, List)} and {@link #createLine(LineInfo, int, int, String)}), and
     *         update preferredSize with {@link #buildSize()}.
     *         Also, if forMouseEvents is false, {@link #setSelectionFromValue(Object)}</li>
     *
     * </ol>
     * @param value a new value
     * @param forMouseEvents {@link #setSelectionFromValue(Object)} will be skipped
     * @return true if updated
     */
    public boolean setValue(ValueType value, boolean forMouseEvents) {
        if (isValueSame(value, forMouseEvents)) {
            return false;
        }
        this.value = value;
        this.text = format(value);
        this.forMouseEvents = forMouseEvents;
        buildFromValue();
        return true;
    }

    public boolean isValueSame(ValueType value, boolean forMouseEvents) {
        return this.value != null && this.value.equals(value) &&
                (this.forMouseEvents == forMouseEvents || !this.forMouseEvents); //non-mouseEvents includes mouseEvents
    }

    public String format(ValueType value) {
        if (value == null) {
            return "";
        } else {
            return Objects.toString(value);
        }
    }

    public void buildFromValue() {
        maxWidth = buildLines(text, lines);
        float[] size = buildSize();
        setPreferredSize(new Dimension((int) size[0], (int) size[1]));
        invalidate();

        if (!forMouseEvents) {
            setSelectionFromValue(value);
        }
    }

    /**
     * @param text source text
     * @param lines cleared and appended
     * @return max size of {@link LineInfo#getWidth()} in the lines
     */
    public int buildLines(String text, List<LineInfo> lines) {
        lines.clear();
        int i = 0;
        int maxWidth = 0;
        int lineIndex = 0;
        LineInfo prev = null;
        for (String line : text.split("\\n")) {
            LineInfo info = createLine(prev, lineIndex, i, buildLinePreProcess(line));
            lines.add(info);
            prev = info;
            i += line.length() + 1;
            ++lineIndex;
            maxWidth = Math.max(maxWidth, info.getWidth());
        }
        return maxWidth;
    }

    /**
     * remove tabs from the line; for avoiding displaying tofu by AttributedString
     * @param line a line string without newlines
     * @return remove tabs
     * @since 1.2
     */
    public String buildLinePreProcess(String line) {
        return line.replaceAll("\\t", "    ");
    }

    /**
     * create a line-info
     * @param prevLine a previous line-info or null for first line
     * @param lineIndex the line number index
     * @param start the start position of the line
     * @param line the line string without new-line
     * @return a created line info for the line
     */
    public LineInfo createLine(LineInfo prevLine, int lineIndex, int start, String line) {
        AttributedString a = new AttributedString(line);
        return new LineInfo(a, start, line.length() + start);
    }

    /**
     * multiply maxWidth set by {@link #buildFromValue()} and size of lines
     *    by base char size ({@link #getBaseSize()}) , and border size by {@link #getBorderSize()}
     * @return {width, height}
     */
    public float[] buildSize() {
        int[] bs = getBorderSize();
        float[] baseSize = getBaseSize();
        return new float[] {
                Math.max(1, maxWidth) * baseSize[0] + bs[0],
                Math.max(1, lines.size()) * baseSize[1] + bs[1] };
    }

    //////////////////

    /**
     * in the class, just call {@link #setFindHighlights()}
     * @param value the new value
     */
    public void setSelectionFromValue(ValueType value) {
        setFindHighlights();
    }

    public void clearSelectionRange() {
        this.selectionStart = -1;
        this.selectionEnd = -1;
    }

    public void setSelectionRange(int from, int to) {
        this.selectionStart = from;
        this.selectionEnd = to;
    }

    /**
     * constructs find-ranges for each lines matched by findPattern.
     *  thus, {@link #updateFindPattern(String)} or {@link #updateFindPattern(Pattern)} needs to be called before.
     * @return number of matched portions
     */
    public int setFindHighlights() {
        lines.forEach(LineInfo::clearFindRanges);
        if (findPattern == null) {
            return 0;
        }
        Matcher m = findPattern.matcher(text);
        Iterator<LineInfo> lineIter = lines.iterator();
        if (!lineIter.hasNext()) {
            return 0;
        }
        int count = 0;
        LineInfo line = lineIter.next();
        while (m.find()) {
            ++count;
            int start = m.start();
            int end = m.end();
            while (line.end <= start && lineIter.hasNext()) {
                line = lineIter.next();
            }
            if (line.end <= start) {
                break;
            }
            line.addFindRange(start, end);
            while (line.end <= end && lineIter.hasNext()) {
                line = lineIter.next();
                line.addFindRange(start, end);
            }
        }
        return count;
    }

    /**
     * update findPattern. findText becomes null
     * @param newPattern a new pattern
     * @return true if updated.
     */
    public boolean updateFindPattern(Pattern newPattern) {
        if (newPattern == null) {
            boolean prevIsNull = (findPattern != null);
            findText = null;
            findPattern = null;
            return prevIsNull;
        } else if (findPattern == null || !findPattern.equals(newPattern)) {
            findText = null;
            findPattern = newPattern;
            return true;
        } else {
            return false;
        }
    }

    /**
     * update findText and findPattern
     * @param findKeyword a new string, nullable
     * @return true if the string is updated. if true, it needs to be called {@link #setFindHighlights()}
     */
    public boolean updateFindPattern(String findKeyword) {
        if (findKeyword == null || findKeyword.isEmpty()) {
            boolean prevIsNull = (findText != null);
            findText = null;
            findPattern = null;
            return prevIsNull;
        } else if (findText == null || !findText.equals(findKeyword)) {
            findText = findKeyword;
            findPattern = Pattern.compile(Pattern.quote(findKeyword));
            return true;
        } else {
            return false;
        }
    }

    /**
     * focus next matched range and return it.
     *   <pre>
     *       m = null;
     *       m = getFocusNextFound(v, m, true);
     *       m = getFocusNextFound(v, m, true);
     *       ...
     *   </pre>
     * @param value the value to be set
     * @param prevIndex the previous focus object or initially null
     * @param forward true for forward or false for backward
     * @return a focus object {@link LineInfoMatch} or null
     */
    public Object getFocusNextFound(ValueType value, Object prevIndex, boolean forward) {
        setValue(value, false);
        if (!(prevIndex instanceof LineInfoMatch) || //null
                !((LineInfoMatch) prevIndex).sameKeys(this, value)) {
            if (forward) {
                int i = 0;
                for (LineInfo line : lines) {
                    if (!line.getFindRanges().isEmpty()) {
                        lastMatch = new LineInfoMatch(i, 0, this, value);
                        return lastMatch;
                    }
                    ++i;
                }
                lastMatch = null;
                return null;
            } else {
                for (int i = lines.size() - 1; i >= 0; --i) {
                    LineInfo line = lines.get(i);
                    if (!line.getFindRanges().isEmpty()) {
                        lastMatch = new LineInfoMatch(i, line.getFindRanges().size() - 1,
                                this, value);
                        return lastMatch;
                    }
                }
                lastMatch = null;
                return null;
            }
        } else {
            LineInfoMatch m = (LineInfoMatch) prevIndex;
            lastMatch = forward ? m.next(lines) : m.previous(lines);
            return lastMatch;
        }
    }

    //////////////////

    /**
     * change selection range by mouse pressing, dragging or releasing.
     *  each rs values will be updated by {@link #setValue(Object, boolean)} with forMouseEvents=true.
     *  update map entries of range and selection-range of rs by {@link #setSelectionRange(int, int)}
     * @param value the value to be set for each rs
     * @param ranges updated ranges
     * @param pressInit if true, it means first pressing and clear ranges and updates both "from" and "to".
     *                    otherwise just updates "to"
     * @param pointComponent the coordinate base of point
     * @param point a point in pointComponent
     * @param rs components within pointComponent
     * @param <V> the type of cell values
     */
    @SafeVarargs
    @SuppressWarnings({"unchecked", "varargs"})
    public static <V> void mouseUpdateForComposition(V value, Map<TextCellRenderer<?>,int[]> ranges, boolean pressInit,
                                                     Component pointComponent, Point point, TextCellRenderer<V>... rs) {
        if (pressInit) {
            ranges.clear();
        }
        Arrays.stream(rs)
                .forEach(r -> r.setValue(value, true));
        pointComponent.invalidate();
        pointComponent.validate(); //update layout of the parent component

        for (TextCellRenderer<V> r : rs) {
            int idx = r.getIndex(SwingUtilities.convertPoint(pointComponent, point, r));
            int[] range = ranges.computeIfAbsent(r, v -> new int[2]);
            if (pressInit) {
                range[0] = idx;
                range[1] = idx;
            } else {
                range[1] = idx;
            }
            r.setSelectionRange(range[0], range[1]);
        }
    }

    @SafeVarargs
    @SuppressWarnings({"unchecked", "varargs"})
    public static <V> boolean updateFindPatternForComposition(String findKeyword, TextCellRenderer<V>... rs) {
        List<Boolean> bs = Arrays.stream(rs)
            .map(r -> r.updateFindPattern(findKeyword))
            .collect(Collectors.toList());

        for (boolean b : bs) {
            if (b) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    @SuppressWarnings({"unchecked", "varargs"})
    public static <V> int findTextForComposition(V value, String findKeyword, TextCellRenderer<V>... rs) {
        for (TextCellRenderer<V> r : rs) {
            r.setValue(value, false);
        }
        updateFindPatternForComposition(findKeyword, rs);
        return Arrays.stream(rs)
                .mapToInt(TextCellRenderer::setFindHighlights)
                .sum();
    }

    @SafeVarargs
    @SuppressWarnings({"unchecked", "varargs"})
    public static <V> Object getFocusNextFoundForComposition(V value, Object prevIndex, boolean forward, TextCellRenderer<V>... rs) {
        List<TextCellRenderer<V>> list = Arrays.asList(rs);
        int idx;
        if (prevIndex instanceof LineInfoMatch) {
            TextCellRenderer.LineInfoMatch prevMatch = (TextCellRenderer.LineInfoMatch) prevIndex;
            prevIndex = ((TextCellRenderer<V>) prevMatch.renderer).getFocusNextFound(value, prevIndex, forward);
            idx = list.indexOf(prevMatch.renderer);
        } else {
            idx = (forward ? -1 : list.size());
        }
        while (prevIndex == null) {
            idx += (forward ? 1 : -1);
            if (0 <= idx && idx < list.size()) {
                prevIndex = list.get(idx).getFocusNextFound(value, null, forward);
            } else {
                break;
            }
        }
        return prevIndex;
    }

    @SafeVarargs
    public static <V> void setValueForComposition(V value, Map<TextCellRenderer<?>,int[]> rangesOpt,
                                                  boolean forMouseEvents,
                                                  TextCellRenderer<V>... rs) {
        for (TextCellRenderer<V> r : rs) {
            r.setValue(value, forMouseEvents);
            int[] range = (rangesOpt == null ? null : rangesOpt.get(r));
            if (rangesOpt != null) {
                r.clearSelectionRange();
            }
            if (range != null) {
                r.setSelectionRange(range[0], range[1]);
            }
        }
    }

    @SafeVarargs
    @SuppressWarnings({"unchecked", "varargs"})
    public static <V> String getSelectedTextForComposition(V value, boolean entireText,
                                                           Map<TextCellRenderer<?>,int[]> rangesOpt,
                                                           TextCellRenderer<V>... rs) {
        setValueForComposition(value, rangesOpt, false, rs);

        if (entireText) {
            return Arrays.stream(rs)
                    .map(TextCellRenderer::getText)
                    .collect(Collectors.joining("\n"));
        } else {
            return Arrays.stream(rs)
                    .map(TextCellRenderer::getTextSelection)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining("\n"));
        }
    }

    //////////////////

    public float[] getBaseSize() {
        if (testImage == null) {
            testImage = new BufferedImage(32, 32, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = testImage.createGraphics();

            AttributedString a = new AttributedString("M");
            setDefaultAttributes(a, 1);

            TextLayout l = new TextLayout(a.getIterator(), g.getFontRenderContext());
            baseWidth = l.getAdvance();
            baseHeight = l.getAscent() + l.getDescent() + l.getLeading();
            g.dispose();
        }
        return new float[] {baseWidth, baseHeight};
    }

    protected void setDefaultAttributes(AttributedString a, int len) {
        Font font = getFont();
        if (font != null) {
            a.addAttribute(TextAttribute.FONT, font, 0, len);
        }
    }

    public int[] getBorderSize() {
        Insets insets = getBorder().getBorderInsets(this);
        return new int[] {
                insets.left + insets.right,
                insets.top + insets.bottom};
    }

    /////////////////

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }

    public void setSelectionBackground(Color selectionBackground) {
        this.selectionBackground = selectionBackground;
    }

    public void setSelectionForeground(Color selectionForeground) {
        this.selectionForeground = selectionForeground;
    }

    public Color getLineFindColor() {
        return Color.orange;
    }

    public Color getLineFindMatchColor() {
        return Color.yellow;
    }

    protected Map<Color, Map<Color, Color>> textToBackToColor = new HashMap<>();

    @Override
    protected void paintComponent(Graphics g) {
        getBorder().paintBorder(this, g, 0, 0, getWidth(), getHeight());
        Insets insets = getBorder().getBorderInsets(this);

        Graphics2D g2 = (Graphics2D) g;
        paintSetUpGraphics(g2);
        FontRenderContext frc = g2.getFontRenderContext();
        float x = insets.left;
        float y = insets.top;
        Color findColor = getLineFindColor();
        Color findMatchColor = getLineFindMatchColor();

        Color selectionColor = getSelectionBackground();
        Color selectionTextColor = getSelectionForeground();
        Color backgroundColor = getBackground();
        Color foregroundColor = getForeground();

        int lineIndex = 0;
        LineInfo prev = null;
        float lineX = 0;
        g2.translate(x, y);
        for (LineInfo line : lines) {
            TextLayout l = line.getLayout(frc, this.selectionStart, this.selectionEnd,
                    foregroundColor, backgroundColor, selectionTextColor, selectionColor, textToBackToColor);
//            TextLayout l = line.getLayout(frc);

            float ascent = l.getAscent();
            y += ascent;
            g2.translate(0, ascent);

            lineX = paintStartX(lineIndex, prev, lineX, line, l, frc);

            paintLineSelection(g2, line, l, selectionColor, lineX);
            paintLineFinds(g2, line, l, lineIndex, findColor, findMatchColor, lineX);

            g2.setPaint(foregroundColor);

            l.draw(g2, lineX, 0);

            float descent = l.getDescent() + l.getLeading();
            y += descent;
            g2.translate(0, descent);

            ++lineIndex;
            prev = line;
        }
        g2.translate(-x, -y);

        if (selected) {
            paintCellSelection(g2, selectionColor);
        }
    }

    public void paintSetUpGraphics(Graphics2D g2) {
        g2.setFont(getFont());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(getForeground());
    }

    public void paintLineSelection(Graphics2D g2, LineInfo line, TextLayout l, Color selectionColor, float lineX) {
        g2.setPaint(selectionColor);
        Shape sel = line.getSelectionShape(l, this.selectionStart, this.selectionEnd);
        if (sel != null){
            g2.translate(lineX, 0);
            g2.fill(sel);
            g2.translate(-lineX, 0);
        }
    }

    public void paintLineFinds(Graphics2D g2, LineInfo line, TextLayout l, int lineIndex, Color findColor, Color findMatchColor, float lineX) {

        g2.translate(lineX, 0);
        int i = 0;
        for (int[] range : line.getFindRanges()) {
            Shape find = line.getSelectionShape(l, range[0], range[1]);
            if (isLastMatch(lineIndex, i)) {
                g2.setPaint(findMatchColor);
            } else {
                g2.setPaint(findColor);
            }
            if (find != null) {
                g2.fill(find);
            }
            ++i;
        }
        g2.translate(-lineX, 0);
    }

    public boolean isLastMatch(int lineIndex, int rangeIndex) {
        if (lastMatch != null) {
            return lastMatch.value == this.value && lastMatch.line == lineIndex && lastMatch.range == rangeIndex;
        } else {
            return false;
        }
    }

    public float paintStartX(int lineIndex, LineInfo prev, float prevX, LineInfo line, TextLayout l,
                             FontRenderContext frc) {
        if (line.getIndent() > 0) {
            return line.getIndent() * getBaseSize()[0];
        }
        return 0;
    }


    public void paintCellSelection(Graphics g, Color selectionColor) {
        Dimension size = getSize();
        UIManagerUtil ui = UIManagerUtil.getInstance();
        int xy = ui.getScaledSizeInt(2);
        int wh = ui.getScaledSizeInt(5);
        int arc = ui.getScaledSizeInt(3);
        RoundRectangle2D.Float r = new RoundRectangle2D.Float(xy, xy, size.width - wh, size.height - wh, arc, arc);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(selectionColor);
        g2.draw(r);
    }

    /**
     * @param point the component coordinate
     * @return the character index for the point
     */
    public int getIndex(Point point) {
        Insets insets = getBorder().getBorderInsets(this);
        Graphics2D g2 = (Graphics2D) getGraphics();
        boolean disposeGraphics = false;
        if (g2 == null) {
            getBaseSize(); //setup testImage
            g2 = testImage.createGraphics();
            disposeGraphics = true;
        }
        paintSetUpGraphics(g2);

        int end = 0;
        try {
            FontRenderContext frc = g2.getFontRenderContext();
            float baseX = insets.left;
            float y = insets.top;
            if (point.y < y) {
                return 0;
            }
            LineInfo prev = null;
            int lineIndex = 0;
            float startX = 0;
            for (LineInfo line : lines) {
                TextLayout l = line.getLayout(frc);
                float w = l.getAdvance();
                float h = l.getAscent() + l.getDescent() + l.getLeading();

                startX = paintStartX(lineIndex, prev, startX, line, l, frc);
                float x = startX + baseX;

                if (y <= point.y && point.y < y + h) {
                    if (x <= point.x && point.x < x + w) {
                        TextHitInfo info = l.hitTestChar(point.x - x, point.y - y);
                        return line.start + info.getCharIndex();
                    } else if (point.x < x) {
                        return line.start;
                    } else if ((x + w) <= point.x) {
                        return line.end;
                    }
                }

                y += h;
                end = line.end;
                ++lineIndex;
                prev = line;
            }
        } finally {
            if (disposeGraphics) {
                g2.dispose();
                lines.forEach(LineInfo::clearLayout);
            }
        }
        return end;
    }

    /**
     * a line holding {@link AttributedString}
     */
    public static class LineInfo {
        public AttributedString attributedString;
        public int start;
        public int end;

        public int indent;

        protected TextLayout layout;
        protected int selectionStart;
        protected int selectionEnd;
        protected List<Object> layoutState = Collections.emptyList();

        protected List<int[]> findRanges;

        public LineInfo(AttributedString attributedString, int start, int end) {
            if (attributedString.getIterator().next() == CharacterIterator.DONE) {
                //empty string cause an error at creating TextLayout
                attributedString = new AttributedString(" ");
            }
            this.attributedString = attributedString;
            this.start = start;
            this.end = end;
        }

        /**
         * @return number of characters including indents
         */
        public int getWidth() {
            return indent + (end - start);
        }

        /**
         * @param indent number of indent characters
         */
        public void setIndent(int indent) {
            this.indent = indent;
        }

        public int getIndent() {
            return indent;
        }

        /**
         * @param frc the font-rendering-context used when creating a new text-layout
         * @return a new or cached text-layout
         */
        public TextLayout getLayout(FontRenderContext frc) {
            if (layout == null) {
                layoutState = Collections.emptyList();
                layout = new TextLayout(attributedString.getIterator(), frc);
                return layout;
            } else {
                //the empty state always included any other layoutState
                return layout;
            }
        }

        public TextLayout getLayout(FontRenderContext frc, int selectionStart, int selectionEnd,
                                             Color foreground, Color background, Color selectionForeground, Color selectionBackground,
                                             Map<Color, Map<Color, Color>> textToBackToColor) {
            if (selectionStart > selectionEnd) {
                int tmp = selectionStart;
                selectionStart = selectionEnd;
                selectionEnd = tmp;
            }
            int ss = Math.min(Math.max(selectionStart, start), end);
            int se = Math.min(Math.max(selectionEnd, start), end + 1);

            List<Object> newLayoutState = Arrays.asList(ss, se, foreground, background, selectionForeground, selectionBackground);
            if (layout != null && newLayoutState.equals(layoutState)) {
                return layout; //reuse
            }
            this.layoutState = newLayoutState;
            this.selectionStart = ss;
            this.selectionEnd = se;

            AttributedString selStr = colorUpdate(attributedString, foreground, background,
                    selectionForeground, selectionBackground, textToBackToColor);
            layout = new TextLayout(selStr.getIterator(), frc);
            return layout;
        }

        public AttributedString colorUpdate(AttributedString attrStr,
                                            Color text, Color background, Color selectionForeground, Color selectionBackground,
                                            Map<Color, Map<Color, Color>> textToBackToColor) {
            int i = 0;
            Map<Color, List<int[]>> colorRange = new HashMap<>();
            AttributedCharacterIterator iter = attrStr.getIterator();
            for (char c = iter.first(); c != AttributedCharacterIterator.DONE; c = iter.next(), ++i) {
                Map<AttributedCharacterIterator.Attribute,Object> attr = iter.getAttributes();
                Color customColor = (attr == null ? null : (Color) attr.get(TextAttribute.FOREGROUND));
                Color f;
                Color b;
                Color rf;
                if (selectionStart <= (i + start) && (i + start) < selectionEnd) {
                    b = selectionBackground;
                    rf = selectionForeground;
                    if (customColor != null) {
                        f = customColor;
                    } else {
                        f = selectionForeground;
                    }
                } else {
                    b = background;
                    rf = text;
                    if (customColor != null) {
                        f = customColor;
                    } else {
                        f = text;
                    }
                }

                Color computedColor = textToBackToColor.computeIfAbsent(f, tc -> new HashMap<>())
                        .computeIfAbsent(b, bc -> getColor(f, rf, b));
                List<int[]> r = colorRange.computeIfAbsent(computedColor, cc -> new ArrayList<>());
                if (!r.isEmpty() && r.get(r.size() - 1)[1] == i) {
                    r.get(r.size() - 1)[1] = i + 1;
                } else {
                    r.add(new int[] {i, i + 1});
                }
            }
            AttributedString colored = new AttributedString(attrStr.getIterator());
            colorRange.forEach((c, rs) -> {
                rs.forEach(r -> {
                    if (r[0] < r[1]) {
                        colored.addAttribute(TextAttribute.FOREGROUND, c, r[0], r[1]);
                    }
                });
            });
            return colored;
        }

        public Color getColor(Color foreground, Color regularForeground, Color background) {
            double bb = brightness(background);
            double rfb = brightness(regularForeground);
            double fb = brightness(foreground);
            //foreground is a color for dark-foreground and bright-background
            if (rfb > bb && fb - bb < 0.2) { //dark mode, but the foreground brightness is not sufficient against the background
                float[] hsb = Color.RGBtoHSB(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), null);
                hsb[1] = Math.min(1.0f, hsb[1] * 0.5f);
                hsb[2] = Math.min(1.0f, hsb[2] * 1.8f);
                return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
            } else {
                return foreground;
            }
        }

        public double brightness(Color c) {
            return (c.getRed() * 0.3 + c.getGreen() * 0.58 + c.getBlue() * 0.11) / 255.0;
        }


        public void clearLayout() {
            layout = null;
        }

        /**
         * @param l text-layout of the line
         * @param i a character index (0 means the line-top)
         * @return X position of the right-hit of the character i
         */
        public float getX(TextLayout l, int i) {
            if (i > 0) {
                TextHitInfo hit = l.getNextRightHit(i);
                if (hit != null) {
                    float[] pos = l.getCaretInfo(hit);
                    return pos[0]; //seems {x,baseLine0, x,-ascent, x,descent}
                }
            }
            return 0;
        }

        /**
         * @param l created text-layout of this line
         * @param start same origin as this.start
         * @param end same origin as this.start
         * @return a logical highlight shape for the range
         */
        public Shape getSelectionShape(TextLayout l, int start, int end) {
            if (end < start) {
                int tmp = end;
                end = start;
                start = tmp;
            }
            if ((start == -1 && end == -1) || start > end ||
                    (end != -1 && end < this.start) ||
                    (start != -1 && this.end < start)) {
                return null;
            } else {
                if (start == -1) {
                    start = 0;
                }
                if (end == -1) {
                    end = this.end;
                }
                return l.getLogicalHighlightShape(
                        Math.max(start, this.start) - this.start,
                        Math.min(end, this.end) - this.start);
            }
        }

        /**
         * add a new range
         * @param start same origin as this.start
         * @param end same origin as this.start
         */
        public void addFindRange(int start, int end) {
            start = Math.max(this.start, start);
            end = Math.min(this.end, end);
            if (start < end) {
                if (findRanges == null) {
                    findRanges = new ArrayList<>(3);
                }
                findRanges.add(new int[]{start, end});
            }
        }

        /**
         * @return never null
         */
        public List<int[]> getFindRanges() {
            if (findRanges == null) {
                return Collections.emptyList();
            }
            return findRanges;
        }

        public void clearFindRanges() {
            findRanges = null;
        }
    }

    /**
     * focusing information for a matched  pattern position
     */
    public static class LineInfoMatch {
        public TextCellRenderer<?> renderer;
        public Object value;
        /**
         * a line number index
         */
        public int line;
        /**
         * an index of ranges, returned by {@link LineInfo#getFindRanges()}
         */
        public int range;

        public LineInfoMatch(int line, int range, TextCellRenderer<?> r, Object v) {
            this.renderer = r;
            this.value = v;
            this.line = line;
            this.range = range;
        }

        public boolean sameKeys(TextCellRenderer<?> r, Object v) {
            return Arrays.equals(new Object[] {renderer, value}, new Object[] {r, v});
        }

        public LineInfoMatch next(List<LineInfo> lines) {
            if (line < lines.size()) {
                LineInfo info = lines.get(line);
                if (range + 1 < info.getFindRanges().size()) {
                    return new LineInfoMatch(line, range + 1, renderer, value);
                } else {
                    for (int nextLine = line + 1; nextLine < lines.size(); ++nextLine) {
                        info = lines.get(nextLine);
                        if (!info.getFindRanges().isEmpty()) {
                            return new LineInfoMatch(nextLine, 0, renderer, value);
                        }
                    }
                    return null;
                }
            } else {
                return null;
            }
        }

        public LineInfoMatch previous(List<LineInfo> lines) {
            if (line < lines.size()) {
                if (range - 1 >= 0) {
                    return new LineInfoMatch(line, range - 1, renderer, value);
                } else {
                    for (int prevLine = line - 1; prevLine >= 0; --prevLine) {
                        LineInfo info = lines.get(prevLine);
                        if (!info.getFindRanges().isEmpty()) {
                            return new LineInfoMatch(prevLine, info.getFindRanges().size() - 1, renderer, value);
                        }
                    }
                    return null;
                }
            } else {
                return null;
            }
        }


        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "renderer=" + renderer +
                    ", value=" + value +
                    ", line=" + line +
                    ", range=" + range +
                    '}';
        }
    }

}
