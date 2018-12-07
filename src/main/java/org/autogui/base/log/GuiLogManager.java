package org.autogui.base.log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/** the base class for log managers
 * <pre>
 *     GuiLogManager.get().log("hello");
 *
 *     GuiLogManager.get().logFormat("%d", 123);
 *
 *     try { ... } catch (Exception ex) { GuiLogManager.get().logError(ex); }
 *
 *     try (GuiLogEntryProgress p = GuiLogManager.get().logProgress()) { //see {@link #logProgress()}
 *         ...
 *     }
 * </pre>
 * */
public class GuiLogManager {
    protected static GuiLogManager manager;

    /** @return a shared instance */
    public static GuiLogManager get() {
        synchronized (GuiLogManager.class) {
            if (manager == null) {
                manager = new GuiLogManagerConsole();
            }
            return manager;
        }
    }

    public static void setManager(GuiLogManager manager) {
        synchronized (GuiLogManager.class) {
            GuiLogManager.manager = manager;
        }
    }

    /**
     * create and report a log entry.
     * args are concatenated with a space and pass to {@link #logString(String)}.
     *    if the previous arg is a String and it ends with an ASCII symbol char, it will not insert a space.
     *    <pre>
     *        log("hello", "world") =&gt; logString("hello world")
     *        log("hello:", "world") =&gt; logString("hello:world")
     *        log("x=", 1, "y=", 2, "ex", new Exception())
     *           =&gt; logString("x=1 y=2 ex") and logError(new Exception())
     *    </pre>
     * when the last item of args is an exception, it will be passed to {@link #logError(Throwable)}.
     *    if the args is only a single exception, it will returns its {@link GuiLogEntryException},
     *      otherwise {@link GuiLogEntryString}
     * @param args concatenated values for creating a log entry.
     * @return the created log entry
     *      */
    public GuiLogEntry log(Object... args) {
        Formatter formatter = new Formatter();
        boolean needSpace = false;
        int idx = 0;
        Throwable lastException = null;
        for (Object arg : args) {
            if (idx + 1 == args.length && arg instanceof Throwable) {
                lastException = (Throwable) arg;
                break;
            }
            if (needSpace) {
                formatter.format(" %s", arg);
            } else {
                formatter.format("%s", arg);
            }
            if (arg instanceof String) {
                String str = (String) arg;
                needSpace = !(!str.isEmpty() && isSymbol(str.charAt(str.length() - 1)));
            } else {
                needSpace = true;
            }
            ++idx;
        }
        String str = formatter.toString();
        if (str.isEmpty() && lastException != null) {
            return logError(lastException);
        } else if (lastException != null) {
            GuiLogEntryString s = logString(str);
            logError(lastException);
            return s;
        } else {
            return logString(str);
        }
    }

    public boolean isSymbol(char c) {
        return (0x20 <= c && c <= 0x2f) ||
                (0x3a <= c && c <= 0x40) ||
                (0x5b <= c && c <= 0x60) ||
                (0x7b <= c && c <= 0x7f);
    }

    /** create and report a log entry.
     *  use {@link String#format(String, Object...)} and pass to {@link #logString(String)}
     *  @param format the format string
     *  @param args   the arguments for the format
     *  @return the created log entry
     * */
    public GuiLogEntryString logFormat(String format, Object... args) {
        return logString(String.format(format, args));
    }

    /**
     * replace {@link System#err} and {@link System#out} with a string redirecting to this manager.
     *   System.err will be completely replaced with the manager,
     *      because the manager usually outputs an entry to the original System.err.
     *   System.out will be replaced with a manager stream with original System.out.
     *   those replaced streams are instances of {@link LogPrintStream}
     *   @param replaceError if true, it will replace with a new {@link LogPrintStream}.
     *   @param replaceOutput  if true, it will replace with a new {@link LogPrintStream}.
     *   */
    public void replaceConsole(boolean replaceError, boolean replaceOutput) {
        if (replaceError) {
            PrintStream exErr = System.err;
            if (exErr instanceof LogPrintStream) {
                System.setErr(new LogPrintStream(this, exErr, exErr, LogStreamType.Stderr));
            } else {
                //err -> logString -> original err
                System.setErr(new LogPrintStream(this, exErr, null, LogStreamType.Stderr));
            }
        }
        if (replaceOutput) {
            PrintStream exOut = System.out;
            //out -> {original out, logString -> original err }
            System.setOut(new LogPrintStream(this, exOut, exOut, LogStreamType.Stdout));
        }
    }

