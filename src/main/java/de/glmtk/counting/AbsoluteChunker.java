package de.glmtk.counting;

import static de.glmtk.Config.CONFIG;
import static de.glmtk.common.Output.OUTPUT;
import static de.glmtk.util.PrintUtils.humanReadableByteCount;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.javamex.classmexer.MemoryUtil;
import com.javamex.classmexer.MemoryUtil.VisibilityFilter;

import de.glmtk.Constants;
import de.glmtk.Status;
import de.glmtk.common.Output.Phase;
import de.glmtk.common.Output.Progress;
import de.glmtk.common.Pattern;
import de.glmtk.util.NioUtils;
import de.glmtk.util.StatisticalNumberHelper;
import de.glmtk.util.StringUtils;
import de.glmtk.util.ThreadUtils;

public enum AbsoluteChunker {

    ABSOLUTE_CHUNKER;

    private static final Logger LOGGER = LogManager
            .getFormatterLogger(AbsoluteChunker.class);

    private static final int TAB_COUNT_NL_BYTES = "\t10\n"
            .getBytes(Constants.CHARSET).length;

    private class Thread implements Callable<Object> {

        private Pattern pattern;

        private Path patternDir;

        private List<Path> chunkFiles;

        private long chunkSize;

        private Map<String, Long> chunkCounts;

        @Override
        public Object call() throws InterruptedException, IOException {
            while (!patternQueue.isEmpty()) {
                pattern =
                        patternQueue.poll(Constants.QUEUE_TIMEOUT,
                                TimeUnit.MILLISECONDS);
                if (pattern == null) {
                    continue;
                }

                LOGGER.debug("Counting pattern '%s'.", pattern);

                patternDir = absoluteChunkedDir.resolve(pattern.toString());
                chunkFiles = new LinkedList<Path>();
                chunkSize = 0L;
                chunkCounts = new HashMap<String, Long>();

                Files.createDirectories(patternDir);

                int patternSize = pattern.size();
                try (BufferedReader reader =
                        NioUtils.newBufferedReader(trainingFile,
                                Constants.CHARSET, readerMemory)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] split =
                                StringUtils.splitAtChar(line, ' ').toArray(
                                        new String[0]);
                        String[] words = new String[split.length];
                        String[] poses = new String[split.length];
                        StringUtils.extractWordsAndPoses(split,
                                trainingFileHasPos, words, poses);

                        for (int p = 0; p <= split.length - patternSize; ++p) {
                            String sequence = pattern.apply(words, poses, p);
                            countSequenceInChunk(sequence);
                        }
                    }
                }

                writeChunkToFile(); // Write remaining partial chunk.
                chunkCounts = null; // Free memory of map.

                Map<Pattern, List<Path>> m = new HashMap<Pattern, List<Path>>();
                m.put(pattern, chunkFiles);
                status.addChunked(false, m);

                LOGGER.debug("Finished pattern '%s'.", pattern);

                synchronized (progress) {
                    progress.increase(1);
                }
            }

            LOGGER.debug("Thread finished.");
            return null;
        }

        private void countSequenceInChunk(String sequence) throws IOException {
            Long count = chunkCounts.get(sequence);
            if (count == null) {
                chunkSize +=
                        sequence.getBytes(Constants.CHARSET).length
                        + TAB_COUNT_NL_BYTES;
                chunkCounts.put(sequence, 1L);
            } else {
                chunkCounts.put(sequence, count + 1L);
            }

            if (chunkSize > Constants.CHUNK_SIZE) {
                if (Constants.DEBUG_AVERAGE_MEMORY) {
                    StatisticalNumberHelper.average("AbsoluteChunk Map Memory",
                            MemoryUtil.deepMemoryUsageOf(chunkCounts,
                                    VisibilityFilter.ALL));
                }

                writeChunkToFile();
                chunkSize = 0L;
                chunkCounts = new HashMap<String, Long>();
            }
        }

        private void writeChunkToFile() throws IOException {
            Path chunkFile = patternDir.resolve("chunk" + chunkFiles.size());
            Files.deleteIfExists(chunkFile);

            try (BufferedWriter writer =
                    NioUtils.newBufferedWriter(chunkFile, Constants.CHARSET,
                            writerMemory)) {
                Map<String, Long> sortedCounts =
                        new TreeMap<String, Long>(chunkCounts);
                for (Entry<String, Long> entry : sortedCounts.entrySet()) {
                    writer.write(entry.getKey());
                    writer.write('\t');
                    writer.write(entry.getValue().toString());
                    writer.write('\n');
                }
            }

            chunkFiles.add(chunkFile.getFileName());

            LOGGER.debug("Wrote chunk for pattern '%s': '%s'.", pattern,
                    chunkFile);
        }

    }

    private Progress progress;

    private int readerMemory;

    private int writerMemory;

    private long chunkSize;

    private Status status;

    private Path trainingFile;

    private boolean trainingFileHasPos;

    private Path absoluteChunkedDir;

    private BlockingQueue<Pattern> patternQueue;

    public void chunk(
            Status status,
            Set<Pattern> patterns,
            Path trainingFile,
            Path absoluteChunkedDir) throws Exception {
        OUTPUT.setPhase(Phase.ABSOLUTE_CHUNKING, true);
        LOGGER.debug("patterns = '%s'", patterns);
        progress = new Progress(patterns.size());

        if (patterns.isEmpty()) {
            LOGGER.debug("No patterns to chunk, returning.");
            progress.set(1.0);
            return;
        }

        calculateMemory();
        this.status = status;
        this.trainingFile = trainingFile;
        trainingFileHasPos = true;
        this.absoluteChunkedDir = absoluteChunkedDir;
        patternQueue = new LinkedBlockingQueue<Pattern>(patterns);

        List<Callable<Object>> threads = new LinkedList<Callable<Object>>();
        for (int i = 0; i != CONFIG.getNumberOfCores(); ++i) {
            threads.add(new Thread());
        }
        ThreadUtils.executeThreads(CONFIG.getNumberOfCores(), threads);
    }

    private void calculateMemory() {
        double CHUNK_LOAD_FACTOR = 5.5;
        double AVAILABLE_MEM_RATIO = 0.5;

        LOGGER.debug("Calculating Memory...");
        Runtime r = Runtime.getRuntime();
        r.gc();

        long totalFreeMem = r.maxMemory() - r.totalMemory() + r.freeMemory();
        long availableMem = (long) (AVAILABLE_MEM_RATIO * totalFreeMem);
        long memPerThread = availableMem / CONFIG.getNumberOfCores();

        readerMemory = Constants.BUFFER_SIZE;
        writerMemory = Constants.BUFFER_SIZE;
        long chunkMemory =
                (long) Math.min(CHUNK_LOAD_FACTOR * Constants.CHUNK_SIZE,
                        memPerThread - readerMemory - writerMemory);
        chunkSize = (long) (chunkMemory / CHUNK_LOAD_FACTOR);

        LOGGER.debug("totalFreeMem = %s", humanReadableByteCount(totalFreeMem));
        LOGGER.debug("availableMem = %s", humanReadableByteCount(availableMem));
        LOGGER.debug("memPerThread = %s", humanReadableByteCount(memPerThread));
        LOGGER.debug("readerMemory = %s", humanReadableByteCount(readerMemory));
        LOGGER.debug("writerMemory = %s", humanReadableByteCount(writerMemory));
        LOGGER.debug("chunkSize    = %s", humanReadableByteCount(chunkSize));
    }

}
