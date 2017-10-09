package autogui.swing.log;

import autogui.base.log.*;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class GuiSwingLogManager extends GuiLogManager {
    protected List<Consumer<GuiLogEntry>> views = new ArrayList<>();

    /** view might accept same entries */
    public void addView(Consumer<GuiLogEntry> view) {
        views.add(view);
    }

    @Override
    public GuiLogEntryString logString(String str) {
        GuiLogEntryString e = new GuiSwingLogEntryString(str);
        show(e);
        return e;
    }

    @Override
    public GuiLogEntryException logError(Throwable ex) {
        //TODO
        return super.logError(ex);
    }

    @Override
    public GuiLogEntryProgress logProgress() {
        GuiSwingLogEntryProgress p = new GuiSwingLogEntryProgress();
        p.addListener(this::updateProgress);
        show(p);
        return p;
    }

    @Override
    public void updateProgress(GuiLogEntryProgress p) {
        show(p);
    }

    public void show(GuiLogEntry e) {
        views.forEach(v -> v.accept(e));
    }

    public static Font getFont() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return new Font("Menlo", Font.PLAIN, 14);
        } else {
            return new Font(Font.MONOSPACED, Font.PLAIN, 12);
        }
    }

    public static class GuiSwingLogRenderer implements TableCellRenderer, ListCellRenderer<GuiLogEntry> {
        protected GuiSwingLogManager manager;
        protected JLabel nullLabel;
        protected Map<Object, GuiSwingLogEntry.LogEntryRenderer> rendererMap = new HashMap<>();
        protected GuiSwingLogEntry.ContainerType containerType;

        public GuiSwingLogRenderer(GuiSwingLogManager manager, GuiSwingLogEntry.ContainerType type) {
            this.manager = manager;
            nullLabel = new JLabel("null");
            this.containerType = type;
        }

        public GuiSwingLogEntry.LogEntryRenderer getEntryRenderer(GuiSwingLogEntry e) {
            Object rendererKey = e.getRendererKey();
            if (rendererKey == null) {
                return e.getRenderer(manager, containerType);
            } else {
                return rendererMap.computeIfAbsent(rendererKey, k ->
                        e.getRenderer(manager, containerType));
            }
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends GuiLogEntry> list, GuiLogEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null && value instanceof GuiSwingLogEntry) {
                ListCellRenderer<GuiLogEntry> renderer = getEntryRenderer((GuiSwingLogEntry) value).getTableCellRenderer();
                if (renderer != null) {
                    return renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            }
            nullLabel.setText(Objects.toString(value));
            return nullLabel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            if (value != null && value instanceof GuiSwingLogEntry) {
                GuiSwingLogEntry e = (GuiSwingLogEntry) value ;
                TableCellRenderer renderer = (TableCellRenderer) getEntryRenderer(e).getTableCellRenderer();
                if (renderer != null) {
                    return renderer.getTableCellRendererComponent(table, e, isSelected, hasFocus, row, column);
                }
            }
            nullLabel.setText(Objects.toString(value));
            return nullLabel;
        }
    }
}
