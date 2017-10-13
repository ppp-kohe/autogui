package autogui.swing.log;

import autogui.base.log.GuiLogEntry;
import autogui.base.log.GuiLogManager;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

public class GuiSwingLogStatusBar extends JComponent {
    protected GuiLogEntry entry;
    protected Component entryComponent;

    protected TableCellRenderer renderer;
    protected Timer activePainter;

    protected CellRendererPane cellRendererPane;

    protected JPanel centerPane;

    public GuiSwingLogStatusBar(GuiSwingLogManager manager) {
        this(manager, true);
    }

    public GuiSwingLogStatusBar(GuiSwingLogManager manager, boolean addManagerAsView) {
        setPreferredSize(new Dimension(100, 28));
        setMinimumSize(new Dimension(100, 28));
        setLayout(new BorderLayout());
        renderer = new GuiSwingLogManager.GuiSwingLogRenderer(manager, GuiSwingLogEntry.ContainerType.StatusBar);
        cellRendererPane = new CellRendererPane();
        centerPane = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                paintCell(g, this);
            }
        };
        centerPane.add(cellRendererPane);
        add(centerPane, BorderLayout.CENTER);

        if (addManagerAsView) {
            manager.addView(this::addLogEntry);
        }
    }

    public void setEntry(GuiLogEntry entry) {
        boolean changed = this.entry != entry;
        this.entry = entry;
        if (changed) {
            entryComponent = null;
        }
    }

    public void setRenderer(TableCellRenderer renderer) {
        this.renderer = renderer;
    }

    public GuiLogEntry getEntry() {
        return entry;
    }

    public TableCellRenderer getRenderer() {
        return renderer;
    }


    public void paintCell(Graphics g, JComponent owner) {
        if (renderer != null && entry != null) {
            //clear, add, layout, paint
            Component c = renderer.getTableCellRendererComponent(null, entry,  false, false, 0, 0);
            cellRendererPane.paintComponent(g, c, owner, 0, 0, owner.getWidth(), owner.getHeight(), true);
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        if (renderer != null && entry != null) {
            Component c = renderer.getTableCellRendererComponent(null, entry,  false, false, 0, 0);
            c.setSize(getSize());
            c.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, c));
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
