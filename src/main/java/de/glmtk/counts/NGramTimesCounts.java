package de.glmtk.counts;

public class NGramTimesCounts {
    private long oneCount;
    private long twoCount;
    private long threeCount;
    private long fourCount;

    public NGramTimesCounts() {
        this(0L, 0L, 0L, 0L);
    }

    public NGramTimesCounts(long oneCount,
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
}
