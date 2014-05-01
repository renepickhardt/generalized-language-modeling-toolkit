package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Comparator;
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

    // this comparator is based on the value of startSortAtColumn
    private Comparator<String> stringComparator = new Comparator<String>() {

        @Override
        public int compare(String string1, String string2) {
            return string1.compareTo(string2);
        }

    };

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
        SortedMap<String, Long[]> wordMapAdditionalCounts =
                new TreeMap<String, Long[]>(stringComparator);
        SortedMap<String, Long> wordMapNoAdditionalCounts =
                new TreeMap<String, Long>(stringComparator);

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

                if (additionalCounts) {
                    addCountWithAdditional(wordMapAdditionalCounts, sequence,
                            count);
                } else {
                    addCountWithNoAdditional(wordMapNoAdditionalCounts,
                            sequence, count);
                }
            }
        }

        try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(output))) {
            if (additionalCounts) {
                for (Entry<String, Long[]> entry : wordMapAdditionalCounts
                        .entrySet()) {
                    String words = entry.getKey();
                    // [0]=1+
                    // [1]=1
                    // [2]=2
                    // [3]=3+
                    writer.write(words + delimiter + entry.getValue()[0]
                            + delimiter + entry.getValue()[1] + delimiter
                            + entry.getValue()[2] + delimiter
                            + entry.getValue()[3] + "\n");
                }
            } else {
                for (Entry<String, Long> entry : wordMapNoAdditionalCounts
                        .entrySet()) {
                    String words = entry.getKey();
                    writer.write(words + delimiter + entry.getValue() + "\n");
                }
            }
        }
    }

    private void addCountWithNoAdditional(
            SortedMap<String, Long> wordMapNoAdditionalCounts,
            String words,
            long count) {
        Long curCount = wordMapNoAdditionalCounts.get(words);
        if (curCount != null) {
            wordMapNoAdditionalCounts.put(words, curCount + count);
        } else {
            wordMapNoAdditionalCounts.put(words, count);
        }
    }

    private void addCountWithAdditional(
            SortedMap<String, Long[]> wordMap,
            String words,
            long count) {
        Long[] countTypeArray = wordMap.get(words);
        if (countTypeArray != null) {
            countTypeArray[0] = countTypeArray[0] + count;
            if (count == 1) {
                countTypeArray[1] = countTypeArray[1] + count;
            }
            if (count == 2) {
                countTypeArray[2] = countTypeArray[2] + count;
            }
            if (count >= 3) {
                countTypeArray[3] = countTypeArray[3] + count;
            }
        } else {
            countTypeArray = new Long[4];
            countTypeArray[0] = count;
            if (count == 1) {
                countTypeArray[1] = count;
            } else {
                countTypeArray[1] = 0L;
            }
            if (count == 2) {
                countTypeArray[2] = count;
            } else {
                countTypeArray[2] = 0L;
            }
            if (count >= 3) {
                countTypeArray[3] = count;
            } else {
                countTypeArray[3] = 0L;
            }
            wordMap.put(words, countTypeArray);
        }
    }

}
