package autogui.base.mapping;

import java.util.prefs.Preferences;

public class GuiPreferences {
    protected GuiMappingContext context;
    protected GuiPreferences parent;
    protected GuiValueStore valueStore;

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
    }
}
