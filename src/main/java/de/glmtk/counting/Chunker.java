package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.javamex.classmexer.MemoryUtil;
import com.javamex.classmexer.MemoryUtil.VisibilityFilter;

import de.glmtk.Constants;
import de.glmtk.Status;
import de.glmtk.Status.Training;
import de.glmtk.common.Counter;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StatisticalNumberHelper;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

public enum Chunker {

    CHUNKER;

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(Chunker.class);

    private static final int TAB_COUNTER_NL_BYTES = ("\t"
            + new Counter(10, 10, 10, 10).toString() + "\n")
            .getBytes(Constants.CHARSET).length;

    private abstract class Thread implements Callable<Object> {

        protected Pattern pattern;

        private Path patternDir;

        private List<Path> chunkFiles;

        private long chunkSize;

        private Map<String, Counter> chunkCounts;

        @Override
        public Object call() throws Exception {
            while (!patternQueue.isEmpty()) {
                pattern =
                        patternQueue.poll(Constants.QUEUE_TIMEOUT,
                                TimeUnit.MILLISECONDS);
                if (pattern == null) {
                    continue;
                }

                List<Path> inputFiles = getInputFiles();
                if (inputFiles == null) {
                    LOGGER.trace("Pattern '%s' not vailable.", pattern);
                    StatisticalNumberHelper.count("Pattern not available");
                    // wait until other threads finishes pattern
                    java.lang.Thread.sleep(10);
                    patternQueue.put(pattern);
                    continue;
                }

                LOGGER.debug("Chunking pattern '%s'.", pattern);

                if (!continuation) {
                    patternDir = absoluteChunkedDir.resolve(pattern.toString());
                } else {
                    patternDir =
                            continuationChunkedDir.resolve(pattern.toString());
                }
                chunkFiles = new LinkedList<Path>();
                chunkSize = 0L;
                chunkCounts = new HashMap<String, Counter>();

                Files.createDirectories(patternDir);

                for (Path inputFile : inputFiles) {
                    sequenceInput(inputFile);
                }

                writeChunkToFile(); // Write remaining partial chunk.
                chunkCounts = null; // Free memory of map.

                status.setChunksForPattern(continuation, pattern, chunkFiles);

                LOGGER.debug("Finished pattern '%s'.", pattern);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        protected abstract List<Path> getInputFiles() throws Exception;

        protected abstract void sequenceInput(Path inputFile) throws Exception;

        protected void countSequence(String sequence, long count)
                throws IOException {
            Counter counter = chunkCounts.get(sequence);
            if (counter == null) {
                counter = new Counter();
                chunkCounts.put(sequence, counter);
                chunkSize +=
                        sequence.getBytes(Constants.CHARSET).length
                        + TAB_COUNTER_NL_BYTES;
            }
            counter.add(count);

            if (chunkSize > Constants.MAX_CHUNK_SIZE) {
                if (Constants.DEBUG_AVERAGE_MEMORY) {
                    StatisticalNumberHelper.average("Chunk Map Memory",
                            MemoryUtil.deepMemoryUsageOf(chunkCounts,
                                    VisibilityFilter.ALL));
                }

                writeChunkToFile();
                chunkSize = 0L;
                chunkCounts = new HashMap<String, Counter>();
            }
        }

        private void writeChunkToFile() throws IOException {
            Path chunkFile = patternDir.resolve("chunk" + chunkFiles.size());
            Files.deleteIfExists(chunkFile);

            try (BufferedWriter writer =
                    NioUtils.newBufferedWriter(chunkFile, Constants.CHARSET,
                            writerMemory)) {
                Map<String, Counter> sortedCounts =
                        new TreeMap<String, Counter>(chunkCounts);
                for (Entry<String, Counter> entry : sortedCounts.entrySet()) {
                    writer.write(entry.getKey());
                    writer.write('\t');
                    if (!continuation) {
                        writer.write(Long.toString(entry.getValue()
                                .getOnePlusCount()));
                    } else {
                        writer.write(entry.getValue().toString());
                    }
                    writer.write('\n');
                }
            }

            chunkFiles.add(chunkFile.getFileName());

            LOGGER.debug("Wrote chunk for pattern '%s': '%s'.", pattern,
                    chunkFile);
        }

    }

    private class AbsoluteThread extends Thread {

        @Override
        protected List<Path> getInputFiles() throws Exception {
            return Arrays.asList(trainingFile);
        }

        @Override
        protected void sequenceInput(Path inputFile) throws Exception {
            int patternSize = pattern.size();
            try (BufferedReader reader =
                    NioUtils.newBufferedReader(inputFile, Constants.CHARSET,
                            readerMemory)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] split =
                            StringUtils.splitAtChar(line, ' ').toArray(
                                    new String[0]);
                    String[] words = new String[split.length];
                    String[] poses = new String[split.length];
                    StringUtils.extractWordsAndPoses(split, trainingFileHasPos,
                            words, poses);

                    for (int p = 0; p <= split.length - patternSize; ++p) {
                        String sequence = pattern.apply(words, poses, p);
                        countSequence(sequence, 1L);
                    }
                }
            }
        }

    }

