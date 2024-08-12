package org.autogui.base.mapping;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
@SuppressWarnings("this-escape")
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

    protected volatile boolean enabled = false;

    public static ScheduledExecutorService sharedExecutor;
    public static AtomicInteger sharedCount = new AtomicInteger();
    static boolean debug = System.getProperty("autogui.base.debug", "false").equals("true");

    /**
     * creates a new task-runner with {@link #sharedExecutor}.
     * @param delay the delay milliseconds for launching a task
     * @param consumer the actual task runner as taking accumulated events
     */
    public ScheduledTaskRunner(long delay, Consumer<List<EventType>> consumer) {
        this.delay = delay;
        setExecutor(getSharedExecutor());
        this.consumer = consumer;
        debugInit();
        enabled = initEnabled();
    }

    protected boolean initEnabled() {
        return true;
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
        enabled = initEnabled();
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
                    final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

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
                depthStack.pendingTasks.decrementAndGet();
                scheduledTask.cancel(false);
            }
            withDepthInfo("event(" + event + ")", () -> {
                try {
                    scheduledTask = executor.schedule(depthRunner(this::run), delayMSec, TimeUnit.MILLISECONDS);
                } catch (RejectedExecutionException re) {
                    if (executorIsShared) {
                        setExecutor(getSharedExecutor()); //if shutdown, sharedCount is 0
                        scheduledTask = executor.schedule(depthRunner(this::run), delayMSec, TimeUnit.MILLISECONDS);
                    } else {
                        throw re;
                    }
                }
            });
        }
    }

    /** non-null depth-stack */
    protected static DepthStack depthStack = new DepthStack();

    /** debug features for task-running:
     * the system-property {@systemProperty org.autogui.base.mapping.debugDepth} can be
     *  <ul>
     *      <li>{@code err} : specifies the output to {@link System#err}, or</li>
     *      <li>a file-path : specifies the output to the file.</li>
     *  </ul>
     *  <p>
     *     {@snippet :
     *       withDepthInfo("contextInfo", //push the string to the thread-local stack
     *         () -> executor.execute(depthRunner(task))) // copy the thread-local stack and the wrapper of task hold it.
     *          //when the task is run by the executor-thread, the stack-info will be set the the thread-local stack
     *     }
     *  </p>
     * */
    public static class DepthStack {
        protected ThreadLocal<List<String>> depthStack = ThreadLocal.withInitial(ArrayList::new);
        protected PrintStream depthDebug;
        protected AtomicInteger depthDebugMax = new AtomicInteger();
        protected AtomicInteger pendingTasks = new AtomicInteger();
        protected AtomicInteger startedTasks = new AtomicInteger();
        protected AtomicInteger debugLastTasks = new AtomicInteger();

        public DepthStack() {
            var str = System.getProperty("org.autogui.base.mapping.debugDepth", "");
            if (str.equals("err")) {
                System.err.printf("debugDebug: err%n");
                depthDebug = System.err;
            } else if (!str.isEmpty()) {
                try {
                    System.err.printf("debugDebug: file '%s'%n", str);
                    var out = Files.newOutputStream(Paths.get(str), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                    depthDebug = new PrintStream(out, true);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        /**
         * @return the current stack info
         */
        public List<String> getDepthStack() {
            return new ArrayList<>(depthStack.get());
        }

        /**
         * run the body with a new context with the info
         * @param info the context info, pushed to the stack
         * @param body the task body
         */
        public void withDepthInfo(String info, Runnable body) {
            try {
                withDepthInfo(info, (Callable<Void>) () -> {
                    body.run();
                    return null;
                });
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        /**
         * run the body with a new text with the info
         * @param info  the context info, pushed to the stack
         * @param body the task body
         * @return the returned value of the body
         * @param <T> the retuned type
         * @throws Exception thrown by the body
         */
        public <T> T withDepthInfo(String info, Callable<T> body) throws Exception {
            var stack = depthStack.get();
            stack.add(info);
            var d = depthDebug;
            if (d != null && depthDebugMax.get() < stack.size()) {
                depthDebugMax.set(stack.size());
                d.printf("[%s] queue[%,d/%,d] stack[%,d]%s%n", Instant.now(),
                        startedTasks.get(), pendingTasks.get(), stack.size(), String.join( ", ", stack));
                d.flush();
            }
            try {
                return body.call();
            } finally {
                depthStack.get().removeLast();
            }
        }

        /** if enabled the debugging output, it checks pending-tasks and writes a log-message if some condition satifeed;
         *  abs(pendingTasks-(previous pendingTasks)) &gt; 100 or pendingTasks=0 */
        public void checkQueue() {
            var d  = depthDebug;
            var c = pendingTasks.get();
            var l = debugLastTasks.get();
            if (d != null && (Math.abs(c - l) > 100 || (l == 0 && c != 0))) {
                debugLastTasks.set(c);

                d.printf("[%s] queue[%,d/%,d] {%s}%n", Instant.now(), startedTasks.get(), c,
                        Arrays.stream(Thread.currentThread().getStackTrace()).map(Objects::toString).collect(Collectors.joining(", ")));
                d.flush();
            }
        }

        /**
         * @param r the task body
         * @return wrappend body by {@link TaskWithContext}
         */
        public Runnable depthRunner(Runnable r) {
            checkQueue();
            return new TaskWithContext<Void>(() -> { r.run(); return null; }, getDepthStack());
        }

        /**
         * @param r the task body
         * @return wrapped body by {@link TaskWithContext}
         * @param <T> the task returned type
         */
        public <T> Callable<T> depthRunner(Callable<T> r) {
            checkQueue();
            return new TaskWithContext<>(r, getDepthStack());
        }

        /**
         * @param r the task body
         * @return wrapped body by {@link TaskWithContext}
         * @param <T> the task returned type
         */
        public <T> Supplier<T> depthRunner(Supplier<T> r) {
            checkQueue();
            return new TaskWithContext<>(r::get, getDepthStack());
        }
    }

    /**
     * @see DepthStack#withDepthInfo(String, Runnable) 
     */
    public static void withDepthInfo(String info, Runnable body) {
        depthStack.withDepthInfo(info, body);
    }

    /**
     * @see DepthStack#withDepthInfo(String, Callable) 
     */
    public static <T> T withDepthInfo(String info, Callable<T> body) throws Exception {
        return depthStack.withDepthInfo(info, body);
    }

    /**
     * @see DepthStack#depthRunner(Runnable) 
     */
    public static Runnable depthRunner(Runnable r) {
        return depthStack.depthRunner(r);
    }

    /**
     * @see DepthStack#depthRunner(Callable) 
     */
    public static <T> Callable<T> depthRunner(Callable<T> r) {
        return depthStack.depthRunner(r);
    }

    /**
     * @see DepthStack#depthRunner(Supplier)
     */
    public static <T> Supplier<T> depthRunner(Supplier<T> r) {
        return depthStack.depthRunner(r);
    }

    /**
     * a task wrapper for {@link DepthStack}
     * @param <T> the returned type
     */
    public static class TaskWithContext<T> implements Callable<T>, Runnable, Supplier<T> {
        protected Callable<T> task;
        protected List<String> contextInfo;

        public TaskWithContext(Callable<T> task, List<String> contextInfo) {
            this.task = task;
            this.contextInfo = contextInfo;
            depthStack.pendingTasks.incrementAndGet();
        }

        /**
         * @return a copy of the context-info
         */
        public List<String> getContextInfo() {
            return contextInfo;
        }

        @Override
        public T get() {
            try {
                return call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void run() {
            try {
                call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public T call() throws Exception {
            depthStack.startedTasks.incrementAndGet();
            var old = depthStack.depthStack.get();
            try {
                depthStack.depthStack.set(contextInfo);
                return task.call();
            } finally {
                depthStack.depthStack.set(old);
                depthStack.pendingTasks.decrementAndGet();
                depthStack.startedTasks.decrementAndGet();
                depthStack.checkQueue();
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

    public void run() {
        List<EventType> eventsCopy;
        synchronized (this) {
            eventsCopy = new ArrayList<>(accumulatedEvents);
            accumulatedEvents = new ArrayList<>();
            scheduledTask = null;
        }
        try {
            consumer.accept(eventsCopy); //the events list should be only handled within synchronized blocks, but...
        } catch (Throwable ex) { //the schduled-executors do not output any errors (but hold them within Future)
            handleException(ex);
        }
    }

    public void handleException(Throwable ex) {
        ex.printStackTrace();
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
