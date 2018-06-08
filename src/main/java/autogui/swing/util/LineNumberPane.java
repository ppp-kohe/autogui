package autogui.swing.util;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * a line-numbers display.
 * <pre>
 *     new LineNumberPane(textPane).install();
 * </pre>
 */
public class LineNumberPane extends JComponent implements DocumentListener {
    protected JTextComponent field;

    /** line-starting positions excluding 0 */
    protected List<Integer> linePosition = new ArrayList<>();

    protected static boolean debug = false;

    protected Color noOverlappingColor;

    public static JScrollPane scroll(Container c) {
        if (c == null) {
            return null;
        } else if (c instanceof JViewport) {
            return scroll(c.getParent());
        } else if (c instanceof JScrollPane) {
            return (JScrollPane) c;
        } else {
            return null;
        }
    }

    public LineNumberPane(JTextComponent field) {
        this.field = field;
        setOpaque(true);
        UIManagerUtil ui = UIManagerUtil.getInstance();
        setBackground(ui.getLabelBackground());
        setForeground(ui.getLabelForeground());
        Color fc = getForeground();
        Color bc = getBackground();

        noOverlappingColor = new Color(getCenter(fc.getRed(), bc.getRed()),
                    getCenter(fc.getBlue(), bc.getGreen()),
                    getCenter(fc.getBlue(), bc.getBlue()),
                    fc.getAlpha());

        Font font = UIManagerUtil.getInstance().getEditorPaneFont();
        font = font.deriveFont(font.getSize2D() * 0.84f);
        setFont(font);
        build();
    }

    private int getCenter(int l, int r) {
        if (l > r) {
            return r + (l - r) / 2;
        } else {
            return l + (r - l) / 2;
        }
    }

    public void install() {
        field.getDocument().addDocumentListener(this);
        field.addCaretListener(e -> repaint());
        installToScrollPane(scroll(field.getParent()));
    }

    public void installToScrollPane(JScrollPane scrollPane) {
        if (scrollPane != null) {
            scrollPane.setRowHeaderView(this);
            scrollPane.getViewport().addChangeListener(e -> repaint());
        }
    }

