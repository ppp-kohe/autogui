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

/**
 * support class for displaying JTextPane as a list cell
 */
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

    public JTextPane getPane() {
        return pane;
    }

    /** key for selection highlight */
    public Object getSelectionHighlight() {
        return selectionHighlight;
    }

    /** existing finding-highlight keys, might be unused */
    public List<Object> getFindHighlightKeys() {
        return findHighlightKeys;
    }

    /** automatically called from the constructor */
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

    /** default highlight painter is automatically set in the constructor */
    public Highlighter.HighlightPainter getFindPainter() {
        return findPainter;
    }

    public void setSelectionHighlightClear() {
        setSelectionHighlight(false, 0, 0);
    }

    public void setSelectionHighlight(boolean isSelected, int from, int to) {
        setHighlight(selectionHighlight, isSelected, from, to);
    }

    /** change the position of the highlight. if isSelected == false, the positions become 0,0.
     *     from and to can be both from&lt;to and to&lt;=from  */
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

    /** set the highlight painter to locations matched by the findPattern.
     *   it automatically reuses or adds findHighlightKeys */
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

    /** recursively checks text components within the component
     *    whether those text components are clicked or not at clickPoint.
     *    <p>
     *    If clicked on a text component,
     *     it adds or updates a value for the  text component.
     *     the value of selection map becomes {startIndex, endIndex}.
     *    <p>
     *    If start == true,
     *       this can be used for mousePressed events,
     *       it sets the text index at clickPoint to value[0],
     *     otherwise,
     *       can be used for mouseDragged or mouseReleased events,
     *       it sets the index to value[1].
     *     <p>
     *   The selectionMap can be used for {@link #setSelectionHighlight(Map)}
     * */
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

    /** update the selection highlight by the range obtained from selectionMap for the pane */
    public void setSelectionHighlight(Map<JTextComponent, int[]> selectionMap) {
        int[] sel = selectionMap.get(pane);
        if (sel == null) {
            setSelectionHighlightClear();
        } else {
            setSelectionHighlight(true, sel[0], sel[1]);
        }
    }

    ////////////

    /** update renderer's finding-highlight-pattern.
     *  this makes the renderer highlight texts of subsequent rendering items */
    public boolean updateFindPattern(String findKeyword) {
        if (findKeyword == null || findKeyword.isEmpty()) {
            findText = null;
            findPattern = null;
            findMatchedPositions.clear();
            return true;
        } else if (findText == null || !findText.endsWith(findKeyword)) {
            findText = findKeyword;
            findPattern = Pattern.compile(Pattern.quote(findKeyword));
            findMatchedPositions.clear();
            return true;
        } else {
            return false;
        }
    }

    /** update renderer's finding-highlight-pattern,
     *  match the pattern with text,
     *  and return matched positions */
    public List<Integer> findText(String findKeyword, String text) {
        updateFindPattern(findKeyword);
        if (findPattern != null) {
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

    /**
     * update find-highlights with current findPattern and findPainter
     * by calling {@link #setFindHighlights(Pattern, Highlighter.HighlightPainter)}.
     * The method can be used within getListCellRenderer, in order to update highlights,
     *   after the text of the pane is set.
     * */
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
            int i = ((TextPaneCellMatch) prevIndex).indexOfFindMatchedPositions + (forward ? 1 : -1);
            if (0 <= i && i < findMatchedPositions.size()) {
                return new TextPaneCellMatch(i, keys);
            } else {
                return null;
            }
        }
    }

    /**
     * clear the selectionMap and set matched range as the value for the pane */
    public int[] updateSelectionMap(Map<JTextComponent, int[]> selectionMap, TextPaneCellMatch m) {
        int[] r = getFindMatchedRange(m);
        selectionMap.clear();
        selectionMap.put(pane, r);
        return r;
    }

    public int[] getFindMatchedRange(TextPaneCellMatch m) {
        if (m == null || !(0 <= m.indexOfFindMatchedPositions && m.indexOfFindMatchedPositions < findMatchedPositions.size())) {
            return new int[] {0, 0};
        } else {
            int n = findMatchedPositions.get(m.indexOfFindMatchedPositions);
            return new int[] {n, n + findText.length()};
        }
    }

    public static class TextPaneCellMatch {
        public Object[] keys;
        public int indexOfFindMatchedPositions;

        public TextPaneCellMatch(int indexOfFindMatchedPositions, Object... keys) {
            this.indexOfFindMatchedPositions = indexOfFindMatchedPositions;
            this.keys = keys;
        }

        public boolean sameKeys(Object... keys) {
            return Arrays.equals(this.keys, keys);
        }

        public Object key(int i) {
            return 0 <= i && i < keys.length ? keys[i] : null;
        }

        @Override
        public String toString() {
            return Arrays.toString(keys) + ":" + indexOfFindMatchedPositions;
        }
    }

    public static class TextPaneCellMatchList extends TextPaneCellMatch {
        protected int supportIndex;

        public TextPaneCellMatchList(int index, int supportIndex, Object... keys) {
            super(index, keys);
            this.supportIndex = supportIndex;
        }

        public TextPaneCellMatchList(TextPaneCellMatch m, int supportIndex) {
            this(m.indexOfFindMatchedPositions, supportIndex, m.keys);
        }

        public int getSupportIndex() {
            return supportIndex;
        }
    }

    public static class TextPaneCellSupportList {
        protected List<TextPaneCellSupport> supports = new ArrayList<>();

        public TextPaneCellSupportList(JTextPane... panes) {
            Arrays.stream(panes)
                    .map(TextPaneCellSupport::new)
                    .forEach(supports::add);
        }

        public List<TextPaneCellSupport> getSupports() {
            return supports;
        }

        public TextPaneCellMatchList nextFindMatchedList(Object prevIndex, boolean forward, Object... fixedKeys) {
            //keys: ...fixedKeys, index
            if (prevIndex != null && prevIndex instanceof TextPaneCellMatchList &&
                    ((TextPaneCellMatch) prevIndex).sameKeys(fixedKeys)) {
                TextPaneCellMatchList pm = (TextPaneCellMatchList) prevIndex;
                int supportIndex = pm.getSupportIndex();
                TextPaneCellMatch m = supports.get(supportIndex)
                        .nextFindMatched(pm, forward, pm.keys);
                if (m != null) {
                    return new TextPaneCellMatchList(m, supportIndex);
                } else {
                    supportIndex += (forward ? 1 : -1);
                    return nextFindMatchedListSupport(supportIndex, forward, fixedKeys);
                }
            } else {
                return nextFindMatchedListSupport(forward ? 0 : supports.size() - 1, forward, fixedKeys);
            }
        }

        private TextPaneCellMatchList nextFindMatchedListSupport(int supportIndex, boolean forward, Object[] fixedKeys) {
            while (0 <= supportIndex && supportIndex < supports.size()) {
                TextPaneCellMatch m = supports.get(supportIndex)
                        .nextFindMatched(null, forward, fixedKeys);
                if (m != null) {
                    return new TextPaneCellMatchList(m, supportIndex);
                }
                supportIndex += (forward ? 1 : -1);
            }
            return null;
        }



        public void setSelectionHighlights(Map<JTextComponent, int[]> selectionMap) {
            supports.forEach(s -> s.setSelectionHighlight(selectionMap));
        }

        public void setSelectionHighlightsClear() {
            supports.forEach(TextPaneCellSupport::setSelectionHighlightClear);
        }

        public boolean updateFindPattern(String keyword) {
            boolean r = false;
            for (TextPaneCellSupport s : supports) {
                if (s.updateFindPattern(keyword)) {
                    r = true;
                }
            }
            return r;
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

        public void setFindHighlights() {
            supports.forEach(TextPaneCellSupport::setFindHighlights);
        }

    }
}
