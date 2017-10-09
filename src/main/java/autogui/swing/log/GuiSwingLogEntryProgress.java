package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryProgress;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.time.Instant;

public class GuiSwingLogEntryProgress extends GuiLogEntryProgress implements GuiSwingLogEntry {

    @Override
    public LogEntryRenderer getRenderer(GuiSwingLogManager manager, ContainerType type) {
        return new GuiSwingLogProgressRenderer(manager, type);
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
            } else {
                if (previousState.isFinished()) {
                    setLayoutToProgress();
                }
                progressBar.setIndeterminate(true);
                message.setText("");
                message2.setText("");
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
    }


}
