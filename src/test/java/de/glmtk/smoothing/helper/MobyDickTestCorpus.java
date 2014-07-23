package de.glmtk.smoothing.helper;

import java.io.IOException;

public class MobyDickTestCorpus extends TestCorpus {

    public MobyDickTestCorpus() throws IOException, InterruptedException {
        super(resourcesDir.resolve("mobydick.txt"), resourcesDir
                .resolve("mobydick"));
    }

    @Override
    public String[] getWords() {
        return new String[] {
            "A", "BOOK", "BY", "CHER", "DICK", "DIFFERENT", "JOHN", "MARY",
            "MOBY", "READ", "SHE"
        };
    }

    public static void main(String[] args) throws IOException,
            InterruptedException {
        new MobyDickTestCorpus();
    }

}
