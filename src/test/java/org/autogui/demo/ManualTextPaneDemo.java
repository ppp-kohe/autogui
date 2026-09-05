package org.autogui.demo;

import org.autogui.swing.util.TextPaneBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class ManualTextPaneDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new ManualTextPaneDemo()::run);
    }

    JFrame frame;
    JTextPane textPane;

    TextPaneBuilder builder;

    public void run() {
        frame = new JFrame("TextPaneExp");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        {
            class MyTextPane extends JTextPane implements TextPaneBuilder.WrapLineSupport {
                boolean wrapLine = true;
                @Override
                public void setWrapLine(boolean wrapLine) {
                    this.wrapLine = wrapLine;
                }

                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return wrapLine;
                }

                @Override
                public Dimension getPreferredSize() {
                    return TextPaneBuilder.getPreferredSize(this, wrapLine);
                }
            }
            textPane = new MyTextPane();
            {
                builder = new TextPaneBuilder(textPane);
                builder.init();
            }
            frame.add(new JScrollPane(textPane), BorderLayout.CENTER);

            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);
            {
                toolBar.add(new JButton(builder.getUndoManager().getUndoAction()));
                toolBar.add(new JButton(builder.getUndoManager().getRedoAction()));
                toolBar.add(new JToolBar.Separator());
                toolBar.add(new JButton(new AbstractAction() {
                    {
                        putValue(NAME, "Append Time");
                    }
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        appendTime();
                    }
                }));
                toolBar.add(new JButton(new AbstractAction() {
                    {
                        putValue(NAME, "Replace Date");
                    }
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        updateTime();
                    }
                }));
            }
            frame.add(toolBar, BorderLayout.NORTH);
        }
        frame.pack();
        frame.setVisible(true);
    }

    public void appendTime() {
        builder.getContents().append(" ").append(LocalDateTime.now().toString());
    }

    public void updateTime() {
        builder.getContents().set(Pattern.compile("\\w\\w\\w\\w-\\w\\w-\\w\\w")
                .matcher(builder.getContents())
                .replaceAll("____-__-__"));
    }
}
