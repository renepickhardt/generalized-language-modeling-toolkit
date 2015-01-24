package de.glmtk.counts;

public class LambdaCount {
    private double high;
    private double low;

    public LambdaCount() {
        this(0.0, 0.0);
    }

    public LambdaCount(double high,
                       double low) {
        set(high, low);
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public void set(double high,
                    double low) {
        this.high = high;
        this.low = low;
    }
}
