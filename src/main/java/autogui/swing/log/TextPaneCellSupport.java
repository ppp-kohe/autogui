package autogui.swing.log;

import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextPaneCellSupport {
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

    public List<Integer> findText(String findKeyword, String text) {
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

    public static class TextPaneCellMatchList extends TextPaneCellMatch {
        protected int supportKeyIndex;

        public TextPaneCellMatchList(int index, int supportKeyIndex, Object... keys) {
            super(index, keys);
            this.supportKeyIndex = supportKeyIndex;
        }

        public TextPaneCellMatchList(TextPaneCellMatch m, int supportKeyIndex) {
            this(m.index, supportKeyIndex, m.keys);
        }

        public int getSupportIndex() {
            return (Integer) key(supportKeyIndex);
        }
    }

    public static class TextPaneCellSupportList {
        protected List<TextPaneCellSupport> supports = new ArrayList<>();

        public TextPaneCellSupportList(JTextPane... panes) {
            Arrays.stream(panes)
                    .map(TextPaneCellSupport::new)
                    .forEach(supports::add);
        }

        public TextPaneCellMatchList nextFindMatchedList(Object prevIndex, boolean forward, Object... fixedKeys) {
            //keys: ...fixedKeys, index
            if (prevIndex != null && prevIndex instanceof TextPaneCellMatch &&
                    ((TextPaneCellMatch) prevIndex).sameKeysHead(fixedKeys)) {
                TextPaneCellMatch pm = (TextPaneCellMatch) prevIndex;
                int supportIndex = (Integer) pm.key(fixedKeys.length);
                TextPaneCellMatch m = supports.get(supportIndex)
                        .nextFindMatched(pm, forward, pm.keys);
                if (m != null) {
                    return new TextPaneCellMatchList(m, fixedKeys.length);
                } else {
                    supportIndex += (forward ? 1 : -1);
                    while (0 <= supportIndex && supportIndex < supports.size()) {
                        m = supports.get(supportIndex)
                                .nextFindMatched(null, forward, fixedKeysAndSupportIndex(fixedKeys, supportIndex));
                        if (m != null) {
                            return new TextPaneCellMatchList(m, fixedKeys.length);
                        }
                        supportIndex += (forward ? 1 : -1);
                    }
                    return null;
                }
            } else {
                int supportIndex = forward ? 0 : supports.size() - 1;
                TextPaneCellMatch m = supports.get(supportIndex)
                        .nextFindMatched(null, forward, fixedKeysAndSupportIndex(fixedKeys, supportIndex));
                if (m != null) {
                    return new TextPaneCellMatchList(m, fixedKeys.length);
                } else {
                    return null;
                }
            }
        }

        private static Object[] fixedKeysAndSupportIndex(Object[] fixedKeys, int supportIndex) {
            Object[] nextKeys = Arrays.copyOf(fixedKeys, fixedKeys.length + 1);
            nextKeys[fixedKeys.length] = supportIndex;
            return nextKeys;
        }

        public void setSelectionHighlights(Map<JTextComponent, int[]> selectionMap) {
            supports.forEach(s -> s.setSelectionHighlight(selectionMap));
        }

        public void setSelectionHighlightsClear() {
            supports.forEach(TextPaneCellSupport::setSelectionHighlightClear);
        }

        public List<List<Integer>> findTexts(String keyword, String... text) {
            List<List<Integer>> is = new ArrayList<>();
            for (int i = 0, l = Math.min(text.length, supports.size()) ; i < l; ++i) {
                is.add(supports.get(i)
                        .findText(keyword, text[i]));
            }
            return is;
        }

        public TextPaneCellSupport getSupport(int i) {
            return supports.get(i);
        }
    }
}
