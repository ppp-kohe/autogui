package org.autogui.swing.log;

import org.autogui.base.log.GuiLogEntry;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
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

    /**
     * obtains cached layout-size or computes it by the size.
     *  the default impl. is just always computing as no caching.
     *  available by
     *  <pre>
     *      SizeCache c = new SizeCache(2);
     *      float[] sizeCache(Object k, Supplier src) {
     *          return c.computeIfAbsent(k, _k -&gt; src.get());
     *      }
     *  </pre>
     * @param renderer the key for the size
     * @param src the task for computing the size
     * @return the layout-size, {w,h}
     * @since 1.5
     */
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

    /**
     * the light-weight map between Object to float[].
     * it supposes the table size is small.
     * @since 1.5
     */
    class SizeCache {
        private Object[] keys;
        private float[][] values;
        public SizeCache(int capacity) {
            keys = new Object[capacity];
            values = new float[capacity][];
        }

        public float[] computeIfAbsent(Object key, Function<Object, float[]> keyToValue) {
            int len = keys.length;
            for (int i = 0; i < len; ++i) {
                Object ek = keys[i];
                if (ek == null) {
                    return putAt(key, keyToValue, i);
                } else if (ek == key) {
                    return values[i];
                }
            }
            keys = Arrays.copyOf(keys, len * 2);
            values = Arrays.copyOf(values, len * 2);
            return putAt(key, keyToValue, len);
        }

        private float[] putAt(Object key, Function<Object, float[]> keyToValue, int i) {
            keys[i] = key;
            float[] v = keyToValue.apply(key);
            values[i] = v;
            return v;
        }
    }
}
