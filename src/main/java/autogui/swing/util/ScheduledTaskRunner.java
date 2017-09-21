package autogui.swing.util;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ScheduledTaskRunner<EventType> {
    /** msec */
    protected long delay;
    protected ScheduledExecutorService executor;
    protected Consumer<List<EventType>> consumer;

    protected List<EventType> accumulatedEvents = new ArrayList<>();
    protected ScheduledFuture<?> scheduledTask;

    protected boolean enabled = true;

    public ScheduledTaskRunner(long delay, Consumer<List<EventType>> consumer) {
        this.delay = delay;
        executor = Executors.newSingleThreadScheduledExecutor();
        this.consumer = consumer;
    }

    public Consumer<List<EventType>> getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer<List<EventType>> consumer) {
        this.consumer = consumer;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public synchronized void schedule(EventType event) {
        schedule(event, delay);
    }

    public synchronized void schedule(EventType event, long delayMSec) {
        if (enabled) {
            accumulatedEvents.add(event);
            if (scheduledTask == null) {
                scheduledTask = executor.schedule(this::run, delayMSec, TimeUnit.MILLISECONDS);
            }
        }
    }

    public synchronized void runImmediately(EventType event) {
        if (enabled) {
            accumulatedEvents.add(event);
            if (scheduledTask != null) {
                if (scheduledTask.cancel(false)) {
                    return;
                }
            }
            run();
        }
    }

    public synchronized void run() {
        consumer.accept(accumulatedEvents);
        accumulatedEvents = new ArrayList<>();
        scheduledTask = null;
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
    }

    public void shutdown() {
        this.executor.shutdown();
    }

    public static class EditingRunner extends ScheduledTaskRunner<Object>
            implements DocumentListener, KeyListener, ActionListener, FocusListener, ChangeListener {
        public EditingRunner(long delay, Consumer<List<Object>> consumer) {
            super(delay, consumer);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            schedule(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            schedule(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            schedule(e);
        }

        @Override
        public void keyTyped(KeyEvent e) {
            schedule(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            schedule(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            schedule(e);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //enter the key
            runImmediately(e);
        }

        @Override
        public void focusGained(FocusEvent e) {
            //nothing happen
        }

        @Override
        public void focusLost(FocusEvent e) {
            //leave the editing target
            runImmediately(e);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            schedule(e);
        }
    }
}
