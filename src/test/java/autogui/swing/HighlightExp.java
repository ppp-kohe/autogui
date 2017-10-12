package autogui.swing;

import autogui.swing.log.TextPaneCellSupport;

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
            pane.add(new JScrollPane(text));

            MouseAdapter a = new MouseAdapter() {
                int from;
                int end;
                @Override
                public void mousePressed(MouseEvent e) {
                    int i = text.locationToIndex(e.getPoint());
                    System.err.println("press " + i);
                    if (i >= 0) {
                        text.runEntry(i, o -> {
                            int n = text.text.viewToModel(e.getPoint());
                            System.err.println(n);
                            text.setHighlight(0, n);
                        });
                        text.getSelectionModel().addSelectionInterval(i, i);
                    }
                    from = i;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    int i = text.locationToIndex(e.getPoint());
                    end = i;
                    if (from >= 0 && end >= 0) {
                        text.getSelectionModel().addSelectionInterval(from, end);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    int i = text.locationToIndex(e.getPoint());
                    end = i;
                    if (from >= 0 && end >= 0) {
                        text.getSelectionModel().addSelectionInterval(from, end);
                    }
                }
            };
            text.addMouseListener(a);
            text.addMouseMotionListener(a);

            JButton btn = new JButton("Action");
            btn.addActionListener(e -> {
                //text.randomChange();
//                //text.repaint();
//                text.getSelectionModel().setAnchorSelectionIndex(5);
//                text.getSelectionModel().setLeadSelectionIndex(10);
                System.err.println("anchor " + text.getSelectionModel().getAnchorSelectionIndex()
                        + " lead " + text.getSelectionModel().getLeadSelectionIndex());
                text.getSelectionModel().addSelectionInterval(5, 10);
                System.err.println("anchor " + text.getSelectionModel().getAnchorSelectionIndex()
                        + " lead " + text.getSelectionModel().getLeadSelectionIndex());

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
        CellRendererPane rendererPane = new CellRendererPane();
        TextPaneCellSupport support;

        public PaneList() {
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            text = new JTextPane();
            text.setOpaque(true);
            support = new TextPaneCellSupport(text);
            DefaultListModel m = new DefaultListModel();
            try {
                Files.readAllLines(Paths.get("pom.xml"))
                        .forEach(m::addElement);
            } catch (Exception ex) {
            }

            text.setText("hello, world");

            setModel(m);
            setCellRenderer(new ListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    try {
                        text.setText(value.toString());

                        StyledDocument doc = text.getStyledDocument();

                        Style s = doc.getStyle(StyleContext.DEFAULT_STYLE);
                        StyleConstants.setBold(s, true);
                        StyleConstants.setBackground(s, isSelected ? Color.blue : Color.gray);
                        StyleConstants.setForeground(s, isSelected ? Color.white : Color.darkGray);
                        doc.setLogicalStyle(0, s);
                        text.invalidate();
                        //randomChange();
                    } catch (Exception ex) {
                    }
                    if (isSelected) {
                        text.setBackground(Color.blue);
                    } else {
                        text.setBackground(Color.white);
                    }
                    return text;
                }
            });
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
            support.setSelectionHighlight(true, start, start + sLen);
        }
        public void setHighlight(int s, int e) {
            support.setSelectionHighlight(true, s, e);
        }


    }

    static class Pane extends JComponent {
        CellRendererPane cell = new CellRendererPane();
        public JTextPane text;
        TextPaneCellSupport support;

        public Pane() {
            text = new JTextPane();
            try {
                text.setText(Files.readAllLines(Paths.get("pom.xml")).stream().collect(Collectors.joining("\n")));
            } catch (Exception ex) {
            }

            support = new TextPaneCellSupport(text);
        }

        public void randomChange() {
            StyledDocument doc = text.getStyledDocument();
            int len = doc.getLength();
            int start = (int) (Math.random() * len);
            int sLen = (int) ((len - start) * Math.random());
            support.setSelectionHighlight(true, start, start + sLen);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            randomChange();
            cell.paintComponent(g, text, this, 0, 0, getWidth(), getHeight());
        }
    }
}
