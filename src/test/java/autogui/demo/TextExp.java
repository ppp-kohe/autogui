package autogui.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
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
        JFrame f = new JFrame("hello");
        {
            JPanel pane = new JPanel(new BorderLayout());
            {
                text = new Text();
                pane.add(text);

                text.setText(IntStream.range(0, 30)
                        .mapToObj(i -> " line " + i)
                        .collect(Collectors.joining("\n")));
            }
            f.setContentPane(pane);
        }
        f.setSize(1000, 1000);
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
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setPaint(Color.white);
            g2.fill(new Rectangle(0, 0, getWidth(), getHeight()));
            if (font == null) {
                font = new Font("Menlo", Font.PLAIN, 14);
            }

            String[] lines = text.split("\\n");

            ((Graphics2D) g).setPaint(Color.black);

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
                y += tl.getBounds().getHeight() + 10;
                tl.draw(g2, 10, y);

            }
        }
    }
}
