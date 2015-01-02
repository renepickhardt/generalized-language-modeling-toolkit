package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.Constants.B;
import static de.glmtk.common.Output.OUTPUT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
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
import de.glmtk.common.Counter;
import de.glmtk.common.Output;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.common.PatternElem;
import de.glmtk.util.StatisticalNumberHelper;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

public enum ContinuationChunker {

    CONTINUATION_CHUNKER;

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(ContinuationChunker.class);

    private static final long CHUNK_MAX_SIZE = Constants.CHUNK_MAX_SIZE;

    private static final long AVERAGE_QUEUE_ITEM_SIZE = 580 * B;

    private static final int AVAILABLE_MEMORY_PERCENT = 80;

    private static final int CHUNK_SIZE_MEMORY_PERCENT = 70;

    private static final int TAB_COUNTER_NL_BYTES = ("\t"
            + new Counter(10, 10, 10, 10).toString() + "\n")
            .getBytes(Constants.CHARSET).length;

    private static final Comparator<Pattern> SOURCE_PATTERN_COMPARATOR =
            new Comparator<Pattern>() {

        @Override
        public int compare(Pattern a, Pattern b) {
            return ((Integer) a.numElems(PatternElem.CSKIP_ELEMS))
                    .compareTo(b.numElems(PatternElem.CSKIP_ELEMS));
        }

    };

    private static class NGramWithCount {

        private Pattern pattern;

        private String sequence;

        private long count;

        public NGramWithCount(
                Pattern pattern,
                String sequence,
                long count) {
            this.pattern = pattern;
            this.sequence = sequence;
            this.count = count;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public String getSequence() {
            return sequence;
        }

        public long getCount() {
            return count;
        }

    }

    private static class Chunk {

        private int size = 0;

        private int counter = 0;

        private Map<String, Counter> sequenceCounts;

        public Chunk(
                int counter) {
            size = 0;
            this.counter = counter;
            sequenceCounts = new HashMap<String, Counter>();
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

        public Map<String, Counter> getSequenceCounts() {
            return sequenceCounts;
        }

        public Counter getSequenceCount(String sequence) {
            return sequenceCounts.get(sequence);
        }

        public void putSequenceCount(String sequence, Counter counter) {
            sequenceCounts.put(sequence, counter);
        }

    }

    private class SequencingThread implements Callable<Object> {

        @Override
        public Object call() throws InterruptedException, IOException {
            while (!patternsQueue.isEmpty()) {
                Pattern pattern =
                        patternsQueue.poll(Constants.QUEUE_IDLE_TIME,
                                TimeUnit.MILLISECONDS);
                if (pattern == null) {
                    LOGGER.trace("SequencingThread idle, because queue empty.");
                    StatisticalNumberHelper
                    .count("ContinuationChunker#SequencingThread idle, because queue empty");
                    continue;
                }

                boolean[] args = new boolean[2];
                Path patternDir = getPatternDir(pattern, args);
                if (patternDir == null) {
                    LOGGER.trace("Pattern '%s' not available.", pattern);
                    StatisticalNumberHelper.count("Pattern not available");
                    // wait for agregators to finish pattern
                    Thread.sleep(10);
                    patternsQueue.put(pattern);
                    continue;
                }
                boolean chunked = args[0];
                boolean fromAbsolute = args[1];

                if (!chunked) {
                    sequence(pattern, patternDir, true, fromAbsolute);
                } else {
                    try (DirectoryStream<Path> inputDirStream =
                            Files.newDirectoryStream(patternDir)) {
                        Iterator<Path> iter = inputDirStream.iterator();
                        while (iter.hasNext()) {
                            sequence(pattern, iter.next(), !iter.hasNext(),
                                    fromAbsolute);
                        }
                    }
                }

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            --numActiveSequencingThreads;
            LOGGER.debug("SequencingThread finished");
            return null;
        }

        private Path getPatternDir(Pattern pattern, boolean[] args) {
            Path patternDir;
            boolean chunked;
            boolean fromAbsolute;

            boolean isAbsolute = pattern.isAbsolute();
            if (isAbsolute && status.getCounted(false).contains(pattern)) {
                patternDir = absoluteCountedDir;
                chunked = false;
                fromAbsolute = true;
            } else if (isAbsolute
                    && status.getChunkedPatterns(false).contains(pattern)) {
                patternDir = absoluteChunkedDir;
                chunked = true;
                fromAbsolute = true;
            } else if (!isAbsolute && status.getCounted(true).contains(pattern)) {
                patternDir = continuationCountedDir;
                chunked = false;
                fromAbsolute = false;
            } else if (!isAbsolute
                    && status.getChunkedPatterns(true).contains(pattern)) {
                patternDir = continuationChunkedDir;
                chunked = true;
                fromAbsolute = false;
            } else {
                return null;
            }
            patternDir = patternDir.resolve(pattern.toString());

            args[0] = chunked;
            args[1] = fromAbsolute;
            return patternDir;
        }

        private void sequence(
                Pattern pattern,
                Path inputFile,
                boolean lastFile,
                boolean fromAbsolute) throws IOException, InterruptedException {
            LOGGER.debug("Sequencing '%s' from '%s'.", pattern, inputFile);
            try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    Files.newInputStream(inputFile),
                                    Constants.CHARSET), (int) readerMemory)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    List<String> split = StringUtils.splitAtChar(line, '\t');
                    if (split.size() < 2) {
                        LOGGER.error(
                                "Unable to derminine sequence and counts of line '%s'.",
                                line);
                        continue;
                    }

