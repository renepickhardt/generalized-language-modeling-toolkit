package de.glmtk;

public class Termination extends RuntimeException {

    private static final long serialVersionUID = -6305395239947614194L;

    public Termination() {
        super();
    }

    public Termination(
            String message) {
        super(message);
    }

}
