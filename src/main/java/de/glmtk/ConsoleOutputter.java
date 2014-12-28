package de.glmtk;

import java.text.NumberFormat;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import de.glmtk.utils.StringUtils;

public class ConsoleOutputter {

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

        private int number;

        private Phase(
                String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Integer getNumber() {
            return number;
        }

    }

    public static final double DISABLE_PERCENT = -1.0;

    private static final int NUM_PERCENTEGEBAR_BLOCKS = 30;

    private static ConsoleOutputter instance = null;

    public static ConsoleOutputter getInstance() {
        if (instance == null) {
            instance = new ConsoleOutputter();
        }
        return instance;
    }

    private Phase phase = Phase.BEFORE;

    private double percent = 0;

    private boolean lastPrintCorpusAnalyzation = false;

    private boolean ansiEnabled = false;

    /**
     * Was the last print a call to {@link ConsoleOutputter#printStatus()}?
     */
    private boolean lastPrintStatus = false;

    private ConsoleOutputter() {
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
        if (ansiEnabled && lastPrintStatus) {
            System.err.print(Ansi.ansi().cursorUp(1).eraseLine());
        }
        lastPrintStatus = true;

        System.err.print("(" + phase.getNumber() + "/" + Phase.NUM_PHASES
                + ") ");

        System.err.print(phase.getName());

        if (percent == DISABLE_PERCENT) {
            System.err.print("...");
        } else {
            System.err.print(StringUtils.repeat(" ", Phase.MAX_NAME_LENGTH + 1
                    - phase.getName().length()));

            printPercentegeBar();
        }

        System.err.println();
    }

    private void printPercentegeBar() {
        int numBlocks = (int) Math.ceil(percent * NUM_PERCENTEGEBAR_BLOCKS);

        System.err.print("[");
        System.err.print(StringUtils.repeat("#", numBlocks));
        System.err.print(StringUtils.repeat(" ", NUM_PERCENTEGEBAR_BLOCKS
                - numBlocks));
        System.err.print("]");

        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMinimumFractionDigits(2);
        percentFormat.setMaximumFractionDigits(2);
        String percentStr = percentFormat.format(percent);
        System.err.print(StringUtils.repeat(" ", " ###.##%".length()
                - percentStr.length()));
        System.err.print(percentStr);
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
        if (ansiEnabled && lastPrintCorpusAnalyzation && lastPrintStatus) {
            System.err.print(Ansi.ansi().cursorUp(1).eraseLine().cursorUp(1)
                    .eraseLine());
        }
        System.err.println("Corpus Analyzation done ("
                + humanReadableByteCount(size, false) + " taken).");
        lastPrintCorpusAnalyzation = true;
        lastPrintStatus = false;
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
