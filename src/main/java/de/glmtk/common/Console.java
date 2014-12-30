package de.glmtk.common;

import java.util.Formatter;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import de.glmtk.util.StringUtils;

public enum Console {

    CONSOLE;

    // TODO: Check if this works on windows.

    public static enum Phase {

        BEFORE(""),

        ABSOLUTE_CHUNKING("Chunking Absolute"),

        ABSOLUTE_MERGING("Merging Absolute"),

        CONTINUATION_CHUNKING("Chunking Continuation"),

        CONTINUATION_MERGING("Merging Continuation"),

        EVALUATING("Evaluating"),

        AFTER("");

        public static final int MAX_NAME_LENGTH;

        public static final int NUM_PHASES;

        static {
            int number = 0;
            int max = 0;
            for (Phase phase : Phase.values()) {
                if (phase != BEFORE && phase != AFTER) {
                    phase.number = ++number;
                }

                if (max < phase.name.length()) {
                    max = phase.name.length();
                }
            }
            MAX_NAME_LENGTH = max;
            NUM_PHASES = number;
        }

        private String name;

        private int number = -1;

        private Phase(
                String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public int getNumber() {
            return number;
        }

    }

    public static final double DISABLE_PERCENT = -1.0;

    private static final int NUM_PERCENTEGEBAR_BLOCKS = 30;

    private Phase phase = Phase.BEFORE;

    private double percent = 0;

    private boolean lastPrintCorpusAnalyzation = false;

    private boolean ansiEnabled = false;

    /**
     * Was the last print a call to {@link Console#printStatus()}?
     */
    private boolean lastPrintStatus = false;

    private Console() {
        AnsiConsole.systemInstall();
    }

    public void enableAnsi() {
        ansiEnabled = true;
    }

    public void disableAnsi() {
        ansiEnabled = false;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
        percent = DISABLE_PERCENT;
        printStatus();
    }

    public void setPhase(Phase phase, double percent) {
        this.phase = phase;
        this.percent = percent;
        printStatus();
    }

    public void setPercent(double percent) {
        this.percent = percent;
        printStatus();
    }

    public String bold(String string) {
        if (ansiEnabled) {
            return Ansi.ansi().bold() + string + Ansi.ansi().boldOff();
        } else {
            return string;
        }
    }

    public void printStatus() {
        try (Formatter f = new Formatter()) {
            if (ansiEnabled && lastPrintStatus) {
                System.err.print(Ansi.ansi().cursorUp(1).eraseLine());
            }
            lastPrintStatus = true;

            f.format("(%d/%d) ", phase.getNumber(), Phase.NUM_PHASES);
            if (percent == DISABLE_PERCENT) {
                f.format("%s...", phase.getName());
            } else {
                int numBlocks =
                        (int) Math.ceil(percent * NUM_PERCENTEGEBAR_BLOCKS);
                f.format("%-" + Phase.MAX_NAME_LENGTH + "s [%-"
                        + NUM_PERCENTEGEBAR_BLOCKS + "s] %6.2f%%",
                        phase.getName(), StringUtils.repeat("#", numBlocks),
                        100.0 * percent);
            }
            System.err.println(f.toString());
        }
    }

    public void printMessage(Object message) {
        printMessage(message.toString());
    }

    public void printMessage(String message) {
        lastPrintCorpusAnalyzation = false;
        lastPrintStatus = false;
        System.err.println(message);
    }

    public void printCorpusAnalyzationInProcess() {
        lastPrintCorpusAnalyzation = true;
        System.err.println("Corpus Analyzation...");
    }

    public void printCorpusAnalyzationDone(long size) {
        try (Formatter f = new Formatter()) {
            if (ansiEnabled && lastPrintCorpusAnalyzation && lastPrintStatus) {
                System.err.print(Ansi.ansi().cursorUp(1).eraseLine()
                        .cursorUp(1).eraseLine());
            }
            System.err.println(f.format("Corpus Analyzation done (%s taken).",
                    humanReadableByteCount(size, false)));
            lastPrintCorpusAnalyzation = true;
            lastPrintStatus = false;
        }
    }

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

}
