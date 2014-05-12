package de.typology.counting;

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

    public String toString(String delimiter) {
        return onePlusCount + delimiter + oneCount + delimiter + twoCount
                + delimiter + threePlusCount;
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

}
