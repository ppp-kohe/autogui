package org.autogui.base.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** a delayed task executor accumulating subsequent firing events while the specified delay
 *  <pre>
 *      ScheduledTaskRunner&lt;E&gt; r = new ScheduledTaskRunner&lt;&gt;(300, runner);
 *      r.schedule(e1); //start a task with waiting 300 msec
 *      r.schedule(e2); //cancel the previous task and start another new task with waiting 300 msec
 *      sleep(300);
 *      //the runner will be dispatched with a list of [e1, e2]
 *  </pre>
 *
 * */
public class ScheduledTaskRunner<EventType> {
    /** msec */
    protected long delay;
    protected ScheduledExecutorService executor;
    /** true if the {@link #executor} is {@link #sharedExecutor}.
     * @since 1.2 */
    protected boolean executorIsShared;
    protected Consumer<List<EventType>> consumer;

    protected List<EventType> accumulatedEvents = new ArrayList<>();
    protected ScheduledFuture<?> scheduledTask;

    protected boolean enabled = true;

    public static ScheduledExecutorService sharedExecutor;
    public static AtomicInteger sharedCount = new AtomicInteger();
    static boolean debug = System.getProperty("autogui.base.debug", "false").equals("true");

    /**
     * creates a new task-runner with {@link #sharedExecutor}.
     * @param delay the delay milli-seconds for launching a task
     * @param consumer the actual task runner as taking accumulated events
     */
    public ScheduledTaskRunner(long delay, Consumer<List<EventType>> consumer) {
        this.delay = delay;
        setExecutor(getSharedExecutor());
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
        setExecutor(executor);
        this.consumer = consumer;
        debugInit();
    }

    /**
     * init the executor to the runner.
     * if the executor is {@link #sharedExecutor}, {@link #executorIsShared} becomes true.
     * Basically the executor will not be changed, but {@link #shutdown()} might cause renewal of {@link #sharedExecutor}.
     *  Then {@link #schedule(Object, long)} will call the method again for obtaining a new {@link #sharedExecutor}.
     * @param executor the executor to be set
     * @since 1.2
     */
    protected void setExecutor(ScheduledExecutorService executor) {
        runForShared(() -> {
            this.executor = executor;
            executorIsShared = (executor == sharedExecutor);
        });
    }

    /**
     * execute a task with synchronization for handling {@link #sharedExecutor}
     * @param task a task with synchronization
     * @since 1.2
     */
    public static void runForShared(Runnable task) {
        synchronized (ScheduledTaskRunner.class) {
            task.run();
        }
    }

    public static ScheduledExecutorService getSharedExecutor() {
        runForShared(() -> {
            if (sharedExecutor != null && sharedExecutor.isShutdown()) {
                sharedCount.set(0);
                sharedExecutor = null;
            }
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
        });
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

    /**
     * if {@link #enabled}, schedules a task for processing the event.
     *  if another task is already scheduled, then the old task will be canceled.
     *  So, subsequent tasks cause extending the delayed time.
     *  <p>
     *  If the {@link #executor} have been shutdown and the executor is the {@link #sharedExecutor},
     *    then it will obtain a new shared executor by {@link #getSharedExecutor()}.
     *    This means the method causes reincarnation of the shared executor.
     * @param event an accumulated event processed by a delayed task
     * @param delayMSec delaying milliseconds
     */
    public synchronized void schedule(EventType event, long delayMSec) {
        if (enabled) {
            accumulatedEvents.add(event);
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
            }
            try {
                scheduledTask = executor.schedule(this::run, delayMSec, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException re) {
                if (executorIsShared) {
                    setExecutor(getSharedExecutor()); //if shutdown, sharedCount is 0
                    scheduledTask = executor.schedule(this::run, delayMSec, TimeUnit.MILLISECONDS);
                } else {
                    throw re;
                }
            }
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
        if (executorIsShared) {
            if (sharedCount.decrementAndGet() <= 0 && !executor.isShutdown()) {
                executor.shutdown();
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

    /**
     * @return true if it has awaiting task
     * @since 1.3
     */
    public synchronized boolean hasScheduledTask() {
        return scheduledTask != null;
    }
}
