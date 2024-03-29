package org.autogui.swing.table;

import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiTaskClock;
import org.autogui.swing.GuiSwingView;
import org.autogui.swing.GuiSwingViewEmbeddedComponent;
import org.autogui.swing.util.TextCellRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * a column factory for {@link JComponent}
 *
 * <p>
 *     The renderer is realized by {@link ColumnEmbeddedPane}.
 *     The editor is realized by {@link ColumnEditEmbeddedPane}.
 * </p>
 *
 * <p>
 *     To achieve active repainting of value-components, {@link EmbeddedComponentRepaintManager} is used.
 *     Renderer and editor share the manager with mappings between the value-component and (row,col).
 *     Also, it installs the value-component {@link EmbeddedComponentHierarchyMaintainer} for observe removed components
 *     and it can  immediately add a orphan component to {@link EmbeddedComponentOrphanPane}.
 *     The client can call repaint() of the parent component of the value-component in order to updating cells.
 * </p>
 */
public class GuiSwingTableColumnEmbeddedComponent implements GuiSwingTableColumn {
    @Override
    public ObjectTableColumn createColumn(GuiMappingContext context, SpecifierManagerIndex rowSpecifier,
                                          GuiSwingView.SpecifierManager parentSpecifier) {
        GuiSwingView.SpecifierManager valueSpecifier = new GuiSwingView.SpecifierManagerDefault(parentSpecifier::getSpecifier);
        EmbeddedComponentRepaintManager repaintManager = new EmbeddedComponentRepaintManager();
        ObjectTableColumnValue.ObjectTableCellEditor editor = new ObjectTableColumnValue.ObjectTableCellEditor(new ColumnEditEmbeddedPane(context, repaintManager, valueSpecifier), false, rowSpecifier);
        editor.setClickCount(0);
        return new ObjectTableColumnValue(context, rowSpecifier, valueSpecifier,
                new ObjectTableColumnValue.ObjectTableCellRenderer(new ColumnEmbeddedPane(context, valueSpecifier, repaintManager), rowSpecifier),
                editor)
                .withBorderType(ObjectTableColumnValue.CellBorderType.Regular)
                .withEditorForColumnAlwaysApplying(true)
                .withValueType(JComponent.class)
                .withComparator(Comparator.comparing(Objects::hash));
    }

    public static class ColumnEmbeddedPane extends GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane {
        protected EmbeddedComponentRepaintManager repaintManager;
        public ColumnEmbeddedPane(GuiMappingContext context, GuiSwingView.SpecifierManager specifierManager,
                                  EmbeddedComponentRepaintManager repaintManager) {
            super(context, specifierManager);
            this.repaintManager = repaintManager;
            TextCellRenderer.setCellDefaultProperties(this);
        }

        @Override
        public void setSwingViewValueForTable(JTable table, Object value, int row, int column) {
            if (value instanceof JComponent) {
                repaintManager.putRow(table, (JComponent) value, row, column);
            }
            super.setSwingViewValueForTable(table, value, row, column);
        }

        @Override
        public void setSwingViewValueComponent(JComponent comp) {
            repaintManager.lockOrphanCandidates();
            try {
                if (component != null) {
                    repaintManager.addOrphanCandidate(component);
                }
                super.setSwingViewValueComponent(comp);
            } finally {
                repaintManager.unlockOrphanCandidates();
            }
        }
    }

    public static class ColumnEditEmbeddedPane extends GuiSwingViewEmbeddedComponent.PropertyEmbeddedPane {
        protected List<Runnable> finishRunners = new ArrayList<>(1);
        /** an intermediate layer between this and the value component:
         *    the purpose is making deeper component depth against changing a getParent result while painting of flatlaf UI impl. */
        protected EditWrapperPane wrapperPane;
        protected EmbeddedComponentRepaintManager repaintManager;
        public ColumnEditEmbeddedPane(GuiMappingContext context, EmbeddedComponentRepaintManager repaintManager, GuiSwingView.SpecifierManager specifierManager) {
            super(context, specifierManager);
            this.repaintManager = repaintManager;
            TextCellRenderer.setCellDefaultProperties(this);
            ObjectTableColumnValue.KeyHandlerFinishEditing.installFinishEditingKeyHandler(this, finishRunners);
            wrapperPane = new EditWrapperPane();
            add(wrapperPane, 0);
        }

        @Override
        public void addSwingEditFinishHandler(Runnable eventHandler) {
            finishRunners.add(eventHandler);
            super.addSwingEditFinishHandler(eventHandler);
        }

        @Override
        public void setSwingViewValueComponent(JComponent comp) {
            if (component == comp) {
                return;
            }
            repaintManager.lockOrphanCandidates();
            try {
                for (Component existing : wrapperPane.getComponents()) {
                    if (existing != comp && existing instanceof JComponent) {
                        repaintManager.addOrphanCandidate((JComponent) existing);
                    }
                }
                wrapperPane.removeAll();
                if (comp != null) {
                    wrapperPane.add(comp);
                    consumeInitPrefsJson(comp);
                }
                component = comp;
            } finally {
                repaintManager.unlockOrphanCandidates();
            }
        }

