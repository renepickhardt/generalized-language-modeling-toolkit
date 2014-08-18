package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.IOException;

import de.glmtk.Counter;

/* package */class SequencerCountReader implements AutoCloseable,
Comparable<SequencerCountReader> {

    //    public static final Comparator<SequencerCountReader> COMPARATOR =
    //            new Comparator<SequencerCountReader>() {
    //
    //        @Override
    //                public int compare(
    //                SequencerCountReader a,
    //                SequencerCountReader b) {
    //            return a.compareTo(b);
    //        }
    //
    //    };

    private BufferedReader reader;

    private String sequence;

    private Counter counter;

    public SequencerCountReader(
            BufferedReader reader) throws IOException {
        this.reader = reader;
        nextLine();
    }

    public void nextLine() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            sequence = null;
            counter = null;
        } else {
            counter = new Counter();
            sequence = Counter.getSequenceAndCounter(line, counter);
        }
    }

    public String getSequence() {
        return sequence;
    }

    public Counter getCounter() {
        return counter;
    }

    public boolean isEmpty() {
        return getSequence() == null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public int compareTo(SequencerCountReader other) {
        return getSequence().compareTo(other.getSequence());
    }

}
