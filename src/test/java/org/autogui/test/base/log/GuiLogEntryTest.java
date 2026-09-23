package org.autogui.test.base.log;

import org.autogui.base.log.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class GuiLogEntryTest {
    @Test
    public void testException() {
        Instant time = Instant.EPOCH;
        var ex = new RuntimeException("msg");
        var e = new GuiLogEntryException(time, ex);
        assertEquals(time, e.getTime(), "time");
        assertEquals(ex, e.getException(), "exception");
        assertTrue(e.toString().contains(time.toString()), "toString contains time");
        assertTrue(e.toString().contains("msg"), "toString contains message");
    }

    @Test
    public void testProgressSet() {
        Instant time = Instant.EPOCH;
        Thread thread = Thread.currentThread();
        try (var p = new GuiLogEntryProgress(time, thread)) {
            assertEquals(time, p.getTime(), "time");
            assertEquals(thread, p.getThread(), "thread");
            assertTrue(p.toString().contains(time.toString()), "toString contains time");
            assertTrue(p.toString().contains(thread.getName()), "toString contains thread");
            assertEquals(0, p.getValue(), "value init");
            assertEquals("", p.getMessage(), "getMessage init");
            assertNull(p.getEndTime(), "getEndTime init");
            assertEquals(0.0, p.getValueP(), 0, "getValueP init");
            p.setState(null);
            assertNull(p.getTime(), "time after set-null");
            assertNull(p.getThread(), "thread after set-null");
            assertEquals(0, p.getValue(), "value after set-null");
            assertEquals(0, p.getMinimum(), "min after set-null");
            assertEquals(Integer.MAX_VALUE, p.getMaximum(), "max after set-null");
            assertEquals("", p.getMessage(), "getMessage set-null");
            assertNull(p.getEndTime(), "getEndTime set-null");
            assertEquals(0, p.getValueP(), 0.00001, "getValueP set-null");

            Instant time2 = Instant.now();
            Instant time3 = time2.plusSeconds(1);
            Thread thread2 = new Thread(() -> {
            });
            try {
                var ap = new GuiLogEntryProgress(time2, thread2);
                ap.setMessage("msg2");
                ap.setMaximum(100);
                ap.setMinimum(10);
                ap.addValue(30);
                ap.setEndTime(time3);

                p.setState(ap);
                assertEquals(time2, p.getTime(), "time after setState");
                assertEquals(thread2, p.getThread(), "thread after setState");
                assertEquals(30, p.getValue(), "value after setState");
                assertEquals(10, p.getMinimum(), "min after setState");
                assertEquals(100, p.getMaximum(), "max after setState");
                assertEquals("msg2", p.getMessage(), "getMessage setState");
                assertEquals(time3, p.getEndTime(), "getEndTime setState");
                assertEquals(0.3333, p.getValueP(), 0.0001, "getValueP setState");

            } finally {
                thread2.start();
            }
        }
    }

    @Test
    public void testProgressListener() {
        try (var p = new GuiLogEntryProgress()) {
            var l1 = new TestProgressListenr();
            var l2 = new TestProgressListenr();
            p.addListener(l1);
            p.addListener(l2);
            p.removeListener(l1);
            p.setMaximum(3);
            p.addValue(1);
            p.addValue(0);  //0 change: no firing
            p.addValue(2);
            p.addValue(10); //over max: no firing
            assertEquals(List.of(1, 3), l2.list, "active listener");
            assertEquals(List.of(), l1.list, "removed listener");
        }
    }

    @Test
    public void testProgressInterrpt() {
        try (var p = new GuiLogEntryProgress()) {
            Thread.currentThread().interrupt();
            try {
                p.addValue(1);
                fail("interrupted exception expected");
            } catch (GuiLogEntryProgress.GuiLogEntryProgressInterruptedException ex) {
                //ok
            } catch (Throwable ex) {
                fail("interrupted exception expected");
            }
            try {
                p.addValue(2); //ok
            } catch (Throwable ex) {
                fail("no exception expected after checked");
            }
        }
    }


    static class TestProgressListenr implements Consumer<GuiLogEntryProgress> {
        public List<Integer> list = new ArrayList<>();

        @Override
        public void accept(GuiLogEntryProgress value) {
            list.add(value.getValue());
        }
    }

    @Test
    public void testProgressFinish() throws Exception {
        Instant start = Instant.now();
        Thread.sleep(15);
        Instant end = null;
        GuiLogEntryProgress progress = null;
        try (var p = new GuiLogEntryProgress()) {
            assertTrue(p.getTime().isAfter(start), "init time");
            assertTrue(p.isActive(), "active");
            Thread.sleep(15);
            end = Instant.now();
            progress = p;
            Thread.sleep(15);
        } finally {
            assertTrue(progress.getEndTime().isAfter(end), "finish time");
            assertTrue(progress.isFinished(), "finished");
            assertEquals(1.0, progress.getValueP(), 0.001, "finished valueP");
            assertEquals(Integer.MAX_VALUE, progress.getValue(), "finished value");
            assertFalse(progress.isActive(), "finished no active");
        }
    }

    @Test
    public void testString() {
        Instant time = Instant.EPOCH;
        var s = new GuiLogEntryString(time, "hello");
        assertEquals(time, s.getTime(), "time");
        assertEquals("hello", s.getData(), "data");
        assertFalse(s.isFromStandard(), "not stdout");
        assertFalse(s.isActive(), "no active");
        assertTrue(s.toString().contains(time.toString()), "toString contains time");
        assertTrue(s.toString().contains("hello"), "toString contains message");
    }

    @Test
    public void testManagerLog() {
        GuiLogManager manager = GuiLogManager.get();
        var l1 = manager.log("hello");
        assertTrue(l1 instanceof GuiLogEntryString, "log returns string");
        var s1 = (GuiLogEntryString) l1;
        assertEquals("hello", s1.getData(), "log message");

        var l2 = manager.log("hello", 123, 456);
        assertTrue(l2 instanceof GuiLogEntryString, "log returns string with vals");
        var s2 = (GuiLogEntryString) l2;
        assertEquals("hello 123 456", s2.getData(), "log message with vals");

        var ex = new RuntimeException("world");
        var l3 = manager.log("hello", 123, 456, ex);
        assertTrue(l3 instanceof GuiLogEntryString, "log returns string with ex");
        var s3 = (GuiLogEntryString) l3;
        assertEquals("hello 123 456", s3.getData(), "log message with vals ex");

        var l4 = manager.log(ex);
        assertTrue(l4 instanceof GuiLogEntryException, "log returns ex");
        var s4 = (GuiLogEntryException) l4;
        assertEquals(ex, s4.getException(), "log message with ex");
    }

    @Test
    public void testManagerFormatDuration() {
        var manager = GuiLogManager.get();
        OffsetDateTime time1 = OffsetDateTime.of(2000, 1, 2, 3, 4, 5, 6_000_000, ZoneOffset.UTC);
        var time2 = time1.plusYears(1).plusMonths(2).plusDays(3).plusHours(4).plusMinutes(5).plusSeconds(6).plusNanos(7_000_000);

        assertEquals("   1y  2mo   3d,  4h  5m  6s   7ms", manager.formatDuration(time1.toInstant(), time2.toInstant()), "formatDuration");
    }

    @Test
    public void testManagerConsole() throws Exception {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(byteOut);
        var manager = new GuiLogManagerConsole(out);
        manager.log("hello");

        var longMsg = "1234567890".repeat(8);
        try (var p = manager.logProgress(4)) {
            p.setMessage("world");
            p.addValue(1);
            p.setMessage(longMsg);
            p.addValue(2);
            p.setIndeterminate(true);
            Thread.sleep(10);
            p.addValue(1);
        }

        var bufferOverMsg = "1234567890abcdefghijklmnopqrtuxyz.,!?+-*".repeat(105);
        manager.logString(bufferOverMsg);

        var outData = byteOut.toString(StandardCharsets.UTF_8);
        System.err.println(outData);
        assertTrue(outData.contains("hello"), "console output contains log");
        assertTrue(outData.contains("world"), "console output contains progress message");
        assertTrue(outData.contains("1234567890"), "console output contains progress long message part");
        assertTrue(outData.contains(bufferOverMsg), "console output contains long string as is");
    }

    @Test
    public void testManagerLogStream() throws Exception {
        List<GuiLogEntryString> strs = new ArrayList<>();
        var manager = new GuiLogManagerConsole(System.out) {
            @Override
            public void showString(GuiLogEntryString s) {
                strs.add(s);
            }
        };
        var bout = new ByteArrayOutputStream();
        var top = "TOP";
        var bufferOverMsg = "1234567890abcdefghijklmnopqrtuxyz.,!?+-*".repeat(105) + "\n2ndLine\nEND";
        try (var out = new GuiLogManager.LogOutputStream(manager, bout)) {
            out.write(top.getBytes());
            out.write(bufferOverMsg.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(1, strs.size(), "stream output contains log");
        assertEquals(top + bufferOverMsg, strs.getFirst().getData(), "stream output contains log");
    }

    @Test
    public void testManagerUncaughtException() throws Exception {
        List<GuiLogEntryException> exs = new ArrayList<>();
        var manager = new GuiLogManagerConsole() {
            @Override
            public void showError(GuiLogEntryException e) {
                exs.add(e);
            }
        };

        GuiLogManager.setManager(manager);
        var th = new Thread(() -> {
            manager.replaceUncaughtHandler();
            throw new RuntimeException("hello");
        });

        th.start();
        th.join();

        assertEquals(1, exs.size(), "unchaought error size");
        assertEquals("hello", exs.getFirst().getException().getMessage(), "unchaought error msg");
    }
}
