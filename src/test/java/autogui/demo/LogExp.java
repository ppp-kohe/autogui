package autogui.demo;

import autogui.GuiIncluded;
import autogui.base.log.GuiLogEntryProgress;
import autogui.base.log.GuiLogManager;
import autogui.swing.AutoGuiShell;

@GuiIncluded
public class LogExp {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new LogExp());
    }

    @GuiIncluded
    public StringBuilder message = new StringBuilder();

    @GuiIncluded
    public int max = 10;

    @GuiIncluded
    public void showMessage() {
        System.err.print(message.toString()); //GUI based StringBuilder always has a new-line at the end of the data
    }

    @GuiIncluded
    public void showProgress() {
        final int max = this.max;
        new Thread(() -> {
            try (GuiLogEntryProgress p = GuiLogManager.get().logProgress(max)) {
                for (int i = 0; i < max; ++i) {
                    p.addValue(1);
                    p.setMessage("progress " + i);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        p.setMessage("interrupted");
                        break;
                    }
                }
            }
        }).start();
    }

    @GuiIncluded
    public void showException() {
        throw new RuntimeException(message.toString());
    }
}
