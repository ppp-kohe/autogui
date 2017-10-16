package autogui.base.mapping;

import autogui.base.JsonReader;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

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
        HistoryValueEntry e = findHistoryValue(value);
        if (e != null) {
            historyValues.remove(e);
        } else {
            e = new HistoryValueEntry(this, value);
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

    public HistoryValueEntry findHistoryValue(Object value) {
        return historyValues.stream()
                .filter(entry -> entry.match(value))
                .findFirst()
                .orElse(null);
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
            this.value = value;
        }

        public boolean match(Object v) {
            return value.equals(v);
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
                //load
                this.keyIndex = keyIndex;
                GuiValueStore store = getValueStore();
                int index = store.getInt("index", -1);
                if (index != -1) {
                    this.index = index;
                    String v = store.getString("value", "null");
                    Object jsonValue = JsonReader.create(v).parseValue();
                    if (jsonValue != null) {
                        this.value = preferences.getContext().getRepresentation()
                                .fromJson(preferences.getContext(), jsonValue);
                    }
                }
            }
            this.keyIndex = keyIndex;
        }

        public GuiValueStore getValueStore() {
            if (valueStore != null) {
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
