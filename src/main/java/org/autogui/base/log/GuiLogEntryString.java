package org.autogui.base.log;

import java.time.Instant;

/** a log-entry for a string */
public class GuiLogEntryString implements GuiLogEntry {
    protected Instant time;
    protected String data;
    /** @since 1.1 */
    protected boolean fromStandard;

    public GuiLogEntryString(String data) {
        this(data, false);
    }

    /**
     * @param data the entry string
     * @param fromStandard whether data comes from {@link System#out}
     * @since 1.1
     */
    public GuiLogEntryString(String data, boolean fromStandard) {
        this(Instant.now(), data);
        this.fromStandard = fromStandard;
    }

    public GuiLogEntryString(Instant time, String data) {
        this.time = time;
        this.data = data;
    }

    public Instant getTime() {
        return time;
    }

    public String getData() {
        return data;
    }

    /**
     * @return indicates that the entry is created from standard output redirection. default is false
     * @since 1.1
     */
    public boolean isFromStandard() {
        return fromStandard;
    }

    @Override
    public String toString() {
        return "[" + time + "] " + data;
    }
}
