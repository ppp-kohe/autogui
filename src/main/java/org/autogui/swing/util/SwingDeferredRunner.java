package org.autogui.swing.util;

import org.autogui.base.mapping.ScheduledTaskRunner;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** a task runner in the event dispatching thread without blocking
 * <pre>
 *   try {
 *      Object ret = SwingDeferredRunner.run(task);
 *      if (ret instanceof {@link TaskResultFuture}) {
 *         Future&lt;Object&gt; f = ((TaskResultFuture) ret).getFuture();
 *          ...
 *      } else {
 *          ...
 *      }
 *   } catch(Throwable errorWhileTask) { ... }
 * </pre>
 * */
public class SwingDeferredRunner {
    protected LinkedBlockingQueue<Object> result = new LinkedBlockingQueue<>();
    protected Task task;
    protected Supplier<ExecutorService> futureFactory;

    public static ThreadLocal<Boolean> dispatchedFromEventThread = ThreadLocal.withInitial(() -> false);

    public static boolean isEventThreadOrDispatchedFromEventThread() {
        return SwingUtilities.isEventDispatchThread() || isDispatchedFromEventThread();
    }

    public static boolean isDispatchedFromEventThread() {
        return dispatchedFromEventThread.get();
    }

    /**
     * run the task by {@link SwingUtilities#invokeLater(Runnable)} with wrapping by 
     *  {@link ScheduledTaskRunner#depthRunner(Runnable)}
     * @param r the task
     */
    public static void invokeLater(Runnable r) {
        SwingUtilities.invokeLater(ScheduledTaskRunner.depthRunner(r));
    }

    /**
     *  run the task by {@link SwingUtilities#invokeAndWait(Runnable)} with wrapping by
     *   {@link ScheduledTaskRunner#withDepthInfo(String, Runnable)} and 
     *    {@link ScheduledTaskRunner#depthRunner(Runnable)}
     * @param r the task
     * @throws InterruptedException from invokeWait
     * @throws InvocationTargetException from invokeWait
     */
    public static void invokeAndWait(Runnable r) throws InterruptedException, InvocationTargetException {
        try {
            ScheduledTaskRunner.withDepthInfo("invokeAndWait", (Callable<Void>) () -> {
                SwingUtilities.invokeAndWait(ScheduledTaskRunner.depthRunner(r));
                return null;
            });
        } catch (InvocationTargetException|InterruptedException ite) {
            throw ite;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static ExecutorService defaultService;

    public static ExecutorService getDefaultService() {
        synchronized (SwingDeferredRunner.class) {
            if (defaultService == null) {
                defaultService = Executors.newCachedThreadPool(new ThreadFactory() {
                    final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread th = defaultFactory.newThread(r);
                        th.setDaemon(true);
                        th.setName(SwingDeferredRunner.class.getSimpleName() + "-" + th.getName());
                        return th;
                    }
                });
            }
        }
        return defaultService;
    }

    public static void cleanDefaultService() {
        synchronized (SwingDeferredRunner.class) { //there is a possibility that get().submit(t) in run() causes positing t to a shutting-down-service
            if (defaultService != null) {
                defaultService.shutdown();
                defaultService = null;
            }
        }
    }

    public static Object run(Task task) throws Throwable {
        return new SwingDeferredRunner(SwingDeferredRunner::getDefaultService, task).run();
    }

    public SwingDeferredRunner(Supplier<ExecutorService> futureFactory, Task task) {
        this.task = task;
        this.futureFactory = futureFactory;
    }

    /**
     * execute the specified task on the event dispatching thread and return the result.
     *  if the current thread is the event dispatching thread, then it runs immediately.
     *  otherwise, submit the task to the event dispatching thread,
     *     and wait a little while and return a future value if it is not completed.
     * @return the result of the task (nullable) if no-delaying,
     *     or {@link TaskResultFuture} submitted to the futureFactory
     * @throws Throwable if the task throws an exception without delay, then it is thrown
     */
    public Object run() throws Throwable {
        if (SwingDeferredRunner.isEventThreadOrDispatchedFromEventThread()) {
            return task.call();
        } else {
            ScheduledTaskRunner.withDepthInfo("deferredRunner", () ->
                        SwingDeferredRunner.invokeLater(() -> {
                            try {
                                Object r = task.call();
                                if (r == null) {
                                    r = TASK_RESULT_NULL;
                                }
                                result.put(r);
                            } catch (Throwable t) {
                                try {
                                    result.put(new TaskResultError(t));
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        }));
            Object res = result.poll(300, TimeUnit.MILLISECONDS);
            if (res == null) {
                //timeout
                return new TaskResultFuture(futureFactory.get().submit(result::take));
            } else if (res instanceof TaskResultError) {
                throw ((TaskResultError) res).getException();
            } else if (res.equals(TASK_RESULT_NULL)) {
                return null;
            } else {
                return res;
            }
        }
    }

    public static Object TASK_RESULT_NULL = new Object();

    /**
     * a function interface under the event dispatching thread
     */
    public interface Task {
        Object call() throws Throwable;
    }

    /** a result for a thrown exception:
     *  {@link TaskResultFuture} return the value if the task failed with an exception */
    public static class TaskResultError {
        protected Throwable exception;

        public TaskResultError(Throwable exception) {
            this.exception = exception;
        }

        public Throwable getException() {
            return exception;
        }
    }

    /** a incomplete result */
    public static class TaskResultFuture {
        protected Future<Object> future;

        public TaskResultFuture(Future<Object> future) {
            this.future = future;
        }

        public Future<Object> getFuture() {
            return future;
        }
    }

}