                    String sequence = split.get(0);
                    long count =
                            fromAbsolute ? 1 : Long.parseLong(split.get(1));
                    NGramWithCount item =
                            new NGramWithCount(pattern, sequence, count);

                    while (!aggregatingQueues.get(pattern).offer(item,
                            Constants.QUEUE_IDLE_TIME, TimeUnit.MILLISECONDS)) {
                        LOGGER.trace("Idle, because queue full.");
                        StatisticalNumberHelper
                        .count("ContinuationChunker#SequencingThread idle, beacause queue full");
                    }

                    if (Constants.DEBUG_AVERAGE_MEMORY) {
                        StatisticalNumberHelper.average(
                                "ContinuationChunker.NGramWithCount Memory",
                                MemoryUtil.deepMemoryUsageOf(item,
                                        VisibilityFilter.ALL));
                    }
                }
                if (lastFile) {
                    NGramWithCount item = new NGramWithCount(pattern, null, 0);
                    while (!aggregatingQueues.get(pattern).offer(item,
                            Constants.QUEUE_IDLE_TIME, TimeUnit.MILLISECONDS)) {
                        LOGGER.trace("Idle, because queue full.");
                        StatisticalNumberHelper
                        .count("ContinuationChunker#SequencingThread idle, beacause queue full");
                    }
                }
            }
        }

    }

    private class AggregatingThread implements Callable<Object> {

        private BlockingQueue<NGramWithCount> queue;

        private Map<Pattern, List<Pattern>> sourceToPattern;

        private Map<Pattern, Chunk> chunks;

        private Map<Pattern, List<Path>> chunkFiles;

        public AggregatingThread(
                BlockingQueue<NGramWithCount> queue,
                Map<Pattern, List<Pattern>> sourceToPattern) {
            this.queue = queue;
            this.sourceToPattern = sourceToPattern;
            chunks = new HashMap<Pattern, Chunk>();
            chunkFiles = new HashMap<Pattern, List<Path>>();
        }

        @Override
        public Object call() throws InterruptedException, IOException {
            while (!(numActiveSequencingThreads == 0 && queue.isEmpty())) {
                NGramWithCount nGramWithCount =
                        queue.poll(Constants.QUEUE_IDLE_TIME,
                                TimeUnit.MILLISECONDS);
                if (nGramWithCount == null) {
                    LOGGER.trace("AggregatingThread idle, because queue empty.");
                    StatisticalNumberHelper
                    .count("ContinuationChunker#AggregatingThread idle, because queue empty");
                    continue;
                }

                for (Pattern pattern : sourceToPattern.get(nGramWithCount
                        .getPattern())) {
                    addToChunk(pattern, nGramWithCount.getSequence(),
                            nGramWithCount.getCount());
                }
            }

            LOGGER.debug("AggregatingThread finished.");
            return null;
        }

        private void addToChunk(Pattern pattern, String sequence, long count)
                throws IOException {
            Chunk chunk = chunks.get(pattern);
            if (chunk == null) {
                chunk = new Chunk(0);
                chunks.put(pattern, chunk);
                chunkFiles.put(pattern, new LinkedList<Path>());
            }

            if (sequence == null) {
                writeChunkToFile(pattern, chunk);
                status.addChunked(true, pattern, new LinkedList<Path>(
                        chunkFiles.get(pattern)));
                LOGGER.debug("Finished pattern '%s'.", pattern);
                return;
            }

            String seq =
                    pattern.apply(StringUtils.splitAtChar(sequence, ' ')
                            .toArray(new String[0]));
            Counter counter = chunk.getSequenceCount(seq);
            if (counter == null) {
                counter = new Counter();
                chunk.increaseSize(seq.getBytes(Constants.CHARSET).length
                        + TAB_COUNTER_NL_BYTES);
                chunk.putSequenceCount(seq, counter);
            }
            counter.add(count);

            if (chunk.getSize() > chunkSize) {
                writeChunkToFile(pattern, chunk);
                chunks.put(pattern, new Chunk(chunk.getCounter() + 1));
            }
        }

        private void writeChunkToFile(Pattern pattern, Chunk chunk)
                throws IOException {
            Path chunkFile =
                    continuationChunkedDir.resolve(pattern.toString()).resolve(
                            "chunk" + chunk.getCounter());
            Files.deleteIfExists(chunkFile);

            try (BufferedWriter writer =
                    Files.newBufferedWriter(chunkFile, Constants.CHARSET)) {
                Map<String, Counter> sortedSequenceCounts =
                        new TreeMap<String, Counter>(chunk.getSequenceCounts());
                for (Entry<String, Counter> entry : sortedSequenceCounts
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

    private int numActiveSequencingThreads;

    private Status status;

    private long chunkSize;

    private long readerMemory;

    private long queueMemory;

    private Path absoluteCountedDir;

    private Path absoluteChunkedDir;

    private Path continuationCountedDir;

    private Path continuationChunkedDir;

    private Set<Pattern> patterns;

    private BlockingQueue<Pattern> patternsQueue;

    private Map<Pattern, BlockingQueue<NGramWithCount>> aggregatingQueues;

    private Progress progress;

    public void chunk(
            Status status,
            Set<Pattern> patterns,
            Path absoluteCountedDir,
            Path absoluteChunkedDir,
            Path continuationCountedDir,
            Path continuationChunkedDir) throws Exception {
        OUTPUT.setPhase(Phase.CONTINUATION_CHUNKING, true);

        if (patterns.isEmpty()) {
            LOGGER.debug("No patterns to chunk, returning.");
            progress.set(1.0);
            return;
        }
        LOGGER.debug("patterns = %s", patterns);
        Files.createDirectories(continuationChunkedDir);
        for (Pattern pattern : patterns) {
            Files.createDirectories(continuationChunkedDir.resolve(pattern
                    .toString()));
        }

        int numSequencingThreads = 1;
        int numAggregatingThreads =
                Math.max(1, CONFIG.getNumberOfCores() - numSequencingThreads);
        LOGGER.debug("numSequencingThreads  = %d", numSequencingThreads);
        LOGGER.debug("numAggregatingThreads = %d", numAggregatingThreads);

        calculateMemory(patterns.size(), numSequencingThreads,
                numAggregatingThreads);
        this.status = status;
        this.absoluteCountedDir = absoluteCountedDir;
        this.absoluteChunkedDir = absoluteChunkedDir;
        this.continuationCountedDir = continuationCountedDir;
        this.continuationChunkedDir = continuationChunkedDir;
        this.patterns = patterns;
        progress = new Progress(patterns.size());

        List<Callable<Object>> threads =
                prepareThreads(numSequencingThreads, numAggregatingThreads);

        LOGGER.debug("Launching Threads...");
        numActiveSequencingThreads = numSequencingThreads;
        ThreadUtils.executeThreads(CONFIG.getNumberOfCores(), threads);
    }

    private void calculateMemory(
            int numPatterns,
            int numSequencingThreads,
            int numAggregatingThreads) {
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

        readerMemory = chunkSize;
        queueMemory =
                (availableMemory - chunkMemory - readerMemory
                        * numSequencingThreads)
                        / numAggregatingThreads;

        LOGGER.debug("totalFreeMemory = %s",
                Output.humanReadableByteCount(totalFreeMemory, false));
        LOGGER.debug("availableMemory = %s",
                Output.humanReadableByteCount(availableMemory, false));
        LOGGER.debug("readerMemory    = %s",
                Output.humanReadableByteCount(readerMemory, false));
        LOGGER.debug("queueMemory     = %s",
                Output.humanReadableByteCount(queueMemory, false));
        LOGGER.debug("chunkSize       = %s",
                Output.humanReadableByteCount(chunkSize, false));
    }

    private List<Callable<Object>> prepareThreads(
            int numSequencingThreads,
            int numAggregatingThreads) {
        LOGGER.debug("Preparing Threads...");

        Map<Pattern, Pattern> patternToSource = new HashMap<Pattern, Pattern>();
        for (Pattern pattern : patterns) {
            patternToSource.put(pattern, pattern.getContinuationSource());
        }
        Map<Pattern, List<Pattern>> sourceToPattern =
                computeSourceToPattern(patternToSource, status);

        List<BlockingQueue<NGramWithCount>> aggregatingQueues =
                new ArrayList<BlockingQueue<NGramWithCount>>(
                        numAggregatingThreads);
        List<Map<Pattern, List<Pattern>>> aggregatingSourceToPattern =
                new ArrayList<Map<Pattern, List<Pattern>>>(
                        numAggregatingThreads);
        Map<Pattern, BlockingQueue<NGramWithCount>> sourceToAggregatingQueue =
                new HashMap<Pattern, BlockingQueue<NGramWithCount>>();
        setupThreadParameters(numAggregatingThreads, queueMemory,
                sourceToPattern, aggregatingQueues, aggregatingSourceToPattern,
                sourceToAggregatingQueue);

        PriorityBlockingQueue<Pattern> sourcePatternsQueue =
                new PriorityBlockingQueue<Pattern>(sourceToPattern.size(),
                        SOURCE_PATTERN_COMPARATOR);
        sourcePatternsQueue.addAll(sourceToPattern.keySet());

        patternsQueue = sourcePatternsQueue;
        this.aggregatingQueues = sourceToAggregatingQueue;

        List<Callable<Object>> threads = new LinkedList<Callable<Object>>();
        for (int i = 0; i != numSequencingThreads; ++i) {
            threads.add(new SequencingThread());
        }
        for (int i = 0; i != numAggregatingThreads; ++i) {
            threads.add(new AggregatingThread(aggregatingQueues.get(i),
                    aggregatingSourceToPattern.get(i)));
        }
        return threads;
    }

    private Map<Pattern, List<Pattern>> computeSourceToPattern(
            Map<Pattern, Pattern> patternToSource,
            Status status) {
        Set<Pattern> possiblePatterns = new HashSet<Pattern>();
        Set<Pattern> availablePatterns = new HashSet<Pattern>();
        availablePatterns.addAll(status.getChunkedPatterns(false));
        availablePatterns.addAll(status.getCounted(false));
        availablePatterns.addAll(status.getChunkedPatterns(true));
        availablePatterns.addAll(status.getCounted(true));

        Map<Pattern, List<Pattern>> result =
                new HashMap<Pattern, List<Pattern>>();
        while (!patternToSource.isEmpty()) {
            boolean changed = false;

            Iterator<Map.Entry<Pattern, Pattern>> i =
                    patternToSource.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<Pattern, Pattern> entry = i.next();
                Pattern pattern = entry.getKey();
                Pattern source = entry.getValue();

                if (availablePatterns.contains(source)
                        || possiblePatterns.contains(source)) {
                    List<Pattern> patterns = result.get(source);
                    if (patterns == null) {
                        patterns = new LinkedList<Pattern>();
                        result.put(source, patterns);
                    }
                    patterns.add(pattern);
                    possiblePatterns.add(pattern);
                    i.remove();
                    changed = true;
                }
            }

            if (!changed) {
                LOGGER.error(
                        "Cannot not calculate continuation counts for these patterns, since source patterns are not present: %s.",
                        patternToSource.keySet());
                break;
            }
        }
        return result;
    }

    private
    void
    setupThreadParameters(
            int numAggregatingThreads,
            long queueMemory,
            Map<Pattern, List<Pattern>> sourceToPattern,
            List<BlockingQueue<NGramWithCount>> aggregatingQueues,
            List<Map<Pattern, List<Pattern>>> aggregatingSourceToPattern,
            Map<Pattern, BlockingQueue<NGramWithCount>> sourceToAggregatingQueues) {
        for (int i = 0; i != numAggregatingThreads; ++i) {
            aggregatingQueues.add(new ArrayBlockingQueue<NGramWithCount>(
                    (int) (queueMemory / AVERAGE_QUEUE_ITEM_SIZE)));
            aggregatingSourceToPattern
            .add(new HashMap<Pattern, List<Pattern>>());
        }

        int threadIter = 0;
        for (Map.Entry<Pattern, List<Pattern>> entry : sourceToPattern
                .entrySet()) {
            Pattern source = entry.getKey();
            aggregatingSourceToPattern.get(threadIter).put(source,
                    entry.getValue());
            sourceToAggregatingQueues.put(source,
                    aggregatingQueues.get(threadIter));
            threadIter = (threadIter + 1) % numAggregatingThreads;
        }
    }

}
