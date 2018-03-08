package autogui.base.mapping;

import autogui.base.JsonReader;
import autogui.base.JsonWriter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences;

/** the Preferences holder associated to a {@link GuiMappingContext}.
 *    The class holds its parent, but does not hold children.
 *     Each child is held by {@link GuiMappingContext}.
 *
 *   The class has a {@link GuiValueStore}.
 *   It also has a list of history values.
 * */
public class GuiPreferences {
    protected GuiMappingContext context;
    protected GuiPreferences parent;
    protected Map<GuiMappingContext, GuiPreferences> children;
    protected GuiValueStore valueStore;
    protected List<HistoryValueEntry> historyValues;
    protected int historyValueLimit = 10;

    public GuiPreferences(GuiMappingContext context) {
        this.context = context;
    }

    public GuiPreferences(GuiPreferences parent, GuiMappingContext context) {
        this.parent = parent;
        this.context = context;
    }

    public GuiPreferences(GuiValueStore valueStore, GuiMappingContext context) {
        this.valueStore = valueStore;
        this.context = context;
    }

    /**
     * @param context the parent
     * @return a child preference for context
     */
    public GuiPreferences getChild(GuiMappingContext context) {
        if (children == null) {
            children = new HashMap<>();
        }
        return children.computeIfAbsent(context,
                c -> new GuiPreferences(this, c));
    }

    public String getName() {
        return context.getName();
    }

    public GuiMappingContext getContext() {
        return context;
    }

    public GuiPreferences getParent() {
        return parent;
    }

    /**
     * @param descendantContext a descendant context of the context returned by {@link #getContext()},
     *                           can be {@link #getContext()} itself
     * @return a descendant preference associated with the descendantContext
     */
    public GuiPreferences getDescendant(GuiMappingContext descendantContext) {
        List<GuiMappingContext> path = new ArrayList<>();
        GuiMappingContext interContext = descendantContext;
        while (interContext != null && !interContext.equals(context)) {
            path.add(0, interContext);
            interContext = interContext.getParent();
        }
        GuiPreferences p = this;
        for (GuiMappingContext context : path) {
            p = p.getChild(context);
        }
        return p;
    }

    public GuiValueStore getValueStore() {
        if (valueStore == null) {
            if (parent != null) {
                valueStore = parent.getValueStore().getChild(this);
            } else {
                GuiRepresentation repr = context.getRepresentation();
                if (repr instanceof GuiReprValue) {
                    Class<?> type = ((GuiReprValue) repr).getValueType(context);
                    valueStore = new GuiValueStoreDefault(this,
                            Preferences.userNodeForPackage(type));
                }
            }
        }
        return valueStore;
    }

    /**
     * add a value to the history.
     * <ol>
     *  <li>it first loads the existing history if not yet loaded, by {@link #loadHistoryValues()}.</li>
     *  <li>
     *   it finds an existing entry or a create new entry by {@link #getHistoryValue(Object)}.
     *  </li>
     *  <li>
     *    if the entry is a new one (keyIndex==-1), call {@link #replaceMin(Object, HistoryValueEntry)}
     *      and obtain the oldest entry with replacing the value.
     *  </li>
     *  <li>
     *   The entry moves to the end of the history as the latest value,
     *    by setting the max index + 1 as it's index, which is a temporarly index.
     *    The entire history is sorted by the indexes.
     *  </li>
     *  <li>
     *   remove overflowed entries with calling {@link HistoryValueEntry#remove()}.
     *    (regularly, this step never happens.)
     *  </li>
     *  <li>set index and keyIndex (if keyIndex==-1) for all entries, and flush the store.</li>
     * </ol>
     * @param value the new entry value
     */
    public void addHistoryValue(Object value) {
        if (historyValues == null) {
            loadHistoryValues();
        }
        HistoryValueEntry e = getHistoryValue(value);
        if (e == null) {
            return; //value is null or cannot be converted to json
        }

        if (e.getKeyIndex() == -1) {
            e = replaceMin(e.getValue(), e);
        }

        int maxIndex = historyValues.stream()
                .mapToInt(HistoryValueEntry::getIndex)
                .max()
                .orElse(-1);
        e.setIndex(maxIndex + 1); //temporarily index

        historyValues.sort(Comparator.comparing(HistoryValueEntry::getIndex));

        while (historyValues.size() > historyValueLimit) {
            historyValues
                    .remove(0)
                    .remove();
        }

        for (int i = 0, l = historyValues.size(); i < l; ++i) {
            e = historyValues.get(i);
            if (e.getKeyIndex() == -1) { //a new entry will have key=-1 and then the value will be stored
                e.setKeyIndex(getHistoryValueUnusedKeyIndex());
            }
            e.setIndex(i);
        }
        getValueStore().flush();
    }

