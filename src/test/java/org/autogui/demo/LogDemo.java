package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.base.log.GuiLogEntryProgress;
import org.autogui.base.log.GuiLogManager;
import org.autogui.swing.AutoGuiShell;
import org.autogui.swing.log.GuiSwingLogManager;

@GuiIncluded
public class LogDemo {
    public static void main(String[] args) {
        GuiSwingLogManager.setDefaultReplace(false);
        AutoGuiShell.get().showWindow(new LogDemo());
    }

    @GuiIncluded
    public StringBuilder message = new StringBuilder("Hello, world");

    @GuiIncluded
    public int max = 10;

    @GuiIncluded(index = 1)
    public void showMessage() {
        if (GuiSwingLogManager.replaceErr) {
            System.err.println(message.toString());
        } else {
            GuiLogManager.get().logString(message.toString());
        }
    }

    @GuiIncluded(index = 2)
    public void showException() {
        if (GuiSwingLogManager.replaceExceptionHandler) {
            GuiLogManager.get().logError(new RuntimeException(message.toString()));
        } else {
            throw new RuntimeException(message.toString());
        }
    }

    @GuiIncluded(index = 3)
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

}