    private class ContinuationThread extends Thread {

        @Override
        protected List<Path> getInputFiles() throws IOException {
            Pattern inputPattern = pattern.getContinuationSource();

            Path inputDir;
            boolean fromChunked;

            boolean isAbsolute = inputPattern.isAbsolute();
            if (isAbsolute && status.getCounted(false).contains(inputPattern)) {
                inputDir = absoluteCountedDir;
                fromChunked = false;
            } else if (isAbsolute
                    && status.getChunkedPatterns(false).contains(inputPattern)) {
                inputDir = absoluteChunkedDir;
                fromChunked = true;
            } else if (!isAbsolute
                    && status.getCounted(true).contains(inputPattern)) {
                inputDir = continuationCountedDir;
                fromChunked = false;
            } else if (!isAbsolute
                    && status.getChunkedPatterns(true).contains(inputPattern)) {
                inputDir = continuationChunkedDir;
                fromChunked = true;
            } else {
                return null;
            }

            inputDir = inputDir.resolve(inputPattern.toString());

            if (!fromChunked) {
                return Arrays.asList(inputDir);
            } else {
                List<Path> result = new ArrayList<Path>();
                try (DirectoryStream<Path> inputDirStream =
                        Files.newDirectoryStream(inputDir)) {
                    for (Path inputFile : inputDirStream) {
                        result.add(inputFile);
                    }
                }
                return result;
            }
        }

        @Override
        protected void sequenceInput(Path inputFile) throws Exception {
            LOGGER.debug("Sequencing '%s' from '%s'.", pattern, inputFile);
            try (BufferedReader reader =
                    NioUtils.newBufferedReader(inputFile, Constants.CHARSET,
                            readerMemory)) {
                String line;
                int lineNo = -1;
                while ((line = reader.readLine()) != null) {
                    ++lineNo;

                    List<String> split = StringUtils.splitAtChar(line, '\t');
                    String seq = split.get(0);
                    long count;
                    try {
                        if (split.size() == 2) {
                            // from Absolute
                            count = 1;
                        } else if (split.size() == 5) {
                            // from Continuation
                            count = Long.parseLong(split.get(1));
                        } else {
                            throw new IllegalStateException();
                        }
                    } catch (IllegalStateException | NumberFormatException e) {
                        throw new IllegalStateException(
                                String.format(
                                        "Illegal input line '%d' in file '%s'. Need to be of format '<sequence> <count> (<count> <count> <count)'. Where <sequence> is a sequence of strings, and <count> is an integer.\nLine was: '%s'.",
                                        lineNo, inputFile, line));
                    }

                    String sequence =
                            pattern.apply(StringUtils.splitAtChar(seq, ' ')
                                    .toArray(new String[0]));
                    countSequence(sequence, count);
                }
            }
        }

    }

