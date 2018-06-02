package autogui.swing.util;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextCellRenderer<ValueType> extends JComponent
    implements TableCellRenderer, ListCellRenderer<ValueType> {
    protected boolean selected;
    protected int selectionStart;
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

    public TextCellRenderer() {
        lines = new ArrayList<>();
        setBorder(BorderFactory.createEmptyBorder(7, 10, 3, 10));
        setOpaque(false);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ValueType> list, ValueType value, int index, boolean isSelected, boolean cellHasFocus) {
        return getTableCellRendererComponent(null, value, isSelected, cellHasFocus, index, 0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        boolean forMouseEvents = row <= -1;
        if (!forMouseEvents) {
            this.selected = isSelected;
            this.selectionStart = -1;
            this.selectionEnd = -1;
        }

        setValue((ValueType) value, forMouseEvents);
        return this;
    }

    public boolean setValue(ValueType value, boolean forMouseEvents) {
        if (this.value != null && this.value.equals(value) && this.forMouseEvents == forMouseEvents) {
            return false;
        }
        this.value = value;
        this.text = format(value);
        this.forMouseEvents = forMouseEvents;

        int maxWidth = buildLines(text, lines);
        float[] size = buildSize(maxWidth);
        setPreferredSize(new Dimension((int) size[0], (int) size[1]));

        if (!forMouseEvents) {
            setSelectionFromValue(value);
        }
        return true;
    }

    public String format(ValueType value) {
        if (value == null) {
            return "";
        } else {
            return Objects.toString(value);
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
            LineInfo info = createLine(prev, lineIndex, i, line);
            lines.add(info);
            prev = info;
            i += line.length() + 1;
            ++lineIndex;
            maxWidth = Math.max(maxWidth, info.getWidth());
        }
        return maxWidth;
    }

    public LineInfo createLine(LineInfo prevLine, int lineIndex, int start, String line) {
        AttributedString a = new AttributedString(line);
        return new LineInfo(a, start, line.length() + start);
    }

    /**
     * @param maxWidth max size of line width
     * @return {width, height}
     */
    public float[] buildSize(int maxWidth) {
        int[] bs = getBorderSize();
        float[] baseSize = getBaseSize();
        return new float[] {
                Math.max(1, maxWidth) * baseSize[0] + bs[0],
                Math.max(1, lines.size()) * baseSize[1] + bs[1] };
    }

    //////////////////

    public void setSelectionFromValue(ValueType value) {
        setFindHighlights();
    }

    /**
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
     * @param value the value to be set
     * @param prevIndex the previous focus object or null
     * @param forward true for forward or false for backward
     * @return a focus object {@link LineInfoMatch} or null
     */
    public Object getFocusNextFound(ValueType value, Object prevIndex, boolean forward) {
        setValue(value, false);
        if (!(prevIndex instanceof LineInfoMatch) || //null
                !((LineInfoMatch) prevIndex).entry.equals(value)) {
            if (forward) {
                int i = 0;
                for (LineInfo line : lines) {
                    if (!line.getFindRanges().isEmpty()) {
                        return new LineInfoMatch(value, i, 0);
                    }
                    ++i;
                }
                return null;
            } else {
                for (int i = lines.size() - 1; i >= 0; --i) {
                    LineInfo line = lines.get(i);
                    if (!line.getFindRanges().isEmpty()) {
                        return new LineInfoMatch(value, i, line.getFindRanges().size() - 1);
                    }
                }
                return null;
            }
        } else {
            LineInfoMatch m = (LineInfoMatch) prevIndex;
            return forward ? m.next(lines) : m.previous(lines);
        }
    }

    //////////////////

    public float[] getBaseSize() {
        if (testImage == null) {
            testImage = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = testImage.createGraphics();

            AttributedString a = new AttributedString("M");
            setDefaultAttributes(a, 1);

            TextLayout l = new TextLayout(a.getIterator(), g.getFontRenderContext());
            baseWidth = l.getAdvance();
            baseHeight = l.getAscent() + l.getDescent() + l.getLeading();
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

    @Override
    protected void paintComponent(Graphics g) {
        getBorder().paintBorder(this, g, 0, 0, getWidth(), getHeight());
        Insets insets = getBorder().getBorderInsets(this);

        Graphics2D g2 = (Graphics2D) g;
        paintSetUpGraphics(g2);
        FontRenderContext frc = g2.getFontRenderContext();
        float x = insets.left;
        float y = insets.top;
        Color selectionColor = UIManager.getColor("TextPane.selectionBackground");
        Color findColor = Color.orange;

        int lineIndex = 0;
        LineInfo prev = null;
        float lineX = 0;
        g2.translate(x, y);
        for (LineInfo line : lines) {
            TextLayout l = line.getLayout(frc);

            float ascent = l.getAscent();
            y += ascent;
            g2.translate(0, ascent);

            lineX = paintStartX(lineIndex, prev, lineX, line, l, frc);

            paintLineSelection(g2, line, l, selectionColor, lineX);
            paintLineFinds(g2, line, l, findColor, lineX);

            g2.setPaint(Color.black);

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
        g2.setPaint(Color.white);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setPaint(Color.black);
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

    public void paintLineFinds(Graphics2D g2, LineInfo line, TextLayout l, Color findColor, float lineX) {
        g2.setPaint(findColor);
        g2.translate(lineX, 0);
        for (int[] range : line.getFindRanges()) {
            Shape find = line.getSelectionShape(l, range[0], range[1]);
            if (find != null) {
                g2.fill(find);
            }
        }
        g2.translate(-lineX, 0);
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
        RoundRectangle2D.Float r = new RoundRectangle2D.Float(2, 2, size.width - 5, size.height - 5, 3, 3);
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

    public static class LineInfo {
        public AttributedString attributedString;
        public int start;
        public int end;

        public int indent;

        protected TextLayout layout;

        protected List<int[]> findRanges;

        public LineInfo(AttributedString attributedString, int start, int end) {
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

        public void setIndent(int indent) {
            this.indent = indent;
        }

        public int getIndent() {
            return indent;
        }

        public TextLayout getLayout(FontRenderContext frc) {
            if (layout == null) {
                layout = new TextLayout(attributedString.getIterator(), frc);
            }
            return layout;
        }

        public void clearLayout() {
            layout = null;
        }

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

    public static class LineInfoMatch {
        public Object entry;
        public int line;
        public int range;

        public LineInfoMatch(Object entry, int line, int range) {
            this.entry = entry;
            this.line = line;
            this.range = range;
        }

        public LineInfoMatch next(List<LineInfo> lines) {
            if (line < lines.size()) {
                LineInfo info = lines.get(line);
                if (range + 1 < info.getFindRanges().size()) {
                    return new LineInfoMatch(entry, line, range + 1);
                } else {
                    for (int nextLine = line + 1; nextLine < lines.size(); ++nextLine) {
                        info = lines.get(nextLine);
                        if (!info.getFindRanges().isEmpty()) {
                            return new LineInfoMatch(entry, nextLine, 0);
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
                    return new LineInfoMatch(entry, line, range - 1);
                } else {
                    for (int prevLine = line - 1; prevLine >= 0; --prevLine) {
                        LineInfo info = lines.get(prevLine);
                        if (!info.getFindRanges().isEmpty()) {
                            return new LineInfoMatch(entry, prevLine, info.getFindRanges().size() - 1);
                        }
                    }
                    return null;
                }
            } else {
                return null;
            }
        }
    }

}
