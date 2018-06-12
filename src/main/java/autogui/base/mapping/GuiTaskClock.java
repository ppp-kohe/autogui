package autogui.base.mapping;

import java.util.Objects;

/**
 * clock of a context or a view.
 *
 *  both the context({@link #view}=false) and the view({@link #view}=true) maintain each clock.
 *  <p>
 *  values are passed between the context and the view with clocks.
 *  <p>
 *  the one of two sides, the context or the view, takes a passed value with an opponent clock,
 *    and if the current clock of the side is older than the opponent clock,
 *       then it can accept the value and updates the clock with the opponent one:
 *       {@link #isOlderWithSet(GuiTaskClock)}.
 *   <p>
 *   The one side updates its value (e.g. an user edits the GUI value of the view),
 *      then the clock of the side {@link #increment()} and {@link #copy()}
 *        and send it to the other side.
 */
public class GuiTaskClock implements Comparable<GuiTaskClock>, Cloneable {
    protected volatile long count;
    protected boolean view;

    public GuiTaskClock(boolean view) {
        this(0, view);
    }

    public GuiTaskClock(long count, boolean view) {
        this.count = count;
        this.view = view;
    }

    public boolean isNewer(GuiTaskClock o) {
        return compareTo(o) > 0;
    }

    public boolean isOlderWithSet(GuiTaskClock o) {
        if (o.isNewer(this)) {
            this.count = o.count;
            return true;
        } else {
            return false;
        }
    }

    public GuiTaskClock increment() {
        synchronized (this) {
            ++count;
        }
        return this;
    }

    public GuiTaskClock copy() {
        try {
            return (GuiTaskClock) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int compareTo(GuiTaskClock o) {
        int n = Long.compare(count, o.count);
        if (n == 0) {
            return Boolean.compare(view, o.view);
        } else {
            return n;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuiTaskClock taskClock = (GuiTaskClock) o;
        return count == taskClock.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(count);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + count + ",view=" + view + ")";
    }
}
