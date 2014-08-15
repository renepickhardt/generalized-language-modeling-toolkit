package de.glmtk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.pattern.Pattern;
import de.glmtk.utils.StringUtils;

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

    private Map<Pattern, Queue<Path>> absoluteChunked;

    private Set<Pattern> absoluteCounted;

    public Status(
            Path file,
            Path corpus) throws IOException {
        this.file = file;
        this.corpus = corpus;

        hash = generateFileHash(this.corpus);

        setDefaultSettings();
        if (Files.exists(file)) {
            readStatusFromFile();
        }
    }

    private void setDefaultSettings() {
        training = TrainingStatus.NONE;
        absoluteChunked = new HashMap<Pattern, Queue<Path>>();
        absoluteCounted = new HashSet<Pattern>();
    }

    public TrainingStatus getTraining() {
        return training;
    }

    public void setTraining(TrainingStatus training) throws IOException {
        this.training = training;

        // Reset all other options
        absoluteChunked = new HashMap<Pattern, Queue<Path>>();
        absoluteCounted = new HashSet<Pattern>();

        writeStatusToFile();
    }

    public Map<Pattern, Queue<Path>> getAbsoluteChunked() {
        return absoluteChunked;
    }

    public void setAbsoluteChunked(Map<Pattern, Queue<Path>> absoluteChunked) {
        this.absoluteChunked = absoluteChunked;
    }

    public Set<Pattern> getAbsoluteCounted() {
        return absoluteCounted;
    }

    public void setAbsoluteCounted(Set<Pattern> absoluteCounted) {
        this.absoluteCounted = absoluteCounted;
    }

    public void logStatus() {
        LOGGER.debug("Status {}", StringUtils.repeat("-", 80 - 7));
        LOGGER.debug("hash            = {}", hash);
        LOGGER.debug("training        = {}", training);
        LOGGER.debug("absoluteChunked = {}", absoluteChunked);
        LOGGER.debug("absoluteCounted = {}", absoluteCounted);
    }

    public void writeStatusToFile() throws IOException {
        Files.deleteIfExists(file);
        try (BufferedWriter writer =
                Files.newBufferedWriter(file, Charset.defaultCharset())) {
            // Hash
            writer.append("hash = " + hash + "\n");

            // Training
            writer.append("training = " + training + "\n");

            // Absolute Chunked
            writer.append("absoluteChunked = ");
            boolean first = true;
            for (Map.Entry<Pattern, Queue<Path>> entry : absoluteChunked
                    .entrySet()) {
                Pattern pattern = entry.getKey();
                Queue<Path> chunks = entry.getValue();
                if (first) {
                    first = false;
                } else {
                    writer.append(',');
                }
                writer.append(pattern + ":[" + StringUtils.join(chunks, ";")
                        + "]");
            }
            writer.append('\n');

            // Absolute Couted
            writer.append("absoluteCounted = "
                    + StringUtils.join(absoluteCounted, ",") + "\n");
        }
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
                        setDefaultSettings();
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
                    absoluteChunked = new HashMap<Pattern, Queue<Path>>();
                    for (String patternAndChunks : StringUtils.splitAtChar(
                            matcher.group(1), ',')) {
                        List<String> split =
                                StringUtils.splitAtChar(patternAndChunks, ':');
                        Pattern pattern = new Pattern(split.get(0));
                        Queue<Path> chunks = new LinkedList<Path>();
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