    /**
     * @since 1.1
     */
    public void resetErr() {
        PrintStream exOut = System.err;
        while (exOut instanceof LogPrintStream) {
            LogPrintStream lp = (LogPrintStream) exOut;
            if (lp.getOutType().equals(LogStreamType.Stderr) && lp.getManager().equals(this)) {
                exOut = lp.getOriginal();
            } else {
                break;
            }
        }
        System.setErr(exOut);
    }

    /**
     * @since 1.1
     */
    public void resetOut() {
        PrintStream exOut = System.out;
        while (exOut instanceof LogPrintStream) {
            LogPrintStream lp = (LogPrintStream) exOut;
            if (lp.getOutType().equals(LogStreamType.Stdout) && lp.getManager().equals(this)) {
                exOut = lp.getOriginal();
            } else {
                break;
            }
        }
        System.setOut(exOut);
    }

    /** replace the default uncaught handler with {@link LogUncaughtHandler}
     * by calling {@link Thread#setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)}.
     *
     * this makes the uncaught exception will be sent to the manager.
     * */
    public void replaceUncaughtHandler() {
        Thread.UncaughtExceptionHandler h = Thread.getDefaultUncaughtExceptionHandler();
        if (h instanceof LogUncaughtHandler) {
            Thread.setDefaultUncaughtExceptionHandler(new LogUncaughtHandler(this, h));
        } else {
            Thread.setDefaultUncaughtExceptionHandler(new LogUncaughtHandler(this, null));
        }
    }

    /**
     * @since 1.1
     */
    public void resetUncaughtHandler() {
        Thread.UncaughtExceptionHandler h = Thread.getDefaultUncaughtExceptionHandler();
        while (h instanceof LogUncaughtHandler) {
            LogUncaughtHandler lh = (LogUncaughtHandler) h;
            if (lh.getManager().equals(this)) {
                h = lh.getHandler();
            }
        }
        Thread.setDefaultUncaughtExceptionHandler(h);
    }

    /** @return  obtains the original {@link System#err} from wrapped System.err
     * */
    public PrintStream getSystemErr() {
        PrintStream err = System.err;
        if (err instanceof LogPrintStream) {
            err = ((LogPrintStream) err).getOriginal();
        }
        return err;
    }

    public PrintStream getErr() {
        return null;
    }

    /** create a string entry and show it. call {@link #logString(String, boolean)} with fromStdout=false
     * @param str the string for the entry
     * @return the created log entry */
    public GuiLogEntryString logString(String str) {
        return logString(str, false);
    }

    /**
     * create a string entry and show it.
     * @param str the string for the entry
     * @param fromStandard whether the string come from the standard output (or error) redirection
     * @return the created log entry
     * @since 1.1
     */
    public GuiLogEntryString logString(String str, boolean fromStandard) {
        return new GuiLogEntryString(str, fromStandard);
    }

    /**
     * @param i the formatted date-time
     * @return <code>[YYYY-mm-dd HH:MM:SS.LLL]</code> with local date-time
     *  */
    public String formatTime(Instant i) {
        LocalDateTime time = LocalDateTime.ofInstant(i, ZoneId.systemDefault());
        return String.format("[%tF %tT.%tL]", time, time, time);
    }

    /** create an exception entry and show it
     * @param ex the exception
     * @return the created entry
     * */
    public GuiLogEntryException logError(Throwable ex) {
        return new GuiLogEntryException(ex);
    }

    /**
     * create an active progress entry and show it.
     * the returned progress is determinate (isIndeterminate() == false),
     * and notifies progress changes to the manager by {@link #updateProgress(GuiLogEntryProgress)}
     * <pre>
     *     try (GuiLogEntryProgress p = manager.logProgress();) {
     *         for (...) {
     *             //those methods will cause a runtime-exception if the thread is interrupted.
     *             p.addValueP(0.01f)
     *               .setMessage("running ...");
     *             ...
     *             //the running code can checks interruption
     *             if (Thread.interrupted()) {
     *                 break;
     *             }
     *             ...
     *         }
     *     } catch (Exception ex) { ... }
     * </pre>
     * @return the created progress entry
     */
    public GuiLogEntryProgress logProgress() {
        GuiLogEntryProgress p = new GuiLogEntryProgress();
        p.addListener(this::updateProgress);
        return p;
    }

