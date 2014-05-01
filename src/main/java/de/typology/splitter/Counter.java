package de.typology.splitter;

public class Counter {

    private long onePlusCount = 0;

    private long oneCount = 0;

    private long twoCount = 0;

    private long threePlusCount = 0;

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

    public long getOneCount() {
        return oneCount;
    }

    public long getTwoCount() {
        return twoCount;
    }

    public long getThreePlusCount() {
        return threePlusCount;
    }

}
