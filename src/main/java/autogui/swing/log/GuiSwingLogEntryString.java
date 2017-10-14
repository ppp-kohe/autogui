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

public class GuiSwingLogEntryString extends GuiLogEntryString implements GuiSwingLogEntry {
    protected int selectionFrom;
    protected int selectionTo;
    protected boolean selected;

    public GuiSwingLogEntryString(String data) {
        super(data);
    }

    public GuiSwingLogEntryString(Instant time, String data) {
        super(time, data);
    }

    @Override
    public LogEntryRenderer getRenderer(GuiSwingLogManager manager, ContainerType type) {
        return new GuiSwingLogStringRenderer(manager, type);
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

    @Override
    public void clearSelection() {
        selectionFrom = 0;
        selectionTo = 0;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean isSelected() {
        return selected;
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
        protected ContainerType containerType;

        protected TextPaneCellSupport support;

        protected Style defaultStyle;
        protected Style timeStyle;
        protected Style followingLinesStyle;

        public GuiSwingLogStringRenderer(GuiLogManager manager, ContainerType type) {
            this.containerType = type;
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
            if (selected && containerType.equals(ContainerType.List)) {
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
                    selected = sStr.isSelected();
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
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            str.setSelectionTo(viewToModel(point));
        }

        @Override
        public boolean updateFindPattern(String findKeyword) {
            return support.updateFindPattern(findKeyword);
        }

        @Override
        public int findText(GuiSwingLogEntry entry, String findKeyword) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            String text = formatString(str);
            return support.findText(findKeyword, text).size();
        }

        @Override
        public Object focusNextFound(GuiSwingLogEntry entry, Object prevIndex, boolean forward) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            TextPaneCellSupport.TextPaneCellMatch m = support.nextFindMatched(prevIndex, forward, entry);
            int[] range = support.getFindMatchedRange(m);
            if (range[1] > 0) {
                str.setSelectionFrom(range[0]);
                str.setSelectionTo(range[1]);
            }
            return m;
        }

        @Override
        public String getSelectedText(GuiSwingLogEntry entry, boolean entireText) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            int from = str.getSelectionFrom();
            int to = str.getSelectionTo();
            String text = formatString(str);
            if (entireText || from == to || !range(from, text) || !range(to, text)) {
                return text;
            } else {
                if (from > to) {
                    int tmp = to;
                    to = from;
                    from = tmp;
                }
                return text.substring(from, to);
            }
        }

        private boolean range(int i, String s) {
            return 0 <= i && i <= s.length();
        }
    }

}
