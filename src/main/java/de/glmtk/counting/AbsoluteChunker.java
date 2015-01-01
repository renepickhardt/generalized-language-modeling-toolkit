package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.Constants.B;
import static de.glmtk.common.Output.OUTPUT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.javamex.classmexer.MemoryUtil;
import com.javamex.classmexer.MemoryUtil.VisibilityFilter;

import de.glmtk.Constants;
import de.glmtk.Status;
import de.glmtk.Status.TrainingStatus;
import de.glmtk.common.Output;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StatisticalNumberHelper;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

public enum AbsoluteChunker {

    ABSOLUTE_CHUNKER;

    private static class PatternAndSequence {

        private Pattern pattern = null;

        private String sequence = null;

        public PatternAndSequence(
                Pattern pattern,
                String sequence) {
            this.pattern = pattern;
            this.sequence = sequence;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public String getSequence() {
            return sequence;
        }

    }

    private static class Chunk {

        private int size;

        private int counter;

        private Map<String, Long> sequenceCounts;

        public Chunk(
                int counter) {
            size = 0;
            this.counter = counter;
            sequenceCounts = new HashMap<String, Long>();
        }

        public int getSize() {
            return size;
        }

        public void increaseSize(int increase) {
            size += increase;
        }

        public int getCounter() {
            return counter;
        }

        public Map<String, Long> getSequenceCounts() {
            return sequenceCounts;
        }

        public Long getSequenceCount(String sequence) {
            return sequenceCounts.get(sequence);
        }

        public void putSequenceCount(String sequence, long count) {
            sequenceCounts.put(sequence, count);
        }

    }

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(AbsoluteChunker.class);

    private static final long CHUNK_MAX_SIZE = Constants.CHUNK_MAX_SIZE;

    private static final long AVERAGE_QUEUE_ITEM_SIZE = 550 * B;

    private static final int AVAILABLE_MEMORY_PERCENT = 50;

    private static final int CHUNK_SIZE_MEMORY_PERCENT = 70;

    private static final int READER_MEMORY_PERCENT = 50;

    private static final int QUEUE_MEMORY_PERCENT = 50;

    private static final int TAB_COUNT_NL_BYTES = "\t10\n"
            .getBytes(Constants.CHARSET).length;

    private class SequencingThread implements Runnable {

        private Map<Integer, Set<Pattern>> patternsBySize;

        @Override
        public void run() {
            patternsBySize = Patterns.groupPatternsBySize(patterns);

            try (BufferedReader reader =
                    NioUtils.newBufferedReader(trainingFile, Constants.CHARSET,
                            (int) readerMemory)) {
                Progress progress = new Progress(Files.size(trainingFile));

                String line;
                while ((line = reader.readLine()) != null) {
                    progress.increase(line.getBytes(Constants.CHARSET).length);
                    generateAndQueueSequences(line);
                }
            } catch (InterruptedException | IOException e) {
                // Rethrow as unchecked exception, because it is not allowed
                // to throw checked exceptions from threads.
                throw new RuntimeException(e);
            }

            sequencingDone = true;
            LOGGER.debug("SequencingThread finished.");
        }

        private void generateAndQueueSequences(String line)
                throws InterruptedException {
            String[] split =
                    StringUtils.splitAtChar(line, ' ').toArray(new String[0]);
            String[] words = new String[split.length];
            String[] poses = new String[split.length];
            StringUtils.extractWordsAndPoses(split, trainingFileHasPos, words,
                    poses);

            for (Entry<Integer, Set<Pattern>> entry : patternsBySize.entrySet()) {
                int size = entry.getKey();
                Set<Pattern> patterns = entry.getValue();
                for (int p = 0; p <= split.length - size; ++p) {
                    for (Pattern pattern : patterns) {
                        String sequence = pattern.apply(words, poses, p);

                        PatternAndSequence item =
                                new PatternAndSequence(pattern, sequence);
                        while (!aggregatingQueues.get(pattern).offer(item,
                                Constants.QUEUE_IDLE_TIME,
                                TimeUnit.MILLISECONDS)) {
                            LOGGER.trace("SequencingThread Idle, because queue full.");
                            StatisticalNumberHelper
                                    .count("AbsoluteChunker#SequencingThread idle (queue full).");
                        }

                        if (Constants.DEBUG_AVERAGE_MEMORY) {
                            StatisticalNumberHelper
                                    .average(
                                            "AbsoluteChunker.PatternAndSequence Memory",
                                            MemoryUtil.deepMemoryUsageOf(item,
                                                    VisibilityFilter.ALL));
                        }
                    }
                }
            }
        }
    }

