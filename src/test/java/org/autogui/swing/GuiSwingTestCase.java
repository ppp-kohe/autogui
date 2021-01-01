package org.autogui.swing;

import org.autogui.base.mapping.ScheduledTaskRunner;
import org.autogui.swing.util.EditingRunner;
import org.autogui.swing.util.PopupCategorized;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class GuiSwingTestCase {
    public JFrame createFrame(JComponent pane) {
        JFrame frame = new JFrame("test");
        {
            frame.add(pane);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
        }
        return frame;
    }

    public void runWait() {
        runWait(100);
    }

    public void runWait(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void run(Runnable r) {
        runGet(() -> { r.run(); return null; });
    }

    public <T> T runGet(final Callable<T> r) {
        try {
            Thread.sleep(300);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        final ArrayBlockingQueue<GuiResponse<T>> q = new ArrayBlockingQueue<>(1);
        SwingUtilities.invokeLater(() -> {
            try {
                q.add(new GuiResponse<>(r.call()));
            } catch (Throwable ex) {
                ex.printStackTrace();
                q.add(new GuiResponse<>(ex));
            }
        });
        try {
            Thread.sleep(200);
            GuiResponse<T> o = q.poll(10, TimeUnit.MINUTES);
            if (o == null) {
                throw new RuntimeException("timeout");
            } else if (o.exception != null) {
                throw new RuntimeException(o.exception);
            } else {
                return o.value;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static class GuiResponse<T> {
        public T value;
        public Throwable exception;

        public GuiResponse(T value) {
            this.value = value;
        }

        public GuiResponse(Throwable exception) {
            this.exception = exception;
        }
    }

    //////////////////// popup-menu specific methods

    public <ItemType extends PopupCategorized.CategorizedMenuItem> ItemType findMenuItem(
            Iterable<? extends PopupCategorized.CategorizedMenuItem> items,
            Class<ItemType> type, String category, String subCategory, String name,
            Predicate<ItemType> cond) {
        for (PopupCategorized.CategorizedMenuItem i : items) {
            if (type.isInstance(i)) {
                if (category != null && !i.getCategory().equals(category)) {
                    continue;
                }

                if (subCategory != null && !i.getCategory().equals(subCategory)) {
                    continue;
                }

                if (name != null && !i.getName().equals(name)) {
                    continue;
                }
                ItemType ret = type.cast(i);
                if (cond != null && !cond.test(ret)) {
                    continue;
                }
                return ret;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <ActionType extends Action> ActionType findMenuItemAction(
            Iterable<? extends PopupCategorized.CategorizedMenuItem> items,
            Class<ActionType> type, String category, String subCategory, String name) {
        PopupCategorized.CategorizedMenuItemActionDelegate d = findMenuItem(items,
                PopupCategorized.CategorizedMenuItemActionDelegate.class,
                category, subCategory, name, i -> type.isInstance(i.getAction()));
        if (d != null) {
            return type.cast(d.getAction());
        } else if (PopupCategorized.CategorizedMenuItemAction.class.isAssignableFrom(type)) {
            return type.cast(findMenuItem(items,
                    (Class<PopupCategorized.CategorizedMenuItemAction>) type, category, subCategory, name, null));
        } else {
            return null;
        }
    }


    public <ActionType extends Action> ActionType findMenuItemAction(
            Iterable<? extends PopupCategorized.CategorizedMenuItem> items,
            Class<ActionType> type) {
        return findMenuItemAction(items, type, null, null, null);
    }


    public String getClipboardText() {
        Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (board.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            try {
                return (String) board.getData(DataFlavor.stringFlavor);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            throw new RuntimeException("no string");
        }
    }

    public void setClipboardText(String str) {
        Clipboard board = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection se = new StringSelection(str);
        board.setContents(se, se);
    }

    public EditWait editWait(ScheduledTaskRunner<?> er) {
        return new EditWait(er);
    }

    public class EditWait {
        ScheduledTaskRunner<?> runner;
        public EditWait(ScheduledTaskRunner<?> r) {
            this.runner = r;
        }

        public void awaitNextFinish() {
            await(true);
            System.err.println("await(true): " + runner.hasScheduledTask());
            await(false);
            System.err.println("await(false): " + runner.hasScheduledTask());
        }

        public void await() {
            await(false);
        }

        public void await(boolean flag) {
            int i = 0;
            while (flag != runner.hasScheduledTask() && i < 20) {
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                }
                ++i;
            }
            runWait();
        }
    }
}
