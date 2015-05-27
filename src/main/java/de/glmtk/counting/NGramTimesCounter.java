/*
 * Generalized Language Modeling Toolkit (GLMTK)
 * 
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 * 
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 * 
 * See the AUTHORS file for contributors.
 */

package de.glmtk.counting;

import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import de.glmtk.Constants;
import de.glmtk.Glmtk;
import de.glmtk.common.Config;
import de.glmtk.common.Pattern;
import de.glmtk.common.Status;
import de.glmtk.counts.NGramTimes;
import de.glmtk.files.CountsReader;
import de.glmtk.files.NGramTimesWriter;
import de.glmtk.logging.Logger;
import de.glmtk.output.ProgressBar;
import de.glmtk.util.ThreadUtils;

public class NGramTimesCounter {
    private static final Logger LOGGER = Logger.get(NGramTimesCounter.class);

    private class Thread implements Callable<Object> {
        private Pattern pattern;
        private NGramTimes ngramTimes;

        @Override
        public Object call() throws InterruptedException, IOException {
            while (!patternQueue.isEmpty()) {
                pattern = patternQueue.poll(Constants.MAX_IDLE_TIME,
                        TimeUnit.MILLISECONDS);
                if (pattern == null)
                    continue;

                LOGGER.debug("Counting pattern '%s'.", pattern);

                countNGramTimes();
                ngramTimesForPattern.put(pattern, ngramTimes);

                LOGGER.debug("Finished pattern '%s'.", pattern);

                synchronized (progressBar) {
                    progressBar.increase();
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        private void countNGramTimes() throws IOException {
            ngramTimes = new NGramTimes();

            Path inputDir = pattern.isAbsolute()
                    ? absoluteDir
                    : continuationDir;
            Path inputFile = inputDir.resolve(pattern.toString());
            int memory = (int) Math.min(Files.size(inputFile), readerMemory);
            try (CountsReader reader = new CountsReader(inputFile,
                    Constants.CHARSET, memory)) {
                while (reader.readLine() != null)
                    ngramTimes.add(reader.getCount());
            }
        }
    }

    private Config config;

    private ProgressBar progressBar;
    private Path outputFile;
    private Path absoluteDir;
    private Path continuationDir;
    private BlockingQueue<Pattern> patternQueue;
    private ConcurrentHashMap<Pattern, NGramTimes> ngramTimesForPattern;
    private int readerMemory;

    public NGramTimesCounter(Config config) {
        this.config = config;
    }

    public void count(Status status,
                      Path outputFile,
                      Path absoluteDir,
                      Path continuationDir,
                      ProgressBar progressBar) throws Exception {
        if (status.isNGramTimesCounted()) {
            LOGGER.debug("Status reports ngram times already counted, returning.");
            return;
        }

        Set<Pattern> patterns = status.getCounted();

        this.outputFile = outputFile;
        this.absoluteDir = absoluteDir;
        this.continuationDir = continuationDir;
        patternQueue = new LinkedBlockingQueue<>(patterns);
        ngramTimesForPattern = new ConcurrentHashMap<>();
        calculateMemory();

        List<Callable<Object>> threads = new LinkedList<>();
        for (int i = 0; i != config.getNumberOfThreads(); ++i)
            threads.add(new Thread());

        this.progressBar = progressBar;
        this.progressBar.total(patternQueue.size());
        ThreadUtils.executeThreads(config.getNumberOfThreads(), threads);

        Glmtk.validateExpectedResults("ngram times couting", patterns,
                ngramTimesForPattern.keySet());

        writeToFile();
        status.setNGramTimesCounted();
    }

    private void calculateMemory() {
        readerMemory = config.getMemoryReader();
        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
    }

    private void writeToFile() throws IOException {
        SortedMap<Pattern, NGramTimes> sortedNGramTimesForPattern = new TreeMap<>(
                ngramTimesForPattern);
        try (NGramTimesWriter writer = new NGramTimesWriter(outputFile,
                Constants.CHARSET)) {
            for (Entry<Pattern, NGramTimes> entry : sortedNGramTimesForPattern.entrySet())
                writer.append(entry.getKey(), entry.getValue());
        }
    }
}
