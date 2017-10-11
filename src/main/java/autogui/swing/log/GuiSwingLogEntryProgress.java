package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryProgress;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.awt.*;
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
        protected JTextPane message;
        protected JTextPane message2;
        protected GuiSwingLogEntryString.TextPaneCellSupport messageSupport;
        protected GuiSwingLogEntryString.TextPaneCellSupport message2Support;

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

        public GuiSwingLogProgressRenderer(GuiSwingLogManager manager, ContainerType type) {
            this.manager = manager;
            setBorder(BorderFactory.createEmptyBorder(7, 10, 3, 20));
            setLayout(new BorderLayout());
            setOpaque(false);

            Font font = GuiSwingLogManager.getFont();

            progressBar = new JProgressBar(0, 1000_000);
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
            progressContainer.add(message2, BorderLayout.EAST);

            messageContainer = new JPanel(new BorderLayout());
            messageContainer.setOpaque(false);
            messageContainer.add(message);

            if (type.equals(ContainerType.List)) {
                messageSupport = new GuiSwingLogEntryString.TextPaneCellSupport(message);
                message2Support = new GuiSwingLogEntryString.TextPaneCellSupport(message2);
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
                if (messageSupport != null) {
                    messageSupport.setSelectionHighlight(p.getSelections());
                }
                if (message2Support != null) {
                    message2Support.setSelectionHighlight(p.getSelections());
                }
            } else {
                if (messageSupport != null) {
                    messageSupport.setSelectionHighlightClear();
                }
                if (message2Support != null) {
                    message2Support.setSelectionHighlightClear();
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
            progressContainer.add(message2, BorderLayout.EAST);
            progressBar.setEnabled(true);
            message2Layout = false;
            message2.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
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
            GuiSwingLogEntryString.TextPaneCellSupport.click(p.getSelections(), true, point, this);
        }

        @Override
        public void mouseDragged(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            GuiSwingLogEntryString.TextPaneCellSupport.click(p.getSelections(), false, point, this);
        }

        @Override
        public int findText(GuiSwingLogEntry entry, String findKeyword) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            if (messageSupport != null) {
                List<Integer> i1 = messageSupport.findText(formatMessageText(p), findKeyword);
                List<Integer> i2 = message2Support.findText(formatProgressText(p), findKeyword);
                return i1.size() + i2.size();
            } else {
                return 0;
            }
        }

        @Override
        public Object focusNextFound(GuiSwingLogEntry entry, Object prevIndex, boolean forward) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            if (messageSupport != null) {
                List<GuiSwingLogEntryString.TextPaneCellSupport> supports = Arrays.asList(messageSupport, message2Support);
                GuiSwingLogEntryString.TextPaneCellMatch m = GuiSwingLogEntryString.nextFindMatchedList(
                        supports, prevIndex, forward, entry);
                if (m != null) {
                    GuiSwingLogEntryString.TextPaneCellSupport support = supports.get(((Integer) m.key(1)));
                    int[] range = support.getFindMatchedRange(m);
                    p.getSelections().clear();
                    p.getSelections().put(support.pane, range);
                }
                return m;
            } else {
                return null;
            }
        }
    }
}
