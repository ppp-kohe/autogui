package autogui.swing;

import autogui.swing.log.GuiSwingLogEntry;
import autogui.swing.log.GuiSwingLogEntryString;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HighlightExp extends GuiSwingTestCase {
    public static void main(String[] args) {
        new HighlightExp().test();
    }

    public void test() {
        run(() -> {
            JPanel pane = new JPanel(new BorderLayout());

            PaneList text = new PaneList();
            pane.add(text);

            text.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    System.err.println("press");
                    text.runEntry(0, o -> {
                        int n = text.text.viewToModel(e.getPoint());
                        System.err.println(n);
                        text.setHighlight(0, n);
                    });
                }
            });

            JButton btn = new JButton("rand");
            btn.addActionListener(e -> {
                //text.randomChange();
                //text.repaint();
                text.runEntry(0, o -> text.randomChange());
            });
            pane.add(btn, BorderLayout.NORTH);

            testFrame(pane).setSize(1000, 800);
        });
    }

    static class Painter extends DefaultHighlighter.DefaultHighlightPainter {
        public Painter(Color c) {
            super(c);
        }

        @Override
        public void paint(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c) {
            //System.err.println("paint " + offs0 + "," + offs1 + " : bounds:" + bounds + " size:" + c.getBounds());
            super.paint(g, offs0, offs1, bounds, c);
        }

        @Override
        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
            //System.err.println("paintL " + offs0 + "," + offs1 + " : bounds:" + bounds + " size:" + c.getBounds() + " : view:" + view);
            return super.paintLayer(g, offs0, offs1, bounds, c, view);
        }
    }

    static class PaneList extends JList {
        public JTextPane text;
        public Object h;
        CellRendererPane rendererPane = new CellRendererPane();

        public PaneList() {
            text = new JTextPane();

            DefaultListModel m = new DefaultListModel();
            try {
                String text1 = Files.readAllLines(Paths.get("autogui.iml")).stream().collect(Collectors.joining("\n"));
                String text2 = Files.readAllLines(Paths.get("pom.xml")).stream().collect(Collectors.joining("\n"));

                m.addElement(text1);
                m.addElement(text2);

            } catch (Exception ex) {
            }

            text.setText("hello, world");

            setModel(m);
            setCellRenderer(new ListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    //randomChange();
                    //text.setText(value.toString());

                    try {
                        //text.getHighlighter().removeAllHighlights();
                        StyledDocument doc = text.getStyledDocument();
                        doc.remove(0, doc.getLength());
                        doc.insertString(0, value.toString(), null);
                        //text.updateUI();
                        text.invalidate();
                        //h = text.getHighlighter().addHighlight(0, 0, new Painter(Color.blue));
                        randomChange();
                    } catch (Exception ex) {

                    }
                    return text;
                }
            });
            try {
                Color c = UIManager.getColor("TextPane.selectionBackground");
                h = text.getHighlighter().addHighlight(0, 100,
                        new Painter(c));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.err.println(text.getHighlighter().getHighlights().length);
        }

        public void runEntry(int row, Consumer<Object> runner) {
            Object r = getModel().getElementAt(row);
            if (r != null) {
                Component cell = getCellRenderer()
                        .getListCellRendererComponent(this, r, -1, false, true);
                rendererPane.add(cell);
                cell.setBounds(this.getCellBounds(row, row));
                runner.accept(r);
                rendererPane.removeAll();
                repaint();
            }
        }
        public void randomChange() {
            StyledDocument doc = text.getStyledDocument();
            int len = doc.getLength();
            int start = (int) (Math.random() * len);
            int sLen = (int) ((len - start) * Math.random());
            GuiSwingLogEntryString.setHighlight(text, h, true, start, start + sLen);
        }
        public void setHighlight(int s, int e) {
            GuiSwingLogEntryString.setHighlight(text, h, true, s, e);
        }


    }

    static class Pane extends JComponent {
        CellRendererPane cell = new CellRendererPane();
        public JTextPane text;
        public Object h;

        public Pane() {
            text = new JTextPane();
            try {
                text.setText(Files.readAllLines(Paths.get("pom.xml")).stream().collect(Collectors.joining("\n")));
            } catch (Exception ex) {
            }

            try {
                Color c = UIManager.getColor("TextPane.selectionBackground");
                h = text.getHighlighter().addHighlight(0, 100,
                        new Painter(c));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void randomChange() {
            StyledDocument doc = text.getStyledDocument();
            int len = doc.getLength();
            int start = (int) (Math.random() * len);
            int sLen = (int) ((len - start) * Math.random());
            GuiSwingLogEntryString.setHighlight(text, h, true, start, start + sLen);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            randomChange();
            cell.paintComponent(g, text, this, 0, 0, getWidth(), getHeight());
        }
    }
}
