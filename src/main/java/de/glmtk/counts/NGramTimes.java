package de.glmtk.counts;

public class NGramTimes {
    private long oneCount;
    private long twoCount;
    private long threeCount;
    private long fourCount;

    public NGramTimes() {
        this(0L, 0L, 0L, 0L);
    }

    public NGramTimes(long oneCount,
                      long twoCount,
                      long threeCount,
                      long fourCount) {
        set(oneCount, twoCount, threeCount, fourCount);
    }

    public long getOneCount() {
        return oneCount;
    }

    public long getTwoCount() {
        return twoCount;
    }

    public long getThreeCount() {
        return threeCount;
    }

    public long getFourCount() {
        return fourCount;
    }

    public void setOneCount(long oneCount) {
        this.oneCount = oneCount;
    }

    public void setTwoCount(long twoCount) {
        this.twoCount = twoCount;
    }

    public void setThreeCount(long threeCount) {
        this.threeCount = threeCount;
    }

    public void setFourCount(long fourCount) {
        this.fourCount = fourCount;
    }

    public void set(long oneCount,
                    long twoCount,
                    long threeCount,
                    long fourCount) {
        this.oneCount = oneCount;
        this.twoCount = twoCount;
        this.threeCount = threeCount;
        this.fourCount = fourCount;
    }

    public void add(long count) {
        if (count == 1)
            ++oneCount;
        else if (count == 2)
            ++twoCount;
        else if (count == 3)
            ++threeCount;
        else if (count == 4)
            ++fourCount;
    }
}
