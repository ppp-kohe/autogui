package autogui.swing.log;

import autogui.base.log.GuiLogEntry;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public interface GuiSwingLogEntry extends GuiLogEntry {

    /** for each key object (and also a container type),
     *  a renderer will be created by {@link #getRenderer(GuiSwingLogManager, ContainerType)}.
     *  by default, the key is {@link #getClass()} */
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

        default boolean updateFindPattern(String findKeyword) {
            return false;
        }

        /** returns &gt;0 value if it has matched string */
        default int findText(GuiSwingLogEntry entry, String findKeyword) {
            return 0;
        }

        /** prevIndex might be different from the returned one for  same renderer (and same entry).
         *   As a precondition, findText(entry, text) is called for the entry before.
         *   it focuses a next ( or previous if !forward) target if found, or null */
        default Object focusNextFound(GuiSwingLogEntry entry, Object prevIndex, boolean forward) {
            return null;
        }

        default String getSelectedText(GuiSwingLogEntry entry, boolean entireText) {
            return "";
        }
    }

    default void clearSelection() {
    }

    void setSelected(boolean selected);
    boolean isSelected();
}
