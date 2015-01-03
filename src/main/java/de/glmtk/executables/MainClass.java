package de.glmtk.executables;

import java.util.Arrays;

public class MainClass {
    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            throw new IllegalArgumentException("No main class specified.");

        String mainClass = args[0];
        args = Arrays.copyOfRange(args, 1, args.length);
        switch (mainClass) {
            case "glmtk":
                GlmtkExecutable.main(args);
                break;

            default:
                throw new IllegalStateException(String.format(
                        "Unkown main class specified: '%s'.", mainClass));
        }
    }
}
