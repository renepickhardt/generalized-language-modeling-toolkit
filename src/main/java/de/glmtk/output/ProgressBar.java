package de.glmtk.output;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static de.glmtk.output.Output.eraseLine;
import static de.glmtk.output.Output.getTerminalWidth;
import static de.glmtk.output.Output.isFormattingEnabled;
import static de.glmtk.output.Output.printlnVolatile;
import static de.glmtk.util.StringUtils.repeat;
import static java.lang.Math.ceil;
import static java.lang.Math.log10;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class ProgressBar {
    private static final long DISPLAY_INTERVAL = 200;

    private List<String> phases;
    private String curPhase;
    private int maxPhaseLength;
    private int curIndex;
    private int maxIndex;
    private long current;
    private long total;
    private long lastDisplay;

    public ProgressBar(String phase,
                       long total) {
        this(ImmutableList.of(phase));
        setPhase(phase, total);
    }

    public ProgressBar(String... phases) {
        this(ImmutableList.copyOf(phases));
    }

    public ProgressBar(Collection<String> phases) {
        checkNotNull(phases);
        checkArgument(!phases.isEmpty());

        maxPhaseLength = 0;
        for (String phase : phases) {
            checkNotNull(phase);
            checkArgument(!phase.isEmpty());
            if (maxPhaseLength < phase.length())
                maxPhaseLength = phase.length();
        }

        if (phases instanceof List)
            this.phases = (List<String>) phases;
        else
            this.phases = ImmutableList.copyOf(phases);

        curPhase = null;
        curIndex = 0;
        maxIndex = this.phases.size();
    }

    public void setPhase(String phase,
                         long total) {
        checkNotNull(phase);
        checkArgument(phases.contains(phase),
                "Given phase was not specified in constructor.");

        curPhase = phase;
        curIndex = phases.indexOf(phase) + 1; // indexOf() starts at 0.
        current = 0;
        this.total = total;
        lastDisplay = 0;
        display();
    }

    public void increase() {
        increase(1);
    }

    public void increase(long increase) {
        set(current + increase);
    }

    public void set(long current) {
        this.current = current;
        if (current == total) {
            // We always want to display the 100%
            forceDisplay();
            // No longer need to update after 100% though
            lastDisplay = Long.MAX_VALUE;
        } else
            display();
    }

    public void set(double percent) {
        set((long) ceil(percent * total));
    }

    public void display() {
        long time = currentTimeMillis();
        if (time - lastDisplay < DISPLAY_INTERVAL)
            return;
        lastDisplay = time;

        forceDisplay();
    }

    private void forceDisplay() {
        float percent = (float) current / total;

        String beforeBlocks = format("(%" + numDigits(numDigits(phases.size()))
                + "d/%d) %-" + maxPhaseLength + "s [", curIndex, maxIndex,
                curPhase);
        String afterBlocks = format("] %6.2f%%", 100.0 * percent);

        int lengthWithoutBlocks = beforeBlocks.length() + afterBlocks.length();
        int widthForBlocks = 80 - lengthWithoutBlocks;
        if (isFormattingEnabled())
            widthForBlocks = getTerminalWidth() - lengthWithoutBlocks;
        int numBlocks = (int) ceil(percent * widthForBlocks);

        eraseLine();
        printlnVolatile(beforeBlocks + repeat("#", numBlocks)
                + repeat("-", widthForBlocks - numBlocks) + afterBlocks);
    }

    private static int numDigits(long number) {
        return (int) log10(number) + 1;
    }
}
