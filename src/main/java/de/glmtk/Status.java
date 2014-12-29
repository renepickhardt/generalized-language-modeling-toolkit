package de.glmtk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.util.HashUtils;
import de.glmtk.util.StringUtils;

/**
 * This class is a wrapper for status.txt which controls unwanted interruptions
 * of the program. So if the program crashes during training one does not have
 * to repeat the entire training phase.
 *
 * This file should be save to multithreading and save against outside
 * manipulations of status.txt
 */

public class Status {

    private static final Logger LOGGER = LogManager.getLogger(Status.class);

    private Path file;

    private Path corpus;

    public static enum TrainingStatus {

        NONE, DONE, DONE_WITH_POS;

        public static TrainingStatus fromString(String trainginStatus) {
            try {
                return valueOf(trainginStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Unkown training status '" + trainginStatus
                        + "'. Assuming 'NONE'.");
                return NONE;
            }
        }

    }

    private String hash;

    private TrainingStatus training;

    private Map<Pattern, List<Path>> absoluteChunked;

    private Set<Pattern> absoluteCounted;

    private Map<Pattern, List<Path>> continuationChunked;

    private Set<Pattern> continuationCounted;

    public Status(
            Path file,
            Path corpus) throws IOException {
        this.file = file;
        this.corpus = corpus;

        hash = HashUtils.generateMd5Hash(this.corpus);

        setDefaultSettings(true);
        if (Files.exists(file)) {
            readStatusFromFile();
        }
    }

    private void setDefaultSettings(boolean withTraning) {
        if (withTraning) {
            training = TrainingStatus.NONE;
        }
        absoluteChunked = new HashMap<Pattern, List<Path>>();
        absoluteCounted = new HashSet<Pattern>();
        continuationChunked = new HashMap<Pattern, List<Path>>();
        continuationCounted = new HashSet<Pattern>();
    }

    public TrainingStatus getTraining() {
        return training;
    }

    public void setTraining(TrainingStatus training, Path trainingFile)
            throws IOException {
        synchronized (this) {
            hash = HashUtils.generateMd5Hash(trainingFile);
            this.training = training;
            setDefaultSettings(false);
            writeStatusToFile();
        }
    }

    private Map<Pattern, List<Path>> chunked(boolean continuation) {
        return continuation ? continuationChunked : absoluteChunked;
    }

    private Set<Pattern> counted(boolean continuation) {
        return continuation ? continuationCounted : absoluteCounted;
    }

    public Set<Pattern> getChunkedPatterns(boolean continuation) {
        return Collections.unmodifiableSet(chunked(continuation).keySet());
    }

    public List<Path> getChunks(boolean continuation, Pattern pattern) {
        return Collections.unmodifiableList(chunked(continuation).get(pattern));
    }

    public void addChunked(
            boolean continuation,
            Map<Pattern, List<Path>> chunked) throws IOException {
        synchronized (this) {
            chunked(continuation).putAll(chunked);
            writeStatusToFile();
        }
    }

    public void addChunked(
            boolean continuation,
            Pattern pattern,
            List<Path> chunks) throws IOException {
        synchronized (this) {
            chunked(continuation).put(pattern, chunks);
            writeStatusToFile();
        }
    }

    public void performChunkedMerge(
            boolean continuation,
            Pattern pattern,
            List<Path> mergedChunks,
            Path mergeFile) throws IOException {
        synchronized (this) {
            List<Path> chunks = chunked(continuation).get(pattern);
            chunks.removeAll(mergedChunks);
            chunks.add(mergeFile);
            writeStatusToFile();
        }
    }

    public void finishChunkedMerge(boolean continuation, Pattern pattern)
            throws IOException {
        synchronized (this) {
            chunked(continuation).remove(pattern);
            counted(continuation).add(pattern);
            writeStatusToFile();
        }
    }

    public Set<Pattern> getCounted(boolean continuation) {
        return Collections.unmodifiableSet(counted(continuation));
    }

    public void logStatus() {
        LOGGER.debug("Status {}", StringUtils.repeat("-", 80 - 7));
        LOGGER.debug("hash                = {}", hash);
        LOGGER.debug("training            = {}", training);
        LOGGER.debug("absoluteChunked     = {}", absoluteChunked);
        LOGGER.debug("absoluteCounted     = {}", absoluteCounted);
        LOGGER.debug("continuationChunked = {}", continuationChunked);
        LOGGER.debug("continuationCounted = {}", continuationCounted);
    }

