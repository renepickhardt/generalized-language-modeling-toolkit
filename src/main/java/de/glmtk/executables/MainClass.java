package de.glmtk.executables;

import java.util.Arrays;

public class MainClass {
    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            throw new IllegalArgumentException("No main class specified.");

        String mainClass = args[0];
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (mainClass) {
            case "glmtk":
                GlmtkExecutable.main(newArgs);
                break;

            default:
                throw new IllegalArgumentException(String.format(
                        "Unkown main class specified: '%s'.", mainClass));
        }
    }
}
