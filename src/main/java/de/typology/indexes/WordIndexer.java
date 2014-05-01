package de.typology.indexes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * A class for building a text file containing a index representation for a
 * given text file based on the alphabetical distribution of its words.
 * 
 * @author Martin Koerner
 * 
 */
public class WordIndexer {

    private TreeMap<String, Long> buildMap(
            File trainingFile,
            String beforeLine,
            String afterLine) throws IOException {
        try (BufferedReader reader =
                new BufferedReader(new FileReader(trainingFile))) {
            TreeMap<String, Long> wordMap = new TreeMap<String, Long>();
            String line;
            // long lineCount=0L;
            while ((line = reader.readLine()) != null) {
                line = beforeLine + line + afterLine;
                String[] words = line.split("\\s+");
                for (String word : words) {
                    Long curCount = wordMap.get(word);
                    if (curCount != null) {
                        wordMap.put(word, curCount + 1L);
                    } else {
                        wordMap.put(word, 1L);
                    }
                }
            }

            return wordMap;
        }
    }

    /**
     * @return maxCountPerFile
     */
    public long buildIndex(
            File trainingFile,
            File indexOutputFile,
            int maxCountDivider,
            String beforeLine,
            String afterLine) throws IOException {

        // build WordMap
        TreeMap<String, Long> wordMap =
                buildMap(trainingFile, beforeLine, afterLine);

        // summarize all word counts
        Long totalCount = 0L;
        for (Entry<String, Long> word : wordMap.entrySet()) {
            totalCount += word.getValue();
        }

        // calculate max count per file
        Long maxCountPerFile = totalCount / maxCountDivider;
        if (maxCountPerFile < 1L) {
            maxCountPerFile = 1L;
        }

        // build index
        try (BufferedWriter indexWriter =
                new BufferedWriter(new FileWriter(indexOutputFile))) {
            Long currentFileCount = 0L;
            int fileCount = 0;
            Iterator<Map.Entry<String, Long>> wordMapIterator =
                    wordMap.entrySet().iterator();
            Entry<String, Long> word;

            try {
                while (wordMapIterator.hasNext()) {
                    // get next word
                    word = wordMapIterator.next();
                    if (fileCount == 0
                            || currentFileCount + word.getValue() > maxCountPerFile) {
                        indexWriter.write(word.getKey() + "\t" + fileCount
                                + "\n");
                        currentFileCount = word.getValue();
                        fileCount++;
                    } else {
                        currentFileCount += word.getValue();
                    }
                }
            } catch (IOException e) {
                // make sure that no corrupted index file is stored
                if (indexOutputFile.exists()) {
                    indexOutputFile.delete();
                }
                throw e;
            }
        }
        return maxCountPerFile;
    }

}
