package org.autogui.swing.log;

import org.autogui.base.log.GuiLogEntry;
import org.autogui.base.log.GuiLogEntryProgress;
import org.autogui.swing.icons.GuiSwingIcons;
import org.autogui.swing.util.TextCellRenderer;
import org.autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.font.TextLayout;
import java.awt.geom.RoundRectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * a log-entry of a progress bar
 */
public class GuiSwingLogEntryProgress extends GuiLogEntryProgress implements GuiSwingLogEntry {
    protected Map<TextCellRenderer<?>,int[]> selections;
    protected boolean selected;
    protected int interruptCount;

    protected SizeCache rendererToSizeCache;

    public GuiSwingLogEntryProgress() {
        selections = new HashMap<>(2);
    }

    public GuiSwingLogEntryProgress(GuiLogEntryProgress p) {
        setState(p);
    }

    @Override
    public float[] sizeCache(Object renderer, Supplier<float[]> src) {
        if (rendererToSizeCache == null) {
            rendererToSizeCache = new SizeCache(3);
        }
        return rendererToSizeCache.computeIfAbsent(renderer, _r -> src.get());
    }

    @Override
    public void setState(GuiLogEntryProgress p) {
        super.setState(p);
        if (p instanceof GuiSwingLogEntryProgress) {
            GuiSwingLogEntryProgress sp = (GuiSwingLogEntryProgress) p;
            selections = sp.selections;
            selected = sp.selected;
            interruptCount = sp.interruptCount;
        }
    }

    @Override
    public LogEntryRenderer getRenderer(GuiSwingLogManager manager, ContainerType type) {
        return new GuiSwingLogProgressRenderer(manager, type);
    }

    public Map<TextCellRenderer<?>, int[]> getSelections() {
        return selections;
    }

