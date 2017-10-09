package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryString;
import autogui.base.log.GuiLogManager;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.Instant;

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

    public static class GuiSwingLogStringRenderer extends JEditorPane
            implements TableCellRenderer, ListCellRenderer<GuiLogEntry>, LogEntryRenderer {
        protected GuiLogManager manager;
        protected boolean selected;
        protected Object highlightKey;

        public GuiSwingLogStringRenderer(GuiLogManager manager) {
            setBorder(BorderFactory.createEmptyBorder(7, 10, 3, 10));
            setFont(GuiSwingLogManager.getFont());
            this.manager = manager;
            //setEditable(false);

            setOpaque(false);

            try {
                Color c = UIManager.getColor("TextPane.selectionBackground");
                highlightKey = getHighlighter().addHighlight(0, 0,
                        new DefaultHighlighter.DefaultHighlightPainter(c));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
            if (!isSelected) {
                try {
                    getHighlighter().changeHighlight(highlightKey, 0, 0);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (value instanceof GuiLogEntryString) {
                GuiLogEntryString str = (GuiLogEntryString) value;
                setText(String.format("%s %s",
                        manager.formatTime(str.getTime()),
                        str.getData()));

                if (value instanceof GuiSwingLogEntryString) {
                    GuiSwingLogEntryString sStr = (GuiSwingLogEntryString) value;

                    int from = sStr.getSelectionFrom();
                    int to = sStr.getSelectionTo();
                    if (from > to){
                        int tmp = from;
                        from = to;
                        to = tmp;
                    }
                    if (from >= 0 && to >= 0 && isSelected) {
                        try {
                            getHighlighter().changeHighlight(highlightKey, from, to);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            return this;
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
    }
}
