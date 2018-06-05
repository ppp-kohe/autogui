package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogManagerConsole;
import autogui.swing.icons.GuiSwingIcons;
import autogui.swing.util.ResizableFlowLayout;
import autogui.swing.util.SearchTextField;
import autogui.swing.util.SettingsWindow;
import autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * a list component for displaying log entries
 */
public class GuiSwingLogList extends JList<GuiLogEntry> {
    protected GuiSwingLogManager manager;
    protected Timer activePainter;
    protected GuiSwingLogEventDispatcher eventDispatcher;

    protected LogListSaveAction saveAction;
    protected LogListClearAction clearAction;
    protected LogListCopyTextAction copyAction;

    protected Object managerKey;

    public GuiSwingLogList(GuiSwingLogManager manager) {
        this(manager, true);
    }

    public GuiSwingLogList(GuiSwingLogManager manager, boolean addManagerAsView) {
        super(new GuiSwingLogListModel());
        setOpaque(true);
        setFocusable(true);

        //clear default UI listeners: BasicListUI.Handler changes the selection interval,
        //  and it will unexpectedly clear the starting row of a drag range
        Arrays.stream(getMouseListeners()).forEach(this::removeMouseListener);
        Arrays.stream(getMouseMotionListeners()).forEach(this::removeMouseMotionListener);

        eventDispatcher = new GuiSwingLogEventDispatcher(this);
        addMouseListener(eventDispatcher);
        addMouseMotionListener(eventDispatcher);

        getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        setCellRenderer(new GuiSwingLogManager.GuiSwingLogRenderer(manager, GuiSwingLogEntry.ContainerType.List));

        saveAction = new LogListSaveAction(this);

        //clear action
        clearAction = new LogListClearAction(this);
        Object name = clearAction.getValue(Action.NAME);
        getInputMap().put((KeyStroke) clearAction.getValue(Action.ACCELERATOR_KEY), name);
        getActionMap().put(name, clearAction);

        copyAction = new LogListCopyTextAction(this);
        getActionMap().put("copy", copyAction);

        this.manager = manager;
        if (addManagerAsView) {
            managerKey = manager.addView(this::addLogEntry);
        }
    }

    public void removeFromManager() {
        manager.removeView(managerKey);
    }

    public LogListSaveAction getSaveAction() {
        return saveAction;
    }

    public LogListCopyTextAction getCopyAction() {
        return copyAction;
    }

    public LogListClearAction getClearAction() {
        return clearAction;
    }

    public GuiSwingLogListModel getLogListModel() {
        return (GuiSwingLogListModel) getModel();
    }

    public int getEntryLimit() {
        return getLogListModel().getEntryLimit();
    }

    /**
     * @param n the limit. &lt;1 means no limit
     */
    public void setEntryLimit(int n) {
        getLogListModel().setEntryLimit(n);
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
        SwingUtilities.invokeLater(() -> addLogEntryInEvent(entry, !entry.isActive()));
    }

    public void addLogEntryInEvent(GuiLogEntry entry, boolean lowPriority) {
        GuiSwingLogListModel model = getLogListModel();
        int index = model.indexOfElement(entry);
        if (index >= 0) {
            if (entry instanceof GuiSwingLogEntry) {
                updateSelectionToSelectionModel((GuiSwingLogEntry) entry, index);
            }
            model.fireRowChangedAt(index);
            return;
        }
        index = model.addLogEntry(entry, lowPriority);
        int first = getFirstVisibleIndex(); //TODO the operation is slow?
        int last = getLastVisibleIndex();

        if (isRowSelected(index)) {
            removeRowSelectionInterval(index, index);
        }

        if (!(first <= index && index <= last + 1) || index == getRowCount() - 1) { //invisible or last item
            int target = Math.max(model.getRowCount() - 1, last + 1);
            scrollRectToVisible(getTargetEntryRectForScroll(target));
        }
    }