    /**
     * @param max a maximum value
     * @return an active progress entry with the maximum value, and show it. */
    public GuiLogEntryProgress logProgress(int max) {
        GuiLogEntryProgress p = logProgress();
        p.setMaximum(max);
        p.setIndeterminate(false);
        return p;
    }

    public void updateProgress(GuiLogEntryProgress p) { }

    /** update display of the entry.
     * @param entry the displayed entry
     * */
    public void show(GuiLogEntry entry) { }

    /**
     *  @param msg the message to be formatted
     *  @return the formatted line:  a tab and a newline are converted to an space.
     *  the length is limited to 70 chars
     *  */
    public String formatMessageLine(String msg) {
        if (msg != null && (msg.contains("\n") || msg.length() > 70)) {
            StringBuilder buf = new StringBuilder();
            for (char c : msg.toCharArray()) {
                if (c == '\n' || c == '\t') {
                    buf.append(' ');
                } else {
                    buf.append(c);
                }
                if (buf.length() >= 70) {
                    buf.append("...");
                    break;
                }
            }
            return buf.toString();
        } else {
            return msg;
        }
    }

    /**
     * @param from the start time
     * @param to  the end time
     * @return <code>
     *    [[[[[[&lt;years&gt;y] &lt;months&gt;mo] &lt;days&gt;d] &lt;hours&gt;h] &lt;minutes&gt;m] &lt;seconds&gt;s] (&lt;millis&gt;ms|&lt;nanos&gt;ns)
     * </code>
     *   : years, months, and days are calculated by local date
     * */
    public String formatDuration(Instant from, Instant to) {
        List<String> parts = new ArrayList<>(8);
        Duration elapsed = Duration.between(from, to);

        long n = elapsed.getNano();
        long nsPart = n % 1000_000L;
        long msPart = n / 1000_000L;

        long t = elapsed.getSeconds();

        long secPart = t % 60;
        t /= 60;

        long minPart = t % 60;
        t /= 60;

        long hourPart = t % 24;
        t /= 24;

        if (t > 0) {
            ZoneId id = ZoneId.systemDefault();
            Period period = Period.between(LocalDateTime.ofInstant(from, id).toLocalDate(),
                    LocalDateTime.ofInstant(from, id).toLocalDate());
            long days = period.getDays();
            long months = period.getMonths();
            long years = period.getYears();

            if (years > 0) {
                parts.add(String.format("%,4dy", years));
            }
            if (!parts.isEmpty() || months > 0) {
                parts.add(String.format("%2dmo", months));
            }
            if (!parts.isEmpty() || days > 0) {
                parts.add(String.format("%3dd,", days));
            }
        }

        if (!parts.isEmpty() || hourPart > 0) {
            parts.add(String.format("%2dh", hourPart));
        }
        if (!parts.isEmpty() || minPart > 0) {
            parts.add(String.format("%2dm", minPart));
        }
        if (!parts.isEmpty() || secPart > 0) {
            parts.add(String.format("%2ds", secPart));
        }

        if (msPart > 0) {
            parts.add(String.format("%3dms", msPart));
        } else if (parts.isEmpty()) {
            parts.add(String.format("%,dns", nsPart));
        }
        return String.join(" ", parts);
    }

    /**
     * @since 1.1
     */
    public enum LogStreamType {
        Stdout(true),
        Stderr(true),
        Other(false);

        private boolean std;

        LogStreamType(boolean std) {
            this.std = std;
        }

        public boolean isStandard() {
            return std;
        }
    }

    /** a console-wrapper stream, with supporting printing exceptions */
    public static class LogPrintStream extends PrintStream {
        private GuiLogManager manager;
        protected PrintStream original;

        public LogPrintStream(GuiLogManager manager) {
            this(manager, null, null, LogStreamType.Other);
        }

        public LogPrintStream(GuiLogManager manager, OutputStream out) {
            super(new LogOutputStream(manager, out));
            this.manager = manager;
        }

        /**
         * @param manager the manager
         * @param out the wrapped out
         * @param outType the type of out
         * @since 1.1
         */
        public LogPrintStream(GuiLogManager manager, PrintStream original, OutputStream out, LogStreamType outType) {
            super(new LogOutputStream(manager, out, outType));
            this.original = original;
            this.manager = manager;
        }

        public GuiLogManager getManager() {
            return manager;
        }

        /**
         * @return the wrapped stream
         * @since 1.1
         */
        public PrintStream getOriginal() {
            return original;
        }

        public OutputStream getOut() {
            return out;
        }

