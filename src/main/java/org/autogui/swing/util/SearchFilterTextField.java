package org.autogui.swing.util;

import org.autogui.swing.icons.GuiSwingIcons;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.font.TextLayout;
import java.io.Serial;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * a text-field for imeediate filtering data in text-components
 */
public class SearchFilterTextField extends JComponent {
    @Serial private static final long serialVersionUID = 1L;
    protected JTextField textField;
    protected JButton iconButton;
    protected SearchTextField.SearchBackgroundPainterBordered painter;
    protected SearchRunner runner;
    protected String placeHolderText = "";

    /** call-back for searching*/
    public interface SearchRunner {
        /**
         * called when editing text
         * @param text the current input-text
         */
        void updateText(String text);

        /**
         * called when the enter-key is pressed
         * @param text the current input-text
         * @param shiftDown true if the SHIFT key is down
         */
        default void action(String text, boolean shiftDown) { }
    }

    public static class SearchRunnerEmpty implements SearchRunner {
        public SearchRunnerEmpty() {}
        @Override public void updateText(String text) {}
    }
    public static SearchRunnerEmpty RUNNER_EMPTY = new SearchRunnerEmpty();

    public SearchFilterTextField() {
        this(RUNNER_EMPTY);
    }

    @SuppressWarnings("this-escape")
    public SearchFilterTextField(SearchRunner runner) {
        this.runner = runner;
        init();
    }

    protected void init() {
        initLayout();
        initIconButton();
        initField();
    }

    protected void initLayout() {
        painter = new SearchTextField.SearchBackgroundPainterBordered(this);
        setBackground(UIManagerUtil.getInstance().getTextPaneBackground());
    }

    protected void initIconButton() {
        iconButton = createIconButton();
        add(iconButton, BorderLayout.WEST);
    }

