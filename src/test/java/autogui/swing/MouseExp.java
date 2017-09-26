package autogui.swing;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MouseExp {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        {
            JPanel  pane = new JPanel();

            pane.add(new DragPane());

            JButton btn = new JButton("Test");
            pane.add(btn);


            frame.setContentPane(pane);
        }
        frame.pack();

        frame.setVisible(true);
    }

    static class DragPane extends JPanel implements MouseListener, MouseMotionListener {
        public Image image;
        public DragPane() {
            setPreferredSize(new Dimension(200, 200));
            setBackground(Color.gray);
            addMouseMotionListener(this);
            addMouseListener(this);
            th = DragSource.getDragThreshold();
            setTransferHandler(new ImageTransferHandler(this));
        }
        int th;
        int pressX;
        int pressY;
        boolean start;

        @Override
        public void mouseDragged(MouseEvent e) {
            if (start) {
                int dx = Math.abs(e.getX() - pressX);
                int dy = Math.abs(e.getY() - pressY);
                if (dx >= th || dy >= th) {
                    getTransferHandler().exportAsDrag(this, e, TransferHandler.COPY);
                }
            }
        }

        public void setImage(Image image) {
            this.image = image;
            repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {
            pressX = e.getX();
            pressY = e.getY();
            start = true;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            start = false;
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                g.drawImage(image, 0, 0, getWidth(), getHeight(),this);
            }
        }
    }


    public static class ImageTransferHandler extends TransferHandler {
        protected DragPane imagePane;

        public ImageTransferHandler(DragPane imagePane) {
            this.imagePane = imagePane;
        }
        static DataFlavor urlList;
        static {
            try {
                urlList = new DataFlavor("application/x-java-url;class=java.net.URL");
                System.err.println(urlList);
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        @Override
        public boolean canImport(TransferSupport support) {
            System.out.println("-------------------");
            Arrays.stream(support.getDataFlavors())
                    .forEach(System.out::println);
            return  (support.isDataFlavorSupported(DataFlavor.imageFlavor) ||
                            support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                            support.isDataFlavorSupported(urlList) ||
                            support.isDataFlavorSupported(DataFlavor.stringFlavor));
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                return select(getTransferableAsImage(support, DataFlavor.imageFlavor));
            } else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return select(loadTransferableFilesAsImage(support, DataFlavor.javaFileListFlavor));
            } else if (support.isDataFlavorSupported(urlList)) {
                try {
                    DataFlavor flv = Arrays.stream(support.getDataFlavors())
                            .filter(f -> f.toString().equals(urlList.toString()))
                            .findFirst().orElse(null);
                    Object obj = support.getTransferable().getTransferData(flv);
                    System.out.println("DATA: " + obj.getClass() + " : " + obj);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return true;
            } else if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    System.out.println("DATA: " + support.getTransferable().getTransferData(DataFlavor.stringFlavor));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return true;
            } else {
                return false;
            }
        }

        public Image getTransferableAsImage(TransferSupport support, DataFlavor flavor) {
            try {
                return (Image) support.getTransferable().getTransferData(flavor);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @SuppressWarnings("unchecked")
        public Image loadTransferableFilesAsImage(TransferSupport support, DataFlavor flavor) {
            try {
                java.util.List<File> fs = (List<File>) support.getTransferable().getTransferData(flavor);
                if (fs != null && !fs.isEmpty()) {
                    try {
                        return ImageIO.read(fs.get(0));
                    } catch (Exception ex) {
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public boolean select(Image image) {
            if (image != null) {
                imagePane.setImage(image);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public int getSourceActions(JComponent c) {
            if (c.equals(imagePane)) {
                return COPY;
            }
            return super.getSourceActions(c);
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new GuiSwingViewImagePane.ImageSelection(imagePane.image);
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            super.exportDone(source, data, action);
        }
    }
}
