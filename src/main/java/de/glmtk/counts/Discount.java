package de.glmtk.counts;

public class Discount {
    private double one;
    private double two;
    private double threePlus;

    public Discount() {
        this(0L, 0L, 0L);
    }

    public Discount(double one,
                    double two,
                    double threePlus) {
        set(one, two, threePlus);
    }

    public double getOne() {
        return one;
    }

    public double getTwo() {
        return two;
    }

    public double getThree() {
        return threePlus;
    }

    public void setOne(double one) {
        this.one = one;
    }

    public void setTwo(double two) {
        this.two = two;
    }

    public void setThreePlus(double threePlus) {
        this.threePlus = threePlus;
    }

    public void set(double one,
                    double two,
                    double threePlus) {
        this.one = one;
        this.two = two;
        this.threePlus = threePlus;
    }
}