    protected void initField() {
        textField = new JTextField(20);
        textField.setOpaque(true);
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateText(); }
            @Override public void removeUpdate(DocumentEvent e) { updateText(); }
            @Override public void changedUpdate(DocumentEvent e) { updateText(); }
        });
        textField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { enterText(e); }
        });
        add(textField, BorderLayout.CENTER);
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp instanceof JComponent jc) {
            painter.setChild(jc);
        }
        super.addImpl(comp, constraints, index);
    }

    @Override
    protected void paintComponent(Graphics g) {
        painter.paintComponent(g);
        if (textField.getDocument().getLength() == 0 && !placeHolderText.isEmpty() &&
            g instanceof Graphics2D g2) {
            var text = new TextLayout(placeHolderText, getTextField().getFont(), g2.getFontRenderContext());
            g2.setColor(Color.gray);
            var base = textField.getBounds();
            text.draw(g2, (base.x + UIManagerUtil.getInstance().getScaledSizeInt(3)), (base.y + text.getAscent()));
        }
    }

    public SearchFilterTextField setPlaceHolderText(String placeHolderText) {
        this.placeHolderText = placeHolderText;
        return this;
    }

    public String getPlaceHolderText() {
        return placeHolderText;
    }



    public static JButton createIconButton() {
        JButton icon = new JButton();
        UIManagerUtil ui = UIManagerUtil.getInstance();
        int size = ui.getScaledSizeInt(16);
        icon.setBorder(BorderFactory.createEmptyBorder());
        icon.setMargin(new Insets(0, 0, 0, 0));
        icon.setIcon(GuiSwingIcons.getInstance().getIcon("log-", "find", size, size));
        icon.setBorderPainted(false);
        icon.setContentAreaFilled(false);
        icon.setFocusable(false);
        return icon;
    }

    public JTextField getTextField() {
        return textField;
    }

    public JButton getIconButton() {
        return iconButton;
    }

    public SearchRunner getRunner() {
        return runner;
    }

    public void setRunner(SearchRunner runner) {
        this.runner = runner;
    }

    public void updateText() {
        if (runner != null) {
            var searchText = textField.getText();
            runner.updateText(searchText);
        }
    }

    public void enterText(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_ENTER) {
            doAction(event.isShiftDown());
        } else if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
            doActionReset();
        }
    }

    public void doAction(boolean reverse) {
        if (runner != null) {
            var searchText = textField.getText();
            runner.action(searchText, reverse);
        }
    }

    public void doActionReset() {
        var searchText = textField.getText();
        runner.action(searchText, false);
    }

    public static JTextField createTextFieldAsLabel(String l) {
        var label = new JTextField(l);
        {
            label.setFocusable(false);
            label.setOpaque(false);
            label.setEditable(false);
        }
        int s = UIManagerUtil.getInstance().getScaledSizeInt(5);
        label.setBorder(BorderFactory.createEmptyBorder(s, s, s, s));
        return label;
    }

    public static SearchFilterTextField createTextHighlightIterator(Supplier<Iterator<? extends JTextComponent>> textSource) {
        return createTextHighlightIterator(textSource, createDefaultHighlightPainter());
    }

    public static SearchFilterTextField createTextHighlightCollection(Supplier<? extends Iterable<? extends JTextComponent>> textSource) {
        return createTextHighlightCollection(textSource, createDefaultHighlightPainter());
    }

    public static SearchFilterTextField createTextHighlightCollection(Iterable<? extends JTextComponent> textSource) {
        return createTextHighlightCollection(textSource, createDefaultHighlightPainter());
    }

    public static SearchFilterTextField createTextHighlightIterator(Supplier<Iterator<? extends JTextComponent>> textSource, Highlighter.HighlightPainter painter) {
        return new SearchFilterTextField(new SearchRunnerTextHighlight(textSource, painter));
    }

    public static SearchFilterTextField createTextHighlightCollection(Supplier<? extends Iterable<? extends JTextComponent>> textSource, Highlighter.HighlightPainter painter) {
        return new SearchFilterTextField(new SearchRunnerTextHighlight(() -> textSource.get().iterator(), painter));
    }

    public static SearchFilterTextField createTextHighlightCollection(Iterable<? extends JTextComponent> textSource, Highlighter.HighlightPainter painter) {
        return new SearchFilterTextField(new SearchRunnerTextHighlight(textSource::iterator, painter));
    }

    public static Highlighter.HighlightPainter createDefaultHighlightPainter() {
        return new DefaultHighlighter.DefaultHighlightPainter(Color.orange);
    }

    /** a simple implementation for highlighting of multiple text-panes */
    public static class SearchRunnerTextHighlight implements SearchRunner {
        protected Supplier<Iterator<? extends JTextComponent>> textSource;
        protected List<SearchTextHighlightMatch> foundTexts = new ArrayList<>();
        protected Highlighter.HighlightPainter painter;
        protected SearchTextHighlightMatch lastMatch;
        protected int[] lastPosition;

        public SearchRunnerTextHighlight(Supplier<Iterator<? extends JTextComponent>> textSource, Highlighter.HighlightPainter painter) {
            this.textSource = textSource;
            this.painter = painter;
        }

        @Override
        public void updateText(String text) {
            var iter = textSource.get();
            Map<JTextComponent, SearchTextHighlightMatch> map = foundTexts.stream()
                    .collect(Collectors.toMap(SearchTextHighlightMatch::getTextPane, Function.identity(), (a,b)->b, IdentityHashMap::new));
            foundTexts.clear();
            if (iter != null) {
                while (iter.hasNext()) {
                    var pane = iter.next();
                    var m = map.remove(pane);
                    if (m == null) {
                        m = new SearchTextHighlightMatch(pane);
                    }
                    foundTexts.add(m);
                    search(text, m);
                }
            }
            map.values()
                    .forEach(SearchTextHighlightMatch::removeAllResults);
        }

        public void search(String text, SearchTextHighlightMatch match) {
            String str = match.getTextPane().getText();
            String t = text.trim();
            match.removeAllResults();
            if (!t.isEmpty()) {
                int i = 0;
                List<int[]> pos = new ArrayList<>();
                var pat = toPattern(t);
                var matcher = pat.matcher(str);
                while (matcher.find()) {
                    pos.add(new int[] {matcher.start(), matcher.end()});
                }
                match.setPositions(pos, painter);
            }
        }

        protected Pattern toPattern(String text) {
            return Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);
        }

        @Override
        public void action(String text, boolean shiftDown) {
            if (lastMatch == null || !foundTexts.contains(lastMatch)) {
                nextMatch(shiftDown);
            }
            if (lastMatch != null) { //has foundTexts & lastMatch is contained in foundTexts
                var oldMatch = lastMatch;
                do {
                    lastPosition = lastMatch.nextPosition(lastPosition, shiftDown);
                    if (lastPosition != null) {
                        break;
                    } else {
                        if (!nextMatch(shiftDown)) {
                            break;
                        }
                    }
                } while (lastMatch != oldMatch);
            }
            if (lastMatch != null && lastPosition != null) {
                show(lastMatch.textPane, lastPosition);
            }
        }

        protected boolean nextMatch(boolean reverse) {
            var old = lastMatch;
            if (foundTexts.isEmpty()) {
                lastMatch = null;
            } else if (lastMatch == null) {
                lastMatch = (reverse ? foundTexts.getLast() : foundTexts.getFirst());
            } else if (lastMatch != null) {
                int lastIdx = foundTexts.indexOf(lastMatch); //the pane may be removed
                int i = Math.max(lastIdx, 0) + (reverse ? -1 : 1);
                if (i < 0) {
                    lastMatch = foundTexts.getLast();
                } else if (i >= foundTexts.size()) {
                    lastMatch = foundTexts.getFirst();
                } else {
                    lastMatch = foundTexts.get(i);
                }
            }
            lastPosition = null;
            return lastMatch != null && old != lastMatch;
        }

        public void show(JTextComponent textPane, int[] position) {
            JComponent next = textPane;
            Rectangle bounds = next.getBounds();
            if (position != null && 0 <= position[0] && position[0] < textPane.getDocument().getLength()) {
                try {
                    var baseBounds = bounds;
                    bounds = textPane.modelToView2D(position[0]).getBounds();
                    bounds.translate(baseBounds.x, baseBounds.y);
                } catch (BadLocationException be) {
                    throw new RuntimeException(be);
                }
            }
            while (next != null) {
                if (next.getParent() instanceof JViewport viewport) {
                    viewport.scrollRectToVisible(bounds);
                    bounds = viewport.getBounds(); //reset
                }
                if (next.getParent() instanceof JComponent parent) {
                    next = parent;
                    var parentPos = parent.getLocation();
                    bounds.translate(parentPos.x, parentPos.y);
                } else {
                    break;
                }
            }
        }
    }

    public static class SearchTextHighlightMatch {
        protected JTextComponent textPane;
        protected List<int[]> positions;
        protected List<Object> hilights;

        public SearchTextHighlightMatch(JTextComponent textPane) {
            this.textPane = textPane;
            positions = List.of();
            hilights = new ArrayList<>();
        }

        public JTextComponent getTextPane() {
            return textPane;
        }

        public void removeAllResults() {
            hilights.forEach(h ->
                textPane.getHighlighter().removeHighlight(h));
            hilights.clear();
        }

        public void setPositions(List<int[]> pos, Highlighter.HighlightPainter painter) {
            this.positions = pos;
            pos.forEach(p -> addHighlight(p, painter));
        }

        public void addHighlight(int[] pos, Highlighter.HighlightPainter painter) {
            try {
                hilights.add(textPane.getHighlighter().addHighlight(pos[0], pos[1], painter));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public int[] nextPosition(int[] lastPosition, boolean reverse) {
            if (positions.isEmpty()) {
                return null;
            } else if (lastPosition == null) {
                return reverse ? positions.getLast() : positions.getFirst();
            } else {
                int pos = Collections.binarySearch(positions, lastPosition, (a,b) -> {
                    if (a[1]-1 < b[0]) {
                        return -1;
                    } else if (b[1]-1 < a[0]) {
                        return 1;
                    } else {
                        return 0;
                    }
                });
                int nextPoint =
                        (pos < 0 ? (-pos - 1) : pos) +
                        (reverse ? -1 : 1);
                if (0 <= nextPoint && nextPoint < positions.size()) {
                    return positions.get(nextPoint);
                } else {
                    return null;
                }
            }
        }
    }
}
