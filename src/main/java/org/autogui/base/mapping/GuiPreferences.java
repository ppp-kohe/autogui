package org.autogui.base.mapping;

import org.autogui.base.JsonReader;
import org.autogui.base.JsonWriter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/** the Preferences holder associated to a {@link GuiMappingContext}.
 *     Each child is also held by {@link GuiMappingContext}.
 *   The class has a {@link GuiValueStore}.
 *   It also has a list of history values.
 *
 *   <pre>
 *        pack/Type/  //root-context, in a case of {@link GuiReprObjectPane}(pack.Type)
 *                    //otherwise, "org/autogui/base/mapping/pack_Type"
 *            "$default"/
 *               propName = value... //regular preferences entries
 *               ...
 *               "$value" = ... //current value, which will precede to history values
 *               "$history"/
 *                  "0"/    //key-index
 *                     "index" = ...
 *                     "value" = ...
 *                     "time"  = ... //{@link Instant#toString()}
 *                  ...
 *               propertyName/   //the name of a sub-context
 *                  ...
 *            "$launchPrefs" = "..." //value of "$uuid", "" or "empty"
 *            "$saved"/
 *               "$0"/
 *                 "$name" = ...
 *                 "$uuid" = "..." //a value from UUID.randomUUID()
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
    protected List<HistoryValueEntry> historyValuesFree;
    protected int historyValueLimit = 10;
    /** @since 1.6 */
    protected PreferencesLock lock;
    /** the key for the default {@link GuiPreferences} node under the root */
    public static final String KEY_DEFAULT = "$default";
    /** the key for {@link GuiPreferences#getCurrentValue()} */
    public static final String KEY_CURRENT_VALUE = "$value";
    /** the key for the string uuid refering a saved {@link GuiPreferences} under the root */
    public static final String KEY_LAUNCH_PREFS = "$launchPrefs";
    /** the key for saved node containing sub-nodes of {@link GuiPreferences} under the root */
    public static final String KEY_SAVED = "$saved";
    /** the key for the string name of {@link GuiPreferences} */
    public static final String KEY_NAME = "$name";
    /** the key for the string UUID of {@link GuiPreferences} */
    public static final String KEY_UUID = "$uuid";
    /** the key for the history node containing {@link HistoryValueEntry} under {@link GuiPreferences}*/
    public static final String KEY_HISTORY = "$history";
    /** the key for the int index of {@link HistoryValueEntry} */
    public static final String KEY_HISTORY_ENTRY_INDEX = "index";
    /** the key for the JSON string value of {@link HistoryValueEntry} */
    public static final String KEY_HISTORY_ENTRY_VALUE = "value";
    /** the key for the string {@link Instant} value of {@link HistoryValueEntry} */
    public static final String KEY_HISTORY_ENTRY_TIME = "time";

    /**
     * the interface for supporting user-defined prefs:
     *   a context-object that implements the interface always saves/loads prefs via the interface.
     *   Note: JSON objects returned by the method must be a JSON object ({@link Map} (String keyed), {@link List}, {@link String}, {@link Number} or {@link Boolean}),
     *    and the stringified data of those JSONs must be small for storing as a prefs entry.
     * @since 1.5
     */
    public interface PreferencesJsonSupport {
        Map<String,Object> getPrefsJson();
        void setPrefsJson(Map<String,Object> prefs);
    }

    /**
     * @return the type of the context of the prefs is {@link PreferencesJsonSupport}
     * @since 1.5
     */
    public boolean isContextTypeIsJsonSupport() {
        GuiMappingContext context = getContext();
        Class<?> cls = null;
        if (context.isTypeElementValue()) {
            cls = context.getTypeElementValueAsClass();
        } else if (context.isTypeElementProperty()) {
            cls = context.getTypeElementPropertyTypeAsClass();
        }
        return cls != null && PreferencesJsonSupport.class.isAssignableFrom(cls);
    }

    /**
     * a subclass of prefs for custom usage with supplying specific sub-paths.
     * @since 1.5
     */
    public static class GuiPreferencesWithPaths extends GuiPreferences {
        protected List<String> subPaths;
        public GuiPreferencesWithPaths(GuiMappingContext context, String... subPaths) {
            super(context);
            this.subPaths = Arrays.asList(subPaths);
        }

        @Override
        public GuiValueStore getPreferencesNodeAsRoot() {
            GuiValueStore store = super.getPreferencesNodeAsRoot();
            for (String subPath : subPaths) {
                store = store.getChild(subPath);
            }
            return store;
        }
    }


    /**
     * the entire lock object for the preferences
     * <pre>
     *     try (var lock = prefs.lock()) {
     *         ... //editing
     *     }
     * </pre>
     * @since 1.6
     */
    public static class PreferencesLock extends ReentrantLock implements AutoCloseable {
        public PreferencesLock() {}
        public PreferencesLock open() {
            lock();
            return this;
        }

        /**
         * empty operation for suppressing warnings of no resource usage
         */
        public void use() {}

        @Override
        public void close() {
            unlock();
        }
    }

    public GuiPreferences(GuiMappingContext context) {
        this.context = context;
    }

    public GuiPreferences(GuiPreferences parent, GuiMappingContext context) {
        this.parent = parent;
        this.context = context;
    }

    @SuppressWarnings("this-escape")
    public GuiPreferences(GuiValueStore valueStore, GuiMappingContext context) {
        this.valueStore = valueStore;
        if (valueStore != null) {
            valueStore.setPreferences(this);
        }
        this.context = context;
    }

    public GuiPreferences copyInitAsRoot() {
        return new GuiPreferences(valueStore == null ? null : (valueStore.copyInitAsRoot()), context);
    }

    public static int getStoreValueMaxLength() {
        return Preferences.MAX_VALUE_LENGTH;
    }

    public static String toStoreKey(String key) {
        int max = Math.min(Preferences.MAX_KEY_LENGTH, Preferences.MAX_NAME_LENGTH); //80
        if (key.length() > max - 8) {
            return key.substring(0, max - 8) + Integer.toHexString(key.hashCode()); //Java String hashCode is permanent
        } else {
            return key;
        }
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
            path.addFirst(interContext);
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
        GuiValueStore root = getPreferencesNodeAsRoot();
        return root.getChild(KEY_DEFAULT);
    }

    public GuiValueStore getPreferencesNodeAsRoot() {
        GuiRepresentation repr = context.getRepresentation();
        Preferences node;
        if (repr instanceof GuiReprObjectPane) {
            Class<?> type = ((GuiReprObjectPane) repr).getValueType(context);
            node = Preferences.userNodeForPackage(type).node(toStoreKey(type.getSimpleName()));
        } else {
            node = Preferences.userNodeForPackage(GuiPreferences.class)
                    .node(toStoreKey(context.getName().replace('.', '_')));
        }
        return new GuiValueStoreDefault(this, node);
    }

    public void resetAsRoot() {
        clearAll();
        GuiValueStore root = getPreferencesNodeAsRoot();
        root.removeThisNode();
        root.flush();
        valueStore = null;
        synchronized (this) {
            lock = null;
        }
    }

    /**
     * @return lock object with holding the lock
     * @since 1.6
     */
    public PreferencesLock lock() {
        GuiPreferences parent = this;
        while (parent.getParent() != null) {
            parent = parent.getParent();
        }
        synchronized (parent) {
            if (parent.lock == null) {
                parent.lock = new PreferencesLock();
            }
            return parent.lock.open();
        }
    }

    /**
     * <pre>
     *            "$default/"
     *               ...
     *            "$launchPrefs" = "..."
     *            "$saved/"
     *               "$0/"
     *                  "$name" = "prefs yyyy/mm/dd hh:mm"
     *                  "$uuid" = "..." //checked
     *                  ...
     *               "$1/"
     *               ...
     * </pre>
     * @return saved preferences
     */
    public List<GuiPreferences> getSavedStoreListAsRoot() {
        GuiValueStore root = getPreferencesNodeAsRoot();
        try {
            List<GuiPreferences> savedList = new ArrayList<>();
            if (root.hasNodeKey(KEY_SAVED)) {
                GuiValueStore saved = root.getChild(KEY_SAVED);
                List<String> stores = saved.getKeys();

                for (String key : stores) {
                    if (saved.hasNodeKey(key)) {
                        GuiPreferences savedPrefs = new GuiPreferences(context);
                        savedPrefs.valueStore = saved.getChild(savedPrefs, key);
                        if (!savedPrefs.getValueStore().getString(KEY_UUID, "").isEmpty()) {
                            savedList.add(savedPrefs);
                        }
                    }
                }
            }
            return savedList;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @return "": value of "$uuid", "" (default) or "empty" (empty)
     */
    public String getLaunchPrefsAsRoot() {
        GuiValueStore root = getPreferencesNodeAsRoot();
        return root.getString(KEY_LAUNCH_PREFS, "");
    }

    public void setLaunchPrefsAsRoot(String uuid) {
        GuiValueStore root = getPreferencesNodeAsRoot();
        root.putString(KEY_LAUNCH_PREFS, uuid);
    }

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public GuiPreferences addNewSavedStoreAsRoot() {
        GuiValueStore root = getPreferencesNodeAsRoot();
        GuiValueStore saved = root.getChild(KEY_SAVED);
        try {
            List<String> stores = saved.getKeys().stream()
                    .filter(saved::hasNodeKey)
                    .toList();
            int n = stores.size();
            String key = "$" + n;
            while (stores.contains(key)) {
                n++;
                key = "$" + n;
            }
            GuiPreferences newPrefs = new GuiPreferences(context);
            newPrefs.valueStore = saved.getChild(newPrefs, key);

            String name = context.getName();

            newPrefs.valueStore.putString(KEY_NAME,
                    String.format("%s %d - %s", name, (n + 1), LocalDateTime.now().format(formatter)));
            newPrefs.valueStore.putString(KEY_UUID,
                    UUID.randomUUID().toString());

            return newPrefs;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void setCurrentValue(Object value) {
        HistoryValueEntry e = createHistoryValueEntry(value); //create an entry for serialization
        e.storeAsCurrentValue();
    }

    /**
     *  saves the given JSON as a prefs entry
     * @param json the saved object
     * @since 1.5
     */
    public void setCurrentValueAsJsonSupported(Object json) {
        HistoryValueEntry e = new HistoryValueEntryJsonSupported(this, json);
        e.storeAsCurrentValue();
    }

    public Object getCurrentValue() {
        HistoryValueEntry e = createHistoryValueEntry(null);
        return e.loadAsCurrentValue();
    }

    /**
     *  loads prefs JSON object for {@link PreferencesJsonSupport#setPrefsJson(Map)}
     * @return loaded JSON object
     * @since 1.5
     */
    public Object getCurrentValueAsJsonSupported() {
        HistoryValueEntry e = new HistoryValueEntryJsonSupported(this, null);
        return e.loadAsCurrentValue();
    }

    /**
     * add a value to the history.
     * <ol>
     *  <li>it first loads the existing history if not yet loaded, by {@link #loadHistoryValues()}.</li>
     *  <li>
     *   it finds an existing entry or a create new entry by {@link #getHistoryValue(Object)}.
     *    the entire number of entries always keeps under {@link #historyValueLimit}.
     *  </li>
     *  <li>
     *   The entry moves to the end of the history as the latest value,
     *    by setting the max index + 1 as it's index.
     *  </li>
     *  <li>set keyIndex if -1; those are new entries and no exsting nodes</li>
     *  <li>if the max index overs 100 x {@link #historyValueLimit}, reset all indices from 0</li>
     * </ol>
     * @param value the new entry value
     */
    public void addHistoryValue(Object value) {
        addHistoryValue(value, null);
    }

    public void addHistoryValue(Object value, Instant optionalTime) {
        HistoryValueEntry e = getHistoryValue(value);
        if (e == null) {
            return; //value is null or cannot be converted to json
        }
        int maxIndex = historyValues.isEmpty() ? -1 : historyValues.getLast().getIndex(); //historyValues are sorted by index
        e.setIndex(maxIndex + 1);
        if (optionalTime != null) {
            e.setTime(optionalTime);
        }
        historyValues.add(e); //add to the tail
        syncHistoryValues((maxIndex + 1) > ((long) getHistoryValueLimit()) * 100L);
    }

    public int getHistoryValueLimit() {
        return historyValueLimit;
    }

    public void syncHistoryValues() {
        syncHistoryValues(true);
    }

    public void syncHistoryValues(boolean resetIndex) {
        for (int i = 0, l = historyValues.size(); i < l; ++i) {
            var e = historyValues.get(i);
            if (resetIndex) {
                e.setIndex(i);
            }
            if (e.getKeyIndex() == -1) { //a new entry will have key=-1 and then the value will be stored
                e.setKeyIndexWithLoadOrStore(getHistoryValueUnusedKeyIndex());
            }
        }
        getValueStore().flush();
    }

    /**
     * sort es by index,
     * @param es entries from another prefs
     */
    public void setHistoryValues(List<HistoryValueEntry> es) {
        List<HistoryValueEntry> sorted = es.stream()
                .sorted(Comparator.comparing(HistoryValueEntry::getIndex))
                .limit(getHistoryValueLimit())
                .collect(Collectors.toCollection(ArrayList::new));
        if (historyValues == null) {
            loadHistoryValues();
        }
        var existingRemain = new HashSet<>(getHistoryValues());
        for (var e : sorted) {
            var existing = getHistoryValueForStoredValue(e.getValue());
            if (existing == null) {
                var created = createHistoryValueEntry(null);
                created.setValue(e.getValue());
                created.setIndex(e.getIndex());
                created.setTime(e.getTime());
                created.setKeyIndexWithLoadOrStore(getHistoryValueUnusedKeyIndex());
                historyValues.add(created);
            } else {
                existingRemain.remove(existing);
                existing.setIndex(e.getIndex());
                existing.setTime(e.getTime());
                historyValues.add(existing);
            }
        }
        if (!existingRemain.isEmpty()) {
            removeHistories(existingRemain);
        }
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

    public void overwriteByAnotherPrefs(GuiPreferences prefs) {
        //keep structure of the self store
        var storeSelf = this.getValueStore();
        var storeOthr = prefs.getValueStore();
        var remainingKeysSelf = new HashSet<>(storeSelf.getKeys());
        var childrenSelf = this.childrenSet();
        var chidlrenOthr = prefs.childrenSet();
        remainingKeysSelf.remove(KEY_HISTORY);
        for (var key : storeOthr.getKeys()) {
            if (key.equals(KEY_HISTORY)) {
                continue; //processed always later
            } else if (storeOthr.hasEntryKey(key)) {
                if (storeSelf.hasNodeKey(key)) { //incompatible: the key becomes a sub-node
                    var childSelf = removeFromChildrenSet(childrenSelf, key);
                    if (childSelf != null) {
                        childSelf.overwriteToEmpty();
                    } else {
                        storeSelf.remove(key);
                    }
                } else { //compatible,  not yet saved, or incompatible: the key disappears
                    var value = storeOthr.getString(key, "");
                    storeSelf.putString(key, value);
                }
                remainingKeysSelf.remove(key);
            } else if (storeOthr.hasNodeKey(key)) { //child prefs
                var childSelf = removeFromChildrenSet(childrenSelf, key);
                var childOthr = removeFromChildrenSet(chidlrenOthr, key);
                if (childSelf != null && childOthr != null) {
                    childSelf.overwriteByAnotherPrefs(childOthr);
                } else if (childSelf == null && childOthr != null) { //incompatbile: the sub-node disappears
                    storeSelf.remove(key);
                } else if (childSelf != null && childOthr == null) { //incompatbile: the unkown structure node becomes a sub-node
                    childSelf.overwriteToEmpty();
                } else { //unkown structure node
                    overwirteStore(storeSelf.getChild(key), storeOthr.getChild(key));
                }
                remainingKeysSelf.remove(key);
            }
        }
        for (var key : remainingKeysSelf) {
            var childSelf = removeFromChildrenSet(childrenSelf, key);
            if (childSelf != null) {
                childSelf.overwriteToEmpty();
            } else {
                storeSelf.remove(key);
            }
        }
        setHistoryValues(prefs.getHistoryValues());
    }

    private void overwirteStore(GuiValueStore storeSelf, GuiValueStore storeOthr) {
        var remainingKeys = new HashSet<>(storeSelf.getKeys());
        for (var key : storeOthr.getKeys()) {
            remainingKeys.remove(key);
            if (storeOthr.hasEntryKey(key)) {
                if (storeSelf.hasEntryKey(key)) {
                    var value = storeOthr.getString(key, "");
                    storeSelf.putString(key, value);
                } else if (storeSelf.hasNodeKey(key)) { //incompatible
                    storeSelf.remove(key);
                } else { //not yet set or incompatible
                    var value = storeOthr.getString(key, "");
                    storeSelf.putString(key, value);
                }
            } else if (storeOthr.hasNodeKey(key)) {
                if (storeSelf.hasEntryKey(key)) { //incompatible
                    storeSelf.remove(key);
                } else {
                    overwirteStore(storeSelf.getChild(key), storeOthr.getChild(key));
                }
            }
        }
        remainingKeys.forEach(storeSelf::remove);
    }

    public void overwriteToEmpty() {
        var storeSelf = getValueStore();
        var childrenSelf = this.childrenSet();
        for (var key : storeSelf.getKeys()) {
            if (key.equals(KEY_HISTORY)) {
                setHistoryValues(List.of());
            } else if (storeSelf.hasEntryKey(key)) {
                storeSelf.remove(key);
            } else if (storeSelf.hasNodeKey(key)) {
                var childSelf = removeFromChildrenSet(childrenSelf, key);
                if (childSelf != null) {
                    childSelf.overwriteToEmpty();
                } else {
                    storeSelf.remove(key);
                }
            }
        }
    }

    private GuiPreferences removeFromChildrenSet(Set<GuiPreferences> children, String key) {
        var childOpt = children.stream()
                .filter(e -> e.getName().equals(key))
                .findFirst();
        if (childOpt.isPresent()) {
            var child = childOpt.get();
            children.remove(child);
            return child;
        } else {
            return null;
        }
    }

    public void load() {
        if (historyValues == null) {
            loadHistoryValues();
        }
        var store = getValueStore();
        var keys = new HashSet<>(store.getKeys());
        var children = childrenSet();
        children.forEach(GuiPreferences::load);
        children.forEach(child -> keys.remove(child.getName()));
        loadNodes(store, keys);
    }

    protected void loadNodes(GuiValueStore store, Collection<String> keys) {
        for (var key : keys) {
            if (store.hasNodeKey(key)) {
                var child = store.getChild(key);
                loadNodes(child, child.getKeys());
            }
        }
    }

    private Set<GuiPreferences> childrenSet() {
        return getContext().getChildren().stream()
                .map(this::getChild)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * loaded history values
     * <ul>
     *     <li>the size of the list is up to {@link #historyValueLimit}</li>
     *     <li>all entries have valid indices, !=-1, ordered, but might not start from 0</li>
     *     <li>can be sorted by indices; the larger indices are recent entries</li>
     *     <li>their keyIndex are meaningless, just unique key for node</li>
     * </ul>
     * @return loaded history-values
     */
    public List<HistoryValueEntry> getHistoryValues() {
        if (historyValues == null) {
            loadHistoryValues();
        }
        return historyValues;
    }

    /**
     * @param value the entry value (raw-object)
     * @return a new entry (keyIndex == -1),
     *   an existing entry (index!=-1; removed from {@link #historyValues}), or
     *   an existing free entry (index==-1; removed from {@link #historyValuesFree});
     *  the value is set to JSON of the given value;
     *   the obtained entry is temporarly not parted in both historyValues and historyValuesFree.
     * */
    public HistoryValueEntry getHistoryValue(Object value) {
        HistoryValueEntry created = createHistoryValueEntry(value);
        Object v = created.getValue(); //it might be converted to JSON
        if (v == null) {
            return null;
        }
        var existing = getHistoryValueForStoredValue(v);
        if (existing == null) {
            return created;
        } else {
            return existing;
        }
    }

    protected HistoryValueEntry getHistoryValueForStoredValue(Object v) {
        if (historyValues == null) {
            loadHistoryValues();
        }
        for (var existing : historyValues) {
            if (existing.match(v)) { //reuse matched item
                historyValues.remove(existing);
                return existing;
            }
        }
        if (historyValues.size() >= getHistoryValueLimit()) { //no free space: oldest item
            var existing = historyValues.removeFirst();
            existing.setValue(v);
            existing.setTime(Instant.now()); //different value: update the time
            return existing;
        } else if (!historyValuesFree.isEmpty()) {
            var existing = historyValuesFree.removeFirst();
            existing.setValue(v);
            existing.setTime(Instant.now());
            return existing;
        } else {
            return null;
        }
    }

    /**
     *
     * @return a new entry (keyIndex == -1),
     *    an existing entry (index!=-1; removed from {@link #historyValues}), or
     *    an existing free entry (index==-1; removed from {@link #historyValuesFree});
     *   the obtained entry is temporarly not parted in both historyValues and historyValuesFree.
     */
    public HistoryValueEntry getHistoryValueFree() {
        if (getHistoryValues().size() >= getHistoryValueLimit()) {
            var existing = historyValues.removeFirst();
            existing.setTime(Instant.now());
            return existing;
        } else if (!historyValuesFree.isEmpty()) {
            var existing = historyValuesFree.removeFirst();
            existing.setTime(Instant.now());
            return existing;
        } else {
            return createHistoryValueEntry(null);
        }
    }

    public List<HistoryValueEntry> getHistoryValuesFree() {
        return historyValuesFree;
    }

    /**
     * load or create historyValues: it's size becomes up to historyValueLimit and sorted by index.
     * The new entries have -1 indices.
     */
    public void loadHistoryValues() {
        var hs = createHistoryValuesByLoad();
        int l = getHistoryValueLimit();
        historyValuesFree = new ArrayList<>(l);
        historyValues = new ArrayList<>(l);
        for (var entry : hs) {
            if (entry.getIndex() == -1) {
                historyValuesFree.add(entry);
            } else {
                historyValues.add(entry);
            }
        }
    }

    protected List<HistoryValueEntry> createHistoryValuesByLoad() {
        int l = getHistoryValueLimit();
        var historyValues = new ArrayList<HistoryValueEntry>(l);
        for (int i = 0; i < l; ++i) {
            HistoryValueEntry e = createHistoryValueEntry(null);
            try {
                e.setKeyIndexWithLoadOrStore(i);
                historyValues.add(e);
            } catch (Exception ex) {
                //failed to load the entry
            }
        }
        historyValues.sort(Comparator.comparing(HistoryValueEntry::getIndex));
        return historyValues;
    }

    public HistoryValueEntry createHistoryValueEntry(Object value) {
        if (context.isHistoryValueStored(value)) {
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
        getValueStore().flush();
    }

    public void clearHistoriesTree() {
        clearHistoriesTree(true);
    }

    public void clearHistoriesTree(boolean clearPrefs) {
        clearHistories();
        for (GuiMappingContext subContext : context.getChildren()) {
            GuiPreferences prefs = subContext.getPreferences();
            try (var lock = prefs.lock()) {
                lock.use();
                prefs.clearHistoriesTree(clearPrefs);
            }
        }
        if (clearPrefs) {
            context.clearPreferences();
        }
    }

    public void clearHistories() {
        //move to free-list
        getHistoryValues().forEach(HistoryValueEntry::remove);
        historyValuesFree.addAll(historyValues);
        historyValues.clear();
    }

    public void removeHistories(Collection<HistoryValueEntry> loadedValuesRemoved) {
        historyValues.removeAll(loadedValuesRemoved);
        loadedValuesRemoved.forEach(HistoryValueEntry::remove);
        historyValuesFree.addAll(loadedValuesRemoved);
    }

    /** the abstract definition of key-value store.
     *
     *  <ul>
     *      <li>a key may be associated with a child node or an entry, and it shares the name space.</li>
     *      <li>an entry can be obtained as a String or a Integer</li>
     *      <li>an Integer entry can be obtained as a String entry</li>
     *      <li>a node is associated with a {@link GuiPreferences}, which might be the preferences of the store
     *            or a sub-preferences.
     *            if a sub-preferences, the name of the prefs ({@link GuiPreferences#getName()} becomes the node key.</li>
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

        public void setPreferences(GuiPreferences preferences) {
            this.preferences = preferences;
        }

        public abstract void putString(String key, String val);
        public abstract String getString(String key, String def);

        public abstract void putInt(String key, int val);
        public abstract int getInt(String key, int def);

        public GuiValueStore getChild(GuiPreferences preferences) {
            return getChild(preferences, preferences.getName());
        }

        public GuiValueStore getChild(String key) {
            return getChild(preferences, key);
        }

        public abstract GuiValueStore getChild(GuiPreferences preferences, String key);

        public abstract boolean hasEntryKey(String key);
        public abstract boolean hasNodeKey(String key);

        /**
         * @return list of a key which might be different from a given key for put(k,...)
         *         because of the max key length,
         *          converted by {@link #toStoreKey(String)}.
         *          such key will be acceptable as an argument for other methods.
         */
        public abstract List<String> getKeys();

        public abstract void remove(String key);

        /**
         * erase the node, but it might not remove the node from the parent
         */
        public abstract void removeThisNode();
        public void flush() { }

        public abstract GuiValueStore copyInitAsRoot();
    }

    /** the concrete implementation of the store by {@link Preferences} */
    public static class GuiValueStoreDefault extends GuiValueStore {
        /* note: in macOS, ~/Library/Preferences/com.apple.java.util.prefs.plist ? */
        protected Preferences store;

        protected Supplier<Preferences> parentStore;
        protected String storeName;
        protected String storeNameActual;

        public GuiValueStoreDefault(GuiPreferences preferences, Preferences store) {
            super(preferences);
            this.store = store;
        }

        @SuppressWarnings("this-escape")
        public GuiValueStoreDefault(GuiPreferences preferences, Supplier<Preferences> parentStore, String storeName) {
            super(preferences);
            this.parentStore = parentStore;
            this.storeName = storeName;
            this.storeNameActual = toStoreKey(storeName);
            try {
                Preferences parent = parentStore.get();
                if (parent.nodeExists(storeNameActual)) {
                    store = withTry(storeName, () -> parent.node(storeNameActual));
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
                store = withTry(storeName, () -> parentStore.get().node(storeNameActual));
            }
            return store;
        }

        @Override
        public void putString(String key, String val) {
            getOrCreateStore().put(toStoreKey(key), val);
        }

        @Override
        public String getString(String key, String def) {
            return store == null ? def : withTry(key, () -> store.get(toStoreKey(key), def));
        }

        @Override
        public void putInt(String key, int val) {
            withTry(key, () -> getOrCreateStore().putInt(toStoreKey(key), val));
        }

        @Override
        public int getInt(String key, int def) {
            return store == null ? def : withTry(key, () -> store.getInt(toStoreKey(key), def));
        }

        protected void withTry(String key, Runnable task) {
            Supplier<Void> s = () -> {task.run(); return null;};
            withTry(key, s);
        }

        protected <T> T withTry(String key, Supplier<T> task) {
            try {
                return task.get();
            } catch (Exception ex) {
                throw new RuntimeException(toStoreKey(key), ex);
            }
        }

        @Override
        public GuiValueStore getChild(GuiPreferences preferences, String key) {
            return new GuiValueStoreDefault(preferences, this::getOrCreateStore, key);
        }

        @Override
        public boolean hasEntryKey(String key) {
            try {
                return Arrays.asList(getOrCreateStore().keys()).contains(toStoreKey(key));
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public boolean hasNodeKey(String key) {
            try {
                return getOrCreateStore().nodeExists(toStoreKey(key));
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
            key = toStoreKey(key);
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
                store = null;
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

        @Override
        public GuiValueStore copyInitAsRoot() {
            if (parentStore == null) {
                return new GuiValueStoreDefault(null, store);
            } else {
                return new GuiValueStoreDefault(null, parentStore, storeName);
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
     *<p>
     * The value has the following stages. Some types have same form in the multiple stages.
     * <ol>
     *     <li>rawObject : actual object. the constructor takes it.</li>
     *     <li>value : the instance-field holds on the memory, {@link #toValueInit(Object)} with rawObject.
     *           if non-null {@link #isJsonValue()} rawObject, it will be converted as JSON by repr,
     *           otherwise, rawObject. {@link #setValue(Object)} takes it.
     *        </li>
     *     <li>storedJsonValue : a JSON object internally created by {@link #getStoredJsonValue()}.
     *        if {@link #isJsonValue()}, value itself, otherwise, converted JSON by repr. </li>
     *     <li>source : JSON source by {@link #getStoredJsonValue()}</li>
     * </ol>
     */
    public static class HistoryValueEntry {
        protected GuiPreferences preferences;
        protected Object value;
        protected int keyIndex = -1;
        protected int index = -1;
        protected GuiValueStore valueStore;
        protected Instant time;

        @SuppressWarnings("this-escape")
        public HistoryValueEntry(GuiPreferences preferences, Object rawObject) {
            this.preferences = preferences;
            this.time = Instant.now();
            this.value = toValueInit(rawObject);
        }

        /**
         * called from the constructor
         * @param rawObject the constructor argument
         * @return stored value as a prefs entry
         * @since 1.5
         */
        protected Object toValueInit(Object rawObject) {
            if (isJsonValue() && rawObject != null) {
                return preferences.getContext().getRepresentation()
                        .toJsonWithNamed(preferences.getContext(), rawObject);
            } else {
                return rawObject;
            }
        }

        public boolean match(Object v) {
            return Objects.equals(value, v);
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            boolean diff = !Objects.equals(this.value, value);
            this.value = value;
            if (diff && keyIndex != -1) {
                storeValue();
            }
        }

        protected void storeValue() {
            GuiValueStore store = getValueStore();
            String jsonSource = getStoredJsonValue();
            store.putString(KEY_HISTORY_ENTRY_VALUE, jsonSource);
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


        public void remove() {
            if (this.keyIndex != -1) {
                GuiValueStore store = getValueStore();
                store.putInt(KEY_HISTORY_ENTRY_INDEX, -1);
                store.putString(KEY_HISTORY_ENTRY_VALUE, "null");
                store.putString(KEY_HISTORY_ENTRY_TIME, "null");
                store.removeThisNode();
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
         *   Also, when the value is not -1, load "$history/time" and decode it as a {@link Instant} string.
         *  {@link #getValue()} will be null if it failed.
         *    */
        public void load() {
            GuiValueStore store = getValueStore();
            int index = store.getInt(KEY_HISTORY_ENTRY_INDEX, -1);
            if (index != -1) {
                this.index = index;
                String v = store.getString(KEY_HISTORY_ENTRY_VALUE, "null");
                this.value = fromJsonSource(v);

                String timeVal = store.getString(KEY_HISTORY_ENTRY_TIME, null);
                //Note: the time is stored directly as a string created by Instant#toString()
                 //  thus the following code is probably needless and as a precaution.
                if (timeVal != null && timeVal.startsWith("\"")) { //it seems that the string is a JSON string.
                    try {
                        Object timeJson = JsonReader.create(timeVal).parseValue();
                        if (timeJson instanceof String) {
                            timeVal = ((String) timeJson);
                        }
                    } catch (Exception ex) {
                        //
                    }
                }

                if (timeVal != null) {
                    try {
                        this.time = Instant.parse(timeVal);
                    } catch (Exception ex) {
                        //
                    }
                }
            }
        }

        public Object fromJsonSource(String s) {
            Object jsonValue = JsonReader.create(s).parseValue();
            if (jsonValue != null) {
                if (isJsonValue()) {
                    return jsonValue;
                } else {
                    return preferences.getContext().getRepresentation()
                            .fromJson(preferences.getContext(), null, jsonValue);
                }
            } else {
                return null;
            }
        }

        /** @return if false, the value holds a raw-object which might be a non-JSON object,
         *        and then the associated representation can create a new value from a JSON source without any raw-object. */
        public boolean isJsonValue() {
            return preferences.getContext().getRepresentation().isJsonSetter();
        }

        public void store() {
            storeValue();
            GuiValueStore store = getValueStore();
            if (index != -1) {
                store.putInt(KEY_HISTORY_ENTRY_INDEX, index);
            }
            store.putString(KEY_HISTORY_ENTRY_TIME, time.toString());
        }

        public String getStoredJsonValue() {
            if (value == null || getValueStore() == null) {
                return null;
            }
            Object json = isJsonValue() ? this.value :
                    preferences.getContext().getRepresentation()
                            .toJsonWithNamed(preferences.getContext(), this.value);
            return JsonWriter.create().withNewLines(false).write(json).toSource();
        }

        public GuiValueStore getValueStore() {
            if (valueStore == null) {
                valueStore = getParent()
                        .getChild("" + keyIndex);
            }
            return valueStore;
        }

        protected GuiValueStore getParent() {
            return preferences.getValueStore()
                    .getChild(KEY_HISTORY);
        }

        public void setIndex(int index) {
            if (this.index != index && keyIndex != -1) {
                //update index
                GuiValueStore store = getValueStore();
                store.putInt(KEY_HISTORY_ENTRY_INDEX, index);
            }
            this.index = index;
        }

        public void setTime(Instant time) {
            if (!Objects.equals(time, this.time) && time != null && keyIndex != -1) {
                GuiValueStore store = getValueStore();
                store.putString(KEY_HISTORY_ENTRY_TIME, time.toString());
            }
            this.time = time;
        }

        public void storeAsCurrentValue() {
            String jsonSource = getStoredJsonValue();
            if (jsonSource != null) {
                preferences.getValueStore().putString(KEY_CURRENT_VALUE, jsonSource);
            }
        }

        public Object loadAsCurrentValue() {
            String jsonSource = preferences.getValueStore().getString(KEY_CURRENT_VALUE, null);
            if (jsonSource != null && getValueStore() != null) {
                return this.value = fromJsonSource(jsonSource);
            } else {
                return null;
            }
        }
    }

    /**
     * JSON entry for JSON support object
     * @since 1.5
     */
    public static class HistoryValueEntryJsonSupported extends HistoryValueEntry {
        public HistoryValueEntryJsonSupported(GuiPreferences preferences, Object json) {
            super(preferences, json);
        }

        @Override
        protected Object toValueInit(Object rawObject) {
            return rawObject;
        }

        @Override
        public boolean isJsonValue() {
            return true;
        }
    }

    /**
     * an on-memory impl. of value history entry, created when the repr. is {@link GuiReprValue#isHistoryValueStored(Object)}.
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
        public void setTime(Instant time) {
            this.time = time;
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
        var existingKeys = store.getKeys();

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
        protected GuiValueStoreOnMemory parent;
        protected Map<String,Object> values;

        /** for test purpose: it's preferences can be set by the GuiPreferences constructor that takes the store */
        public GuiValueStoreOnMemory() {
            this(null);
        }

        public GuiValueStoreOnMemory(GuiPreferences preferences) {
            this(preferences, null);
        }

        public GuiValueStoreOnMemory(GuiPreferences preferences, GuiValueStoreOnMemory parent) {
            super(preferences);
            this.parent = parent;
            this.values = new LinkedHashMap<>();
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
                return Integer.parseInt((String) values.getOrDefault(key, Integer.toString(def)));
            } catch (Exception ex) {
                return def;
            }
        }

        @Override
        public GuiValueStore getChild(GuiPreferences preferences, String key) {
            return (GuiValueStore) values.computeIfAbsent(key,
                    k -> new GuiValueStoreOnMemory(preferences, this));
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
            return v instanceof GuiValueStore;
        }

        public void putChild(String name, GuiValueStore value) {
            values.put(name, value);
            if (value instanceof GuiValueStoreOnMemory m) {
                m.parent = this;
            }
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
            if (parent != null) {
                for (var e : new ArrayList<>(parent.values.entrySet())) {
                    if (e.getValue() == this) {
                        parent.values.remove(e.getKey());
                    }
                }
            }
        }

        @Override
        public GuiValueStore copyInitAsRoot() {
            return new GuiValueStoreOnMemory();
        }
    }
}
