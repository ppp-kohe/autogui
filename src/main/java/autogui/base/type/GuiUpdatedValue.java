package autogui.base.type;

import java.util.Objects;

public interface GuiUpdatedValue {
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

    final class GuiUpdatedValueObject implements GuiUpdatedValue {
        protected Object value;

        public GuiUpdatedValueObject(Object value) {
            this.value = value;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Updated(" + Objects.toString(value) + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GuiUpdatedValueObject that = (GuiUpdatedValueObject) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    final class GuiUpdatedValueNone implements GuiUpdatedValue {
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
