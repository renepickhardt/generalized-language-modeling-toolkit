package de.typology.smoothing;

import java.io.IOException;

public class AbcTestCorpus extends TestCorpus {

    public AbcTestCorpus() throws IOException, InterruptedException {
        super(resourcesDir.resolve("abc.txt"), resourcesDir.resolve("abc"));
    }

    @Override
    public String[] getWords() {
        return new String[] {
            "a", "b", "c"
        };
    }

    public static void main(String[] args) throws IOException,
            InterruptedException {
        new AbcTestCorpus();
    }

}
