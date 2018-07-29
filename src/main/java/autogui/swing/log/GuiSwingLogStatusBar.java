package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.swing.util.UIManagerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * a status-bar for displaying a last log-entry
 */
public class GuiSwingLogStatusBar extends JComponent {
    private static final long serialVersionUID = 1L;
    protected GuiSwingLogManager manager;
    protected GuiLogEntry entry;
    protected Component entryComponent;

    protected GuiSwingLogManager.GuiSwingLogRenderer renderer;
    protected Timer activePainter;

    protected CellRendererPane cellRendererPane;

    protected JPanel centerPane;
    protected Object managerKey;

    public GuiSwingLogStatusBar(GuiSwingLogManager manager) {
        this(manager, true);
    }

    public GuiSwingLogStatusBar(GuiSwingLogManager manager, boolean addManagerAsView) {
        UIManagerUtil ui = UIManagerUtil.getInstance();
        int w = ui.getScaledSizeInt(100);
        int h = ui.getScaledSizeInt(28);
        setPreferredSize(new Dimension(w, h));
        setMinimumSize(new Dimension(w, h));
        setLayout(new BorderLayout());
        renderer = new GuiSwingLogManager.GuiSwingLogRenderer(manager, GuiSwingLogEntry.ContainerType.StatusBar);
        cellRendererPane = new CellRendererPane();
        centerPane = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                paintCell(g, this);
            }
        };
        centerPane.add(cellRendererPane);
        add(centerPane, BorderLayout.CENTER);

        this.manager = manager;
        if (addManagerAsView) {
            managerKey = manager.addView(this::addLogEntry);
        }

        EventDispatcher e = new EventDispatcher(this);
        addMouseListener(e);
        addMouseMotionListener(e);
    }

    public void removeFromManager() {
        manager.removeView(managerKey);
    }

    public void setEntry(GuiLogEntry entry) {
        boolean changed = this.entry != entry;
        this.entry = entry;
        if (changed) {
            entryComponent = null;
        }
    }

    public void setRenderer(GuiSwingLogManager.GuiSwingLogRenderer renderer) {
        this.renderer = renderer;
    }

    public GuiLogEntry getEntry() {
        return entry;
    }

    public GuiSwingLogManager.GuiSwingLogRenderer getRenderer() {
        return renderer;
    }


    public void paintCell(Graphics g, JComponent owner) {
        if (renderer != null && entry != null) {
            //clear, add, layout, paint
            Component c = renderer.getTableCellRendererComponent(null, entry,  false, false, 0, 0);
            cellRendererPane.paintComponent(g, c, owner, 0, 0, owner.getWidth(), owner.getHeight(), true);
        }
    }

    /** a dispatcher delegating to {@link GuiSwingLogStatusBar#dispatch(MouseEvent)} */
    public static class EventDispatcher extends MouseAdapter {
        protected GuiSwingLogStatusBar bar;

        public EventDispatcher(GuiSwingLogStatusBar bar) {
            this.bar = bar;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            bar.dispatch(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            bar.dispatch(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            bar.dispatch(e);
        }
    }

    public void dispatch(MouseEvent e) {
        if (renderer != null && entry instanceof GuiSwingLogEntry) {
            GuiSwingLogEntry se = (GuiSwingLogEntry) entry;
            runEntry(se, r -> {
                Point p = SwingUtilities.convertPoint(this, e.getPoint(), centerPane);
                switch (e.getID()) {
                    case MouseEvent.MOUSE_PRESSED:
                        r.mousePressed(se, p);
                        break;
                    case MouseEvent.MOUSE_RELEASED:
                        r.mouseReleased(se, p);
                        break;
                    case MouseEvent.MOUSE_DRAGGED:
                        r.mouseDragged(se, p);
                        break;
                }
            });
        }
    }

    public void runEntry(GuiSwingLogEntry entry, Consumer<GuiSwingLogEntry.LogEntryRenderer> runner) {
        GuiSwingLogEntry.LogEntryRenderer r = renderer.getEntryRenderer(entry);
        if (r != null) {
            Component cell = r.getTableCellRenderer()
                    .getListCellRendererComponent(null, entry, -1, true, true);
            cellRendererPane.add(cell);
            cell.setBounds(centerPane.getBounds());
            runner.accept(r);
            cellRendererPane.removeAll();
            repaint();
        }
        if (entry != null && entry.isActive()) {
            startActivePainter();
        } else {
            stopActivePainter();
        }
    }

    public void addLogEntry(GuiLogEntry entry) {
        SwingUtilities.invokeLater(() -> {
            this.entry = entry;
            repaint();
        });
    }

    public void startActivePainter() {
        if (activePainter == null) {
            activePainter = new Timer(500, this::paintActive);
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
}
