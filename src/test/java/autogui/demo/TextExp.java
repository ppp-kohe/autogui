package autogui.demo;

import autogui.base.log.*;
import autogui.swing.log.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TextExp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new TextExp()::run);
    }

    Text text;

    public void run() {
        GuiSwingLogManager m = new GuiSwingLogManager();
        GuiLogManager.setManager(m);

        GuiSwingLogManager.GuiSwingLogWindow w = new GuiSwingLogManager.GuiSwingLogWindow(m);
        w.setVisible(true);

        JFrame f = new JFrame("hello");
        {
            JPanel pane = new JPanel(new BorderLayout());
            {
                text = new Text();
                pane.add(text);

                text.setText(IntStream.range(0, 30)
                        .mapToObj(i -> " line " + i)
                        .collect(Collectors.joining("\n")));

//                DefaultListModel<GuiLogEntry> logs = new DefaultListModel<>();
//                logs.addElement(new GuiLogEntryString("hello\nworld"));
//                JList<GuiLogEntry> list = new JList<>(logs);
//                list.setCellRenderer(
//                        new GuiSwingLogEntryString.GuiSwingLogStringRenderer2(GuiLogManager.get(), GuiSwingLogEntry.ContainerType.List)
//                                .getTableCellRenderer());
//                pane.add(new JScrollPane(list));

                JToolBar bar = new JToolBar();
                {
                    JButton btn = new JButton("Message 1000");
                    {
                        btn.addActionListener(e -> {
                            for (int i = 0; i < 1000; ++i) {
                                //logs.addElement(new GuiLogEntryString("hello " + i + "\n world" + + i));
                                m.show(new GuiSwingLogEntryString("hello " + i + "\n world" + +i));
                            }
                        });
                        bar.add(btn);
                    }

                    JButton exc = new JButton("Exception");
                    {
                        exc.addActionListener(e -> {
                                    for (int i = 0; i < 20; ++i) {
                                        //m.show(new GuiSwingLogEntryString(i + " hello\nworld"));
                                        GuiLogManager.get().logError(
                                                new RuntimeException(i + " wrapper message",
                                                        new RuntimeException(i + "hello\nworld")));
                                    }
                                });
                        bar.add(exc);
                    }

                    JButton msg = new JButton("Message");
                    {
                        msg.addActionListener(e -> {
                            GuiLogManager.get().logString("hello, world");
                        });
                        bar.add(msg);
                    }

                    JButton prg = new JButton("Progress");
                    {
                        prg.addActionListener(e -> {
                            new Thread(() -> {
                                int stopCount = 0;
                                try (GuiLogEntryProgress p = GuiSwingLogManager.get().logProgress()) {
                                    for (int i = 0; i < 100; ++i) {
                                        p.addValueP(0.01f).setMessage("loop \n <" + i + ">");
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException ie) {
                                            ++stopCount;
                                            if (stopCount > 3) {
                                                throw ie;
                                            }
                                        }
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }).start();
                        });
                        bar.add(prg);
                    }

                    pane.add(bar, BorderLayout.NORTH);
                }

                pane.add(new GuiSwingLogStatusBar(m), BorderLayout.SOUTH);
            }
            f.setContentPane(pane);
        }
        f.setSize(400, 100);
        f.setLocation(500, 200);
        f.setVisible(true);

    }



    static class Text extends JPanel {


        String text = "Hello, world\naaaaaaaaaaaaa\nbbbbbbbbbbb";
        Font font;

        public void setText(String text) {
            this.text = text;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(Color.white);
            g2.fill(new Rectangle(0, 0, getWidth(), getHeight()));
            if (font == null) {
                font = new Font("Menlo", Font.PLAIN, 14);
            }

            String[] lines = text.split("\\n");

            g2.setPaint(Color.black);

            g2.draw(new Rectangle(10, 10, 1000, 1000));

            float y = 10;
            for (String line : lines) {
                line = "[" + Instant.now() + "]" + line;
                AttributedString s = new AttributedString(line);
                int i = line.indexOf("]");
                if (i >= 0) {
                    s.addAttribute(TextAttribute.FOREGROUND, Color.green,0, i + 1);
                }
                s.addAttribute(TextAttribute.FONT, font, 0, line.length());
                TextLayout tl = new TextLayout(s.getIterator(), g2.getFontRenderContext());


                g2.setPaint(Color.red);

                Rectangle2D bs = tl.getBounds();
                g2.draw(new Rectangle2D.Double(10, y, tl.getAdvance(), bs.getHeight()));

                y += tl.getAscent();
                g2.setPaint(Color.black);
                tl.draw(g2, 10, y);

                y += tl.getDescent() + tl.getLeading();
            }
        }
    }
}
