package autogui.swing.util;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * <pre>
 *     new LineNumberPane(textPane).install();
 * </pre>
 */
public class LineNumberPane extends JComponent implements DocumentListener {
    protected JTextComponent field;

    protected List<Integer> linePosition = new ArrayList<>();

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
        setBackground(new Color(240, 240, 240));
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        build();
    }

    public void install() {
        field.getDocument().addDocumentListener(this);
        field.addCaretListener(e -> repaint());
        installToScrollPane(scroll(field.getParent()));
    }

    public void installToScrollPane(JScrollPane scrollPane) {
        scrollPane.setRowHeaderView(this);
        scrollPane.getViewport().addChangeListener(e -> repaint());
    }

    public void build() {
        Document doc = field.getDocument();
        try {
            String text = doc.getText(0, doc.getLength());
            linePosition = linePositions(text, true, 0);
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
            String text = e.getDocument().getText(off, len);

            List<Integer> insertedLines = linePositions(text, false, off);

            if (!insertedLines.isEmpty()) {
                int idx = findPosition(insertedLines.get(0));

                for (int i = idx, l = linePosition.size(); i < l; ++i) {
                    linePosition.set(i, linePosition.get(i) + len);
                }
                linePosition.addAll(idx, insertedLines);
            }
            updatePreferredSize();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<Integer> linePositions(String text, boolean startLineHead, int off) {
        List<Integer> insertedLines = new ArrayList<>();
        boolean lineHead = startLineHead;
        for (int i = 0, l = text.length(); i < l; ++i) {
            if (lineHead) {
                insertedLines.add(i + off);
                lineHead = false;
            }
            char c = text.charAt(i);
            if (c == '\n') {
                lineHead = true;
            }
        }
        if (lineHead) {
            insertedLines.add(text.length() + off);
        }
        return insertedLines;
    }

    public int findPosition(int n) {
        int idx = Collections.binarySearch(this.linePosition, n);
        if (idx < 0) {
            return -idx - 1;
        } else {
            return idx;
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        int off = e.getOffset();
        int end = off + e.getLength();

        int idx = findPosition(off);

        if (idx < linePosition.size()) {
            for (ListIterator<Integer> iter = linePosition.listIterator(idx); iter.hasNext(); ) {
                int pos = iter.next();
                if (pos < end) {
                    iter.remove();
                } else {
                    break;
                }
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

            int startIndex = field.viewToModel(textRect.getLocation()); //TODO jdk9: viewToModel2D
            int endIndex = field.viewToModel(new Point(textRect.x + textRect.width, textRect.y + textRect.height));

            int selfOffsetY = selfRect.y - textRect.y;

            g.setFont(getFont());

            int lineIndex = Math.max(0, findPosition(startIndex) - 1);
            int lines = linePosition.size();
            int endLineIndex = Math.min(lines, findPosition(endIndex));
            for (int i = lineIndex; i < endLineIndex; ++i) {
                int lineStart = linePosition.get(i);
                int lineEnd = i + 1 < lines ? linePosition.get(i + 1) - 1 : Integer.MAX_VALUE;
                if (overlap(lineStart, lineEnd, selStart, selEnd)) {
                    g.setColor(Color.black);
                } else {
                    g.setColor(Color.gray);
                }
                Rectangle lineRect = field.modelToView(lineStart);
                int y = lineRect.y + selfOffsetY;
                g.drawString(Integer.toString(i + 1), 4, y + 12);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean overlap(int s1, int e1, int s2, int e2) {
        return (s1 <= s2 && s2 <= e1) || (s1 <= e2 && e2 <= e1) ||
                (s2 <= s1 && s1 <= e2) || (s2 <= e1 && e1 <= e2) ;
    }
}
