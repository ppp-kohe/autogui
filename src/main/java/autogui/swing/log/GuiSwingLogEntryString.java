package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryString;
import autogui.base.log.GuiLogManager;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.Instant;
import java.util.ArrayList;
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
        g2.setColor(UIManager.getColor("Table.selectionBackground"));
        g2.draw(r);
    }

    public static class GuiSwingLogStringRenderer extends JTextPane
            implements TableCellRenderer, ListCellRenderer<GuiLogEntry>, LogEntryRenderer {
        protected GuiLogManager manager;
        protected boolean selected;
        protected Object highlightKey;

        protected Pattern findPattern;
        protected DefaultHighlighter.DefaultHighlightPainter findPainter;
        protected List<Object> findHighlightKeys = new ArrayList<>();

        public GuiSwingLogStringRenderer(GuiLogManager manager) {
            setBorder(BorderFactory.createEmptyBorder(7, 10, 3, 10));
            setFont(GuiSwingLogManager.getFont());
            this.manager = manager;
            //setEditable(false);

            setOpaque(false);

            highlightKey = addHighlight(this);
            findPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.orange);


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
            selected = isSelected;
            setHighlight(this, highlightKey, true, 0, 0);
            if (value instanceof GuiLogEntryString) {
                GuiLogEntryString str = (GuiLogEntryString) value;
                String text = formatString(str);
                try {
                    //TODO [time (gray)] message(black) \n (left Indent) message...
                    getDocument().remove(0, getDocument().getLength());
                    SimpleAttributeSet sas = new SimpleAttributeSet();
                    StyleConstants.setBold(sas, true);

                    getDocument().insertString(0, text.substring(0, 10), sas);

                    sas = new SimpleAttributeSet();
                    StyleConstants.setLeftIndent(sas, 100);
                    getDocument().insertString(10, text.substring(10), sas);

                } catch (Exception ex) {
                    //
                }

                if (value instanceof GuiSwingLogEntryString) {
                    GuiSwingLogEntryString sStr = (GuiSwingLogEntryString) value;
                    updateSelection(sStr);
                    updateFind(sStr);
                }
            }
            return this;
        }

        public void updateSelection(GuiSwingLogEntryString sStr) {
            int from = sStr.getSelectionFrom();
            int to = sStr.getSelectionTo();
            if (from > to){
                int tmp = from;
                from = to;
                to = tmp;
            }
            setHighlight(this, highlightKey, selected, from, to);
        }

        public void updateFind(GuiSwingLogEntryString sStr) {
            int hk = 0;
            if (findPattern != null) {
                Matcher m = findPattern.matcher(getText());
                while (m.find()) {
                    int s = m.start();
                    int e = m.end();
                    if (hk < findHighlightKeys.size()) {
                        try {
                            getHighlighter().changeHighlight(findHighlightKeys.get(hk), s, e);
                        } catch (Exception ex) {
                            //
                        }
                    } else {
                        try {
                            findHighlightKeys.add(getHighlighter().addHighlight(s, e, findPainter));
                        } catch (Exception ex) {
                            //
                        }
                    }
                    ++hk;
                }
            }
            for (int size = findHighlightKeys.size(); hk < size; ++hk) {
                try {
                    getHighlighter().changeHighlight(findHighlightKeys.get(hk), 0, 0);
                } catch (Exception ex) {
                    //
                }
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
        public int findText(GuiSwingLogEntry entry, String text) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            if (text.isEmpty()) {
                findPattern = null;
                return 0;
            } else {
                findPattern = Pattern.compile(Pattern.quote(text));
                Matcher m = findPattern.matcher(formatString(str));
                int count = 0;
                while (m.find()) {
                    ++count;
                }
                return count;
            }
        }

        @Override
        public boolean focusNextFound() {
            return false; //TODO
        }

        @Override
        public boolean focusPreviousFound() {
            return false; //TODO
        }
    }

    public static Object addHighlight(JTextComponent comp) {
        try {
            Color c = UIManager.getColor("TextPane.selectionBackground");
            return comp.getHighlighter().addHighlight(0, 0,
                    new DefaultHighlighter.DefaultHighlightPainter(c));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void setHighlight(JTextComponent comp, Object highlightKey, boolean isSelected, int from, int to) {
        if (highlightKey == null) {
            return;
        }
        try {
            if (from >= 0 && to >= 0 && isSelected) {
                int len = comp.getDocument().getLength();
                if (from > to) {
                    int tmp = to;
                    to = from;
                    from = tmp;
                }
                comp.getHighlighter().changeHighlight(highlightKey, Math.min(from, len), Math.min(to, len));
            } else {
                comp.getHighlighter().changeHighlight(highlightKey, 0, 0);
            }
        } catch (Exception ex) {
            //nothing
        }
    }
}