        @Override
        public Object getSwingViewValue() {
            return component;
        }

        @Override
        public void updateFromGui(Object v, GuiTaskClock viewClock) {}
    }

    public static class EditWrapperPane extends JPanel {
        public EditWrapperPane() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder());
            setOpaque(false);
        }

        public Component getRowComponent() {
            if (getComponentCount() == 1) {
                return getComponent(0);
            } else {
                return null;
            }
        }
    }

    /**
     * maintaining mapping between a row-component and row-column-indices for repainting.
     * It keeps any row-components always have a parent component linking to the manager,
     *   and invalidate() to a row-component enables cause repainting via the manager.
     */
    public static class EmbeddedComponentRepaintManager {
        /** maintains row and column indices for rowComponents: {row, column} and might be -1 if a rowComponent is shared to multiple cells.
         */
        protected WeakHashMap<JComponent, int[]> indexMap = new WeakHashMap<>();
        protected EmbeddedComponentOrphanPane orphanParent;
        protected JTable table;
        protected static int[] emptyRowAndColumn = new int[] {-1, -1};
        protected AtomicBoolean orphanLock = new AtomicBoolean();
        protected List<JComponent> orphanCandidates = new ArrayList<>();

        public EmbeddedComponentRepaintManager() {
            orphanParent = new EmbeddedComponentOrphanPane(this);
            orphanParent.setLayout(null);
        }

        public void clearRows() {
            indexMap.clear();
        }

        public void putRow(JTable table, JComponent rowComponent, int row, int column) {
            this.table = table;
            int modelRow = table.convertRowIndexToModel(row);
            int modelCol = table.convertColumnIndexToModel(column);
            int[] rowAndCol = indexMap.get(rowComponent);
            if (rowAndCol == null) {
                if (rowComponent != null) {
                    installListener(rowComponent);
                }
                indexMap.put(rowComponent, new int[] {modelRow, modelCol});
            } else if (rowAndCol[0] != modelRow || rowAndCol[1] != modelCol) {
                indexMap.put(rowComponent, emptyRowAndColumn);
            }
        }

        public void installListener(JComponent rowComponent) {
            rowComponent.addHierarchyListener(new EmbeddedComponentHierarchyMaintainer(this, rowComponent));
        }

        public void lockOrphanCandidates() {
            orphanLock.set(true);
        }

        public void unlockOrphanCandidates() {
            orphanLock.set(false);
            for (JComponent orphan : orphanCandidates) {
                if (orphan.getParent() == null) {
                    orphanParent.add(orphan);
                }
            }
            orphanCandidates.clear();
        }

        public void addOrphanCandidate(JComponent rowComponent) {
            if (orphanLock.get()) {
                orphanCandidates.add(rowComponent);
            } else {
                orphanParent.add(rowComponent);
            }
        }

        public int[] getRow(JComponent rowComponent) {
            if (rowComponent == null) {
                return emptyRowAndColumn;
            } else {
                return indexMap.getOrDefault(rowComponent, emptyRowAndColumn);
            }
        }

        public void repaint(JComponent rowComponent) {
            if (table != null) {
                int[] rowAndCol = getRow(rowComponent);
                if (rowAndCol[0] == -1 || rowAndCol[1] == -1) {
                    table.repaint();
                } else {
                    Rectangle rect = table.getCellRect(table.convertRowIndexToView(rowAndCol[0]), table.convertColumnIndexToView(rowAndCol[1]), false);
                    table.repaint(rect);
                }
            }
        }
    }

    public static class EmbeddedComponentHierarchyMaintainer implements HierarchyListener {
        protected EmbeddedComponentRepaintManager manager;
        protected JComponent component;

        public EmbeddedComponentHierarchyMaintainer(EmbeddedComponentRepaintManager manager, JComponent component) {
            this.manager = manager;
            this.component = component;
        }

        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            if (e.getID() == HierarchyEvent.HIERARCHY_CHANGED &&
                    ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) &&
                    component.getParent() == null) {
                manager.addOrphanCandidate(component);
            }
        }
    }

    public static class EmbeddedComponentOrphanPane extends JPanel {
        protected EmbeddedComponentRepaintManager repaintManager;
        public EmbeddedComponentOrphanPane(EmbeddedComponentRepaintManager repaintManager) {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder());
            setOpaque(false);
            this.repaintManager = repaintManager;
        }

        public JComponent getRowComponent() {
            if (getComponentCount() != 1) {
                return null;
            } else {
                return (JComponent) getComponent(0);
            }
        }

        @Override
        public void repaint(long tm, int x, int y, int width, int height) {
            super.repaint(tm, x, y, width, height);
            if (repaintManager != null) {
                repaintManager.repaint(getRowComponent());
            }
        }
    }
}