        /**
         * @return if out is {@link LogOutputStream}, it's outType, or Other
         * @since 1.1
         */
        public LogStreamType getOutType() {
            if (out instanceof LogOutputStream) {
                return ((LogOutputStream) out).getOutType();
            } else {
                return LogStreamType.Other;
            }
        }

        @Override
        public void println(Object x) {
            if (x instanceof Throwable) {
                flush();
                manager.logError((Throwable) x);
            } else {
                super.println(x);
            }
        }
    }

    /** a default handler for integrating the log-manager */
    public static class LogUncaughtHandler implements Thread.UncaughtExceptionHandler {
        protected GuiLogManager manager;
        protected Thread.UncaughtExceptionHandler handler;

        public LogUncaughtHandler(GuiLogManager manager, Thread.UncaughtExceptionHandler handler) {
            this.manager = manager;
            this.handler = handler;
        }

        public void handle(Throwable e) {
            uncaughtException(Thread.currentThread(), e);
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            manager.logError(e);
            if (handler != null) {
                handler.uncaughtException(t, e);
            }
        }

        /**
         * @return owner manager
         * @since 1.1
         */
        public GuiLogManager getManager() {
            return manager;
        }

        /**
         * @return wrapped handler
         * @since 1.1
         */
        public Thread.UncaughtExceptionHandler getHandler() {
            return handler;
        }
    }

    /** a stream wrapper with writing to a log-manager, used by {@link LogPrintStream} */
    public static class LogOutputStream extends OutputStream {
        protected OutputStream out;
        protected ByteBuffer buffer;
        protected GuiLogManager manager;
        protected Charset defaultCharset;
        /** @since 1.1 */
        protected LogStreamType outType;

        public LogOutputStream(GuiLogManager manager) {
            this(manager, null, LogStreamType.Other);
        }

        public LogOutputStream(GuiLogManager manager, OutputStream out) {
            this(manager, out, LogStreamType.Other);
        }

        /**
         * @param manager the manager
         * @param out the wrapped output
         * @param outType the out is stdout, err or another
         * @since 1.1
         */
        public LogOutputStream(GuiLogManager manager, OutputStream out, LogStreamType outType) {
            this.manager = manager;
            this.out = out;
            this.outType = outType;
            buffer = ByteBuffer.allocateDirect(4096);
            defaultCharset = Charset.defaultCharset(); //PrintStream always encodes by default encoding
        }

        /**
         * @return the type of wrapped output
         * @since  1.1
         */
        public LogStreamType getOutType() {
            return outType;
        }

        /**
         * @return wrapped output
         * @since 1.1
         */
        public OutputStream getOut() {
            return out;
        }

        @Override
        public void write(int b) throws IOException {
            if (out != null) {
                out.write(b);
            }
            synchronized (this) {
                expand(1000);
                buffer.put((byte) b);
                if (b == '\n') {
                    flushLog();
                }
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            if (out != null) {
                out.write(b);
            }
            synchronized (this) {
                expand(b.length);
                buffer.put(b);
                for (byte e : b) {
                    if (e == '\n') {
                        flushLog();
                        break;
                    }
                }
            }
        }

        public void expand(int len) {
            if (len >= buffer.remaining()) {
                ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.position() + (int) (len * 1.2));
                ((Buffer) buffer).flip();
                newBuffer.put(buffer);
                buffer = newBuffer;
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (out != null) {
                out.write(b, off, len);
            }
            synchronized (this) {
                expand(len);
                buffer.put(b, off, len);
                for (int i = 0; i < len; ++i) {
                    if (b[off + i] == '\n') {
                        flushLog();
                        break;
                    }
                }
            }
        }

        @Override
        public void flush() throws IOException {
            synchronized (this) {
                flushLog();
            }
            if (out != null) {
                out.flush();
            }
        }

        @Override
        public void close() throws IOException {
            synchronized (this) {
                flushLog();
            }
            if (out != null) {
                out.close();
            }
        }

        public void flushLog() {
            ((Buffer) buffer).flip();
            if (buffer.hasRemaining()) {
                try {
                    String data = defaultCharset.decode(buffer).toString();
                    //cut the last line
                    if (data.endsWith("\n")) {
                        data = data.substring(0, data.length() - 1);
                    }
                    if (manager != null) {
                        manager.logString(data, outType.equals(LogStreamType.Stdout));
                    }
                } catch (Exception ex) {
                    if (manager != null) {
                        manager.logString("data...");
                    }
                }
            }
            ((Buffer) buffer).clear();
        }
    }
}
