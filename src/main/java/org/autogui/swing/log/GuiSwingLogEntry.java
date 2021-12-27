package org.autogui.swing.log;

import org.autogui.base.log.GuiLogEntry;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * a log-entry supporting GUI rendering, managed by {@link GuiSwingLogManager} */
public interface GuiSwingLogEntry extends GuiLogEntry {

    /**
     * for each key object (and also a container type),
     *  a renderer will be created by {@link #getRenderer(GuiSwingLogManager, ContainerType)}.
     *  by default, the key is {@link Object#getClass()}
     *  @return the key object
     */
    default Object getRendererKey() {
        return getClass();
    }

    /**
     * @param manager the manager
     * @param type rendering for a list or a status-bar
     *  @return factory method of renderer */
    LogEntryRenderer getRenderer(GuiSwingLogManager manager, ContainerType type);

    default float[] sizeCache(Object renderer, Supplier<float[]> src) {
        return src.get();
    }

    /** type of a rendering component */
    enum ContainerType {
        List,
        StatusBar
    }

    /**
     * the renderer interface
     */
    interface LogEntryRenderer {
        ListCellRenderer<GuiLogEntry> getTableCellRenderer();

        default void mousePressed(GuiSwingLogEntry entry, Point point) {}
        default void mouseDragged(GuiSwingLogEntry entry, Point point) {}
        default void mouseReleased(GuiSwingLogEntry entry, Point point) {}

        default boolean updateFindPattern(String findKeyword) {
            return false;
        }

        /**
         * @param entry searching the entry
         * @param findKeyword the key-word for searching
         * @return &gt;0 value if it has matched string
         */
        default int findText(GuiSwingLogEntry entry, String findKeyword) {
            return 0;
        }

        /**
         *   As a precondition, {@link #findText(GuiSwingLogEntry, String)} is called for the entry before.
         *   it focuses a next ( or previous if !forward) target if found, or null
         * @param entry the target entry
         * @param prevIndex might be different from the returned one for same renderer (and same entry)
         * @param forward forward=true or backward=false
         * @return an index object, or null if not found
         */
        default Object focusNextFound(GuiSwingLogEntry entry, Object prevIndex, boolean forward) {
            return null;
        }

        default String getSelectedText(GuiSwingLogEntry entry, boolean entireText) {
            return "";
        }

        default void close() {}
    }

    default void clearSelection() {
    }

    void setSelected(boolean selected);
    boolean isSelected();
}
