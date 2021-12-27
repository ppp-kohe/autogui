package org.autogui.swing.log;

import org.autogui.base.log.GuiLogEntry;
import org.autogui.base.log.GuiLogEntryString;
import org.autogui.base.log.GuiLogManager;
import org.autogui.swing.util.TextCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * a log-entry of a string message with supporting GUI rendering
 */
public class GuiSwingLogEntryString extends GuiLogEntryString implements GuiSwingLogEntry {
    protected int selectionFrom;
    protected int selectionTo;
    protected boolean selected;

    protected SizeCache rendererToSizeCache;

    public float[] sizeCache(Object r, Supplier<float[]> src) {
        if (rendererToSizeCache == null) {
            rendererToSizeCache = new SizeCache(3);
        }
        return rendererToSizeCache.computeIfAbsent(r, _r -> src.get());
    }

    public GuiSwingLogEntryString(String data) {
        super(data);
    }

    /**
     * @param data the string data
     * @param fromStdout whether stdout redirection
     * @since 1.1
     */
    public GuiSwingLogEntryString(String data, boolean fromStdout) {
        super(data, fromStdout);
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

    public static Map<AttributedCharacterIterator.Attribute, Object> getTimeStyle() {
        Map<AttributedCharacterIterator.Attribute, Object> m = new HashMap<>();
        m.put(TextAttribute.FOREGROUND, new Color(40, 134,10));
        m.put(TextAttribute.FONT, GuiSwingLogManager.getFont());
        return m;
    }

    public static Map<AttributedCharacterIterator.Attribute, Object> getBodyStyle() {
        Map<AttributedCharacterIterator.Attribute, Object> m = new HashMap<>();
        m.put(TextAttribute.FOREGROUND, new Color(50, 100, 180));
        m.put(TextAttribute.FONT, GuiSwingLogManager.getFont());
        return m;
    }


    public static int setHeaderStyle(AttributedString str, String line, String headerEnd, Map<AttributedCharacterIterator.Attribute, Object> style) {
        int headerEndIndex = line.indexOf(headerEnd);
        if (headerEndIndex >= 0) {
            headerEndIndex += headerEnd.length();
            if (headerEndIndex > 0) {
                str.addAttributes(style, 0, headerEndIndex);
            }
        }
        return headerEndIndex;
    }

    public static LineInfoHead createLineHead(int start, String line, Object idxIntOrDelimStr,
                                              Map<AttributedCharacterIterator.Attribute, Object> headAttrs,
                                              Map<AttributedCharacterIterator.Attribute, Object> attrs) {
        AttributedString a = new AttributedString(line);
        LineInfoHead head = new LineInfoHead(a, start, line.length() + start);
        if (idxIntOrDelimStr instanceof Integer) {
            head.headerEnd = (Integer) idxIntOrDelimStr;
            if (head.headerEnd > 0) {
                a.addAttributes(headAttrs, 0, head.headerEnd);
            }
        } else {
            head.headerEnd = setHeaderStyle(a, line, Objects.toString(idxIntOrDelimStr), headAttrs);
        }
        int s = Math.max(0, head.headerEnd);
        int e = line.length();
        if (s < e) {
            a.addAttributes(attrs, s, e);
        }
        return head;
    }

    public static TextCellRenderer.LineInfo createLineFollowing(TextCellRenderer.LineInfo prevLine, int lineIndex,
                                                       int start, String line,
                                                       Map<AttributedCharacterIterator.Attribute, Object> attrs) {
        AttributedString a = new AttributedString(line);

        TextCellRenderer.LineInfo info = new TextCellRenderer.LineInfo(a, start, line.length() + start);
        if (!line.isEmpty()) {
            a.addAttributes(attrs, 0, line.length());
        }
        int indent = 0;
        if (prevLine instanceof LineInfoHead) {
            indent += Math.max(0, ((LineInfoHead) prevLine).headerEnd);
        }
        if (prevLine != null) {
            indent += prevLine.getIndent();
        }
        info.setIndent(indent);
        return info;
    }

    /**
     * a string log-entry renderer
     */
    public static class GuiSwingLogStringRenderer extends TextCellRenderer<GuiLogEntry>
            implements LogEntryRenderer {
        private static final long serialVersionUID = 1L;

        protected GuiLogManager manager;

        protected ContainerType containerType;

        protected Map<AttributedCharacterIterator.Attribute, Object> timeStyle;
        protected Map<AttributedCharacterIterator.Attribute, Object> followingLineStyle;

        public GuiSwingLogStringRenderer(GuiLogManager manager, ContainerType type) {
            this.manager = manager;
            this.containerType = type;

            timeStyle = getTimeStyle();
            followingLineStyle = getBodyStyle();

            setFont(GuiSwingLogManager.getFont());
            setOpaque(false);
        }

        @Override
        public ListCellRenderer<GuiLogEntry> getTableCellRenderer() {
            return this;
        }

        @Override
        public boolean setValue(GuiLogEntry value, boolean forMouseEvents) {
            if (super.setValue(value, forMouseEvents)) {
                if (value instanceof GuiSwingLogEntryString) {
                    selected = ((GuiSwingLogEntryString) value).isSelected();
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean isValueSame(GuiLogEntry value, boolean forMouseEvents) {
            if (super.isValueSame(value, forMouseEvents)) {
                if (value instanceof GuiSwingLogEntryString) {
                    return selectionStart == ((GuiSwingLogEntryString) value).getSelectionFrom() &&
                        selectionEnd == ((GuiSwingLogEntryString) value).getSelectionTo();
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }

        @Override
        public void setSelectionFromValue(GuiLogEntry value) {
            if (value instanceof GuiSwingLogEntryString) {
                GuiSwingLogEntryString sStr = (GuiSwingLogEntryString) value;
                setSelectionRange(sStr);
            }
            super.setSelectionFromValue(value);
        }

        public void setSelectionRange(GuiSwingLogEntryString sStr) {
            selectionStart = sStr.getSelectionFrom();
            selectionEnd = sStr.getSelectionTo();
        }

        @Override
        public LineInfo createLine(LineInfo prevLine, int lineIndex, int start, String line) {
            if (lineIndex == 0) {
                return createLineHead(start, line, "]", timeStyle, followingLineStyle);
            } else {
                return createLineFollowing(prevLine, lineIndex, start, line, followingLineStyle);
            }
        }

        @Override
        public String format(GuiLogEntry value) {
            if (value instanceof GuiLogEntryString) {
                GuiLogEntryString str = (GuiLogEntryString) value;
                return formatPreProcess(String.format("%s %s",
                        manager.formatTime(str.getTime()),
                        str.getData()));
            } else {
                return super.format(value);
            }
        }

        @Override
        public float[] buildSize() {
            if (value instanceof GuiSwingLogEntry) {
                return ((GuiSwingLogEntry) value).sizeCache(this, super::buildSize);
            } else {
                return super.buildSize();
            }
        }

        @Override
        public float paintStartX(int lineIndex, LineInfo prev, float prevX, LineInfo line, TextLayout l,
                                 FontRenderContext frc) {
            if (prev instanceof LineInfoHead) {
                int h = ((LineInfoHead) prev).headerEnd;
                return prev.getX(prev.getLayout(frc), h);
            } else {
                return prevX;
            }
        }

        @Override
        public void paintLineSelection(Graphics2D g2, LineInfo line, TextLayout l, Color selectionColor, float lineX) {
            if (containerType.equals(ContainerType.StatusBar)) {
                return;
            }
            super.paintLineSelection(g2, line, l, selectionColor, lineX);
        }

        @Override
        public void paintCellSelection(Graphics g, Color selectionColor) {
            if (containerType.equals(ContainerType.StatusBar)) {
                return;
            }
            super.paintCellSelection(g, selectionColor);
        }

        @Override
        public void mousePressed(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            setValue(str, true);
            int idx = getIndex(point);
            str.setSelectionFrom(idx);
            str.setSelectionTo(idx);
            setSelectionRange(str);
        }

        @Override
        public void mouseDragged(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;

            setValue(str, true);
            str.setSelectionTo(getIndex(point));
            setSelectionRange(str);
        }

        @Override
        public void mouseReleased(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            setValue(str, true);
            str.setSelectionTo(getIndex(point));
            setSelectionRange(str);
        }

        @Override
        public String getSelectedText(GuiSwingLogEntry entry, boolean entireText) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            int from = str.getSelectionFrom();
            int to = str.getSelectionTo();
            String text = format(str);
            if (entireText || from == to || outOfRange(from, text) || outOfRange(to, text)) {
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

        private boolean outOfRange(int i, String s) {
            return i < 0 || s.length() < i;
        }

        @Override
        public int findText(GuiSwingLogEntry entry, String findKeyword) {
            GuiSwingLogEntryString str = (GuiSwingLogEntryString) entry;
            setValue(str, false);
            updateFindPattern(findKeyword);
            return setFindHighlights();
        }

        @Override
        public Object focusNextFound(GuiSwingLogEntry entry, Object prevIndex, boolean forward) {
            return getFocusNextFound(entry, prevIndex, forward);
        }
    }

    /** line info. with the header end position */
    public static class LineInfoHead extends TextCellRenderer.LineInfo  {
        public int headerEnd;

        public LineInfoHead(AttributedString attributedString, int start, int end) {
            super(attributedString, start, end);
        }
    }

}
