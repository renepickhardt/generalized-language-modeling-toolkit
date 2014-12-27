package de.glmtk;

import java.text.NumberFormat;

import org.fusesource.jansi.Ansi;

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
            Ansi.ansi();
            return name;
        }

        public Integer getNumber() {
            return number;
        }

    }

    private static final int NUM_PERCENTEGEBAR_BLOCKS = 30;

    private static final NumberFormat PERCENT_FORMAT = NumberFormat
            .getPercentInstance();
    static {
        PERCENT_FORMAT.setMinimumFractionDigits(2);
        PERCENT_FORMAT.setMaximumFractionDigits(2);
    }

    private Phase phase = Phase.BEFORE;

    private double percent = 0;

    private boolean lastPrintCorpusAnalyzation = false;

    /**
     * Was the last print a call to {@link ConsoleOutputter#printStatus()}?
     */
    private boolean lastPrintStatus = false;

    public void setPhase(Phase phase, double percent) {
        this.phase = phase;
        this.percent = percent;
        printStatus();
    }

    public void setPercent(double percent) {
        this.percent = percent;
        printStatus();
    }

    public void printStatus() {
        if (lastPrintStatus) {
            System.err.print(Ansi.ansi().cursorUp(1).eraseLine());
        }
        lastPrintStatus = true;

        System.err.print("(" + phase.getNumber() + "/" + Phase.NUM_PHASES
                + ") ");

        System.err.print(phase.getName());

        if (percent == -1.0) {
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

        String percentStr = PERCENT_FORMAT.format(percent);
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

    public void printCorpusAnalyzationDone() {
        if (lastPrintCorpusAnalyzation && lastPrintStatus) {
            System.err.print(Ansi.ansi().cursorUp(1).eraseLine().cursorUp(1)
                    .eraseLine());
        }
        System.err.println("Corpus Analyzation done.");
        lastPrintCorpusAnalyzation = true;
        lastPrintStatus = false;
    }

}