    public int getHistoryValueUnusedKeyIndex() {
        int[] keys = historyValues.stream()
                .mapToInt(HistoryValueEntry::getKeyIndex)
                .distinct()
                .sorted()
                .toArray();
        int keyIndex = 0;
        for (int i = 0; i < Integer.MAX_VALUE; ++i) {
            boolean free = true;
            for (; keyIndex < keys.length; ++keyIndex) {
                if (i == keys[keyIndex]) {
                    //used
                    free = false;
                    break;
                } else if (i < keys[keyIndex]) {
                    //free
                    break;
                }
            }
            if (free) {
                return i;
            }
        }
        return -1;
    }

    public List<HistoryValueEntry> getHistoryValues() {
        if (historyValues == null) {
            loadHistoryValues();
        }
        return historyValues;
    }

    /**
     * @param value the entry value
     * @return a new entry (keyIndex == -1) or an existing entry
     * */
    public HistoryValueEntry getHistoryValue(Object value) {
        HistoryValueEntry e = new HistoryValueEntry(this, value);
        Object v = e.getValue(); //it might be converted to JSON
        if (v == null) {
            return null;
        }
        return historyValues.stream()
                .filter(entry -> entry.match(v))
                .findFirst()
                .orElse(e);
    }

    public HistoryValueEntry replaceMin(Object v, HistoryValueEntry optionalDefault) {
        HistoryValueEntry e = historyValues.stream()
                .min(Comparator.comparing(HistoryValueEntry::getIndex))
                .orElse(optionalDefault);
        if (e != null) {
            e.setValue(v);
            e.setIndex(-1);
            e.setTime(optionalDefault.getTime());
        }
        return e;
    }

    /**
     * load or create historyValues: it's size becomes historyValueLimit and sorted by index.
     * The new entries have -1 indexes.
     */
    public void loadHistoryValues() {
        historyValues = new ArrayList<>();
        for (int i = 0; i < historyValueLimit; ++i) {
            HistoryValueEntry e = new HistoryValueEntry(this, null);
            e.setKeyIndex(i);
            historyValues.add(e);
        }

        historyValues.sort(Comparator.comparing(HistoryValueEntry::getIndex));
    }

    public abstract static class GuiValueStore {
        protected GuiPreferences preferences;

        public GuiValueStore(GuiPreferences preferences) {
            this.preferences = preferences;
        }

        public GuiPreferences getPreferences() {
            return preferences;
        }

        public abstract void putString(String key, String val);
        public abstract String getString(String key, String def);

        public abstract void putInt(String key, int val);
        public abstract int getInt(String key, int def);

        public abstract GuiValueStore getChild(GuiPreferences preferences);
        public abstract GuiValueStore getChild(String name);

        public void flush() { }
    }

    /** the concrete implementation of the store by {@link Preferences} */
    public static class GuiValueStoreDefault extends GuiValueStore {
        /* note: in macOS, ~/Library/Preferences/com.apple.java.util.prefs.plist ? */
        protected Preferences store;

        public GuiValueStoreDefault(GuiPreferences preferences, Preferences store) {
            super(preferences);
            this.store = store;
        }

        public Preferences getStore() {
            return store;
        }

        @Override
        public void putString(String key, String val) {
            store.put(key, val);
        }

        @Override
        public String getString(String key, String def) {
            return store.get(key, def);
        }

        @Override
        public void putInt(String key, int val) {
            store.putInt(key, val);
        }

        @Override
        public int getInt(String key, int def) {
            return store.getInt(key, def);
        }

        @Override
        public GuiValueStore getChild(GuiPreferences preferences) {
            return new GuiValueStoreDefault(preferences, store.node(preferences.getName()));
        }

