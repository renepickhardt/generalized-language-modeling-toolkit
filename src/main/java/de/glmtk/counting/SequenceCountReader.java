package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.IOException;

import de.glmtk.utils.Counter;

/* package */class SequenceCountReader implements AutoCloseable,
Comparable<SequenceCountReader> {

    private BufferedReader reader;

    private String sequence;

    private Counter counter;

    public SequenceCountReader(
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
        return sequence == null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public int compareTo(SequenceCountReader other) {
        if (this == other) {
            return 0;
        } else if (other == null) {
            return -1;
        }

        if (sequence == null) {
            if (other.sequence == null) {
                return 0;
            } else {
                return 1;
            }
        } else if (other.sequence == null) {
            return -1;
        }

        return sequence.compareTo(other.sequence);
    }

}
