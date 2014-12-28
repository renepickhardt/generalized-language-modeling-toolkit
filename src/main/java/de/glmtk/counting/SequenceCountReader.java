package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;

import de.glmtk.common.Counter;
import de.glmtk.util.StringUtils;

/* package */class SequenceCountReader implements AutoCloseable {

    /**
     * Compares {@link SequenceCountReader}s based on the {@link #sequence} they
     * have stored.
     */
    public static class SequencerCountReaderComparator implements
            Comparator<SequenceCountReader>, Serializable {

        private static final long serialVersionUID = 3923169929604198131L;

        @Override
        public int compare(SequenceCountReader lhs, SequenceCountReader rhs) {
            if (lhs == rhs) {
                return 0;
            } else if (lhs == null) {
                return 1;
            } else if (rhs == null) {
                return -1;
            } else {
                return StringUtils.compare(lhs.sequence, rhs.sequence);
            }
        }

    }

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

}
