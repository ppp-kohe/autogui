package autogui.swing.log;

import autogui.base.log.GuiLogEntry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

public class GuiSwingLogList extends JList<GuiLogEntry> {
    protected Timer activePainter;
    protected GuiSwingLogEventDispatcher eventDispatcher;

    public GuiSwingLogList(GuiSwingLogManager manager) {
        super(new GuiSwingLogListModel());
        setOpaque(true);

        eventDispatcher = new GuiSwingLogEventDispatcher(this);
        addMouseListener(eventDispatcher);
        addMouseMotionListener(eventDispatcher);

        getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        //addComponentListener(new GuiSwingLogListResize());
        setCellRenderer(new GuiSwingLogManager.GuiSwingLogRenderer(manager, GuiSwingLogEntry.ContainerType.List));
    }

    public GuiSwingLogListModel getLogListModel() {
        return (GuiSwingLogListModel) getModel();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getLogListModel().hasActiveEntries()) {
            startActivePainter();
        } else {
            stopActivePainter();
        }
    }

    public void startActivePainter() {
        if (activePainter == null) {
            activePainter = new Timer(400, this::paintActive);
            activePainter.setRepeats(true);
        }
        if (!activePainter.isRunning()) {
            activePainter.start();
        }
    }

    public void stopActivePainter() {
        if (activePainter != null && activePainter.isRunning()) {
            activePainter.stop();
        }
    }

    public void paintActive(ActionEvent e) {
        repaint();
    }

    //////////

    public void addLogEntry(GuiLogEntry entry) {
        SwingUtilities.invokeLater(() -> addLogEntry(entry, !entry.isActive()));
    }

    public void addLogEntry(GuiLogEntry entry, boolean lowPriority) {
        GuiSwingLogListModel model = getLogListModel();
        if (model.contains(entry)) {
            return;
        }
        int index = model.addLogEntry(entry, lowPriority);
        Rectangle rect = getVisibleRect();
        int first = rowAtPoint(rect.getLocation());
        int last = rowAtPoint(new Point(rect.x, rect.y + rect.height));

        if (isRowSelected(index)) {
            removeRowSelectionInterval(index, index);
        }

        if (first <= index && index <= last + 1) { //visible
            int target = Math.max(model.getRowCount() - 1, last + 1);
            scrollRectToVisible(getTargetEntryRect(target));
        }
    }

    //// JList vs JTable

    public int rowAtPoint(Point p) {
        return this.locationToIndex(p);
    }

    public boolean isRowSelected(int index) {
        return isSelectedIndex(index);
    }

    public void removeRowSelectionInterval(int from, int to) {
        setSelectionInterval(from, to);
    }

    public int getRowCount() {
        return getModel().getSize();
    }

    public Rectangle getCellRect(int index) {
        return getCellBounds(index, index);
    }

    public int convertRowIndexToModel(int r) {
        return r;
    }

    //public Point indexToLocation(int row) {
        //return getCellRect(row, 0, false).getLocation();
    //}

    public GuiLogEntry getValueAt(int row) {
        return getModel().getElementAt(row);
    }

    /////////////// find

    public void findText(String str, boolean forward) {
        eventDispatcher.findText(str, forward);
    }

    /////////////////

    public Rectangle getTargetEntryRect(int target) {
        Point location = indexToLocation(target);
        Dimension size;
        if (target + 1 < getRowCount()) {
            Point nextLocation = indexToLocation(target + 1);
            size = new Dimension(getWidth(), Math.max(nextLocation.y - location.y, 16));
        } else {
            size = getVisibleRect().getSize();
            location = new Point(size.width, size.height);
        }
        return new Rectangle(location, size);
    }

    public static class GuiSwingLogListModel extends AbstractListModel<GuiLogEntry> {
        java.util.List<GuiLogEntry> entries = new ArrayList<>();


        @Override
        public int getSize() {
            return getRowCount();
        }

        @Override
        public GuiLogEntry getElementAt(int index) {
            return entries.get(index);
        }

        public int getRowCount() {
            return entries.size();
        }


        public int getColumnCount() {
            return 1;
        }


        public Object getValueAt(int rowIndex, int columnIndex) {
            return entries.get(rowIndex);
        }

        public boolean hasActiveEntries() {
            return entries.stream()
                    .anyMatch(GuiLogEntry::isActive);
        }

        public void reload() {
            fireTableDataChanged();
        }

        public void fireTableDataChanged() {
            fireContentsChanged(this, 0, getRowCount() - 1);
        }

        public boolean contains(GuiLogEntry entry) {
            return entries.contains(entry);
        }

        public int addLogEntry(GuiLogEntry entry, boolean lowPriority) {
            int index = 0;
            if (entry.isActive() && !lowPriority) {
                index = entries.size();
            } else {
                for (int i = entries.size() - 1;  i >= 0; --i) {
                    if (!entries.get(i).isActive()) {
                        index = i + 1;
                        break;
                    }
                }
            }
            entries.add(index, entry);
            fireTableRowsInserted(index, entries.size() - 1);
            return index;
        }

        public void fireTableRowsInserted(int from, int to) {
            fireIntervalAdded(this, from, to);
        }
    }


    public static class GuiSwingLogListResize extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            ((GuiSwingLogList) e.getComponent())
                    .getLogListModel()
                    .reload();
        }
    }

    public static class GuiSwingLogEventDispatcher implements MouseListener, MouseMotionListener {
        protected GuiSwingLogList table;
        protected CellRendererPane rendererPane;
        protected Point pressPoint;
        protected String lastFind;

        public GuiSwingLogEventDispatcher(GuiSwingLogList table) {
            this.table = table;
            this.rendererPane = new CellRendererPane();
            table.add(rendererPane);
        }

        public GuiSwingLogEntry getEntry(Point p) {
            int row = table.rowAtPoint(p);
            int modelRow = table.convertRowIndexToModel(row);
            if (0 <= modelRow && modelRow < table.getRowCount()) {
                Object v = table.getValueAt(modelRow);
                if (v instanceof GuiSwingLogEntry) {
                    return (GuiSwingLogEntry) v;
                }
            }
            return null;
        }

        public GuiSwingLogEntry.LogEntryRenderer getEntryRenderer(GuiSwingLogEntry e) {
            if (e == null) {
                return null;
            }
            ListCellRenderer<? super GuiLogEntry> r = table.getCellRenderer();
            if (r instanceof GuiSwingLogManager.GuiSwingLogRenderer) {
                return ((GuiSwingLogManager.GuiSwingLogRenderer) r).getEntryRenderer(e);
            } else {
                return null;
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) { }

        @Override
        public void mousePressed(MouseEvent e) {
            pressPoint = e.getPoint();

            int row = table.rowAtPoint(pressPoint);
            Rectangle cellRect = table.getCellRect(row);
            GuiSwingLogEntry entry = getEntry(pressPoint);
            if (entry == null) {
                table.getSelectionModel().clearSelection();
            } else {
                runEntry(row, entry, r -> {
                    r.mousePressed(entry, convert(cellRect, pressPoint));
                });
            }
        }

        public void runEntry(int row, GuiSwingLogEntry entry, Consumer<GuiSwingLogEntry.LogEntryRenderer> runner) {
            GuiSwingLogEntry.LogEntryRenderer r = getEntryRenderer(entry);
            if (r != null) {
                Component cell = r.getTableCellRenderer().getListCellRendererComponent(table, entry, -1, false, true);
                rendererPane.add(cell);
                cell.setBounds(table.getCellRect(row));
                runner.accept(r);
                rendererPane.removeAll();
                table.repaint();
            }
        }

        public Point convert(Rectangle cellRect, Point p) {
            return new Point(p.x - cellRect.x, p.y - cellRect.y);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            Point point = e.getPoint();
            int row = table.rowAtPoint(point);
            Rectangle cellRect = table.getCellRect(row);
            GuiSwingLogEntry entry = getEntry(point);

            if (pressPoint != null && !pressPoint.equals(point)) {
                drag(pressPoint, point);
            }
            runEntry(row, entry, r -> {
                r.mouseReleased(entry, convert(cellRect, point));
            });
            pressPoint = null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point dragPoint = e.getPoint();
            int row = table.rowAtPoint(dragPoint);
            Rectangle cellRect = table.getCellRect(row);
            GuiSwingLogEntry entry = getEntry(dragPoint);
            if (pressPoint != null) {
                drag(pressPoint, dragPoint);
            } else {
                runEntry(row, entry, r -> {
                    r.mouseDragged(entry, convert(cellRect, dragPoint));
                });
                pressPoint = dragPoint;
            }
        }

        public void drag(Point from, Point to) {
            boolean upToDown = from.y < to.y;
            int rowFrom = table.rowAtPoint(from);
            int rowTo = table.rowAtPoint(to);

            if (rowFrom < 0) {
                rowFrom = upToDown ? 0 : table.getRowCount() - 1;
            }
            if (rowTo < 0) {
                rowTo = upToDown ? table.getRowCount() - 1 : 0;
            }

            Rectangle bounds = new Rectangle(
                    Math.min(from.x, to.x), Math.min(from.y, to.y),
                    Math.abs(to.x - from.x), Math.abs(to.y - from.y));
            if (rowFrom <= rowTo) {
                for (int i = rowFrom; i <= rowTo; ++i) {
                    drag(i, false, i == rowFrom, i == rowTo, bounds, to.x);
                }
            } else {
                for (int i = rowFrom; i >= rowTo; --i) {
                    drag(i, true, i == rowTo, i == rowFrom, bounds, to.x);
                }
            }
            table.repaint();
        }


        public void drag(int i, boolean back, boolean top, boolean bottom, Rectangle bounds, int toX) {
            Rectangle cellRect = table.getCellRect(i);
            int rowIndex = table.convertRowIndexToModel(i);
            Object val = table.getValueAt(rowIndex);
            if (val instanceof GuiSwingLogEntry) {
                GuiSwingLogEntry e = (GuiSwingLogEntry) val;
                runEntry(i, e, r -> {
                    Rectangle selectRect = cellRect.intersection(bounds);
                    Point topLeft = cellRect.getLocation();
                    Point bottomRight = new Point((int) cellRect.getMaxX(), (int) cellRect.getMaxY());
                    if (!back) {
                        if (!top) { //top component is already pressed
                            r.mousePressed(e, convert(cellRect, topLeft));
                        }
                        if (!bottom) {
                            r.mouseDragged(e, convert(cellRect, bottomRight));
                        } else {
                            Point mouseBottomRight = new Point(toX, (int) selectRect.getMaxY());
                            r.mouseDragged(e, convert(cellRect, mouseBottomRight));
                        }
                    } else {
                        if (!bottom) { //bottom component is already pressed
                            r.mousePressed(e, convert(cellRect, bottomRight));
                        }
                        if (!top) {
                            r.mouseDragged(e, convert(cellRect, topLeft));
                        } else {
                            Point mouseTopLeft = new Point(toX, selectRect.y);
                            r.mouseDragged(e, convert(cellRect, mouseTopLeft));
                        }
                    }
                });
            }
        }


        @Override
        public void mouseEntered(MouseEvent e) { }

        @Override
        public void mouseExited(MouseEvent e) { }

        @Override
        public void mouseMoved(MouseEvent e) { }

        public void findText(String str, boolean forward) {
            //TODO backward
            if (Objects.equals(lastFind, str)) {
                boolean found = false;
                int[] is = table.getSelectedIndices();
                int lastIndex = 0;
                if (is != null) {
                    //TODO reverse if backward
                    for (int i : is) {
                        if (findNext(i, forward)) {
                            found = true;
                            lastIndex = i;
                            break;
                        }
                    }
                }
                if (!found) {
                    findLoop(lastIndex, str);
                }
            } else {
                //TODO reverse
                findLoop(table.getFirstVisibleIndex(), str);
            }
            lastFind = str;
        }

        public void findLoop(int start, String str) {
            int count = 0;
            for (int i = start, l = table.getRowCount(); i < l; ++i) {
                count += find(str, i);
                if (count > 0) {
                    table.scrollRectToVisible(table.getCellRect(i));
                    table.setSelectedIndex(i);
                    break;
                }
            }
            if (count == 0) {
                for (int i = 0; i < start; ++i) {
                    count += find(str, i);
                    if (count > 0) {
                        table.scrollRectToVisible(table.getCellRect(i));
                        table.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        private int find(String str, int i) {
            GuiLogEntry e = table.getValueAt(i);
            int[] count = new int[] {0};
            if (e instanceof GuiSwingLogEntry) {
                GuiSwingLogEntry se = (GuiSwingLogEntry) e;
                runEntry(i, se, r -> {
                    count[0] += r.findText(se, str);
                });
            }
            return count[0];
        }

        private boolean findNext(int i, boolean next) {
            GuiLogEntry e = table.getValueAt(i);
            int[] count = new int[] {0};
            if (e instanceof GuiSwingLogEntry) {
                GuiSwingLogEntry se = (GuiSwingLogEntry) e;
                runEntry(i, se, r -> {
                    if (next ? r.focusNextFound() : r.focusPreviousFound()) {
                        count[0] = 1;
                    }
                });
            }
            return count[0] > 0;
        }
    }
}
