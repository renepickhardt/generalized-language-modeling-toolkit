package de.typology.counting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Groups multiple sequences with counts into distinct sequences with aggregated
 * counts.
 * 
 * Expects an {@link InputStream} with a size that is 30% of the allocated main
 * memory.
 */
public class Aggregator {

    private InputStream input;

    private OutputStream output;

    private String delimiter;

    private boolean additionalCounts;

    /**
     * Expects an {@code input} where each line is formatted as
     * {@code <Sequence><Delimiter><Count>}. For each distinct sequence it
     * writes to {@code output}:
     * {@code <Sequence><Delimiter><1+Count><Delimiter><1Count><Delimiter><2Count><Delimiter><3+Count>}
     * where:
     * 
     * <ul>
     * <li>{@code 1+Count} is the aggregated count of this sequence.</li>
     * <li>{@code 1Count} is the number of lines where count was {@code 1} for
     * this sequence.</li>
     * <li>{@code 2Count} is the number of lines where count was {@code 2} for
     * this sequence.</li>
     * <li>{@code 3+Count} is the number of lines where count was {@code 3+} for
     * this sequence.</li>
     * </ul>
     * 
     * @param input
     *            {@link InputStream} to be read.
     * @param output
     *            {@link OutputStream} to be written to.
     * @param delimiter
     *            Delimiter that separates Sequences and Counts.
     * @param additionalCounts
     *            If {@code true} will act as described above. If {@code false}
     *            will only output {@code <Sequence><Delimiter><1+Count>}.
     */
    public Aggregator(
            InputStream input,
            OutputStream output,
            String delimiter,
            boolean additionalCounts) {
        this.input = input;
        this.output = output;
        this.delimiter = delimiter;
        this.additionalCounts = additionalCounts;
    }

    /**
     * Perform the actual aggregating and writing output.
     */
    public void aggregate() throws IOException {
        Map<String, Counter> sequenceCounts = new TreeMap<String, Counter>();

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(delimiter);
                String sequence = split[0];
                long count = Long.parseLong(split[1]);

                if (sequence.length() == 0) {
                    continue;
                }

                Counter counter = sequenceCounts.get(sequence);
                if (counter == null) {
                    counter = new Counter();
                    sequenceCounts.put(sequence, counter);
                }
                counter.add(count);
            }
        }

        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(output))) {
            for (Entry<String, Counter> entry : sequenceCounts.entrySet()) {
                String sequence = entry.getKey();
                Counter counter = entry.getValue();
                writer.write(sequence
                        + delimiter
                        + (additionalCounts
                                ? counter.toString(delimiter)
                                : counter.getOnePlusCount()) + "\n");
            }
        }
    }

}
