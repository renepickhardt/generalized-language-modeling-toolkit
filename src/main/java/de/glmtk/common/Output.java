package de.glmtk.common;

import static de.glmtk.Config.CONFIG;

import java.io.IOException;
import java.util.Formatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;

import de.glmtk.util.StringUtils;

public enum Output {

    OUTPUT;

    // TODO: Check if this works on windows.

    public static enum Phase {

        ABSOLUTE_CHUNKING(1, 5, "Chunking Absolute"),

        ABSOLUTE_MERGING(2, 5, "Merging Absolute"),

        CONTINUATION_CHUNKING(3, 5, "Chunking Continuation"),

        CONTINUATION_MERGING(4, 5, "Merging Continuation"),

        EVALUATING(5, 5, "Evaluating"),

        QUERYING_FILE(1, 1, "Querying");

        public static final int MAX_NAME_LENGTH;

        static {
            int max = 0;
            for (Phase phase : Phase.values()) {
                if (max < phase.name.length()) {
                    max = phase.name.length();
                }
            }
            MAX_NAME_LENGTH = max;
        }

        private int number;

        private int maxNumber;

        private String name;

        private Phase(
                int number,
                int maxNumber,
                String name) {
            this.number = number;
            this.maxNumber = maxNumber;
            this.name = name;
        }

        public int getNumber() {
            return number;
        }

        public int getMaxNumber() {
            return maxNumber;
        }

        public String getName() {
            return name;
        }

    }

    public static class Progress {

        private long current;

        private long total;

        private long lastConsoleUpdate;

        private long lastLogUpdate;

        private boolean updateConsole;

        private boolean updateLog;

        public Progress(
                long total) throws IOException {
            current = 0;
            this.total = total;
            lastConsoleUpdate = System.currentTimeMillis();
            lastLogUpdate = lastConsoleUpdate;
            updateConsole = CONFIG.getConsoleUpdateInterval() != 0;
            updateLog = CONFIG.getLogUpdateInterval() != 0;
        }

        public void increase(long increase) {
            current += increase;

            long time = System.currentTimeMillis();

            if (updateConsole
                    && time - lastConsoleUpdate >= CONFIG
                    .getConsoleUpdateInterval()) {
                OUTPUT.setPercent((double) current / total);
                lastConsoleUpdate = time;
            }
            if (updateLog
                    && time - lastLogUpdate >= CONFIG.getLogUpdateInterval()) {
                LOGGER.info("%6.2f%%", 100.0 * current / total);
                lastLogUpdate = time;
            }
        }

    }

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(Output.class);

    private static final double DISABLE_PERCENT = -1.0;

    /**
     * See <a href="http://stackoverflow.com/a/3758880/211404">Stack Overflow:
     * How to convert byte size into human readable format in java?</a>
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre =
                (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private boolean ansiEnabled = false;

    private Phase phase = null;

    private double percent = 0;

    /**
     * {@code true} if the last print was a call to {@link #beginPhases(String)}
     * , or followed only by {@link #printPhase()} calls. {@code false} if not.
     */
    private boolean lastPrintBeginPhases = false;

    /**
     * {@code true} if the last print was a call to {@link #printPhase()}.
     * {@code false} if not.
     */
    private boolean lastPrintPhase = false;

    private int numPercentegebarBlocks;

    private Output() {
        AnsiConsole.systemInstall();

        Integer columns =
                Integer.valueOf(System.getProperty("glmtk.columns", "80"));
        numPercentegebarBlocks = columns - 17 - Phase.MAX_NAME_LENGTH;
    }

    public void enableAnsi() {
        ansiEnabled = true;
    }

    public void disableAnsi() {
        ansiEnabled = false;
    }

    public void setPhase(Phase phase, boolean progress) {
        this.phase = phase;
        percent = progress ? 0.0 : -1.0;
        printPhase();
    }

    public void setPercent(double percent) {
        this.percent = percent;
        printPhase();
    }

    public String bold(Object object) {
        return bold(object.toString());
    }

    public String bold(String message) {
        if (ansiEnabled) {
            return Ansi.ansi().bold() + message + Ansi.ansi().boldOff();
        } else {
            return message;
        }
    }

    public void beginPhases(String message) {
        System.err.println(message);

        lastPrintBeginPhases = true;
        lastPrintPhase = false;
    }

    public void endPhases(String message) {
        if (ansiEnabled && lastPrintBeginPhases && lastPrintPhase) {
            System.err.print(Ansi.ansi().cursorUp(1).eraseLine().cursorUp(1)
                    .eraseLine());
        }

        System.err.println(message);

        lastPrintBeginPhases = false;
        lastPrintPhase = false;
    }

    private void printPhase() {
        if (phase == null) {
            return;
        }

        if (ansiEnabled && lastPrintPhase) {
            System.err.print(Ansi.ansi().cursorUp(1).eraseLine());
        }

        String message;
        try (Formatter f = new Formatter()) {
            f.format("(%d/%d) ", phase.getNumber(), phase.getMaxNumber());
            if (percent == DISABLE_PERCENT) {
                f.format("%s...", phase.getName());
            } else {
                int numBlocks =
                        (int) Math.ceil(percent * numPercentegebarBlocks);
                f.format(
                        "%-" + Phase.MAX_NAME_LENGTH + "s [%s%s] %6.2f%%",
                        phase.getName(),
                        StringUtils.repeat("#", numBlocks),
                        StringUtils.repeat("-", numPercentegebarBlocks
                                - numBlocks), 100.0 * percent);
            }

            message = f.toString();
        }

        System.err.println(message);

        lastPrintPhase = true;
    }

    public void printMessage(Object message) {
        printMessage(message.toString());
    }

    public void printMessage(String message) {
        System.err.println(message);

        lastPrintBeginPhases = false;
        lastPrintPhase = false;
    }

    public void printError(Object object) {
        printError(object.toString());
    }

    public void printError(Throwable throwable) {
        printError(throwable.getMessage());
    }

    public void printError(String message) {
        if (ansiEnabled) {
            System.err.print(Ansi.ansi().fg(Color.RED));
        }
        if (message == null || message.isEmpty()) {
            message =
                    "A critical error has occured, program execution had to be stopped.";
        }
        System.err.println("Error: " + message);

        lastPrintBeginPhases = false;
        lastPrintPhase = false;
    }

    public void printWarning(Object object) {
        printWarning(object.toString());
    }

    public void printWarning(String message) {
        if (ansiEnabled) {
            System.err.print(Ansi.ansi().fg(Color.YELLOW));
        }
        if (message == null || message.isEmpty()) {
            message = "A warning has occured.";
        }
        System.err.println("Warning: " + message);

        lastPrintBeginPhases = false;
        lastPrintPhase = false;
    }
}
