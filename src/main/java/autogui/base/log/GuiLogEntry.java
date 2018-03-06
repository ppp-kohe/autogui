package autogui.base.log;

/** a log-record */
public interface GuiLogEntry {
    /** @return true if the record is running and updating */
    default boolean isActive() {
        return false;
    }
}