    public void updateSelectionToSelectionModel(GuiSwingLogEntry e, int index) {
        ListSelectionModel selModel = getSelectionModel();
        boolean sel = e.isSelected();
        if (selModel.isSelectedIndex(index) != sel) {
            if (sel) {
                selModel.addSelectionInterval(index, index);
            } else {
                selModel.removeSelectionInterval(index, index);
            }
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

    /** only update matching filter of existing renderer
     * @param str the searched text
     */
    public void findText(String str) {
        eventDispatcher.findText(str);
    }

    /** find and move
     * @param str the searched text
     * @param forward forward=true, or backward=false
     */
    public void findText(String str, boolean forward) {
        eventDispatcher.findTextAndScroll(str, forward);
    }

    /////////////////

    public List<String> getSelectedText() {
        return eventDispatcher.getSelectedText();
    }

    public Rectangle getTargetEntryRectForScroll(int target) {
        Rectangle cellRect = getCellRect(target);
        Rectangle visibleRect = getVisibleRect();
        int unit = Math.max(1, UIManagerUtil.getInstance().getScaledSizeInt(1));
        if (cellRect == null) {
            if (target == 0) {
                return new Rectangle(visibleRect.x, 0, visibleRect.width, unit);
            } else if (target == getRowCount() - 1) {
                return new Rectangle(visibleRect.x, getHeight() - unit, visibleRect.width, unit);
            } else {
                return visibleRect;
            }
        } else {
            return new Rectangle(visibleRect.x, (int) cellRect.getMaxY() - unit, visibleRect.width, unit);
        }
    }

    public void showPopup(Point p) {
        JPopupMenu menu = getComponentPopupMenu();
        if (menu == null) {
            menu = new JPopupMenu();
            setComponentPopupMenu(menu);
        }
        menu.removeAll();
        menu.add(getCopyAction());
        menu.add(new JPopupMenu.Separator());
        menu.add(getClearAction());
        menu.show(this, p.x, p.y);
    }

    /** the list model for the log-list component */
    public static class GuiSwingLogListModel extends AbstractListModel<GuiLogEntry> {
        protected int entryLimit = -1;
        protected java.util.List<GuiLogEntry> entries = new ArrayList<>();

        public int getEntryLimit() {
            return entryLimit;
        }

        public void setEntryLimit(int entryLimit) {
            this.entryLimit = entryLimit;
        }

        public java.util.List<GuiLogEntry> getEntries() {
            return entries;
        }

        public int indexOfElement(GuiLogEntry e) {
            return entries.indexOf(e);
        }

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

        public void fireTableDataChanged() {
            fireContentsChanged(this, 0, getRowCount() - 1);
        }

        public void fireRowChanged(GuiLogEntry entry) {
            int i = entries.indexOf(entry);
            if (i >= 0) {
                fireRowChangedAt(i);
            }
        }

        public void fireRowChangedAt(int i) {
            fireContentsChanged(this, i, i);
        }

        public boolean contains(GuiLogEntry entry) {
            return entries.contains(entry);
        }

        public int addLogEntry(GuiLogEntry entry, boolean lowPriority) {
            if (entryLimit > 0 && entries.size() >= entryLimit) {
                int removeIndex = -1;
                for (int i = 0, s = entries.size(); i < s; ++i) {
                    if (!entries.get(i).isActive()) {
                        removeIndex = i;
                        break;
                    }
                }
                if (removeIndex < 0) {
                    removeIndex = 0;
                }
                entries.remove(removeIndex);
                fireIntervalRemoved(this, removeIndex, removeIndex);
            }
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


        public void removeInactiveEntries() {
            boolean removingRange = false;
            int removingStart = 0;
            int i = 0;
            for (Iterator<GuiLogEntry> iter = entries.iterator(); iter.hasNext(); ) {
                GuiLogEntry e = iter.next();
                if (!e.isActive()) {
                    iter.remove();
                    if (!removingRange) {
                        removingStart = i;
                        removingRange = true;
                    }
                } else {
                    if (removingRange) {
                        fireIntervalRemoved(this, removingStart, i);
                    }
                    removingRange = false;
                    i -= (i - removingStart);
                }
                ++i;
            }
            if (removingRange) {
                fireIntervalRemoved(this, removingStart, i - 1);
            }
        }
    }

    /**
     * the event handler for log-entries in the list
     * */
    public static class GuiSwingLogEventDispatcher implements MouseListener, MouseMotionListener {
        protected GuiSwingLogList table;
        protected CellRendererPane rendererPane;
        protected Point pressPoint;
        protected int pressIndex;

        protected FindState findState = new FindState();

        public GuiSwingLogEventDispatcher(GuiSwingLogList table) {
            this.table = table;
            this.rendererPane = new CellRendererPane();
            table.add(rendererPane);

            table.addListSelectionListener(e -> {
                selectionChange(e.getFirstIndex(), e.getLastIndex());
            });
        }

        public void selectionChange(int from, int to) {
            ListSelectionModel sel = table.getSelectionModel();
            for (int i = from; i <= to; ++i) {
                if (i >= 0 && i < table.getRowCount()) {
                    boolean selected = sel.isSelectedIndex(i);
                    GuiLogEntry e = table.getValueAt(i);
                    if (e instanceof GuiSwingLogEntry) {
                        GuiSwingLogEntry se = (GuiSwingLogEntry) e;
                        boolean alreadySelected = se.isSelected();
                        se.setSelected(selected);
                        if (alreadySelected && !selected) {
                            se.clearSelection();
                        }
                    }
                }
            }
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

        public java.util.List<GuiSwingLogEntry.LogEntryRenderer> getRendererList() {
            ListCellRenderer<? super GuiLogEntry> r = table.getCellRenderer();
            if (r instanceof GuiSwingLogManager.GuiSwingLogRenderer) {
                return ((GuiSwingLogManager.GuiSwingLogRenderer) r).getRendererList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) { }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                e.consume();
                table.showPopup(e.getPoint());
                return;
            }

            table.requestFocusInWindow();
            table.setValueIsAdjusting(true);
            pressPoint = e.getPoint();

            int row = table.rowAtPoint(pressPoint);
            pressIndex = row;

            Rectangle cellRect = table.getCellRect(row);
            GuiSwingLogEntry entry = getEntry(pressPoint);
            if (entry != null) {
                ListSelectionModel sel = table.getSelectionModel();
                //sel.addSelectionInterval(row, row);
                if (e.isShiftDown()) {
                    sel.setSelectionInterval(sel.getAnchorSelectionIndex(), row);
                } else {
                    sel.setSelectionInterval(row, row);
                }
                runEntry(row, entry, r -> {
                    r.mousePressed(entry, convert(cellRect, pressPoint));
                });
            }
        }

        public void runEntry(int row, GuiSwingLogEntry entry, Consumer<GuiSwingLogEntry.LogEntryRenderer> runner) {
            GuiSwingLogEntry.LogEntryRenderer r = getEntryRenderer(entry);
            if (r != null) {
                Component cell = r.getTableCellRenderer()
                        .getListCellRendererComponent(table, entry, -1, entry.isSelected(), true);
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
            if (e.isPopupTrigger()) {
                e.consume();
                table.showPopup(e.getPoint());
                return;
            }
            Point point = e.getPoint();
            int row = table.rowAtPoint(point);
            Rectangle cellRect = table.getCellRect(row);
            GuiSwingLogEntry entry = getEntry(point);

            if (pressPoint != null && !pressPoint.equals(point)) {
                updateSelectionRange(pressIndex, row);
                drag(pressPoint, point);
            }
            runEntry(row, entry, r -> {
                r.mouseReleased(entry, convert(cellRect, point));
            });
            pressPoint = null;
            table.setValueIsAdjusting(false);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Point dragPoint = e.getPoint();
            int row = table.rowAtPoint(dragPoint);
            Rectangle cellRect = table.getCellRect(row);
            GuiSwingLogEntry entry = getEntry(dragPoint);
            if (pressPoint != null) {
                updateSelectionRange(pressIndex, row);
                drag(pressPoint, dragPoint);
            } else {
                runEntry(row, entry, r -> {
                    r.mouseDragged(entry, convert(cellRect, dragPoint));
                });
                pressPoint = dragPoint;
            }
        }

        public void updateSelectionRange(int from, int to) {
            table.getSelectionModel().setSelectionInterval(from, to);
        }

        public void drag(Point from, Point to) {
            boolean upToDown = from.y < to.y;
            int rowFrom = table.rowAtPoint(from);
            int rowTo = table.rowAtPoint(to);

            if (table.getRowCount() == 0) {
                return;
            }

            if (rowFrom < 0) {
                rowFrom = upToDown ? 0 : table.getRowCount() - 1;
            }
            if (rowTo < 0) {
                rowTo = upToDown ? table.getRowCount() - 1 : 0;
            }


            Rectangle bounds = new Rectangle(
                    Math.min(from.x, to.x), Math.min(from.y, to.y),
                    Math.abs(to.x - from.x), Math.abs(to.y - from.y));
            if (rowFrom < rowTo ||
                    (rowFrom == rowTo && from.y <= to.y)) { //within same item, up to down
                for (int i = rowFrom; i <= rowTo; ++i) {
                    drag(i, false, i == rowFrom, i == rowTo, bounds, to.x);
                }
            } else { //a lower item to an upper item, or with same item, down to up
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

        public void findText(String str) {
            boolean updated = false;
            for (GuiSwingLogEntry.LogEntryRenderer r : getRendererList()) {
                if (r.updateFindPattern(str)) {
                    updated = true;
                }
            }
            if (updated) {
                table.repaint();
            }
        }

        public void findTextAndScroll(String str, boolean forward) {
            findState.text = str;
            int i = getCurrentFindIndex(forward);

            findTextRows(str, forward, i,
                    forward ? (table.getRowCount() - 1) : 0);

            if (!findState.found()) {
                //loop
                findTextRows(str, true,
                        forward ? 0 : (table.getRowCount() - 1),
                        forward ? (i - 1) : (i + 1));

            }

            if (!findState.found()) {
                findState.entryIndex = -1;
            } else {
                table.scrollRectToVisible(table.getCellRect(findState.entryIndex));
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().addSelectionInterval(findState.entryIndex, findState.entryIndex);
            }
        }

        public int getCurrentFindIndex(boolean forward) {
            if (findState.entryIndex < 0 && forward) {
                return this.table.getFirstVisibleIndex();
            } else if (findState.entryIndex < 0 /*&& !forward*/) {
                return this.table.getLastVisibleIndex();
            } else {
                Rectangle cell = findState.entryIndex < table.getRowCount() ?
                        table.getCellRect(findState.entryIndex) : null;
                if (cell != null && table.getVisibleRect().intersects(cell)) { //visible?
                    return findState.entryIndex;
                } else {
                    return this.table.getFirstVisibleIndex();
                }
            }
        }

        public void findTextRows(String str, boolean forward, int startRow, int endRow) {
            for (int i = startRow;
                    forward ? i <= endRow : i >= endRow;
                    i += (forward ? 1 : -1)) {
                if (i < 0) {
                    continue;
                }

                GuiLogEntry rowValue = table.getValueAt(i);
                if (rowValue instanceof GuiSwingLogEntry) {
                    GuiSwingLogEntry e = (GuiSwingLogEntry) rowValue;
                    int row = i;
                    runEntry(i, e, r -> {
                        findState.entryIndex = row;
                        if (r.findText(e, str) > 0) {
                            findState.entryFocusIndex = r.focusNextFound(e, findState.entryFocusIndex, forward);
                        } else {
                            findState.entryFocusIndex = null;
                        }
                    });

                    if (findState.found()) {
                        break;
                    }
                }
            }
        }

        public List<String> getSelectedText() {
            ListSelectionModel sel = table.getSelectionModel();
            java.util.List<String> lines = new ArrayList<>(table.getRowCount());
            for (int i = sel.getMinSelectionIndex(), l = sel.getMaxSelectionIndex(); i >= 0 && i <= l; ++i) {
                if (sel.isSelectedIndex(i)) {
                    GuiLogEntry e = table.getValueAt(i);
                    if (e instanceof GuiSwingLogEntry) {
                        GuiSwingLogEntry se = (GuiSwingLogEntry) e;
                        runEntry(i, se, r -> {
                            String text = r.getSelectedText(se, false);
                            if (text.isEmpty()) {
                                text = r.getSelectedText(se, true);
                            }
                            lines.add(text);
                        });
                    }
                }
            }
            return lines;
        }
    }

    /**
     * a mutable state type for searching
     */
    public static class FindState {
        public String text;
        public int entryIndex = -1;
        public Object entryFocusIndex;

        public boolean found() {
            return entryIndex != -1 && entryFocusIndex != null;
        }

        @Override
        public String toString() {
            return "<" + text + "> : entry=" + entryIndex + " focus=" + entryFocusIndex;
        }
    }

    public JToolBar createToolBar() {
        JToolBar bar = new JToolBar();
        bar.setBorderPainted(false);
        bar.setFloatable(false);
        bar.add(new GuiSwingIcons.ActionButton(getSaveAction()));
        bar.add(new GuiSwingIcons.ActionButton(getClearAction()));


        JComponent findPane = new JComponent() {
            SearchTextField.SearchBackgroundPainterBordered p = new SearchTextField.SearchBackgroundPainterBordered(this);
            {
                setLayout(new BorderLayout());
                setBackground(Color.white);
            }

            @Override
            public Component add(Component comp) {
                p.setChild((JComponent) comp);
                return super.add(comp);
            }

            @Override
            public void add(Component comp, Object constraints) {
                p.setChild((JComponent) comp);
                super.add(comp, constraints);
            }

            @Override
            protected void paintComponent(Graphics g) {
                p.paintComponent(g);
            }
        };

        JButton icon = new JButton();
        UIManagerUtil ui = UIManagerUtil.getInstance();
        int size = ui.getScaledSizeInt(16);
        icon.setIcon(GuiSwingIcons.getInstance().getPressedIcon("log-", "find", size, size));
        icon.setBorderPainted(false);
        icon.setContentAreaFilled(false);
        findPane.add(icon, BorderLayout.WEST);

        JTextField field = new JTextField(20);
        field.setOpaque(true);
        field.addKeyListener(new LogTextFindAdapter(this, field));
        findPane.add(field);

        int bSize = ui.getScaledSizeInt(5);
        bar.add(ResizableFlowLayout.create(true)
                .withBorder(bSize, bSize)
                .add(findPane, true).getContainer());
        return bar;
    }

    public static class LogListSaveAction extends AbstractAction {
        protected GuiSwingLogList list;
        protected DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm");

        public LogListSaveAction(GuiSwingLogList list) {
            this.list = list;
            putValue(NAME, "Save...");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("save"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("save"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SettingsWindow.FileDialogManager fd = SettingsWindow.getFileDialogManager();

            Path path = fd.showConfirmDialogIfOverwriting(list,
                    fd.showSaveDialog(list, null, "log-" + LocalDateTime.now().format(formatter)+ ".txt"));
            if (path != null) {
                try (PrintStream out = new PrintStream(Files.newOutputStream(path), false, StandardCharsets.UTF_8.name())) {
                    GuiLogManagerConsole console = new GuiLogManagerConsole(out);
                    console.setControlSequence(false);

                    new ArrayList<>(list.getLogListModel().getEntries())
                            .forEach(console::show);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * the action for clearing the log-list
     */
    public static class LogListClearAction extends AbstractAction {
        protected GuiSwingLogList list;

        public LogListClearAction(GuiSwingLogList list) {
            putValue(NAME, "Clear");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            putValue(LARGE_ICON_KEY, GuiSwingIcons.getInstance().getIcon("clear"));
            putValue(GuiSwingIcons.PRESSED_ICON_KEY, GuiSwingIcons.getInstance().getPressedIcon("clear"));
            this.list = list;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            list.getLogListModel().removeInactiveEntries();
        }
    }

    /**
     * the key-handler for starting searching
     */
    public static class LogTextFindAdapter extends KeyAdapter {
        protected GuiSwingLogList list;
        protected JTextComponent findText;

        public LogTextFindAdapter(GuiSwingLogList list, JTextComponent findText) {
            this.list = list;
            this.findText = findText;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                list.findText(findText.getText(), !e.isShiftDown());
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            list.findText(findText.getText());
        }
    }

    /**
     * the action for copying texts of log-entries
     */
    public static class LogListCopyTextAction extends AbstractAction {
        protected GuiSwingLogList list;

        public LogListCopyTextAction(GuiSwingLogList list) {
            putValue(NAME, "Copy");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            this.list = list;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String text = String.join("\n", list.getSelectedText());

            Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection sel = new StringSelection(text);
            board.setContents(sel, sel);
        }
    }
}