    public void build() {
        Document doc = field.getDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            linePosition = linePositions(text, 0);
            updatePreferredSize();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        try {
            int off = e.getOffset();
            int len = e.getLength();
            String insertedText = e.getDocument().getText(off, len);

            List<Integer> insertedLines = linePositions(insertedText, off);
            int idx = findPosition(off);

            boolean appendToEnd = (e.getDocument().getLength() == off + len);

            for (int i = idx, l = linePosition.size(); i < l; ++i) {
                int exPos = linePosition.get(i);
                if (exPos == off || (appendToEnd && l - 1 == i)) { //excludes [0] and [... , end]
                    continue;
                }
                linePosition.set(i, exPos + len);
            }
            if (debug) {
                debug("INS[" + idx + "]", e.getDocument().getText(0, e.getDocument().getLength()), 0, linePosition);
            }

            if (!insertedLines.isEmpty()) {
                if (appendToEnd) { //[..., (idx) end] -> [..., (idx) end, newLine1, newLine2,...]
                    linePosition.addAll(insertedLines);
                } else {
                    linePosition.addAll(idx, insertedLines);
                }

                if (debug) {
                    debug("INS", e.getDocument().getText(0, e.getDocument().getLength()), 0, linePosition);
                }
            }
            updatePreferredSize();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param text sub-string of the document
     * @param off an offset in the document (not the text)
     * @return list of line-head positions.
     */
    public List<Integer> linePositions(String text, int off) {
        List<Integer> insertedLines = new ArrayList<>();

        for (int i = 0, len = text.length(); i < len; ++i) {
            char c = text.charAt(i);
            if (c == '\n') {
                insertedLines.add(i + off + 1);
            }
        }
        if (debug) {
            debug("POS", text, off, insertedLines);
        }

        return insertedLines;
    }

    private void debug(String head, String text, int off, List<Integer> lineHeads) {
        StringBuilder buf = new StringBuilder();
        buf.append(head + ":<");
        int i = off;
        for (char c : text.toCharArray()) {
            if (lineHeads.contains(i)) {
                buf.append("[");
            }
            if (c == '\n') {
                buf.append("\\n");
            } else {
                buf.append(c);
            }
            if (lineHeads.contains(i)) {
                buf.append("]");
            }
            ++i;
        }
        if (lineHeads.contains(i)) {
            buf.append("[_]");
        }
        buf.append(">");
        buf.append("\n   heads: ").append(lineHeads);
        System.err.println(buf);
    }

    public int findPosition(int n) {
        int idx = Collections.binarySearch(this.linePosition, n);
        if (idx < 0) {
            return -idx - 1;
        } else {
            return idx + 1;
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        int off = e.getOffset();
        int len = e.getLength();
        int end = off + len;

        int idx = findPosition(off);

        if (debug) {
            try {
                debug("RMV[" + idx + "] (" + off + "+" + len + ":" + end + ")", e.getDocument().getText(0, e.getDocument().getLength()), 0, linePosition);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (idx < linePosition.size()) {
            for (ListIterator<Integer> iter = linePosition.listIterator(idx); iter.hasNext(); ) {
                int pos = iter.next();
                if (pos <= end) {
                    iter.remove();
                } else {
                    iter.set(pos - len);
                }
            }
        }

        if (debug) {
            try {
                debug("RMV", e.getDocument().getText(0, e.getDocument().getLength()), 0, linePosition);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        updatePreferredSize();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        //attribute change
    }

    public void updatePreferredSize() {
        int size = linePosition.size();
        int len = Integer.toString(size).length();
        Font font = getFont();
        int width;
        if (font != null) {
            width = (int) (font.getSize2D() * len + 8);
        } else {
            width = 10 * len + 8;
        }
        setPreferredSize(new Dimension(width, field.getPreferredSize().height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g);
        try {
            int selStart = field.getSelectionStart();
            int selEnd = field.getSelectionEnd();

            Rectangle textRect = field.getVisibleRect();
            Rectangle selfRect = getVisibleRect();

            g.setColor(getBackground());
            g.fillRect(selfRect.x, selfRect.y, selfRect.width, selfRect.height);

            int startIndex = PopupExtensionText.textComponentViewToModel(field, textRect.getLocation());
            int endIndex = PopupExtensionText.textComponentViewToModel(field,
                    new Point(textRect.x + textRect.width, textRect.y + textRect.height));

            int selfOffsetY = selfRect.y - textRect.y;

            g.setFont(getFont());

            int lineIndex = Math.max(0, findPosition(startIndex) - 1);
            int lines = linePosition.size();
            int endLineIndex = Math.min(lines, findPosition(endIndex));
            if (lineIndex == 0) {
                paintLine(g2, 0, 0,
                        overlap(0, lines == 0 ? Integer.MAX_VALUE : linePosition.get(0) - 1, selStart, selEnd),
                        selfOffsetY);
            }
            for (int i = lineIndex; i < endLineIndex; ++i) {
                paintLine(g2, i, lines, selStart, selEnd, selfOffsetY);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void paintLine(Graphics2D g, int i, int lines, int selStart, int selEnd, int selfOffsetY) throws Exception {
        int lineStart = linePosition.get(i);
        int lineEnd = i + 1 < lines ? linePosition.get(i + 1) - 1 : Integer.MAX_VALUE;
        boolean overlap = overlap(lineStart, lineEnd, selStart, selEnd);
        paintLine(g, i + 1, lineStart, overlap, selfOffsetY);
    }

    public void paintLine(Graphics2D g, int i, int lineStart, boolean overlap, int selfOffsetY) throws Exception {
        if (overlap) {
            g.setColor(getForeground());
        } else {
            g.setColor(noOverlappingColor);
        }
        Rectangle lineRect = PopupExtensionText.textComponentModelToView(field, lineStart);
        int y = lineRect.y + selfOffsetY;
        UIManagerUtil ui = UIManagerUtil.getInstance();
        g.drawString(Integer.toString(i + 1), ui.getScaledSizeFloat(4), y + ui.getScaledSizeInt(12));
    }

    private boolean overlap(int s1, int e1, int s2, int e2) {
        return (s1 <= s2 && s2 <= e1) || (s1 <= e2 && e2 <= e1) ||
                (s2 <= s1 && s1 <= e2) || (s2 <= e1 && e1 <= e2) ;
    }
}
