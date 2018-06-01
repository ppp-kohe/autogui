package autogui.base.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** a delayed task executor accumulating subsequent firing events while the specified delay */
public class ScheduledTaskRunner<EventType> {
    /** msec */
    protected long delay;
    protected ScheduledExecutorService executor;
    protected Consumer<List<EventType>> consumer;

    protected boolean shutdown;

    protected List<EventType> accumulatedEvents = new ArrayList<>();
    protected ScheduledFuture<?> scheduledTask;

    protected boolean enabled = true;

    public static ScheduledExecutorService sharedExecutor;
    public static AtomicInteger sharedCount = new AtomicInteger();
    static boolean debug = System.getProperty("autogui.util.debug", "false").equals("true");

    public ScheduledTaskRunner(long delay, Consumer<List<EventType>> consumer) {
        this.delay = delay;
        executor = getSharedExecutor();
        this.consumer = consumer;
        debugInit();
    }

    private void debugInit() {
        if (debug) {
            StringBuilder buf = new StringBuilder();
            buf.append("init ");
            buf.append(System.identityHashCode(this));
            int i = 0;
            for (StackTraceElement s : new RuntimeException("").getStackTrace()) {
                buf.append(", ");
                buf.append(s.getClassName()).append("::").append(s.getMethodName()).append(":").append(s.getLineNumber());
                if (i > 6) {
                    break;
                }
                ++i;
            }
            buf.append(" : ").append(consumer);
            System.err.println(buf);
        }
    }

    public ScheduledTaskRunner(long delay, Consumer<List<EventType>> consumer, ScheduledExecutorService executor) {
        this.delay = delay;
        this.executor = executor;
        this.consumer = consumer;
        debugInit();
    }



    public static ScheduledExecutorService getSharedExecutor() {
        if (sharedExecutor == null) {
            sharedExecutor = Executors.newScheduledThreadPool(4, new ThreadFactory() {
                ThreadFactory defaultFactory = Executors.defaultThreadFactory();
                @Override
                public Thread newThread(Runnable r) {
                    Thread th = defaultFactory.newThread(r);
                    th.setDaemon(true);
                    th.setName(ScheduledTaskRunner.class.getSimpleName() + "-" + th.getName());
                    return th;
                }
            });
        }
        sharedCount.incrementAndGet();
        return sharedExecutor;
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
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
            }
            scheduledTask = executor.schedule(this::run, delayMSec, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void runImmediately(EventType event) {
        if (enabled) {
            accumulatedEvents.add(event);
            if (scheduledTask != null) {
                if (!scheduledTask.cancel(false)) {
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

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public void shutdown() {
        if (executor == sharedExecutor) {
            if (!shutdown && sharedCount.decrementAndGet() <= 0) {
                executor.shutdown();
                shutdown = true;
            }
            debugShutdown();
        } else {
            getExecutor().shutdown();
        }
    }

    private void debugShutdown() {
        if (debug) {
            int n = sharedCount.get();
            if (n <= 0) {
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            System.err.println("sharedShutdown count " + n + " : " + System.identityHashCode(this) +
                    " shutdown=" + executor.isShutdown() + " terminated=" + executor.isTerminated());
        }
    }

}
