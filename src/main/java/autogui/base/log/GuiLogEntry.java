package autogui.base.log;

/** a log-record */
public interface GuiLogEntry {
    /** the record is running and updating */
    default boolean isActive() {
        return false;
    }
}
