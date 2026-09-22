package org.autogui.base.log;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiLogEntryTest {
    @Test
    public void testException() {
        Instant time = Instant.EPOCH;
        var ex = new RuntimeException("msg");
        var e = new GuiLogEntryException(time, ex);
        Assert.assertEquals("time", time, e.getTime());
        Assert.assertEquals("exception", ex, e.getException());
        Assert.assertTrue("toString contains time", e.toString().contains(time.toString()));
        Assert.assertTrue("toString contains message", e.toString().contains("msg"));
    }

    @Test
    public void testProgressSet() {
        Instant time = Instant.EPOCH;
        Thread thread = Thread.currentThread();
        try (var p = new GuiLogEntryProgress(time, thread)) {
            Assert.assertEquals("time", time, p.getTime());
            Assert.assertEquals("thread", thread, p.getThread());
            Assert.assertTrue("toString contains time", p.toString().contains(time.toString()));
            Assert.assertTrue("toString contains thread", p.toString().contains(thread.getName()));
            Assert.assertEquals("value init", 0, p.getValue());
            Assert.assertEquals("getMessage init", "", p.getMessage());
            Assert.assertNull("getEndTime init", p.getEndTime());
            Assert.assertEquals("getValueP init", 0.0, p.getValueP(), 0);
            p.setState(null);
            Assert.assertNull("time after set-null", p.getTime());
            Assert.assertNull("thread after set-null", p.getThread());
            Assert.assertEquals("value after set-null", 0, p.getValue());
            Assert.assertEquals("min after set-null", 0, p.getMinimum());
            Assert.assertEquals("max after set-null", Integer.MAX_VALUE, p.getMaximum());
            Assert.assertEquals("getMessage set-null", "", p.getMessage());
            Assert.assertNull("getEndTime set-null", p.getEndTime());
            Assert.assertEquals("getValueP set-null", 0, p.getValueP(), 0.00001);

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
                Assert.assertEquals("time after setState", time2, p.getTime());
                Assert.assertEquals("thread after setState", thread2, p.getThread());
                Assert.assertEquals("value after setState", 30, p.getValue());
                Assert.assertEquals("min after setState", 10, p.getMinimum());
                Assert.assertEquals("max after setState", 100, p.getMaximum());
                Assert.assertEquals("getMessage setState", "msg2", p.getMessage());
                Assert.assertEquals("getEndTime setState", time3, p.getEndTime());
                Assert.assertEquals("getValueP setState", 0.3333, p.getValueP(), 0.0001);

            } finally {
                thread2.start();
            }
        }
    }

    @Test
    public void testProgressListener() {
        try (var p  =new GuiLogEntryProgress()) {
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
            Assert.assertEquals("active listener", List.of(1, 3), l2.list);
            Assert.assertEquals("removed listener", List.of(), l1.list);
        }
    }

    @Test
    public void testProgressInterrpt() {
        try (var p = new GuiLogEntryProgress()) {
            Thread.currentThread().interrupt();
            try {
                p.addValue(1);
                Assert.fail("interrupted exception expected");
            } catch (GuiLogEntryProgress.GuiLogEntryProgressInterruptedException ex) {
                //ok
            } catch (Throwable ex) {
                Assert.fail("interrupted exception expected");
            }
            try {
                p.addValue(2); //ok
            } catch (Throwable ex) {
                Assert.fail("no exception expected after checked");
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
            Assert.assertTrue("init time", p.getTime().isAfter(start));
            Assert.assertTrue("active", p.isActive());
            Thread.sleep(15);
            end = Instant.now();
            progress = p;
            Thread.sleep(15);
        } finally {
            Assert.assertTrue("finish time", progress.getEndTime().isAfter(end));
            Assert.assertTrue("finished", progress.isFinished());
            Assert.assertEquals("finished valueP", 1.0, progress.getValueP(), 0.001);
            Assert.assertEquals("finished value", Integer.MAX_VALUE, progress.getValue());
            Assert.assertFalse("finished no active", progress.isActive());
        }
    }

    @Test
    public void testString() {
        Instant time = Instant.EPOCH;
        var s = new GuiLogEntryString(time, "hello");
        Assert.assertEquals("time", time, s.getTime());
        Assert.assertEquals("data", "hello", s.getData());
        Assert.assertFalse("not stdout", s.isFromStandard());
        Assert.assertFalse("no active", s.isActive());
        Assert.assertTrue("toString contains time", s.toString().contains(time.toString()));
        Assert.assertTrue("toString contains message", s.toString().contains("hello"));
    }

    @Test
    public void testManagerLog() {
        GuiLogManager manager = GuiLogManager.get();
        var l1 = manager.log("hello");
        Assert.assertTrue("log returns string", l1 instanceof GuiLogEntryString);
        var s1 = (GuiLogEntryString) l1;
        Assert.assertEquals("log message", "hello", s1.getData());

        var l2 = manager.log("hello", 123, 456);
        Assert.assertTrue("log returns string with vals", l2 instanceof GuiLogEntryString);
        var s2 = (GuiLogEntryString) l2;
        Assert.assertEquals("log message with vals", "hello 123 456", s2.getData());

        var ex = new RuntimeException("world");
        var l3 = manager.log("hello", 123, 456, ex);
        Assert.assertTrue("log returns string with ex", l3 instanceof GuiLogEntryString);
        var s3 = (GuiLogEntryString) l3;
        Assert.assertEquals("log message with vals ex", "hello 123 456", s3.getData());

        var l4 = manager.log(ex);
        Assert.assertTrue("log returns ex", l4 instanceof GuiLogEntryException);
        var s4 = (GuiLogEntryException) l4;
        Assert.assertEquals("log message with ex", ex, s4.getException());
    }

    @Test
    public void testManagerFormatDuration() {
        var manager = GuiLogManager.get();
        OffsetDateTime time1 = OffsetDateTime.of(2000, 1, 2, 3, 4, 5, 6_000_000, ZoneOffset.UTC);
        var time2 = time1.plusYears(1).plusMonths(2).plusDays(3).plusHours(4).plusMinutes(5).plusSeconds(6).plusNanos(7_000_000);

        Assert.assertEquals("formatDuration", "   1y  2mo   3d,  4h  5m  6s   7ms", manager.formatDuration(time1.toInstant(), time2.toInstant()));
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
        Assert.assertTrue("console output contains log", outData.contains("hello"));
        Assert.assertTrue("console output contains progress message", outData.contains("world"));
        Assert.assertTrue("console output contains progress long message part", outData.contains("1234567890"));
        Assert.assertTrue("console output contains long string as is", outData.contains(bufferOverMsg));
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
        var bout =  new ByteArrayOutputStream();
        var top = "TOP";
        var bufferOverMsg = "1234567890abcdefghijklmnopqrtuxyz.,!?+-*".repeat(105) + "\n2ndLine\nEND";
        try (var out = new GuiLogManager.LogOutputStream(manager, bout)) {
            out.write(top.getBytes());
            out.write(bufferOverMsg.getBytes(StandardCharsets.UTF_8));
        }

        Assert.assertEquals("stream output contains log", 1, strs.size());
        Assert.assertEquals("stream output contains log", top + bufferOverMsg, strs.getFirst().getData());
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

        Assert.assertEquals("unchaought error size", 1, exs.size());
        Assert.assertEquals("unchaought error msg", "hello", exs.getFirst().getException().getMessage());
    }
}
