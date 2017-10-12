package autogui.base.log;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class GuiLogEntryProgress implements GuiLogEntry, Closeable {
    protected int minimum = 0;
    protected int maximum = Integer.MAX_VALUE;
    protected int value;
    protected boolean indeterminate = true;
    protected String message = "";
    protected Instant time;
    protected Instant endTime;
    protected Thread thread;

    protected List<Consumer<GuiLogEntryProgress>> listeners;

    public GuiLogEntryProgress() {
        time = Instant.now();
        thread = Thread.currentThread();
    }

    public GuiLogEntryProgress addListener(Consumer<GuiLogEntryProgress> listener) {
        if (listeners == null) {
            listeners = new ArrayList<>(3);
        }
        listeners.add(listener);
        return this;
    }

    public GuiLogEntryProgress removeListener(Consumer<GuiLogEntryProgress> listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
        return this;
    }

    public List<Consumer<GuiLogEntryProgress>> getListeners() {
        if (listeners == null) {
            return Collections.emptyList();
        } else {
            return listeners;
        }
    }

    public void fireChange() {
        if (listeners != null) {
            listeners.forEach(l -> l.accept(this));
        }
    }

    public GuiLogEntryProgress setMinimum(int minimum) {
        checkInterruption();
        boolean change = this.minimum == minimum;
        this.minimum = minimum;
        if (maximum - minimum > 0) {
            indeterminate = false;
        }
        if (change) {
            fireChange();
        }
        checkInterruption();
        return this;
    }

    public GuiLogEntryProgress setMaximum(int maximum) {
        boolean change = this.maximum == maximum;
        this.maximum = maximum;
        if (maximum - minimum > 0) {
            indeterminate = false;
        }
        if (change) {
            fireChange();
        }
        checkInterruption();
        return this;
    }

    /**
     * @param p  0 to 1.0
     * @return this
     */
    public GuiLogEntryProgress setValueP(double p) {
        return setValue((int) ((maximum - minimum) * p));
    }

    public GuiLogEntryProgress setValue(int n) {
        if (value != n) {
            value = n;
            fireChange();
        }
        checkInterruption();
        return this;
    }

    /**
     * @param p 0 to 1.0
     * @return this
     */
    public GuiLogEntryProgress addValueP(double p) {
        return addValue((int) ((maximum - minimum) * p));
    }

    public GuiLogEntryProgress addValue(int n) {
        return setValue(value + n);
    }

    public GuiLogEntryProgress setIndeterminate(boolean indeterminate) {
        if (indeterminate != this.indeterminate) {
            this.indeterminate = indeterminate;
            fireChange();
        }
        checkInterruption();
        return this;
    }

    public GuiLogEntryProgress setMessage(String message) {
        this.message = message;
        checkInterruption();
        return this;
    }

    public GuiLogEntryProgress setEndTime(Instant endTime) {
        this.endTime = endTime;
        return this;
    }

    public GuiLogEntryProgress setTime(Instant time) {
        this.time = time;
        return this;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public int getMaximum() {
        return maximum;
    }

    public int getMinimum() {
        return minimum;
    }

    public int getValue() {
        return value;
    }

    public double getValueP() {
        return value / (double) (maximum - minimum);
    }

    public Instant getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFinished() {
        return endTime != null;
    }

    @Override
    public void close() {
        finish();
    }

    public void finish() {
        endTime = Instant.now();
        value = maximum;
        fireChange();
        checkInterruption();
    }

    public Instant getEndTime() {
        return endTime;
    }

    public GuiLogEntryProgress setThread(Thread thread) {
        this.thread = thread;
        return this;
    }

    public Thread getThread() {
        return thread;
    }

    public void checkInterruption() {
        if (thread == Thread.currentThread() && Thread.interrupted()) {
            throw new GuiLogEntryProgressInterruptedException();
        }
    }

    @Override
    public boolean isActive() {
        return !isFinished();
    }

    @Override
    public String toString() {
        return "[" + getTime() + "] # " +
                getValue() +"/(" + getMinimum() + ".." + getMaximum() +"): " +
                String.format("%.3f", getValueP()) +
                (isIndeterminate() ? " I" : "") +
                (isFinished() ? " [" + getEndTime() + "]" : "") +
                (thread != null ? " @" + getThread().getName() : "") + getMessage();
    }

    public static class GuiLogEntryProgressInterruptedException extends RuntimeException {

    }
}
