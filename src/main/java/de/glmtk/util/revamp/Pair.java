package de.glmtk.util.revamp;

public class Pair<L, R> {
    public static <L, R> Pair<L, R> pair(L left,
                                         R right) {
        return new Pair<>(left, right);
    }

    public L left;
    public R right;

    public Pair(L left,
                R right) {
        this.left = left;
        this.right = right;
    }
}
