package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A class for aggregating sequences by counting their occurrences. Expects an
 * inputStream with a size that is 30% of the allocated main memory.
 * 
 * @author Martin Koerner
 * 
 */
public class Aggregator {

    private InputStream input;

    private OutputStream output;

    private String delimiter;

    private boolean additionalCounts;

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

    public void aggregate() throws IOException {
        SortedMap<String, Counter> sequenceCounts =
                new TreeMap<String, Counter>();

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
