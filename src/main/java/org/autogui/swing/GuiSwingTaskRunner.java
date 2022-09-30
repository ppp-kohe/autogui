package org.autogui.swing;

import org.autogui.base.log.GuiLogEntryProgress;
import org.autogui.base.log.GuiLogManager;
import org.autogui.base.mapping.GuiMappingContext;
import org.autogui.swing.util.SwingDeferredRunner;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** a task runner for deferring a task takes long time to complete.
 * basically, the methods of GuiRepresentation need to be executed
 *   under the thread of the task runner of {@link GuiMappingContext#getTaskRunner()}.
 *  {@link #executeContextTask(Supplier, Consumer)} achieves this.
 *  <pre>
 *      new GuiSwingTaskRunner(context).executeContextTask(
 *          () -&gt; context.getReprValue().getUpdatedValue(context, spec),
 *          r  -&gt; r.executeIfPresented(v -&gt;
 *                      SwingUtilities.invokeLater(() -&gt; setSwingViewValue(v))))
 *  </pre>
 *
 *  */
public class GuiSwingTaskRunner {

    protected GuiMappingContext context;

    public GuiSwingTaskRunner(GuiMappingContext context) {
        this.context = context;
    }

    public GuiMappingContext getContext() {
        return context;
    }

    /**
     * run task on the task-runner of the context ({@link GuiMappingContext#getTaskRunner()}).
     * <ul>
     *   <li>if the task takes a long time to complete,
     *         the method will return with {@link ContextTaskResult#isTimeout()}==true,
     *         and start to wait the completion of the task on a thread of {@link SwingDeferredRunner#getDefaultService()}.</li>
     *   <li>if the task causes an {@link InterruptedException},
     *     the method will returns with {@link ContextTaskResult#isCancel()}==true.</li>
     *
     *   <li>After the completion, afterTask will be executed.</li>
     *   <li>
     *   The afterTask also be executed when a timeout or an interruption  happens:
     *     after the timeout task completion, the afterTask will be executed again
     *      with returned value wrapped by a result value with {@link ContextTaskResult#isPresentedWithDelay()}==true.
     *      <ul>
     *      <li>
     *     While waiting after timeout, the user may click the cancel button of the task.
     *      This may cause an {@link InterruptedException} and then the afterTask is executed with {@link ContextTaskResult#isCancel()}==true
     *       </li>
     *       </ul>
     *    </li>
     *    <li>
     *     In most cases, the method is executed from the event-dispatching thread.
     *      Thus, the task should not use {@link SwingUtilities#invokeAndWait(Runnable)}.
     *       To check the condition, use {@link SwingDeferredRunner#isEventThreadOrDispatchedFromEventThread()}.
     *       The method turns on the flag {@link SwingDeferredRunner#dispatchedFromEventThread} while running the task.
     *    </li>
     *   </ul>
     * @param task the executed task.
     * @param afterTask a task executed after the task. nullable
     * @param <RetType> the value type of the task
     * @return the result status of the task: cancel, timeout or returned
     */
    public <RetType> ContextTaskResult<RetType> executeContextTask(Supplier<RetType> task, Consumer<ContextTaskResult<RetType>> afterTask) {
        return executeContextTask(true, task, afterTask);
    }

    /**
     * {@link #executeContextTask(Supplier, Consumer)} with additional flag for task-runner:
     *  the method is used for custom checking of the runner submission like collection-table: the context of the runner is not the actual context of the task.
     * @param useTaskRunner if false, it will be executed by {@link #executeContextTaskWithContext(Supplier, Consumer)}
     * @param task the running task
     * @param afterTask a task executed after the task. nullable: if null, the method might cause an exception. otherwise, the consumer becomes a finally-block.
     * @return the result status of the task: cancel, timeout or returned
     * @param <RetType> the value type of the task
     * @since 1.6
     */
    public <RetType> ContextTaskResult<RetType> executeContextTask(boolean useTaskRunner, Supplier<RetType> task, Consumer<ContextTaskResult<RetType>> afterTask) {
        if (!useTaskRunner || context == null || !context.getRepresentation().isTaskRunnerUsedFor(task)) {
            return executeContextTaskWithContext(task, afterTask);
        } else {
            GuiMappingContext.ContextExecutorService taskRunner = context.getTaskRunner();
            Future<RetType> ret = taskRunner.submit(taskWithThreadFlag(task));
            try {
                RetType value = ret.get(2, TimeUnit.SECONDS);
                ContextTaskResult<RetType> retValue = new ContextTaskResult<>(value);
                if (afterTask != null) {
                    afterTask.accept(retValue);
                }
                return retValue;

            } catch (ExecutionException ex) {
                if (afterTask != null) {
                    afterTask.accept(new ContextTaskResultFailException<>(ex));
                    return null;
                } else {
                    throw new RuntimeException(ex);
                }
            } catch (InterruptedException ie) {
                return fail(false, afterTask);

            } catch (TimeoutException timeout) {
                ContextTaskResult<RetType> retValue = fail(true, afterTask);
                getFutureWaiter().execute(() -> waitCompletion(ret, afterTask));
                return retValue;
            }
        }
    }

    private <RetType> Callable<RetType> taskWithThreadFlag(Supplier<RetType> task) {
        boolean eventThread = SwingDeferredRunner.isEventThreadOrDispatchedFromEventThread();
        return () -> {
            try {
                SwingDeferredRunner.dispatchedFromEventThread.set(eventThread);
                return task.get();
            } finally {
                SwingDeferredRunner.dispatchedFromEventThread.set(false);
            }
        };
    }

    private <RetType> void waitCompletion(Future<RetType> ret, Consumer<ContextTaskResult<RetType>> afterTask) {
        GuiLogEntryProgress p = GuiLogManager.get().logProgress();
        p.setIndeterminate(true);
        p.setMessage("executing...");
        try {
            RetType value = ret.get();
            if (afterTask != null) {
                afterTask.accept(new ContextTaskResultWithDelay<>(value));
            }
        } catch (InterruptedException ie) {
            GuiLogManager.get().logString("cancelling...");
            p.setMessage("cancelling...");
            ret.cancel(true);
            fail(false, afterTask);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        } finally {
            if (ret.isDone()) {
                p.setMessage("completed");
            }
            p.finish();
        }
    }

    public <RetType> ContextTaskResultFail<RetType> fail(boolean timeout, Consumer<ContextTaskResult<RetType>> afterTask) {
        ContextTaskResultFail<RetType> retValue = new ContextTaskResultFail<>(timeout);
        if (afterTask != null) {
            afterTask.accept(retValue);
        }
        return retValue;
    }

    public ExecutorService getFutureWaiter() {
        return SwingDeferredRunner.getDefaultService();
    }

    public static <RetType> ContextTaskResult<RetType> executeContextTaskWithContext(Supplier<RetType> task, Consumer<ContextTaskResult<RetType>> afterTask) {
        try {
            ContextTaskResult<RetType> ret = new ContextTaskResult<>(task.get());
            if (afterTask != null) {
                afterTask.accept(ret);
            }
            return ret;
        } catch (Throwable ex) {
            if (afterTask != null) {
                afterTask.accept(new ContextTaskResultFailException<>(ex));
                return null;
            } else {
                throw ex;
            }
        }
    }

    /** the returned value for successfully obtaining a value
     * @param <RetType> the type of the value
     */
    public static class ContextTaskResult<RetType> {
        protected RetType value;

        public ContextTaskResult(RetType value) {
            this.value = value;
        }

        public RetType getValue() {
            return value;
        }

        public boolean isCancel() {
            return false;
        }

        public boolean isTimeout() {
            return false;
        }

        public boolean isPresentedWithDelay() {
            return false;
        }

        /**
         * @return true if the value is an error
         * @since 1.6.1
         */
        public boolean isError() {
            return false;
        }

        /**
         * @return true if no-cancellation, no-timeout, immediately returned
         */
        public boolean isPresented() {
            return !isTimeout() && !isCancel() && !isPresentedWithDelay();
        }

        /**
         * executes the task with it's obtained value (both immediate and delayed cases)
         * @param task to be executed with the value
         */
        public void executeIfPresent(Consumer<RetType> task) {
            task.accept(value);
        }

        /**
         * executes the task if delayed (nothing happen with the class)
         * @param task to be executed with the value
         */
        public void executeIfPresentWithDelay(Consumer<RetType> task) {
            //nothing
        }

        public RetType getValueOr(RetType cancelValue, RetType timeoutValue) {
            return value;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(value=" + value +
                    ",cancel=" + isCancel() + ",timeout=" + isTimeout() + ",presentedWithDelay=" + isPresentedWithDelay() +
                    ",presented=" + isPresented() + ")";
        }
    }

    /**
     * delayed result
     * @param <RetType> the type of the value
     */
    public static class ContextTaskResultWithDelay<RetType> extends ContextTaskResult<RetType> {
        public ContextTaskResultWithDelay(RetType value) {
            super(value);
        }

        @Override
        public boolean isPresentedWithDelay() {
            return true;
        }

        /**
         * executes the task with a obtained value with delay
         * @param task to be executed with the value
         */
        @Override
        public void executeIfPresentWithDelay(Consumer<RetType> task) {
            task.accept(value);
        }
    }

    /**
     * a task failure by delay
     * @param <RetType> the type of the value
     */
    public static class ContextTaskResultFail<RetType> extends ContextTaskResult<RetType> {
        protected boolean timeout;

        public ContextTaskResultFail(boolean timeout) {
            super(null);
            this.timeout = timeout;
        }

        @Override
        public boolean isCancel() {
            return !timeout;
        }

        @Override
        public boolean isTimeout() {
            return timeout;
        }

        /**
         * the value is null and thus the method does nothing
         * @param task to be executed with the value
         */
        @Override
        public void executeIfPresent(Consumer<RetType> task) {
            //nothing
        }

        /**
         * @param cancelValue the value for cancellation
         * @param timeoutValue  the value for timeout
         * @return cancelValue or timeoutValue
         */
        @Override
        public RetType getValueOr(RetType cancelValue, RetType timeoutValue) {
            return timeout ? timeoutValue : cancelValue;
        }
    }

    /**
     * the failure status of execution with an exception
     * @param <RetType> the missing value type
     * @since 1.6.1
     */
    public static class ContextTaskResultFailException<RetType> extends ContextTaskResult<RetType> {
        protected Throwable error;

        public ContextTaskResultFailException(Throwable error) {
            super(null);
            this.error = error;
        }

        @Override
        public boolean isError() {
            return true;
        }

        public Throwable getError() {
            return error;
        }
    }

    /** a base class for actions */
    public static class ContextAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        protected GuiSwingTaskRunner taskRunner;

        public ContextAction(GuiMappingContext context) { //context may be null
            taskRunner = new GuiSwingTaskRunner(context);
        }

        public GuiMappingContext getContext() {
            return taskRunner.getContext();
        }


        public <RetType> ContextTaskResult<RetType> executeContextTask(Supplier<RetType> task, Consumer<ContextTaskResult<RetType>> afterTask) {
            return taskRunner.executeContextTask(task, afterTask);
        }

        /**
         * @param useTaskRunner controls task submission
         * @param task the executed task
         * @param afterTask after task. nullable.
         * @return the result of the task
         * @param <RetType> the result type
         * @since 1.6
         */
        public <RetType> ContextTaskResult<RetType> executeContextTask(boolean useTaskRunner, Supplier<RetType> task, Consumer<ContextTaskResult<RetType>> afterTask) {
            return taskRunner.executeContextTask(useTaskRunner, task, afterTask);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException();
        }
    }
}
