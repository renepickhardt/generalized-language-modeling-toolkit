package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A class for aggregating sequences by counting their occurrences. Expects an
 * inputStream with a size that is 30% of the allocated main memory.
 * 
 * @author Martin Koerner
 * 
 */
public class Aggregator {

    private File trainingFile;

    private File outputFile;

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
            File trainingFile,
            File outputFile,
            String delimiter,
            boolean additionalCounts) {
        this.trainingFile = trainingFile;
        this.outputFile = outputFile;
        this.delimiter = delimiter;
        this.additionalCounts = additionalCounts;
    }

    public void aggregateCounts() throws IOException {
        SortedMap<String, Long[]> wordMapAdditionalCounts =
                new TreeMap<String, Long[]>(stringComparator);
        SortedMap<String, Long> wordMapNoAdditionalCounts =
                new TreeMap<String, Long>(stringComparator);

        try (BufferedReader trainingFileReader =
                new BufferedReader(new FileReader(trainingFile))) {
            String inputLine;
            while ((inputLine = trainingFileReader.readLine()) != null) {
                String[] inputLineSplit = inputLine.split(delimiter);
                String words = inputLineSplit[0];
                long count = Long.parseLong(inputLineSplit[1]);
                if (words.length() == 0) {
                    continue;
                }

                if (additionalCounts) {
                    addCountWithAdditional(wordMapAdditionalCounts, words,
                            count);
                } else {
                    addCountWithNoAdditional(wordMapNoAdditionalCounts, words,
                            count);
                }
            }
        }

        try (BufferedWriter outputFileWriter =
                new BufferedWriter(new FileWriter(outputFile))) {
            if (additionalCounts) {
                for (Entry<String, Long[]> entry : wordMapAdditionalCounts
                        .entrySet()) {
                    String words = entry.getKey();
                    // [0]=1+
                    // [1]=1
                    // [2]=2
                    // [3]=3+
                    outputFileWriter.write(words + delimiter
                            + entry.getValue()[0] + delimiter
                            + entry.getValue()[1] + delimiter
                            + entry.getValue()[2] + delimiter
                            + entry.getValue()[3] + "\n");
                }
            } else {
                for (Entry<String, Long> entry : wordMapNoAdditionalCounts
                        .entrySet()) {
                    String words = entry.getKey();
                    outputFileWriter.write(words + delimiter + entry.getValue()
                            + "\n");
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

    public void aggregateWithoutCounts() throws IOException {
        SortedSet<String> wordSet = new TreeSet<String>(stringComparator);

        try (BufferedReader trainingFileReader =
                new BufferedReader(new FileReader(trainingFile))) {
            String inputLine;
            while ((inputLine = trainingFileReader.readLine()) != null) {
                wordSet.add(inputLine);
            }
        }

        try (BufferedWriter outputFileWriter =
                new BufferedWriter(new FileWriter(outputFile))) {
            for (String line : wordSet) {
                outputFileWriter.write(line + "\n");
            }
        }
    }
}