    private class AggregatingThread implements Runnable {

        private BlockingQueue<PatternAndSequence> queue;

        private Map<Pattern, Chunk> chunks;

        private Map<Pattern, List<Path>> chunkFiles;

        public AggregatingThread(
                BlockingQueue<PatternAndSequence> queue) {
            this.queue = queue;
            chunks = new HashMap<Pattern, Chunk>();
            chunkFiles = new HashMap<Pattern, List<Path>>();
        }

        @Override
        public void run() {
            try {
                while (!(sequencingDone && queue.isEmpty())) {
                    PatternAndSequence patternAndSequence =
                            queue.poll(Constants.QUEUE_IDLE_TIME,
                                    TimeUnit.MILLISECONDS);
                    if (patternAndSequence == null) {
                        LOGGER.trace("AggregatingThreaed idle, because queue empty.");
                        StatisticalNumberHelper
                                .count("AbsoluteChunker#AggregatingThread Idle, because queue empty.");
                        continue;
                    }

                    addToChunk(patternAndSequence.getPattern(),
                            patternAndSequence.getSequence());
                }

                for (Entry<Pattern, Chunk> entry : chunks.entrySet()) {
                    Pattern pattern = entry.getKey();
                    Chunk chunk = entry.getValue();
                    writeChunkToFile(pattern, chunk);
                    LOGGER.debug("Finished pattern '%s'.", pattern);
                }

                status.addChunked(false, chunkFiles);
            } catch (InterruptedException | IOException e) {
                // Rethrow as unchecked exception, because it is not allowed
                // to throw checked exceptions from threads.
                throw new RuntimeException(e);
            }

            LOGGER.debug("AggregatingThread finished.");
        }

        private void addToChunk(Pattern pattern, String sequence)
                throws IOException {
            Chunk chunk = chunks.get(pattern);
            if (chunk == null) {
                chunk = new Chunk(0);
                chunks.put(pattern, chunk);
                chunkFiles.put(pattern, new LinkedList<Path>());
            }

            Long count = chunk.getSequenceCount(sequence);
            if (count == null) {
                chunk.increaseSize(sequence.getBytes(Constants.CHARSET).length
                        + TAB_COUNT_NL_BYTES);
                chunk.putSequenceCount(sequence, 1L);
            } else {
                chunk.putSequenceCount(sequence, count + 1l);
            }

            if (chunk.getSize() > chunkSize) {
                writeChunkToFile(pattern, chunk);
                chunks.put(pattern, new Chunk(chunk.getCounter() + 1));
            }
        }

        private void writeChunkToFile(Pattern pattern, Chunk chunk)
                throws IOException {
            Path chunkFile =
                    absoluteChunkedDir.resolve(pattern.toString()).resolve(
                            "chunk" + chunk.getCounter());
            Files.deleteIfExists(chunkFile);

            try (BufferedWriter writer =
                    Files.newBufferedWriter(chunkFile, Constants.CHARSET)) {
                Map<String, Long> sortedSequenceCounts =
                        new TreeMap<String, Long>(chunk.getSequenceCounts());
                for (Entry<String, Long> entry : sortedSequenceCounts
                        .entrySet()) {
                    writer.write(entry.getKey());
                    writer.write('\t');
                    writer.write(entry.getValue().toString());
                    writer.write('\n');
                }
            }

            chunkFiles.get(pattern).add(chunkFile.getFileName());
            LOGGER.debug("Wrote chunk for pattern '%s': '%s'.", pattern,
                    chunkFile);
        }

    }

    private boolean sequencingDone;

    private Status status;

    private long chunkSize;

    private long readerMemory;

    private long queueMemory;

    private Path trainingFile;

    private boolean trainingFileHasPos;

    private Path absoluteChunkedDir;

    private Set<Pattern> patterns;

    private Map<Pattern, BlockingQueue<PatternAndSequence>> aggregatingQueues;

