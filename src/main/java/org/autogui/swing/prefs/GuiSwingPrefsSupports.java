package org.autogui.swing.prefs;

import org.autogui.base.JsonReader;
import org.autogui.base.JsonWriter;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.base.mapping.GuiPreferences;
import org.autogui.swing.util.SettingsWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GuiSwingPrefsSupports {
    protected GuiSwingPrefsSupports() {}

    /**
     * @param targetList the updated non-null list; cleared and set all elements from the map.
     * @param map the source map
     * @param type the element type
     * @param key the key
     * @param <E> the element type
     * @since 1.6.1
     */
    public static <E> void setAsList(List<E> targetList, Map<?, ?> map, Class<E> type, String key) {
        targetList.clear();
        targetList.addAll(getAsListNonNull(map, type, key));
    }

    /**
     * @param targetList the updated non-null list; cleared and set all elements from the map.
     * @param map the source map
     * @param type the element type of the JSON map
     * @param key   the key
     * @param mapper the mapper from a JSON element to an actual value in the list
     * @param <E> the element type in the JSON
     * @param <V> the actual element type of the trgetList
     */
    public static <E,V> void setAsList(List<V> targetList, Map<?, ?> map, Class<E> type, String key, Function<E, V> mapper) {
        targetList.clear();
        getAsListNonNull(map, type, key).stream()
                .map(mapper)
                .forEach(targetList::add);
    }

    /**
     * @param targetList the updated non-null list; cleared and set all elements from the list.
     * @param list the source list
     * @param type the element type of the JSON map
     * @param mapper the mapper from a JSON element to an actual value in the list
     * @param <E> the element type in the JSON
     * @param <V> the actual element type of the trgetList
     */
    public static <E,V> void setAsList(List<V> targetList, Object list, Class<E> type, Function<E, V> mapper) {
        targetList.clear();
        getAsListNonNull(list, type).stream()
                .map(mapper)
                .forEach(targetList::add);
    }

    /**
     * supporting method for setting JSON values
     * @param map the non-null map
     * @param type the element type
     * @param key the key for the map
     * @return a list of map.get(key) that satifies all elements are the type.
     * @param <E> the element type
     * @since 1.7
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> getAsListNonNull(Map<?, ?> map, Class<E> type, String key) {
        return getAsListNonNull(map.get(key), type);
    }

    /**
     * @param map the map for getting
     * @param type the type instance
     * @param key the map-key
     * @param defVal the default value if failure of type check
     * @return the non-null map.get(key) or defVal
     * @param <E> the type
     */
    public static <E> E getAs(Map<?, ?> map, Class<E> type, String key, E defVal) {
        var e = map.get(key);
        if (!type.isInstance(e)) {
            return defVal;
        } else {
            return type.cast(e);
        }
    }

    /**
     * supporting method for setting JSON values
     * @param list a list (nullable)
     * @param type the element type
     * @return the list that satifies all elements are the type, or a new empty list.
     * @param <E> the element type
     * @since 1.7
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> getAsListNonNull(Object list, Class<E> type) {
        if (list != null && list instanceof List<?> l) {
            if (!l.isEmpty() &&
                    l.stream().anyMatch(e -> !type.isInstance(e))) { //check type safety of all elements
                return new ArrayList<>();
            } else {
                return (List<E>) l;
            }
        } else {
            return new ArrayList<>();
        }
    }

    /** partial updater */
    public interface PreferencesUpdateSupport {
        void setPreferencesUpdater(Consumer<PreferencesUpdateEvent> updater);
    }

    public interface Preferences {
        void loadFrom(GuiPreferences prefs);

        void saveTo(GuiPreferences prefs);
    }

    public interface PreferencesByJsonEntry extends Preferences {
        String getKey();
        Object toJson();
        void setJson(Object json);

        default void loadFrom(GuiPreferences prefs) {
            setJson(JsonReader.create(prefs.getValueStore().getString(getKey(), "null"))
                    .parseValue());
        }

        default void saveTo(GuiPreferences prefs) {
            prefs.getValueStore().putString(getKey(),
                    JsonWriter.create().withNewLines(false).write(toJson()).toSource());
        }

        default Object loadFromAndToJson(GuiPreferences prefs) {
            loadFrom(prefs);
            return toJson();
        }

        default void setJsonAndSaveTo(Object json, GuiPreferences prefs) {
            setJson(json);
            saveTo(prefs);
        }
    }

    public static class PreferencesForWindow implements PreferencesByJsonEntry {
        protected int x;
        protected int y;
        protected int width;
        protected int height;
        protected String key = "$window";

        public PreferencesForWindow(String key) {
            this.key = key;
        }

        public PreferencesForWindow() {}

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public void applyTo(Window window) {
            applyTo(window, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
        }

        /**
         *
         * @param window the target window
         * @param options options for applying: only setting locations if {@link GuiSwingPrefsApplyOptions#isInit()}
         * @since 1.4
         */
        public void applyTo(Window window, GuiSwingPrefsApplyOptions options) {
            if (width > 0 && height > 0) {
                Dimension size = new Dimension(width, height);
                window.setSize(size);
            } else {
                window.pack();
            }
            if (x != 0 && y != 0 && options.isInit()) {
                //we handle (0,0) as the null value: for multi-monitors, negative values can be possible for locations
                Rectangle screen = getDeviceBoundsOrNull(x, y);
                if (screen != null) {
                    window.setLocation(new Point(x, y));
                } //otherwise, changed devices: nothing happen
            }
        }

        /**
         * @param x the absolute x location; 0 is the left of the main monitor
         * @param y the absolute y location; 0 is the top of the main monitor
         * @return rectangle for containing x,y
         */
        public static Rectangle getDeviceBoundsOrNull(int x, int y) {
            GraphicsDevice[] devs = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getScreenDevices();
            return Arrays.stream(devs)
                    .map(d -> d.getDefaultConfiguration().getBounds())
                    .filter(r -> r.contains(x, y))
                    .findFirst()
                    .orElse(null);
        }

        public void setSizeFrom(Window window) {
            Dimension size = window.getSize();
            width = size.width;
            height = size.height;
        }

        public void setLocationFrom(Window window) {
            Point p = window.getLocation();
            x = p.x;
            y = p.y;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("x", x);
            map.put("y", y);
            map.put("width", width);
            map.put("height", height);
            return map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?,?> map) {
                this.x = getAs(map, Integer.class, "x", 0);
                this.y = getAs(map, Integer.class, "y", 0);
                this.width = getAs(map, Integer.class, "width", 0);
                this.height = getAs(map, Integer.class, "height", 0);
            }
        }
    }

    public static class WindowPreferencesUpdater implements ComponentListener, SettingsWindow.SettingSupport {
        protected Window window;
        protected GuiMappingContext context;
        protected PreferencesForWindow prefs;
        protected Consumer<PreferencesUpdateEvent> updater;
        protected boolean savingDisabled = false;

        public WindowPreferencesUpdater(Window window, GuiMappingContext context) {
            this.window = window;
            this.context = context;
            prefs = new PreferencesForWindow();
        }

        public WindowPreferencesUpdater(Window window, GuiMappingContext context, String name) {
            this.window = window;
            this.context = context;
            prefs = new PreferencesForWindow(name);
        }

        public void setUpdater(Consumer<PreferencesUpdateEvent> updater) {
            this.updater = updater;
        }

        public PreferencesForWindow getPrefs() {
            return prefs;
        }

        @Override
        public void resized(JFrame window) {
            if (!savingDisabled) {
                prefs.setSizeFrom(window);
                sendToUpdater();
            }
        }

        @Override
        public void moved(JFrame window) {
            if (!savingDisabled) {
                prefs.setLocationFrom(window);
                sendToUpdater();
            }
        }

        @Override
        public void setup(JFrame window) {
            savingDisabled = true;
            try {
                prefs.applyTo(window); //prefs window always ignore window-locations
            } finally {
                savingDisabled = false;
            }
        }

        public void apply(GuiPreferences p) {
            apply(p, GuiSwingPrefsApplyOptions.APPLY_OPTIONS_DEFAULT);
        }

        /**
         * @param p the source prefs
         * @param options options for applying
         * @since 1.4
         */
        public void apply(GuiPreferences p, GuiSwingPrefsApplyOptions options) {
            savingDisabled = true;
            try {
                prefs.loadFrom(p);
                if (window != null) {
                    prefs.applyTo(window, options);
                }
            } finally {
                savingDisabled = false;
            }
        }

        public void sendToUpdater() {
            if (updater != null) {
                updater.accept(new PreferencesUpdateEvent(context, prefs));
            }
        }

        @Override
        public void componentResized(ComponentEvent e) {
            if (window != null && !savingDisabled) {
                prefs.setSizeFrom(window);
                sendToUpdater();
            }
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            if (window != null && !savingDisabled) {
                prefs.setLocationFrom(window);
                sendToUpdater();
            }
        }

        @Override
        public void componentShown(ComponentEvent e) { }

        @Override
        public void componentHidden(ComponentEvent e) { }
    }

    public static class PreferencesForFileDialog implements PreferencesByJsonEntry {
        protected java.util.List<String> fileList = new ArrayList<>();
        protected String backPath;
        protected String currentDirectory;
        protected String key = "$fileDialog";

        public PreferencesForFileDialog() {}

        public void setFileList(java.util.List<Path> fileList) {
            this.fileList = new ArrayList<>(fileList.stream()
                    .map(Path::toString)
                    .toList());
        }

        public void setBackPath(Path path) {
            this.backPath = (path == null ? null : path.toString());
        }

        public void setCurrentDirectory(Path path) {
            this.currentDirectory = (path == null ? null : path.toString());
        }

        public void applyTo(SettingsWindow.FileDialogManager dialogManager) {
            dialogManager.setFieList(fileList.stream()
                    .map(Paths::get)
                    .collect(Collectors.toList()));
            if (currentDirectory != null) {
                dialogManager.setCurrentPath(Paths.get(currentDirectory));
            }
            if (backPath != null) {
                dialogManager.setBackButtonPath(Paths.get(backPath));
            }
        }

        @Override
        public String getKey() {
            return key;
        }

        /**
         * @return the direct reference to the property fileList
         * @since 1.7
         */
        public List<String> getFileListDirect() {
            return fileList;
        }

        /**
         * @return the property value or null
         * @since 1.7
         */
        public Path getBackPath() {
            return backPath == null ? null : Paths.get(backPath);
        }

        /**
         * @return the property value or null
         * @since 1.7
         */
        public Path getCurrentDirectory() {
            return currentDirectory == null ? null : Paths.get(currentDirectory);
        }

        @Override
        public Object toJson() {
            Map<String,Object> map = new LinkedHashMap<>();
            map.put("fileList", fileList);
            map.put("currentDirectory", currentDirectory);
            map.put("backPath", backPath);
            return map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setJson(Object json) {
            if (json instanceof Map<?,?> map) {
                setAsList(fileList, map, String.class,"fileList");
                currentDirectory = getAs(map, String.class, "currentDirectory", null);
                backPath = getAs(map, String.class, "backPath", null);
            }
        }
    }

    public static class FileDialogPreferencesUpdater implements SettingsWindow.FileDialogManagerListener {
        protected SettingsWindow.FileDialogManager dialogManager;
        protected GuiMappingContext context;
        protected Consumer<PreferencesUpdateEvent> updater;
        protected PreferencesForFileDialog prefs;

        protected boolean savingDisabled;

        public FileDialogPreferencesUpdater(SettingsWindow.FileDialogManager dialogManager, GuiMappingContext context) {
            this.dialogManager = dialogManager;
            this.context = context;
            prefs = new PreferencesForFileDialog();
        }

        public void setUpdater(Consumer<PreferencesUpdateEvent> updater) {
            this.updater = updater;
        }

        public void addToDialogManager() {
            int len = GuiPreferences.getStoreValueMaxLength(); //8192
            int maxPathLength = 255 + 4;
            int jsonSymbols = 47;
            //suppose the max length of a path is 255 (actually it would be longer),
            //   then a path string becomes 255 + (2 of \" : 4), with a comma separator
            // (note: Windows path separators will be escaped)
            // JSON keys and symbols: {"fileList":[],"currentDirectory":,"backPath":} :47
            // 8192 = (n + 2) * 259 + (n - 1) + 47;  thus n = (8192 - 259 * 2 + 1 - 47) / 260 =~ 29
            int maxList = (len - maxPathLength * 2 + 1 - jsonSymbols) / (maxPathLength + 1);
            dialogManager.setMaxHistoryList(maxList);
            dialogManager.addListener(this);
        }

        public void removeFromDialogManager() {
            dialogManager.removeListener(this);
        }

        @Override
        public void updateFileList(SettingsWindow.FileListModel listModel) {
            if (!savingDisabled) {
                prefs.setFileList(listModel.getPaths());
                sendToUpdater();
            }
        }

        @Override
        public void updateCurrentDirectory(Path path) {
            if (!savingDisabled) {
                prefs.setCurrentDirectory(path);
                sendToUpdater();
            }
        }

        @Override
        public void updateBackButton(Path path) {
            if (!savingDisabled) {
                prefs.setBackPath(path);
                sendToUpdater();
            }
        }

        public void sendToUpdater() {
            if (updater != null) {
                updater.accept(new PreferencesUpdateEvent(context, prefs));
            }
        }

        public void apply(GuiPreferences p) {
            savingDisabled = true;
            try {
                prefs.loadFrom(p);
                prefs.applyTo(dialogManager);
            } finally {
                savingDisabled = false;
            }
        }

        public PreferencesForFileDialog getPrefs() {
            return prefs;
        }
    }

    public static class PreferencesUpdateEvent {
        protected GuiMappingContext context;
        protected Preferences prefs;

        public PreferencesUpdateEvent(GuiMappingContext context, Preferences prefs) {
            this.context = context;
            this.prefs = prefs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PreferencesUpdateEvent that = (PreferencesUpdateEvent) o;
            return Objects.equals(context, that.context) &&
                    Objects.equals(prefs, that.prefs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(context, prefs);
        }

        /**
         * @return Boolean.TRUE if  it has both context and prefs and {@link #save()}ed. otherwise returns Boolean.FALSE
         */
        public Object saveAndGetPostOperation() {
            if (context != null && prefs != null) {
                save();
                return true;
            } else {
                return false;
            }
        }

        public void save() {
            GuiPreferences cxtPrefs = context.getPreferences();
            try (var lock = cxtPrefs.lock()) {
                lock.use();
                prefs.saveTo(cxtPrefs);
            }
        }
    }
}