    @Override
    public void clearSelection() {
        selections.clear();
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    public void interrupt() {
        Thread t = getThread();
        if (t != null && !isFinished()) {
            ++interruptCount;
            t.interrupt();
        }
    }

    public int getInterruptCount() {
        return interruptCount;
    }

    /** a renderer for a progress entry */
    public static class GuiSwingLogProgressRenderer extends JComponent
            implements TableCellRenderer, ListCellRenderer<GuiLogEntry>, LogEntryRenderer {
        private static final long serialVersionUID = 1L;
        protected ContainerType containerType;
        protected GuiSwingLogManager manager;

        protected JProgressBar progressBar;
        protected JButton stopButton;
        protected TextCellRenderer<GuiLogEntryProgress> message;
        protected TextCellRenderer<GuiLogEntryProgress> message2;
        protected JComponent progressContainer;
        protected JComponent messageContainer;

        protected boolean message2Layout = false;
        protected Dimension message2Size;
        protected boolean leftToRight;

        protected boolean selected;
        protected GuiLogEntryProgress lastValue;
        protected GuiSwingLogEntryProgress stopPressedValue;
        protected GuiLogEntryProgress previousState = new GuiSwingLogEntryProgress();

        protected int lastWidth = 0;

        public GuiSwingLogProgressRenderer(GuiSwingLogManager manager, ContainerType containerType) {
            this.manager = manager;
            this.containerType = containerType;
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int w = ui.getScaledSizeInt(10);
            int h = ui.getScaledSizeInt(5);
            setBorder(BorderFactory.createEmptyBorder(h, w, h, w * 2));
            setLayout(new BorderLayout());
            setOpaque(false);

            Font font = GuiSwingLogManager.getFont();

            progressBar = new JProgressBar(0, 1_000_000);
            progressBar.setOpaque(false);

            stopButton = new GuiSwingIcons.ActionButton(new ProgressStopAction(this));
            {
                stopButton.setBorderPainted(false);
                stopButton.setHideActionText(true);
                int btnSize = ui.getScaledSizeInt(24);
                stopButton.setPreferredSize(new Dimension(btnSize, btnSize));
            }

            message = new ProgressMessageRenderer(manager, containerType);
            {
                message.setBorder(BorderFactory.createEmptyBorder());
                message.setFont(font);
            }

            message2 = new ProgressStatusRenderer(manager, containerType);
            {
                message2.setBorder(BorderFactory.createEmptyBorder());
                message2.setFont(font);
            }

            progressContainer = new JPanel(new BorderLayout());
            {
                progressContainer.setOpaque(false);
            }

            messageContainer = new JPanel(new BorderLayout());
            {
                messageContainer.setOpaque(false);
                messageContainer.add(message);
            }

            initLayout(containerType.equals(ContainerType.StatusBar));
        }

        public void initLayout(boolean leftToRight) {
            if (getComponentCount() > 0) {
                removeAll();
            }
            this.leftToRight = leftToRight;
            if (leftToRight) {
                setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
                add(progressContainer);
                add(Box.createHorizontalStrut(UIManagerUtil.getInstance().getScaledSizeInt(10)));
                add(messageContainer);
            } else {
                setLayout(new BorderLayout());
                add(messageContainer, BorderLayout.NORTH);
                add(progressContainer, BorderLayout.CENTER);
            }
            setLayoutWithProgress();
        }

        public void setLayoutWithProgress() {
            progressContainer.removeAll();
            {
                progressContainer.add(progressBar, BorderLayout.CENTER);
                progressContainer.add(getProgressAccessory(), BorderLayout.EAST);
            }

            progressBar.setEnabled(true);
            message2Layout = false;
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int r = ui.getScaledSizeInt(10);
            message2.setBorder(BorderFactory.createEmptyBorder(0, r, 0, 0));
        }

        public void setLayoutWithFinish() {
            progressContainer.removeAll();
            {
                progressContainer.add(message2, BorderLayout.CENTER);
            }
            progressBar.setEnabled(false);
            message2Layout = true;
            message2.setBorder(BorderFactory.createEmptyBorder());
        }

        public JComponent getProgressAccessory() {
            JPanel progressTailPane = new JPanel(new BorderLayout());
            {
                if (message2Size != null) {
                    //set prefSize for the parent pane instead of message2
                    progressTailPane.setPreferredSize(new Dimension(
                            message2Size.width + stopButton.getPreferredSize().width,
                            Math.max(message2Size.height, stopButton.getPreferredSize().height)));
                }
                progressTailPane.add(stopButton, BorderLayout.WEST);
                progressTailPane.add(message2, BorderLayout.CENTER);
                progressTailPane.setOpaque(false);
                progressTailPane.setBorder(BorderFactory.createEmptyBorder());
            }
            return progressTailPane;
        }

        ///////////////


        @Override
        public ListCellRenderer<GuiLogEntry> getTableCellRenderer() {
            return this;
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            if (lastWidth > 0) {
                width = lastWidth;
            }
            super.setBounds(x, y, width, height);
        }

        public void setLastWidthFromVisibleSize(JComponent comp) {
            if (comp != null && !containerType.equals(ContainerType.StatusBar)) {
                Rectangle rect = comp.getVisibleRect();
                lastWidth = (int) rect.getWidth();
            }
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends GuiLogEntry> list, GuiLogEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            setLastWidthFromVisibleSize(list);
            message.setProperty(list);
            message2.setProperty(list);
            return getTableCellRendererComponent(null, value, isSelected, cellHasFocus, index, 0);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setLastWidthFromVisibleSize(table);
            message.setProperty(table);
            message2.setProperty(table);
            selected = isSelected;
            if (value instanceof GuiLogEntryProgress) {
                lastValue = (GuiLogEntryProgress) value;
            } else {
                lastValue = null;
            }
            updateFromLastValue(row <= -1);
            return this;
        }

        public void updateFromLastValue(boolean forMouseEvents) {
            Dimension size = getPreferredSize();
            GuiSwingLogEntryProgress nextValue = new GuiSwingLogEntryProgress(lastValue);

            TextCellRenderer.setValueForComposition(nextValue, nextValue.getSelections(),
                    forMouseEvents, message, message2);

            selected = nextValue.isSelected();

            if (lastValue != null) {
                if (nextValue.isFinished()) {
                    if (!previousState.isFinished()) {
                        setLayoutWithFinish();
                    }
                } else {
                    if (previousState.isFinished()) {
                        setLayoutWithProgress();
                    }
                    progressBar.setIndeterminate(nextValue.isIndeterminate());
                    progressBar.setValue(nextValue.getValue());
                    progressBar.setMaximum(nextValue.getMaximum());
                    progressBar.setMinimum(nextValue.getMinimum());
                }
            } else {
                if (previousState.isFinished()) {
                    setLayoutWithProgress();
                }
                progressBar.setIndeterminate(true);
            }
            previousState = nextValue;
            Dimension nextSize = getPreferredSize();
            if (!size.equals(nextSize)) {
                invalidate();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (selected && !containerType.equals(ContainerType.StatusBar)) {
                Dimension size = getSize();
                UIManagerUtil ui = UIManagerUtil.getInstance();
                int xy = ui.getScaledSizeInt(2);
                int wh = ui.getScaledSizeInt(5);
                int arc = ui.getScaledSizeInt(3);
                RoundRectangle2D.Double r = new RoundRectangle2D.Double(xy, xy, size.width - wh, size.height - wh, arc, arc);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(message.getSelectionBackground());
                g2.draw(r);
            }
        }

        ///////////////


        @Override
        public void mousePressed(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            TextCellRenderer.mouseUpdateForComposition(p, p.getSelections(), true, this, point, message, message2);

            Point stopPoint = SwingUtilities.convertPoint(this, point, stopButton);
            stopPressedValue = stopButton.contains(stopPoint) ? p : null;
        }

        @Override
        public void mouseDragged(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            TextCellRenderer.mouseUpdateForComposition(p, p.getSelections(), false, this, point, message, message2);
        }

        @Override
        public void mouseReleased(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            TextCellRenderer.mouseUpdateForComposition(p, p.getSelections(), false, this, point, message, message2);

            if (stopPressedValue != null) {
                stopButton.doClick();
            }
        }

        /////////////////


        @Override
        public boolean updateFindPattern(String findKeyword) {
            return TextCellRenderer.updateFindPatternForComposition(findKeyword, message, message2);
        }

        @Override
        public int findText(GuiSwingLogEntry entry, String findKeyword) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            return TextCellRenderer.findTextForComposition(p, findKeyword, message, message2);
        }

        @Override
        public Object focusNextFound(GuiSwingLogEntry entry, Object prevIndex, boolean forward) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            return TextCellRenderer.getFocusNextFoundForComposition(p, prevIndex, forward, message, message2);
        }

        @Override
        public String getSelectedText(GuiSwingLogEntry entry, boolean entireText) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            return TextCellRenderer.getSelectedTextForComposition(p, entireText, p.getSelections(), message, message2);
        }

