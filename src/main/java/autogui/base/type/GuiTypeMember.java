package autogui.base.type;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;

/** the super type for actions and properties */
public abstract class GuiTypeMember implements GuiTypeElement {
    protected String name;
    /** nullable */
    protected GuiTypeObject owner;
    protected MemberOrdinal ordinal;

    public GuiTypeMember(String name) {
        this.name = name;
    }

    public void setOwner(GuiTypeObject owner) {
        this.owner = owner;
    }

    public GuiTypeObject getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    /**
     * @param name the name compared by {@link Method#getName()}
     * @param p a filter of methods
     * @return a method whose name is equivalent to the name with matching the p */
    public Method findOwnerMethod(String name, Predicate<Method> p) {
        if (getOwner() != null) {
            Class<?> type = getOwner().getType();
            if (type != null) {
                return Arrays.stream(type.getMethods())
                        .filter(m -> m.getName().equals(name))
                        .filter(p)
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }


    public void setOrdinal(MemberOrdinal ordinal) {
        this.ordinal = ordinal;
    }

    public MemberOrdinal getOrdinal() {
        if (ordinal == null) {
            ordinal = new MemberOrdinal(-1, getName());
        }
        return ordinal;
    }

    public static class MemberOrdinal implements Comparable<MemberOrdinal> {
        protected int index;
        protected String name;

        public MemberOrdinal(int index, String name) {
            this.index = index;
            this.name = name;
        }

        @Override
        public int compareTo(MemberOrdinal o) {
            int i = Integer.compare(index, o.index);
            return i != 0 ? i : name.compareTo(o.name);
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return index + ":" + name;
        }
    }
}
