package autogui.base.log;

/** a log-record issued by {@link GuiLogManager} */
public interface GuiLogEntry {
    /** @return true if the record is running and updating */
    default boolean isActive() {
        return false;
    }
}
