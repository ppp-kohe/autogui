package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryProgress;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.time.Instant;
import java.util.*;
import java.util.List;

public class GuiSwingLogEntryProgress extends GuiLogEntryProgress implements GuiSwingLogEntry {
    protected Map<JTextComponent,int[]> selections = new HashMap<>(2);

    @Override
    public LogEntryRenderer getRenderer(GuiSwingLogManager manager, ContainerType type) {
        return new GuiSwingLogProgressRenderer(manager, type);
    }

    public Map<JTextComponent, int[]> getSelections() {
        return selections;
    }

    public static class GuiSwingLogProgressRenderer extends JComponent
            implements TableCellRenderer, ListCellRenderer<GuiLogEntry>, LogEntryRenderer {
        protected JProgressBar progressBar;
        protected JButton stopButton;
        protected JTextPane message;
        protected JTextPane message2;
        protected TextPaneCellSupport.TextPaneCellSupportList supports;

        protected boolean message2Layout = false;
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
            setBorder(BorderFactory.createEmptyBorder(7, 10, 3, 20));
            setLayout(new BorderLayout());
            setOpaque(false);

            Font font = GuiSwingLogManager.getFont();

            progressBar = new JProgressBar(0, 1000_000);
            stopButton = new JButton(new ProgressStopAction(this));
            stopButton.setBorderPainted(false);
            stopButton.setPreferredSize(new Dimension(64, 15));

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
                setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                add(messageContainer);
                add(Box.createVerticalStrut(3));
                add(progressContainer);
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


        @Override
        protected void paintComponent(Graphics g) {
            updateByLastValue();
            if (!message2Layout) {
                FontRenderContext context = ((Graphics2D) g).getFontRenderContext();
                Font font = getFont();
                if (font == null) {
                    font = GuiSwingLogManager.getFont();
                }
                if (context != null) {
                    TextLayout l = new TextLayout("# NN% +NNh NNm NNs NNNms", font, context);
                    message2.setMinimumSize(new Dimension((int) l.getAdvance(), message2.getPreferredSize().height));
                    message2.setPreferredSize(message2.getMinimumSize());
                    message2.setSize(message2.getMinimumSize());
                    message2Layout = true;
                }
            }
            super.paintComponent(g);
            if (selected) {
                GuiSwingLogEntryString.drawSelection(getSize(), g);
            }
        }

        public void updateByLastValue() {
            message.setText(formatMessageText(lastValue));
            message.invalidate();

            message2.setText(formatProgressText(lastValue));
            message2.invalidate();

            if (lastValue != null) {
                GuiSwingLogEntryString.setHeaderStyle(message, "] ", timeStyle);

                if (lastValue.isFinished()) {
                    if (!previousState.isFinished()) {
                        setLayoutToFinish();
                    }

                    GuiSwingLogEntryString.setHeaderStyle(message2, "] ", timeStyle2);
                } else {
                    if (previousState.isFinished()) {
                        setLayoutToProgress();
                    }

                    progressBar.setIndeterminate(lastValue.isIndeterminate());
                    progressBar.setValue(lastValue.getValue());
                    progressBar.setMaximum(lastValue.getMaximum());
                    progressBar.setMinimum(lastValue.getMinimum());
                }
            } else {
                if (previousState.isFinished()) {
                    setLayoutToProgress();
                }
                progressBar.setIndeterminate(true);
            }

            setSelectionHighlight();

            setPreviousState(lastValue);
        }

        public String formatMessageText(GuiLogEntryProgress p) {
            if (p != null){
                return String.format("%s # %s",
                        manager.formatTime(p.getTime()),
                        p.getMessage());
            } else {
                return "";
            }
        }

        public String formatProgressText(GuiLogEntryProgress p) {
            if (p != null) {
                if (p.isFinished()) {
                    return String.format("%s # finished: +%s",
                            manager.formatTime(p.getEndTime()),
                            manager.formatDuration(p.getTime(), p.getEndTime()));
                } else {
                    if (p.isIndeterminate()) {
                        return String.format("# +%s",
                                manager.formatDuration(p.getTime(), Instant.now()));
                    } else {
                        return String.format("# %2d%% +%s",
                                (int) (p.getValueP() * 100),
                                manager.formatDuration(p.getTime(), Instant.now()));
                    }
                }
            } else {
                return "";
            }
        }

        public void setSelectionHighlight() {
            if (lastValue != null && lastValue instanceof GuiSwingLogEntryProgress && selected) {
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
            progressContainer.add(message2, BorderLayout.CENTER);
            progressBar.setEnabled(false);
            message2.setPreferredSize(null);
            message2.setBorder(BorderFactory.createEmptyBorder());
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
            JPanel progressTailPane = new JPanel(new FlowLayout());
            progressTailPane.add(stopButton);
            progressTailPane.add(message2);
            progressTailPane.setOpaque(false);
            progressTailPane.setBorder(BorderFactory.createEmptyBorder());
            return progressTailPane;
        }

        public void setPreviousState(GuiLogEntryProgress p) {
            if (p == null) {
                previousState
                        .setTime(null)
                        .setMessage("")
                        .setValue(0)
                        .setIndeterminate(false)
                        .setMaximum(0)
                        .setMinimum(0)
                        .setThread(null)
                        .setEndTime(null);
            } else {
                previousState
                        .setTime(p.getTime())
                        .setMessage(p.getMessage())
                        .setValue(p.getValue())
                        .setIndeterminate(p.isIndeterminate())
                        .setMaximum(p.getMaximum())
                        .setMinimum(p.getMinimum())
                        .setThread(p.getThread())
                        .setEndTime(p.getEndTime());
            }
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
                    int[] range = support.getFindMatchedRange(m);
                    p.getSelections().clear();
                    p.getSelections().put(support.pane, range);
                }
                return m;
            } else {
                return null;
            }
        }

        public void stop() {
            if (stopPressed != null) {
                Thread t = stopPressed.getThread();
                if (t != null && !stopPressed.isFinished()) {
                    t.interrupt();
                }
            }
        }
    }

    public static class ProgressStopAction extends AbstractAction {
        protected GuiSwingLogProgressRenderer renderer;
        public ProgressStopAction(GuiSwingLogProgressRenderer renderer) {
            putValue(NAME, "Stop");
            this.renderer = renderer;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            renderer.stop();
        }
    }
}
