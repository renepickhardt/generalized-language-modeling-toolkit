package de.glmtk.exceptions;

import java.nio.file.Path;

public class WrongStatusException extends RuntimeException {

    private static final long serialVersionUID = 37535866048450910L;

    public WrongStatusException(String task,
                                Path file) {
        super(
                String.format(
                        "Status reports %s as completed. But file '%s' does not exist.",
                        task, file));
    }

}
