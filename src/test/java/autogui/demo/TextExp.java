package autogui.demo;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogEntryString;
import autogui.base.log.GuiLogManager;
import autogui.swing.log.GuiSwingLogEntry;
import autogui.swing.log.GuiSwingLogEntryString;
import autogui.swing.log.GuiSwingLogManager;

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

        for (int i = 0; i < 20; ++i) {
            m.show(new GuiSwingLogEntryString(i + " hello\nworld"));
        }

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

                JButton btn = new JButton("Add");
                btn.addActionListener(e -> {
                    for (int i = 0; i < 1000; ++i) {
                        //logs.addElement(new GuiLogEntryString("hello " + i + "\n world" + + i));
                        m.show(new GuiSwingLogEntryString("hello " + i + "\n world" + + i));
                    }
                });

                pane.add(btn, BorderLayout.NORTH);
            }
            f.setContentPane(pane);
        }
        f.setSize(200, 100);
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
                System.err.println(bs);

                y += tl.getAscent();
                g2.setPaint(Color.black);
                tl.draw(g2, 10, y);

                y += tl.getDescent() + tl.getLeading();
            }
        }
    }
}