        @Override
        public GuiValueStore getChild(String name) {
            return new GuiValueStoreDefault(preferences, store.node(name));
        }

        @Override
        public void flush() {
            try {
                store.flush();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * <pre>
     *     preferences/
     *        "$history"/
     *           keyIndex/    : valueStore
     *                "index" : int
     *                "value" : String
     *                "time"  : String //Instant
     *           ...
     * </pre>
     */
    public static class HistoryValueEntry {
        protected GuiPreferences preferences;
        protected Object value;
        protected int keyIndex = -1;
        protected int index = -1;
        protected GuiValueStore valueStore;
        protected Instant time;

        public HistoryValueEntry(GuiPreferences preferences, Object value) {
            this.preferences = preferences;
            this.time = Instant.now();
            if (isJsonValue() && value != null) {
                this.value = preferences.getContext().getRepresentation()
                        .toJsonWithNamed(preferences.getContext(), value);
            } else {
                this.value = value;
            }
        }

        public boolean match(Object v) {
            return Objects.equals(value, v);
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public int getKeyIndex() {
            return keyIndex;
        }

        public int getIndex() {
            return index;
        }

        public Instant getTime() {
            return time;
        }

        public void setTime(Instant time) {
            this.time = time;
        }

        public void remove() {
            if (this.keyIndex != -1) {
                GuiValueStore store = getValueStore();
                store.putInt("index", -1);
                store.putString("value", "null");
                store.putString("time", "null");
            }
            this.keyIndex = -1;
        }

        /**
         * if the keyIndex is not -1,
         *   it causes {@link #store()} (if value has been set) or {@link #load()} (not set).
         * @param keyIndex the key index
         */
        public void setKeyIndex(int keyIndex) {
            if (this.keyIndex == -1 && keyIndex != -1) {
                this.keyIndex = keyIndex;
                if (value != null) {
                    store();
                } else {
                    load();
                }
            }
            this.keyIndex = keyIndex;
        }

        /** load the "$history/index" value,
         *   and if the value is not -1, then also load "$history/value" and
         *   decode it as JSON and {@link GuiRepresentation#fromJson(GuiMappingContext, Object, Object)}.
         *  {@link #getValue()} will be null if it failed.
         *    */
        public void load() {
            GuiValueStore store = getValueStore();
            int index = store.getInt("index", -1);
            if (index != -1) {
                this.index = index;
                String v = store.getString("value", "null");
                Object jsonValue = JsonReader.create(v).parseValue();
                if (jsonValue != null) {
                    if (isJsonValue()) {
                        this.value = jsonValue;
                    } else {
                        this.value = preferences.getContext().getRepresentation()
                                .fromJson(preferences.getContext(), null, jsonValue);
                    }
                }

                String timeVal = store.getString("time", "null");
                Object timeJson = JsonReader.create(timeVal).parseValue();
                if (timeJson != null) {
                    try {
                        this.time = Instant.parse((String) timeJson);
                    } catch (Exception ex) {
                        //
                    }
                }
            }
        }

        /** @return if false, the value holds a raw-object which might be a non-JSON object,
         *        and then the associated representation can create a new value from a JSON source without any raw-object. */
        public boolean isJsonValue() {
            return preferences.getContext().getRepresentation().isJsonSetter();
        }

        public void store() {
            GuiValueStore store = getValueStore();
            Object json = isJsonValue() ? this.value :
                    preferences.getContext().getRepresentation()
                            .toJsonWithNamed(preferences.getContext(), this.value);
            String jsonSource = JsonWriter.create().write(json).toSource();
            store.putString("value", jsonSource);
            if (index != -1) {
                store.putInt("index", index);
            }
            store.putString("time", time.toString());
        }

        public GuiValueStore getValueStore() {
            if (valueStore == null) {
                valueStore = preferences.getValueStore()
                        .getChild("$history")
                        .getChild("" + keyIndex);
            }
            return valueStore;
        }

        public void setIndex(int index) {
            if (this.index != index) {
                //update index
                GuiValueStore store = getValueStore();
                store.putInt("index", index);
            }
            this.index = index;
        }
    }
}
