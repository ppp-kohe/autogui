package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryProgress;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.util.TextCellRenderer;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.RoundRectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * a log-entry of a progress bar
 */
public class GuiSwingLogEntryProgress extends GuiLogEntryProgress implements GuiSwingLogEntry {
    protected Map<TextCellRenderer<?>,int[]> selections;
    protected boolean selected;

    public GuiSwingLogEntryProgress() {
        selections = new HashMap<>(2);
    }

    public GuiSwingLogEntryProgress(GuiLogEntryProgress p) {
        setState(p);
        if (p instanceof GuiSwingLogEntryProgress) {
            selections = ((GuiSwingLogEntryProgress) p).selections;
            selected = ((GuiSwingLogEntryProgress) p).selected;
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

    public static class GuiSwingLogProgressRenderer extends JComponent
            implements TableCellRenderer, ListCellRenderer<GuiLogEntry>, LogEntryRenderer {
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

        public GuiSwingLogProgressRenderer(GuiSwingLogManager manager, ContainerType containerType) {
            this.manager = manager;
            this.containerType = containerType;

            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 20));
            setLayout(new BorderLayout());
            setOpaque(false);

            Font font = GuiSwingLogManager.getFont();

            progressBar = new JProgressBar(0, 1_000_000);

            stopButton = new GuiSwingIcons.ActionButton(new ProgressStopAction(this));
            {
                stopButton.setBorderPainted(false);
                stopButton.setHideActionText(true);
                stopButton.setPreferredSize(new Dimension(24, 24));
            }

            message = new ProgressMessageRenderer(manager);
            {
                message.setBorder(BorderFactory.createEmptyBorder());
                message.setFont(font);
            }

            message2 = new ProgressStatusRenderer(manager);
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
                add(Box.createHorizontalStrut(10));
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
            message2.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        }

        public void setLayoutWithFinish() {
            progressContainer.removeAll();
            {
                progressContainer.add(message2, BorderLayout.CENTER);
            }
            progressBar.setEnabled(false);
            message2.setPreferredSize(null);
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
        public Component getListCellRendererComponent(JList<? extends GuiLogEntry> list, GuiLogEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            return getTableCellRendererComponent(null, value, isSelected, cellHasFocus, index, 0);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
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
            GuiSwingLogEntryProgress nextValue = new GuiSwingLogEntryProgress(lastValue);

            TextCellRenderer.setValueForComposition(nextValue, ((GuiSwingLogEntryProgress) nextValue).getSelections(),
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
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (selected) {
                Dimension size = getSize();
                RoundRectangle2D.Float r = new RoundRectangle2D.Float(2, 2, size.width - 5, size.height - 5, 3, 3);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(TextCellRenderer.getSelectionColor());
                g2.draw(r);
            }
        }

        ///////////////


        @Override
        public void mousePressed(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            TextCellRenderer.mouseUpdateForComposition(p, p.getSelections(), true, point, message, message2);

            Point stopPoint = SwingUtilities.convertPoint(this, point, stopButton);
            stopPressedValue = stopButton.contains(stopPoint) ? p : null;
        }

        @Override
        public void mouseDragged(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            TextCellRenderer.mouseUpdateForComposition(p, p.getSelections(), false, point, message, message2);
        }

        @Override
        public void mouseReleased(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            TextCellRenderer.mouseUpdateForComposition(p, p.getSelections(), false, point, message, message2);

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
                Thread t = stopPressedValue.getThread();
                if (t != null && !stopPressedValue.isFinished()) {
                    t.interrupt();
                }
            }
        }
    }

    /**
     * an action for terminating a progress bar entry
     */
    public static class ProgressStopAction extends AbstractAction {
        protected GuiSwingLogProgressRenderer renderer;
        public ProgressStopAction(GuiSwingLogProgressRenderer renderer) {
            putValue(NAME, "Stop");
            GuiSwingIcons icons = GuiSwingIcons.getInstance();
            putValue(LARGE_ICON_KEY, icons.getIcon("log-", "stop", 16, 16));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, icons.getPressedIcon("log-", "stop", 16, 16));
            this.renderer = renderer;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            renderer.interruptStopPressed();
        }
    }


    public static class ProgressMessageRenderer extends TextCellRenderer<GuiLogEntryProgress> {
        protected GuiSwingLogManager manager;

        protected Map<AttributedCharacterIterator.Attribute, Object> timeStyle;
        protected Map<AttributedCharacterIterator.Attribute, Object> followingLineStyle;

