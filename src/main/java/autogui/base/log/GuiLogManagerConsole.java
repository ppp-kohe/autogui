package autogui.base.log;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class GuiLogManagerConsole extends GuiLogManager {
    protected PrintStream out = System.err;
    protected GuiLogEntry lastEntry;
    protected boolean controlSequence = true;

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public PrintStream getOut() {
        return out;
    }

    public void setControlSequence(boolean controlSequence) {
        this.controlSequence = controlSequence;
    }

    public boolean isControlSequence() {
        return controlSequence;
    }

    @Override
    public GuiLogEntryString logString(String str) {
        GuiLogEntryString s = super.logString(str);
        out.format("%s %s\n", formatTime(s.getTime()), s.getData());
        lastEntry = s;
        return s;
    }

    @Override
    public GuiLogEntryException logError(Throwable ex) {
        GuiLogEntryException e = super.logError(ex);
        out.print(formatTime(e.getTime()) + " !!! ");
        e.getException().printStackTrace(out);
        out.flush();
        lastEntry = e;
        return e;
    }

    @Override
    public GuiLogEntryProgress logProgress() {
        GuiLogEntryProgress p = super.logProgress();
        out.print(formatTime(p.getTime()) + " # \n\n");
        lastEntry = p;
        return p;
    }

    @Override
    public void updateProgress(GuiLogEntryProgress p) {
        Instant now = Instant.now();
        if (p == lastEntry && controlSequence) {
            out.print("\033[2F\033[K"); //back to top of the 2 previous row, and clear the row
        }
        //1st-line: [YYYY-MM-DD hh:mm:ss.msec] # message
        out.format("%s # %s\n", formatTime(p.getTime()), formatMessageLine(p.getMessage()));
        if (p == lastEntry && controlSequence) {
            out.print("\033[K");
        }
        if (p.isFinished()) {
            //2nd-line: [YYYY-MM-DD hh:mm:ss.msec] # finished: +duration
            out.format("%s # finished: +%s\n",
                    formatTime(p.getEndTime()),
                    formatDuration(p.getTime(), p.getEndTime()));
        } else {
            if (p.isIndeterminate()) {
                //2nd-line: # <emoji> +duration
                Duration d = Duration.between(p.getTime(), now);
                out.print("# " + formatDurationIndeterminate(d) + "  ");
            } else {
                //2nd-line: # NN% |====...   | +duration
                out.print(formatBar(p.getValueP()));
            }
            out.format(" +%s\n", formatDuration(p.getTime(), now));
        }

        lastEntry = p;
    }

    public String formatDurationIndeterminate(Duration d) {
        //f0 9f 95 90-9B,
        long halfSec = d.getNano() / 500_000_000L;
        int progress = (int) ((d.getSeconds() % 4) * 2 + halfSec) % 12;
        byte[] chars = {(byte) 0xf0, (byte) 0x9f, (byte) 0x95, (byte) (0x90 | progress)};
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(chars)).toString();
    }

    public String formatBar(double p) {
        int n = (int) (p * 100);
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("# %2d", n)).append("% |");
        int max = 50;
        for (int i = 0, l = (int) (p * max); i < l; ++i) {
            buf.append('=');
            --max;
        }
        for (int i = 0; i < max; ++i) {
            buf.append(' ');
        }
        buf.append('|');
        return buf.toString();
    }

}
