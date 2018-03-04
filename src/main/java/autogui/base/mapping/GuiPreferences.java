package autogui.base.mapping;

import autogui.base.JsonReader;
import autogui.base.JsonWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

/** the Preferences holder associated to a GuiMappingContext.
 *    The class holds its parent, but does not hold children.
 *     Each child is held by GuiMappingContext.
 *
 *   It also has a list of history values.
 * */
public class GuiPreferences {
    protected GuiMappingContext context;
    protected GuiPreferences parent;
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

    /**
     * @param context the parent
     * @return a new instance for context
     */
    public GuiPreferences getChild(GuiMappingContext context) {
        return new GuiPreferences(this, context);
    }

    public String getName() {
        return context.getName();
    }

    public GuiMappingContext getContext() {
        return context;
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

    public void addHistoryValue(Object value) {
        if (historyValues == null) {
            loadHistoryValues();
        }
        HistoryValueEntry e = getHistoryValue(value);
        if (e == null) {
            return; //value is null or cannot convert to json
        } else if (e.getKeyIndex() != -1) {
            historyValues.remove(e);
        }
        historyValues.add(0, e);
        while (historyValues.size() > historyValueLimit) {
            historyValues
                    .remove(historyValues.size() - 1)
                    .remove();
        }

        for (int i = 0, l = historyValues.size(); i < l; ++i) {
            e = historyValues.get(i);
            if (e.getKeyIndex() == -1) {
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

    public void loadHistoryValues() {
        historyValues = new ArrayList<>();
        for (int i = 0; i < historyValueLimit; ++i) {
            HistoryValueEntry e = new HistoryValueEntry(this, null);
            e.setKeyIndex(i);

            historyValues.add(e);
        }
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

    /* note: in macOS, ~/Library/Preferences/com.apple.java.util.prefs.plist ? */
    public static class GuiValueStoreDefault extends GuiValueStore {
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

    public static class HistoryValueEntry {
        protected GuiPreferences preferences;
        protected Object value;
        protected int keyIndex = -1;
        protected int index = -1;
        protected GuiValueStore valueStore;

        public HistoryValueEntry(GuiPreferences preferences, Object value) {
            this.preferences = preferences;
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

        public int getKeyIndex() {
            return keyIndex;
        }

        public int getIndex() {
            return index;
        }

        public void remove() {
            if (this.keyIndex != -1) {
                GuiValueStore store = getValueStore();
                store.putInt("index", -1);
                store.putString("value", "null");
            }
            this.keyIndex = -1;
        }

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
            }
        }

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
        }

        public GuiValueStore getValueStore() {
            if (valueStore == null) {
                valueStore = preferences.getValueStore()
                        .getChild("$")
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
