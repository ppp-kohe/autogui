package autogui.base.log;

public interface GuiLogEntry {
    default boolean isActive() {
        return false;
    }
}
