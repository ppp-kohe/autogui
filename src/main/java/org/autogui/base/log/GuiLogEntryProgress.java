package org.autogui.base.log;

import java.io.Closeable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * a log-entry for progress:
 * <pre>
 *     [time] minimum|=========value - - - - |maximum : valueP %, message
 *     or
 *     [time] |- - - indeterminate - - - | message
 *     =&gt;
 *     [time] message
 *     [endTime] 100%
 * </pre>
 */
public class GuiLogEntryProgress implements GuiLogEntry, Closeable {
    protected int minimum = 0;
    protected int maximum = Integer.MAX_VALUE;
    protected int value;
    protected boolean indeterminate = false;
    protected String message = "";
    protected Instant time;
    protected Instant endTime;
    protected Thread thread;

    protected List<Consumer<GuiLogEntryProgress>> listeners;

    public GuiLogEntryProgress() {
        time = Instant.now();
        thread = Thread.currentThread();
    }

    /** copy fields of p. p can be null
     * @param p copied state
     * */
    public void setState(GuiLogEntryProgress p) {
        if (p == null) {
            minimum = 0;
            maximum = Integer.MAX_VALUE;
            value = 0;
            indeterminate = false;
            message = "";
            time = null;
            endTime = null;
            thread = null;
        } else {
            synchronized (p) {
                minimum = p.getMinimum();
                maximum = p.getMaximum();
                value = p.getValue();
                indeterminate = p.isIndeterminate();
                message = p.getMessage();
                time = p.getTime();
                endTime = p.getEndTime();
                thread = p.getThread();
            }
        }
    }

    /**
     * @param listener the listener will receive the progress when its valueP is updated
     * @return this
     */
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

    /** notifies to listeners. this is automatically called from setter methods*/
    public void fireChange() {
        if (listeners != null) {
            listeners.forEach(l -> l.accept(this));
        }
    }

    /** it might cause {@link GuiLogEntryProgressInterruptedException}.
     * it also causes a notification to listeners when the minimum value is actually changed.
     * @param minimum the minimum value of the progress to be set: if the value is smaller than the maximum,
     *                 then the intermediate flag becomes false
     * @return this
     * */
    public synchronized GuiLogEntryProgress setMinimum(int minimum) {
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


    /** it might cause {@link GuiLogEntryProgressInterruptedException}.
     * it also causes a notification to listeners when the maximum value is actually changed.
     * @param maximum the maximum value of the progress to be set: if the value is larger than the minimum
     *                 then the intermediate flag becomes false
     * @return this
     *  */
    public synchronized GuiLogEntryProgress setMaximum(int maximum) {
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
     *  it might cause {@link GuiLogEntryProgressInterruptedException}
     * @param p  0 to 1.0
     * @return this
     * @see #setValue(int)
     */
    public synchronized GuiLogEntryProgress setValueP(double p) {
        return setValue((int) ((maximum - minimum) * p));
    }

    /**  it might cause {@link GuiLogEntryProgressInterruptedException}
     * if the value is actually changed, it causes a notification to listeners
     * @param n the progress value
     * @return this
     *
     * */
     public synchronized GuiLogEntryProgress setValue(int n) {
        if (value != n) {
            value = n;
            fireChange();
        }
        checkInterruption();
        return this;
    }

    /**
     *  it might cause {@link GuiLogEntryProgressInterruptedException}
     * @param p 0 to 1.0
     * @return this
     * @see #addValue(int)
     */
    public synchronized GuiLogEntryProgress addValueP(double p) {
        return addValue((int) ((maximum - minimum) * p));
    }

    /**  it might cause {@link GuiLogEntryProgressInterruptedException}
     * @param n added value to the progress
     * @return this
     * @see #setValue(int)
     * */
     public synchronized GuiLogEntryProgress addValue(int n) {
         if ((n >= 0 && value >= maximum) || (n < 0 && value <= minimum)) {
             return setValue(value);
         } else {
             return setValue(value + n);
         }
    }

    /**  it might cause {@link GuiLogEntryProgressInterruptedException}.
     * if the flag is actually changed, it causes a notification to listeners
     * @param indeterminate the intermediate flag to be set
     * @return this
     * */
     public synchronized GuiLogEntryProgress setIndeterminate(boolean indeterminate) {
        if (indeterminate != this.indeterminate) {
            this.indeterminate = indeterminate;
            fireChange();
        }
        checkInterruption();
        return this;
    }

    /**  it might cause {@link GuiLogEntryProgressInterruptedException}.
     * the method will never cause a notification
     * @param message the new message
     * @return this
     * */
     public synchronized GuiLogEntryProgress setMessage(String message) {
        this.message = message;
        checkInterruption();
        return this;
    }

    public synchronized GuiLogEntryProgress setEndTime(Instant endTime) {
        this.endTime = endTime;
        return this;
    }

    public synchronized GuiLogEntryProgress setTime(Instant time) {
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

    /** @return true if endTime is set */
    public boolean isFinished() {
        return endTime != null;
    }

    @Override
    public void close() {
        finish();
    }

    /**  it might cause {@link GuiLogEntryProgressInterruptedException}.
     *  endTime will be set. value becomes maximum
     * */
    public synchronized void finish() {
        endTime = Instant.now();
        value = maximum;
        fireChange();
        checkInterruption();
    }

    public Instant getEndTime() {
        return endTime;
    }

    public synchronized GuiLogEntryProgress setThread(Thread thread) {
        this.thread = thread;
        return this;
    }

    /** @return by default, the current thread of constructor sender */
    public Thread getThread() {
        return thread;
    }

    /** if the current thread is equal to the thread of the progress, and the thread is interrupted,
     *     it causes {@link GuiLogEntryProgressInterruptedException}. */
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

    /** an exception thrown by checking-interruption */
    public static class GuiLogEntryProgressInterruptedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
