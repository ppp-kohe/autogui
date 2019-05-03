package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.GuiNotifierSetter;
import org.autogui.swing.AutoGuiShell;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@GuiIncluded
public class BackgroundUpdateDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new BackgroundUpdateDemo());
    }

    Instant time = Instant.now();
    AtomicInteger count = new AtomicInteger();
    List<BackgroundRun> runners = new ArrayList<>();

    Runnable notifier;

    @GuiIncluded
    public List<BackgroundRun> getRunners() {
        return runners;
    }

    @GuiIncluded(history = false)
    public String getStatus() {
        int size = runners.size();
        Instant t;
        synchronized (this) {
            t = time;
        }
        return String.format("count: %d, runs: %d, time: %s", count.get(), size, t);
    }

    @GuiIncluded
    public void start() {
        runners = new ArrayList<>(runners);
        BackgroundRun r = new BackgroundRun(this);
        Thread th = new Thread(r);
        th.start();
        runners.add(r);
    }

    @GuiIncluded
    public synchronized void update() {
        count.incrementAndGet();
        time = Instant.now();
        if (notifier != null) {
            notifier.run();
        }
    }

    @GuiNotifierSetter
    public synchronized void setNotifier(Runnable notifier) {
        this.notifier = notifier;
        System.err.println("setNotifier : " + notifier);
    }

    @GuiIncluded
    public static class BackgroundRun implements Runnable {
        AtomicInteger count = new AtomicInteger();
        Instant time = Instant.now();
        AtomicBoolean running = new AtomicBoolean(true);
        Thread thread;

        BackgroundUpdateDemo owner;

        public BackgroundRun(BackgroundUpdateDemo owner) {
            this.owner = owner;
        }

        @GuiIncluded(history = false)
        public String getStatus() {
            return String.format("[%s] count: %d  time: %s", (running.get() ? "R" : " "), count.get(), getTime());
        }

        public synchronized Instant getTime() {
            return time;
        }

        private synchronized void setTime(Instant time) {
            this.time = time;
        }

        @Override
        public void run() {
            try {
                thread = Thread.currentThread();
                for (int i = 0; i < 100; ++i) {
                    count.incrementAndGet();
                    owner.update();
                    setTime(Instant.now());
                    Thread.sleep(1000);
                }
            } catch (InterruptedException ex) {
                System.err.println("interrupt " + Thread.currentThread());
            } finally {
                running.set(false);
            }
        }

        @GuiIncluded
        public void stop() {
            thread.interrupt();
        }
    }


}