        public ProgressMessageRenderer(GuiSwingLogManager manager) {
            this.manager = manager;
            timeStyle = GuiSwingLogEntryString.getTimeStyle();
            followingLineStyle = GuiSwingLogEntryString.getBodyStyle();

            setFont(GuiSwingLogManager.getFont());
        }

        @Override
        public String format(GuiLogEntryProgress p) {
            if (p != null) {
                return String.format("%s # %s",
                        manager.formatTime(p.getTime()),
                        p.getMessage());
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
    }

    public static class ProgressStatusRenderer extends TextCellRenderer<GuiLogEntryProgress> {
        protected GuiSwingLogManager manager;
        protected boolean finish = false;

        protected Map<AttributedCharacterIterator.Attribute, Object> timeStyle;
        protected Map<AttributedCharacterIterator.Attribute, Object> followingLineStyle;

        public ProgressStatusRenderer(GuiSwingLogManager manager) {
            this.manager = manager;
            timeStyle = GuiSwingLogEntryString.getTimeStyle();
            followingLineStyle = GuiSwingLogEntryString.getBodyStyle();

            setFont(GuiSwingLogManager.getFont());
        }

        @Override
        public String format(GuiLogEntryProgress p) {
            finish = false;
            if (p != null) {
                Instant time = p.getTime();
                if (p.isFinished()) {
                    finish = true;
                    Instant endTime = p.getEndTime();
                    return String.format("%s # finished: +%s",
                            manager.formatTime(endTime),
                            manager.formatDuration(time, endTime));
                } else {
                    if (p.isIndeterminate()) {
                        return String.format("# +%s",
                                manager.formatDuration(time, Instant.now()));
                    } else {
                        return String.format("# %2d%% +%s",
                                (int) (p.getValueP() * 100),
                                manager.formatDuration(time, Instant.now()));
                    }
                }
            } else {
                return "";
            }
        }

        @Override
        public LineInfo createLine(LineInfo prevLine, int lineIndex, int start, String line) {
            AttributedString a = new AttributedString(line);
            if (lineIndex == 0) {
                return GuiSwingLogEntryString.createLineHead(start, line, (finish ? "]" : 0), timeStyle, followingLineStyle);
            } else {
                return GuiSwingLogEntryString.createLineFollowing(prevLine, lineIndex, start, line, followingLineStyle);
            }
        }
    }

    /*
    @Deprecated
    public static class GuiSwingLogProgressRenderer extends JComponent
            implements TableCellRenderer, ListCellRenderer<GuiLogEntry>, LogEntryRenderer {
        protected ContainerType containerType;
        protected JProgressBar progressBar;
        protected JButton stopButton;
        protected JTextPane message;
        protected JTextPane message2;
        protected TextPaneCellSupport.TextPaneCellSupportList supports;

        protected boolean message2Layout = false;
        protected Dimension message2Size;
        protected boolean leftToRight;
        protected GuiLogEntryProgress lastValue;

        protected JComponent progressContainer;
        protected JComponent messageContainer;

        protected GuiSwingLogManager manager;

        protected boolean selected;

        protected GuiLogEntryProgress previousState = new GuiSwingLogEntryProgress();

        protected Style timeStyle;
        protected Style timeStyle2;

        protected GuiLogEntryProgress stopPressed;

        public GuiSwingLogProgressRenderer(GuiSwingLogManager manager, ContainerType type) {
            this.manager = manager;
            this.containerType = type;
            setBorder(BorderFactory.createEmptyBorder(7, 10, 3, 20));
            setLayout(new BorderLayout());
            setOpaque(false);

            Font font = GuiSwingLogManager.getFont();

            progressBar = new JProgressBar(0, 1000_000);
            stopButton = new GuiSwingIcons.ActionButton(new ProgressStopAction(this));
            stopButton.setBorderPainted(false);
            stopButton.setHideActionText(true);
            stopButton.setPreferredSize(new Dimension(24, 24));

            message = new JTextPane();
            message.setBorder(BorderFactory.createEmptyBorder());
            message.setFont(font);
            message.setEditable(false);
            message.setOpaque(false);

            message2 = new JTextPane();
            message2.setBorder(BorderFactory.createEmptyBorder());
            message2.setFont(font);
            message2.setEditable(false);
            message2.setOpaque(false);

            progressContainer = new JPanel(new BorderLayout());
            progressContainer.setOpaque(false);
            progressContainer.add(progressBar, BorderLayout.CENTER);

            progressContainer.add(progressAccessory(), BorderLayout.EAST);

            messageContainer = new JPanel(new BorderLayout());
            messageContainer.setOpaque(false);
            messageContainer.add(message);

            if (type.equals(ContainerType.List)) {
                supports = new TextPaneCellSupport.TextPaneCellSupportList(message, message2);
            }

            timeStyle = GuiSwingLogEntryString.getTimeStyle(message.getStyledDocument());
            timeStyle2 = GuiSwingLogEntryString.getTimeStyle(message2.getStyledDocument());
            StyleConstants.setForeground(timeStyle2, new Color(82, 116, 213));

            init(type.equals(ContainerType.StatusBar));

            setLayoutToProgress();
        }

        public void init(boolean leftToRight) {
            if (getComponentCount() > 0) {
                removeAll();
            }
            if (leftToRight) {
                setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
                add(progressContainer);
                add(Box.createHorizontalStrut(10));
                add(messageContainer);
            } else {
                setLayout(new BorderLayout());
                add(messageContainer, BorderLayout.NORTH);
                add(progressContainer, BorderLayout.CENTER);
            }
            setLayoutToProgress();
            this.leftToRight = leftToRight;
        }

        @Override
        public ListCellRenderer<GuiLogEntry> getTableCellRenderer() {
            return this;
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends GuiLogEntry> list, GuiLogEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            return getTableCellRendererComponent(null, value, isSelected, cellHasFocus, index, 0);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            selected = isSelected;
            if (value instanceof GuiLogEntryProgress) {
                lastValue = (GuiLogEntryProgress) value;
            } else {
                lastValue = null;
            }
            updateByLastValue();
            return this;
        }

        //TODO copied?
        @Override
        protected void paintComponent(Graphics g) {

            updateByLastValue();
            if (!message2Layout &&
                    message2Size == null) { //frequent process of the following code cause flicker
                FontRenderContext context = ((Graphics2D) g).getFontRenderContext();
                Font font = getFont();
                if (font == null) {
                    font = GuiSwingLogManager.getFont();
                }
                if (context != null) {
                    TextLayout l = new TextLayout("# NN% +NNh NNm NNs NNNms", font, context);
                    message2Size = new Dimension((int) l.getAdvance(), message2.getPreferredSize().height);
                    message2.setSize(message2Size);
                    message2Layout = true;
                }
            }
            super.paintComponent(g);
            if (selected && containerType.equals(ContainerType.List)) {
                GuiSwingLogEntryString.drawSelection(getSize(), g);
            }
        }

        public void updateByLastValue() {
            GuiLogEntryProgress nextValue = new GuiSwingLogEntryProgress();
            nextValue.setState(lastValue);

            message.setText(formatMessageText(nextValue));

            message2.setText(formatProgressText(nextValue));

            if (lastValue != null) {
                GuiSwingLogEntryString.setHeaderStyle(message, "] ", timeStyle);

                if (nextValue.isFinished()) {
                    if (!previousState.isFinished()) {
                        setLayoutToFinish();
                    }

                    GuiSwingLogEntryString.setHeaderStyle(message2, "] ", timeStyle2);
                } else {
                    if (previousState.isFinished()) {
                        setLayoutToProgress();
                    }

                    progressBar.setIndeterminate(nextValue.isIndeterminate());
                    progressBar.setValue(nextValue.getValue());
                    progressBar.setMaximum(nextValue.getMaximum());
                    progressBar.setMinimum(nextValue.getMinimum());
                }
            } else {
                if (previousState.isFinished()) {
                    setLayoutToProgress();
                }
                progressBar.setIndeterminate(true);
            }

            setSelectionHighlight();
            if (supports != null) {
                supports.setFindHighlights();
            }

            previousState = nextValue;
        }

        public String formatMessageText(GuiLogEntryProgress p) {
            if (p != null) {
                return String.format("%s # %s",
                        manager.formatTime(p.getTime()),
                        p.getMessage());
            } else {
                return "";
            }
        }

        public String formatProgressText(GuiLogEntryProgress p) {
            if (p != null) {
                Instant time = p.getTime();
                if (p.isFinished()) {
                    Instant endTime = p.getEndTime();
                    return String.format("%s # finished: +%s",
                            manager.formatTime(endTime),
                            manager.formatDuration(time, endTime));
                } else {
                    if (p.isIndeterminate()) {
                        return String.format("# +%s",
                                manager.formatDuration(time, Instant.now()));
                    } else {
                        return String.format("# %2d%% +%s",
                                (int) (p.getValueP() * 100),
                                manager.formatDuration(time, Instant.now()));
                    }
                }
            } else {
                return "";
            }
        }

        public void setSelectionHighlight() {
            if (lastValue instanceof GuiSwingLogEntryProgress) {
                selected = ((GuiSwingLogEntryProgress) lastValue).isSelected();
            }

            if (lastValue instanceof GuiSwingLogEntryProgress && selected) {
                GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) lastValue;
                if (supports != null) {
                    supports.setSelectionHighlights(p.getSelections());
                }
            } else {
                if (supports != null) {
                    supports.setSelectionHighlightsClear();
                }
            }
        }

        public void setLayoutToFinish() {
            progressContainer.removeAll();
            progressBar.setEnabled(false);
            message2.setPreferredSize(null);
            message2.setBorder(BorderFactory.createEmptyBorder());
            progressContainer.add(message2, BorderLayout.CENTER);
        }

        public void setLayoutToProgress() {
            progressContainer.removeAll();
            progressContainer.add(progressBar, BorderLayout.CENTER);

            progressContainer.add(progressAccessory(), BorderLayout.EAST);
            progressBar.setEnabled(true);
            message2Layout = false;
            message2.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        }

        public JComponent progressAccessory() {
            JPanel progressTailPane = new JPanel(new BorderLayout());//new JPanel(new FlowLayout());
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
            return progressTailPane;
        }

        @Override
        public boolean isShowing() {
            return true;
        }

        @Override
        public void mousePressed(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            p.getSelections().clear();
            TextPaneCellSupport.click(p.getSelections(), true, point, this);

            Point stopPoint = SwingUtilities.convertPoint(this, point, stopButton);
            stopPressed = stopButton.contains(stopPoint) ? p : null;
        }

        @Override
        public void mouseReleased(GuiSwingLogEntry entry, Point point) {
            if (stopPressed != null) {
                stopButton.doClick();
            }
        }

        @Override
        public void mouseDragged(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            TextPaneCellSupport.click(p.getSelections(), false, point, this);
        }

        @Override
        public boolean updateFindPattern(String findKeyword) {
            return supports != null &&
                    supports.updateFindPattern(findKeyword);
        }

        @Override
        public int findText(GuiSwingLogEntry entry, String findKeyword) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            if (supports != null) {
                List<List<Integer>> is = supports.findTexts(findKeyword, formatMessageText(p), formatProgressText(p));
                return is.stream()
                        .mapToInt(List::size)
                        .sum();
            } else {
                return 0;
            }
        }

        @Override
        public Object focusNextFound(GuiSwingLogEntry entry, Object prevIndex, boolean forward) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            if (supports != null) {
                TextPaneCellSupport.TextPaneCellMatchList m = supports.nextFindMatchedList(prevIndex, forward, entry);
                if (m != null) {
                    TextPaneCellSupport support = supports.getSupport(m.getSupportIndex());
                    support.updateSelectionMap(p.getSelections(), m);
                }
                return m;
            } else {
                return null;
            }
        }

        public void interruptStopPressed() {
            if (stopPressed != null) {
                Thread t = stopPressed.getThread();
                if (t != null && !stopPressed.isFinished()) {
                    t.interrupt();
                }
            }
        }

        @Override
        public String getSelectedText(GuiSwingLogEntry entry, boolean entireText) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            if (p != lastValue) {
                lastValue = p;
                updateByLastValue();
            }
            if (supports != null) {
                return supports.getSelectedTexts(entireText ? null : p.getSelections()).stream()
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining("\n"));

            } else {
                return message2.getText() + "\t" + message.getText();
            }
        }
    }

    public static class ProgressStopAction extends AbstractAction {
        protected GuiSwingLogProgressRenderer renderer;
        public ProgressStopAction(GuiSwingLogProgressRenderer renderer) {
            putValue(NAME, "Stop");
            GuiSwingIcons icons = GuiSwingIcons.getInstance();
            putValue(LARGE_ICON_KEY, icons.getIcon("log-", "stop", 16, 16));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, icons.getPressedIcon("log-", "stop", 16, 16));
            this.renderer = renderer;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            renderer.interruptStopPressed();
        }
    }*/
}
