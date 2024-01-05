package org.autogui.base.type;

import java.util.Objects;

/**
 * a value wrapper for changed property values:
 * <pre>
 *     GuiUpdatedValue.of(v)
 *     //or
 *     GuiUpdatedValue.NO_UPDATE //means that a property is not changed
 * </pre>
 */
public interface GuiUpdatedValue {
    /**
     * {@link #isNone()} returns true
     */
    GuiUpdatedValueNone NO_UPDATE = new GuiUpdatedValueNone();

    default boolean isNone() {
        return false;
    }

    /**
     * @return null if none
     */
    default Object getValue() {
        return null;
    }

    static GuiUpdatedValueObject of(Object v) {
        return new GuiUpdatedValueObject(v);
    }

    /**
     * the value holder for {@link GuiUpdatedValue}
     */
    record GuiUpdatedValueObject(Object value) implements GuiUpdatedValue {

    @Override
        public String toString() {
            return "Updated(" + value + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GuiUpdatedValueObject that = (GuiUpdatedValueObject) o;
            return Objects.equals(value, that.value);
        }
    }

    /**
     * the no value for {@link GuiUpdatedValue}, can be obtained by {@link #NO_UPDATE}
     */
    final class GuiUpdatedValueNone implements GuiUpdatedValue {
        public GuiUpdatedValueNone() {}
        @Override
        public boolean isNone() {
            return true;
        }

        @Override
        public String toString() {
            return "NO_UPDATE";
        }
    }
}
