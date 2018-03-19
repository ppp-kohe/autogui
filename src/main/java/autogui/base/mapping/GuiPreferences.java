package autogui.base.mapping;

import autogui.base.JsonReader;
import autogui.base.JsonWriter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/** the Preferences holder associated to a {@link GuiMappingContext}.
 *     Each child is also held by {@link GuiMappingContext}.
 *
 *   The class has a {@link GuiValueStore}.
 *   It also has a list of history values.
 *
 *   <pre>
 *        pack/Type/  //root-context, in a case of {@link GuiReprObjectPane}(pack.Type)
 *                    //otherwise, "autogui/base/mapping/pack_Type"
 *            "$default"/
 *               propName = value... //regular preferences entries
 *               ...
 *               "$history"/
 *                  "0"/    //key-index
 *                     "index" = ...
 *                     "value" = ...
 *                     "time"  = ... //{@link Instant#toString()}
 *                  ...
 *               propertyName/   //the name of a sub-context
 *                  ...
 *            "$saved"/
 *               "$0"/
 *                 "name" = ...
 *                 ... //same structure as "$default"
 *               ...
 *   </pre>
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

    protected void setChild(GuiMappingContext context, GuiPreferences child) {
        if (children == null) {
            children = new HashMap<>();
        }
        children.put(context, child);
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

    /**
     * @return Preferences based value-store.
     *    if it has parent, node for context's name of the parent,
     *    otherwise, node for context's object-type (user-node "/pack/ObjType") or
     *               node for (pack of this type)+"/"+(context's name).
     */
    public GuiValueStore getValueStore() {
        if (valueStore == null) {
            if (parent != null) {
                valueStore = parent.getValueStore().getChild(this);
            } else {
                valueStore = getValueStoreRootFromRepresentation();
            }
        }
        return valueStore;
    }

    public GuiValueStore getValueStoreRootFromRepresentation() {
        return new GuiValueStoreDefault(this, getPreferencesNodeAsRoot(), "$default");
    }

    public Preferences getPreferencesNodeAsRoot() {
        GuiRepresentation repr = context.getRepresentation();
        Preferences node;
        if (repr instanceof GuiReprObjectPane) {
            Class<?> type = ((GuiReprObjectPane) repr).getValueType(context);
            node = Preferences.userNodeForPackage(type).node(type.getSimpleName());
        } else {
            node = Preferences.userNodeForPackage(GuiPreferences.class)
                    .node(context.getName().replace('.', '_'));
        }
        return node;
    }

    /**
     * <pre>
     *            "$default/"
     *               ...
     *            "$saved/"
     *               "$0/"
     *                  "$name" = "prefs yyyy/mm/dd hh:mm"
     *                  ...
     *               "$1/"
     *               ...
     * </pre>
     * @return saved preferences
     */
    public List<GuiPreferences> getSavedStoreListAsRoot() {
        Preferences prefs = getPreferencesNodeAsRoot();
        try {
            List<GuiPreferences> savedList = new ArrayList<>();
            if (prefs.nodeExists("$saved")) {
                Preferences savedNode = prefs.node("$saved");
                List<String> stores = Arrays.asList(savedNode.childrenNames());

                for (String key : stores) {
                    GuiPreferences savedPrefs = new GuiPreferences(context);
                    savedPrefs.valueStore = new GuiValueStoreDefault(savedPrefs, savedNode, key);
                    savedList.add(savedPrefs);
                }
            }
            return savedList;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public GuiPreferences addNewSavedStoreAsRoot() {
        Preferences prefs = getPreferencesNodeAsRoot();
        Preferences savedNode = prefs.node("$saved");
        try {
            List<String> stores = Arrays.asList(savedNode.childrenNames());
            int n = stores.size();
            String key = "$" + Integer.toString(n);
            while (stores.contains(key)) {
                n++;
                key = "$" + Integer.toString(n);
            }
            GuiPreferences newPrefs = new GuiPreferences(context);
            newPrefs.valueStore = new GuiValueStoreDefault(newPrefs, savedNode, key);

            String name = context.getName();

            newPrefs.valueStore.putString("$name",
                    String.format("%s %d - %s", name, (n + 1), LocalDateTime.now().format(formatter)));

            return newPrefs;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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
        if (!historyValues.contains(e)) {
            historyValues.add(e);
        }

        historyValues.sort(Comparator.comparing(HistoryValueEntry::getIndex));

        while (historyValues.size() > historyValueLimit) {
            historyValues
                    .remove(0)
                    .remove();
        }

        for (int i = 0, l = historyValues.size(); i < l; ++i) {
            e = historyValues.get(i);
            if (e.getKeyIndex() == -1) { //a new entry will have key=-1 and then the value will be stored
                e.setKeyIndexWithLoadOrStore(getHistoryValueUnusedKeyIndex());
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
        HistoryValueEntry e = createHistoryValueEntry(value);
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
        HistoryValueEntry e = historyValues.size() > historyValueLimit ?
                historyValues.stream()
                    .min(Comparator.comparing(HistoryValueEntry::getIndex))
                    .orElse(optionalDefault) :
                optionalDefault;
        if (e != null) {
            e.setValue(v);
            e.setIndex(-1);
            e.setTime(optionalDefault.getTime());
        }
        return e;
    }

    /**
     * load or create historyValues: it's size becomes up to historyValueLimit and sorted by index.
     * The new entries have -1 indexes.
     */
    public void loadHistoryValues() {
        historyValues = new ArrayList<>();
        for (int i = 0; i < historyValueLimit; ++i) {
            HistoryValueEntry e = createHistoryValueEntry(null);
            try {
                e.setKeyIndexWithLoadOrStore(i);
                if (e.getIndex() != -1) {
                    historyValues.add(e);
                }
            } catch (Exception ex) {
                //failed to load the entry
            }
        }

        historyValues.sort(Comparator.comparing(HistoryValueEntry::getIndex));
    }

    public HistoryValueEntry createHistoryValueEntry(Object value) {
        if (context.isHistoryValueStored()) {
            return new HistoryValueEntry(this, value);
        } else {
            return new HistoryValueEntryOnMemory(this, value);
        }
    }

    /** after calling the method, the preferences will be invalidated */
    public void clearAll() {
        if (context.getPreferences().equals(this)) { //current context: recursively clear the histories
            clearHistoriesTree();
        }
        getValueStore().removeThisNode();
    }

    public void clearHistoriesTree() {
        clearHistories();
        for (GuiMappingContext subContext : context.getChildren()) {
            subContext.getPreferences().clearHistoriesTree();
        }
        context.clearPreferences();
    }

    public void clearHistories() {
        if (historyValues == null) {
            loadHistoryValues();
        }
        historyValues.forEach(HistoryValueEntry::remove);
        historyValues = null;
    }

    /** the abstract definition of key-value store.
     *
     *  <ul>
     *      <li>a key may be associated with a child node or an entry, and it shares the name space.</li>
     *      <li>an entry can be obtained as a String or a Integer</li>
     *      <li>an Integer entry can be obtained as a String entry</li>
     *      <li>a node is associated with a {@link GuiPreferences}, which might be the preferences of the store
     *            or a sub-preferences.</li>
     *  </ul>
     *
     * */
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
        public abstract GuiValueStore getChild(String key);

        public abstract boolean hasEntryKey(String key);
        public abstract boolean hasNodeKey(String key);
        public abstract List<String> getKeys();

        public abstract void remove(String key);

        /**
         * erase the node, but it might not remove the node from the parent
         */
        public abstract void removeThisNode();
        public void flush() { }
    }

    /** the concrete implementation of the store by {@link Preferences} */
    public static class GuiValueStoreDefault extends GuiValueStore {
        /* note: in macOS, ~/Library/Preferences/com.apple.java.util.prefs.plist ? */
        protected Preferences store;

        protected Preferences parentStore;
        protected String storeName;

        public GuiValueStoreDefault(GuiPreferences preferences, Preferences store) {
            super(preferences);
            this.store = store;
        }

        public GuiValueStoreDefault(GuiPreferences preferences, Preferences parentStore, String storeName) {
            super(preferences);
            this.parentStore = parentStore;
            this.storeName = storeName;
            try {
                if (parentStore.nodeExists(storeName)) {
                    store = parentStore.node(storeName);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        public Preferences getStore() {
            return store;
        }

        public Preferences getOrCreateStore() {
            if (store == null) {
                store = parentStore.node(storeName);
            }
            return store;
        }

        @Override
        public void putString(String key, String val) {
            getOrCreateStore().put(key, val);
        }

        @Override
        public String getString(String key, String def) {
            return store == null ? def : store.get(key, def);
        }

        @Override
        public void putInt(String key, int val) {
            getOrCreateStore().putInt(key, val);
        }

        @Override
        public int getInt(String key, int def) {
            return store == null ? def : store.getInt(key, def);
        }

        @Override
        public GuiValueStore getChild(GuiPreferences preferences) {
            return new GuiValueStoreDefault(preferences, getOrCreateStore(), preferences.getName());
        }

        @Override
        public GuiValueStore getChild(String name) {
            return new GuiValueStoreDefault(preferences, getOrCreateStore(), name);
        }

        @Override
        public boolean hasEntryKey(String key) {
            try {
                return Arrays.asList(getOrCreateStore().keys()).contains(key);
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public boolean hasNodeKey(String key) {
            try {
                return getOrCreateStore().nodeExists(key);
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public List<String> getKeys() {
            try {
                List<String> keys = new ArrayList<>(Arrays.asList(getOrCreateStore().keys()));
                keys.addAll(Arrays.asList(getOrCreateStore().childrenNames()));
                return keys;
            } catch (Exception ex) {
                return Collections.emptyList();
            }
        }

        @Override
        public void remove(String key) {
            if (hasEntryKey(key)) {
                getOrCreateStore().remove(key);
            } else if (hasNodeKey(key)) {
                try {
                    getOrCreateStore().node(key).removeNode();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        @Override
        public void removeThisNode() {
            try {
                getOrCreateStore().removeNode();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void flush() {
            try {
                if (store != null) {
                    store.flush();
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * an entry of the value history, consisting of a value, an index and a time-stamp, associated with a key-index.
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
        public void setKeyIndexWithLoadOrStore(int keyIndex) {
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
            if (this.index != index && keyIndex != -1) {
                //update index
                GuiValueStore store = getValueStore();
                store.putInt("index", index);
            }
            this.index = index;
        }
    }

    /**
     * an on-memory impl. of value history entry, created when the repr. is {@link GuiReprValue#isHistoryValueStored()}.
     */
    public static class HistoryValueEntryOnMemory extends HistoryValueEntry {
        public HistoryValueEntryOnMemory(GuiPreferences preferences, Object value) {
            super(preferences, value);
        }

        @Override
        public void load() { }

        @Override
        public void store() { }

        @Override
        public void remove() {
            this.keyIndex = -1;
            this.value = null;
        }

        @Override
        public void setIndex(int index) {
            this.index = index;
        }

        @Override
        public GuiValueStore getValueStore() {
            return null;
        }
    }

    //////////////////

    public Map<String,Object> toJson() {
        return ((GuiValueStoreOnMemory) copyOnMemoryAsRoot()
                .getValueStore())
                .toJson();
    }

    public GuiPreferences copyOnMemoryAsRoot() {
        return copyOnMemory(null);
    }

    /**
     * @param parent the parent which is already an on-memory instance or null
     * @return the copied preferences whose value-store is a {@link GuiValueStoreOnMemory}
     */
    public GuiPreferences copyOnMemory(GuiPreferences parent) {
        GuiPreferences src = this;
        GuiPreferences prefs = new GuiPreferences(parent, src.context);
        prefs.valueStore = copyOnMemoryStore(src, prefs, src.getValueStore(), true);
        for (GuiMappingContext subContext : src.getContext().getChildren()) {
            GuiPreferences subPrefs = src.getChild(subContext).copyOnMemory(prefs);
            prefs.setChild(subContext, subPrefs);
        }
        return prefs;
    }

    public static GuiValueStore copyOnMemoryStore(GuiPreferences src, GuiPreferences prefs, GuiValueStore store, boolean putToParent) {
        GuiValueStoreOnMemory copyStore = new GuiValueStoreOnMemory(prefs);
        if (putToParent && prefs.getParent() != null) {
            GuiValueStore parentStore = prefs.getParent().getValueStore();
            if (parentStore instanceof GuiValueStoreOnMemory) {
                ((GuiValueStoreOnMemory) parentStore).putChild(prefs.getName(), copyStore);
            }
        }
        Set<String> subNodeNames = src.getContext().getChildren().stream()
                .map(GuiMappingContext::getName)
                .collect(Collectors.toSet());
        for (String key : store.getKeys()) {
            if (store.hasEntryKey(key)) {
                copyStore.putString(key, store.getString(key, ""));
            } else if (store.hasNodeKey(key) &&
                    !subNodeNames.contains(key)) { //only copies nodes for "this"
                GuiValueStore child = store.getChild(key);
                GuiValueStore childCopy = copyOnMemoryStore(src, prefs, child, false);
                copyStore.putChild(key, childCopy);
            }
        }
        return copyStore;
    }

    @SuppressWarnings("unchecked")
    public void fromJson(Map<String,Object> json) {
        GuiPreferences src = this;

        Map<String,Object> remainingNodes = new LinkedHashMap<>(json);

        for (GuiMappingContext subContext : src.getContext().getChildren()) {
            GuiPreferences subPrefs = src.getChild(subContext);
            String key = subPrefs.getName();
            Object v = json.get(key);
            if (v instanceof Map<?,?>) {
                subPrefs.fromJson((Map<String,Object>) v);
                remainingNodes.remove(key);
            }
        }
        //non-sub-prefs nodes
        src.fromJsonChildNodes(src.getValueStore(), remainingNodes);
    }

    @SuppressWarnings("unchecked")
    protected void fromJsonChildNodes(GuiValueStore store, Map<String,Object> json) {
        for (Map.Entry<String,Object> e : json.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            if (val instanceof String) {
                store.putString(key, (String) val);
            } else if (val instanceof Map<?,?>) {
                fromJsonChildNodes(store.getChild(key), (Map<String, Object>) val);
            }
        }
    }

    /**
     * an on-memory impl. of value-store
     */
    public static class GuiValueStoreOnMemory extends GuiValueStore {
        protected Map<String,Object> values;

        public GuiValueStoreOnMemory(GuiPreferences preferences) {
            super(preferences);
            values = new LinkedHashMap<>();
        }

        @Override
        public void putString(String key, String val) {
            values.put(key, val);
        }

        @Override
        public String getString(String key, String def) {
            try {
                return (String) values.getOrDefault(key, def);
            } catch (Exception ex) {
                return def;
            }
        }

        @Override
        public void putInt(String key, int val) {
            values.put(key, Integer.toString(val));
        }

        @Override
        public int getInt(String key, int def) {
            try {
                return Integer.valueOf((String) values.getOrDefault(key, Integer.toString(def)));
            } catch (Exception ex) {
                return def;
            }
        }

        @Override
        public GuiValueStore getChild(GuiPreferences preferences) {
            return (GuiValueStore) values.computeIfAbsent(preferences.getName(),
                    k -> new GuiValueStoreOnMemory(preferences));
        }

        @Override
        public GuiValueStore getChild(String name) {
            return (GuiValueStore) values.computeIfAbsent(name,
                    k -> new GuiValueStoreOnMemory(preferences));
        }

        @Override
        public List<String> getKeys() {
            return new ArrayList<>(values.keySet());
        }

        @Override
        public boolean hasEntryKey(String key) {
            Object v = values.get(key);
            return v != null && !(v instanceof GuiValueStore);
        }

        @Override
        public boolean hasNodeKey(String key) {
            Object v = values.get(key);
            return v != null && v instanceof GuiValueStore;
        }

        public void putChild(String name, GuiValueStore value) {
            values.put(name, value);
        }

        public Map<String,Object> toJson() {
            LinkedHashMap<String,Object> map = new LinkedHashMap<>();
            for (Map.Entry<String,Object> e : values.entrySet()) {
                Object v = e.getValue();
                if (v instanceof GuiValueStoreOnMemory) {
                    map.put(e.getKey(), ((GuiValueStoreOnMemory) v).toJson());
                } else {
                    map.put(e.getKey(), v);
                }
            }
            return map;
        }

        @Override
        public void remove(String key) {
            values.remove(key);
        }

        @Override
        public void removeThisNode() {
            values.clear();
        }
    }
}
