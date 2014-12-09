package de.glmtk.counting;

import java.util.List;

import de.glmtk.utils.StringUtils;

/**
 * This class is used for counting continuation counts
 * It is also a wrapper class to handle the continuation counts during
 * Kneser Ney Smoothing. It is thus called during training and also
 * during testing.
 */
public class Counter {

    private long onePlusCount;

    private long oneCount;

    private long twoCount;

    private long threePlusCount;

    public Counter() {
        onePlusCount = 0;
        oneCount = 0;
        twoCount = 0;
        threePlusCount = 0;
    }

    public Counter(
            long onePlusCount,
            long oneCount,
            long twoCount,
            long threePlusCount) {
        this.onePlusCount = onePlusCount;
        this.oneCount = oneCount;
        this.twoCount = twoCount;
        this.threePlusCount = threePlusCount;
    }

    public void add(long count) {
        onePlusCount += count;
        if (count == 1) {
            oneCount += count;
        }
        if (count == 2) {
            twoCount += count;
        }
        if (count >= 3) {
            threePlusCount += count;
        }
    }

    public void add(Counter counter) {
        onePlusCount += counter.onePlusCount;
        oneCount += counter.oneCount;
        twoCount += counter.twoCount;
        threePlusCount += counter.threePlusCount;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(Long.toString(onePlusCount));
        result.append('\t');
        result.append(Long.toString(oneCount));
        result.append('\t');
        result.append(Long.toString(twoCount));
        result.append('\t');
        result.append(Long.toString(threePlusCount));
        return result.toString();
    }

    public long getOnePlusCount() {
        return onePlusCount;
    }

    public void setOnePlusCount(long onePlusCount) {
        this.onePlusCount = onePlusCount;
    }

    public long getOneCount() {
        return oneCount;
    }

    public void setOneCount(long oneCount) {
        this.oneCount = oneCount;
    }

    public long getTwoCount() {
        return twoCount;
    }

    public void setTwoCount(long twoCount) {
        this.twoCount = twoCount;
    }

    public long getThreePlusCount() {
        return threePlusCount;
    }

    public void setThreePlusCount(long threePlusCount) {
        this.threePlusCount = threePlusCount;
    }

    public static String getSequenceAndCounter(String line, Counter counter) {
        List<String> split = StringUtils.splitAtChar(line, '\t');
        if (split.size() == 2) {
            // absolute
            counter.setOnePlusCount(Long.valueOf(split.get(1)));
            counter.setOneCount(0L);
            counter.setTwoCount(0L);
            counter.setThreePlusCount(0L);
        } else if (split.size() == 5) {
            // continuation
            counter.setOnePlusCount(Long.valueOf(split.get(1)));
            counter.setOneCount(Long.valueOf(split.get(2)));
            counter.setTwoCount(Long.valueOf(split.get(3)));
            counter.setThreePlusCount(Long.valueOf(split.get(4)));
        } else {
            throw new IllegalArgumentException(
                    "Couldn't not get Sequence and Counter of line '" + line
                            + "'.");
        }
        return split.get(0);
    }

}
