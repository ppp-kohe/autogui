package autogui.base.log;

import java.time.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

public class GuiLogManager {
    public GuiLogEntryString log(Object... args) {
        Formatter formatter = new Formatter();
        boolean needSpace = false;
        for (Object arg : args) {
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
        }
        return logString(formatter.toString());
    }

    public boolean isSymbol(char c) {
        return (0x20 <= c && c <= 0x2f) ||
                (0x3a <= c && c <= 0x40) ||
                (0x5b <= c && c <= 0x60) ||
                (0x7b <= c && c <= 0x7f);
    }

    public GuiLogEntryString logFormat(String format, Object... args) {
        return logString(String.format(format, args));
    }

    public GuiLogEntryString logString(String str) {
        return new GuiLogEntryString(str);
    }

    public String formatTime(Instant i) {
        LocalDateTime time = LocalDateTime.ofInstant(i, ZoneId.systemDefault());
        return String.format("[%tF %tT.%tL]", time, time, time);
    }

    public GuiLogEntryException logError(Throwable ex) {
        return new GuiLogEntryException(ex);
    }

    public GuiLogEntryProgress logProgress() {
        GuiLogEntryProgress p = new GuiLogEntryProgress();
        p.addListener(this::updateProgress);
        return p;
    }

    public GuiLogEntryProgress logProgress(int max) {
        GuiLogEntryProgress p = logProgress();
        p.setMaximum(max);
        p.setIndeterminate(false);
        return p;
    }

    public void updateProgress(GuiLogEntryProgress p) { }

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
                parts.add(String.format("%,dy", years));
            }
            if (!parts.isEmpty() || months > 0) {
                parts.add(months + "mo");
            }
            if (!parts.isEmpty() || days > 0) {
                parts.add(days + "d,");
            }
        }

        if (!parts.isEmpty() || hourPart > 0) {
            parts.add(hourPart + "h");
        }
        if (!parts.isEmpty() || minPart > 0) {
            parts.add(minPart + "m");
        }
        if (!parts.isEmpty() || secPart > 0) {
            parts.add(secPart + "s");
        }

        if (msPart > 0) {
            parts.add(msPart + "ms");
        } else if (parts.isEmpty()) {
            parts.add(String.format("%,dns", nsPart));
        }
        return String.join(" ", parts);
    }
}