    public void chunk(
            Status status,
            Set<Pattern> patterns,
            Path trainingFile,
            Path absoluteChunkedDir) throws IOException, InterruptedException {
        OUTPUT.setPhase(Phase.ABSOLUTE_CHUNKING, true);
        if (patterns.isEmpty()) {
            LOGGER.debug("No patterns to chunk, returning.");
            OUTPUT.setPercent(1.0);
            return;
        }

        LOGGER.debug("patterns = %s", patterns);
        Files.createDirectories(absoluteChunkedDir);
        for (Pattern pattern : patterns) {
            Files.createDirectories(absoluteChunkedDir.resolve(pattern
                    .toString()));
        }

        calculateMemory(patterns.size());
        this.status = status;
        this.patterns = patterns;
        this.trainingFile = trainingFile;
        trainingFileHasPos =
                status.getTraining() == TrainingStatus.DONE_WITH_POS;
        this.absoluteChunkedDir = absoluteChunkedDir;
        aggregatingQueues =
                new HashMap<Pattern, BlockingQueue<PatternAndSequence>>();

        List<Runnable> threads = prepareThreds();

        LOGGER.debug("Launching Threads...");
        sequencingDone = false;
        ThreadUtils.executeThreads(CONFIG.getNumberOfCores(), threads);
    }

    private void calculateMemory(int numPatterns) {
        LOGGER.debug("Calculating Memory...");
        Runtime r = Runtime.getRuntime();
        r.gc();

        long totalFreeMemory = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long availableMemory =
                (AVAILABLE_MEMORY_PERCENT * totalFreeMemory) / 100;

        long chunkMemory =
                Math.min((CHUNK_SIZE_MEMORY_PERCENT * availableMemory) / 100,
                        CHUNK_MAX_SIZE * numPatterns);
        chunkSize = chunkMemory / numPatterns;

        long remainingMemory = availableMemory - chunkMemory;
        readerMemory = (READER_MEMORY_PERCENT * remainingMemory) / 100;
        queueMemory = (QUEUE_MEMORY_PERCENT * remainingMemory) / 100;

        LOGGER.debug("totalFreeMemory = %s",
                Output.humanReadableByteCount(totalFreeMemory, false));
        LOGGER.debug("availableMemory = %s",
                Output.humanReadableByteCount(availableMemory, false));
        LOGGER.debug("readerMemory    = %s",
                Output.humanReadableByteCount(readerMemory, false));
        LOGGER.debug("qeueMemory      = %s",
                Output.humanReadableByteCount(queueMemory, false));
        LOGGER.debug("chunkSize       = %s",
                Output.humanReadableByteCount(chunkSize, false));
    }

    private List<Runnable> prepareThreds() {
        LOGGER.debug("Praparing Threads...");
        int numSequencingThreads = 1;
        int numAggregatingThreads =
                Math.max(1, CONFIG.getNumberOfCores() - numSequencingThreads);
        LOGGER.debug("numSequencingThreads  = %d", numSequencingThreads);
        LOGGER.debug("numAggregatingThreads = %d", numAggregatingThreads);

        List<Runnable> threads = new LinkedList<Runnable>();
        threads.add(new SequencingThread());

        List<Pattern> reamainingPatterns = new LinkedList<Pattern>(patterns);
        for (int i = 0; i != numAggregatingThreads; ++i) {
            BlockingQueue<PatternAndSequence> aggregatingQueue =
                    new ArrayBlockingQueue<PatternAndSequence>(
                            (int) (queueMemory / AVERAGE_QUEUE_ITEM_SIZE));
            if (i != numAggregatingThreads - 1) {
                // If not last thread: have thread receive
                // (patterns.size() / numAggregatings Threads) patterns.
                for (int j = 0; j != patterns.size() / numAggregatingThreads; ++j) {
                    aggregatingQueues.put(reamainingPatterns.remove(0),
                            aggregatingQueue);
                }
            } else {
                // If last thread: have thread receive all remaining patterns.
                for (Pattern pattern : reamainingPatterns) {
                    aggregatingQueues.put(pattern, aggregatingQueue);
                }
            }

            threads.add(new AggregatingThread(aggregatingQueue));
        }

        return threads;
    }

}
