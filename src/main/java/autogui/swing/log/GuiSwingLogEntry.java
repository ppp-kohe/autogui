package autogui.swing.log;

import autogui.base.log.GuiLogEntry;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public interface GuiSwingLogEntry extends GuiLogEntry {

    default Object getRendererKey() {
        return getClass();
    }

    /** factory method of renderer */
    LogEntryRenderer getRenderer(GuiSwingLogManager manager, ContainerType type);

    enum ContainerType {
        List,
        StatusBar
    }

    interface LogEntryRenderer {
        ListCellRenderer<GuiLogEntry> getTableCellRenderer();

        default void mousePressed(GuiSwingLogEntry entry, Point point) {}
        default void mouseDragged(GuiSwingLogEntry entry, Point point) {}
        default void mouseReleased(GuiSwingLogEntry entry, Point point) {}
    }
}
