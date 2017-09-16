package autogui.base.type;

public class GuiTypeValue implements GuiTypeElement {
    protected String name;

    protected Class<?> type;

    public GuiTypeValue(Class<?> type) {
        this(type.getName());
        this.type = type;
    }

    public GuiTypeValue(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public Class<?> getType() {
        if (type == null) {
            try {
                type = Class.forName(name);
            } catch (Exception ex) {
                type = null;
            }
        }
        return type;
    }

    @Override
    public String toString() {
        return "type(" + name + ")";
    }

    /** returns a value other than {@link #NO_UPDATE} if the value is updated from the prevValue;
     *   the returned value is a next(current) state of the prevValue.
     *   Even if the returned value is equivalent to the prevValue, it is non-NO_UPDATE and then it indicates an update.
     * the default implementation returns NO_UPDATE.
     *  The main purpose of the method is provide extensibility for GUI source.
     * */
    public Object updatedValue(Object prevValue) {
        return null;
    }

    public boolean isWritable(Object value) {
        return true;
    }

    /** returns a new value */
    public Object writeValue(Object prevValue, Object newValue) {
        return newValue;
    }

    public static NoUpdate NO_UPDATE = NoUpdate.NoUpdate;

    public enum NoUpdate {
        NoUpdate;
    }
}
