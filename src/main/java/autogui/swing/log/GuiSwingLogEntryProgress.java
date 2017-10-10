package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryProgress;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiSwingLogEntryProgress extends GuiLogEntryProgress implements GuiSwingLogEntry {
    protected Selections selections = new Selections();

    @Override
    public LogEntryRenderer getRenderer(GuiSwingLogManager manager, ContainerType type) {
        return new GuiSwingLogProgressRenderer(manager, type);
    }

    public Selections getSelections() {
        return selections;
    }

    public static class GuiSwingLogProgressRenderer extends JComponent
            implements TableCellRenderer, ListCellRenderer<GuiLogEntry>, LogEntryRenderer {
        protected JProgressBar progressBar;
        protected JTextField message;
        protected JTextField message2;
        protected boolean message2Layout = false;
        protected boolean leftToRight;
        protected GuiLogEntryProgress lastValue;

        protected JComponent progressContainer;
        protected JComponent messageContainer;

        protected GuiSwingLogManager manager;

        protected boolean selected;

        protected GuiLogEntryProgress previousState = new GuiSwingLogEntryProgress();

        protected Object messageHighlightKey;
        protected Object message2HighlightKey;

        public GuiSwingLogProgressRenderer(GuiSwingLogManager manager, ContainerType type) {
            this.manager = manager;
            setBorder(BorderFactory.createEmptyBorder(7, 10, 3, 20));
            setLayout(new BorderLayout());
            setOpaque(false);

            Font font = GuiSwingLogManager.getFont();

            progressBar = new JProgressBar(0, 1000_000);
            message = new JTextField();
            message.setBorder(BorderFactory.createEmptyBorder());
            message.setFont(font);
            message.setEditable(false);
            message.setOpaque(false);

            message2 = new JTextField();
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
                messageHighlightKey = GuiSwingLogEntryString.addHighlight(message);
                message2HighlightKey = GuiSwingLogEntryString.addHighlight(message2);
            }

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
            if (lastValue != null) {
                message.setText(String.format("%s # %s",
                        manager.formatTime(lastValue.getTime()),
                        lastValue.getMessage()));
                if (lastValue.isFinished()) {

                    if (!previousState.isFinished()) {
                        setLayoutToFinish();
                    }

                    message2.setText(String.format("%s # finished: +%s",
                            manager.formatTime(lastValue.getEndTime()),
                            manager.formatDuration(lastValue.getTime(), lastValue.getEndTime())));

                } else {
                    if (previousState.isFinished()) {
                        setLayoutToProgress();
                    }

                    progressBar.setIndeterminate(lastValue.isIndeterminate());
                    progressBar.setValue(lastValue.getValue());
                    progressBar.setMaximum(lastValue.getMaximum());
                    progressBar.setMinimum(lastValue.getMinimum());
                    if (lastValue.isIndeterminate()) {
                        message2.setText(String.format("# +%s",
                                manager.formatDuration(lastValue.getTime(), Instant.now())));
                    } else {
                        message2.setText(String.format("# %2d%% +%s",
                                (int) (lastValue.getValueP() * 100),
                                manager.formatDuration(lastValue.getTime(), Instant.now())));
                    }
                }
                if (lastValue instanceof GuiSwingLogEntryProgress) {
                    Selections sel = ((GuiSwingLogEntryProgress) lastValue).getSelections();
                    GuiSwingLogEntryString.setHighlight(message, messageHighlightKey, selected, sel.message[0], sel.message[1]);
                    GuiSwingLogEntryString.setHighlight(message2, message2HighlightKey, selected, sel.message2[0], sel.message2[1]);
                }
            } else {
                if (previousState.isFinished()) {
                    setLayoutToProgress();
                }
                progressBar.setIndeterminate(true);
                message.setText("");
                message2.setText("");
                GuiSwingLogEntryString.setHighlight(message, messageHighlightKey, true, 0, 0);
                GuiSwingLogEntryString.setHighlight(message2, message2HighlightKey, true, 0, 0);
            }
            setPreviousState(lastValue);
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
            select(0, point, this, p.getSelections());
        }

        @Override
        public void mouseDragged(GuiSwingLogEntry entry, Point point) {
            GuiSwingLogEntryProgress p = (GuiSwingLogEntryProgress) entry;
            select(1, point, this, p.getSelections());
        }

        public void select(int selectionIndex, Point p, JComponent comp, Selections selections) {
            if (comp instanceof JTextComponent) {
                JTextComponent text = (JTextComponent) comp;

                Rectangle bounds = text.getBounds();
                Point viewPoint = new Point(p);
                if (bounds.getMaxY() <= viewPoint.getY()) {
                    viewPoint.x = (int) bounds.getMaxX() + 1;
                } else if (viewPoint.getY() <= bounds.getY()){
                    viewPoint.x = (int) bounds.getX() - 1;
                }

                int pos = text.viewToModel(viewPoint);
                if (comp == message) {
                    selections.message[selectionIndex] = pos;
                } else if (comp == message2) {
                    selections.message2[selectionIndex] = pos;
                }
            }
            for (Component c : comp.getComponents()) {
                if (c instanceof JComponent) {
                    select(selectionIndex, SwingUtilities.convertPoint(comp, p, c), (JComponent) c, selections);
                }
            }
        }
    }

    public static class Selections {
        public int[] message = new int[] {-1, -1};
        public int[] message2 = new int[] {-1, -1};

        public void clear() {
            Arrays.fill(message, -1);
            Arrays.fill(message2, -1);
        }
    }


}