    private Progress progress;

    private boolean continuation;

    private Status status;

    private Path trainingFile;

    private boolean trainingFileHasPos;

    private Path absoluteCountedDir;

    private Path absoluteChunkedDir;

    private Path continuationCountedDir;

    private Path continuationChunkedDir;

    private BlockingQueue<Pattern> patternQueue;

    private int readerMemory;

    private int writerMemory;

    private long maxChunkSize;

    public void chunkAbsolute(
            Set<Pattern> patterns,
            Status status,
            Path trainingFile,
            Path absoluteChunkedDir) throws Exception {
        OUTPUT.setPhase(Phase.ABSOLUTE_CHUNKING, true);
        this.status = status;
        this.trainingFile = trainingFile;
        trainingFileHasPos = status.getTraining() == Training.TAGGED;
        this.absoluteChunkedDir = absoluteChunkedDir;
        chunk(false, patterns);
    }

    public void chunkContinuation(
            Set<Pattern> patterns,
            Status status,
            Path absoluteCountedDir,
            Path absoluteChunkedDir,
            Path continuationCountedDir,
            Path continuationChunkedDir) throws Exception {
        OUTPUT.setPhase(Phase.CONTINUATION_CHUNKING, true);
        this.status = status;
        this.absoluteCountedDir = absoluteCountedDir;
        this.absoluteChunkedDir = absoluteChunkedDir;
        this.continuationCountedDir = continuationCountedDir;
        this.continuationChunkedDir = continuationChunkedDir;
        chunk(true, patterns);
    }

    private void chunk(boolean continuation, Set<Pattern> patterns)
            throws Exception {
        LOGGER.debug("patterns = '%s'", patterns);
        progress = new Progress(patterns.size());

        if (patterns.isEmpty()) {
            LOGGER.debug("No patterns to chunk, returning.");
            progress.set(1.0);
            return;
        }

        this.continuation = continuation;
        patternQueue =
                new PriorityBlockingQueue<Pattern>(patterns.size(),
                        new Comparator<Pattern>() {

                    @Override
                    public int compare(Pattern lhs, Pattern rhs) {
                        return Integer.compare(
                                lhs.numElems(PatternElem.CSKIP_ELEMS),
                                rhs.numElems(PatternElem.CSKIP_ELEMS));
                    }

                });
        patternQueue.addAll(patterns);
        calculateMemory();

        List<Callable<Object>> threads = new LinkedList<Callable<Object>>();
        for (int i = 0; i != CONFIG.getNumberOfCores(); ++i) {
            threads.add(!continuation
                    ? new AbsoluteThread()
                    : new ContinuationThread());
        }
        ThreadUtils.executeThreads(CONFIG.getNumberOfCores(), threads);
    }

    private void calculateMemory() {
        double AVAILABLE_MEM_RATIO = 0.5;
        double CHUNK_LOAD_FACTOR = 5.5;

        Runtime r = Runtime.getRuntime();
        r.gc();

        long totalFreeMem = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long availableMem = (long) (AVAILABLE_MEM_RATIO * totalFreeMem);
        long memPerThread = availableMem / CONFIG.getNumberOfCores();

        readerMemory = Constants.BUFFER_SIZE;
        writerMemory = Constants.BUFFER_SIZE;
        long chunkMemory =
                (long) Math.min(CHUNK_LOAD_FACTOR * Constants.MAX_CHUNK_SIZE,
                        memPerThread - readerMemory - writerMemory);
        maxChunkSize = (long) (chunkMemory / CHUNK_LOAD_FACTOR);

        LOGGER.debug("totalFreeMem = %s", humanReadableByteCount(totalFreeMem));
        LOGGER.debug("availableMem = %s", humanReadableByteCount(availableMem));
        LOGGER.debug("memPerThread = %s", humanReadableByteCount(memPerThread));
        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
        LOGGER.debug("maxChunkSize = %s", humanReadableByteCount(maxChunkSize));
    }

}
