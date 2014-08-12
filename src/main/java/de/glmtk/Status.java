package de.glmtk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
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

    private Set<Pattern> sequenced;

    private Set<Pattern> absolute;

    private Set<Pattern> continuation;

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
        sequenced = new HashSet<Pattern>();
        absolute = new HashSet<Pattern>();
        continuation = new HashSet<Pattern>();
    }

    public TrainingStatus getTraining() {
        return training;
    }

    public void setTraining(TrainingStatus training) throws IOException {
        this.training = training;

        // Reset all other options
        sequenced = new HashSet<Pattern>();
        absolute = new HashSet<Pattern>();
        continuation = new HashSet<Pattern>();

        writeStatusToFile();
    }

    public Set<Pattern> getSequenced() {
        return sequenced;
    }

    public void setSequenced(Set<Pattern> sequenced) {
        this.sequenced = sequenced;
    }

    public Set<Pattern> getAbsolute() {
        return absolute;
    }

    public void setAbsolute(Set<Pattern> absolute) throws IOException {
        this.absolute = absolute;

        writeStatusToFile();
    }

    public Set<Pattern> getContinuation() {
        return continuation;
    }

    public void setContinuation(Set<Pattern> continuation) throws IOException {
        this.continuation = continuation;

        writeStatusToFile();
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

                matcher = getPattern("sequenced").matcher(line);
                if (matcher.matches()) {
                    sequenced = new HashSet<Pattern>();
                    for (String pattern : StringUtils.splitAtChar(
                            matcher.group(1), ',')) {
                        sequenced.add(new Pattern(pattern));
                    }
                    continue;
                }

                matcher = getPattern("absolute").matcher(line);
                if (matcher.matches()) {
                    absolute = new HashSet<Pattern>();
                    for (String pattern : StringUtils.splitAtChar(
                            matcher.group(1), ',')) {
                        absolute.add(new Pattern(pattern));
                    }
                    continue;
                }

                matcher = getPattern("continuation").matcher(line);
                if (matcher.matches()) {
                    continuation = new HashSet<Pattern>();
                    for (String pattern : StringUtils.splitAtChar(
                            matcher.group(1), ',')) {
                        continuation.add(new Pattern(pattern));
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

    private void writeStatusToFile() throws IOException {
        Files.deleteIfExists(file);
        try (BufferedWriter writer =
                Files.newBufferedWriter(file, Charset.defaultCharset())) {
            writer.append("hash = " + hash + "\n");
            writer.append("training = " + training + "\n");
            writer.append("sequenced = " + StringUtils.join(sequenced, ",")
                    + "\n");
            writer.append("absolute = " + StringUtils.join(absolute, ",")
                    + "\n");
            writer.append("continuation = "
                    + StringUtils.join(continuation, ",") + "\n");
        }
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
