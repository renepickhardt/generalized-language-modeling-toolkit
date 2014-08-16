package de.glmtk.counting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import de.glmtk.Counter;
import de.glmtk.utils.StringUtils;

/* package */class SequencerCountReader implements AutoCloseable,
        Comparable<SequencerCountReader> {

    public static final Comparator<SequencerCountReader> COMPARATOR =
            new Comparator<SequencerCountReader>() {

                @Override
        public int compare(
                        SequencerCountReader a,
                        SequencerCountReader b) {
                    return a.compareTo(b);
                }

            };

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
            List<String> split = StringUtils.splitAtChar(line, '\t');
            sequence = split.get(0);
            if (split.size() == 2) {
                // absolute
                counter = new Counter(Long.valueOf(split.get(1)), 0, 0, 0);
            } else {
                // continuation
                counter =
                        new Counter(Long.valueOf(split.get(1)),
                                Long.valueOf(split.get(2)), Long.valueOf(split
                                        .get(3)), Long.valueOf(split.get(4)));
            }
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
