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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public Status(
            Path file,
            Path corpus) throws IOException {
        this.file = file;
        this.corpus = corpus;

        hash = generateFileHash(corpus);

        if (Files.exists(file)) {
            readStatusFromFile();
        } else {
            setDefaultSettings();
        }
    }

    private void setDefaultSettings() {
        training = TrainingStatus.NONE;
    }

    public TrainingStatus getTraining() {
        return training;
    }

    public void setTraining(TrainingStatus training) {
        this.training = training;
        // Reset all other options
    }

    public void update() throws IOException {
        Files.deleteIfExists(file);
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

                matcher = Pattern.compile(getPattern("hash")).matcher(line);
                if (matcher.matches()) {
                    String statusHash = matcher.group(1);
                    if (!hash.equals(statusHash)) {
                        setDefaultSettings();
                        LOGGER.warn("New Hash didn't match old one. Overwriting.");
                        break;
                    }
                }

                matcher = Pattern.compile(getPattern("training")).matcher(line);
                if (matcher.matches()) {
                    training = TrainingStatus.fromString(matcher.group(1));
                }
            }
        }
    }

    private static String getPattern(String option) {
        return "^" + option + "\\s*=\\s*(\\w+)\\s*$";
    }

    private void writeStatusToFile() throws IOException {
        try (BufferedWriter writer =
                Files.newBufferedWriter(file, Charset.defaultCharset())) {
            writer.append("hash = " + hash + "\n");
            writer.append("training = " + training + "\n");
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
