package de.glmtk.common;

/**
 * This class is used for counting continuation counts It is also a wrapper
 * class to handle the continuation counts during Kneser Ney Smoothing. It is
 * thus called during training and also during testing.
 */
public class Counts {
    private long onePlusCount;
    private long oneCount;
    private long twoCount;
    private long threePlusCount;

    public Counts() {
        onePlusCount = 0;
        oneCount = 0;
        twoCount = 0;
        threePlusCount = 0;
    }

    public Counts(long onePlusCount,
                  long oneCount,
                  long twoCount,
                  long threePlusCount) {
        set(onePlusCount, oneCount, twoCount, threePlusCount);
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

    public void setOnePlusCount(long onePlusCount) {
        this.onePlusCount = onePlusCount;
    }

    public void setOneCount(long oneCount) {
        this.oneCount = oneCount;
    }

    public void setTwoCount(long twoCount) {
        this.twoCount = twoCount;
    }

    public void setThreePlusCount(long threePlusCount) {
        this.threePlusCount = threePlusCount;
    }

    public void set(long onePlusCount,
                    long oneCount,
                    long twoCount,
                    long threePlusCount) {
        this.onePlusCount = onePlusCount;
        this.oneCount = oneCount;
        this.twoCount = twoCount;
        this.threePlusCount = threePlusCount;
    }

    public void add(Counts counts) {
        onePlusCount += counts.onePlusCount;
        oneCount += counts.oneCount;
        twoCount += counts.twoCount;
        threePlusCount += counts.threePlusCount;
    }

    public void add(long count) {
        onePlusCount += count;
        if (count == 1)
            ++oneCount;
        if (count == 2)
            ++twoCount;
        if (count >= 3)
            ++threePlusCount;
    }

    public void addOne(long count) {
        ++onePlusCount;
        if (count == 1)
            ++oneCount;
        else if (count == 2)
            ++twoCount;
        else if (count >= 3)
            ++threePlusCount;
    }

    /**
     * Currently only used in testing.
     *
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        else if (other == null || getClass() != other.getClass())
            return false;

        Counts o = (Counts) other;
        if (onePlusCount != o.onePlusCount)
            return false;
        else if (oneCount != o.oneCount)
            return false;
        else if (twoCount != o.twoCount)
            return false;
        else if (threePlusCount != o.threePlusCount)
            return false;
        return true;
    }

    /**
     * Has to be implemented, because {@link #equals(Object)} is implemented.
     *
     * <p>
     * Implementation guided by: <a href=
     * "http://www.angelikalanger.com/Articles/EffectiveJava/03.HashCode/03.HashCode.html"
     * >Angelika Langer: Implementing the hashCode() Method</a>
     *
     * <p>
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 23984;
        int mult = 457;

        hash += mult * onePlusCount;
        hash += mult * oneCount;
        hash += mult * twoCount;
        hash += mult * threePlusCount;

        return hash;
    }
}
