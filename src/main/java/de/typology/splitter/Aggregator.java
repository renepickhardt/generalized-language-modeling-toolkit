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

    private File inputFile;

    private File outputFile;

    private String delimiter;

    private int startSortAtColumn;

    private boolean additionalCounts;

    // this comparator is based on the value of startSortAtColumn
    private Comparator<String> stringComparator = new Comparator<String>() {

        @Override
        public int compare(String string1, String string2) {
            if (startSortAtColumn == 0) {
                return string1.compareTo(string2);
            } else {
                String[] string1Split = string1.split("\\s");
                String[] string2Split = string2.split("\\s");
                String newString1 = "";
                String newString2 = "";
                for (int i = startSortAtColumn; i < string1Split.length; i++) {
                    newString1 += string1Split[i] + " ";
                    newString2 += string2Split[i] + " ";
                }
                newString1 = newString1.replaceFirst(" $", "");
                newString2 = newString2.replaceFirst(" $", "");
                int result = newString1.compareTo(newString2);
                if (result != 0) {
                    // not equal
                    return result;
                } else {
                    int i = 0;
                    while (i < startSortAtColumn) {
                        String newNewString1 = newString1;
                        String newNewString2 = newString2;
                        for (int j = i; j >= 0; j--) {
                            newNewString1 =
                                    string1Split[j] + " " + newNewString1;
                            newNewString2 =
                                    string2Split[j] + " " + newNewString2;
                        }
                        result = newNewString1.compareTo(newNewString2);
                        if (result != 0) {
                            // not equal
                            return result;
                        }
                        // equal
                        i++;
                    }
                    // final result: equal
                    return 0;
                }
            }
        }
    };

    /**
     * @param inputStream
     * @param outputStream
     * @param delimiter
     * @param startSortAtColumn
     *            : First column is zero
     */
    public Aggregator(
            File inputFile,
            File outputFile,
            String delimiter,
            int startSortAtColumn,
            boolean additionalCounts) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.delimiter = delimiter;
        this.startSortAtColumn = startSortAtColumn;
        this.additionalCounts = additionalCounts;

    }

    public void aggregateCounts() {
        try {
            BufferedReader inputFileReader =
                    new BufferedReader(new FileReader(inputFile));

            SortedMap<String, Long[]> wordMapAdditionalCounts =
                    new TreeMap<String, Long[]>(stringComparator);
            SortedMap<String, Long> wordMapNoAdditionalCounts =
                    new TreeMap<String, Long>(stringComparator);
            String inputLine;

            while ((inputLine = inputFileReader.readLine()) != null) {
                String[] inputLineSplit = inputLine.split(delimiter);
                String words = inputLineSplit[0];
                long count = Long.parseLong(inputLineSplit[1]);
                if (words.length() == 0) {
                    // TODO: understand the following comment
                    // logger.error("empty row in " + this.inputFile + ": \""
                    // + inputLine + "\"");
                    // logger.error("exiting JVM");
                    // System.exit(1);
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

            inputFileReader.close();
            BufferedWriter outputFileWriter =
                    new BufferedWriter(new FileWriter(outputFile));
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
            outputFileWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void addCountWithNoAdditional(
            SortedMap<String, Long> wordMapNoAdditionalCounts,
            String words,
            long count) {
        if (wordMapNoAdditionalCounts.containsKey(words)) {
            wordMapNoAdditionalCounts.put(words,
                    wordMapNoAdditionalCounts.get(words) + count);
        } else {
            wordMapNoAdditionalCounts.put(words, count);
        }
    }

    private void addCountWithAdditional(
            SortedMap<String, Long[]> wordMap,
            String words,
            long count) {
        if (wordMap.containsKey(words)) {
            Long[] countTypeArray = wordMap.get(words);
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
            Long[] countTypeArray = new Long[4];
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

    public void aggregateWithoutCounts() {
        try {
            BufferedReader inputFileReader =
                    new BufferedReader(new FileReader(inputFile));

            SortedSet<String> wordSet = new TreeSet<String>(stringComparator);
            String inputLine;

            while ((inputLine = inputFileReader.readLine()) != null) {
                wordSet.add(inputLine);
            }
            inputFileReader.close();
            BufferedWriter outputFileWriter =
                    new BufferedWriter(new FileWriter(outputFile));
            for (String line : wordSet) {
                outputFileWriter.write(line + "\n");
            }
            outputFileWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
