package org.autogui.base.log;

import java.time.Instant;

/**
 * the class is not a throwable exception class, instead
 *  this is a log-entry class for an exception object.
 * */
public class GuiLogEntryException implements GuiLogEntry {
    protected Instant time;
    protected Throwable exception;

    public GuiLogEntryException(Throwable exception) {
        this(Instant.now(), exception);
    }

    public GuiLogEntryException(Instant time, Throwable exception) {
        this.time = time;
        this.exception = exception;
    }

    public Instant getTime() {
        return time;
    }

    public Throwable getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "[" + getTime() + "] !!! " + getException();
    }
}