    private void writeStatusToFile() throws IOException {
        Path tmpFile = Paths.get(file + ".tmp");

        Files.deleteIfExists(tmpFile);
        try (BufferedWriter writer =
                Files.newBufferedWriter(tmpFile, Constants.CHARSET)) {
            // Hash
            writer.append("hash = " + hash + "\n");

            // Training
            writer.append("training = " + training + "\n");

            // Absolute Chunked
            writeChunked(writer, "absoluteChunked", absoluteChunked);

            // Absolute Counted
            writeCounted(writer, "absoluteCounted", absoluteCounted);

            // Continuation Chunked
            writeChunked(writer, "continuationChunked", continuationChunked);

            // Continuation Counted
            writeCounted(writer, "continuationCounted", continuationCounted);
        }

        Files.deleteIfExists(file);
        Files.move(tmpFile, file);
    }

    private void writeChunked(
            BufferedWriter writer,
            String name,
            Map<Pattern, List<Path>> chunked) throws IOException {
        writer.append(name);
        writer.append(" = ");
        boolean first = true;
        for (Map.Entry<Pattern, List<Path>> entry : chunked.entrySet()) {
            Pattern pattern = entry.getKey();
            List<Path> chunks = entry.getValue();
            if (first) {
                first = false;
            } else {
                writer.append(',');
            }
            writer.append(pattern + ":" + StringUtils.join(chunks, ";"));
        }
        writer.append('\n');
    }

    private void writeCounted(
            BufferedWriter writer,
            String name,
            Set<Pattern> counted) throws IOException {
        writer.append(name);
        writer.append(" = ");
        writer.append(StringUtils.join(counted, ","));
        writer.append('\n');
    }

    private void readStatusFromFile() throws IOException {
        try (BufferedReader reader =
                Files.newBufferedReader(file, Constants.CHARSET)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                Matcher matcher;

                matcher = getPattern("hash").matcher(line);
                if (matcher.matches()) {
                    String statusHash = matcher.group(1);
                    if (!hash.equals(statusHash)) {
                        setDefaultSettings(true);
                        LOGGER.warn("New Hash didn't match old one. Overwriting.");
                        break;
                    }
                    continue;
                }

                matcher = getPattern("training").matcher(line);
                if (matcher.matches()) {
                    training = TrainingStatus.fromString(matcher.group(1));
                    continue;
                }

                matcher = getPattern("absoluteChunked").matcher(line);
                if (matcher.matches()) {
                    readChunked(false, matcher);
                    continue;
                }

                matcher = getPattern("absoluteCounted").matcher(line);
                if (matcher.matches()) {
                    readCounted(false, matcher);
                    continue;
                }

                matcher = getPattern("continuationChunked").matcher(line);
                if (matcher.matches()) {
                    readChunked(true, matcher);
                    continue;
                }

                matcher = getPattern("continuationCounted").matcher(line);
                if (matcher.matches()) {
                    readCounted(true, matcher);
                    continue;
                }
            }
        }
    }

    private void readChunked(boolean continuation, Matcher matcher) {
        if (continuation) {
            continuationChunked = new HashMap<Pattern, List<Path>>();
        } else {
            absoluteChunked = new HashMap<Pattern, List<Path>>();
        }
        for (String patternAndChunks : StringUtils.splitAtChar(
                matcher.group(1), ',')) {
            List<String> split = StringUtils.splitAtChar(patternAndChunks, ':');
            if (split.size() != 2) {
                throw new RuntimeException("Illegal format for '"
                        + (continuation
                                ? "continuationChunked"
                                        : "absoluteChunged") + "': " + patternAndChunks);
            }

            Pattern pattern = Patterns.get(split.get(0));
            List<Path> chunks = new LinkedList<Path>();
            for (String chunk : StringUtils.splitAtChar(split.get(1), ';')) {
                chunks.add(Paths.get(chunk));
            }
            chunked(continuation).put(pattern, chunks);
        }
    }

    private void readCounted(boolean continuation, Matcher matcher) {
        if (continuation) {
            continuationCounted = new HashSet<Pattern>();
        } else {
            absoluteCounted = new HashSet<Pattern>();
        }
        for (String pattern : StringUtils.splitAtChar(matcher.group(1), ',')) {
            counted(continuation).add(Patterns.get(pattern));
        }
    }

    private static java.util.regex.Pattern getPattern(String option) {
        return java.util.regex.Pattern.compile("^" + option
                + "\\s*=\\s*(\\S+)\\s*$");
    }

}
