package org.autogui.demo;

import org.autogui.swing.log.GuiSwingLogList;
import org.autogui.swing.log.GuiSwingLogManager;
import org.autogui.swing.util.SwingDeferredRunner;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.Duration;
import java.time.Instant;

public class ManualLogScalingDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new ManualLogScalingDemo()::init);
    }

    GuiSwingLogManager manager;

    JFrame frame;
    JLabel infoField;
    GuiSwingLogList list;
    void init() {
        frame = new JFrame("Test");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        {
            JPanel pane = new JPanel(new BorderLayout());
            {
                JToolBar bar = new JToolBar();
                {
                    bar.add(new AbstractAction() {
                        {
                            putValue(NAME, "Test 100");
                        }
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            addLogs(100);
                        }
                    });
                    bar.add(new AbstractAction() {
                        {
                            putValue(NAME, "Test 1K");
                        }
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            addLogs(1000);
                        }
                    });

                    bar.add(new AbstractAction() {
                        {
                            putValue(NAME, "Test 10K");
                        }
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            addLogs(10_000);
                        }
                    });
                    bar.add(new AbstractAction() {
                        {
                            putValue(NAME, "Test 100K");
                        }
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            addLogs(100_000);
                        }
                    });
                }
                pane.add(bar, BorderLayout.NORTH);

                manager = new GuiSwingLogManager();
                list = new GuiSwingLogList(manager);
                list.setEntryLimit(10000);

                JScrollPane scroll = new JScrollPane(list);

                pane.add(scroll);

                infoField = new JLabel(" ");
                infoField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                pane.add(infoField, BorderLayout.SOUTH);
                list.getModel().addListDataListener(new ListDataListener() {
                    @Override
                    public void intervalAdded(ListDataEvent e) {
                        SwingDeferredRunner.invokeLater(ManualLogScalingDemo.this::updateInfo);
                    }

                    @Override
                    public void intervalRemoved(ListDataEvent e) {
                        SwingDeferredRunner.invokeLater(ManualLogScalingDemo.this::updateInfo);
                    }

                    @Override
                    public void contentsChanged(ListDataEvent e) {
                        SwingDeferredRunner.invokeLater(ManualLogScalingDemo.this::updateInfo);
                    }
                });
            }
            frame.getContentPane().add(pane);
        }
        frame.pack();
        frame.setVisible(true);
    }

    long count;

    public void addLogs(int n) {
        Instant time = Instant.now();
        for (int i = 0; i < n-1; ++i) {
            manager.logFormat("%,d", count);
            ++count;
        }
        manager.logFormat("%,d : %s", count, Duration.between(time, Instant.now()));
    }

    public void updateInfo() {
        int rows = list.getRowCount();
        infoField.setText(String.format("displaying-logs: %,d,  count: %,d", rows, count));
    }
}
