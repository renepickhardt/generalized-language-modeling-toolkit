package de.glmtk.executables;

import java.util.Arrays;

public class MainClass {

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalStateException("No main class specified.");
        }

        String mainClass = args[0];
        args = Arrays.copyOfRange(args, 1, args.length);
        switch (mainClass) {
            case "glmtk":
                GlmtkExecutable.main(args);
                break;

            default:
                throw new IllegalStateException("Unkown main class specified.");
        }
    }

}