        /////////////////

        public void interruptStopPressed() {
            if (stopPressedValue != null) {
                stopPressedValue.interrupt();
            }
        }

        @Override
        public void close() {
            progressBar.getUI().uninstallUI(progressBar);
             //to stop animator: a timer thread will be stop and then the application can stop.
        }
    }

    /**
     * an action for terminating a progress bar entry
     */
    public static class ProgressStopAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        protected GuiSwingLogProgressRenderer renderer;
        public ProgressStopAction(GuiSwingLogProgressRenderer renderer) {
            putValue(NAME, "Stop");
            GuiSwingIcons icons = GuiSwingIcons.getInstance();
            int size = UIManagerUtil.getInstance().getScaledSizeInt(16);
            putValue(LARGE_ICON_KEY, icons.getIcon("log-", "stop", size, size));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, icons.getPressedIcon("log-", "stop", size, size));
            this.renderer = renderer;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            renderer.interruptStopPressed();
        }
    }

    /** a renderer for a progress message */
    public static class ProgressMessageRenderer extends TextCellRenderer<GuiLogEntryProgress> {
        private static final long serialVersionUID = 1L;
        protected GuiSwingLogManager manager;
        protected ContainerType containerType;

        protected Map<AttributedCharacterIterator.Attribute, Object> timeStyle;
        protected Map<AttributedCharacterIterator.Attribute, Object> followingLineStyle;

        public ProgressMessageRenderer(GuiSwingLogManager manager, ContainerType containerType) {
            this.manager = manager;
            this.containerType = containerType;
            timeStyle = GuiSwingLogEntryString.getTimeStyle();
            followingLineStyle = GuiSwingLogEntryString.getBodyStyle();

            setFont(GuiSwingLogManager.getFont());
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
        public String format(GuiLogEntryProgress p) {
            if (p != null) {
                return formatPreProcess(String.format("%s # %s",
                        manager.formatTime(p.getTime()),
                        p.getMessage()));
            } else {
                return "";
            }
        }

        @Override
        public LineInfo createLine(LineInfo prevLine, int lineIndex, int start, String line) {
            AttributedString a = new AttributedString(line);
            if (lineIndex == 0) {
                return GuiSwingLogEntryString.createLineHead(start, line, "]", timeStyle, followingLineStyle);
            } else {
                return GuiSwingLogEntryString.createLineFollowing(prevLine, lineIndex, start, line, followingLineStyle);
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
    }

    /** a renderer for progress status */
    public static class ProgressStatusRenderer extends TextCellRenderer<GuiLogEntryProgress> {
        private static final long serialVersionUID = 1L;
        protected GuiSwingLogManager manager;
        protected ContainerType containerType;
        protected boolean finish = false;

        protected Map<AttributedCharacterIterator.Attribute, Object> timeStyle;
        protected Map<AttributedCharacterIterator.Attribute, Object> followingLineStyle;

        public ProgressStatusRenderer(GuiSwingLogManager manager, ContainerType containerType) {
            this.manager = manager;
            this.containerType = containerType;
            timeStyle = GuiSwingLogEntryString.getTimeStyle();
            followingLineStyle = GuiSwingLogEntryString.getBodyStyle();

            setFont(GuiSwingLogManager.getFont());
        }

        @Override
        public String format(GuiLogEntryProgress p) {
            return formatPreProcess(formatBody(p));
        }

        private String formatBody(GuiLogEntryProgress p) {
            finish = false;
            if (p != null) {
                Instant time = p.getTime();
                String interrupt = "   ";
                if (p instanceof GuiSwingLogEntryProgress) {
                    int ic = ((GuiSwingLogEntryProgress) p).getInterruptCount();
                    if (ic > 0) {
                        interrupt = String.format(" X%d", ic);
                    }
                }
                if (p.isFinished()) {
                    finish = true;
                    Instant endTime = p.getEndTime();
                    return String.format("%s # finished: +%s%s",
                            manager.formatTime(endTime),
                            manager.formatDuration(time, endTime),
                            interrupt);
                } else {
                    if (p.isIndeterminate()) {
                        return String.format("# +%s%s",
                                manager.formatDuration(time, Instant.now()),
                                interrupt);
                    } else {
                        return String.format("# %3d%% +%s%s",
                                (int) (p.getValueP() * 100),
                                manager.formatDuration(time, Instant.now()),
                                interrupt);
                    }
                }
            } else {
                return "_";
            }
        }

        @Override
        public LineInfo createLine(LineInfo prevLine, int lineIndex, int start, String line) {
            if (lineIndex == 0) {
                return GuiSwingLogEntryString.createLineHead(start, line, "]", timeStyle, followingLineStyle);
            } else {
                return GuiSwingLogEntryString.createLineFollowing(prevLine, lineIndex, start, line, followingLineStyle);
            }
        }

        @Override
        public void buildFromValue() {
            super.buildFromValue();
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
    }
}
