package org.autogui.test.swing.log;

import org.autogui.base.log.GuiLogEntry;
import org.autogui.base.log.GuiLogEntryProgress;
import org.autogui.base.log.GuiLogEntryString;
import org.autogui.swing.log.GuiSwingLogEntryException;
import org.autogui.swing.log.GuiSwingLogEntryString;
import org.autogui.swing.log.GuiSwingLogManager;
import org.autogui.swing.util.UIManagerUtil;
import org.autogui.test.swing.GuiSwingTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuiSwingLogManagerTest extends GuiSwingTestCase {
    /*
    public static void main(String[] args) {
        GuiSwingLogManagerTest t = new GuiSwingLogManagerTest();
        t.setUp();
        t.testLogErrorExpand();
    }*/

    GuiSwingLogManager manager;

    GuiSwingLogManager.GuiSwingLogWindow window;

    TestPrint out;
    TestPrint err;
    TestOut outOut;
    TestOut errOut;

    @Before
    public void setUp() {
        manager = new GuiSwingLogManager();

        window = runGet(manager::createWindow);

        synchronized (GuiSwingLogManagerTest.class) {
            outOut = new TestOut(System.out);
            out = new TestPrint(outOut);
            System.setOut(out);

            errOut = new TestOut(System.err);
            err = new TestPrint(errOut);
            System.setErr(err);
        }
    }



    @After
    public void tearDown() {
        if (window != null) {
            window.dispose();
            window = null;
        }

        if (progress1Thread != null && progress1Thread.isAlive()) {
            progress1Thread.interrupt();
        }
        if (progress2Thread != null && progress2Thread.isAlive()) {
            progress2Thread.interrupt();
        }

        synchronized (GuiSwingLogManagerTest.class) {
            removeSaved(System.out, out, System::setOut);
            removeSaved(System.err, err, System::setErr);
        }
    }

    private void removeSaved(PrintStream top, TestPrint saved, Consumer<PrintStream> setter) {
        TestOut parent = null;
        PrintStream next = top;
        while (next != null && next != saved) {
            if (next instanceof TestPrint) {
                OutputStream out = ((TestPrint) next).getOut();
                if (out instanceof TestOut) {
                    parent = (TestOut) out;
                    next = parent.out;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        if (parent != null && next == saved) {
            parent.out = ((TestOut) out.getOut()).out;
        } else if (parent == null && next == saved) {
            setter.accept(((TestOut) out.getOut()).out);
        }
    }

    public static class TestPrint extends PrintStream {
        TestPrint(OutputStream out) {
            super(out);
        }

        public OutputStream getOut() {
            return out;
        }
    }

    public static class TestOut extends OutputStream {
        ByteBuffer buffer;
        public List<String> lines;
        public PrintStream out;

        public TestOut(PrintStream out) {
            this.out = out;
            lines = new ArrayList<>();
            buffer = ByteBuffer.allocate(1000_000);
        }

        @Override
        public void write(int b) {
            buffer.put((byte) b);
            if (out != null) {
                out.write(b);
            }
        }

        @Override
        public void flush() throws IOException {
            buffer.flip();
            String data = StandardCharsets.UTF_8.decode(buffer).toString();
            lines.addAll(Arrays.asList(data.trim().split("\\n")));
            buffer.clear();
            if (out != null) {
                out.flush();
            }
        }

        public List<String> getLines() {
            if (buffer.position() != 0) {
                try {
                    flush();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            return lines;
        }
    }

    @Test
    public void testLogErrorExpand() {
        RuntimeException error1 = new RuntimeException("error1");
        RuntimeException error2 = new RuntimeException("error2: different message size");
        UIManagerUtil ui = UIManagerUtil.getInstance();

        run(() -> {
            window.setSize(new Dimension(ui.getScaledSizeInt(800), ui.getScaledSizeInt(300)));
            window.setVisible(true);

            manager.logString("msg1");
            manager.logError(error1);
            manager.logString("msg2");
            manager.logError(error2);
        });

        List<GuiLogEntry> es = runGet(() -> window.getList().getLogListModel().getEntries());
        Assert.assertEquals("logged",
                4, es.size());

        Assert.assertEquals("listed entry 0", "msg1",
                ((GuiSwingLogEntryString) es.get(0)).getData());
        Assert.assertEquals("listed entry 1", error1,
                ((GuiSwingLogEntryException) es.get(1)).getException());
        Assert.assertEquals("listed entry 2", "msg2",
                ((GuiSwingLogEntryString) es.get(2)).getData());
        Assert.assertEquals("listed entry 3", error2,
                ((GuiSwingLogEntryException) es.get(3)).getException());

        Point errorExpandPoint1 = getListExpandPoint(1);
        Point errorExpandPoint2 = getListExpandPoint(3);
        //click error 1: expand
        clickList(errorExpandPoint1);

        Assert.assertTrue("after expand by click error 1",
                ((GuiSwingLogEntryException) es.get(1)).isExpanded());

        Assert.assertFalse("after expand by click error 1, not error 2",
                ((GuiSwingLogEntryException) es.get(3)).isExpanded());

        //re-click the error1: close
        clickList(errorExpandPoint1);

        //expand error2
        clickList(errorExpandPoint2);

        Assert.assertFalse("after expand by click error 2, not error 1",
                ((GuiSwingLogEntryException) es.get(1)).isExpanded());

        Assert.assertTrue("after expand by click error 2",
                ((GuiSwingLogEntryException) es.get(3)).isExpanded());

    }

    public Point getListExpandPoint(int listItemIndex) {
        return runGet(() -> {
            Rectangle rectForError = window.getList().getCellRect(listItemIndex);
            GuiSwingLogEntryException e = (GuiSwingLogEntryException) window.getList().getLogListModel().getEntries().get(listItemIndex);
            String errorStr = e.getException().toString();
            int w = getWidthOfText("[2017-07-07 07:07:07.777] !!! " + errorStr);
            UIManagerUtil ui = UIManagerUtil.getInstance();
            int borderWidth = ui.getScaledSizeInt(10);
            // border - messageLine - expandButton(32x28)
            double clickX = borderWidth + w + ui.getScaledSizeInt(10) + rectForError.x;
            int borderHeight = ui.getScaledSizeInt(5);
            double clickY = borderHeight + ui.getScaledSizeInt(10) + rectForError.y;
            return new Point((int) clickX, (int) clickY);
        });
    }

    public int getWidthOfText(String base) {
        return (int) getBoundsOfText(base).getWidth();
    }

    public Rectangle2D getBoundsOfText(String base) {
        //String base = "[2017-07-07 07:07:07.777] !!! java.lang.RuntimeException: error1";
        Graphics2D g2 = (Graphics2D) window.getGraphics();
        g2.setFont(GuiSwingLogManager.getFont());
        FontRenderContext frc = g2.getFontRenderContext();
        AttributedString str = new AttributedString(base);
        str.addAttribute(TextAttribute.FONT, GuiSwingLogManager.getFont(), 0, base.length());
        TextLayout l = new TextLayout(str.getIterator(), frc);
        return l.getBounds();
    }

    public void clickList(Point p) {
        run(() -> {
            System.err.println("click " + p);
            window.getList().dispatchEvent(new MouseEvent(window.getList(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, p.x, p.y, 1, false));
            window.getList().dispatchEvent(new MouseEvent(window.getList(), MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, p.x, p.y, 1, false));
        });
    }

    volatile GuiLogEntryProgress progress1;
    volatile GuiLogEntryProgress progress2;
    volatile BlockingQueue<Boolean> progressQueue = new ArrayBlockingQueue<>(1);
    volatile BlockingQueue<Boolean> progress1EndQueue = new ArrayBlockingQueue<>(1);
    volatile BlockingQueue<Boolean> progress2EndQueue = new ArrayBlockingQueue<>(1);

    volatile Thread progress1Thread;
    volatile Thread progress2Thread;

    @Test
    public void testLogProgress() {
        UIManagerUtil ui = UIManagerUtil.getInstance();
        run(() -> {
            try {
                window.setSize(new Dimension(ui.getScaledSizeInt(800), ui.getScaledSizeInt(300)));
                window.setVisible(true);

                manager.logString("msg1");

                progressQueue.clear();
                progress1Thread = new Thread(() -> {
                    try (GuiLogEntryProgress prg = manager.logProgress()) {
                        progress1 = prg;
                        progress1.addValueP(0.1);
                        progressQueue.add(true);
                        try {
                            Thread.sleep(100_0000);
                            progress1EndQueue.add(false);
                        } catch (Exception ex) {
                            //
                            progress1EndQueue.add(true);
                        }
                    }
                });
                progress1Thread.start();

                progressQueue.poll(1, TimeUnit.SECONDS);

                manager.logString("msg2");

                progress2Thread = new Thread(() -> {
                    try (GuiLogEntryProgress prg = manager.logProgress()) {
                        progress2 = prg;
                        progress2.addValueP(0.2);
                        progressQueue.add(true);
                        try {
                            Thread.sleep(100_0000);

                            progress2EndQueue.add(false);
                        } catch (Exception ex) {
                            //
                            progress2EndQueue.add(true);
                        }
                    }
                });
                progress2Thread.start();

                progressQueue.poll(1, TimeUnit.SECONDS);

                manager.logString("msg3");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });


        List<GuiLogEntry> es = runGet(() -> window.getList().getLogListModel().getEntries());
        Assert.assertEquals("logged",
                5, es.size());


        Assert.assertEquals("listed entry 0", "msg1",
                ((GuiSwingLogEntryString) es.get(0)).getData());
        Assert.assertEquals("listed entry 1", "msg2",
                ((GuiSwingLogEntryString) es.get(1)).getData());
        Assert.assertEquals("listed entry 2", "msg3",
                ((GuiSwingLogEntryString) es.get(2)).getData());

        Assert.assertEquals("listed entry 3: sorted active progress1", progress1,
                es.get(3));
        Assert.assertEquals("listed entry 4: sorted active progress2", progress2,
                es.get(4));

        this.runWait(500);

        Point progress2Button = getListStopPoint(4);
        clickList(progress2Button);

        Assert.assertTrue("progress2 finished by button click", progress2.isFinished());
        Assert.assertFalse("progress1 is not finished", progress1.isFinished());

        Assert.assertTrue("progress2 finished by interruption", Objects.equals(true, progress2EndQueue.peek()));

        run(() -> manager.logString("msg4"));

        ////////////////////

        es = runGet(() -> window.getList().getLogListModel().getEntries());
        Assert.assertEquals("after stop and a new log msg4",
                6, es.size());

        Assert.assertEquals("listed entry 0", "msg1",
                ((GuiSwingLogEntryString) es.get(0)).getData());
        Assert.assertEquals("listed entry 1", "msg2",
                ((GuiSwingLogEntryString) es.get(1)).getData());
        Assert.assertEquals("listed entry 2", "msg3",
                ((GuiSwingLogEntryString) es.get(2)).getData());

        Assert.assertEquals("listed entry 3: sorted active progress2", progress2,
                es.get(3));
        Assert.assertEquals("listed entry 4", "msg4",
                ((GuiSwingLogEntryString) es.get(4)).getData());

        Assert.assertEquals("listed entry 5: sorted active progress1", progress1,
                es.get(5));



        runWait(500);
        progress1.finish();
    }

    public Point getListStopPoint(int listItemIndex) {
        return runGet(() -> {
            UIManagerUtil ui = UIManagerUtil.getInstance();

            Rectangle rect = window.getList().getVisibleRect();
            Rectangle cellRect = window.getList().getCellRect(listItemIndex);

            //cell width - (button + message2-text + message2-right + message2-border + border-right)
            int br = ui.getScaledSizeInt(10) + ui.getScaledSizeInt(10) * 2 + ui.getScaledSizeInt(10) * 2;

            double clickX = rect.width - (ui.getScaledSizeInt(10) + getWidthOfText("#  77% +77s 777ms   ") + br);
            int borderHeight = ui.getScaledSizeInt(5);
            //border-height + message-height + button-height(16)
            double clickY = borderHeight + getBoundsOfText("[2017-07-07 07:07:07.777] #").getHeight() + ui.getScaledSizeInt(10) + cellRect.y;
            return new Point((int) clickX, (int) clickY);
        });
    }

    Pattern logPat = Pattern.compile("\\[(.*?)] (.*)");

    private String logData(String msg) {
        Matcher m = logPat.matcher(msg);
        if (m.find()) {
            return m.group(2);
        } else {
            throw new RuntimeException("error: not a log message: " + msg);
        }
    }

    String flagReplaceOut = "autogui.log.replaceOut";
    String flagReplaceErr = "autogui.log.replaceErr";
    String flagReplaceExceptionHandler = "autogui.log.replaceExceptionHandler";
    String flagRedirectToConsole = "autogui.log.redirectToConsole";
    String flagSuppressOutputRedirection = "autogui.log.suppressOutputRedirection";

    @Test
    public void testSetupConsole() {
        UIManagerUtil ui = UIManagerUtil.getInstance();
        run(() -> {
                window.setSize(new Dimension(ui.getScaledSizeInt(800), ui.getScaledSizeInt(300)));
                window.setVisible(true);

                System.setProperty(flagReplaceOut, "true");
                System.setProperty(flagReplaceErr, "true");
                System.setProperty(flagReplaceExceptionHandler, "true");
                System.setProperty(flagRedirectToConsole, "true");
                System.setProperty(flagSuppressOutputRedirection, "true");
                manager.setupConsoleWithDefaultFlags();


                System.out.println("message1");
                System.err.println("message2");
                manager.logString("message3");
            });

        List<GuiLogEntry> es = runGet(() -> window.getList().getLogListModel().getEntries());
        Assert.assertEquals(3, es.size());
        GuiLogEntryString m1 = (GuiLogEntryString) es.get(0);
        GuiLogEntryString m2 = (GuiLogEntryString) es.get(1);
        GuiLogEntryString m3 = (GuiLogEntryString) es.get(2);

        Assert.assertEquals("message1", m1.getData());
        Assert.assertEquals("message2", m2.getData());
        Assert.assertEquals("message3", m3.getData());

        Assert.assertEquals(1, outOut.getLines().size());
        Assert.assertEquals(2, errOut.getLines().size());

        Assert.assertEquals("message1", outOut.getLines().get(0));
        Assert.assertEquals("message2", logData(errOut.getLines().get(0)));
        Assert.assertEquals("message3", logData(errOut.getLines().get(1)));
    }

    @Test
    public void testSetupConsoleNoReplace() {
        UIManagerUtil ui = UIManagerUtil.getInstance();
        run(() -> {
            window.setSize(new Dimension(ui.getScaledSizeInt(800), ui.getScaledSizeInt(300)));
            window.setVisible(true);

            System.setProperty(flagReplaceOut, "false");
            System.setProperty(flagReplaceErr, "false");
            System.setProperty(flagReplaceExceptionHandler, "true");
            System.setProperty(flagRedirectToConsole, "true");
            System.setProperty(flagSuppressOutputRedirection, "true");
            manager.setupConsoleWithDefaultFlags();


            System.out.println("message1");
            System.err.println("message2");
            manager.logString("message3");
        });

        List<GuiLogEntry> es = runGet(() -> window.getList().getLogListModel().getEntries());
        Assert.assertEquals(1, es.size());
        GuiLogEntryString m1 = (GuiLogEntryString) es.get(0);

        Assert.assertEquals("message3", m1.getData());

        Assert.assertEquals(1, outOut.getLines().size());
        Assert.assertEquals(2, errOut.getLines().size());
        Assert.assertEquals("message1", outOut.getLines().get(0));
        Assert.assertEquals("message2", errOut.getLines().get(0));
        Assert.assertEquals("message3", logData(errOut.getLines().get(1)));
    }

    @Test
    public void testSetupConsoleNoRedirect() {
        UIManagerUtil ui = UIManagerUtil.getInstance();
        run(() -> {
            window.setSize(new Dimension(ui.getScaledSizeInt(800), ui.getScaledSizeInt(300)));
            window.setVisible(true);

            System.setProperty(flagReplaceOut, "true");
            System.setProperty(flagReplaceErr, "true");
            System.setProperty(flagReplaceExceptionHandler, "true");
            System.setProperty(flagRedirectToConsole, "false");
            System.setProperty(flagSuppressOutputRedirection, "true");
            manager.setupConsoleWithDefaultFlags();


            System.out.println("message1");
            System.err.println("message2");
            manager.logString("message3");
        });

        List<GuiLogEntry> es = runGet(() -> window.getList().getLogListModel().getEntries());
        Assert.assertEquals(3, es.size());
        GuiLogEntryString m1 = (GuiLogEntryString) es.get(0);
        GuiLogEntryString m2 = (GuiLogEntryString) es.get(1);
        GuiLogEntryString m3 = (GuiLogEntryString) es.get(2);

        Assert.assertEquals("message1", m1.getData());
        Assert.assertEquals("message2", m2.getData());
        Assert.assertEquals("message3", m3.getData());

        Assert.assertEquals(1, outOut.getLines().size());
        Assert.assertEquals(0, errOut.getLines().size());

        Assert.assertEquals("message1", outOut.getLines().get(0));

    }

    @Test
    public void testSetupConsoleNoReplaceNoRedirect() {
        UIManagerUtil ui = UIManagerUtil.getInstance();
        run(() -> {
            window.setSize(new Dimension(ui.getScaledSizeInt(800), ui.getScaledSizeInt(300)));
            window.setVisible(true);

            System.setProperty(flagReplaceOut, "false");
            System.setProperty(flagReplaceErr, "false");
            System.setProperty(flagReplaceExceptionHandler, "true");
            System.setProperty(flagRedirectToConsole, "false");
            System.setProperty(flagSuppressOutputRedirection, "true");
            manager.setupConsoleWithDefaultFlags();


            System.out.println("message1");
            System.err.println("message2");
            manager.logString("message3");
        });

        List<GuiLogEntry> es = runGet(() -> window.getList().getLogListModel().getEntries());
        Assert.assertEquals(1, es.size());
        GuiLogEntryString m1 = (GuiLogEntryString) es.get(0);

        Assert.assertEquals("message3", m1.getData());

        Assert.assertEquals(1, outOut.getLines().size());
        Assert.assertEquals(1, errOut.getLines().size());

        Assert.assertEquals("message1", outOut.getLines().get(0));
        Assert.assertEquals("message2", errOut.getLines().get(0));
    }

    @Test
    public void testSetupConsoleOutputRedirection() {
        UIManagerUtil ui = UIManagerUtil.getInstance();
        run(() -> {
            window.setSize(new Dimension(ui.getScaledSizeInt(800), ui.getScaledSizeInt(300)));
            window.setVisible(true);

            System.setProperty(flagReplaceOut, "true");
            System.setProperty(flagReplaceErr, "true");
            System.setProperty(flagReplaceExceptionHandler, "true");
            System.setProperty(flagRedirectToConsole, "true");
            System.setProperty(flagSuppressOutputRedirection, "false");
            manager.setupConsoleWithDefaultFlags();


            System.out.println("message1");
            System.err.println("message2");
            manager.logString("message3");
        });

        List<GuiLogEntry> es = runGet(() -> window.getList().getLogListModel().getEntries());
        Assert.assertEquals(3, es.size());
        GuiLogEntryString m1 = (GuiLogEntryString) es.get(0);
        GuiLogEntryString m2 = (GuiLogEntryString) es.get(1);
        GuiLogEntryString m3 = (GuiLogEntryString) es.get(2);

        Assert.assertEquals("message1", m1.getData());
        Assert.assertEquals("message2", m2.getData());
        Assert.assertEquals("message3", m3.getData());

        Assert.assertEquals(1, outOut.getLines().size());
        Assert.assertEquals(3, errOut.getLines().size());

        Assert.assertEquals("message1", outOut.getLines().get(0));
        Assert.assertEquals("message1", logData(errOut.getLines().get(0)));
        Assert.assertEquals("message2", logData(errOut.getLines().get(1)));
        Assert.assertEquals("message3", logData(errOut.getLines().get(2)));
    }

}
