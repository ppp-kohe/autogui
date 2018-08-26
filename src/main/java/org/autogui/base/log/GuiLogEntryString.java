package org.autogui.base.log;

import java.time.Instant;

/** a log-entry for a string */
public class GuiLogEntryString implements GuiLogEntry {
    protected Instant time;
    protected String data;

    public GuiLogEntryString(String data) {
        this(Instant.now(), data);
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

    @Override
    public String toString() {
        return "[" + time + "] " + data;
    }
}
