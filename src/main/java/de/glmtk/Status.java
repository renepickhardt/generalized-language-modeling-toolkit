package de.glmtk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

import de.glmtk.executables.Termination;
import de.glmtk.pattern.Pattern;
import de.glmtk.utils.StringUtils;

public class Status {

    private static final Logger LOGGER = LogManager.getLogger(Status.class);

    private Path file;

    private Path tmpFile;

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
            Path tmpFile,
            Path corpus) throws IOException {
        this.file = file;
        this.tmpFile = tmpFile;
        this.corpus = corpus;

        hash = generateFileHash(this.corpus);

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

    public void setTraining(TrainingStatus training) throws IOException {
        synchronized (this) {
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
        Files.deleteIfExists(tmpFile);
        try (OutputStreamWriter writer =
                new OutputStreamWriter(Files.newOutputStream(tmpFile))) {
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
            OutputStreamWriter writer,
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
            OutputStreamWriter writer,
            String name,
            Set<Pattern> counted) throws IOException {
        writer.append(name);
        writer.append(" = ");
        writer.append(StringUtils.join(counted, ","));
        writer.append('\n');
    }

    private void readStatusFromFile() throws IOException {
        try (BufferedReader reader =
                Files.newBufferedReader(file, Charset.defaultCharset())) {
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
                    absoluteChunked = new HashMap<Pattern, List<Path>>();
                    for (String patternAndChunks : StringUtils.splitAtChar(
                            matcher.group(1), ',')) {
                        List<String> split =
                                StringUtils.splitAtChar(patternAndChunks, ':');
                        if (split.size() != 2) {
                            LOGGER.error(
                                    "Illegal format for 'absoluteChunked': {}",
                                    patternAndChunks);
                            throw new Termination();
                        }

                        Pattern pattern = new Pattern(split.get(0));
                        List<Path> chunks = new LinkedList<Path>();
                        for (String chunk : StringUtils.splitAtChar(
                                split.get(1), ';')) {
                            chunks.add(Paths.get(chunk));
                        }
                        absoluteChunked.put(pattern, chunks);
                    }
                    continue;
                }

                matcher = getPattern("absoluteCounted").matcher(line);
                if (matcher.matches()) {
                    absoluteCounted = new HashSet<Pattern>();
                    for (String pattern : StringUtils.splitAtChar(
                            matcher.group(1), ',')) {
                        absoluteCounted.add(new Pattern(pattern));
                    }
                    continue;
                }
            }
        }
    }

    private static java.util.regex.Pattern getPattern(String option) {
        return java.util.regex.Pattern.compile("^" + option
                + "\\s*=\\s*(\\S+)\\s*$");
    }

    /**
     * @see <a href=
     *      "http://stackoverflow.com/questions/304268/getting-a-files-md5-checksum-in-java"
     *      >Stackoverflow: Getting a File's MD5 Checksum in Java</a>.
     */
    private static String generateFileHash(Path file) throws IOException {
        try {
            InputStream input = Files.newInputStream(file);

            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");

            int numRead;
            do {
                numRead = input.read(buffer);
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead);
                }
            } while (numRead != -1);

            input.close();

            byte[] resultByte = digest.digest();
            String result = "";
            for (int i = 0; i != resultByte.length; ++i) {
                result +=
                        Integer.toString((resultByte[i] & 0xff) + 0x100, 16)
                        .substring(1);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
